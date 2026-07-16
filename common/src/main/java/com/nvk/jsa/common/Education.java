package com.nvk.jsa.common;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Education history: institution, degree, field of study, and dates.
 * Extracted from résumé and editable in the profile editor.
 */
public record Education(
        UUID id,
        UUID profileId,
        String institution,
        String degree,
        String field,
        LocalDate startDate,
        LocalDate endDate) {
}