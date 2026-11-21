package com.power.ai.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author maguocai
 * @date 2026/11/18 23:24
 * @description Spring AI configuration
 */
@Configuration
public class SpringAiConfig {

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model}")
    private String chatModelName;

//    @Value("${spring.ai.openai.embedding.options.model}")
//    private String embeddingModelName;
//
//    @Value("${embeddings.base_url}")
//    private String embeddingsBaseUrl;
//
//    @Value("${embeddings.api-key}")
//    private String embeddingsApiKey;

    /**
     * 聊天模型（手动注册，稳定可控）
     */
    @Bean
    public ChatModel chatModel() {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(new SimpleApiKey(apiKey))  // 用 SimpleApiKey 包装
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(chatModelName)
                        .temperature(0.2)
                        .maxTokens(2048)
                        .build())
                .build();
    }

    /**
     * 嵌入模型（手动注册，彻底解决路径/模型兼容问题）
     */
//    @Bean
//    public EmbeddingModel embeddingModel() {
//        OpenAiApi openAiApi = OpenAiApi.builder()
//                .baseUrl(embeddingsBaseUrl)
//                .apiKey(embeddingsApiKey)
//                .build();
//
//        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
//                .model(embeddingModelName)
//                .build();
//
//        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options);
//    }
}
