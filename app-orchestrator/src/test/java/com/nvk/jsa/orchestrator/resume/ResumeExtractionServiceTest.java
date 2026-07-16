package com.nvk.jsa.orchestrator.resume;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nvk.jsa.orchestrator.llm.LlmTask;
import com.nvk.jsa.orchestrator.llm.ModelRouter;
import com.nvk.jsa.orchestrator.profile.ProfileView;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

class ResumeExtractionServiceTest {

    private final ResumeTextExtractor textExtractor = mock(ResumeTextExtractor.class);
    private final ModelRouter modelRouter = mock(ModelRouter.class);
    private final ResumeExtractionService service = new ResumeExtractionService(textExtractor, modelRouter);

    @Test
    void mapsExtractedJsonIntoProfileViewWithoutInventingData() {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[0]);
        when(textExtractor.extractText(file)).thenReturn("raw resume text");
        when(modelRouter.generate(eq(LlmTask.EXTRACTION), eq(userId), any(), eq("raw resume text")))
                .thenReturn("""
                        {
                          "headline": "Backend Engineer",
                          "summary": "10 years of Java",
                          "experiences": [{"company": "Acme", "title": "Engineer", "startDate": "2020-01-01", "endDate": null, "bullets": ["Built things"]}],
                          "skills": [{"name": "Java", "level": "Expert"}],
                          "education": [{"institution": "State University", "degree": "BS", "field": "CS", "startDate": "2012-09-01", "endDate": "2016-05-01"}]
                        }
                        """);

        ProfileView view = service.extract(userId, file);

        assertThat(view.profile().headline()).isEqualTo("Backend Engineer");
        assertThat(view.profile().userId()).isEqualTo(userId);
        assertThat(view.experiences()).hasSize(1);
        assertThat(view.experiences().get(0).company()).isEqualTo("Acme");
        assertThat(view.experiences().get(0).startDate()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(view.experiences().get(0).endDate()).isNull();
        assertThat(view.skills()).hasSize(1);
        assertThat(view.skills().get(0).name()).isEqualTo("Java");
        assertThat(view.education()).hasSize(1);
        assertThat(view.education().get(0).institution()).isEqualTo("State University");
    }

    @Test
    void systemPromptForbidsInventingFacts() {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[0]);
        when(textExtractor.extractText(file)).thenReturn("raw resume text");
        when(modelRouter.generate(eq(LlmTask.EXTRACTION), eq(userId), any(), eq("raw resume text")))
                .thenReturn("""
                        {"headline": null, "summary": null, "experiences": [], "skills": [], "education": []}
                        """);

        service.extract(userId, file);

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(modelRouter).generate(eq(LlmTask.EXTRACTION), eq(userId), systemPromptCaptor.capture(), eq("raw resume text"));
        assertThat(systemPromptCaptor.getValue()).contains("never invent");
    }

    @Test
    void malformedDateFromExtractionSurfacesAsIllegalStateException() {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[0]);
        when(textExtractor.extractText(file)).thenReturn("raw resume text");
        when(modelRouter.generate(eq(LlmTask.EXTRACTION), eq(userId), any(), eq("raw resume text")))
                .thenReturn("""
                        {
                          "headline": null,
                          "summary": null,
                          "experiences": [{"company": "Acme", "title": "Engineer", "startDate": "not-a-date", "endDate": null, "bullets": []}],
                          "skills": [],
                          "education": []
                        }
                        """);

        assertThatThrownBy(() -> service.extract(userId, file))
                .isInstanceOf(IllegalStateException.class);
    }
}
