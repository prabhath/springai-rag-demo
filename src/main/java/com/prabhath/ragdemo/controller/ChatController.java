package com.prabhath.ragdemo.controller;

import com.prabhath.ragdemo.service.RAGService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final RAGService RAGService;
    private final ChatClient chatClient;

    public ChatController(RAGService RAGService, ChatClient chatClient) {
        this.RAGService = RAGService;
        this.chatClient = chatClient;
    }

    @GetMapping("/chat_with_rag_agent")
    public String chat(@RequestParam("message") String message) {
        return RAGService.doRag(message);
    }

    @GetMapping("/chat_with_hr_agent")
    String inquire(@RequestParam("message") String message) {
        return chatClient
                .prompt()
                .user(message)
                .call()
                .content();
    }


}