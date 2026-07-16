package com.nvk.jsa.orchestrator.profile;

import com.nvk.jsa.common.Education;
import com.nvk.jsa.common.Experience;
import com.nvk.jsa.common.Profile;
import com.nvk.jsa.common.Skill;
import java.util.List;

/**
 * View DTO combining Profile (headline, summary) with nested Experience, Skill, and Education lists.
 * Used throughout the API and UI for display and editing.
 */
public record ProfileView(
        Profile profile,
        List<Experience> experiences,
        List<Skill> skills,
        List<Education> education) {
}
