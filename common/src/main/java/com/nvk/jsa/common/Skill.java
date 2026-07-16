package com.nvk.jsa.common;

import java.util.UUID;

/**
 * Skill entry: name and optional level (e.g., "Expert", "Intermediate").
 * Extracted from résumé and editable in the profile editor.
 */
public record Skill(UUID id, UUID profileId, String name, String level) {
}