package com.fang.langchaindemo.service;

import com.fang.langchaindemo.domain.enums.Sentiment;
import dev.langchain4j.service.UserMessage;

/**
 *
 * @author fangwennan
 * @date 2026/3/23 22:37
 */
public interface SentimentAnalyzer {

    @UserMessage("分析{{it}}的情感")
    Sentiment analyzeSentimentOf(String text);

    @UserMessage("{{it}}有积极的评价嘛?")
    boolean isPositive(String text);
}
