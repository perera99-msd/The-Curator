package com.curator.services.impl;

import com.curator.config.FirebaseConfig;
import com.curator.domain.AuthSession;
import com.curator.domain.StolenArtEntry;
import com.curator.domain.StolenArtRecord;
import com.curator.services.StolenArtRepository;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// Persists stolen artworks in Firestore under users/{uid}/stolenArt.
public class FirestoreStolenArtRepository implements StolenArtRepository {

    private static final Gson GSON = new Gson();
    private static final String BASE_URL = "https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents";

    private final FirebaseConfig config;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    public FirestoreStolenArtRepository(FirebaseConfig config) {
        this.config = config;
    }

    @Override
    public CompletableFuture<Void> saveStolenArt(AuthSession session, List<StolenArtRecord> records) {
        return CompletableFuture.runAsync(() -> {
            if (session == null || session.idToken() == null || session.idToken().isBlank()) {
                throw new IllegalStateException("No authenticated session for vault upload.");
            }
            if (records == null || records.isEmpty()) {
                return;
            }

            String endpoint = collectionUrl(session);
            for (StolenArtRecord record : records) {
                JsonObject payload = new JsonObject();
                JsonObject fields = new JsonObject();
                fields.add("title", stringField(record.title()));
                fields.add("artist", stringField(record.artist()));
                fields.add("value", integerField(record.value()));
                fields.add("mode", stringField(record.mode()));
                fields.add("stolenAt", stringField(record.stolenAt()));
                fields.add("imageUrl", stringField(record.imageUrl()));
                payload.add("fields", fields);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + session.idToken())
                        .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
                        .build();

                sendRequest(request, "Vault upload failed");
            }
        });
    }

    @Override
    public CompletableFuture<List<StolenArtEntry>> fetchStolenArt(AuthSession session) {
        return CompletableFuture.supplyAsync(() -> {
            if (session == null || session.idToken() == null || session.idToken().isBlank()) {
                throw new IllegalStateException("No authenticated session for vault fetch.");
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(collectionUrl(session)))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + session.idToken())
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 404) {
                    return List.of();
                }
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("Vault fetch failed: HTTP " + response.statusCode());
                }
                return parseVault(response.body());
            } catch (Exception e) {
                if (e instanceof IllegalStateException) {
                    throw (IllegalStateException) e;
                }
                throw new IllegalStateException("Vault fetch failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteStolenArt(AuthSession session, String documentId) {
        return CompletableFuture.runAsync(() -> {
            if (session == null || session.idToken() == null || session.idToken().isBlank()) {
                throw new IllegalStateException("No authenticated session for vault delete.");
            }
            if (documentId == null || documentId.isBlank()) {
                throw new IllegalArgumentException("Missing document id.");
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(documentUrl(session, documentId)))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + session.idToken())
                    .DELETE()
                    .build();

            sendRequest(request, "Vault delete failed");
        });
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

    private List<StolenArtEntry> parseVault(String body) {
        JsonObject json = GSON.fromJson(body, JsonObject.class);
        JsonArray documents = json == null ? null : json.getAsJsonArray("documents");
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        List<StolenArtEntry> entries = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            JsonObject doc = documents.get(i).getAsJsonObject();
            String name = safeString(doc, "name");
            String docId = name == null ? "" : name.substring(name.lastIndexOf('/') + 1);
            JsonObject fields = doc.getAsJsonObject("fields");
            if (fields == null) {
                continue;
            }

            String title = readString(fields, "title");
            String artist = readString(fields, "artist");
            int value = readInt(fields, "value");
            String mode = readString(fields, "mode");
            String stolenAt = readString(fields, "stolenAt");
            String imageUrl = readString(fields, "imageUrl");

            StolenArtRecord record = new StolenArtRecord(title, artist, value, mode, stolenAt, imageUrl);
            entries.add(new StolenArtEntry(docId, record));
        }

        entries.sort(Comparator.comparing(entry -> entry.record().stolenAt()));
        return entries;
    }

    private String collectionUrl(AuthSession session) {
        return String.format(BASE_URL + "/users/%s/stolenArt?key=%s",
                config.projectId(),
                urlEncode(session.userId()),
                config.apiKey());
    }

    private String documentUrl(AuthSession session, String documentId) {
        return String.format(BASE_URL + "/users/%s/stolenArt/%s?key=%s",
                config.projectId(),
                urlEncode(session.userId()),
                urlEncode(documentId),
                config.apiKey());
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

    private String readString(JsonObject fields, String key) {
        if (!fields.has(key) || fields.get(key).isJsonNull()) {
            return "";
        }
        JsonObject field = fields.getAsJsonObject(key);
        if (field.has("stringValue")) {
            return field.get("stringValue").getAsString();
        }
        if (field.has("integerValue")) {
            return field.get("integerValue").getAsString();
        }
        return "";
    }

    private int readInt(JsonObject fields, String key) {
        String value = readString(fields, key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String safeString(JsonObject obj, String key) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
