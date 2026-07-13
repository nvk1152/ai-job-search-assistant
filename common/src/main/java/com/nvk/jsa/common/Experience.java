package com.nvk.jsa.common;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record Experience(
        UUID id,
        UUID profileId,
        String company,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        List<String> bullets) {
}
