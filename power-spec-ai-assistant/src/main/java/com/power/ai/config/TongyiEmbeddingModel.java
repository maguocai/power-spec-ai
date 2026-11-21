package com.power.ai.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * @author maguocai
 * @date 2026/11/19 21:42
 * @description: 通义千问兼容模式嵌入模型
 */
@Component
@Primary
public class TongyiEmbeddingModel implements EmbeddingModel {

    // 完整绝对路径，无路径拼接问题，彻底避免404
    private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings";
    // 通义 text-embedding-v2 固定1536维，硬编码更稳定
    private static final int EMBEDDING_DIMENSION = 1536;

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String modelName;

    public TongyiEmbeddingModel(
            @Value("${tongyi.embedding.api-key}") String apiKey,
            @Value("${tongyi.embedding.model:text-embedding-v2}") String modelName) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    /**
     * 核心底层方法：向量库add/检索最终都会调用到这里
     */
    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", modelName);
        body.put("input", request.getInstructions());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(API_URL, entity, Map.class);
        Map<String, Object> responseBody = response.getBody();

        List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
        List<Embedding> embeddings = data.stream()
                .map(item -> {
                    int index = (Integer) item.get("index");
                    List<Double> embeddingList = (List<Double>) item.get("embedding");
                    float[] embeddingArray = new float[embeddingList.size()];
                    for (int i = 0; i < embeddingList.size(); i++) {
                        embeddingArray[i] = embeddingList.get(i).floatValue();
                    }
                    return new Embedding(embeddingArray, index);
                })
                .collect(Collectors.toList());

        return new EmbeddingResponse(embeddings);
    }

    /**
     * 单文本向量化
     */
    @Override
    public float[] embed(String text) {
        EmbeddingRequest request = new EmbeddingRequest(List.of(text), null);
        return call(request).getResult().getOutput();
    }

    /**
     * 单文档向量化（向量库内部调用）
     */
    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    /**
     * 批量文本向量化
     */
    @Override
    public List<float[]> embed(List<String> texts) {
        EmbeddingRequest request = new EmbeddingRequest(texts, null);
        return call(request).getResults().stream()
                .map(Embedding::getOutput)
                .collect(Collectors.toList());
    }

    /**
     * 返回向量维度
     */
    @Override
    public int dimensions() {
        return EMBEDDING_DIMENSION;
    }
}



