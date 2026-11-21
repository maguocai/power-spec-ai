package com.power.ai.controller;

import com.power.ai.dto.AnswerResponse;
import com.power.ai.dto.QuestionRequest;
import com.power.ai.service.RagService;
import com.power.ai.service.SpecQaService;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author maguocai
 * @date 2026/11/16 22:55
 * @description: 电力技术规范问答控制器
 */
@RestController
@RequestMapping("/api/spec-qa")
@AllArgsConstructor
public class SpecQaController {

    private final SpecQaService specQaService;
    private final RagService ragService;

    /**
     * 电力技术规范问答接口
     * POST /api/spec-qa/ask
     */
    // 问答接口：只传问题即可，自动分类
    @PostMapping("/ask")
    public AnswerResponse ask(@RequestBody QuestionRequest request) {
        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        return specQaService.ask(request.getQuestion());
    }

    // 上传接口：自动分类，无需手动指定
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadDocument(@RequestPart("file") MultipartFile file) throws Exception {
        ragService.addDocumentToKnowledgeBase(file);
        return "文档上传成功，已自动分类并加入知识库";
    }
}
