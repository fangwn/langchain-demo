package com.fang.langchaindemo.service;

import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.service.UserMessage;

/**
 *
 * @author fangwennan
 * @date 2026/3/24 16:19
 */
public interface AssistantWithChatParams {

    String chat(@UserMessage String userMessage, ChatRequestParameters params);
}
