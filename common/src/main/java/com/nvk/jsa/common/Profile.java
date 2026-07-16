package com.nvk.jsa.common;

import java.util.UUID;

/**
 * User's professional profile (headline + summary). Paired with Experience, Skill, and Education
 * to form a complete ProfileView used across resume extraction and editing.
 */
public record Profile(UUID id, UUID userId, String headline, String summary) {
}
