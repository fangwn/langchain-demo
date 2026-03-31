package com.fang.langchaindemo.mcp;

import com.fang.langchaindemo.service.mcp.Calculator;
import dev.langchain4j.community.mcp.server.McpServer;
import dev.langchain4j.community.mcp.server.transport.StdioMcpServerTransport;
import dev.langchain4j.mcp.protocol.McpImplementation;

import java.util.List;

/**
 *
 * @author fangwennan
 * @date 2026/3/31 10:50
 */
public class McpServerMain {

    public static void main(String[] args) throws InterruptedException {
        McpImplementation serverInfo = new McpImplementation();
        serverInfo.setName("my-java-mcp-server");
        serverInfo.setVersion("1.0.0");
        McpServer server = new McpServer(List.of(new Calculator()), serverInfo);
        new StdioMcpServerTransport(System.in, System.out, server);

        Thread.currentThread().join();
    }
}
