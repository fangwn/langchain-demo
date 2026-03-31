package com.fang.langchaindemo.service.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 *
 * @author fangwennan
 * @date 2026/3/28 14:20
 */
public interface CandidateWorkflow {

    @Agent("根据个人履历和职位描述生成主简历，通过反馈循环针对职位描述进行定制，直至达到合格分数")
    String processCandidate(@V("lifeStory") String userInfo, @V("jobDescription") String jobDescription);
}
