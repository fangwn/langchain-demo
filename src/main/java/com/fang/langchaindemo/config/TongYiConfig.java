package com.fang.langchaindemo.config;

import com.fang.langchaindemo.listener.CustomChatModelListener;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 *
 * @author fangwennan
 * @date 2026/3/23 11:16
 */
@Configuration
public class TongYiConfig {

    @Value("${aliyun.tongyi.base-url}")
    private String baseUrl;

    @Value("${aliyun.tongyi.api-key}")
    private String apiKey;

    @Value("${aliyun.tongyi.model}")
    private String model;

    @Value("${aliyun.tongyi.embeddingModel}")
    private String embeddingModel;

    @Value("${aliyun.tongyi.timeout}")
    private Integer timeoutSeconds;

    @Value("${aliyun.tongyi.temperature}")
    private Double temperature;

    @Value("${aliyun.tongyi.max-tokens}")
    private Integer maxTokens;


    @Bean(name = "tongYiChatModel")
    public ChatModel tongYiChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(false)
                .logResponses(false)
                .listeners(List.of(new CustomChatModelListener()))
                .build();
    }

    /**
     * 配置DeepSeek流式聊天模型
     *
     * @return StreamingChatModel实例
     */
    @Bean(name = "tongYiStreamingChatModel")
    public StreamingChatModel tongYiStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    @Bean(name = "tongYiEmbeddingModel")
    public OpenAiEmbeddingModel tongYiEmbeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(embeddingModel)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
