package com.fang.langchaindemo.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 *
 * @author fangwennan
 * @date 2026/3/23 10:06
 */
public interface ChineseStreamTeacher {

    @SystemMessage("你是一名小学语文老师")
    @UserMessage("请用中文回答我的问题：{{it}}")
    String chat(String query);
}
