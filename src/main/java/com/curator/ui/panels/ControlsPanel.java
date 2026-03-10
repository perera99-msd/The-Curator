package com.curator.ui.panels;

import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class ControlsPanel {

    public static void buildAndShow(MenuNavigator navigator) {
        navigator.setPanelTitle("Controls");

        var controls = new Text(
                "W / A / S / D  : Move Agent\n"
                        + "ESC            : Pause / Menu\n"
                        + "F11            : Toggle Full Screen\n"
                        + "ENTER          : Trigger Interaction (Hack/Exit)\n\n"
                        + "Tip: avoid both guards and their torch cones."
        );
        controls.setFont(Font.font("Bodoni MT", FontWeight.NORMAL, 24));
        controls.setFill(Color.rgb(189, 221, 252, 0.96));

        var openModes = MenuComponents.createPanelButton("Choose Difficulty");
        openModes.setOnAction(e -> navigator.showNewGamePanel());

        var box = new VBox(22, controls, openModes);
        box.setPadding(new Insets(10, 12, 10, 12));
        navigator.swapContent(box);
    }
}
