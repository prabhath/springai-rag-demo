package com.prabhath.ragdemo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RAGService {
    private static final Logger logger = LoggerFactory.getLogger(RAGService.class);
    
    private final ChatModel chatModel;
    private final org.springframework.ai.vectorstore.VectorStore vectorStore;


    public RAGService(ChatModel chatModel,
                      org.springframework.ai.vectorstore.VectorStore vectorStore) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;

    }

    @Tool(name = "employee_data",
            description = "Answer about an employee personal information. Input: user question string.")
    public String doRag(String message) {
        logger.info("Running RAG for question: {}", message);
        // Retrieve relevant chunks from vector store
        List<Document> relevantChunks = vectorStore.similaritySearch(message);
        if (relevantChunks == null || relevantChunks.isEmpty()) {
            return "I couldn't find any relevant information to answer your question.";
        }

        // Build context from the most relevant chunks
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Relevant information from documents:\n\n");
        
        // Keep track of sources to avoid duplicates
        Set<String> usedSources = new HashSet<>();
        int chunkCount = 0;
        final int MAX_CHUNKS = 5; // Limit number of chunks to avoid context overflow
        
        for (Document chunk : relevantChunks) {
            if (chunkCount >= MAX_CHUNKS) break;
            
            // Get chunk content and metadata
            String content = chunk.getText();
            if (content == null) {
                continue; // Skip chunks with null content
            }
            Map<String, Object> metadata = chunk.getMetadata();
            String source = (String) metadata.getOrDefault("filename", "unknown source");
            
            // Skip if we've already used this exact chunk
            String chunkId = source + "_" + metadata.getOrDefault("chunk_index", "");
            if (usedSources.contains(chunkId)) continue;
            
            // Add chunk to context
            contextBuilder.append("---\n")
                        .append("Source: ").append(source).append("\n")
                        .append("Content: ").append(content.trim())
                        .append("\n\n");
            
            usedSources.add(chunkId);
            chunkCount++;
        }
        
        // Build the final prompt
        String context = contextBuilder.toString();
        String systemPrompt = "You are a helpful assistant that answers questions based on the provided context. " +
                            "If the answer cannot be found in the context, say so.\n\n" +
                            "Context:\n" + context + "\n" +
                            "---\n" +
                            "Question: " + message + "\n";

        logger.info("Sending prompt to chat model: {} ", systemPrompt);
        String response = chatModel.call(systemPrompt);
        logger.info("Response: {}", response);
        return response;
    }


}
