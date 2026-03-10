package com.curator.ui.panels;

import com.curator.domain.GameMode;
import com.curator.state.GameSession;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class NewGamePanel {

    public static void buildAndShow(MenuNavigator navigator) {
        navigator.setPanelTitle("Select Difficulty");

        var details = new Text();
        details.setFill(Color.rgb(178, 214, 251));
        details.setFont(Font.font("Bodoni MT", FontWeight.NORMAL, 20));

        var modes = new VBox(12);
        for (GameMode mode : GameMode.values()) {
            var button = MenuComponents.createPanelButton(mode.displayName());
            button.setOnAction(e -> {
                navigator.setSelectedMode(mode);
                GameSession.setSelectedMode(mode);
                details.setText(modeDetails(mode));
            });
            modes.getChildren().add(button);
        }

        details.setText(modeDetails(navigator.getSelectedMode()));

        var launch = MenuComponents.createPanelButton("Launch Mission");
        launch.setOnAction(e -> {
            GameSession.setSelectedMode(navigator.getSelectedMode());
            navigator.requestNewGame();
        });

        var box = new VBox(14, modes, details, launch);
        box.setPadding(new Insets(8, 12, 8, 12));
        navigator.swapContent(box);
    }

    private static String modeDetails(GameMode mode) {
        return mode.displayName() + " Mode\n"
                + "Time: " + mode.missionTimeSeconds() + " sec\n"
                + "Required Art: " + mode.requiredArtCount() + "\n"
                + "Guards: " + mode.guardCount() + " (speed " + (int) mode.guardSpeed() + ")\n"
                + "Puzzle: " + mode.puzzleTimeSeconds() + " sec, " + mode.puzzleAttempts() + " attempts\n"
                + "Art Value Range: $" + mode.minArtValue() + "M - $" + mode.maxArtValue() + "M";
    }
}
