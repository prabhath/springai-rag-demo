package com.prabhath.ragdemo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class EmployeeQueries {

    private final ChatClient chatClient;

    EmployeeQueries(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    String query(String question) {
        return chatClient
                .prompt()
                .user(question)
                .call()
                .content();
    }

}
