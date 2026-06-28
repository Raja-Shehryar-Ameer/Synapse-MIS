package com.synapse.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Full-screen overlay that plays the Lottie wave animation on login.
 * Shown for ~3.5 seconds, then calls onFinished to reveal the main app.
 */
public class WaveLoadingOverlay extends StackPane {

    private final Runnable onFinished;
    private WebEngine engine;

    public WaveLoadingOverlay(Runnable onFinished) {
        this.onFinished = onFinished;
        getStyleClass().add("wave-overlay");
        setStyle("-fx-background-color: #020817;");
        buildWebView();
    }

    private void buildWebView() {
        WebView webView = new WebView();
        webView.setStyle("-fx-background-color: transparent;");
        engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        // Transparent background for WebView
        try {
            webView.getEngine().setUserStyleSheetLocation("");
        } catch (Exception ignored) {}

        URL htmlUrl = getClass().getResource("/lottie-player.html");
        if (htmlUrl != null) {
            engine.load(htmlUrl.toExternalForm());
        }

        // Once page loaded, inject the lottie JSON
        engine.documentProperty().addListener((obs, old, doc) -> {
            if (doc != null) {
                Platform.runLater(this::injectAnimation);
            }
        });

        getChildren().add(webView);
    }

    private void injectAnimation() {
        try {
            InputStream is = getClass().getResourceAsStream("/Wave Animation.json");
            if (is == null) return;
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            // Escape for JS injection
            String escaped = json.replace("\\", "\\\\").replace("'", "\\'");
            engine.executeScript("window.playWave('" + escaped + "')");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Start the overlay and schedule dismiss after 3.5 seconds. */
    public void start() {
        Timeline timer = new Timeline(
            new KeyFrame(Duration.millis(3500), e -> {
                onFinished.run();
            })
        );
        timer.play();
    }
}
