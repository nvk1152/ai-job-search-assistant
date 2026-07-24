package com.nvk.jsa.orchestrator.resume;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

/**
 * Extracts plain text from PDF or other document formats (via Apache PDFBox or Tika).
 * Output feeds into ResumeExtractionService for LLM-powered parsing.
 */
@Component
public class ResumeTextExtractor {

    public String extractText(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded résumé", e);
        }

        if (filename.endsWith(".pdf")) {
            return extractPdf(content);
        }
        return extractWithTika(content);
    }

    private String extractPdf(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse uploaded PDF résumé", e);
        }
    }

    private String extractWithTika(byte[] content) {
        try {
            BodyContentHandler handler = new BodyContentHandler(-1);
            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(new ByteArrayInputStream(content), handler, new Metadata(), new ParseContext());
            return handler.toString();
        } catch (IOException | SAXException | TikaException e) {
            throw new IllegalStateException("Failed to parse uploaded résumé", e);
        }
    }
}
