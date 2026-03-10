package com.curator.services.impl;

import com.curator.config.FirebaseConfig;
import com.curator.domain.AuthSession;
import com.curator.domain.HeistReport;
import com.curator.services.HeistReportRepository;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

// Persists mission results to a remote Firestore database (distributed persistence).
public class FirestoreReportRepository implements HeistReportRepository {

    private static final Gson GSON = new Gson();
    private static final String ENDPOINT = "https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/heistReports?key=%s";

    private final FirebaseConfig config;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    public FirestoreReportRepository(FirebaseConfig config) {
        this.config = config;
    }

    @Override
    public CompletableFuture<Void> submitReport(HeistReport report, AuthSession session) {
        return CompletableFuture.runAsync(() -> {
            if (session == null || session.idToken() == null || session.idToken().isBlank()) {
                throw new IllegalStateException("No authenticated session for report upload.");
            }

            JsonObject payload = new JsonObject();
            JsonObject fields = new JsonObject();
            fields.add("userId", stringField(report.userId()));
            fields.add("score", integerField(report.score()));
            fields.add("outcome", stringField(report.outcome()));
            fields.add("timestamp", timestampField(report.timestamp().toString()));
            payload.add("fields", fields);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(ENDPOINT, config.projectId(), config.apiKey())))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + session.idToken())
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("Firestore write failed: HTTP " + response.statusCode());
                }
            } catch (Exception e) {
                if (e instanceof IllegalStateException) {
                    throw (IllegalStateException) e;
                }
                throw new IllegalStateException("Firestore write failed: " + e.getMessage(), e);
            }
        });
    }

    private JsonObject stringField(String value) {
        JsonObject field = new JsonObject();
        field.addProperty("stringValue", value == null ? "" : value);
        return field;
    }

    private JsonObject integerField(int value) {
        JsonObject field = new JsonObject();
        field.addProperty("integerValue", String.valueOf(value));
        return field;
    }

    private JsonObject timestampField(String value) {
        JsonObject field = new JsonObject();
        field.addProperty("timestampValue", value);
        return field;
    }
}
