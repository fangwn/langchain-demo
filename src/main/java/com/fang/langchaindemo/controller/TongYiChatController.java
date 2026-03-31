package com.fang.langchaindemo.controller;

import com.fang.langchaindemo.domain.enums.IssueCategory;
import com.fang.langchaindemo.domain.enums.Sentiment;
import com.fang.langchaindemo.service.*;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.internal.Json;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static dev.langchain4j.data.message.UserMessage.userMessage;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

/**
 *
 * @author fangwennan
 * @date 2026/3/23 11:44
 */
@Slf4j
@RestController
@RequestMapping("/tongyi")
public class TongYiChatController {

    @Resource
    @Qualifier("tongYiChatModel")
    private OpenAiChatModel tongYiChatModel;

    @Resource
    @Qualifier("tongYiStreamingChatModel")
    private OpenAiStreamingChatModel tongYiStreamingChatModel;

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
            String aiResponse = tongYiChatModel.chat(prompt);
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

        tongYiStreamingChatModel.chat(prompt, new StreamingChatResponseHandler() {
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

    @GetMapping(value = "/prompt/chat", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> promptChat(@RequestParam String message) {
        String template = "帮我写一首诗,按照{{name}}的风格!";
        PromptTemplate promptTemplate = PromptTemplate.from(template);
        Map<String, Object> variables = Map.of("name", "李白");
        Prompt prompt = promptTemplate.apply(variables);
        String response = tongYiChatModel.chat(prompt.text());
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/memory", produces = "text/html;charset=utf-8")
    public ResponseEntity<String> memory() throws ExecutionException, InterruptedException {
        ChatMemory chatMemory = TokenWindowChatMemory.withMaxTokens(10000, new OpenAiTokenCountEstimator(GPT_4_O_MINI));
        SystemMessage systemMessage = SystemMessage.from("你是一名资深Java开发工程师");
        chatMemory.add(systemMessage);
        UserMessage userMessage1 = userMessage("我如何优化大规模电子商务平台的数据库查询?回答简短，最多三到五行.");
        chatMemory.add(userMessage1);
        System.out.println("[User]: " + userMessage1.singleText());
        System.out.print("[LLM]: ");
        AiMessage aiMessage1 = streamChat(tongYiStreamingChatModel, chatMemory);
        chatMemory.add(aiMessage1);
        UserMessage userMessage2 = userMessage("能举个具体的例子来实现第一个观点吗?简短，最多10行代码.");
        chatMemory.add(userMessage2);

        System.out.println("\n\n[User]: " + userMessage2.singleText());
        System.out.print("[LLM]: ");

        AiMessage aiMessage2 = streamChat(tongYiStreamingChatModel, chatMemory);
        chatMemory.add(aiMessage2);
        return ResponseEntity.ok(Json.toJson(chatMemory.messages()));
    }

    private static AiMessage streamChat(OpenAiStreamingChatModel model, ChatMemory chatMemory)
            throws ExecutionException, InterruptedException {

        CompletableFuture<AiMessage> futureAiMessage = new CompletableFuture<>();

        StreamingChatResponseHandler handler = new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.print(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                futureAiMessage.complete(completeResponse.aiMessage());
            }

            @Override
            public void onError(Throwable throwable) {
            }
        };

        model.chat(chatMemory.messages(), handler);
        return futureAiMessage.get();
    }

    @GetMapping(value = "/few-shot", produces = MediaType.TEXT_PLAIN_VALUE)
    public void fewShot() {
        List<ChatMessage> fewShotHistory = new ArrayList<>();
        // Adding positive feedback example to history
        fewShotHistory.add(UserMessage.from(
                "I love the new update! The interface is very user-friendly and the new features are amazing!"));
        fewShotHistory.add(AiMessage.from(
                "Action: forward input to positive feedback storage\nReply: Thank you very much for this great feedback! We have transmitted your message to our product development team who will surely be very happy to hear this. We hope you continue enjoying using our product."));

        // Adding negative feedback example to history
        fewShotHistory.add(UserMessage
                .from("I am facing frequent crashes after the new update on my Android device."));
        fewShotHistory.add(AiMessage.from(
                "Action: open new ticket - crash after update Android\nReply: We are so sorry to hear about the issues you are facing. We have reported the problem to our development team and will make sure this issue is addressed as fast as possible. We will send you an email when the fix is done, and we are always at your service for any further assistance you may need."));

        // Adding another positive feedback example to history
        fewShotHistory.add(UserMessage
                .from("Your app has made my daily tasks so much easier! Kudos to the team!"));
        fewShotHistory.add(AiMessage.from(
                "Action: forward input to positive feedback storage\nReply: Thank you so much for your kind words! We are thrilled to hear that our app is making your daily tasks easier. Your feedback has been shared with our team. We hope you continue to enjoy using our app!"));

        // Adding another negative feedback example to history
        fewShotHistory.add(UserMessage
                .from("The new feature is not working as expected. It’s causing data loss."));
        fewShotHistory.add(AiMessage.from(
                "Action: open new ticket - data loss by new feature\nReply:We apologize for the inconvenience caused. Your feedback is crucial to us, and we have reported this issue to our technical team. They are working on it on priority. We will keep you updated on the progress and notify you once the issue is resolved. Thank you for your patience and support."));

        // Adding real user's message
        UserMessage customerComplaint = UserMessage
                .from("How can your app be so slow? Please do something about it!");
        fewShotHistory.add(customerComplaint);

        System.out.println("[User]: " + customerComplaint.singleText());
        System.out.print("[LLM]: ");
        CompletableFuture<ChatResponse> futureChatResponse = new CompletableFuture<>();

        tongYiStreamingChatModel.chat(fewShotHistory, new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.print(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                futureChatResponse.complete(completeResponse);
            }

            @Override
            public void onError(Throwable error) {
                futureChatResponse.completeExceptionally(error);
            }
        });

        futureChatResponse.join();
    }

    @GetMapping(value = "/aiService", produces = "text/html;charset=utf-8")
    public ResponseEntity<String> aiService() {
        Assistant assistant = AiServices.create(Assistant.class, tongYiChatModel);
        String userMessage = "翻译“证券、股票及类似收益处分所得的资本利得";
        String answer = assistant.chat(userMessage);
        return ResponseEntity.ok(answer);
    }

    @GetMapping(value = "/aiService2", produces = "text/html;charset=utf-8")
    public ResponseEntity<String> aiService2() {
        Chef chef = AiServices.create(Chef.class, tongYiChatModel);
        String answer = chef.answer("鸡肉应该烤多久?");
        return ResponseEntity.ok(answer);
    }

    @GetMapping(value = "/aiService3", produces = "text/html;charset=utf-8")
    public ResponseEntity<Object> aiService3() {
        TextUtils utils = AiServices.create(TextUtils.class, tongYiChatModel);
        String transaction = utils.translate("Hello, how are you?", "Chinese");
        System.out.println(transaction);
        String text = "人工智能，或称人工智能，是计算机科学的一个分支，旨在创造"
                + "或者模仿人类智能的机器。这可以是简单的任务，比如识别"
                + "模式或语言能力，甚至更复杂的任务，比如做决策或预测。";
        List<String> bulletPoints = utils.summarize(text, 3);
        bulletPoints.forEach(System.out::println);
        return ResponseEntity.ok(Json.toJson(bulletPoints));
    }

    @GetMapping(value = "/aiService4", produces = "text/html;charset=utf-8")
    public ResponseEntity<String> aiService4() {
        SentimentAnalyzer sentimentAnalyzer = AiServices.create(SentimentAnalyzer.class, tongYiChatModel);
        Sentiment sentiment = sentimentAnalyzer.analyzeSentimentOf("很好哦!");
        boolean positive = sentimentAnalyzer.isPositive("不好呀!");
        System.out.println("positive: " + positive);
        return ResponseEntity.ok(sentiment.toString());
    }

    @GetMapping(value = "/aiService5", produces = "text/html;charset=utf-8")
    public ResponseEntity<String> aiService5() {
        HotelReviewIssueAnalyzer hotelReviewIssueAnalyzer = AiServices.create(HotelReviewIssueAnalyzer.class, tongYiChatModel);
        String review = "我们在酒店的住宿体验是复杂的。地点非常完美，距离海滩只有咫尺之遥“+”，这让我们的日常外出非常方便。房间宽敞且装饰精美，“+”营造出舒适宜人的环境。然而，我们在“+”停留期间遇到了一些问题。我们房间的空调不正常，导致夜晚非常不舒服。“ + ”此外，客房服务很慢，我们不得不多次打电话去拿额外的毛巾。尽管“+”友好的员工和愉快的自助早餐，这些问题却严重影响了我们的住宿。";
        List<IssueCategory> issueCategories = hotelReviewIssueAnalyzer.analyzeReview(review);
        return ResponseEntity.ok(issueCategories.toString());
    }
}
