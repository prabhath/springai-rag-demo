package com.prabhath.ragdemo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.ai.vectorstore.VectorStore;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.embedding.EmbeddingModel;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Component
public class PDFIngestor {
    private final String pdfDirectory;
    private final VectorStore vectorStore;

    public PDFIngestor(
            @Value("${pdf.ingest.dir}") String pdfDirectory,
            VectorStore vectorStore,
            EmbeddingModel embeddingModel) {
        this.pdfDirectory = pdfDirectory;
        this.vectorStore = vectorStore;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ingestPDFs() {
        File dir = new File(pdfDirectory);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("PDF directory does not exist: " + pdfDirectory);
            return;
        }
        File[] pdfFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));
        if (pdfFiles == null || pdfFiles.length == 0) {
            System.out.println("No PDFs found in directory: " + pdfDirectory);
            return;
        }
        for (File pdf : pdfFiles) {
            try {
                String fullText = extractTextFromPDF(pdf);
                // Split text into chunks (e.g., by paragraphs or fixed size)
                List<String> chunks = splitIntoChunks(fullText, 500); // 500 characters per chunk
                
                // Create documents for each chunk
                List<org.springframework.ai.document.Document> documents = new ArrayList<>();
                for (int i = 0; i < chunks.size(); i++) {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("filename", pdf.getName());
                    metadata.put("chunk_index", i);
                    metadata.put("total_chunks", chunks.size());
                    metadata.put("content", chunks.get(i));
                    documents.add(new org.springframework.ai.document.Document(chunks.get(i), metadata));
                }
                
                // Store all chunks in the vector store
                vectorStore.add(documents);
                System.out.println("Ingested PDF: " + pdf.getName() + " (" + chunks.size() + " chunks)");
            } catch (IOException e) {
                System.err.println("Failed to read PDF: " + pdf.getName());
            }
        }
    }

    private String extractTextFromPDF(File pdfFile) throws IOException {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
    
    /**
     * Splits text into chunks of specified size, trying to break at sentence boundaries.
     * @param text The text to split
     * @param chunkSize The target size of each chunk (in characters)
     * @return List of text chunks
     */
    private List<String> splitIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        
        // First, split by paragraphs (double newlines)
        String[] paragraphs = text.split("\\n\\s*\\n");
        
        StringBuilder currentChunk = new StringBuilder();
        for (String paragraph : paragraphs) {
            // If adding this paragraph would exceed chunk size, finalize current chunk
            if (currentChunk.length() > 0 && 
                currentChunk.length() + paragraph.length() > chunkSize) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }
            
            // Add paragraph to current chunk
            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(paragraph);
            
            // If current chunk is large enough, finalize it
            if (currentChunk.length() >= chunkSize) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }
        }
        
        // Add any remaining text as the last chunk
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }
}
