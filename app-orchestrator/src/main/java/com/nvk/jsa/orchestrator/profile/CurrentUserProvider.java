package com.nvk.jsa.orchestrator.profile;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Phase 1 has no auth yet (that's Phase 6, CLAUDE.md §8) — every request is treated as one fixed
 * demo user, resolved server-side only, never accepted from client input (golden rule 3).
 */
@Component
public class CurrentUserProvider {

    private final UUID demoUserId;

    public CurrentUserProvider(@Value("${app.demo-user-id}") UUID demoUserId) {
        this.demoUserId = demoUserId;
    }

    public UUID currentUserId() {
        return demoUserId;
    }
}
