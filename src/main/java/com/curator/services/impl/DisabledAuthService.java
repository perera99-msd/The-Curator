package com.curator.services.impl;

import com.curator.domain.AuthSession;
import com.curator.services.AuthService;
import java.util.concurrent.CompletableFuture;

// Fallback keeps UI responsive when Firebase config is missing.
public class DisabledAuthService implements AuthService {

    private final String reason;

    public DisabledAuthService(String reason) {
        this.reason = reason;
    }

    @Override
    public CompletableFuture<AuthSession> signIn(String email, String password) {
        return fail();
    }

    @Override
    public CompletableFuture<AuthSession> register(String email, String password) {
        return fail();
    }

    @Override
    public CompletableFuture<AuthSession> signInWithGoogle() {
        return fail();
    }

    private CompletableFuture<AuthSession> fail() {
        return CompletableFuture.failedFuture(new IllegalStateException(reason));
    }
}
