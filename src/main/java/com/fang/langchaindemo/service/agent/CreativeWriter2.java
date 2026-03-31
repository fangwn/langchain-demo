package com.fang.langchaindemo.service.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 *
 * @author fangwennan
 * @date 2026/3/24 18:11
 */
public interface CreativeWriter2 {

    @UserMessage("你是个创意作家。围绕给定主题，生成一篇不超过三句话的故事草稿。只返回故事，不返回其他。主题是 {{topic}}.")
    @Agent(value = "基于给定主题生成故事", outputKey = "story")
    String generateStory2(@V("topic") String topic);
}
