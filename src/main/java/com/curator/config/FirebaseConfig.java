package com.curator.config;

import java.io.InputStream;
import java.util.Properties;

public record FirebaseConfig(String apiKey,
                             String projectId,
                             String googleClientId,
                             String googleClientSecret) {

    public static FirebaseConfig load() {
        Properties props = new Properties();
        try (InputStream in = FirebaseConfig.class.getResourceAsStream("/firebase.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception ignored) {
            // Missing properties will be handled by validation below.
        }

        String apiKey = firstNonBlank(System.getenv("FIREBASE_API_KEY"), props.getProperty("firebase.apiKey"));
        String projectId = firstNonBlank(System.getenv("FIREBASE_PROJECT_ID"), props.getProperty("firebase.projectId"));
        String googleClientId = firstNonBlank(System.getenv("GOOGLE_CLIENT_ID"), props.getProperty("google.clientId"));
        String googleClientSecret = firstNonBlank(System.getenv("GOOGLE_CLIENT_SECRET"), props.getProperty("google.clientSecret"));

        if (isBlank(apiKey) || isBlank(projectId)) {
            throw new IllegalStateException("Firebase configuration missing. Set firebase.apiKey and firebase.projectId.");
        }

        return new FirebaseConfig(apiKey.trim(), projectId.trim(),
                normalizeOptional(googleClientId), normalizeOptional(googleClientSecret));
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (!isBlank(primary)) {
            return primary;
        }
        return isBlank(fallback) ? null : fallback;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String normalizeOptional(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
