package com.curator.gameplay;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.curator.state.GameSession;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MuseumFactory implements EntityFactory {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    @Spawns("player")
    public Entity newPlayer(SpawnData data) {
        ImageView avatar = loadAvatarView(72, "TheifAvatar.png", "thief_avatar.png", "player_avatar.png");
        
        var staticGroup = new javafx.scene.Group();
        Circle aura = new Circle(26);
        aura.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0.22, 0.93, 1.0, 0.22)),
                new Stop(1, Color.color(0.22, 0.93, 1.0, 0.0))));
        Circle base = new Circle(20, Color.TRANSPARENT);
        base.setStroke(Color.web("#00E5FF"));
        base.setStrokeWidth(2.5);
        base.setEffect(new DropShadow(20, Color.web("#00E5FF")));
        
        Circle shadow = new Circle(21, Color.color(0, 0, 0, 0.5));
        shadow.setTranslateY(4);

        if (avatar != null) {
            avatar.setTranslateX(-36);
            avatar.setTranslateY(-36);
            staticGroup.getChildren().addAll(shadow, aura, base, avatar);
        } else {
            Circle body = new Circle(18, Color.web("#1A2436"));
            staticGroup.getChildren().addAll(shadow, aura, base, body);
        }

        var directionalGroup = new javafx.scene.Group();
        Polygon visor = new Polygon(0, -10, 26, 0, 0, 10);
        visor.setFill(Color.web("#00E5FF"));
        visor.setEffect(new Glow(1.0));
        directionalGroup.getChildren().add(visor);

        var view = new javafx.scene.Group(staticGroup, directionalGroup);

        return FXGL.entityBuilder(data)
                .type(EntityType.PLAYER)
                .view(view)
                .bbox(new HitBox(new Point2D(-16, -16), BoundingShape.circle(16)))
                .anchorFromCenter()
                .with(new CollidableComponent(true))
                .with(new com.almasb.fxgl.entity.component.Component() {
                    private double pulse;

                    @Override
                    public void onUpdate(double tpf) {
                        pulse += tpf * 4.2;
                        staticGroup.setRotate(-entity.getRotation());
                        double glow = 0.7 + (Math.sin(pulse) * 0.3);
                        aura.setScaleX(0.95 + glow * 0.1);
                        aura.setScaleY(0.95 + glow * 0.1);
                        visor.setOpacity(0.55 + (Math.sin(pulse * 1.5) * 0.45));
                        entity.setZIndex((int) (entity.getY() + 20));
                    }
                })
                .build();
    }

    @Spawns("wall")
    public Entity newWall(SpawnData data) {
        Rectangle topFace = new Rectangle(40, 40);
        topFace.setFill(Color.web("#3A3E47"));
        topFace.setStroke(Color.web("#4F545C"));
        topFace.setStrokeWidth(1.0);
        topFace.setTranslateY(-40);

        Rectangle frontFace = new Rectangle(40, 40);
        frontFace.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#2A2D34")),
                new Stop(1, Color.web("#141518"))));
        Rectangle sideHighlight = new Rectangle(2, 40, Color.web("#A5B1C8", 0.18));
        sideHighlight.setTranslateX(38);
        Rectangle topRim = new Rectangle(40, 2, Color.web("#C0C8D6", 0.18));
        
        Rectangle baseboard = new Rectangle(40, 6);
        baseboard.setFill(Color.web("#0A0A0C"));
        baseboard.setTranslateY(34);

        var view = new javafx.scene.Group(frontFace, sideHighlight, topRim, baseboard, topFace);
        view.setEffect(new DropShadow(24, Color.color(0, 0, 0, 0.85)));

        return FXGL.entityBuilder(data)
                .type(EntityType.WALL)
                .view(view)
                .bbox(new HitBox(BoundingShape.box(40, 40)))
                .with(new CollidableComponent(true))
                .zIndex((int) data.getY())
                .build();
    }

    @Spawns("borderWall")
    public Entity newBorderWall(SpawnData data) {
        String orientation = data.get("orientation");
        boolean horizontal = !"V".equalsIgnoreCase(orientation);

        Rectangle base = new Rectangle(40, 40, Color.TRANSPARENT);
        Rectangle strip = horizontal
                ? new Rectangle(40, 12, new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#4A5368")),
                new Stop(1, Color.web("#222936"))))
                : new Rectangle(12, 40, new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#4A5368")),
                new Stop(1, Color.web("#222936"))));
        strip.setTranslateX(horizontal ? 0 : 0);
        strip.setTranslateY(horizontal ? 0 : 0);
        strip.setStroke(Color.web("#9BA9C2", 0.35));
        strip.setStrokeWidth(0.8);

        var view = new javafx.scene.Group(base, strip);
        view.setEffect(new DropShadow(12, Color.color(0, 0, 0, 0.7)));

        return FXGL.entityBuilder(data)
                .type(EntityType.BORDER_WALL)
                .view(view)
                .bbox(new HitBox(BoundingShape.box(horizontal ? 40 : 12, horizontal ? 12 : 40)))
                .with(new CollidableComponent(true))
                .zIndex((int) data.getY())
                .build();
    }
    
    @Spawns("lockedDoor")
    public Entity newLockedDoor(SpawnData data) {
        Rectangle topFace = new Rectangle(40, 40);
        topFace.setFill(Color.web("#4A1515"));
        topFace.setStroke(Color.web("#661A1A"));
        topFace.setStrokeWidth(1.0);
        topFace.setTranslateY(-40);

        Rectangle frontFace = new Rectangle(40, 40);
        frontFace.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#330A0A")),
                new Stop(1, Color.web("#1A0505"))));

        Circle lockIcon = new Circle(6, Color.web("#FF3333"));
        lockIcon.setTranslateX(20);
        lockIcon.setTranslateY(20);
        lockIcon.setEffect(new Glow(0.8));
        Rectangle frame = new Rectangle(36, 36, Color.TRANSPARENT);
        frame.setStroke(Color.web("#A33434", 0.75));
        frame.setStrokeWidth(1.4);
        frame.setTranslateX(2);
        frame.setTranslateY(2);

        var view = new javafx.scene.Group(frontFace, frame, lockIcon, topFace);
        view.setEffect(new DropShadow(20, Color.color(0, 0, 0, 0.8)));

        return FXGL.entityBuilder(data)
                .type(EntityType.LOCKED_DOOR)
                .view(view)
                .bbox(new HitBox(BoundingShape.box(40, 40)))
                .with(new CollidableComponent(true))
                .with(new com.almasb.fxgl.entity.component.Component() {
                    private double pulse;

                    @Override
                    public void onUpdate(double tpf) {
                        pulse += tpf * 3.2;
                        lockIcon.setScaleX(0.9 + (Math.sin(pulse) * 0.16));
                        lockIcon.setScaleY(0.9 + (Math.sin(pulse) * 0.16));
                    }
                })
                .zIndex((int) data.getY())
                .build();
    }

    private Entity buildExitDoor(SpawnData data, EntityType type, boolean isFake) {
        Rectangle topFace = new Rectangle(40, 40);
        topFace.setFill(Color.web("#1A3322"));
        topFace.setStroke(Color.web("#226633"));
        topFace.setStrokeWidth(1.0);
        topFace.setTranslateY(-40);

        Rectangle frontFace = new Rectangle(40, 40);
        frontFace.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0D1A11")),
                new Stop(1, Color.web("#050D08"))));

        Text exitText = new Text("EXIT");
        exitText.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
        exitText.setFill(isFake ? Color.web("#D8B75D") : Color.web("#33FF66"));
        exitText.setTranslateX(6);
        exitText.setTranslateY(24);
        exitText.setEffect(new Glow(1.0));
        Rectangle frame = new Rectangle(36, 36, Color.TRANSPARENT);
        frame.setStroke(Color.web("#3A8751", 0.68));
        frame.setStrokeWidth(1.2);
        frame.setTranslateX(2);
        frame.setTranslateY(2);

        var view = new javafx.scene.Group(frontFace, frame, exitText, topFace);
        view.setEffect(new DropShadow(20, Color.color(0, 0, 0, 0.8)));

        return FXGL.entityBuilder(data)
                .type(type)
                .view(view)
                .bbox(new HitBox(BoundingShape.box(40, 40)))
                .with(new CollidableComponent(true))
                .with("isFakeExit", isFake)
                .with(new com.almasb.fxgl.entity.component.Component() {
                    private double pulse;

                    @Override
                    public void onUpdate(double tpf) {
                        pulse += tpf * 2.4;
                        if (!isFake) {
                            exitText.setOpacity(0.72 + (Math.sin(pulse) * 0.28));
                        } else {
                            exitText.setOpacity(0.5 + (Math.sin(pulse * 0.7) * 0.2));
                        }
                    }
                })
                .zIndex((int) data.getY())
                .build();
    }

    @Spawns("fakeExitDoor")
    public Entity newFakeExitDoor(SpawnData data) {
        return buildExitDoor(data, EntityType.FAKE_EXIT, true);
    }

    @Spawns("exitDoor")
    public Entity newExitDoor(SpawnData data) {
        return buildExitDoor(data, EntityType.EXIT, false);
    }

    @Spawns("guard")
    public Entity newGuard(SpawnData data) {
        ImageView avatar = loadAvatarView(72, "GaurdAvatar.png", "guard_avatar.png");

        var staticGroup = new javafx.scene.Group();

        Circle base = new Circle(20, Color.TRANSPARENT);
        base.setStroke(Color.web("#FF0044"));
        base.setStrokeWidth(2.5);
        base.setEffect(new DropShadow(20, Color.web("#FF0044")));

        Circle shadow = new Circle(21, Color.color(0, 0, 0, 0.5));
        shadow.setTranslateY(4);

        if (avatar != null) {
            avatar.setTranslateX(-36);
            avatar.setTranslateY(-36);
            staticGroup.getChildren().addAll(shadow, base, avatar);
        } else {
            Circle body = new Circle(18, Color.web("#1A0505"));
            staticGroup.getChildren().addAll(shadow, base, body);
        }

        var directionalGroup = new javafx.scene.Group();
        Polygon visor = new Polygon(0, -10, 26, 0, 0, 10);
        visor.setFill(Color.web("#FF0044"));
        visor.setEffect(new Glow(1.0));
        Polygon scanner = new Polygon(0, -3, 10, 0, 0, 3, -3, 0);
        scanner.setFill(Color.web("#FF6A8A", 0.9));
        scanner.setTranslateX(15);
        scanner.setTranslateY(0);
        directionalGroup.getChildren().add(visor);
        directionalGroup.getChildren().add(scanner);

        Polygon cone = new Polygon(
            0, 0,
            135, -45,
            135, 45
        );
        cone.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(1, 0, 0, GameSession.isShowGuardCones() ? 0.35 : 0.0)),
                new Stop(1, Color.color(1, 0, 0, 0.0))));
        cone.setBlendMode(BlendMode.ADD);
        cone.setTranslateX(20);
        directionalGroup.getChildren().add(cone);

        var view = new javafx.scene.Group(staticGroup, directionalGroup);

        return FXGL.entityBuilder(data)
                .type(EntityType.GUARD)
                .view(view)
                .bbox(new HitBox(new Point2D(-16, -16), BoundingShape.circle(16)))
                .anchorFromCenter()
                .with(new CollidableComponent(true))
                .with(new com.almasb.fxgl.entity.component.Component() {
                    private double pulse;

                    @Override
                    public void onUpdate(double tpf) {
                        pulse += tpf * 3.0;
                        staticGroup.setRotate(-entity.getRotation());
                        scanner.setRotate(scanner.getRotate() + (80 * tpf));
                        visor.setOpacity(0.6 + (Math.sin(pulse) * 0.4));
                        entity.setZIndex((int) (entity.getY() + 20));
                    }
                })
                .build();
    }

    @Spawns("art")
    public Entity newArt(SpawnData data) {
        Circle spotlight = new Circle(28);
        spotlight.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(1.0, 0.9, 0.55, 0.18)),
                new Stop(1, Color.color(1.0, 0.9, 0.55, 0.0))));
        spotlight.setTranslateX(20);
        spotlight.setTranslateY(12);

        Rectangle pedestal = new Rectangle(36, 12);
        pedestal.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#2A2A2A")),
                new Stop(1, Color.web("#111111"))));
        pedestal.setTranslateX(2);
        pedestal.setTranslateY(28);

        Rectangle topFace = new Rectangle(36, 20);
        topFace.setFill(Color.web("#3A3A3A"));
        topFace.setTranslateX(2);
        topFace.setTranslateY(8);

        Rectangle canvas = new Rectangle(28, 28, Color.web("#000000"));
        canvas.setTranslateX(6);
        canvas.setTranslateY(0);
        
        Rectangle goldFrame = new Rectangle(30, 30, Color.TRANSPARENT);
        goldFrame.setStroke(Color.web("#FFD700"));
        goldFrame.setStrokeWidth(2.0);
        goldFrame.setTranslateX(5);
        goldFrame.setTranslateY(-1);
        goldFrame.setEffect(new DropShadow(10, Color.web("#FFD700", 0.4)));

        ImageView imageView = new ImageView();
        imageView.setFitWidth(28);
        imageView.setFitHeight(28);
        imageView.setTranslateX(6);
        imageView.setTranslateY(0);

        var view = new javafx.scene.Group(spotlight, pedestal, topFace, canvas, imageView, goldFrame);
        view.setEffect(new DropShadow(15, Color.color(0, 0, 0, 0.8)));

        String url = data.get("imageUrl");
        if (url != null && !url.isEmpty()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .GET()
                    .build();

            HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            Platform.runLater(() -> {
                                Image img = new Image(new ByteArrayInputStream(response.body()), 28, 28, true, true);
                                imageView.setImage(img);
                            });
                        }
                    });
        }

        return FXGL.entityBuilder(data)
                .type(EntityType.ART)
                .view(view)
                .bbox(new HitBox(BoundingShape.box(40, 40)))
                .with(new CollidableComponent(true))
                .with("title", data.get("title"))
                .with("artist", data.get("artist"))
                .with("value", data.get("value"))
                .with(new com.almasb.fxgl.entity.component.Component() {
                    private double pulse;

                    @Override
                    public void onUpdate(double tpf) {
                        pulse += tpf * 2.0;
                        goldFrame.setOpacity(0.75 + (Math.sin(pulse) * 0.25));
                        spotlight.setScaleX(0.95 + (Math.sin(pulse * 0.8) * 0.1));
                        spotlight.setScaleY(0.95 + (Math.sin(pulse * 0.8) * 0.1));
                    }
                })
                .zIndex((int) data.getY())
                .build();
    }

    @Spawns("startDoor")
    public Entity newStartDoor(SpawnData data) {
        Rectangle topFace = new Rectangle(40, 40);
        topFace.setFill(Color.web("#152238"));
        topFace.setStroke(Color.web("#203659"));
        topFace.setStrokeWidth(1.0);
        topFace.setTranslateY(-40);

        Rectangle frontFace = new Rectangle(40, 40);
        frontFace.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0A111C")),
                new Stop(1, Color.web("#05080E"))));

        Text startText = new Text("IN");
        startText.setFont(Font.font("Monospaced", FontWeight.BOLD, 16));
        startText.setFill(Color.web("#3399FF"));
        startText.setTranslateX(10);
        startText.setTranslateY(26);
        startText.setEffect(new Glow(1.0));
        Rectangle frame = new Rectangle(36, 36, Color.TRANSPARENT);
        frame.setStroke(Color.web("#4B79C4", 0.7));
        frame.setStrokeWidth(1.2);
        frame.setTranslateX(2);
        frame.setTranslateY(2);

        var view = new javafx.scene.Group(frontFace, frame, startText, topFace);
        view.setEffect(new DropShadow(20, Color.color(0, 0, 0, 0.8)));

        return FXGL.entityBuilder(data)
                .type(EntityType.START)
                .view(view)
                .bbox(new HitBox(BoundingShape.box(40, 40)))
                .with(new com.almasb.fxgl.entity.component.Component() {
                    private double pulse;

                    @Override
                    public void onUpdate(double tpf) {
                        pulse += tpf * 2.1;
                        startText.setOpacity(0.65 + (Math.sin(pulse) * 0.3));
                    }
                })
                .zIndex((int) data.getY() - 1)
                .build();
    }

    private ImageView loadAvatarView(double size, String... fileNames) {
        for (String fileName : fileNames) {
            try {
                var resource = getClass().getResource("/assets/textures/" + fileName);
                if (resource == null) {
                    continue;
                }
                Image img = new Image(resource.toExternalForm(), size, size, true, true);
                return new ImageView(img);
            } catch (Exception ignored) {
                // Try the next fallback avatar name.
            }
        }
        return null;
    }
}
