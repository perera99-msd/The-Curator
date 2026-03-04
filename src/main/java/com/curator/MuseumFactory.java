package com.curator;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.texture.Texture;
import com.curator.state.GameSession;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

public class MuseumFactory implements EntityFactory {

    @Spawns("player")
    public Entity newPlayer(SpawnData data) {
        var body = new Rectangle(28, 36, Color.rgb(42, 62, 84));
        body.setArcWidth(8);
        body.setArcHeight(8);
        body.setStroke(Color.rgb(156, 194, 234));
        body.setStrokeWidth(1.3);

        var visor = new Rectangle(20, 8, Color.rgb(86, 160, 220));
        visor.setTranslateY(6);
        visor.setArcWidth(5);
        visor.setArcHeight(5);

        var plate = new StackPane(body, visor);
        plate.setEffect(new DropShadow(8, Color.rgb(0, 0, 0, 0.6)));

        return FXGL.entityBuilder(data)
                .type(EntityType.PLAYER)
                .viewWithBBox(plate)
                .with(new CollidableComponent(true))
                .build();
    }

    @Spawns("wall")
    public Entity newWall(SpawnData data) {
        var tile = new Rectangle(40, 40);
        tile.setFill(Color.rgb(20, 28, 38));
        tile.setStroke(Color.rgb(70, 95, 120, 0.75));
        tile.setStrokeWidth(1.0);
        tile.setEffect(new DropShadow(6, Color.rgb(0, 0, 0, 0.65)));

        return FXGL.entityBuilder(data)
                .type(EntityType.WALL)
                .viewWithBBox(tile)
                .build();
    }

    @Spawns("guard")
    public Entity newGuard(SpawnData data) {
        var body = new Rectangle(28, 36, Color.rgb(110, 25, 22));
        body.setArcWidth(8);
        body.setArcHeight(8);
        body.setStroke(Color.rgb(255, 168, 150));
        body.setStrokeWidth(1.2);

        var head = new Circle(8, Color.rgb(190, 60, 45));
        head.setTranslateY(-10);
        head.setEffect(new DropShadow(6, Color.rgb(0, 0, 0, 0.55)));

        var cone = new Polygon(
                14.0, 4.0,
                -32.0, 124.0,
                60.0, 124.0
        );
        cone.setFill(Color.rgb(255, 92, 80, GameSession.isShowGuardCones() ? 0.18 : 0.0));
        cone.setStroke(Color.rgb(255, 120, 110, GameSession.isShowGuardCones() ? 0.35 : 0.0));
        cone.setStrokeWidth(1.0);
        cone.setTranslateY(18);

        // Invisible sensor that acts like the torch detection area.
        var sensor = new Rectangle(94, 130, Color.rgb(255, 80, 70, 0.01));
        sensor.setTranslateX(-33);
        sensor.setTranslateY(16);

        return FXGL.entityBuilder(data)
                .type(EntityType.GUARD)
                .view(cone)
                .viewWithBBox(sensor)
                .view(body)
                .view(head)
                .with(new CollidableComponent(true))
                .build();
    }

    @Spawns("art")
    public Entity newArt(SpawnData data) {
        var frame = new Rectangle(72, 72, Color.rgb(118, 87, 36));
        frame.setStroke(Color.rgb(232, 205, 150));
        frame.setStrokeWidth(2.3);
        frame.setEffect(new DropShadow(8, Color.rgb(0, 0, 0, 0.5)));

        var inner = new Rectangle(64, 64, Color.rgb(40, 42, 52));
        inner.setTranslateX(4);
        inner.setTranslateY(4);

        var artNode = loadArtTexture(data);
        artNode.setTranslateX(6);
        artNode.setTranslateY(6);

        return FXGL.entityBuilder(data)
                .type(EntityType.ART)
                .view(frame)
                .view(inner)
                .viewWithBBox(artNode)
                .with(new CollidableComponent(true))
                .with("title", data.get("title"))
                .with("artist", data.get("artist"))
                .with("value", data.get("value"))
                .build();
    }

    @Spawns("startDoor")
    public Entity newStartDoor(SpawnData data) {
        var door = new Rectangle(40, 40, Color.rgb(34, 95, 42));
        door.setStroke(Color.rgb(165, 230, 170));
        door.setStrokeWidth(2);

        return FXGL.entityBuilder(data)
                .type(EntityType.START)
                .view(door)
                .build();
    }

    @Spawns("exitDoor")
    public Entity newExitDoor(SpawnData data) {
        var door = new Rectangle(42, 42, Color.rgb(90, 56, 28));
        door.setArcWidth(7);
        door.setArcHeight(7);
        door.setStroke(Color.rgb(225, 216, 188));
        door.setStrokeWidth(2.2);

        return FXGL.entityBuilder(data)
                .type(EntityType.EXIT)
                .viewWithBBox(door)
                .with(new CollidableComponent(true))
                .build();
    }

    private Texture loadArtTexture(SpawnData data) {
        try {
            String url = data.get("imageUrl");
            var image = new Image(url, 60, 60, true, true, false);

            if (image.isError()) {
                return FXGL.texture("fxgl_icon.png", 60, 60);
            }

            return new Texture(image);
        } catch (Exception e) {
            return FXGL.texture("fxgl_icon.png", 60, 60);
        }
    }
}
