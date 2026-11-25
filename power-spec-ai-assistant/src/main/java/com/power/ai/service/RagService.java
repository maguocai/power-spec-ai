package com.power.ai.service;

import com.power.ai.util.DocumentParserUtil;
import lombok.AllArgsConstructor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author maguocai
 * @date 2026/11/17 22:23
 * @description: RagService类
 */
@Service
@AllArgsConstructor
public class RagService {

    private final VectorStore vectorStore;
    private final ChatModel chatModel; // 新增注入，复用现有聊天模型
    private final TokenTextSplitter textSplitter = new TokenTextSplitter(500, 200, 5, 10000, true);

    // ========== 核心：自动分类Prompt ==========
    private static final String CLASSIFY_SYSTEM_PROMPT = """
        你是电力行业标准分类专家，严格按照给定类目对文本进行分类。
        
        可选分类仅有以下4项：
        1. 储能技术监督
        2. 网源协调技术
        3. 电网安全规程
        4. 通用技术规范
        
        分类规则：
        - 只能从以上4个分类中选择1个最匹配的
        - 直接输出分类名称，禁止输出任何解释、标点、序号、多余文字
        - 无法明确判断时，统一输出「通用技术规范」
        """;
    /**
     * 通用文本分类方法：输入文本，返回分类名称
     */
    private String autoClassify(String text) {
        try {
            // 只取前2000字符，足够判断分类，节省token
            String shortText = text.length() > 2000 ? text.substring(0, 2000) : text;

            List<org.springframework.ai.chat.messages.Message> messages = List.of(
                    new SystemMessage(CLASSIFY_SYSTEM_PROMPT),
                    new UserMessage(shortText)
            );

            ChatResponse response = chatModel.call(new Prompt(
                    messages,
                    org.springframework.ai.openai.OpenAiChatOptions.builder()
                            .temperature(0.0) // 温度设为0，分类结果稳定
                            .maxTokens(20)
                            .build()
            ));

            String result = response.getResult().getOutput().getText().trim();
            // 简单校验，防止AI输出异常内容
            return switch (result) {
                case "储能技术监督", "网源协调技术", "电网安全规程" -> result;
                default -> "通用技术规范";
            };
        } catch (Exception e) {
            // 分类失败兜底，走通用类
            return "通用技术规范";
        }
    }
    /**
     * 上传文档：自动分类 + 入库
     */
    public void addDocumentToKnowledgeBase(MultipartFile file) throws Exception {
        // 1. 提取纯文本
        String content = DocumentParserUtil.parse(file);

        // 2. 文本为空直接抛出提示，不进向量化
        if (content.isBlank()) {
            throw new IllegalArgumentException("文档未提取到有效文");
        }

        // 3. 文本切片
        Document rawDoc = new Document(content);
        rawDoc.getMetadata().put("fileName", file.getOriginalFilename());
        rawDoc.getMetadata().put("specType", autoClassify(content));
        List<Document> chunks = textSplitter.apply(List.of(rawDoc));

        // 4. 切片后再次校验
        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException("文档内容过短，无法生成有效知识片段");
        }

        // 5. 向量化入库
        vectorStore.add(chunks);
    }


    /**
     * 根据问题自动分类后，检索相关上下文
     */
    public String getRelevantContextByAutoClassify(String question, int topK) {
        // 1. 先对问题进行分类
        String specType = autoClassify(question);

        // 2. 按分类过滤检索向量库
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder().query(question)
                        .topK(topK).similarityThreshold(0.7).filterExpression("specType == '" + specType + "'")// 元数据过滤：只匹配对应分类的片段
                        .build()
        );

        // 3. 兜底：分类下无结果，走全库检索
        if (docs.isEmpty()) {
            docs = vectorStore.similaritySearch(
                    SearchRequest.builder().query(question)
                            .topK(topK).similarityThreshold(0.7).build()
            );
        }

        if (docs.isEmpty()) {
            return "";
        }

        return docs.stream()
                .map(doc -> "【来源文件：" + doc.getMetadata().get("fileName")
                        + " | 分类：" + doc.getMetadata().get("specType") + "】\n"
                        + doc.getText())
                .collect(Collectors.joining("\n\n----------------\n\n"));
    }


    /**
     * 根据问题检索最相关的上下文片段（默认取Top3）
     */
    public String getRelevantContext(String question, int topK) {
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder().query(question).topK(topK).similarityThreshold(0.7).build()// 相似度阈值，低于0.7的片段丢弃
        );

        if (docs.isEmpty()) {
            return "";
        }

        // 把片段拼接成上下文，附带来源
        return docs.stream()
                .map(doc -> "【来源文件：" + doc.getMetadata().get("fileName") + "】\n" + doc.getText())
                .collect(Collectors.joining("\n\n----------------\n\n"));
    }
}
