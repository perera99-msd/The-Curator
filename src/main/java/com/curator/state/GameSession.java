package com.curator.state;

import com.curator.model.GameMode;
import com.curator.model.StolenArtRecord;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GameSession {

    private static GameMode selectedMode = GameMode.MEDIUM;
    private static boolean showGuardCones = true;
    private static boolean loggedIn;
    private static String operatorAlias = "Operator";

    private static final List<StolenArtRecord> vault = new ArrayList<>();
    private static final List<StolenArtRecord> runLoot = new ArrayList<>();

    private GameSession() {
    }

    public static synchronized GameMode getSelectedMode() {
        return selectedMode;
    }

    public static synchronized void setSelectedMode(GameMode mode) {
        selectedMode = mode == null ? GameMode.MEDIUM : mode;
    }

    public static synchronized boolean isShowGuardCones() {
        return showGuardCones;
    }

    public static synchronized void setShowGuardCones(boolean showGuardConesValue) {
        showGuardCones = showGuardConesValue;
    }

    public static synchronized boolean isLoggedIn() {
        return loggedIn;
    }

    public static synchronized void setLoggedIn(boolean loggedInValue) {
        loggedIn = loggedInValue;
    }

    public static synchronized String getOperatorAlias() {
        return operatorAlias;
    }

    public static synchronized void setOperatorAlias(String alias) {
        String normalized = alias == null ? "" : alias.trim();
        operatorAlias = normalized.isEmpty() ? "Operator" : normalized;
    }

    public static synchronized void startRun() {
        runLoot.clear();
    }

    public static synchronized void addRunLoot(StolenArtRecord loot) {
        if (loot != null) {
            runLoot.add(loot);
        }
    }

    public static synchronized int getRunLootCount() {
        return runLoot.size();
    }

    public static synchronized int getRunLootValue() {
        return runLoot.stream().mapToInt(StolenArtRecord::value).sum();
    }

    public static synchronized int commitRunLoot() {
        int count = runLoot.size();
        vault.addAll(runLoot);
        runLoot.clear();
        return count;
    }

    public static synchronized int discardRunLoot() {
        int count = runLoot.size();
        runLoot.clear();
        return count;
    }

    public static synchronized List<StolenArtRecord> getVaultSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(vault));
    }

    public static synchronized int getVaultValue() {
        return vault.stream().mapToInt(StolenArtRecord::value).sum();
    }

    public static synchronized void clearVault() {
        vault.clear();
        runLoot.clear();
    }
}
