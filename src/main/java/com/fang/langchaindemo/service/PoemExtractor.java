package com.fang.langchaindemo.service;

import com.fang.langchaindemo.domain.Poem;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 *
 * @author fangwennan
 * @date 2026/3/23 09:39
 */
public interface PoemExtractor {

    @UserMessage("请从以下内容中提取出诗歌内容：{{query}}")
    Poem extract(@V("query") String query);
}
