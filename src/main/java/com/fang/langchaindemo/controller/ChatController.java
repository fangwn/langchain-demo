package com.fang.langchaindemo.controller;

import com.fang.langchaindemo.service.Assistant;
import com.fang.langchaindemo.tools.OrderTools;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.List;

/**
 *
 * @author fangwennan
 * @date 2026/3/19 22:23
 */
@Slf4j
@RestController
public class ChatController {

    @Resource
    @Qualifier("ollamaChatModel")
    private OllamaChatModel ollamaChatModel;

    @Resource
    @Qualifier("ollamaStreamingChatModel")
    private OllamaStreamingChatModel ollamaStreamingChatModel;

    @Resource
    @Qualifier("deepSeekChatModel")
    private OpenAiChatModel deepSeekChatModel;

    @Resource
    private OrderTools orderTools;

    @Resource
    @Qualifier("ollamaEmbeddingModel")
    private OllamaEmbeddingModel ollamaEmbeddingModel;

    EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

    /**
     * 初始化SSE客户端
     *
     * @param sseUrl SSE服务器连接地址
     * @return McpClient实例
     */
    private static McpClient initSseClient(String sseUrl) {
        // 构建默认MCP客户端
        return new DefaultMcpClient.Builder()
                .clientName("yjmyzz.cnblogs.com")
                .protocolVersion("2024-11-05")
                .toolExecutionTimeout(Duration.ofSeconds(10))
                // 配置HTTP传输层参数
                .transport(new HttpMcpTransport.Builder()
                        // 设置SSE服务器连接URL
                        .sseUrl(sseUrl)
                        // 设置连接超时时间
                        .timeout(Duration.ofSeconds(10))
                        // 启用请求日志记录
                        .logRequests(true)
                        // 启用响应日志记录
                        .logResponses(true)
                        .build())
                .build();
    }

    /**
     * 发送聊天消息（GET方式）
     *
     * @param prompt 用户输入的消息
     * @return 聊天响应
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> chat(@RequestParam String prompt) {
        log.info("收到聊天请求: {}", prompt);

        try {
            String aiResponse = ollamaChatModel.chat(prompt);
            return ResponseEntity.ok(aiResponse);

        } catch (Exception e) {
            log.error("与Ollama通信时发生错误", e);
            String errorResponse = "抱歉，处理您的请求时发生了错误: " + e.getMessage();
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * 流式聊天消息（GET方式）
     *
     * @param prompt 用户输入的消息
     * @return 流式聊天响应
     */
    @GetMapping(value = "/chat/stream", produces = "text/html;charset=utf-8")
    public Flux<String> chatStream(@RequestParam String prompt) {
        log.info("收到流式聊天请求: {}", prompt);

        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        ollamaStreamingChatModel.chat(prompt, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String s) {
                log.info("收到部分响应: {}",s);
                // 发送SSE格式的数据
                sink.tryEmitNext(s);
            }

            @Override
            public void onCompleteResponse(ChatResponse chatResponse) {
                log.info("流式响应完成");
                sink.tryEmitComplete();
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("流式响应发生错误", throwable);
                sink.tryEmitError(throwable);
            }
        });

        return sink.asFlux();
    }

    /**
     * 通过DeepSeek模型查询订单状态。
     * 该方法会构造一个包含订单状态查询意图的用户消息，并利用工具调用机制让AI决定是否需要调用订单状态查询工具。
     * 如果AI请求调用工具，则执行工具并再次与AI交互以生成最终响应。
     *
     * @param orderId 订单ID，用于查询对应订单的状态
     * @return ResponseEntity<String> 返回订单状态的文本描述或错误信息
     */
    @GetMapping(value = "/status/deepseek", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getOrderStatusWithDeepseek(@RequestParam String orderId) {

        try {
            // 注册可用的工具规范，供AI在对话中调用
            List<ToolSpecification> toolSpecifications = ToolSpecifications.toolSpecificationsFrom(OrderTools.class);

            // 构造用户提问消息
            UserMessage userMessage = UserMessage.from("订单" + orderId + "的状态是什么，请用友好的方式回答");

            // 构建第一次请求，包含用户消息和工具定义
            ChatRequest request = ChatRequest.builder()
                    .messages(userMessage)
                    .toolSpecifications(toolSpecifications)
                    .build();

            // 第一次调用LLM，获取AI响应
            ChatResponse response = deepSeekChatModel.chat(request);
            AiMessage aiMessage = response.aiMessage();

            // 检查AI是否希望调用工具，并确认是调用订单状态查询工具
            if (aiMessage.hasToolExecutionRequests() && "getOrderStatus".equalsIgnoreCase(aiMessage.toolExecutionRequests().get(0).name())) {
                // 执行订单状态查询工具
                String toolResult = orderTools.getOrderStatus(orderId);
                // 构造工具执行结果消息
                ToolExecutionResultMessage toolExecutionResultMessage = ToolExecutionResultMessage.from(aiMessage.toolExecutionRequests().get(0), toolResult);

                // 构建第二次请求，将用户消息、AI原始响应和工具执行结果一并发送给AI进行总结
                ChatRequest request2 = ChatRequest.builder()
                        .messages(List.of(userMessage, aiMessage, toolExecutionResultMessage))
                        .toolSpecifications(toolSpecifications)
                        .build();

                // 第二次调用LLM，生成最终自然语言响应
                ChatResponse response2 = deepSeekChatModel.chat(request2);
                return ResponseEntity.ok(response2.aiMessage().text());
            } else {
                log.warn("AI没有请求调用任何工具，响应内容: {}", aiMessage);
                return ResponseEntity.ok("AI没有请求调用任何工具，响应内容: " + aiMessage);
            }
        } catch (Exception e) {
            log.error("通过AI调用工具时发生错误", e);
            return ResponseEntity.ok("通过AI调用工具失败: " + e.getMessage());
        }
    }

    /**
     * 直接获取订单状态信息
     *
     * @param orderId 订单ID，用于查询指定订单的状态
     * @return ResponseEntity<String> 包含订单状态信息的响应实体，成功时返回订单状态JSON字符串，
     * 失败时返回包含错误信息的JSON字符串
     */
    @GetMapping(value = "/order", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getOrderStatusDirect(@RequestParam String orderId) {
        McpClient mcpClient = null;
        try {
            // 初始化SSE客户端连接
            mcpClient = initSseClient("http://localhost:8070/sse");

            // 构建AI助手服务，配置聊天模型和工具提供者
            Assistant assistant = AiServices.builder(Assistant.class)
                    .chatModel(deepSeekChatModel)
                    .toolProvider(McpToolProvider.builder().mcpClients(mcpClient).build())
                    .build();

            // 调用AI助手查询订单状态
            String response = assistant.chat("查询订单状态，订单号：" + orderId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("查询订单状态时发生错误", e);
            return ResponseEntity.ok("{\"error\":\"查询订单状态失败: " + e.getMessage() + "\"}");
        } finally {
            // 确保MCP客户端连接被正确关闭
            if (mcpClient != null) {
                try {
                    mcpClient.close();
                } catch (Exception e) {
                    log.error("关闭MCP客户端时发生错误", e);
                }
            }
        }
    }

    @GetMapping(value = "/embed/memory", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> embedMemory() {
        try {
            TextSegment segment1 = TextSegment.from("我喜欢打乒乓球");
            Embedding embedding1 = ollamaEmbeddingModel.embed(segment1).content();
            embeddingStore.add(embedding1, segment1);

            TextSegment segment2 = TextSegment.from("张三是个程序员");
            Embedding embedding2 = ollamaEmbeddingModel.embed(segment2).content();
            embeddingStore.add(embedding2, segment2);

            return ResponseEntity.ok("{\"code\":\"success\"}");
        } catch (Exception e) {
            log.error("embed-in-memory", e);
            return ResponseEntity.ok("{\"error\":\"embed in-memory error: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping(value = "/query/memory", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> queryInMemory(@RequestParam(required = false) String query) {
        try {
            if (!StringUtils.hasText(query)) {
                query = "我最喜欢的运动是什么?";
            }

            Embedding queryEmbedding = ollamaEmbeddingModel.embed(query).content();
            EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(1)
                    .build();
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(embeddingSearchRequest).matches();
            EmbeddingMatch<TextSegment> embeddingMatch = matches.get(0);
            return ResponseEntity.ok("{\"score\":" + embeddingMatch.score() + "\",\"text\":\"" + embeddingMatch.embedded().text() + "\"}");
        } catch (Exception e) {
            log.error("query-in-memory", e);
            return ResponseEntity.ok("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    /**
     * 基于RAG的AI聊天
     *
     * @param query
     * @return
     */
    @GetMapping(value = "/query/bot", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> bot(@RequestParam(required = false) String query) {
        try {
            if (!StringUtils.hasText(query)) {
                query = "张三的职业是什么？";
            }

            ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                    .embeddingStore(embeddingStore)
                    .embeddingModel(ollamaEmbeddingModel)
                    .maxResults(3)
                    .minScore(0.6)
                    .build();

            Assistant assistant = AiServices.builder(Assistant.class)
                    .chatModel(ollamaChatModel)
                    .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                    .contentRetriever(retriever)
                    .build();

            return ResponseEntity.ok(assistant.chat(query));
        } catch (Exception e) {
            log.error("bot", e);
            return ResponseEntity.ok("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
