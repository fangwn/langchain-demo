package com.fang.langchaindemo.service.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 *
 * @author fangwennan
 * @date 2026/3/28 14:21
 */
public interface HiringTeamWorkflow {

    @Agent("根据简历、电话面试和职位描述，该智能体将决定邀请或拒绝候选人")
    void processApplication(@V("candidateCv") String candidateCv,
                            @V("jobDescription") String jobDescription,
                            @V("hrRequirements") String hrRequirements,
                            @V("phoneInterviewNotes") String phoneInterviewNotes,
                            @V("candidateContact") String candidateContact);
}
