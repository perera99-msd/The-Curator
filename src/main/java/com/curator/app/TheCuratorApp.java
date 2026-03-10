package com.curator.app;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.SceneFactory;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.curator.domain.ArtData;
import com.curator.domain.GameMode;
import com.curator.domain.HeistReport;
import com.curator.domain.StolenArtRecord;
import com.curator.gameplay.EntityType;
import com.curator.gameplay.MuseumFactory;
import com.curator.gameplay.components.PatrolComponent;
import com.curator.services.ArtProvider;
import com.curator.services.HeistReportRepository;
import com.curator.services.PuzzleProvider;
import com.curator.services.StolenArtRepository;
import com.curator.state.GameSession;
import com.curator.ui.HackingSubScene;
import com.curator.ui.PremiumMainMenu;
import com.curator.ui.game.GameHud;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.effect.BlendMode;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

public class TheCuratorApp extends GameApplication {

    private static final int APP_WIDTH = 1280;
    private static final int APP_HEIGHT = 720;
    private static final int TILE_SIZE = 40;
    private static final double ACTOR_RADIUS = 16.0;
    private static final String[] MUSEUM_MAP = {
            "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB",
            "B..............................B",
            "B..WWWW..A.....WW.....A..WWWW..B",
            "B..W..W........WW........W..W..B",
            "B..W..WWWW..W......W..WWWW..W..B",
            "B...........W..AA..W...........B",
            "B..WWWW..W..W......W..W..WWWW..B",
            "E..W......W....WW....W......W..F",
            "B..W..A...WW........WW...A..W..B",
            "B..W......W....WW....W......W..B",
            "B..WWWW..W..W......W..W..WWWW..B",
            "B...........W..AA..W...........B",
            "B..W..WWWW..W......W..WWWW..W..B",
            "B..W..W........WW........W..W..B",
            "B..WWWW..A.....WW.....A..WWWW..B",
            "B..............................B",
            "B..............S...............B",
            "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB"
    };

    private final ServiceRegistry services = ServiceRegistry.createDefault();
    private final ArtProvider artProvider = services.artProvider();
    private final PuzzleProvider puzzleProvider = services.puzzleProvider();
    private final HeistReportRepository reportRepository = services.reportRepository();
    private final StolenArtRepository stolenArtRepository = services.stolenArtRepository();

    private Entity player;
    private GameMode mode = GameMode.MEDIUM;
    private boolean puzzleActive;
    private boolean missionEnded;
    private double playerStep = 5.0; // Increased for fluid gameplay
    private boolean sprinting;
    private boolean sneaking;
    private double stamina = 100.0;
    private double movementPulseSeconds = 1.0;
    private final List<Point2D> artSpots = new ArrayList<>();
    private String[] activeMap = MUSEUM_MAP;
    private Point2D startMarker = new Point2D(APP_WIDTH / 2.0, APP_HEIGHT - 2.0 * TILE_SIZE);
    private final Set<Character> blockedTiles = new HashSet<>(Set.of('W', 'E', 'F', 'L'));

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(APP_WIDTH);
        settings.setHeight(APP_HEIGHT);
        settings.setFullScreenAllowed(false);
        settings.setFullScreenFromStart(false);
        settings.setManualResizeEnabled(true);
        settings.setPreserveResizeRatio(true);
        settings.setScaleAffectedOnResize(true);
        settings.setTitle("The Curator | Open Museum");
        settings.setVersion("5.0 Director's Cut");
        settings.setMainMenuEnabled(true);
        settings.setGameMenuEnabled(true);
        settings.setSceneFactory(new SceneFactory() {
            @Override
            public FXGLMenu newMainMenu() {
                return new PremiumMainMenu(services.authService(),
                        services.userProfileRepository(),
                        services.stolenArtRepository());
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
        vars.put("stamina", 100.0);
        vars.put("stance", "NORMAL");
    }

    @Override
    protected void initGame() {
        mode = GameSession.getSelectedMode();
        GameSession.startRun();
        missionEnded = false;
        puzzleActive = false;
        sprinting = false;
        sneaking = false;
        stamina = 100.0;
        movementPulseSeconds = 1.0;

        playerStep = switch (mode) {
            case EASY -> 5.5;
            case MEDIUM -> 5.0;
            case HARD -> 4.5;
        };

        FXGL.getGameWorld().addEntityFactory(new MuseumFactory());

        buildMuseumBackdrop();

        buildLevel();
        spawnGuards();
        spawnArtFromMuseumApi();
    }

    @Override
    protected void initUI() {
        new GameHud().attach(mode);
    }

    @Override
    protected void onUpdate(double tpf) {
        if (missionEnded) return;

        FXGL.inc("time", -tpf);
        updateMovementState(tpf);
        if (FXGL.getd("time") <= 0) {
            failMission("TIME EXPIRED. TEAM EXTRACT FAILED.", true);
        }
        
        checkGuardVision();
    }

    private void checkGuardVision() {
        double distanceLimit = sneaking ? 100 : 145;
        double angleLimit = sneaking ? 25 : 32;
        var guards = FXGL.getGameWorld().getEntitiesByType(EntityType.GUARD);
        for (Entity guard : guards) {
            if (guard.distance(player) < distanceLimit) {
                Point2D start = guard.getCenter();
                Point2D end = player.getCenter();
                Point2D dir = end.subtract(start);
                
                double angleToPlayer = Math.toDegrees(Math.atan2(dir.getY(), dir.getX()));
                double guardAngle = guard.getRotation();
                
                double diff = Math.abs(angleToPlayer - guardAngle) % 360;
                if (diff > 180) diff = 360 - diff;
                
                if (diff < angleLimit) {
                    boolean blocked = false;
                    double dist = start.distance(end);
                    Point2D step = dir.normalize().multiply(2);
                    
                    Point2D current = start;
                    for (double d = 0; d < dist; d += 2) {
                        current = start.add(dir.normalize().multiply(d));
                        final Point2D p = current;
                        // Precise check against the physical bounds of walls and doors
                        boolean hitWall = FXGL.getGameWorld()
                                .getEntitiesByType(EntityType.WALL, EntityType.BORDER_WALL, EntityType.LOCKED_DOOR, EntityType.FAKE_EXIT)
                                .stream()
                                .anyMatch(w -> {
                                    var bbox = w.getBoundingBoxComponent();
                                    return p.getX() >= bbox.getMinXWorld() && p.getX() <= bbox.getMaxXWorld()
                                        && p.getY() >= bbox.getMinYWorld() && p.getY() <= bbox.getMaxYWorld();
                                });
                        if (hitWall) {
                            blocked = true;
                            break;
                        }
                    }
                    if (!blocked) {
                        failMission("DETECTED BY SECURITY TORCH.", false);
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void initPhysics() {
        FXGL.getPhysicsWorld().setGravity(0, 0);

        FXGL.onCollisionBegin(EntityType.PLAYER, EntityType.GUARD, (p, g) -> failMission("DETECTED BY SECURITY TORCH.", false));
    }

    @Override
    protected void initInput() {
        FXGL.onKey(KeyCode.W, () -> movePlayer(0, -playerStep));
        FXGL.onKey(KeyCode.S, () -> movePlayer(0, playerStep));
        FXGL.onKey(KeyCode.A, () -> movePlayer(-playerStep, 0));
        FXGL.onKey(KeyCode.D, () -> movePlayer(playerStep, 0));

        FXGL.onKey(KeyCode.UP, () -> movePlayer(0, -playerStep));
        FXGL.onKey(KeyCode.DOWN, () -> movePlayer(0, playerStep));
        FXGL.onKey(KeyCode.LEFT, () -> movePlayer(-playerStep, 0));
        FXGL.onKey(KeyCode.RIGHT, () -> movePlayer(playerStep, 0));

        FXGL.getInput().addAction(new com.almasb.fxgl.input.UserAction("Sprint") {
            @Override
            protected void onActionBegin() {
                sprinting = true;
            }

            @Override
            protected void onActionEnd() {
                sprinting = false;
            }
        }, KeyCode.Z);

        FXGL.getInput().addAction(new com.almasb.fxgl.input.UserAction("Sneak") {
            @Override
            protected void onActionBegin() {
                sneaking = true;
            }

            @Override
            protected void onActionEnd() {
                sneaking = false;
            }
        }, KeyCode.C);
        
        FXGL.onKeyDown(KeyCode.ENTER, () -> {
            if (missionEnded || puzzleActive) return;
            
            Entity nearestArt = FXGL.getGameWorld().getClosestEntity(player, e -> e.getType() == EntityType.ART)
                                    .filter(e -> e.distance(player) < 70).orElse(null);
                                    
            Entity nearestDoor = FXGL.getGameWorld().getClosestEntity(player, 
                    e -> e.getType() == EntityType.EXIT || e.getType() == EntityType.LOCKED_DOOR || e.getType() == EntityType.FAKE_EXIT)
                    .filter(e -> e.distance(player) < 70).orElse(null);

            if (nearestArt != null) {
                startPuzzle(nearestArt);
            } else if (nearestDoor != null) {
                if (nearestDoor.getType() == EntityType.EXIT) {
                    if (FXGL.geti("stolenCount") >= mode.requiredArtCount()) {
                        completeMission();
                    } else {
                        FXGL.getNotificationService().pushNotification(
                                "EXIT LOCKED: need " + mode.requiredArtCount() + " artworks.");
                    }
                } else if (nearestDoor.getType() == EntityType.FAKE_EXIT) {
                    FXGL.getNotificationService().pushNotification("WRONG DOOR: This exit is locked from the outside!");
                } else {
                    FXGL.getNotificationService().pushNotification("This interior door is locked.");
                }
            } else {
                FXGL.getNotificationService().pushNotification("Nothing to interact with nearby.");
            }
        });
    }
    
    private void movePlayer(double dx, double dy) {
        movementPulseSeconds = 0.0;

        double multiplier = sneaking ? 0.63 : (sprinting && stamina > 1.0 ? 1.55 : 1.0);
        dx *= multiplier;
        dy *= multiplier;

        if (dx != 0 || dy != 0) {
            player.setRotation(Math.toDegrees(Math.atan2(dy, dx)));
        }
        Point2D current = new Point2D(player.getX(), player.getY());
        Point2D fullStep = current.add(dx, dy);

        if (canOccupyWorld(fullStep, ACTOR_RADIUS)) {
            player.setPosition(fullStep);
            return;
        }

        Point2D xOnly = current.add(dx, 0);
        if (canOccupyWorld(xOnly, ACTOR_RADIUS)) {
            player.setPosition(xOnly);
            return;
        }

        Point2D yOnly = current.add(0, dy);
        if (canOccupyWorld(yOnly, ACTOR_RADIUS)) {
            player.setPosition(yOnly);
        }
    }

    private void updateMovementState(double tpf) {
        movementPulseSeconds += tpf;
        boolean isMoving = movementPulseSeconds < 0.09;

        if (sprinting && !sneaking && isMoving) {
            stamina = Math.max(0.0, stamina - (30.0 * tpf));
            if (stamina <= 0.5) {
                sprinting = false;
            }
        } else {
            stamina = Math.min(100.0, stamina + (18.0 * tpf));
        }

        FXGL.set("stamina", stamina);
        FXGL.set("stance", sneaking ? "SNEAK" : (sprinting && stamina > 1.0 ? "SPRINT" : "NORMAL"));
    }

    private void buildMuseumBackdrop() {
        Rectangle base = new Rectangle(APP_WIDTH, APP_HEIGHT);
        base.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#121622")),
                new Stop(0.55, Color.web("#0A0E17")),
                new Stop(1.0, Color.web("#05070D"))));
        FXGL.entityBuilder().at(0, 0).view(base).zIndex(-300).buildAndAttach();

        for (int y = 0; y < APP_HEIGHT; y += TILE_SIZE) {
            for (int x = 0; x < APP_WIDTH; x += TILE_SIZE) {
                Rectangle tile = new Rectangle(TILE_SIZE, TILE_SIZE);
                boolean isDark = ((x / TILE_SIZE) + (y / TILE_SIZE)) % 2 == 0;
                tile.setFill(isDark ? Color.web("#151B29", 0.62) : Color.web("#1E2638", 0.5));
                tile.setStroke(Color.web("#0B0F17", 0.45));
                tile.setStrokeWidth(0.8);
                FXGL.entityBuilder().at(x, y).view(tile).zIndex(-295).buildAndAttach();
            }
        }

        for (int i = 0; i < 6; i++) {
            Circle light = new Circle(170);
            light.setFill(new RadialGradient(
                    0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.color(0.72, 0.83, 1.0, 0.17)),
                    new Stop(0.6, Color.color(0.35, 0.45, 0.9, 0.08)),
                    new Stop(1.0, Color.color(0.2, 0.27, 0.6, 0.0))
            ));
            light.setBlendMode(BlendMode.ADD);
            light.setTranslateX(170 + (i % 3) * 440);
            light.setTranslateY(150 + (i / 3) * 320);
            light.setOpacity(0.35);
            FXGL.entityBuilder().at(0, 0).view(light).zIndex(-290).buildAndAttach();

            Timeline pulse = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(light.radiusProperty(), 160),
                            new KeyValue(light.opacityProperty(), 0.27)),
                    new KeyFrame(Duration.seconds(2.3 + (i * 0.2)),
                            new KeyValue(light.radiusProperty(), 210),
                            new KeyValue(light.opacityProperty(), 0.44))
            );
            pulse.setCycleCount(Timeline.INDEFINITE);
            pulse.setAutoReverse(true);
            pulse.play();
        }

        Rectangle vignette = new Rectangle(APP_WIDTH, APP_HEIGHT);
        vignette.setFill(new RadialGradient(
                0, 0, 0.5, 0.5, 0.8, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(0, 0, 0, 0.0)),
                new Stop(0.75, Color.color(0, 0, 0, 0.25)),
                new Stop(1.0, Color.color(0, 0, 0, 0.5))
        ));
        FXGL.entityBuilder().at(0, 0).view(vignette).zIndex(-285).buildAndAttach();
    }

    private void buildLevel() {
        artSpots.clear();
        startMarker = new Point2D(APP_WIDTH / 2.0, APP_HEIGHT - 2.0 * TILE_SIZE);

        for (int y = 0; y < activeMap.length; y++) {
            for (int x = 0; x < activeMap[y].length(); x++) {
                char c = activeMap[y].charAt(x);
                int worldX = x * TILE_SIZE;
                int worldY = y * TILE_SIZE;
                if (c == 'W') {
                    FXGL.spawn("wall", worldX, worldY);
                } else if (c == 'B') {
                    SpawnData borderData = new SpawnData(worldX, worldY);
                    boolean topOrBottom = y == 0 || y == activeMap.length - 1;
                    borderData.put("orientation", topOrBottom ? "H" : "V");
                    FXGL.spawn("borderWall", borderData);
                } else if (c == 'F') {
                    FXGL.spawn("fakeExitDoor", worldX, worldY);
                } else if (c == 'E') {
                    FXGL.spawn("exitDoor", worldX, worldY);
                } else if (c == 'S') {
                    FXGL.spawn("startDoor", worldX, worldY);
                    startMarker = tileCenter(x, y);
                } else if (c == 'A') {
                    artSpots.add(new Point2D(worldX, worldY));
                }
            }
        }

        Point2D safeSpawn = findSafeSpawn(startMarker);
        player = FXGL.spawn("player", safeSpawn.getX(), safeSpawn.getY());
    }

    private void spawnGuards() {
        List<Point2D[]> patrols = List.of(
                new Point2D[]{tileCenter(2, 1), tileCenter(29, 1)},
                new Point2D[]{tileCenter(29, 15), tileCenter(2, 15)},
                new Point2D[]{tileCenter(2, 2), tileCenter(2, 15)},
                new Point2D[]{tileCenter(29, 2), tileCenter(29, 15)},
                new Point2D[]{tileCenter(14, 5), tileCenter(14, 12)}
        );

        int spawned = 0;
        for (int i = 0; i < patrols.size() && spawned < mode.guardCount(); i++) {
            var path = patrols.get(i);
            if (!isPathNavigable(path, ACTOR_RADIUS)) {
                continue;
            }
            Entity guard = FXGL.spawn("guard", path[0].getX(), path[0].getY());
            guard.addComponent(new PatrolComponent(
                    (mode.guardSpeed() * 0.85) + (spawned * 5),
                    point -> canOccupyWorld(point, ACTOR_RADIUS),
                    path
            ));
            spawned++;
        }
    }

    private Point2D tileCenter(int tileX, int tileY) {
        return new Point2D(tileX * TILE_SIZE + TILE_SIZE / 2.0, tileY * TILE_SIZE + TILE_SIZE / 2.0);
    }

    private Point2D findSafeSpawn(Point2D preferredWorld) {
        int baseX = (int) (preferredWorld.getX() / TILE_SIZE);
        int baseY = (int) (preferredWorld.getY() / TILE_SIZE);
        int maxRadius = Math.max(activeMap.length, activeMap[0].length());

        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != radius) {
                        continue;
                    }
                    int tx = baseX + dx;
                    int ty = baseY + dy;
                    if (!isWalkableTile(tx, ty)) {
                        continue;
                    }
                    Point2D candidate = tileCenter(tx, ty);
                    if (canOccupyWorld(candidate, ACTOR_RADIUS) && hasEscapeRoute(tx, ty)) {
                        return candidate;
                    }
                }
            }
        }

        return tileCenter(1, 1);
    }

    private boolean hasEscapeRoute(int tileX, int tileY) {
        int walkableNeighbors = 0;
        int[][] neighbors = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] dir : neighbors) {
            if (isWalkableTile(tileX + dir[0], tileY + dir[1])) {
                walkableNeighbors++;
            }
        }
        return walkableNeighbors >= 2;
    }

    private boolean isPathNavigable(Point2D[] path, double radius) {
        if (path.length < 2) {
            return false;
        }

        for (Point2D point : path) {
            if (!canOccupyWorld(point, radius)) {
                return false;
            }
        }

        for (int i = 0; i < path.length; i++) {
            Point2D start = path[i];
            Point2D end = path[(i + 1) % path.length];
            Point2D delta = end.subtract(start);
            double distance = delta.magnitude();
            if (distance == 0) {
                continue;
            }
            Point2D direction = delta.normalize();
            for (double step = 0; step <= distance; step += 4.0) {
                Point2D sample = start.add(direction.multiply(step));
                if (!canOccupyWorld(sample, radius)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean canOccupyWorld(Point2D center, double radius) {
        if (center.getX() < radius || center.getY() < radius
                || center.getX() > APP_WIDTH - radius || center.getY() > APP_HEIGHT - radius) {
            return false;
        }

        double[][] offsets = {
                {0, 0},
                {radius, 0},
                {-radius, 0},
                {0, radius},
                {0, -radius},
                {radius * 0.7, radius * 0.7},
                {-radius * 0.7, radius * 0.7},
                {radius * 0.7, -radius * 0.7},
                {-radius * 0.7, -radius * 0.7}
        };

        for (double[] offset : offsets) {
            int tileX = (int) ((center.getX() + offset[0]) / TILE_SIZE);
            int tileY = (int) ((center.getY() + offset[1]) / TILE_SIZE);
            if (!isWalkableTile(tileX, tileY)) {
                return false;
            }
        }

        return !isSolidEntityCollision(center, radius);
    }

    private boolean isSolidEntityCollision(Point2D center, double radius) {
        return FXGL.getGameWorld()
                .getEntitiesByType(EntityType.WALL, EntityType.BORDER_WALL, EntityType.LOCKED_DOOR, EntityType.EXIT, EntityType.FAKE_EXIT)
                .stream()
                .anyMatch(solid -> circleIntersectsAabb(center, radius, solid));
    }

    private boolean circleIntersectsAabb(Point2D center, double radius, Entity solid) {
        var bbox = solid.getBoundingBoxComponent();
        double closestX = clamp(center.getX(), bbox.getMinXWorld(), bbox.getMaxXWorld());
        double closestY = clamp(center.getY(), bbox.getMinYWorld(), bbox.getMaxYWorld());
        double dx = center.getX() - closestX;
        double dy = center.getY() - closestY;
        return (dx * dx + dy * dy) <= (radius * radius);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean isWalkableTile(int tileX, int tileY) {
        if (tileY < 0 || tileY >= activeMap.length || tileX < 0 || tileX >= activeMap[tileY].length()) {
            return false;
        }
        return !blockedTiles.contains(activeMap[tileY].charAt(tileX));
    }

    private void spawnArtFromMuseumApi() {
        artProvider.fetchArtworks(mode).thenAcceptAsync(artList ->
                FXGL.getExecutor().startAsyncFX(() -> spawnArtworks(artList)), FXGL.getExecutor());
    }

    private void spawnArtworks(List<ArtData> artList) {
        List<ArtData> source = new ArrayList<>(artList);
        while (source.size() < mode.artSpawnCount()) {
            source.add(new ArtData(
                    "Recovered Sketch #" + (source.size() + 1),
                    "Unknown Artist",
                    "",
                    ThreadLocalRandom.current().nextInt(mode.minArtValue(), mode.maxArtValue() + 1)
            ));
        }

        for (int i = 0; i < mode.artSpawnCount() && i < artSpots.size(); i++) {
            var art = source.get(i);
            var spot = artSpots.get(i);
            SpawnData data = new SpawnData(spot.getX(), spot.getY());
            data.put("imageUrl", art.imageUrl());
            data.put("title", art.title());
            data.put("artist", art.artist());
            data.put("value", art.value());
            FXGL.spawn("art", data);
        }
    }

    private void startPuzzle(Entity art) {
        puzzleActive = true;

        var scene = new HackingSubScene(puzzleProvider, mode, success -> {
            if (success) {
                int value = art.getInt("value");
                FXGL.inc("score", value);
                FXGL.inc("stolenCount", 1);
                GameSession.addRunLoot(StolenArtRecord.create(
                        art.getString("title"),
                        art.getString("artist"),
                        value,
                        mode,
                        art.getString("imageUrl")
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
        if (missionEnded) return;

        missionEnded = true;
        List<StolenArtRecord> stolenThisRun = GameSession.getRunLootSnapshot();
        int savedCount = GameSession.commitRunLoot();
        int value = FXGL.geti("score");
        String message = "MISSION COMPLETE.\nRecovered: " + savedCount + " artworks\nVault Value: $" + value + "M";
        submitHeistReport("SUCCESS");
        submitStolenArt(stolenThisRun);
        FXGL.getDialogService().showMessageBox(message, () -> FXGL.getGameController().gotoMainMenu());
    }

    private void failMission(String reason, boolean timeExpired) {
        if (missionEnded) return;

        missionEnded = true;
        int lost = GameSession.discardRunLoot();
        String message = reason + "\nMission failed. Lost artworks: " + lost;
        if (timeExpired) {
            submitHeistReport("TIMEOUT");
        }
        FXGL.getDialogService().showMessageBox(message, () -> FXGL.getGameController().gotoMainMenu());
    }

    private void submitHeistReport(String outcome) {
        var session = GameSession.getAuthSession();
        HeistReport report = HeistReport.of(session, FXGL.geti("score"), outcome);

        reportRepository.submitReport(report, session)
                .thenRunAsync(() -> FXGL.getExecutor().startAsyncFX(
                        () -> FXGL.getNotificationService().pushNotification("Heist report synced to cloud.")), FXGL.getExecutor())
                .exceptionally(ex -> {
                    FXGL.getExecutor().startAsyncFX(
                            () -> FXGL.getNotificationService().pushNotification("Heist report upload failed."));
                    return null;
                });
    }

    private void submitStolenArt(List<StolenArtRecord> records) {
        if (records == null || records.isEmpty()) return;
        var session = GameSession.getAuthSession();
        stolenArtRepository.saveStolenArt(session, records)
                .thenRunAsync(() -> FXGL.getExecutor().startAsyncFX(
                        () -> FXGL.getNotificationService().pushNotification("Vault synced to cloud.")), FXGL.getExecutor())
                .exceptionally(ex -> {
                    FXGL.getExecutor().startAsyncFX(
                            () -> FXGL.getNotificationService().pushNotification("Vault sync failed."));
                    return null;
                });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
