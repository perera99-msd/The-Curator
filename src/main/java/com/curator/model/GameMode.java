package com.curator.model;

public enum GameMode {
    EASY("Easy", 160, 2, 2, 4, 62, 14, 2, 10, 16),
    MEDIUM("Medium", 120, 3, 3, 5, 78, 11, 3, 14, 24),
    HARD("Hard", 90, 4, 4, 6, 96, 8, 4, 20, 34);

    private final String displayName;
    private final int missionTimeSeconds;
    private final int requiredArtCount;
    private final int guardCount;
    private final int artSpawnCount;
    private final double guardSpeed;
    private final int puzzleTimeSeconds;
    private final int puzzleAttempts;
    private final int minArtValue;
    private final int maxArtValue;

    GameMode(String displayName,
             int missionTimeSeconds,
             int requiredArtCount,
             int guardCount,
             int artSpawnCount,
             double guardSpeed,
             int puzzleTimeSeconds,
             int puzzleAttempts,
             int minArtValue,
             int maxArtValue) {
        this.displayName = displayName;
        this.missionTimeSeconds = missionTimeSeconds;
        this.requiredArtCount = requiredArtCount;
        this.guardCount = guardCount;
        this.artSpawnCount = artSpawnCount;
        this.guardSpeed = guardSpeed;
        this.puzzleTimeSeconds = puzzleTimeSeconds;
        this.puzzleAttempts = puzzleAttempts;
        this.minArtValue = minArtValue;
        this.maxArtValue = maxArtValue;
    }

    public String displayName() {
        return displayName;
    }

    public int missionTimeSeconds() {
        return missionTimeSeconds;
    }

    public int requiredArtCount() {
        return requiredArtCount;
    }

    public int guardCount() {
        return guardCount;
    }

    public int artSpawnCount() {
        return artSpawnCount;
    }

    public double guardSpeed() {
        return guardSpeed;
    }

    public int puzzleTimeSeconds() {
        return puzzleTimeSeconds;
    }

    public int puzzleAttempts() {
        return puzzleAttempts;
    }

    public int minArtValue() {
        return minArtValue;
    }

    public int maxArtValue() {
        return maxArtValue;
    }

    public String objectiveText() {
        return "STEAL " + requiredArtCount + " ARTWORKS AND ESCAPE";
    }
}
