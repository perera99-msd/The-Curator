package com.curator.ui.panels;

import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class MenuComponents {

    public static Button createPanelButton(String text) {
        String baseStyle = "-fx-background-color: linear-gradient(to right, rgba(16,38,66,0.80), rgba(12,30,52,0.52));"
                + "-fx-border-color: rgba(211,189,131,0.52); -fx-border-radius: 10; -fx-background-radius: 10;";
        String hoverStyle = "-fx-background-color: linear-gradient(to right, rgba(28,62,102,0.90), rgba(18,44,75,0.67));"
                + "-fx-border-color: rgba(243,214,151,0.82); -fx-border-radius: 10; -fx-background-radius: 10;";

        var button = new Button(text);
        button.setFont(Font.font("Cinzel", FontWeight.SEMI_BOLD, 20));
        button.setTextFill(Color.rgb(238, 223, 196));
        button.setPrefWidth(320);
        button.setPrefHeight(42);
        button.setStyle(baseStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));
        applyHoverMotion(button, 1.025);
        return button;
    }

    public static void applyHoverMotion(Button button, double targetScale) {
        var existingEnter = button.getOnMouseEntered();
        button.setOnMouseEntered(e -> {
            if (existingEnter != null) {
                existingEnter.handle(e);
            }
            var scaleUp = new ScaleTransition(Duration.seconds(0.16), button);
            scaleUp.setToX(targetScale);
            scaleUp.setToY(targetScale);
            scaleUp.setInterpolator(Interpolator.EASE_OUT);
            scaleUp.play();
        });

        var existingExit = button.getOnMouseExited();
        button.setOnMouseExited(e -> {
            if (existingExit != null) {
                existingExit.handle(e);
            }
            var scaleDown = new ScaleTransition(Duration.seconds(0.16), button);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);
            scaleDown.setInterpolator(Interpolator.EASE_OUT);
            scaleDown.play();
        });
    }
}
