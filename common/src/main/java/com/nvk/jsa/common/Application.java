package com.nvk.jsa.common;

import java.util.UUID;

public record Application(
        UUID id,
        UUID userId,
        UUID jobDescriptionId,
        ApplicationStage stage,
        String notes) {

    public enum ApplicationStage {
        SAVED, APPLIED, INTERVIEW, OFFER, REJECTED
    }
}
