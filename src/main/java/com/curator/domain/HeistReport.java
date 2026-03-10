package com.curator.domain;

import java.time.Instant;

public record HeistReport(String userId, int score, String outcome, Instant timestamp) {
    public static HeistReport of(AuthSession session, int score, String outcome) {
        String userId = session == null ? "unknown" : session.userId();
        return new HeistReport(userId, score, outcome, Instant.now());
    }
}
