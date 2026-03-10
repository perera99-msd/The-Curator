package com.curator.services.impl;

import com.curator.domain.AuthSession;
import com.curator.domain.HeistReport;
import com.curator.services.HeistReportRepository;
import java.util.concurrent.CompletableFuture;

// Safe fallback that keeps the game running if cloud persistence is unavailable.
public class NoOpHeistReportRepository implements HeistReportRepository {

    private final String reason;

    public NoOpHeistReportRepository(String reason) {
        this.reason = reason;
    }

    @Override
    public CompletableFuture<Void> submitReport(HeistReport report, AuthSession session) {
        return CompletableFuture.failedFuture(new IllegalStateException(reason));
    }
}
