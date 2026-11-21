package com.power.ai.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.web.multipart.MultipartFile;

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
    public static String parse(MultipartFile file) throws IOException {
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
    private static String parsePdf(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
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
