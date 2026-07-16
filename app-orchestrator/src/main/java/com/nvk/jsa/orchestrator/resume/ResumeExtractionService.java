package com.nvk.jsa.orchestrator.resume;

import com.nvk.jsa.common.Education;
import com.nvk.jsa.common.Experience;
import com.nvk.jsa.common.Profile;
import com.nvk.jsa.common.Skill;
import com.nvk.jsa.orchestrator.llm.LlmTask;
import com.nvk.jsa.orchestrator.llm.ModelRouter;
import com.nvk.jsa.orchestrator.profile.ProfileView;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.ai.util.JsonHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Enforces the no-fabrication guardrail (CLAUDE.md §6.1): extracts only facts present in the
 * résumé text; never invents employers, titles, skills, or dates. Uses EXTRACTION task routed
 * through ModelRouter with explicit prompt constraints.
 */
@Service
public class ResumeExtractionService {

    private static final String SYSTEM_PROMPT = """
            You structure résumé text into JSON. Extract ONLY facts present in the résumé text —
            never invent an employer, title, skill, or date. Respond with JSON only, matching exactly
            this shape:
            {
              "headline": string or null,
              "summary": string or null,
              "experiences": [{"company": string, "title": string, "startDate": "YYYY-MM-DD" or null, "endDate": "YYYY-MM-DD" or null, "bullets": [string]}],
              "skills": [{"name": string, "level": string or null}],
              "education": [{"institution": string, "degree": string or null, "field": string or null, "startDate": "YYYY-MM-DD" or null, "endDate": "YYYY-MM-DD" or null}]
            }
            """;

    private final ResumeTextExtractor textExtractor;
    private final ModelRouter modelRouter;
    private final JsonHelper jsonHelper = new JsonHelper();

    public ResumeExtractionService(ResumeTextExtractor textExtractor, ModelRouter modelRouter) {
        this.textExtractor = textExtractor;
        this.modelRouter = modelRouter;
    }

    public ProfileView extract(UUID userId, MultipartFile file) {
        String resumeText = textExtractor.extractText(file);
        String json = modelRouter.generate(LlmTask.EXTRACTION, userId, SYSTEM_PROMPT, resumeText);
        ExtractedProfileData data = jsonHelper.fromJson(json, ExtractedProfileData.class);

        UUID profileId = UUID.randomUUID();
        Profile profile = new Profile(profileId, userId, data.headline(), data.summary());

        List<Experience> experiences = data.experiences() == null
                ? List.of()
                : data.experiences().stream()
                        .map(e -> new Experience(UUID.randomUUID(), profileId, e.company(), e.title(),
                                parseDate(e.startDate()), parseDate(e.endDate()),
                                e.bullets() == null ? List.of() : e.bullets()))
                        .toList();

        List<Skill> skills = data.skills() == null
                ? List.of()
                : data.skills().stream()
                        .map(s -> new Skill(UUID.randomUUID(), profileId, s.name(), s.level()))
                        .toList();

        List<Education> education = data.education() == null
                ? List.of()
                : data.education().stream()
                        .map(ed -> new Education(UUID.randomUUID(), profileId, ed.institution(), ed.degree(), ed.field(),
                                parseDate(ed.startDate()), parseDate(ed.endDate())))
                        .toList();

        return new ProfileView(profile, experiences, skills, education);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalStateException("Résumé extraction returned an invalid date: " + value, e);
        }
    }
}
