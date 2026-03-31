package com.fang.langchaindemo.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 *
 * @author fangwennan
 * @date 2026/3/20 17:04
 */
@Component
public class OrderTools {

    @Tool("查询订单状态")
    public String getOrderStatus(@P("订单ID") String orderId) {
        // 模拟查询订单状态的逻辑
        return "Order: " + orderId + " is in processing";
    }
}
