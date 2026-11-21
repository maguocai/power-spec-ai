package com.power.ai.config;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author maguocai
 * @date 2026/11/18 22:15
 * @description 向量存储配置
 */
@Configuration
public class VectorStoreConfig {

    /**
     * 内存版向量存储：重启后数据丢失，适合开发测试
     * 后续升级Milvus只需要替换这个Bean的实现
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}

