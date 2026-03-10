package com.curator.ui.panels;

import com.curator.state.GameSession;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class HomePanel {

    public static void buildAndShow(MenuNavigator navigator) {
        navigator.setPanelTitle("Mission Brief");

        var intro = new Text(
                "Welcome, " + GameSession.getOperatorAlias() + ". Infiltrate the museum, solve Heart API puzzles,\n"
                        + "steal high-value art, and escape before security locks you in."
        );
        intro.setFill(Color.rgb(189, 220, 250, 0.95));
        intro.setFont(Font.font("Bodoni MT", FontWeight.NORMAL, 23));

        var modeText = new Text("Selected Mode: " + navigator.getSelectedMode().displayName());
        modeText.setFill(Color.rgb(245, 211, 145));
        modeText.setFont(Font.font("Cinzel", FontWeight.SEMI_BOLD, 25));

        var startButton = MenuComponents.createPanelButton("Start New Mission");
        startButton.setOnAction(e -> {
            GameSession.setSelectedMode(navigator.getSelectedMode());
            navigator.requestNewGame();
        });

        var openModes = MenuComponents.createPanelButton("Change Difficulty");
        openModes.setOnAction(e -> navigator.showNewGamePanel());
        var box = new VBox(18);
        box.setPadding(new Insets(10, 12, 10, 12));
        box.getChildren().addAll(intro, modeText);

        if (GameSession.isLoggedIn()) {
            var session = GameSession.getAuthSession();
            String identity = session != null && !session.email().isBlank()
                    ? session.email()
                    : GameSession.getOperatorAlias();

            var authHint = new Text("Authenticated as " + identity + ".");
            authHint.setFill(Color.rgb(145, 214, 255));
            authHint.setFont(Font.font("Bodoni MT", FontWeight.NORMAL, 21));
            box.getChildren().add(authHint);
        } else {
            var authHint = new Text("Authentication required before launching a mission.");
            authHint.setFill(Color.rgb(255, 185, 135));
            authHint.setFont(Font.font("Bodoni MT", FontWeight.NORMAL, 21));

            var authButton = MenuComponents.createPanelButton("Authenticate Operator");
            authButton.setOnAction(e -> navigator.requestAuthentication(() -> {
            }));

            box.getChildren().addAll(authHint, authButton);
        }

        box.getChildren().add(new HBox(14, startButton, openModes));
        navigator.swapContent(box);
    }
}
