package com.fang.langchaindemo.service;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 *
 * @author fangwennan
 * @date 2026/3/24 16:11
 */
public interface Friend {

    @UserMessage("你是我的好朋友。用俚语回答. {{it}}")
    String chat(String userMessage);

    @UserMessage("你是我的好朋友。用俚语回答. {{message}}")
    String chat2(@V("message") String userMessage);
}
