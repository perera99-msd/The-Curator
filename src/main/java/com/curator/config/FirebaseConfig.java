package com.curator.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record FirebaseConfig(String apiKey,
                             String projectId,
                             String googleClientId,
                             String googleClientSecret) {

    public static FirebaseConfig load() {
        Properties props = new Properties();
        Properties dotEnv = loadLocalDotEnv();

        try (InputStream in = FirebaseConfig.class.getResourceAsStream("/firebase.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception ignored) {
            // Missing properties will be handled by validation below.
        }

        String apiKey = firstNonBlank(System.getenv("FIREBASE_API_KEY"), dotEnv.getProperty("FIREBASE_API_KEY"), props.getProperty("firebase.apiKey"));
        String projectId = firstNonBlank(System.getenv("FIREBASE_PROJECT_ID"), dotEnv.getProperty("FIREBASE_PROJECT_ID"), props.getProperty("firebase.projectId"));
        String googleClientId = firstNonBlank(System.getenv("GOOGLE_CLIENT_ID"), dotEnv.getProperty("GOOGLE_CLIENT_ID"), props.getProperty("google.clientId"));
        String googleClientSecret = firstNonBlank(System.getenv("GOOGLE_CLIENT_SECRET"), dotEnv.getProperty("GOOGLE_CLIENT_SECRET"), props.getProperty("google.clientSecret"));

        if (isBlank(apiKey) || isBlank(projectId) || apiKey.startsWith("YOUR_") || projectId.startsWith("YOUR_")) {
            throw new IllegalStateException("Firebase configuration missing. Set firebase.apiKey and firebase.projectId.");
        }

        return new FirebaseConfig(apiKey.trim(), projectId.trim(),
                normalizeOptional(googleClientId), normalizeOptional(googleClientSecret));
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value) && !value.startsWith("YOUR_")) {
                return value;
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String normalizeOptional(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static Properties loadLocalDotEnv() {
        Properties env = new Properties();
        Path path = Path.of(".env.local");
        if (!Files.exists(path)) {
            return env;
        }
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int equalsIndex = trimmed.indexOf('=');
                if (equalsIndex <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, equalsIndex).trim();
                String value = trimmed.substring(equalsIndex + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                env.setProperty(key, value);
            }
        } catch (IOException ignored) {
            // Local env file is optional.
        }
        return env;
    }
}
