package com.curator.services;

import com.curator.domain.AuthSession;
import com.curator.domain.UserProfile;
import java.util.concurrent.CompletableFuture;

// User profile store keeps operator metadata in Firestore without coupling UI to storage.
public interface UserProfileRepository {
    CompletableFuture<UserProfile> fetchProfile(AuthSession session);

    CompletableFuture<UserProfile> upsertProfile(AuthSession session, UserProfile profile);

    CompletableFuture<UserProfile> updateDisplayName(AuthSession session, String displayName);
}
