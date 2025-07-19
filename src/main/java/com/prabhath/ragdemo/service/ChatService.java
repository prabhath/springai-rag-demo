package com.prabhath.ragdemo.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ChatService {
    private final ChatModel chatModel;
    private final org.springframework.ai.vectorstore.VectorStore vectorStore;

    public ChatService(@Qualifier("openAiChatModel") ChatModel chatModel,
                      org.springframework.ai.vectorstore.VectorStore vectorStore) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
    }

    public String chat(String message) {
        // Retrieve relevant context from vector store
        List<Document> similarDocuments = vectorStore.similaritySearch(message);
        if (similarDocuments == null) {
            similarDocuments = Collections.emptyList();
        }

        var results = similarDocuments.stream().limit(3).toList(); // top 3 relevant docs
        StringBuilder contextBuilder = new StringBuilder();
        for (Document doc : results) {
            Object text = doc.getMetadata().get("content");
            if (text == null) {
                text = doc.toString();
            }
            contextBuilder.append(text).append("\n---\n");
        }
        String context = contextBuilder.toString();
        String augmentedPrompt = context + "\nUser: " + message;
        return chatModel.call(augmentedPrompt);
    }
}
