package com.fang.langchaindemo.config;

import com.fang.langchaindemo.listener.CustomChatModelListener;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.util.List;

/**
 *
 * @author fangwennan
 * @date 2026/3/20 16:08
 */
@Configuration
public class DeepSeekConfig {

    @Value("${deepseek.api-key:your-deepseek-api-key-here}")
    private String apiKey;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    @Value("${deepseek.timeout:60}")
    private Integer timeoutSeconds;

    @Value("${deepseek.temperature:0.7}")
    private Double temperature;

    @Value("${deepseek.max-tokens:2048}")
    private Integer maxTokens;

    /**
     * 配置DeepSeek聊天模型
     *
     * @return ChatModel实例
     */
    @Bean(name = "deepSeekChatModel")
    public ChatModel deepSeekChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(true)
                .logResponses(true)
                .listeners(List.of(new CustomChatModelListener()))
                .build();
    }

    /**
     * 配置DeepSeek流式聊天模型
     *
     * @return StreamingChatModel实例
     */
    @Bean(name = "deepSeekStreamingChatModel")
    public StreamingChatModel deepSeekStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(true)
                .logResponses(true)
                .build();

    }
}
