package com.curator.services.impl;

import com.curator.config.FirebaseConfig;
import com.curator.domain.AuthSession;
import com.curator.domain.UserProfile;
import com.curator.services.UserProfileRepository;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

// Stores operator display names in Firestore under users/{uid}.
public class FirestoreUserProfileRepository implements UserProfileRepository {

    private static final Gson GSON = new Gson();
    private static final String BASE_URL = "https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents";

    private final FirebaseConfig config;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    public FirestoreUserProfileRepository(FirebaseConfig config) {
        this.config = config;
    }

    @Override
    public CompletableFuture<UserProfile> fetchProfile(AuthSession session) {
        return CompletableFuture.supplyAsync(() -> {
            if (session == null || session.idToken() == null || session.idToken().isBlank()) {
                throw new IllegalStateException("No authenticated session for profile fetch.");
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(profileUrl(session)))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + session.idToken())
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 404) {
                    return null;
                }
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("Profile fetch failed: HTTP " + response.statusCode());
                }
                return parseProfile(session, response.body());
            } catch (Exception e) {
                if (e instanceof IllegalStateException) {
                    throw (IllegalStateException) e;
                }
                throw new IllegalStateException("Profile fetch failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<UserProfile> upsertProfile(AuthSession session, UserProfile profile) {
        return CompletableFuture.supplyAsync(() -> {
            if (session == null || session.idToken() == null || session.idToken().isBlank()) {
                throw new IllegalStateException("No authenticated session for profile update.");
            }
            if (profile == null) {
                throw new IllegalArgumentException("Missing profile data.");
            }

            JsonObject payload = new JsonObject();
            JsonObject fields = new JsonObject();
            fields.add("displayName", stringField(profile.displayName()));
            fields.add("email", stringField(profile.email()));
            payload.add("fields", fields);

            String url = profileUrl(session)
                    + "&updateMask.fieldPaths=displayName"
                    + "&updateMask.fieldPaths=email";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + session.idToken())
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
                    .build();

            sendRequest(request, "Profile update failed");
            return profile;
        });
    }

    @Override
    public CompletableFuture<UserProfile> updateDisplayName(AuthSession session, String displayName) {
        String normalized = displayName == null ? "" : displayName.trim();
        UserProfile profile = new UserProfile(session.userId(), normalized, session.email());
        return upsertProfile(session, profile);
    }

    private void sendRequest(HttpRequest request, String message) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(message + ": HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }
            throw new IllegalStateException(message + ": " + e.getMessage(), e);
        }
    }

    private UserProfile parseProfile(AuthSession session, String body) {
        JsonObject json = GSON.fromJson(body, JsonObject.class);
        JsonObject fields = json == null ? null : json.getAsJsonObject("fields");
        if (fields == null) {
            return null;
        }

        String displayName = readString(fields, "displayName");
        String email = readString(fields, "email");
        return new UserProfile(session.userId(), displayName, email == null ? session.email() : email);
    }

    private JsonObject stringField(String value) {
        JsonObject field = new JsonObject();
        field.addProperty("stringValue", value == null ? "" : value);
        return field;
    }

    private String readString(JsonObject fields, String key) {
        if (!fields.has(key) || fields.get(key).isJsonNull()) {
            return "";
        }
        JsonObject field = fields.getAsJsonObject(key);
        return field.has("stringValue") ? field.get("stringValue").getAsString() : "";
    }

    private String profileUrl(AuthSession session) {
        return String.format(BASE_URL + "/users/%s?key=%s",
                config.projectId(),
                urlEncode(session.userId()),
                config.apiKey());
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
