package com.nvk.jsa.orchestrator.profile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nvk.jsa.common.Profile;
import com.nvk.jsa.orchestrator.resume.ResumeExtractionService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProfileController.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private ResumeExtractionService resumeExtractionService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void getReturnsTheCurrentUsersProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        when(currentUserProvider.currentUserId()).thenReturn(userId);
        ProfileView view = new ProfileView(
                new Profile(UUID.randomUUID(), userId, "Backend Engineer", "10 years of Java"),
                List.of(), List.of(), List.of());
        when(profileService.getProfile(userId)).thenReturn(view);

        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.headline").value("Backend Engineer"));
    }

    @Test
    void importReturnsExtractedProfileDraft() throws Exception {
        UUID userId = UUID.randomUUID();
        when(currentUserProvider.currentUserId()).thenReturn(userId);
        MockMultipartFile uploadedFile = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[0]);
        ProfileView draft = new ProfileView(
                new Profile(UUID.randomUUID(), userId, "Extracted Headline", null),
                List.of(), List.of(), List.of());
        when(resumeExtractionService.extract(any(UUID.class), any())).thenReturn(draft);

        mockMvc.perform(multipart("/api/profile/import").file(uploadedFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.headline").value("Extracted Headline"));
    }

    @Test
    void extractionFailureSurfacesAsUnprocessableEntityWithApiErrorBody() throws Exception {
        UUID userId = UUID.randomUUID();
        when(currentUserProvider.currentUserId()).thenReturn(userId);
        MockMultipartFile uploadedFile = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[0]);
        when(resumeExtractionService.extract(any(UUID.class), any()))
                .thenThrow(new IllegalStateException("Résumé extraction returned an invalid date: not-a-date"));

        mockMvc.perform(multipart("/api/profile/import").file(uploadedFile))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("extraction_failed"))
                .andExpect(jsonPath("$.message").value("Résumé extraction returned an invalid date: not-a-date"));
    }
}
