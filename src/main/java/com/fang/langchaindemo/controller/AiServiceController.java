package com.fang.langchaindemo.controller;

import com.fang.langchaindemo.domain.Poem;
import com.fang.langchaindemo.service.ChineseStreamTeacher;
import com.fang.langchaindemo.service.ChineseTeacher;
import com.fang.langchaindemo.service.PoemExtractor;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author fangwennan
 * @date 2026/3/20 18:11
 */
@Slf4j
@RestController
public class AiServiceController {

    @Resource
    @Qualifier("tongYiChatModel")
    private OpenAiChatModel tongYiChatModel;

    /**
     * 演示AIService基本用法
     * by 菩提树下的杨过(yjmyzz.cnblogs.com)
     *
     * @param query
     * @return
     */
    @GetMapping(value = "/aiservice/1", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> demo1(@RequestParam(defaultValue = "请问李清照最广为流传的词是哪一首,请给出这首词全文？") String query) {
        try {
            ChineseTeacher teacher = AiServices.builder(ChineseTeacher.class)
                    .chatModel(tongYiChatModel)
                    .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                    .build();
            return ResponseEntity.ok(teacher.chat(query));
        } catch (Exception e) {
            return ResponseEntity.ok("{\"error\":\"chatChain error: " + e.getMessage() + "\"}");
        }
    }

    /**
     * 演示AIService基本用法+结构化返回
     *
     * @param query
     * @return
     */
    @GetMapping(value = "/aiservice/2", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Poem> demo2(@RequestParam(defaultValue = """
            请问李清照最广为流传的词是哪一首,
            请给出这首词全文（以json格式输出，类似{\"author\":\"...\",\"title\":\"...\",\"content\":\"...\"}）？""") String query) {
        try {
            Poem extract = AiServices.builder(PoemExtractor.class)
                    .chatModel(tongYiChatModel).build()
                    .extract(AiServices.builder(ChineseTeacher.class)
                            .chatModel(tongYiChatModel)
                            .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                            .build().chat(query));
            return ResponseEntity.ok(extract);
        } catch (Exception e) {
            return ResponseEntity.ok(new Poem("error", "error", e.getMessage()));
        }
    }

    /**
     * 演示AIService基本用法+流式返回
     *
     * @param query
     * @return
     */
//    @GetMapping(value = "/aiservice/3", produces = "text/html;charset=utf-8")
//    public Flux<String> demo3(@RequestParam(defaultValue = "请问李清照最广为流传的词是哪一首,请给出这首词全文？") String query) {
//        ChineseStreamTeacher teacher = AiServices.builder(ChineseStreamTeacher.class)
//                .streamingChatModel(streamingChatModel)
//                .build();
//        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
//        teacher.chat(query)
//                .onPartialResponse((String s) -> sink.tryEmitNext(s))
//                .onCompleteResponse((ChatResponse response) -> sink.tryEmitComplete())
//                .onError(sink::tryEmitError)
//                .start();
//        return sink.asFlux();
//    }
}