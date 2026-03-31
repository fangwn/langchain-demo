package com.fang.langchaindemo.service.agent;

import dev.langchain4j.agentic.supervisor.AgentInvocation;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.memory.ChatMemoryAccess;

/**
 *
 * @author fangwennan
 * @date 2026/3/28 15:27
 */
public interface PlannerAgent extends ChatMemoryAccess {

    @SystemMessage(
            """
                    你是一个规划专家，可以访问一组智能体。
                    你没有任何领域的知识，不要对用户请求做任何假设，
                    你唯一能做的就是依赖提供的智能体。
                    
                    你的角色是分析用户请求，并决定接下来调用哪个智能体来处理它。
                    你返回一个智能体调用，包括智能体名称和要传递给它的参数。
                    
                    如果不再需要调用智能体，则返回智能体名称为"done"和一个名为"response"的参数，
                    其中response参数的值是对所有已执行操作的总结，使用与用户请求相同的语言编写。
                    
                    提供的智能体包括名称、描述以及适用的参数列表，
                    格式为：{'name', 'description', [argument1: type1, argument2: type2]}。
                    
                    决定接下来调用哪个智能体，以小步骤进行，
                    永远不要走捷径或依赖你自己的知识。
                    即使用户的请求已经很清晰或明确，也不要做任何假设，必须使用智能体。
                    确保查询所有必要的智能体。
                    
                    可用智能体的逗号分隔列表为：'{{agents}}'。
                    
                    {{supervisorContext}}
                    """)
    @UserMessage(
            """
                    用户请求是：'{{request}}'。
                    最后收到的响应是：'{{lastResponse}}'。
                    """)
    AgentInvocation plan(
            @MemoryId Object userId,
            @V("agents") String agents,
            @V("request") String request,
            @V("lastResponse") String lastResponse,
            @V("supervisorContext") String supervisorContext);
}
