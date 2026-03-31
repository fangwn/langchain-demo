package com.fang.langchaindemo.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

/**
 *
 * @author fangwennan
 * @date 2026/3/23 22:26
 */
public interface TextUtils {

    @SystemMessage("你是一名专业的{{language}}翻译")
    @UserMessage("请翻译以下文本: {{text}}")
    String translate(@V("text") String text, @V("language") String language);

    @SystemMessage("用{{n}}个要点总结用户的每条消息。只提供要点。")
    List<String> summarize(@UserMessage String text, @V("n") int n);
}