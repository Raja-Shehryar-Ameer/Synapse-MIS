package com.synapse.ui;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Animated splash screen shown during app startup.
 * Fades in, pulses loading dots, then fades out and calls onDone.
 */
public class SplashScreen {

    private final Stage stage;
    private final StackPane root;

    public SplashScreen(Stage owner) {
        stage = new Stage(StageStyle.UNDECORATED);
        stage.initOwner(owner);

        root = buildUI();
        Scene scene = new Scene(root, 400, 280);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();

        // Fade in
        root.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        fadeIn.play();
    }

    private StackPane buildUI() {
        VBox card = new VBox(18);
        card.setAlignment(Pos.CENTER);
        card.setStyle(
            "-fx-background-color: #ffffff;" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: #eaecef;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 28, 0, 0, 8);"
        );
        card.setMaxSize(360, 248);
        card.setMinSize(360, 248);

        // Outer wrapper for background
        StackPane wrapper = new StackPane(card);
        wrapper.setStyle("-fx-background-color: transparent;");
        wrapper.setMinSize(400, 280);

        // Logo badge
        Label logo = new Label("S");
        logo.setStyle(
            "-fx-background-color: #22c55e;" +
            "-fx-text-fill: #ffffff;" +
            "-fx-font-size: 28px;" +
            "-fx-font-weight: bold;" +
            "-fx-pref-width: 64; -fx-pref-height: 64;" +
            "-fx-background-radius: 16;" +
            "-fx-alignment: center;"
        );

        Label name = new Label("SYNAPSE");
        name.setStyle(
            "-fx-font-size: 22px; -fx-font-weight: bold;" +
            "-fx-letter-spacing: 5px;"
        );

        Label tagline = new Label("Healthcare · Simplified");
        tagline.setStyle("-fx-font-size: 12px; ");

        // Three pulsing dots
        HBox dots = new HBox(10);
        dots.setAlignment(Pos.CENTER);
        for (int i = 0; i < 3; i++) {
            Circle dot = new Circle(5, Color.web("#22c55e"));
            dot.setOpacity(0.3);
            ScaleTransition pulse = new ScaleTransition(Duration.millis(550), dot);
            pulse.setFromX(0.6); pulse.setToX(1.3);
            pulse.setFromY(0.6); pulse.setToY(1.3);
            pulse.setAutoReverse(true);
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.setDelay(Duration.millis(i * 180));
            pulse.play();
            FadeTransition ft = new FadeTransition(Duration.millis(550), dot);
            ft.setFromValue(0.3); ft.setToValue(1.0);
            ft.setAutoReverse(true);
            ft.setCycleCount(Animation.INDEFINITE);
            ft.setDelay(Duration.millis(i * 180));
            ft.play();
            dots.getChildren().add(dot);
        }

        card.getChildren().addAll(logo, name, tagline, dots);
        return wrapper;
    }

    /**
     * Call after 2+ seconds to smoothly close and trigger the main app show.
     */
    public void dismiss(Runnable onDone) {
        PauseTransition hold = new PauseTransition(Duration.seconds(2.4));
        hold.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), root);
            fadeOut.setFromValue(1); fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> {
                stage.close();
                onDone.run();
            });
            fadeOut.play();
        });
        hold.play();
    }
}
