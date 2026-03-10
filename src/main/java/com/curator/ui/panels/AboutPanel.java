package com.curator.ui.panels;

import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class AboutPanel {

    public static void buildAndShow(MenuNavigator navigator) {
        navigator.setPanelTitle("About The Curator");

        var about = new Text(
                "University Assignment Build\n\n"
                        + "- Event-driven gameplay with FXGL\n"
                        + "- Interoperability with Art Institute API\n"
                        + "- Heart API puzzle challenge before each theft\n"
                        + "- Session library for recovered artworks\n"
                        + "- Difficulty-scaled missions (Easy / Medium / Hard)"
        );
        about.setFont(Font.font("Bodoni MT", FontWeight.NORMAL, 22));
        about.setFill(Color.rgb(186, 219, 252, 0.96));

        var box = new VBox(18, about);
        box.setPadding(new Insets(10, 12, 10, 12));
        navigator.swapContent(box);
    }
}
