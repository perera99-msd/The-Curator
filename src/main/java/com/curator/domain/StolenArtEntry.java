package com.curator.domain;

// Firestore document wrapper for a stolen artwork entry.
public record StolenArtEntry(String documentId, StolenArtRecord record) {
}
