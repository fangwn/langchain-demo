package com.fang.langchaindemo.config;

import com.fang.langchaindemo.listener.CustomChatModelListener;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 *
 * @author fangwennan
 * @date 2026/3/19 22:19
 */
@Configuration
public class OllamaConfig {

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:qwen3.5:9b}")
    private String ollamaModel;

    @Value("${ollama.timeout:60}")
    private Integer timeoutSeconds;

    /**
     * 配置Ollama聊天模型
     *
     * @return ChatLanguageModel实例
     */
    @Bean(name = "ollamaChatModel")
    public ChatModel ollamaChatModel() {
        return OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(ollamaModel)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(true)
                .logResponses(true)
                .listeners(List.of(new CustomChatModelListener()))
                .build();
    }

    @Bean(name = "ollamaStreamingChatModel")
    public StreamingChatModel ollamaStreamingChatModel() {
        return OllamaStreamingChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(ollamaModel)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean(name = "ollamaEmbeddingModel")
    public OllamaEmbeddingModel ollamaEmbeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(ollamaModel)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}