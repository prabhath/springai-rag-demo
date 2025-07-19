package com.prabhath.ragdemo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.embedding.EmbeddingModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PDFIngestor {
    private final String pdfDirectory;
    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;

    public PDFIngestor(
            @Value("${pdf.ingest.dir}") String pdfDirectory,
            VectorStore vectorStore,
            EmbeddingModel embeddingModel) {
        this.pdfDirectory = pdfDirectory;
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
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
                String content = extractTextFromPDF(pdf);
                // Store in ChromaVectorStore as a document with metadata
                vectorStore.add(List.of(new org.springframework.ai.document.Document(content, java.util.Map.of("filename", pdf.getName()))));
                System.out.println("Ingested PDF: " + pdf.getName());
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
}
