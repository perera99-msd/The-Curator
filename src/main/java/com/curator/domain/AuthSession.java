package com.curator.domain;

// Holds Firebase identity info so the game can associate actions with a user.
public record AuthSession(String userId, String idToken, String email, boolean isNewUser) {
}
