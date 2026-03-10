package com.curator.domain;

// Stores operator identity metadata in Firestore.
public record UserProfile(String userId, String displayName, String email) {
}
