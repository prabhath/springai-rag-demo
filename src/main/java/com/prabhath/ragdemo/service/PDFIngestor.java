package com.prabhath.ragdemo.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * Splits text into chunks of specified size, trying to break at sentence boundaries and ensuring no chunk exceeds the safe size.
     * @param text The text to split
     * @param chunkSize The target size of each chunk (in characters)
     * @return List of text chunks
     */
    private List<String> splitIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        // Use a safer chunk size (tokens are less than characters)
        int safeChunkSize = Math.min(chunkSize, 200);
        // First, split by paragraphs (double newlines)
        String[] paragraphs = text.split("\\n\\s*\\n");
        for (String paragraph : paragraphs) {
            if (paragraph.length() <= safeChunkSize) {
                chunks.add(paragraph.trim());
            } else {
                // Split by sentences if paragraph is too long
                String[] sentences = paragraph.split("(?<=[.!?])\\s+");
                StringBuilder currentChunk = new StringBuilder();
                for (String sentence : sentences) {
                    if (sentence.length() > safeChunkSize) {
                        // Split long sentence into smaller parts
                        for (int i = 0; i < sentence.length(); i += safeChunkSize) {
                            int end = Math.min(i + safeChunkSize, sentence.length());
                            chunks.add(sentence.substring(i, end).trim());
                        }
                        continue;
                    }
                    if (currentChunk.length() > 0 && currentChunk.length() + sentence.length() > safeChunkSize) {
                        chunks.add(currentChunk.toString().trim());
                        currentChunk = new StringBuilder();
                    }
                    if (currentChunk.length() > 0) {
                        currentChunk.append(" ");
                    }
                    currentChunk.append(sentence);
                }
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                }
            }
        }
        // Remove empty chunks
        chunks.removeIf(String::isEmpty);
        return chunks;
    }
}
