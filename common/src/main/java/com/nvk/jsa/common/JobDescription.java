package com.nvk.jsa.common;

import java.util.UUID;

/**
 * Parsed job description: title, company, and raw/structured text. Matched against the user's
 * profile for fit-gap analysis and tailored outputs.
 */
public record JobDescription(UUID id, UUID userId, String company, String title, String rawText) {
}
