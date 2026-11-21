package com.power.ai.service.impl;

import com.power.ai.config.PromptConfig;
import com.power.ai.dto.AnswerResponse;
import com.power.ai.service.RagService;
import com.power.ai.service.SpecQaService;
import lombok.AllArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author maguocai
 * @date 2026/11/16 22:11
 * @description: 电力技术规范问答服务实现
 */
@Service
@AllArgsConstructor
public class SpecQaServiceImpl implements SpecQaService {

    private final ChatModel chatModel;
    private final PromptConfig promptConfig;
    private final RagService ragService;

    @Override
    public AnswerResponse ask(String question) {
        long start = System.currentTimeMillis();

        // 1. 自动分类 + 检索相关上下文
        String context = ragService.getRelevantContextByAutoClassify(question, 3);

        // 2. 构建RAG增强提示词
        String systemPrompt = buildRagSystemPrompt(context);

        // 3. 调用大模型生成回答
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage(question));
        ChatResponse response = chatModel.call(new Prompt(messages));
        String answer = response.getResult().getOutput().getText();

        return new AnswerResponse(
                answer,
                "智能匹配分类",
                System.currentTimeMillis() - start
        );
    }

    private String buildRagSystemPrompt(String context) {
        String basePrompt = """
            你是资深电力行业技术规范专家，仅回答电力技术标准、规范、规程相关问题。
            回答必须严谨准确，优先依据参考资料内容作答；参考资料中没有答案时，明确说明「知识库中暂无相关规范内容」，禁止编造。
            尽量引用标准编号、条款名称，输出结构清晰。
            """;

        if (context.isBlank()) {
            return basePrompt;
        }

        return basePrompt + "\n\n【参考资料】\n" + context;
    }
}

