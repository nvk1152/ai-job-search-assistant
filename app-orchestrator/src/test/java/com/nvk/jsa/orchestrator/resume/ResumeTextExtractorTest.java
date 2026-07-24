package com.nvk.jsa.orchestrator.resume;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class ResumeTextExtractorTest {

    private final ResumeTextExtractor extractor = new ResumeTextExtractor();

    @Test
    void extractsTextFromNonPdfViaTika() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.txt", "text/plain", "John Doe\nSoftware Engineer".getBytes());

        String text = extractor.extractText(file);

        assertThat(text).contains("Software Engineer");
    }

    @Test
    void wrapsIoExceptionFromReadingUploadedBytes() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("resume.txt");
        when(file.getBytes()).thenThrow(new IOException("disk error"));

        assertThatThrownBy(() -> extractor.extractText(file))
                .isInstanceOf(IllegalStateException.class);
    }
}
