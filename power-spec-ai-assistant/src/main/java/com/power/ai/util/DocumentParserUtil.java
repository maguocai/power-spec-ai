package com.power.ai.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.util.StringUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author maguocai
 * @date 2026/11/17 22:11
 * @description: 文档解析工具类
 */
public class DocumentParserUtil {

    /**
     * 根据文件后缀自动解析，返回纯文本
     */
    public static String parse(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        if (fileName.endsWith(".pdf")) {
            return parsePdf(file.getInputStream());
        } else if (fileName.endsWith(".docx")) {
            return parseWord(file.getInputStream());
        } else {
            throw new IllegalArgumentException("仅支持PDF和DOCX格式");
        }
    }

    /**
     * 解析PDF文本
     */
    private static String parsePdf(InputStream inputStream) throws Exception {
        // 1. 先把流完整读入字节数组，避免被消费后无法复用
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, n);
        }
        byte[] pdfBytes = buffer.toByteArray();

        if (pdfBytes.length == 0) {
            throw new IOException("PDF 内容为空，输入流可能无效");
        }

        // 2. 第一次：正常文本解析
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String result = stripper.getText(document);
            if (!StringUtil.isBlank(result)) {
                return result;
            }
        }

        // 3. 文本解析为空，走 OCR，传入新构造的字节流
        return AliyunOcrService.extractTextFromMultiPagePdf(new ByteArrayInputStream(pdfBytes));
    }

    /**
     * 解析Word（docx）文本
     */
    private static String parseWord(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph p : paragraphs) {
                sb.append(p.getText()).append("\n");
            }
            return sb.toString();
        }
    }
}
