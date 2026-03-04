package com.curator;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.SceneFactory;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.curator.components.PatrolComponent;
import com.curator.model.GameMode;
import com.curator.model.StolenArtRecord;
import com.curator.services.HeartService;
import com.curator.services.MuseumService;
import com.curator.state.GameSession;
import com.curator.ui.HackingSubScene;
import com.curator.ui.PremiumMainMenu;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class TheCuratorApp extends GameApplication {

    private static final int APP_WIDTH = 1280;
    private static final int APP_HEIGHT = 720;

    private final MuseumService museumService = new MuseumService();
    private final HeartService heartService = new HeartService();

    private Entity player;
    private GameMode mode = GameMode.MEDIUM;
    private boolean puzzleActive;
    private boolean missionEnded;
    private double playerStep = 4.1;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(APP_WIDTH);
        settings.setHeight(APP_HEIGHT);
        settings.setFullScreenAllowed(true);
        settings.setFullScreenFromStart(true);
        settings.setManualResizeEnabled(true);
        settings.setPreserveResizeRatio(true);
        settings.setScaleAffectedOnResize(true);
        settings.setTitle("The Curator | Agent 47");
        settings.setVersion("2.0 Ultra");
        settings.setMainMenuEnabled(true);
        settings.setGameMenuEnabled(true);
        settings.setSceneFactory(new SceneFactory() {
            @Override
            public FXGLMenu newMainMenu() {
                return new PremiumMainMenu();
            }
        });
    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        mode = GameSession.getSelectedMode();

        vars.put("time", (double) mode.missionTimeSeconds());
        vars.put("score", 0);
        vars.put("stolenCount", 0);
        vars.put("quota", mode.requiredArtCount());
        vars.put("modeName", mode.displayName().toUpperCase());
    }

    @Override
    protected void initGame() {
        mode = GameSession.getSelectedMode();
        GameSession.startRun();
        missionEnded = false;
        puzzleActive = false;

        playerStep = switch (mode) {
            case EASY -> 4.4;
            case MEDIUM -> 4.1;
            case HARD -> 3.8;
        };

        FXGL.getGameWorld().addEntityFactory(new MuseumFactory());

        FXGL.entityBuilder()
                .at(0, 0)
                .view(new Rectangle(APP_WIDTH, APP_HEIGHT, Color.rgb(8, 13, 22)))
                .buildAndAttach();

        buildLevel();
        spawnDoorsAndPlayer();
        spawnGuards();
        spawnArtFromMuseumApi();
    }

    @Override
    protected void initUI() {
        var modeText = FXGL.getUIFactoryService().newText("MODE: " + mode.displayName().toUpperCase(), Color.rgb(255, 209, 142), 22);
        FXGL.addUINode(modeText, 32, 26);

        var timeText = FXGL.getUIFactoryService().newText("", Color.rgb(142, 231, 255), 24);
        timeText.textProperty().bind(FXGL.getdp("time").asString("TIME: %.0f"));
        FXGL.addUINode(timeText, 32, 56);

        var scoreText = FXGL.getUIFactoryService().newText("", Color.rgb(255, 233, 168), 24);
        scoreText.textProperty().bind(FXGL.getip("score").asString("VAULT VALUE: $%d M"));
        FXGL.addUINode(scoreText, 32, 86);

        var quotaText = FXGL.getUIFactoryService().newText("", Color.rgb(214, 236, 255), 22);
        quotaText.textProperty().bind(FXGL.getip("stolenCount").asString("RECOVERED: %d / " + mode.requiredArtCount()));
        FXGL.addUINode(quotaText, 32, 116);

        var objective = FXGL.getUIFactoryService().newText("OBJECTIVE: " + mode.objectiveText(), Color.rgb(230, 230, 230), 16);
        FXGL.addUINode(objective, 32, 146);
    }

    @Override
    protected void onUpdate(double tpf) {
        if (missionEnded) {
            return;
        }

        FXGL.inc("time", -tpf);
        if (FXGL.getd("time") <= 0) {
            failMission("TIME EXPIRED. TEAM EXTRACT FAILED.");
        }
    }

    @Override
    protected void initPhysics() {
        FXGL.getPhysicsWorld().setGravity(0, 0);

        FXGL.onCollision(EntityType.PLAYER, EntityType.GUARD, (p, g) -> failMission("DETECTED BY SECURITY TORCH."));

        FXGL.onCollision(EntityType.PLAYER, EntityType.EXIT, (p, e) -> {
            if (missionEnded) {
                return;
            }

            if (FXGL.geti("stolenCount") >= mode.requiredArtCount()) {
                completeMission();
            } else {
                FXGL.getNotificationService().pushNotification(
                        "EXIT LOCKED: need " + mode.requiredArtCount() + " artworks.");
            }
        });

        FXGL.onCollisionBegin(EntityType.PLAYER, EntityType.ART, (p, art) -> {
            if (missionEnded || puzzleActive) {
                return;
            }
            startPuzzle(art);
        });
    }

    @Override
    protected void initInput() {
        FXGL.onKeyDown(KeyCode.F11, () -> {
            var stage = FXGL.getPrimaryStage();
            stage.setFullScreen(!stage.isFullScreen());
        });

        FXGL.onKey(KeyCode.W, () -> player.translateY(-playerStep));
        FXGL.onKey(KeyCode.S, () -> player.translateY(playerStep));
        FXGL.onKey(KeyCode.A, () -> player.translateX(-playerStep));
        FXGL.onKey(KeyCode.D, () -> player.translateX(playerStep));

        FXGL.onKey(KeyCode.UP, () -> player.translateY(-playerStep));
        FXGL.onKey(KeyCode.DOWN, () -> player.translateY(playerStep));
        FXGL.onKey(KeyCode.LEFT, () -> player.translateX(-playerStep));
        FXGL.onKey(KeyCode.RIGHT, () -> player.translateX(playerStep));
    }

    private void buildLevel() {
        // Outer shell
        for (int x = 0; x < APP_WIDTH; x += 40) {
            FXGL.spawn("wall", x, 0);
            FXGL.spawn("wall", x, APP_HEIGHT - 40);
        }
        for (int y = 40; y < APP_HEIGHT - 40; y += 40) {
            FXGL.spawn("wall", 0, y);
            FXGL.spawn("wall", APP_WIDTH - 40, y);
        }

        // Interior museum corridors
        spawnVerticalWall(240, 80, 12, 5, 6);
        spawnVerticalWall(560, 120, 11, 4, 8);
        spawnVerticalWall(880, 80, 12, 3, 9);

        spawnHorizontalWall(240, 160, 16, 5, 6);
        spawnHorizontalWall(240, 480, 16, 9, 10);
    }

    private void spawnDoorsAndPlayer() {
        FXGL.spawn("startDoor", 80, 600);
        player = FXGL.spawn("player", 84, 604);
        FXGL.spawn("exitDoor", 1160, 80);
    }

    private void spawnGuards() {
        List<Point2D[]> patrols = List.of(
                new Point2D[]{new Point2D(320, 620), new Point2D(520, 620), new Point2D(520, 300), new Point2D(320, 300)},
                new Point2D[]{new Point2D(700, 120), new Point2D(1020, 120), new Point2D(1020, 340), new Point2D(700, 340)},
                new Point2D[]{new Point2D(960, 580), new Point2D(1140, 580), new Point2D(1140, 220), new Point2D(960, 220)},
                new Point2D[]{new Point2D(420, 220), new Point2D(420, 420), new Point2D(200, 420), new Point2D(200, 220)},
                new Point2D[]{new Point2D(760, 520), new Point2D(760, 280), new Point2D(620, 280), new Point2D(620, 520)}
        );

        for (int i = 0; i < mode.guardCount() && i < patrols.size(); i++) {
            var path = patrols.get(i);
            Entity guard = FXGL.spawn("guard", path[0].getX(), path[0].getY());
            guard.addComponent(new PatrolComponent(mode.guardSpeed() + (i * 4.5), path));
        }
    }

    private void spawnArtFromMuseumApi() {
        museumService.fetchArtworks(mode).thenAcceptAsync(artList ->
                FXGL.getExecutor().startAsyncFX(() -> spawnArtworks(artList)), FXGL.getExecutor());
    }

    private void spawnArtworks(List<MuseumService.ArtData> artList) {
        int[][] artSpots = {
                {280, 120}, {360, 120}, {640, 160}, {760, 160}, {920, 160},
                {280, 440}, {640, 440}, {760, 440}, {920, 440}, {1040, 440}
        };

        List<MuseumService.ArtData> source = new ArrayList<>(artList);
        while (source.size() < mode.artSpawnCount()) {
            source.add(new MuseumService.ArtData(
                    "Recovered Sketch #" + (source.size() + 1),
                    "Unknown Artist",
                    "",
                    ThreadLocalRandom.current().nextInt(mode.minArtValue(), mode.maxArtValue() + 1)
            ));
        }

        for (int i = 0; i < mode.artSpawnCount() && i < artSpots.length; i++) {
            var art = source.get(i);
            SpawnData data = new SpawnData(artSpots[i][0], artSpots[i][1]);
            data.put("imageUrl", art.imageUrl());
            data.put("title", art.title());
            data.put("artist", art.artist());
            data.put("value", art.value());
            FXGL.spawn("art", data);
        }
    }

    private void startPuzzle(Entity art) {
        puzzleActive = true;

        var scene = new HackingSubScene(heartService, mode, success -> {
            if (success) {
                int value = art.getInt("value");
                FXGL.inc("score", value);
                FXGL.inc("stolenCount", 1);
                GameSession.addRunLoot(StolenArtRecord.create(
                        art.getString("title"),
                        art.getString("artist"),
                        value,
                        mode
                ));
                art.removeFromWorld();
                FXGL.getNotificationService().pushNotification(
                        "SECURED: " + art.getString("title") + " ($" + value + "M)");
            } else {
                FXGL.inc("time", -Math.min(12, mode.puzzleTimeSeconds() / 2.0));
                FXGL.getNotificationService().pushNotification(
                        "FAILED HACK: time penalty applied.");
            }

            puzzleActive = false;
        });

        FXGL.getSceneService().pushSubScene(scene);
        scene.startHack();
    }

    private void completeMission() {
        if (missionEnded) {
            return;
        }

        missionEnded = true;
        int savedCount = GameSession.commitRunLoot();
        int value = FXGL.geti("score");
        String message = "MISSION COMPLETE.\nRecovered: " + savedCount + " artworks\nVault Value: $" + value + "M";
        FXGL.getDialogService().showMessageBox(message, () -> FXGL.getGameController().gotoMainMenu());
    }

    private void failMission(String reason) {
        if (missionEnded) {
            return;
        }

        missionEnded = true;
        int lost = GameSession.discardRunLoot();
        String message = reason + "\nMission failed. Lost artworks: " + lost;
        FXGL.getDialogService().showMessageBox(message, () -> FXGL.getGameController().gotoMainMenu());
    }

    private void spawnVerticalWall(int x, int startY, int blocks, int openingA, int openingB) {
        for (int i = 0; i < blocks; i++) {
            if (i == openingA || i == openingB) {
                continue;
            }
            FXGL.spawn("wall", x, startY + (i * 40));
        }
    }

    private void spawnHorizontalWall(int startX, int y, int blocks, int openingA, int openingB) {
        for (int i = 0; i < blocks; i++) {
            if (i == openingA || i == openingB) {
                continue;
            }
            FXGL.spawn("wall", startX + (i * 40), y);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
