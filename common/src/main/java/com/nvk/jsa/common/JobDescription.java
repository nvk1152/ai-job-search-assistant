package com.nvk.jsa.common;

import java.util.UUID;

public record JobDescription(UUID id, UUID userId, String company, String title, String rawText) {
}
