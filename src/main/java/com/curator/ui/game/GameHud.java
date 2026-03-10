package com.curator.ui.game;

import com.almasb.fxgl.dsl.FXGL;
import com.curator.domain.GameMode;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

// Extracted HUD keeps gameplay UI separate from the main menu structure.
public class GameHud {

    public void attach(GameMode mode) {
        Rectangle panel = new Rectangle(430, 176, Color.color(0.06, 0.10, 0.16, 0.38));
        panel.setArcWidth(16);
        panel.setArcHeight(16);
        panel.setStroke(Color.color(0.40, 0.80, 1.0, 0.45));
        panel.setStrokeWidth(1.2);
        panel.setEffect(new GaussianBlur(5));
        FXGL.addUINode(panel, 20, 16);

        var modeText = FXGL.getUIFactoryService().newText("MODE  " + mode.displayName().toUpperCase(), Color.rgb(255, 209, 142), 20);
        FXGL.addUINode(modeText, 34, 30);

        var timeText = FXGL.getUIFactoryService().newText("", Color.rgb(142, 231, 255), 24);
        timeText.textProperty().bind(FXGL.getdp("time").asString("TIME LEFT  %.0fs"));
        FXGL.addUINode(timeText, 34, 58);

        var scoreText = FXGL.getUIFactoryService().newText("", Color.rgb(255, 233, 168), 20);
        scoreText.textProperty().bind(FXGL.getip("score").asString("VAULT VALUE  $%dM"));
        FXGL.addUINode(scoreText, 34, 90);

        var quotaText = FXGL.getUIFactoryService().newText("", Color.rgb(214, 236, 255), 18);
        quotaText.textProperty().bind(FXGL.getip("stolenCount").asString("RECOVERED  %d / " + mode.requiredArtCount()));
        FXGL.addUINode(quotaText, 34, 120);

        var staminaText = FXGL.getUIFactoryService().newText("", Color.rgb(153, 255, 182), 16);
        staminaText.textProperty().bind(FXGL.getdp("stamina").asString("STAMINA  %.0f%%"));
        FXGL.addUINode(staminaText, 252, 30);

        var stanceText = FXGL.getUIFactoryService().newText("", Color.rgb(255, 181, 186), 16);
        stanceText.textProperty().bind(FXGL.getWorldProperties().stringProperty("stance").concat(" MOVEMENT"));
        FXGL.addUINode(stanceText, 252, 56);

        var objective = FXGL.getUIFactoryService().newText("OBJECTIVE  " + mode.objectiveText(), Color.rgb(230, 230, 230), 15);
        FXGL.addUINode(objective, 34, 150);
    }
}
