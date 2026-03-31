package com.fang.langchaindemo.service.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.service.V;

/**
 *
 * @author fangwennan
 * @date 2026/3/28 15:25
 */
public interface HiringSupervisor {

    @Agent("顶级招聘主管，协调候选人评估和决策流程")
    ResultWithAgenticScope<String> invoke(@V("request") String request, @V("supervisorContext") String supervisorContext);
}
