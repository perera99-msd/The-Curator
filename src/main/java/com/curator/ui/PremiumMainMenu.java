package com.curator.ui;

import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import com.curator.model.GameMode;
import com.curator.model.StolenArtRecord;
import com.curator.state.GameSession;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;

public class PremiumMainMenu extends FXGLMenu {

    private static final String MENU_BG_PATH = "/assests/menu/menu_bg.png";
    private static final String THIEF_PATH = "/assests/menu/thief.png";

    private final List<Animation> animations = new ArrayList<>();
    private final Pane contentPanel = new Pane();
    private final Text panelTitle = new Text();

    private GameMode selectedMode = GameSession.getSelectedMode();

    public PremiumMainMenu() {
        super(MenuType.MAIN_MENU);
        buildMenu();
    }

    @Override
    public void onCreate() {
        animations.forEach(Animation::play);
    }

    @Override
    public void onDestroy() {
        animations.forEach(Animation::stop);
    }

    private void buildMenu() {
        int width = getAppWidth();
        int height = getAppHeight();

        var root = getContentRoot();
        root.getChildren().add(createBackground(width, height));
        root.getChildren().add(createTopHeader(width));
        root.getChildren().add(createNavigationPanel(height));
        root.getChildren().add(createContentSurface(width, height));

        showHomePanel();
    }

    private Node createBackground(int width, int height) {
        var fallbackBase = new Rectangle(width, height);
        fallbackBase.setFill(new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#060A10")),
                new Stop(0.5, Color.web("#0E2138")),
                new Stop(1.0, Color.web("#04070B"))
        ));

        var bg = new ImageView(new Image(MENU_BG_PATH));
        bg.setFitWidth(width);
        bg.setFitHeight(height);
        bg.setPreserveRatio(false);
        bg.setSmooth(true);

        var zoom = new ScaleTransition(Duration.seconds(24), bg);
        zoom.setFromX(1.0);
        zoom.setFromY(1.0);
        zoom.setToX(1.08);
        zoom.setToY(1.08);
        zoom.setAutoReverse(true);
        zoom.setCycleCount(Animation.INDEFINITE);
        zoom.setInterpolator(Interpolator.EASE_BOTH);
        animations.add(zoom);

        var pan = new TranslateTransition(Duration.seconds(19), bg);
        pan.setFromX(-18);
        pan.setFromY(-10);
        pan.setToX(20);
        pan.setToY(12);
        pan.setAutoReverse(true);
        pan.setCycleCount(Animation.INDEFINITE);
        pan.setInterpolator(Interpolator.EASE_BOTH);
        animations.add(pan);

        var thief = new ImageView(new Image(THIEF_PATH));
        thief.setPreserveRatio(true);
        thief.setFitHeight(height * 0.68);
        thief.setSmooth(true);
        thief.setTranslateX(width * 0.33);
        thief.setTranslateY(height * 0.10);

        var shadow = new Ellipse(width * 0.77, height * 0.82, 95, 28);
        shadow.setFill(Color.rgb(0, 0, 0, 0.36));
        shadow.setBlendMode(BlendMode.MULTIPLY);

        var thiefBob = new TranslateTransition(Duration.seconds(2.8), thief);
        thiefBob.setFromY(height * 0.10);
        thiefBob.setToY(height * 0.075);
        thiefBob.setAutoReverse(true);
        thiefBob.setCycleCount(Animation.INDEFINITE);
        thiefBob.setInterpolator(Interpolator.EASE_BOTH);
        animations.add(thiefBob);

        var thiefSway = new RotateTransition(Duration.seconds(4.8), thief);
        thiefSway.setFromAngle(-1.7);
        thiefSway.setToAngle(1.4);
        thiefSway.setAutoReverse(true);
        thiefSway.setCycleCount(Animation.INDEFINITE);
        thiefSway.setInterpolator(Interpolator.EASE_BOTH);
        animations.add(thiefSway);

        var shadowPulse = new ScaleTransition(Duration.seconds(2.8), shadow);
        shadowPulse.setFromX(1.0);
        shadowPulse.setToX(0.90);
        shadowPulse.setAutoReverse(true);
        shadowPulse.setCycleCount(Animation.INDEFINITE);
        shadowPulse.setInterpolator(Interpolator.EASE_BOTH);
        animations.add(shadowPulse);

        var sweep = new Rectangle(width * 0.42, height * 1.2);
        sweep.setFill(new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(120, 215, 255, 0.0)),
                new Stop(0.5, Color.rgb(120, 215, 255, 0.25)),
                new Stop(1.0, Color.rgb(120, 215, 255, 0.0))
        ));
        sweep.setRotate(-12);
        sweep.setBlendMode(BlendMode.SCREEN);
        sweep.setTranslateX(-width * 0.72);
        sweep.setTranslateY(-height * 0.10);

        var sweepMove = new TranslateTransition(Duration.seconds(10.2), sweep);
        sweepMove.setFromX(-width * 0.72);
        sweepMove.setToX(width * 0.98);
        sweepMove.setCycleCount(Animation.INDEFINITE);
        sweepMove.setInterpolator(Interpolator.EASE_BOTH);
        animations.add(sweepMove);

        var leftReadability = new Rectangle(width * 0.55, height);
        leftReadability.setFill(new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(0, 0, 0, 0.66)),
                new Stop(0.7, Color.rgb(0, 0, 0, 0.30)),
                new Stop(1.0, Color.rgb(0, 0, 0, 0.0))
        ));

        var ambience = new Rectangle(width, height);
        ambience.setFill(new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(38, 135, 255, 0.10)),
                new Stop(1.0, Color.rgb(255, 120, 45, 0.07))
        ));

        var ambiencePulse = new FadeTransition(Duration.seconds(5.0), ambience);
        ambiencePulse.setFromValue(0.76);
        ambiencePulse.setToValue(1.0);
        ambiencePulse.setCycleCount(Animation.INDEFINITE);
        ambiencePulse.setAutoReverse(true);
        ambiencePulse.setInterpolator(Interpolator.EASE_BOTH);
        animations.add(ambiencePulse);

        var vignette = new Rectangle(width, height);
        vignette.setFill(new RadialGradient(
                0, 0, 0.52, 0.5, 0.82, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(255, 255, 255, 0.0)),
                new Stop(1.0, Color.rgb(0, 0, 0, 0.70))
        ));

        var background = new StackPane(fallbackBase, bg, shadow, thief, sweep, leftReadability, ambience, vignette);
        background.setPrefSize(width, height);
        return background;
    }

    private Node createTopHeader(int width) {
        var topBar = new Rectangle(width, 102, Color.rgb(4, 8, 14, 0.72));
        topBar.setStroke(Color.rgb(120, 185, 255, 0.22));

        var titlePlate = new Rectangle(610, 72);
        titlePlate.setArcWidth(18);
        titlePlate.setArcHeight(18);
        titlePlate.setTranslateX((width - 610) / 2.0);
        titlePlate.setTranslateY(16);
        titlePlate.setFill(new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(6, 15, 28, 0.76)),
                new Stop(0.5, Color.rgb(16, 34, 58, 0.70)),
                new Stop(1.0, Color.rgb(6, 15, 28, 0.76))
        ));
        titlePlate.setStroke(Color.rgb(211, 189, 131, 0.68));
        titlePlate.setStrokeWidth(1.5);

        var title = new Text("THE CURATOR");
        title.setFont(Font.font("Cinzel", FontWeight.BOLD, 46));
        title.setFill(Color.web("#F3D39A"));
        title.setEffect(new DropShadow(14, Color.rgb(255, 190, 120, 0.35)));
        title.setTranslateX(width * 0.5 - 180);
        title.setTranslateY(64);

        var subtitle = new Text("Agent 47 // Museum Infiltration");
        subtitle.setFont(Font.font("Bodoni MT", FontWeight.SEMI_BOLD, 19));
        subtitle.setFill(Color.rgb(170, 212, 255, 0.9));
        subtitle.setTranslateX(width * 0.5 - 154);
        subtitle.setTranslateY(88);

        var pane = new Pane(topBar, titlePlate, title, subtitle);
        pane.setMouseTransparent(true);
        return pane;
    }

    private Node createNavigationPanel(int height) {
        var panel = new VBox(10);
        panel.setPadding(new Insets(20, 18, 20, 18));
        panel.setTranslateX(42);
        panel.setTranslateY(150);
        panel.setPrefWidth(300);
        panel.setPrefHeight(height - 200);
        panel.setStyle("-fx-background-color: linear-gradient(to bottom right, rgba(4,12,24,0.78), rgba(6,18,34,0.52));"
                + "-fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: rgba(150,205,255,0.35);");

        panel.getChildren().addAll(
                createNavButton("Home", this::showHomePanel),
                createNavButton("New Game", this::showNewGamePanel),
                createNavButton("Library", this::showLibraryPanel),
                createNavButton("Controls", this::showControlsPanel),
                createNavButton("Settings", this::showSettingsPanel),
                createNavButton("About", this::showAboutPanel),
                createNavButton("Exit", this::fireExit)
        );

        return panel;
    }

    private Node createContentSurface(int width, int height) {
        var surface = new VBox(16);
        surface.setAlignment(Pos.TOP_LEFT);
        surface.setTranslateX(380);
        surface.setTranslateY(148);
        surface.setPrefWidth(width - 430);
        surface.setPrefHeight(height - 190);
        surface.setPadding(new Insets(18));
        surface.setStyle("-fx-background-color: linear-gradient(to bottom right, rgba(4,12,22,0.72), rgba(8,20,35,0.30));"
                + "-fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: rgba(211,189,131,0.44);");

        panelTitle.setFont(Font.font("Cinzel", FontWeight.BOLD, 33));
        panelTitle.setFill(Color.rgb(244, 222, 178, 0.98));

        contentPanel.setPrefSize(width - 470, height - 280);
        contentPanel.setStyle("-fx-background-color: rgba(6,16,28,0.42); -fx-background-radius: 12;");

        surface.getChildren().addAll(panelTitle, contentPanel);
        return surface;
    }

    private Button createNavButton(String text, Runnable action) {
        var button = new Button(text);
        button.setFont(Font.font("Cinzel", FontWeight.BOLD, 20));
        button.setTextFill(Color.rgb(235, 215, 176));
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPrefWidth(264);
        button.setPrefHeight(42);
        button.setStyle("-fx-background-color: linear-gradient(to right, rgba(10,24,40,0.75), rgba(10,24,40,0.45));"
                + "-fx-border-color: rgba(185,215,245,0.35); -fx-border-radius: 10; -fx-background-radius: 10;");
        button.setOnAction(e -> action.run());
        return button;
    }

    private Button createPanelButton(String text) {
        var button = new Button(text);
        button.setFont(Font.font("Cinzel", FontWeight.SEMI_BOLD, 20));
        button.setTextFill(Color.rgb(238, 223, 196));
        button.setPrefWidth(320);
        button.setPrefHeight(42);
        button.setStyle("-fx-background-color: linear-gradient(to right, rgba(16,38,66,0.80), rgba(12,30,52,0.52));"
                + "-fx-border-color: rgba(211,189,131,0.52); -fx-border-radius: 10; -fx-background-radius: 10;");
        return button;
    }

    private void swapContent(Node node) {
        contentPanel.getChildren().setAll(node);

        var fade = new FadeTransition(Duration.seconds(0.23), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    private void showHomePanel() {
        panelTitle.setText("Mission Brief");

        var intro = new Text(
                "Infiltrate the museum, solve Heart API puzzles, steal high-value art,\n" +
                "and escape before security locks you in."
        );
        intro.setFill(Color.rgb(189, 220, 250, 0.95));
        intro.setFont(Font.font("Bodoni MT", FontWeight.NORMAL, 23));

        var modeText = new Text("Selected Mode: " + selectedMode.displayName());
        modeText.setFill(Color.rgb(245, 211, 145));
        modeText.setFont(Font.font("Cinzel", FontWeight.SEMI_BOLD, 25));

        var startButton = createPanelButton("Start New Mission");
        startButton.setOnAction(e -> {
            GameSession.setSelectedMode(selectedMode);
            fireNewGame();
        });

        var openModes = createPanelButton("Change Difficulty");
        openModes.setOnAction(e -> showNewGamePanel());

        var box = new VBox(18, intro, modeText, new HBox(14, startButton, openModes));
        box.setPadding(new Insets(10, 12, 10, 12));
        swapContent(box);
    }

    private void showNewGamePanel() {
        panelTitle.setText("Select Difficulty");

        var details = new Text();
        details.setFill(Color.rgb(178, 214, 251));
        details.setFont(Font.font("Bodoni MT", FontWeight.NORMAL, 20));

        var modes = new VBox(12);
        for (GameMode mode : GameMode.values()) {
            var button = createPanelButton(mode.displayName());
            button.setOnAction(e -> {
                selectedMode = mode;
                GameSession.setSelectedMode(mode);
                details.setText(modeDetails(mode));
            });
            modes.getChildren().add(button);
        }

        details.setText(modeDetails(selectedMode));

        var launch = createPanelButton("Launch Mission");
        launch.setOnAction(e -> {
            GameSession.setSelectedMode(selectedMode);
            fireNewGame();
        });

        var box = new VBox(14, modes, details, launch);
        box.setPadding(new Insets(8, 12, 8, 12));
        swapContent(box);
    }

    private void showLibraryPanel() {
        panelTitle.setText("Stolen Art Library");

        var records = GameSession.getVaultSnapshot();
        var summary = new Text("Recovered Pieces: " + records.size() + "   |   Vault Value: $" + GameSession.getVaultValue() + "M");
        summary.setFont(Font.font("Cinzel", FontWeight.SEMI_BOLD, 22));
        summary.setFill(Color.rgb(246, 218, 154));

        var list = new ListView<String>();
        list.setPrefSize(760, 360);
        list.setStyle("-fx-background-color: rgba(4,10,18,0.62); -fx-control-inner-background: rgba(4,10,18,0.62);"
                + "-fx-text-fill: #cfe7ff; -fx-font-size: 16px;");

        if (records.isEmpty()) {
            list.getItems().add("No successful heists yet. Complete a mission and escape to store stolen art.");
        } else {
            for (StolenArtRecord record : records) {
                list.getItems().add(record.stolenAt() + " | " + record.mode() + " | $" + record.value() + "M | "
                        + record.title() + " - " + record.artist());
            }
        }

        var clear = createPanelButton("Clear Local Library");
        clear.setOnAction(e -> {
            GameSession.clearVault();
            showLibraryPanel();
        });

        var box = new VBox(14, summary, list, clear);
        box.setPadding(new Insets(8, 12, 8, 12));
        swapContent(box);
    }

    private void showControlsPanel() {
        panelTitle.setText("Controls");

        var controls = new Text(
                "W / A / S / D  : Move Agent\n" +
                "ESC            : Pause / Menu\n" +
                "Touch Artwork  : Trigger Heart Puzzle\n\n" +
                "Tip: avoid both guards and their torch cones."
        );
        controls.setFont(Font.font("Bodoni MT", FontWeight.NORMAL, 24));
        controls.setFill(Color.rgb(189, 221, 252, 0.96));

        var openModes = createPanelButton("Choose Difficulty");
        openModes.setOnAction(e -> showNewGamePanel());

        var box = new VBox(22, controls, openModes);
        box.setPadding(new Insets(10, 12, 10, 12));
        swapContent(box);
    }

    private void showSettingsPanel() {
        panelTitle.setText("Settings");

        var conesToggle = new CheckBox("Show Guard Torch Cones");
        conesToggle.setSelected(GameSession.isShowGuardCones());
        conesToggle.setTextFill(Color.rgb(232, 216, 184));
        conesToggle.setFont(Font.font("Cinzel", FontWeight.SEMI_BOLD, 22));
        conesToggle.setOnAction(e -> GameSession.setShowGuardCones(conesToggle.isSelected()));

        var qualityText = new Text("Visual Profile: Premium Menu + Dynamic Lighting + Heart Puzzle Flow");
        qualityText.setFont(Font.font("Bodoni MT", FontWeight.NORMAL, 22));
        qualityText.setFill(Color.rgb(182, 214, 250, 0.96));

        var restore = createPanelButton("Restore Defaults");
        restore.setOnAction(e -> {
            GameSession.setSelectedMode(GameMode.MEDIUM);
            GameSession.setShowGuardCones(true);
            selectedMode = GameMode.MEDIUM;
            showSettingsPanel();
        });

        var box = new VBox(18, conesToggle, qualityText, restore);
        box.setPadding(new Insets(10, 12, 10, 12));
        swapContent(box);
    }

    private void showAboutPanel() {
        panelTitle.setText("About The Curator");

        var about = new Text(
                "University Assignment Build\n\n" +
                "- Event-driven gameplay with FXGL\n" +
                "- Interoperability with Art Institute API\n" +
                "- Heart API puzzle challenge before each theft\n" +
                "- Session library for recovered artworks\n" +
                "- Difficulty-scaled missions (Easy / Medium / Hard)"
        );
        about.setFont(Font.font("Bodoni MT", FontWeight.NORMAL, 22));
        about.setFill(Color.rgb(186, 219, 252, 0.96));

        var box = new VBox(18, about);
        box.setPadding(new Insets(10, 12, 10, 12));
        swapContent(box);
    }

    private String modeDetails(GameMode mode) {
        return mode.displayName() + " Mode\n"
                + "Time: " + mode.missionTimeSeconds() + " sec\n"
                + "Required Art: " + mode.requiredArtCount() + "\n"
                + "Guards: " + mode.guardCount() + " (speed " + (int) mode.guardSpeed() + ")\n"
                + "Puzzle: " + mode.puzzleTimeSeconds() + " sec, " + mode.puzzleAttempts() + " attempts\n"
                + "Art Value Range: $" + mode.minArtValue() + "M - $" + mode.maxArtValue() + "M";
    }
}
