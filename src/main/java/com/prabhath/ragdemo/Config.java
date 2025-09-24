package com.prabhath.ragdemo;

import com.prabhath.ragdemo.service.RAGService;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class Config {
    @Bean
    ChatClient chatClient(List<McpSyncClient> mcpSyncClients, ChatClient.Builder builder, RAGService RAGService) {
        // Reverted: using deprecated List-based constructor as requested
        return builder
                .defaultToolCallbacks(SyncMcpToolCallbackProvider.builder().mcpClients(mcpSyncClients).build())
                .defaultTools(RAGService)
                .defaultSystem("abbreviate employee last names with the first letter of last name")
                .build();
    }

}
