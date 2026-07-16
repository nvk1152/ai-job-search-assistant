package com.nvk.jsa.orchestrator.profile;

import com.nvk.jsa.orchestrator.resume.ResumeExtractionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST endpoints for profile CRUD: GET retrieves, PUT saves, POST /import extracts from résumé.
 * CurrentUserProvider ensures userId is never taken from client input (CLAUDE.md §2, rule 3).
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final ResumeExtractionService resumeExtractionService;
    private final CurrentUserProvider currentUserProvider;

    public ProfileController(
            ProfileService profileService,
            ResumeExtractionService resumeExtractionService,
            CurrentUserProvider currentUserProvider) {
        this.profileService = profileService;
        this.resumeExtractionService = resumeExtractionService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ProfileView getProfile() {
        return profileService.getProfile(currentUserProvider.currentUserId());
    }

    @PutMapping
    public ProfileView saveProfile(@RequestBody ProfileView profileView) {
        return profileService.saveProfile(currentUserProvider.currentUserId(), profileView);
    }

    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileView importResume(@RequestParam("file") MultipartFile file) {
        return resumeExtractionService.extract(currentUserProvider.currentUserId(), file);
    }
}
