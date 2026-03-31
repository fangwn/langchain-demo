package com.fang.langchaindemo.mcp;

import com.fang.langchaindemo.service.Assistant;
import dev.langchain4j.community.mcp.server.transport.StdioMcpServerTransport;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 *
 * @author fangwennan
 * @date 2026/3/31 11:12
 */
@SpringBootTest
public class McpTest {

    @Resource
    @Qualifier("tongYiChatModel")
    private OpenAiChatModel tongYiChatModel;

    @Test
    public void test() {
        StdioMcpTransport transport = StdioMcpTransport.builder()
                .command(List.of("/usr/bin/npm", "exec", "@modelcontextprotocol/server-filesystem"))
                .logEvents(true)
                .build();

        McpClient mcpClient = DefaultMcpClient.builder()
                .transport(transport)
                .build();

        ToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(mcpClient)
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .toolProvider(toolProvider)
                .chatModel(tongYiChatModel)
                .build();

        String result = assistant.chat("列出当前目录下的文件");
    }
}
