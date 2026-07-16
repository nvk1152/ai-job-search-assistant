package com.nvk.jsa.orchestrator.resume;

import java.util.List;

/**
 * DTO for LLM extraction response: headline, summary, and nested lists of experiences, skills,
 * and education extracted from résumé text.
 */
record ExtractedProfileData(
        String headline,
        String summary,
        List<ExtractedExperience> experiences,
        List<ExtractedSkill> skills,
        List<ExtractedEducation> education) {

    record ExtractedExperience(String company, String title, String startDate, String endDate, List<String> bullets) {
    }

    record ExtractedSkill(String name, String level) {
    }

    record ExtractedEducation(String institution, String degree, String field, String startDate, String endDate) {
    }
}
