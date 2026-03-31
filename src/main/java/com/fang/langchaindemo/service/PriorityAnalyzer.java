package com.fang.langchaindemo.service;

import com.fang.langchaindemo.domain.enums.Priority;
import dev.langchain4j.service.UserMessage;

/**
 *
 * @author fangwennan
 * @date 2026/3/24 16:33
 */
public interface PriorityAnalyzer {

    @UserMessage("分析问题的优先级: {{it}}")
    Priority analyzePriority(String issueDescription);
}
