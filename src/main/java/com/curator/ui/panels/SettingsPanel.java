package com.curator.ui.panels;

import com.curator.domain.GameMode;
import com.curator.services.UserProfileRepository;
import com.curator.state.GameSession;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class SettingsPanel {

    public static void buildAndShow(MenuNavigator navigator, UserProfileRepository profileRepository) {
        navigator.setPanelTitle("Settings");

        var conesToggle = new CheckBox("Show Guard Torch Cones");
        conesToggle.setSelected(GameSession.isShowGuardCones());
        conesToggle.setTextFill(Color.rgb(232, 216, 184));
        conesToggle.setFont(Font.font("Cinzel", FontWeight.SEMI_BOLD, 22));
        conesToggle.setOnAction(e -> GameSession.setShowGuardCones(conesToggle.isSelected()));

        var qualityText = new Text("Visual Profile: Premium Menu + Dynamic Lighting + Heart Puzzle Flow");
        qualityText.setFont(Font.font("Bodoni MT", FontWeight.NORMAL, 22));
        qualityText.setFill(Color.rgb(182, 214, 250, 0.96));

        var profileTitle = new Text("Operator Identity");
        profileTitle.setFont(Font.font("Cinzel", FontWeight.SEMI_BOLD, 22));
        profileTitle.setFill(Color.rgb(244, 222, 178));

        var nameField = new TextField(GameSession.getOperatorAlias());
        nameField.setPromptText("Operator name");
        configureLoginField(nameField, 360);

        var nameStatus = new Text();
        nameStatus.setFont(Font.font("Bodoni MT", FontWeight.SEMI_BOLD, 18));
        nameStatus.setFill(Color.rgb(133, 225, 174));

        var saveName = MenuComponents.createPanelButton("Save Operator Name");
        saveName.setOnAction(e -> {
            String desired = nameField.getText() == null ? "" : nameField.getText().trim();
            if (desired.isBlank()) {
                nameStatus.setText("Operator name cannot be empty.");
                nameStatus.setFill(Color.rgb(255, 140, 140));
                return;
            }
            var session = GameSession.getAuthSession();
            if (session == null) {
                nameStatus.setText("Login required to update your profile.");
                nameStatus.setFill(Color.rgb(255, 140, 140));
                return;
            }

            nameStatus.setText("Updating profile in Firestore...");
            nameStatus.setFill(Color.rgb(133, 225, 174));
            profileRepository.updateDisplayName(session, desired)
                    .thenAccept(profile -> Platform.runLater(() -> {
                        GameSession.setOperatorAlias(profile.displayName());
                        nameStatus.setText("Profile updated.");
                        nameStatus.setFill(Color.rgb(125, 231, 183));
                        navigator.showHomePanel();
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            nameStatus.setText("Profile update failed.");
                            nameStatus.setFill(Color.rgb(255, 140, 140));
                        });
                        return null;
                    });
        });

        var restore = MenuComponents.createPanelButton("Restore Defaults");
        restore.setOnAction(e -> {
            GameSession.setSelectedMode(GameMode.MEDIUM);
            GameSession.setShowGuardCones(true);
            navigator.setSelectedMode(GameMode.MEDIUM);
            buildAndShow(navigator, profileRepository);
        });

        var box = new VBox(18, conesToggle, qualityText, profileTitle, nameField, saveName, nameStatus, restore);
        box.setPadding(new Insets(10, 12, 10, 12));
        navigator.swapContent(box);
    }

    private static void configureLoginField(TextField field, double width) {
        field.setFont(Font.font("Cinzel", FontWeight.SEMI_BOLD, 19));
        field.setMinWidth(width);
        field.setPrefWidth(width);
        field.setMaxWidth(width);
        field.setPrefHeight(44);
        field.setStyle("-fx-background-color: rgba(7,20,34,0.86);"
                + "-fx-text-fill: #efd9ad; -fx-prompt-text-fill: rgba(203,224,245,0.56);"
                + "-fx-border-color: rgba(148,205,253,0.55); -fx-border-radius: 9;"
                + "-fx-background-radius: 9;");
    }
}
