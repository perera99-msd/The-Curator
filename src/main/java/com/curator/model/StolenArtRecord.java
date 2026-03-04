package com.curator.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record StolenArtRecord(String title, String artist, int value, String mode, String stolenAt) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static StolenArtRecord create(String title, String artist, int value, GameMode mode) {
        return new StolenArtRecord(
                title == null || title.isBlank() ? "Untitled Work" : title,
                artist == null || artist.isBlank() ? "Unknown Artist" : artist,
                value,
                mode.displayName(),
                LocalDateTime.now().format(FORMATTER)
        );
    }
}
