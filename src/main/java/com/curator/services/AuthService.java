package com.curator.services;

import com.curator.domain.AuthSession;
import java.util.concurrent.CompletableFuture;

// Auth interface (IAuthenticationService) provides low coupling and supports virtual identity.
public interface AuthService {
    CompletableFuture<AuthSession> signIn(String email, String password);

    CompletableFuture<AuthSession> register(String email, String password);

    CompletableFuture<AuthSession> signInWithGoogle();
}
