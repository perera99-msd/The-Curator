package com.curator.services;

import com.curator.domain.AuthSession;
import com.curator.domain.StolenArtEntry;
import com.curator.domain.StolenArtRecord;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// Repository abstraction keeps Firestore persistence low-coupled from game/UI logic.
public interface StolenArtRepository {
    CompletableFuture<Void> saveStolenArt(AuthSession session, List<StolenArtRecord> records);

    CompletableFuture<List<StolenArtEntry>> fetchStolenArt(AuthSession session);

    CompletableFuture<Void> deleteStolenArt(AuthSession session, String documentId);
}
