package com.fang.langchaindemo.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

import java.util.List;

/**
 *
 * @author fangwennan
 * @date 2026/3/20 17:28
 */
public interface Assistant {
    String chat(String userMessage);

    @UserMessage("Generate an outline for the article on the following topic: {{it}}")
    Result<List<String>> generateOutlineFor(String topic);

    TokenStream chatStream(String message);

    String chat(@MemoryId int memoryId, @UserMessage String message);
}
