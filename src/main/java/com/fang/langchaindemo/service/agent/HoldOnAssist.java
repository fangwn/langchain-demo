package com.fang.langchaindemo.service.agent;

import dev.langchain4j.agentic.Agent;

/**
 *
 * @author fangwennan
 * @date 2026/3/30 10:48
 */
public class HoldOnAssist {

    @Agent(description = "招聘流程暂缓")
    public void abort() {
        System.out.println("招聘流程暂缓");
    }
}
