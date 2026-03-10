package com.curator.services;

import com.curator.domain.AuthSession;
import com.curator.domain.HeistReport;
import java.util.concurrent.CompletableFuture;

// Repository interface keeps persistence decoupled from game logic (IDatabaseService equivalent).
public interface HeistReportRepository {
    CompletableFuture<Void> submitReport(HeistReport report, AuthSession session);
}
