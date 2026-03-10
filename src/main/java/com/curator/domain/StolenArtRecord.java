package com.curator.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record StolenArtRecord(String title, String artist, int value, String mode, String stolenAt, String imageUrl) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static StolenArtRecord create(String title, String artist, int value, GameMode mode) {
        return create(title, artist, value, mode, "");
    }

    public static StolenArtRecord create(String title, String artist, int value, GameMode mode, String imageUrl) {
        return new StolenArtRecord(
                title == null || title.isBlank() ? "Untitled Work" : title,
                artist == null || artist.isBlank() ? "Unknown Artist" : artist,
                value,
                mode.displayName(),
                LocalDateTime.now().format(FORMATTER),
                imageUrl == null ? "" : imageUrl
        );
    }
}
