package com.curator.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.scene.SubScene;
import com.almasb.fxgl.time.TimerAction;
import javafx.animation.PauseTransition;
import com.curator.model.GameMode;
import com.curator.services.HeartService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;
import java.util.function.Consumer;

public class HackingSubScene extends SubScene {

    private final HeartService heartService;
    private final GameMode mode;
    private final Consumer<Boolean> onComplete;

    private final Text statusText = new Text();
    private final Text timerText = new Text();
    private final Text attemptsText = new Text();
    private final Text promptText = new Text();
    private final TextField answerInput = new TextField();
    private final ImageView puzzleImage = new ImageView();

    private TimerAction timerAction;
    private HeartService.HeartPuzzle puzzle;
    private int secondsLeft;
    private int attemptsLeft;
    private boolean completed;

    public HackingSubScene(HeartService heartService, GameMode mode, Consumer<Boolean> onComplete) {
        this.heartService = heartService;
        this.mode = mode;
        this.onComplete = onComplete;
        buildUi();
    }

    public void startHack() {
        completed = false;
        secondsLeft = mode.puzzleTimeSeconds();
        attemptsLeft = mode.puzzleAttempts();
        refreshCounters();
        statusText.setText("CONNECTING TO HEART API...");
        answerInput.clear();
        answerInput.setDisable(true);
        puzzleImage.setImage(null);

        loadPuzzle();

        timerAction = FXGL.getGameTimer().runAtInterval(() -> {
            if (completed) {
                return;
            }

            secondsLeft--;
            refreshCounters();

            if (secondsLeft <= 0) {
                finish(false, "PUZZLE FAILED: TIMEOUT");
            }
        }, Duration.seconds(1));
    }

    private void buildUi() {
        var bg = new Rectangle(1280, 720, Color.rgb(0, 0, 0, 0.86));

        var frame = new Rectangle(820, 560, Color.rgb(8, 20, 36, 0.95));
        frame.setArcWidth(16);
        frame.setArcHeight(16);
        frame.setStroke(Color.rgb(145, 210, 255, 0.72));
        frame.setStrokeWidth(2);

        var title = new Text("HEART API SECURITY CHALLENGE");
        title.setFont(Font.font("Cinzel", FontWeight.BOLD, 33));
        title.setFill(Color.rgb(240, 214, 165));

        timerText.setFont(Font.font("Cinzel", FontWeight.BOLD, 24));
        timerText.setFill(Color.rgb(255, 175, 145));

        attemptsText.setFont(Font.font("Cinzel", FontWeight.BOLD, 24));
        attemptsText.setFill(Color.rgb(176, 225, 255));

        var headerRow = new HBox(30, timerText, attemptsText);
        headerRow.setAlignment(Pos.CENTER);

        promptText.setFont(Font.font("Bodoni MT", FontWeight.NORMAL, 22));
        promptText.setFill(Color.rgb(190, 220, 248));
        promptText.setWrappingWidth(680);
        promptText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        puzzleImage.setFitWidth(410);
        puzzleImage.setFitHeight(250);
        puzzleImage.setPreserveRatio(true);
        puzzleImage.setSmooth(true);

        answerInput.setPromptText("Enter numeric answer");
        answerInput.setFont(Font.font("Cinzel", FontWeight.SEMI_BOLD, 22));
        answerInput.setMaxWidth(260);

        var submitButton = new Button("Submit");
        submitButton.setFont(Font.font("Cinzel", FontWeight.SEMI_BOLD, 21));
        submitButton.setStyle("-fx-background-color: linear-gradient(to right, #1f4e7a, #173855);"
                + "-fx-text-fill: #f6ddb4; -fx-border-color: rgba(225,200,150,0.7); -fx-border-radius: 8;");
        submitButton.setOnAction(e -> submitAnswer());

        answerInput.setOnAction(e -> submitAnswer());

        statusText.setFont(Font.font("Bodoni MT", FontWeight.SEMI_BOLD, 22));
        statusText.setFill(Color.rgb(126, 212, 245));

        var content = new VBox(16, title, headerRow, promptText, puzzleImage, answerInput, submitButton, statusText);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));

        var root = new StackPane(bg, frame, content);
        root.setPrefSize(1280, 720);
        getContentRoot().getChildren().add(root);
    }

    private void loadPuzzle() {
        heartService.fetchPuzzle().thenAcceptAsync(result -> FXGL.getExecutor().startAsyncFX(() -> {
            if (completed) {
                return;
            }

            puzzle = result;
            promptText.setText(result.prompt());
            statusText.setText("Solve to secure the artwork.");
            answerInput.setDisable(false);
            answerInput.requestFocus();

            if (result.imageUrl() != null && !result.imageUrl().isBlank()) {
                puzzleImage.setImage(new Image(result.imageUrl(), 410, 250, true, true, true));
            } else {
                puzzleImage.setImage(null);
            }
        }), FXGL.getExecutor());
    }

    private void submitAnswer() {
        if (completed || puzzle == null || answerInput.isDisabled()) {
            return;
        }

        if (heartService.isCorrect(puzzle, answerInput.getText())) {
            finish(true, "ACCESS GRANTED. ASSET SECURED.");
            return;
        }

        attemptsLeft--;
        refreshCounters();
        answerInput.clear();
        statusText.setText("ACCESS DENIED. TRY AGAIN.");

        if (attemptsLeft <= 0) {
            finish(false, "PUZZLE FAILED: NO ATTEMPTS LEFT");
        }
    }

    private void finish(boolean success, String message) {
        if (completed) {
            return;
        }

        completed = true;
        answerInput.setDisable(true);
        statusText.setText(message);
        statusText.setFill(success ? Color.rgb(120, 230, 178) : Color.rgb(255, 140, 140));

        if (timerAction != null) {
            timerAction.expire();
        }

        var pause = new PauseTransition(Duration.seconds(0.8));
        pause.setOnFinished(e -> {
            FXGL.getSceneService().popSubScene();
            onComplete.accept(success);
        });
        pause.play();
    }

    private void refreshCounters() {
        timerText.setText("TIME: " + Math.max(secondsLeft, 0));
        attemptsText.setText("ATTEMPTS: " + Math.max(attemptsLeft, 0));
    }
}
