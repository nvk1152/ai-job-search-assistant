package com.nvk.jsa.common;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Work history entry: company, title, dates, and bullet-point achievements.
 * Extracted from résumé and editable in the profile editor.
 */
public record Experience(
        UUID id,
        UUID profileId,
        String company,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        List<String> bullets) {
}
