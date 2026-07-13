package com.nvk.jsa.common;

import java.util.UUID;

public record Profile(UUID id, UUID userId, String headline, String summary) {
}
