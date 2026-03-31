package com.fang.langchaindemo.service;

import dev.langchain4j.service.SystemMessage;

/**
 *
 * @author fangwennan
 * @date 2026/3/23 22:24
 */
public interface Chef {

    @SystemMessage("你是专业厨师。你友好、有礼貌且简洁.")
    String answer(String question);
}
