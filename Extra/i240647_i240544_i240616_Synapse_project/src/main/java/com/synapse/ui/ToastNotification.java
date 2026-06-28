package com.synapse.ui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * In-app toast notification — slides in from top-right, auto-dismisses.
 * Type: "info" | "warn" | "success" | "error" | "medicine" | "event"
 */
public class ToastNotification {

    private static Stage ownerStage;

    /** Call once at startup to set the owner window. */
    public static void setOwner(Stage stage) {
        ownerStage = stage;
    }

    /**
     * Show a toast. Safe to call from any thread.
     *
     * @param type     "info" | "warn" | "success" | "error" | "medicine" | "event"
     * @param title    Bold title line
     * @param message  Body text (can be null)
     * @param snoozeRunnable If not null, a "Snooze" button appears; clicking calls this
     * @param ackRunnable    If not null, an "OK / Taken" button appears
     */
    public static void show(String type, String title, String message,
                            Runnable snoozeRunnable, Runnable ackRunnable) {
        Platform.runLater(() -> showOnFxThread(type, title, message, snoozeRunnable, ackRunnable));
    }

    /** Convenience — no action buttons, auto-dismiss only */
    public static void show(String type, String title, String message) {
        show(type, title, message, null, null);
    }

    // ── Internal ───────────────────────────────────────────────

    private static void showOnFxThread(String type, String title, String message,
                                        Runnable snoozeRunnable, Runnable ackRunnable) {
        if (ownerStage == null || !ownerStage.isShowing()) return;

        Popup popup = new Popup();
        popup.setAutoFix(false);
        popup.setHideOnEscape(true);

        VBox card = buildCard(type, title, message, snoozeRunnable, ackRunnable, popup);

        popup.getContent().add(card);

        // Position: top-right of ownerStage
        double x = ownerStage.getX() + ownerStage.getWidth() - 360 - 16;
        double y = ownerStage.getY() + 60;
        popup.show(ownerStage, x, y - 80);  // start above

        // Slide down + fade in
        card.setOpacity(0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(350), card);
        slide.setFromY(-60); slide.setToY(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(350), card);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        ParallelTransition enter = new ParallelTransition(slide, fadeIn);
        enter.play();

        // (Removed logging to AlertView as it has been retired)

        // Auto-dismiss after duration based on type
        boolean hasActions = snoozeRunnable != null || ackRunnable != null;
        int holdMs = hasActions ? 30_000 : 5_000;  // medical alerts persist longer

        PauseTransition hold = new PauseTransition(Duration.millis(holdMs));
        hold.setOnFinished(e -> dismiss(popup, card));
        hold.play();

        // Update position if window moves
        ownerStage.xProperty().addListener((obs, o, n) ->
                popup.setX(n.doubleValue() + ownerStage.getWidth() - 360 - 16));
        ownerStage.yProperty().addListener((obs, o, n) ->
                popup.setY(n.doubleValue() + 60));
    }

    private static VBox buildCard(String type, String title, String message,
                                   Runnable snoozeRunnable, Runnable ackRunnable,
                                   Popup popup) {
        VBox card = new VBox(10);
        card.setPrefWidth(340);
        card.setStyle(
            "-fx-background-color: #ffffff;" +
            "-fx-background-radius: 14;" +
            "-fx-border-color: " + borderColor(type) + ";" +
            "-fx-border-width: 0 0 0 4;" +   // thick left accent
            "-fx-border-radius: 14;" +
            "-fx-padding: 16 18;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.16), 20, 0, 0, 6);"
        );

        // Top row: icon + title + close
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label ico = new Label(icon(type));
        ico.setStyle("-fx-font-size: 18px;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; ");
        titleLbl.setWrapText(true);
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 0 4;"
        );
        closeBtn.setOnAction(e -> dismiss(popup, card));

        topRow.getChildren().addAll(ico, titleLbl, closeBtn);

        card.getChildren().add(topRow);

        // Message body
        if (message != null && !message.isBlank()) {
            Label msgLbl = new Label(message);
            msgLbl.setStyle("-fx-font-size: 12px;");
            msgLbl.setWrapText(true);
            card.getChildren().add(msgLbl);
        }

        // Action buttons
        if (ackRunnable != null || snoozeRunnable != null) {
            HBox btnRow = new HBox(8);
            btnRow.setAlignment(Pos.CENTER_RIGHT);

            if (snoozeRunnable != null) {
                Button snoozeBtn = new Button("Snooze 15 min");
                snoozeBtn.setStyle(
                    "-fx-background-color: #f3f4f6; " +
                    "-fx-font-size: 11px; -fx-font-weight: bold;" +
                    "-fx-padding: 5 12; -fx-background-radius: 7; -fx-cursor: hand;"
                );
                snoozeBtn.setOnAction(e -> { dismiss(popup, card); snoozeRunnable.run(); });
                btnRow.getChildren().add(snoozeBtn);
            }

            if (ackRunnable != null) {
                String ackLabel = type.equals("medicine") ? "✓  Taken" : "✓  OK";
                Button ackBtn = new Button(ackLabel);
                ackBtn.setStyle(
                    "-fx-background-color: " + accentColor(type) + ";" +
                    "-fx-text-fill: #ffffff;" +
                    "-fx-font-size: 11px; -fx-font-weight: bold;" +
                    "-fx-padding: 5 14; -fx-background-radius: 7; -fx-cursor: hand;"
                );
                ackBtn.setOnAction(e -> { dismiss(popup, card); ackRunnable.run(); });
                btnRow.getChildren().add(ackBtn);
            }

            card.getChildren().add(btnRow);
        }

        return card;
    }

    private static void dismiss(Popup popup, VBox card) {
        FadeTransition out = new FadeTransition(Duration.millis(300), card);
        out.setFromValue(card.getOpacity()); out.setToValue(0);
        out.setOnFinished(e -> popup.hide());
        out.play();
    }

    private static String icon(String type) {
        return switch (type) {
            case "medicine" -> "💊";
            case "event"    -> "📅";
            case "warn"     -> "⚠️";
            case "error"    -> "❌";
            case "success"  -> "✅";
            default         -> "🔔";
        };
    }

    private static String borderColor(String type) {
        return switch (type) {
            case "medicine" -> "#22c55e";
            case "event"    -> "#3b82f6";
            case "warn"     -> "#f59e0b";
            case "error"    -> "#ef4444";
            case "success"  -> "#22c55e";
            default         -> "#9ca3af";
        };
    }

    private static String accentColor(String type) {
        return switch (type) {
            case "medicine" -> "#22c55e";
            case "event"    -> "#3b82f6";
            case "warn"     -> "#f59e0b";
            case "error"    -> "#ef4444";
            default         -> "#22c55e";
        };
    }
}
