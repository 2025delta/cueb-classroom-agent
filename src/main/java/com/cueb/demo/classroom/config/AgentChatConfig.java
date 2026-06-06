package com.cueb.demo.classroom.config;

import com.cueb.demo.classroom.service.ClassroomAgentService;
import com.cueb.demo.classroom.service.ClassroomAgentServiceV2;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 分别创建 V1 和 V2 的 ChatClient Bean，使用独立 Builder 避免工具冲突。
 */
@Configuration
public class AgentChatConfig {

    @Bean
    @Qualifier("chatClientV1")
    public ChatClient chatClientV1(ChatModel chatModel, ClassroomAgentService agentService) {
        return ChatClient.builder(chatModel)
                .defaultTools(agentService)
                .build();
    }

    @Bean
    @Qualifier("chatClientV2")
    public ChatClient chatClientV2(ChatModel chatModel, ClassroomAgentServiceV2 agentServiceV2) {
        return ChatClient.builder(chatModel)
                .defaultTools(agentServiceV2)
                .build();
    }
}
