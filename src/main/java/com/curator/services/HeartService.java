package com.curator.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class HeartService {

    // API doc: https://marcconrad.com/uob/heart/doc.php
    private static final String HEART_API = "https://marcconrad.com/uob/heart/api.php?out=json&seed=";

    public record HeartPuzzle(String prompt, String imageUrl, int solution) {
    }

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    public CompletableFuture<HeartPuzzle> fetchPuzzle() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                long seed = ThreadLocalRandom.current().nextLong(100000, 999999);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(HEART_API + seed))
                        .timeout(Duration.ofSeconds(8))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonObject json = new Gson().fromJson(response.body(), JsonObject.class);

                if (json != null && json.has("solution")) {
                    String questionUrl = json.has("question") ? json.get("question").getAsString() : "";
                    int solution = parseSolution(json.get("solution").getAsString());
                    return new HeartPuzzle("Solve the Heart puzzle and enter the number.", questionUrl, solution);
                }
            } catch (Exception ignored) {
                // Fallback below
            }

            return fallbackPuzzle();
        });
    }

    public boolean isCorrect(HeartPuzzle puzzle, String userInput) {
        if (puzzle == null || userInput == null || userInput.isBlank()) {
            return false;
        }

        try {
            return Integer.parseInt(userInput.trim()) == puzzle.solution();
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private int parseSolution(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private HeartPuzzle fallbackPuzzle() {
        int a = ThreadLocalRandom.current().nextInt(4, 16);
        int b = ThreadLocalRandom.current().nextInt(3, 14);
        return new HeartPuzzle("Offline fallback challenge: " + a + " + " + b + " = ?", "", a + b);
    }
}
