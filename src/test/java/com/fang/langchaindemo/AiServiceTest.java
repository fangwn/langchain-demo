package com.fang.langchaindemo;

import com.fang.langchaindemo.domain.Person2;
import com.fang.langchaindemo.domain.enums.Priority;
import com.fang.langchaindemo.service.*;
import com.fang.langchaindemo.service.agent.CreativeWriter;
import com.fang.langchaindemo.service.agent.CreativeWriter2;
import com.fang.langchaindemo.tools.Tools;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 *
 * @author fangwennan
 * @date 2026/3/24 16:02
 */
@SpringBootTest
public class AiServiceTest {

    @Resource
    @Qualifier("tongYiChatModel")
    private OpenAiChatModel tongYiChatModel;

    @Resource
    @Qualifier("tongYiStreamingChatModel")
    private OpenAiStreamingChatModel tongYiStreamingChatModel;

    @Test
    public void testAiServiceSystemMessageProvider() {
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(tongYiChatModel).
                systemMessageProvider((it) -> "你是一个超级牛逼的资深Java开发工程师!").build();
        String chat = assistant.chat("帮我写一个商城程序,要能够直接运行!");
        System.out.println(chat);
    }

    @Test
    public void testAiServiceUserMessage() {
        Friend friend = AiServices.builder(Friend.class)
                .chatModel(tongYiChatModel).build();
//        String chat = friend.chat("Hello");
//        System.out.println(chat);
        String chat2 = friend.chat2("Hello");
        System.out.println(chat2);
    }

    @Test
    public void testChatRequestParameters() {
        AssistantWithChatParams assistant = AiServices.builder(AssistantWithChatParams.class)
                .chatModel(tongYiChatModel).build();
        ChatRequestParameters customParams = ChatRequestParameters.builder()
                .temperature(0.85)
                .build();
        String chat = assistant.chat("hello", customParams);
        System.out.println(chat);
    }

    @Test
    public void testReturnType() {
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(tongYiChatModel).build();
        Result<List<String>> result = assistant.generateOutlineFor("Java");

        List<String> outline = result.content();
        TokenUsage tokenUsage = result.tokenUsage();
        List<Content> sources = result.sources();
        List<ToolExecution> toolExecutions = result.toolExecutions();
        FinishReason finishReason = result.finishReason();
        System.out.println(outline);
    }

    @Test
    public void testStructuredOutput() {
        SentimentAnalyzer sentimentAnalyzer = AiServices.builder(SentimentAnalyzer.class).chatModel(tongYiChatModel).build();
        boolean positive = sentimentAnalyzer.isPositive("It's wonderful!");
        System.out.println("positive: " + positive);
    }

    @Test
    public void testStructuredOutput2() {
        PriorityAnalyzer priorityAnalyzer = AiServices.builder(PriorityAnalyzer.class)
                .chatModel(tongYiChatModel)
                .build();
        Priority priority = priorityAnalyzer.analyzePriority("主支付网关瘫痪，客户无法处理交易");
        System.out.println("priority: " + priority);
    }

    @Test
    public void testStructuredOutput3() {
        PersonExtractor2 extractor = AiServices.builder(PersonExtractor2.class)
                .chatModel(tongYiChatModel)
                .build();
        String text = """
            In 1968, amidst the fading echoes of Independence Day,
            a child named John arrived under the calm evening sky.
            This newborn, bearing the surname Doe, marked the start of a new journey.
            He was welcomed into the world at 345 Whispering Pines Avenue
            a quaint street nestled in the heart of Springfield
            an abode that echoed with the gentle hum of suburban dreams and aspirations.
            """;
        Person2 person = extractor.extractPersonFrom(text);
        System.out.println("person: " + person);
    }

    @Test
    public void testStream() {
        Assistant assistant = AiServices.builder(Assistant.class)
                .streamingChatModel(tongYiStreamingChatModel)
                .build();
        TokenStream tokenStream = assistant.chatStream("请用中文回答我的问题：如何用java实现一个简单的聊天机器人?");
        CompletableFuture<ChatResponse> futureResponse = new CompletableFuture<>();

        tokenStream
                .onPartialResponse(System.out::println)
                .onRetrieved(System.out::println)
                .onToolExecuted(System.out::println)
                .onCompleteResponse(futureResponse::complete)
                .onError(futureResponse::completeExceptionally)
                .start();

        futureResponse.join();
    }

    @Test
    public void testChatMemory() {
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(tongYiChatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
        String chat = assistant.chat("hello");
        System.out.println(chat);
    }

    @Test
    public void testChatMemory2() {
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(tongYiChatModel)
                .chatMemoryProvider((memoryId -> MessageWindowChatMemory.withMaxMessages(10)))
                .build();
        String answerToKlaus = assistant.chat(1, "Hello, my name is Klaus");
        String answerToFrancine = assistant.chat(2, "Hello, my name is Francine");
        System.out.println(answerToKlaus);
        System.out.println(answerToFrancine);
    }

    @Test
    public void testTools() {
        ToolAssistantService assistant = AiServices.builder(ToolAssistantService.class)
                .chatModel(tongYiChatModel)
                .tools(new Tools())
                .build();
        String chat = assistant.chat("计算1+1和1*2");
        System.out.println(chat);
    }
}
