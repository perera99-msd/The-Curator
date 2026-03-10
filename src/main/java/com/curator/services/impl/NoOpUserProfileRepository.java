package com.curator.services.impl;

import com.curator.domain.AuthSession;
import com.curator.domain.UserProfile;
import com.curator.services.UserProfileRepository;
import java.util.concurrent.CompletableFuture;

// Fallback that reports missing Firestore configuration for profile operations.
public class NoOpUserProfileRepository implements UserProfileRepository {

    private final String reason;

    public NoOpUserProfileRepository(String reason) {
        this.reason = reason;
    }

    @Override
    public CompletableFuture<UserProfile> fetchProfile(AuthSession session) {
        return CompletableFuture.failedFuture(new IllegalStateException(reason));
    }

    @Override
    public CompletableFuture<UserProfile> upsertProfile(AuthSession session, UserProfile profile) {
        return CompletableFuture.failedFuture(new IllegalStateException(reason));
    }

    @Override
    public CompletableFuture<UserProfile> updateDisplayName(AuthSession session, String displayName) {
        return CompletableFuture.failedFuture(new IllegalStateException(reason));
    }
}
