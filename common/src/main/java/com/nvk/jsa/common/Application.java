package com.nvk.jsa.common;

import java.util.UUID;

/**
 * Job application record: tracks status (SAVED, APPLIED, INTERVIEW, OFFER, REJECTED) and notes
 * for a specific job. All generated drafts (résumé, cover letter, talking points) are persisted
 * separately per application.
 */
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
