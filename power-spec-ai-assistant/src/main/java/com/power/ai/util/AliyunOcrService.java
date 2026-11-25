package com.power.ai.util;

import com.aliyun.ocr_api20210707.Client;
import com.aliyun.ocr_api20210707.models.RecognizeAllTextRequest;
import com.aliyun.ocr_api20210707.models.RecognizeAllTextResponse;
import com.aliyun.ocr_api20210707.models.RecognizeGeneralRequest;
import com.aliyun.ocr_api20210707.models.RecognizeGeneralResponse;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import jakarta.annotation.PostConstruct;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author maguocai
 * @date 2026/11/24 22:12
 * @description 阿里云OCR工具类
 */
@Component
public class AliyunOcrService {

    private static final Logger log = LoggerFactory.getLogger(AliyunOcrService.class);

    @Value("${aliyun.ocr.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.ocr.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.ocr.endpoint}")
    private String endpoint;

    // 固定 10 个线程，匹配阿里云 OCR 默认 10 QPS 并发限制
    @Value("${aliyun.ocr.qps:10}")
    private Integer OCR_THREAD_POOL_SIZE;

    // OCR 场景 150 DPI 通常足够，体积更可控
    private static final float RENDER_DPI = 150f;
    // JPEG 压缩质量，0.8 在清晰度和体积间较平衡
    private static final float JPEG_QUALITY = 0.8f;

    // 静态字段，供静态方法使用
    private static String AK_ID;
    private static String AK_SECRET;
    private static String ENDPOINT;
    private static int QPS;

    @PostConstruct
    public void init() {
        AK_ID = accessKeyId;
        AK_SECRET = accessKeySecret;
        ENDPOINT = endpoint;
        QPS = OCR_THREAD_POOL_SIZE;
    }

    private static Client createClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(AK_ID)
                .setAccessKeySecret(AK_SECRET)
                .setEndpoint(ENDPOINT);
        return new Client(config);
    }

    /**
     * 多页 PDF 并发 OCR 识别（逐页渲染图片 + RecognizeGeneral）
     */
    public static String extractTextFromMultiPagePdf(InputStream pdfInputStream) throws Exception {
        ByteArrayOutputStream pdfBuffer = new ByteArrayOutputStream();
        byte[] temp = new byte[8192];
        int len;
        while ((len = pdfInputStream.read(temp)) != -1) {
            pdfBuffer.write(temp, 0, len);
        }
        byte[] pdfBytes = pdfBuffer.toByteArray();

        if (pdfBytes.length == 0) {
            throw new IOException("PDF 字节数组为空，输入流可能已被消费");
        }

        int totalPages;
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            totalPages = document.getNumberOfPages();
        }
        log.info("PDF 总页数：{}，开始并发 OCR（逐页渲染图片）", totalPages);

        if (totalPages == 0) {
            return "";
        }

        ExecutorService executor = Executors.newFixedThreadPool(QPS);
        List<Future<String>> futures = new ArrayList<>();

        try {
            for (int pageNo = 1; pageNo <= totalPages; pageNo++) {
                final int currentPage = pageNo;
                futures.add(executor.submit(() -> recognizeSinglePageAsImage(pdfBytes, currentPage)));
            }

            StringBuilder fullText = new StringBuilder();
            for (int i = 0; i < futures.size(); i++) {
                try {
                    String pageText = futures.get(i).get();
                    if (pageText != null && !pageText.isEmpty()) {
                        fullText.append(pageText).append("\n\n");
                    }
                } catch (ExecutionException e) {
                    log.error("第 {} 页 OCR 识别失败", i + 1, e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("第 {} 页 OCR 被中断", i + 1);
                    break;
                }
            }
            return fullText.toString().trim();

        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(120, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 单页处理：PDF 页 -> JPEG 图片 -> RecognizeGeneral
     */
    private static String recognizeSinglePageAsImage(byte[] pdfBytes, int pageNo) {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDFRenderer renderer = new PDFRenderer(document);
            // pageNo 从 1 开始，renderImageWithDPI 索引从 0 开始
            BufferedImage image = renderer.renderImageWithDPI(pageNo - 1, RENDER_DPI);

            // 渲染为 JPEG 字节数组，控制体积
            byte[] imageBytes = renderToJpegBytes(image);

            if (imageBytes.length > 10 * 1024 * 1024) {
                log.warn("第 {} 页图片体积 {} 字节，超过 10MB 限制，跳过", pageNo, imageBytes.length);
                return "";
            }

            Client client = createClient();
            RuntimeOptions runtime = new RuntimeOptions();

            try (ByteArrayInputStream imageStream = new ByteArrayInputStream(imageBytes)) {
                RecognizeGeneralRequest request = new RecognizeGeneralRequest()
                        .setBody(imageStream);

                RecognizeGeneralResponse response = client.recognizeGeneralWithOptions(request, runtime);

                if (response == null || response.getBody() == null
                        || response.getBody().getData() == null) {
                    log.warn("第 {} 页识别结果为空", pageNo);
                    return "";
                }

                String fullText = response.getBody().getData();
                if (fullText == null) {
                    log.warn("第 {} 页 fullText 为 null", pageNo);
                    return "";
                }
                return fullText;
            }
        } catch (Exception e) {
            log.error("第 {} 页 OCR 调用异常", pageNo, e);
            return "";
        }
    }

    /**
     * BufferedImage 转 JPEG 字节数组，使用指定压缩质量控制体积
     */
    private static byte[] renderToJpegBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }
}

