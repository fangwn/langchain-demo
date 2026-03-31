package com.fang.langchaindemo.service.mcp;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/**
 *
 * @author fangwennan
 * @date 2026/3/31 10:49
 */
public class Calculator {

    @Tool
    long add(@P("a") long a, @P("b") long b) {
        return a + b;
    }
}
