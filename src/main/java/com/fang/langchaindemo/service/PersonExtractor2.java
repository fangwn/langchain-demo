package com.fang.langchaindemo.service;

import com.fang.langchaindemo.domain.Person2;
import dev.langchain4j.service.UserMessage;

/**
 *
 * @author fangwennan
 * @date 2026/3/24 16:38
 */
public interface PersonExtractor2 {

    @UserMessage("从中提取信息 {{it}}")
    Person2 extractPersonFrom(String text);
}
