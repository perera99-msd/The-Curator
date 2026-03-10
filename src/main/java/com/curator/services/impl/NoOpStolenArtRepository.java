package com.curator.services.impl;

import com.curator.domain.AuthSession;
import com.curator.domain.StolenArtEntry;
import com.curator.domain.StolenArtRecord;
import com.curator.services.StolenArtRepository;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// Fallback that reports missing Firestore configuration for vault operations.
public class NoOpStolenArtRepository implements StolenArtRepository {

    private final String reason;

    public NoOpStolenArtRepository(String reason) {
        this.reason = reason;
    }

    @Override
    public CompletableFuture<Void> saveStolenArt(AuthSession session, List<StolenArtRecord> records) {
        return CompletableFuture.failedFuture(new IllegalStateException(reason));
    }

    @Override
    public CompletableFuture<List<StolenArtEntry>> fetchStolenArt(AuthSession session) {
        return CompletableFuture.failedFuture(new IllegalStateException(reason));
    }

    @Override
    public CompletableFuture<Void> deleteStolenArt(AuthSession session, String documentId) {
        return CompletableFuture.failedFuture(new IllegalStateException(reason));
    }
}
