package com.fang.langchaindemo.tools;

import dev.langchain4j.agent.tool.Tool;

/**
 *
 * @author fangwennan
 * @date 2026/3/24 17:30
 */
public class Tools {

    @Tool
    int add(int a, int b) {
        System.out.println("add: " + a + " + " + b);
        return a + b;
    }

    @Tool
    int multiply(int a, int b) {
        System.out.println("multiply: " + a + " * " + b);
        return a * b;
    }
}
