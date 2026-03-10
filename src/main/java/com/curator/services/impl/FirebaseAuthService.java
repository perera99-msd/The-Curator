package com.curator.services.impl;

import com.curator.config.FirebaseConfig;
import com.curator.domain.AuthSession;
import com.curator.services.AuthService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.awt.Desktop;

// Uses Firebase REST auth to establish a virtual identity (email/password -> ID token).
public class FirebaseAuthService implements AuthService {

    private static final Gson GSON = new Gson();
    private static final String SIGN_IN_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=";
    private static final String SIGN_UP_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=";
    private static final String SIGN_IN_WITH_IDP_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=";
    private static final String GOOGLE_OAUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    private final FirebaseConfig config;

    public FirebaseAuthService(FirebaseConfig config) {
        this.config = config;
    }

    @Override
    public CompletableFuture<AuthSession> signIn(String email, String password) {
        return authenticate(email, password, SIGN_IN_URL, false);
    }

    @Override
    public CompletableFuture<AuthSession> register(String email, String password) {
        return authenticate(email, password, SIGN_UP_URL, true);
    }

    @Override
    public CompletableFuture<AuthSession> signInWithGoogle() {
        return CompletableFuture.supplyAsync(() -> {
            if (config.googleClientId() == null || config.googleClientSecret() == null) {
                throw new IllegalStateException("Google OAuth config missing. Set google.clientId and google.clientSecret.");
            }

            String state = UUID.randomUUID().toString();
            String redirectUri;
            HttpServer server = null;
            try {
                CompletableFuture<String> codeFuture = new CompletableFuture<>();
                server = HttpServer.create(new java.net.InetSocketAddress("localhost", 0), 0);
                int port = server.getAddress().getPort();
                // Loopback redirect for installed apps; keep path "/" to match "http://localhost" registration.
                redirectUri = "http://localhost:" + port;

                server.createContext("/", exchange -> {
                    String query = exchange.getRequestURI().getQuery();
                    Map<String, String> params = QueryString.parse(query);
                    String code = params.get("code");
                    String returnedState = params.get("state");
                    String error = params.get("error");
                    String errorDescription = params.get("error_description");

                    String response = "<html><body><h3>Login complete.</h3>You can return to the game.</body></html>";
                    exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                    try (var os = exchange.getResponseBody()) {
                        os.write(response.getBytes(StandardCharsets.UTF_8));
                    }

                    if (error != null) {
                        String detail = errorDescription == null ? error : errorDescription;
                        codeFuture.completeExceptionally(new IllegalStateException("Google login failed: " + detail));
                    } else if (code == null || returnedState == null || !returnedState.equals(state)) {
                        codeFuture.completeExceptionally(new IllegalStateException("Google login failed: invalid response."));
                    } else {
                        codeFuture.complete(code);
                    }
                });

                server.start();

                String authUrl = GOOGLE_OAUTH_URL
                        + "?client_id=" + urlEncode(config.googleClientId())
                        + "&redirect_uri=" + urlEncode(redirectUri)
                        + "&response_type=code"
                        + "&scope=" + urlEncode("openid email profile")
                        + "&access_type=offline"
                        + "&prompt=consent"
                        + "&state=" + urlEncode(state);

                openBrowser(authUrl);

                String code = codeFuture.get(120, TimeUnit.SECONDS);
                return firebaseSignInWithGoogle(code, redirectUri);
            } catch (TimeoutException e) {
                throw new IllegalStateException("Google login timed out. Please try again.");
            } catch (Exception e) {
                if (e instanceof IllegalStateException) {
                    throw (IllegalStateException) e;
                }
                throw new IllegalStateException("Google login failed: " + e.getMessage(), e);
            } finally {
                if (server != null) {
                    server.stop(0);
                }
            }
        });
    }

    private CompletableFuture<AuthSession> authenticate(String email, String password, String endpoint, boolean defaultIsNewUser) {
        return CompletableFuture.supplyAsync(() -> {
            String normalizedEmail = email == null ? "" : email.trim();
            String normalizedPassword = password == null ? "" : password.trim();

            if (normalizedEmail.isEmpty() || normalizedPassword.isEmpty()) {
                throw new IllegalArgumentException("Email and password are required.");
            }

            JsonObject payload = new JsonObject();
            payload.addProperty("email", normalizedEmail);
            payload.addProperty("password", normalizedPassword);
            payload.addProperty("returnSecureToken", true);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + config.apiKey()))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return parseSession(response.body(), defaultIsNewUser);
                }
                throw new IllegalStateException(extractError(response.body()));
            } catch (Exception e) {
                if (e instanceof IllegalStateException) {
                    throw (IllegalStateException) e;
                }
                throw new IllegalStateException("Authentication failed: " + e.getMessage(), e);
            }
        });
    }

    private AuthSession parseSession(String body, boolean defaultIsNewUser) {
        JsonObject json = GSON.fromJson(body, JsonObject.class);
        if (json == null) {
            throw new IllegalStateException("Authentication failed: empty response.");
        }

        boolean isNewUser = defaultIsNewUser;
        if (json.has("isNewUser") && !json.get("isNewUser").isJsonNull()) {
            isNewUser = json.get("isNewUser").getAsBoolean();
        }

        String userId = safeString(json, "localId");
        String idToken = safeString(json, "idToken");
        String email = safeString(json, "email");

        if (userId == null || idToken == null) {
            throw new IllegalStateException("Authentication failed: missing token.");
        }

        return new AuthSession(userId, idToken, email == null ? "" : email, isNewUser);
    }

    private String extractError(String body) {
        try {
            JsonObject json = GSON.fromJson(body, JsonObject.class);
            if (json != null && json.has("error")) {
                JsonObject error = json.getAsJsonObject("error");
                String message = safeString(error, "message");
                return mapFirebaseMessage(message);
            }
        } catch (Exception ignored) {
            // Fall through.
        }
        return "Authentication failed: unexpected response.";
    }

    private String mapFirebaseMessage(String message) {
        if (message == null) {
            return "Authentication failed.";
        }
        if (message.startsWith("WEAK_PASSWORD")) {
            return "Password is too weak (min 6 characters).";
        }
        return switch (message) {
            case "EMAIL_NOT_FOUND" -> "Email not found.";
            case "INVALID_PASSWORD" -> "Incorrect password.";
            case "EMAIL_EXISTS", "ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> "Already Registered.";
            case "INVALID_EMAIL" -> "Email address is invalid.";
            case "MISSING_PASSWORD" -> "Password is required.";
            default -> "Authentication error: " + message;
        };
    }

    private String safeString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    private AuthSession firebaseSignInWithGoogle(String authCode, String redirectUri) throws Exception {
        JsonObject tokenPayload = new JsonObject();
        tokenPayload.addProperty("client_id", config.googleClientId());
        tokenPayload.addProperty("client_secret", config.googleClientSecret());
        tokenPayload.addProperty("code", authCode);
        tokenPayload.addProperty("redirect_uri", redirectUri);
        tokenPayload.addProperty("grant_type", "authorization_code");

        HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create(GOOGLE_TOKEN_URL))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(tokenPayload)))
                .build();

        HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
        if (tokenResponse.statusCode() < 200 || tokenResponse.statusCode() >= 300) {
            throw new IllegalStateException("Google token exchange failed.");
        }

        JsonObject tokenJson = GSON.fromJson(tokenResponse.body(), JsonObject.class);
        String idToken = tokenJson == null ? null : safeString(tokenJson, "id_token");
        if (idToken == null) {
            throw new IllegalStateException("Google token exchange failed: missing id_token.");
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("postBody", "id_token=" + urlEncode(idToken) + "&providerId=google.com");
        payload.addProperty("requestUri", "http://localhost");
        payload.addProperty("returnSecureToken", true);

        HttpRequest firebaseRequest = HttpRequest.newBuilder()
                .uri(URI.create(SIGN_IN_WITH_IDP_URL + config.apiKey()))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
                .build();

        HttpResponse<String> firebaseResponse = httpClient.send(firebaseRequest, HttpResponse.BodyHandlers.ofString());
        if (firebaseResponse.statusCode() >= 200 && firebaseResponse.statusCode() < 300) {
            return parseSession(firebaseResponse.body(), false);
        }
        throw new IllegalStateException(extractError(firebaseResponse.body()));
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Exception ignored) {
            // Fall through to error below.
        }
        throw new IllegalStateException("Unable to open browser for Google login. Use email/password instead.");
    }

    private static final class QueryString {
        private static Map<String, String> parse(String query) {
            if (query == null || query.isBlank()) {
                return Map.of();
            }
            String[] pairs = query.split("&");
            java.util.HashMap<String, String> map = new java.util.HashMap<>();
            for (String pair : pairs) {
                String[] parts = pair.split("=", 2);
                String key = java.net.URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                String value = parts.length > 1
                        ? java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                        : "";
                map.put(key, value);
            }
            return map;
        }
    }
}
