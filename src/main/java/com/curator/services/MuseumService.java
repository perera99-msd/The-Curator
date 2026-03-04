package com.curator.services;

import com.curator.model.GameMode;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class MuseumService {

    public record ArtData(String title, String artist, String imageUrl, int value) {
    }

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public CompletableFuture<List<ArtData>> fetchArtworks(GameMode mode) {
        return CompletableFuture.supplyAsync(() -> {
            List<ArtData> artList = new ArrayList<>();
            try {
                int requestLimit = Math.max(10, mode.artSpawnCount() * 3);
                String url = "https://api.artic.edu/api/v1/artworks"
                        + "?fields=id,title,artist_display,image_id,classification_title"
                        + "&limit=" + requestLimit;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(8))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                JsonObject json = new Gson().fromJson(response.body(), JsonObject.class);
                JsonArray data = json == null ? null : json.getAsJsonArray("data");

                if (data != null) {
                    for (int i = 0; i < data.size() && artList.size() < mode.artSpawnCount(); i++) {
                        JsonObject obj = data.get(i).getAsJsonObject();
                        String imageId = safeString(obj, "image_id");
                        if (imageId == null || imageId.isBlank()) {
                            continue;
                        }

                        String title = safeString(obj, "title");
                        String artist = safeString(obj, "artist_display");
                        String imageUrl = "https://www.artic.edu/iiif/2/" + imageId + "/full/320,/0/default.jpg";

                        artList.add(new ArtData(
                                title == null ? "Unknown Piece" : title,
                                artist == null ? "Unknown Artist" : artist,
                                imageUrl,
                                randomValue(mode)
                        ));
                    }
                }
            } catch (Exception ignored) {
                // Fallback below
            }

            List<ArtData> result = artList.isEmpty() ? fallbackArt(mode) : artList;

            Collections.shuffle(result);
            return result.size() > mode.artSpawnCount()
                    ? result.subList(0, mode.artSpawnCount())
                    : result;
        });
    }

    private String safeString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    private int randomValue(GameMode mode) {
        return ThreadLocalRandom.current().nextInt(mode.minArtValue(), mode.maxArtValue() + 1);
    }

    private List<ArtData> fallbackArt(GameMode mode) {
        List<ArtData> fallback = new ArrayList<>();
        for (int i = 1; i <= mode.artSpawnCount(); i++) {
            fallback.add(new ArtData(
                    "Archived Masterpiece #" + i,
                    "Unknown Atelier",
                    "",
                    randomValue(mode)
            ));
        }
        return fallback;
    }
}
