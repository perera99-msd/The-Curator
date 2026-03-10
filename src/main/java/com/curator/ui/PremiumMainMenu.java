package com.curator.ui;

import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import com.curator.domain.AuthSession;
import com.curator.domain.GameMode;
import com.curator.domain.UserProfile;
import com.curator.services.AuthService;
import com.curator.services.StolenArtRepository;
import com.curator.services.UserProfileRepository;
import com.curator.state.GameSession;
import com.curator.ui.library.LibraryPanel;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Control;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.curator.ui.panels.*;

public class PremiumMainMenu extends FXGLMenu implements MenuNavigator {

    private static final String MENU_BG_PATH = "/assets/textures/menu_bg.png";
    private static final String THIEF_PATH = "/assets/textures/thief.png";
    private static final String GOOGLE_LOGO_PATH = "/assets/textures/Google__G__logo.svg.png";

    private enum AuthMode {
        LOGIN,
        REGISTER
    }

    private record AuthOutcome(AuthSession session, String displayName) {
    }

    private final List<Animation> animations = new ArrayList<>();
    private final Pane menuLayer = new Pane();
    private final Pane contentPanel = new Pane();
    private final Text panelTitle = new Text();

    private final AuthService authService;
    private final UserProfileRepository profileRepository;
    private final StolenArtRepository stolenArtRepository;
    private StackPane authLayer;
    private StackPane loginPage;
    private StackPane registerPage;
    private Runnable pendingAction;
    private GameMode selectedMode = GameSession.getSelectedMode();
    private boolean mainMenuBuilt;

    public PremiumMainMenu(AuthService authService,
                           UserProfileRepository profileRepository,
                           StolenArtRepository stolenArtRepository) {
        super(MenuType.MAIN_MENU);
        this.authService = authService;
        this.profileRepository = profileRepository;
        this.stolenArtRepository = stolenArtRepository;
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
        root.getChildren().clear();
        root.getChildren().add(createBackground(width, height));

        menuLayer.getChildren().clear();
        menuLayer.setPrefSize(width, height);

        authLayer = createAuthLayer(width, height);

        root.getChildren().addAll(menuLayer, authLayer);

        if (GameSession.isLoggedIn()) {
            showMainMenu(false);
        } else {
            showAuthScreen(AuthMode.LOGIN, false);
        }
    }

    private void buildMainMenuLayer(int width, int height) {
        if (mainMenuBuilt) {
            return;
        }
        menuLayer.getChildren().setAll(
                createTopHeader(width),
                createNavigationPanel(height),
                createContentSurface(width, height)
        );
        showHomePanel();
        mainMenuBuilt = true;
    }

    private void showMainMenu(boolean animate) {
        buildMainMenuLayer(getAppWidth(), getAppHeight());
        menuLayer.setVisible(true);
        menuLayer.setDisable(false);
        menuLayer.setTranslateY(0);

        if (authLayer != null) {
            authLayer.setVisible(false);
            authLayer.setDisable(true);
            authLayer.setOpacity(1.0);
        }

        if (animate) {
            menuLayer.setOpacity(0.0);
            var fade = new FadeTransition(Duration.seconds(0.5), menuLayer);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.setInterpolator(Interpolator.EASE_OUT);
            fade.play();
        } else {
            menuLayer.setOpacity(1.0);
        }
    }

    private void showAuthScreen(AuthMode mode, boolean animate) {
        if (authLayer == null) {
            return;
        }
        setNodeVisible(loginPage, mode == AuthMode.LOGIN);
        setNodeVisible(registerPage, mode == AuthMode.REGISTER);

        authLayer.setVisible(true);
        authLayer.setDisable(false);
        authLayer.setOpacity(1.0);

        menuLayer.setVisible(false);
        menuLayer.setDisable(true);

        if (animate) {
            authLayer.setOpacity(0.0);
            var fade = new FadeTransition(Duration.seconds(0.35), authLayer);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.setInterpolator(Interpolator.EASE_OUT);
            fade.play();
        }
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

    private StackPane createAuthLayer(int width, int height) {
        var layer = new StackPane();
        layer.setPrefSize(width, height);

        loginPage = createAuthPage(width, height, AuthMode.LOGIN);
        registerPage = createAuthPage(width, height, AuthMode.REGISTER);

        setNodeVisible(registerPage, false);

        layer.getChildren().addAll(loginPage, registerPage);
        return layer;
    }

    private StackPane createAuthPage(int width, int height, AuthMode mode) {
        double horizontalMargin = 48;
        double maxCardWidth = Math.max(640, width - (horizontalMargin * 2));
        double minCardWidth = Math.min(660, maxCardWidth);
        double cardWidth = clamp(width * 0.82, minCardWidth, Math.min(980, maxCardWidth));

        double verticalMargin = 48;
        double maxCardHeight = Math.max(520, height - (verticalMargin * 2));
        double minCardHeight = Math.min(540, maxCardHeight);
        double cardHeight = clamp(height * 0.80, minCardHeight, Math.min(660, maxCardHeight));

        boolean compactLayout = width < 1120 || height < 720;
        double leftPaneWidth = compactLayout ? cardWidth - 80 : cardWidth * 0.62;
        double fieldWidth = clamp(leftPaneWidth - 32, 300, 520);
        double verticalPadding = compactLayout ? 36 : 44;
        double contentHeight = cardHeight - verticalPadding;

        var dim = new Rectangle(width, height, Color.rgb(2, 8, 16, 0.72));

        var topGlow = new Rectangle(width, height);
        topGlow.setFill(new LinearGradient(
                0.5, 0, 0.5, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(132, 214, 255, 0.12)),
                new Stop(0.55, Color.rgb(132, 214, 255, 0.04)),
                new Stop(1.0, Color.rgb(255, 154, 110, 0.08))
        ));

        var signalA = new Circle(260, Color.rgb(106, 190, 255, 0.16));
        signalA.setTranslateX(-width * 0.27);
        signalA.setTranslateY(-height * 0.14);
        signalA.setBlendMode(BlendMode.SCREEN);

        var signalB = new Circle(220, Color.rgb(255, 162, 106, 0.14));
        signalB.setTranslateX(width * 0.30);
        signalB.setTranslateY(height * 0.22);
        signalB.setBlendMode(BlendMode.SCREEN);

        var signalAScale = new ScaleTransition(Duration.seconds(5.6), signalA);
        signalAScale.setFromX(0.94);
        signalAScale.setFromY(0.94);
        signalAScale.setToX(1.06);
        signalAScale.setToY(1.06);
        signalAScale.setAutoReverse(true);
        signalAScale.setCycleCount(Animation.INDEFINITE);
        signalAScale.setInterpolator(Interpolator.EASE_BOTH);
        animations.add(signalAScale);

        var signalAFade = new FadeTransition(Duration.seconds(5.2), signalA);
        signalAFade.setFromValue(0.55);
        signalAFade.setToValue(0.82);
        signalAFade.setAutoReverse(true);
        signalAFade.setCycleCount(Animation.INDEFINITE);
        animations.add(signalAFade);

        var signalBScale = new ScaleTransition(Duration.seconds(4.8), signalB);
        signalBScale.setFromX(1.03);
        signalBScale.setFromY(1.03);
        signalBScale.setToX(0.94);
        signalBScale.setToY(0.94);
        signalBScale.setAutoReverse(true);
        signalBScale.setCycleCount(Animation.INDEFINITE);
        signalBScale.setInterpolator(Interpolator.EASE_BOTH);
        animations.add(signalBScale);

        var signalBFade = new FadeTransition(Duration.seconds(4.4), signalB);
        signalBFade.setFromValue(0.45);
        signalBFade.setToValue(0.70);
        signalBFade.setAutoReverse(true);
        signalBFade.setCycleCount(Animation.INDEFINITE);
        animations.add(signalBFade);

        var panelSurface = new Rectangle(cardWidth, cardHeight);
        panelSurface.setArcWidth(26);
        panelSurface.setArcHeight(26);
        panelSurface.setFill(new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(4, 13, 26, 0.94)),
                new Stop(0.55, Color.rgb(8, 22, 40, 0.88)),
                new Stop(1.0, Color.rgb(8, 19, 32, 0.94))
        ));
        panelSurface.setStroke(Color.rgb(161, 215, 255, 0.46));
        panelSurface.setStrokeWidth(1.5);
        panelSurface.setEffect(new DropShadow(24, Color.rgb(0, 0, 0, 0.55)));

        double formSpacing = compactLayout ? 8 : 10;
        String existingAlias = GameSession.getOperatorAlias();

        var form = (mode == AuthMode.LOGIN)
                ? buildLoginForm(compactLayout, leftPaneWidth, fieldWidth, formSpacing)
                : buildRegisterForm(compactLayout, leftPaneWidth, fieldWidth, formSpacing, existingAlias);

        var formContainer = new StackPane(form);
        formContainer.setAlignment(Pos.TOP_LEFT);
        formContainer.setPrefWidth(leftPaneWidth);
        formContainer.setMaxWidth(leftPaneWidth);
        formContainer.setPrefHeight(contentHeight);
        formContainer.setMaxHeight(contentHeight);

        var content = new HBox(compactLayout ? 0 : 34);
        content.setAlignment(Pos.TOP_LEFT);
        content.setPadding(compactLayout ? new Insets(18, 20, 18, 20) : new Insets(22, 28, 22, 28));
        content.setPrefSize(cardWidth, cardHeight);
        content.setMaxSize(cardWidth, cardHeight);

        content.getChildren().add(formContainer);

        if (!compactLayout) {
            var operativePane = createOperativePane(cardWidth - leftPaneWidth - 80, contentHeight);
            content.getChildren().add(operativePane);
        }

        var card = new StackPane(panelSurface, content);
        card.setOpacity(0);
        card.setTranslateY(26);

        var cardFade = new FadeTransition(Duration.seconds(0.6), card);
        cardFade.setFromValue(0.0);
        cardFade.setToValue(1.0);
        cardFade.setInterpolator(Interpolator.EASE_OUT);
        animations.add(cardFade);

        var cardRise = new TranslateTransition(Duration.seconds(0.6), card);
        cardRise.setFromY(26);
        cardRise.setToY(0);
        cardRise.setInterpolator(Interpolator.EASE_OUT);
        animations.add(cardRise);

        var overlay = new StackPane(dim, signalA, signalB, topGlow, card);
        overlay.setPrefSize(width, height);
        return overlay;
    }

    private StackPane createOperativePane(double width, double height) {
        double operativeHeight = Math.min(height * 0.62, 270);
        var operative = new ImageView(new Image(THIEF_PATH));
        operative.setFitHeight(operativeHeight);
        operative.setPreserveRatio(true);
        operative.setSmooth(true);

        var operativeShadow = new Ellipse(100, 28);
        operativeShadow.setFill(Color.rgb(0, 0, 0, 0.30));
        operativeShadow.setTranslateY(operativeHeight * 0.45);
        operativeShadow.setBlendMode(BlendMode.MULTIPLY);

        var operativeFloat = new TranslateTransition(Duration.seconds(2.7), operative);
        operativeFloat.setFromY(0);
        operativeFloat.setToY(-10);
        operativeFloat.setAutoReverse(true);
        operativeFloat.setCycleCount(Animation.INDEFINITE);
        operativeFloat.setInterpolator(Interpolator.EASE_BOTH);
        animations.add(operativeFloat);

        var operativeSway = new RotateTransition(Duration.seconds(4.5), operative);
        operativeSway.setFromAngle(-1.4);
        operativeSway.setToAngle(1.4);
        operativeSway.setAutoReverse(true);
        operativeSway.setCycleCount(Animation.INDEFINITE);
        operativeSway.setInterpolator(Interpolator.EASE_BOTH);
        animations.add(operativeSway);

        var shadowPulse = new ScaleTransition(Duration.seconds(2.7), operativeShadow);
        shadowPulse.setFromX(1.0);
        shadowPulse.setToX(0.88);
        shadowPulse.setAutoReverse(true);
        shadowPulse.setCycleCount(Animation.INDEFINITE);
        shadowPulse.setInterpolator(Interpolator.EASE_BOTH);
        animations.add(shadowPulse);

        var operativePane = new StackPane(operativeShadow, operative);
        operativePane.setPrefSize(width, height);
        operativePane.setMaxSize(width, height);
        operativePane.setAlignment(Pos.CENTER);
        return operativePane;
    }

    private VBox buildLoginForm(boolean compactLayout,
                                double leftPaneWidth,
                                double fieldWidth,
                                double spacing) {
        var header = createAuthHeader(compactLayout, fieldWidth, "Mission Login",
                "Authenticate with Firebase to unlock The Curator command deck.");

        var emailField = new TextField();
        emailField.setPromptText("Email address");
        configureLoginField(emailField, fieldWidth);

        var passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        configureLoginField(passwordField, fieldWidth);

        var statusText = createStatusText(compactLayout, fieldWidth, "Authenticate to start a mission.");

        var loginButton = createLoginButton("Sign In", fieldWidth);
        loginButton.setDefaultButton(true);

        var googleButton = createLoginButton("Continue with Google", fieldWidth);
        decorateGoogleButton(googleButton);

        var divider = createOrDivider(fieldWidth);
        var toRegisterButton = createLinkButton("Create new account");
        var switchRow = createSwitchRow("New operator?", toRegisterButton, compactLayout);

        loginButton.setOnAction(e -> startEmailLoginFlow(
                emailField, passwordField, statusText,
                loginButton, googleButton, toRegisterButton));

        googleButton.setOnAction(e -> startGoogleLoginFlow(
                AuthMode.LOGIN, statusText,
                loginButton, googleButton, toRegisterButton,
                emailField, passwordField));

        toRegisterButton.setOnAction(e -> showAuthScreen(AuthMode.REGISTER, true));

        emailField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> loginButton.fire());

        var spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        var form = new VBox(spacing,
                header,
                emailField,
                passwordField,
                loginButton,
                divider,
                googleButton,
                statusText,
                spacer,
                switchRow
        );
        form.setAlignment(Pos.TOP_LEFT);
        form.setFillWidth(true);
        form.setPrefWidth(leftPaneWidth);
        form.setMaxWidth(leftPaneWidth);
        return form;
    }

    private VBox buildRegisterForm(boolean compactLayout,
                                   double leftPaneWidth,
                                   double fieldWidth,
                                   double spacing,
                                   String existingAlias) {
        var header = createAuthHeader(compactLayout, fieldWidth, "Operator Registration",
                "Create a new operator identity to access the command deck.");

        var aliasField = new TextField("Operator".equals(existingAlias) ? "" : existingAlias);
        aliasField.setPromptText("Operator name (required)");
        configureLoginField(aliasField, fieldWidth);

        var emailField = new TextField();
        emailField.setPromptText("Email address");
        configureLoginField(emailField, fieldWidth);

        var passwordField = new PasswordField();
        passwordField.setPromptText("Create password (min 6 chars)");
        configureLoginField(passwordField, fieldWidth);

        var confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm password");
        configureLoginField(confirmPasswordField, fieldWidth);

        var statusText = createStatusText(compactLayout, fieldWidth, "Create your credentials to begin.");

        var registerButton = createLoginButton("Register & Enter", fieldWidth);
        registerButton.setDefaultButton(true);

        var googleButton = createLoginButton("Register with Google", fieldWidth);
        decorateGoogleButton(googleButton);

        var divider = createOrDivider(fieldWidth);
        var toLoginButton = createLinkButton("Back to sign in");
        var switchRow = createSwitchRow("Already have access?", toLoginButton, compactLayout);

        registerButton.setOnAction(e -> startEmailRegisterFlow(
                aliasField, emailField, passwordField, confirmPasswordField,
                statusText, registerButton, googleButton, toLoginButton));

        googleButton.setOnAction(e -> startGoogleLoginFlow(
                AuthMode.REGISTER, statusText,
                registerButton, googleButton, toLoginButton,
                aliasField, emailField, passwordField, confirmPasswordField));

        toLoginButton.setOnAction(e -> showAuthScreen(AuthMode.LOGIN, true));

        aliasField.setOnAction(e -> emailField.requestFocus());
        emailField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> confirmPasswordField.requestFocus());
        confirmPasswordField.setOnAction(e -> registerButton.fire());

        var spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        var form = new VBox(spacing,
                header,
                aliasField,
                emailField,
                passwordField,
                confirmPasswordField,
                registerButton,
                divider,
                googleButton,
                statusText,
                spacer,
                switchRow
        );
        form.setAlignment(Pos.TOP_LEFT);
        form.setFillWidth(true);
        form.setPrefWidth(leftPaneWidth);
        form.setMaxWidth(leftPaneWidth);
        return form;
    }

    private void configureLoginField(TextField field, double width) {
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

    private Button createLoginButton(String text, double width) {
        var button = new Button(text);
        button.setFont(Font.font("Cinzel", FontWeight.BOLD, 21));
        button.setTextFill(Color.rgb(244, 227, 192));
        button.setPrefWidth(width);
        button.setPrefHeight(48);
        button.setStyle("-fx-background-color: linear-gradient(to right, rgba(24,72,118,0.95), rgba(20,58,95,0.92));"
                + "-fx-border-color: rgba(241,212,153,0.72); -fx-border-radius: 10; -fx-background-radius: 10;");
        MenuComponents.applyHoverMotion(button, 1.04);
        return button;
    }

    private void decorateGoogleButton(Button button) {
        var logo = new ImageView(new Image(GOOGLE_LOGO_PATH));
        logo.setFitHeight(20);
        logo.setFitWidth(20);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        button.setGraphic(logo);
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setGraphicTextGap(12);
    }

    private VBox createAuthHeader(boolean compactLayout, double fieldWidth, String titleText, String subtitleText) {
        var badge = new Text("SECURE ACCESS TERMINAL");
        badge.setFont(Font.font("Cinzel", FontWeight.SEMI_BOLD, compactLayout ? 15 : 17));
        badge.setFill(Color.rgb(150, 214, 255));

        var title = new Text(titleText);
        title.setFont(Font.font("Cinzel", FontWeight.BOLD, compactLayout ? 36 : 42));
        title.setFill(Color.rgb(245, 220, 172));

        var subtitle = new Text(subtitleText);
        subtitle.setFont(Font.font("Bodoni MT", FontWeight.NORMAL, compactLayout ? 17 : 19));
        subtitle.setFill(Color.rgb(189, 220, 247));
        subtitle.setTextAlignment(TextAlignment.LEFT);
        subtitle.setWrappingWidth(fieldWidth);

        var header = new VBox(6, badge, title, subtitle);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private Text createStatusText(boolean compactLayout, double fieldWidth, String message) {
        var statusText = new Text(message);
        statusText.setFont(Font.font("Bodoni MT", FontWeight.SEMI_BOLD, compactLayout ? 16 : 18));
        statusText.setFill(Color.rgb(133, 225, 174));
        statusText.setWrappingWidth(fieldWidth);
        return statusText;
    }

    private Button createLinkButton(String text) {
        var button = new Button(text);
        button.setFont(Font.font("Cinzel", FontWeight.SEMI_BOLD, 16));
        button.setTextFill(Color.rgb(150, 214, 255));
        button.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-underline: true;");
        return button;
    }

    private HBox createSwitchRow(String labelText, Button linkButton, boolean compactLayout) {
        var label = new Text(labelText);
        label.setFont(Font.font("Bodoni MT", FontWeight.NORMAL, compactLayout ? 16 : 18));
        label.setFill(Color.rgb(189, 214, 238, 0.84));
        var row = new HBox(6, label, linkButton);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Node createOrDivider(double width) {
        var left = new Rectangle(width * 0.36, 1, Color.rgb(148, 205, 253, 0.35));
        var right = new Rectangle(width * 0.36, 1, Color.rgb(148, 205, 253, 0.35));
        var label = new Text("OR");
        label.setFont(Font.font("Cinzel", FontWeight.SEMI_BOLD, 14));
        label.setFill(Color.rgb(186, 208, 234, 0.7));
        var row = new HBox(8, left, label, right);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private void setNodeVisible(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void setControlsDisabled(boolean disabled, Control... controls) {
        if (controls == null) {
            return;
        }
        for (var control : controls) {
            if (control != null) {
                control.setDisable(disabled);
            }
        }
    }

    private void startEmailLoginFlow(TextField emailField,
                                     PasswordField passwordField,
                                     Text statusText,
                                     Button loginButton,
                                     Button googleButton,
                                     Button switchButton) {
        setControlsDisabled(true, emailField, passwordField, loginButton, googleButton, switchButton);

        statusText.setText("Contacting Firebase identity gateway...");
        statusText.setFill(Color.rgb(133, 225, 174));

        var email = emailField.getText();
        var password = passwordField.getText();
        authService.signIn(email, password)
                .thenCompose(session -> resolveDisplayName(session)
                        .thenApply(name -> new AuthOutcome(session, name)))
                .thenAccept(outcome -> Platform.runLater(() -> finalizeLogin(outcome, statusText)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> handleAuthFailure(ex, statusText,
                            emailField, passwordField, loginButton, googleButton, switchButton));
                    return null;
                });
    }

    private void startEmailRegisterFlow(TextField displayNameField,
                                        TextField emailField,
                                        PasswordField passwordField,
                                        PasswordField confirmPasswordField,
                                        Text statusText,
                                        Button registerButton,
                                        Button googleButton,
                                        Button switchButton) {
        setControlsDisabled(true, displayNameField, emailField, passwordField, confirmPasswordField,
                registerButton, googleButton, switchButton);

        String displayName = displayNameField.getText() == null ? "" : displayNameField.getText().trim();
        if (displayName.isBlank()) {
            statusText.setText("Operator name is required.");
            statusText.setFill(Color.rgb(255, 140, 140));
            showPopup("Missing Name", "Please enter an operator name.");
            setControlsDisabled(false, displayNameField, emailField, passwordField, confirmPasswordField,
                    registerButton, googleButton, switchButton);
            return;
        }

        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        if (password == null || !password.equals(confirm)) {
            statusText.setText("Passwords do not match.");
            statusText.setFill(Color.rgb(255, 140, 140));
            showPopup("Check Password", "Passwords do not match.");
            setControlsDisabled(false, displayNameField, emailField, passwordField, confirmPasswordField,
                    registerButton, googleButton, switchButton);
            return;
        }

        statusText.setText("Creating identity in Firebase...");
        statusText.setFill(Color.rgb(133, 225, 174));

        authService.register(emailField.getText(), password)
                .thenCompose(session -> upsertProfile(session, displayName)
                        .thenApply(profile -> new AuthOutcome(session, profile.displayName())))
                .thenAccept(outcome -> Platform.runLater(() -> finalizeLogin(outcome, statusText)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> handleAuthFailure(ex, statusText,
                            displayNameField, emailField, passwordField, confirmPasswordField,
                            registerButton, googleButton, switchButton));
                    return null;
                });
    }

    private void startGoogleLoginFlow(AuthMode mode,
                                      Text statusText,
                                      Button primaryButton,
                                      Button googleButton,
                                      Button switchButton,
                                      Control... formFields) {
        setControlsDisabled(true, formFields);
        setControlsDisabled(true, primaryButton, googleButton, switchButton);

        statusText.setText("Opening Google login in your browser...");
        statusText.setFill(Color.rgb(133, 225, 174));

        authService.signInWithGoogle()
                .thenCompose(session -> {
                    if (mode == AuthMode.REGISTER && !session.isNewUser()) {
                        return CompletableFuture.failedFuture(new IllegalStateException("Already Registered."));
                    }
                    if (session.isNewUser()) {
                        String alias = deriveAliasFromEmail(session.email());
                        return upsertProfile(session, alias)
                                .thenApply(profile -> new AuthOutcome(session, profile.displayName()));
                    }
                    return resolveDisplayName(session)
                            .thenApply(name -> new AuthOutcome(session, name));
                })
                .thenAccept(outcome -> Platform.runLater(() -> finalizeLogin(outcome, statusText)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        handleGoogleFailure(ex, statusText, primaryButton, googleButton, switchButton);
                        setControlsDisabled(false, formFields);
                    });
                    return null;
                });
    }

    private void finalizeLogin(AuthOutcome outcome, Text statusText) {
        String alias = outcome.displayName();
        if (alias == null || alias.isBlank()) {
            alias = deriveAliasFromEmail(outcome.session().email());
        }
        GameSession.setOperatorAlias(alias);
        GameSession.setAuthSession(outcome.session()); // Virtual identity: keep userId/idToken for this session.

        statusText.setText("Identity verified. Opening command deck...");
        statusText.setFill(Color.rgb(125, 231, 183));

        revealMenuFromLogin();
    }

    private void handleAuthFailure(Throwable ex, Text statusText, Control... controls) {
        String message = extractErrorMessage(ex);
        String resolved = (message == null || message.isBlank()) ? "Authentication failed." : message;

        statusText.setText(resolved);
        statusText.setFill(Color.rgb(255, 140, 140));

        if ("Already Registered.".equalsIgnoreCase(resolved.trim())) {
            showPopup("Already Registered", "Already Registered.");
        }

        setControlsDisabled(false, controls);
    }

    private void handleGoogleFailure(Throwable ex, Text statusText, Control... controls) {
        String message = extractErrorMessage(ex);
        String base = (message == null || message.isBlank()) ? "Google login failed." : message;
        String resolved = base + " Use email/password below.";

        statusText.setText(resolved);
        statusText.setFill(Color.rgb(255, 140, 140));

        if ("Already Registered.".equalsIgnoreCase(base.trim())) {
            showPopup("Already Registered", "Already Registered.");
        }

        setControlsDisabled(false, controls);
    }

    private CompletableFuture<String> resolveDisplayName(AuthSession session) {
        return profileRepository.fetchProfile(session)
                .handle((profile, ex) -> profile)
                .thenCompose(profile -> {
                    if (profile == null || profile.displayName() == null || profile.displayName().isBlank()) {
                        String alias = deriveAliasFromEmail(session.email());
                        return upsertProfile(session, alias)
                                .thenApply(UserProfile::displayName)
                                .exceptionally(err -> alias);
                    }
                    return CompletableFuture.completedFuture(profile.displayName());
                });
    }

    private CompletableFuture<UserProfile> upsertProfile(AuthSession session, String displayName) {
        String normalized = displayName == null ? "" : displayName.trim();
        UserProfile profile = new UserProfile(session.userId(), normalized, session.email());
        return profileRepository.upsertProfile(session, profile);
    }

    private String deriveAliasFromEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return "Operator";
        }
        String prefix = email.split("@")[0];
        return prefix == null || prefix.isBlank() ? "Operator" : prefix;
    }

    private String extractErrorMessage(Throwable ex) {
        if (ex == null) {
            return null;
        }
        Throwable root = ex.getCause() != null ? ex.getCause() : ex;
        return root.getMessage();
    }

    private void showPopup(String title, String message) {
        Platform.runLater(() -> {
            var alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
        });
    }

    private void revealMenuFromLogin() {
        buildMainMenuLayer(getAppWidth(), getAppHeight());
        menuLayer.setVisible(true);
        menuLayer.setDisable(false);

        if (authLayer == null) {
            showHomePanel();
            return;
        }

        menuLayer.setOpacity(0.0);
        var menuFade = new FadeTransition(Duration.seconds(0.55), menuLayer);
        menuFade.setFromValue(0.0);
        menuFade.setToValue(1.0);
        menuFade.setInterpolator(Interpolator.EASE_OUT);

        var authFade = new FadeTransition(Duration.seconds(0.45), authLayer);
        authFade.setFromValue(authLayer.getOpacity());
        authFade.setToValue(0.0);
        authFade.setInterpolator(Interpolator.EASE_IN);
        authFade.setOnFinished(e -> {
            authLayer.setVisible(false);
            authLayer.setDisable(true);
            authLayer.setOpacity(1.0);
            if (pendingAction != null) {
                var next = pendingAction;
                pendingAction = null;
                next.run();
            }
            showHomePanel();
        });
        menuFade.play();
        authFade.play();
    }

    
    
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
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
        String baseStyle = "-fx-background-color: linear-gradient(to right, rgba(10,24,40,0.75), rgba(10,24,40,0.45));"
                + "-fx-border-color: rgba(185,215,245,0.35); -fx-border-radius: 10; -fx-background-radius: 10;";
        String hoverStyle = "-fx-background-color: linear-gradient(to right, rgba(20,45,72,0.88), rgba(16,38,61,0.62));"
                + "-fx-border-color: rgba(233,201,138,0.75); -fx-border-radius: 10; -fx-background-radius: 10;";

        var button = new Button(text);
        button.setFont(Font.font("Cinzel", FontWeight.BOLD, 20));
        button.setTextFill(Color.rgb(235, 215, 176));
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPrefWidth(264);
        button.setPrefHeight(42);
        button.setStyle(baseStyle);
        button.setOnAction(e -> action.run());
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));
        MenuComponents.applyHoverMotion(button, 1.03);
        return button;
    }

    
    @Override
    public void showHomePanel() { HomePanel.buildAndShow(this); }

    @Override
    public void showNewGamePanel() { NewGamePanel.buildAndShow(this); }

    @Override
    public void showControlsPanel() { ControlsPanel.buildAndShow(this); }

    @Override
    public void showSettingsPanel() { SettingsPanel.buildAndShow(this, profileRepository); }

    @Override
    public void showAboutPanel() { AboutPanel.buildAndShow(this); }

    @Override
    public void showLibraryPanel() {
        setPanelTitle("Stolen Art Library");
        var library = new com.curator.ui.library.LibraryPanel(stolenArtRepository, com.curator.state.GameSession.getAuthSession());
        swapContent(library);
    }

    @Override
    public com.curator.domain.GameMode getSelectedMode() { return selectedMode; }

    @Override
    public void setSelectedMode(com.curator.domain.GameMode mode) { this.selectedMode = mode; }

    @Override
    public void setPanelTitle(String title) { panelTitle.setText(title); }
    
    @Override
    public void requestNewGame() {
        requestAuthentication(super::fireNewGame);
    }

    @Override
    public void requestAuthentication(Runnable onSuccess) {
        if (com.curator.state.GameSession.isLoggedIn()) {
            onSuccess.run();
            return;
        }
        pendingAction = onSuccess;
        showAuthScreen(AuthMode.LOGIN, true);
    }

    @Override
    public void swapContent(javafx.scene.Node node) {
        contentPanel.getChildren().setAll(node);
        var fade = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(0.23), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }
}
