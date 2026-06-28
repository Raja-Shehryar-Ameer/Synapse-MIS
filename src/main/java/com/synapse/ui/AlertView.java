package com.synapse.ui;

import com.synapse.model.Patient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * System Alerts view — live notification log + manual test triggers.
 */
public class AlertView extends VBox {

    /** In-memory log of all toasts shown this session */
    private static final List<NotifEntry> LOG = new ArrayList<>();
    private static AlertView liveInstance;

    private final VBox logBox = new VBox(8);

    public AlertView() {
        liveInstance = this;
        setFillWidth(true);
        getStyleClass().add("root-pane");

        ScrollPane scroll = new ScrollPane(buildContent());
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);

        // Render existing log entries
        LOG.forEach(this::addLogRow);
    }

    private VBox buildContent() {
        VBox content = new VBox(24);
        content.setPadding(new Insets(36));
        content.getStyleClass().add("root-pane");

        Label title = new Label("🔔 System Alerts");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Reminders and notifications from Synapse — this session");
        sub.getStyleClass().add("page-subtitle");

        // ── How it works card ──────────────────────────────────
        VBox howCard = new VBox(14);
        howCard.getStyleClass().add("card");
        Label howTitle = new Label("How Notifications Work");
        howTitle.getStyleClass().add("card-header");

        String[][] rules = {
            {"📅", "Appointment / Follow-up", "30 min before the scheduled time"},
            {"🏃", "Exercise events",          "15 min before the scheduled time"},
            {"💊", "Medication (calendar)",    "5 min before the scheduled time"},
            {"💊", "Prescription doses",       "Exactly at the scheduled dose time"},
            {"⚠️", "Low stock",                "When a medicine drops to 5 or fewer units"},
            {"⏰", "Near expiry",              "When any medicine expires within 30 days"},
            {"💧", "Hydration",                "When water intake is below your daily goal"},
        };

        for (String[] r : rules) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 2 0;");
            Label ico = new Label(r[0]); ico.setStyle("-fx-font-size: 16px;"); ico.setMinWidth(24);
            Label type = new Label(r[1]);
            type.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-min-width: 190;");
            Label desc = new Label("→  " + r[2]);
            desc.setStyle("-fx-font-size: 12px; ");
            row.getChildren().addAll(ico, type, desc);
            howCard.getChildren().add(row);
        }
        howCard.getChildren().add(0, howTitle);

        // ── Manual test panel ──────────────────────────────────
        VBox testCard = new VBox(12);
        testCard.getStyleClass().add("card");
        Label testTitle = new Label("Test Notifications");
        testTitle.getStyleClass().add("card-header");

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        btnRow.getChildren().addAll(
            testBtn("💊 Medication",    "medicine", "Time for Paracetamol",    "Take 500mg  ·  Twice Daily"),
            testBtn("📅 Appointment",  "event",    "Upcoming: Dr. Smith",     "Starts at 14:30  ·  30 min before"),
            testBtn("⚠️ Low Stock",    "warn",     "Low Medicine Stock",      "Ibuprofen — 5 units remaining"),
            testBtn("💧 Hydration",    "info",     "Hydration Reminder",      "You still need 800 ml for today. 💧"),
            testBtn("✅ Success",       "success",  "Vitals Logged",           "Your readings look normal today.")
        );

        testCard.getChildren().addAll(testTitle, btnRow);

        // ── Notification log ───────────────────────────────────
        VBox logCard = new VBox(12);
        logCard.getStyleClass().add("card");
        VBox.setVgrow(logCard, Priority.ALWAYS);

        Label logTitle = new Label("Notification Log (This Session)");
        logTitle.getStyleClass().add("card-header");

        Button clearBtn = new Button("Clear Log");
        clearBtn.getStyleClass().add("button-secondary");
        clearBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10;");
        clearBtn.setOnAction(e -> { LOG.clear(); logBox.getChildren().clear(); addEmptyState(); });

        HBox logHeader = new HBox(10, logTitle, new Region(), clearBtn);
        HBox.setHgrow(logHeader.getChildren().get(1), Priority.ALWAYS);
        logHeader.setAlignment(Pos.CENTER_LEFT);

        ScrollPane logScroll = new ScrollPane(logBox);
        logScroll.setFitToWidth(true);
        logScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        logScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        logScroll.setPrefHeight(300);

        if (logBox.getChildren().isEmpty()) addEmptyState();

        logCard.getChildren().addAll(logHeader, logScroll);

        content.getChildren().addAll(title, sub, howCard, testCard, logCard);
        return content;
    }

    private Button testBtn(String label, String type, String title, String msg) {
        Button btn = new Button(label);
        btn.setStyle(
            "-fx-background-color: #f9fafb; " +
            "-fx-border-color: #e5e7eb; -fx-border-width: 1; -fx-border-radius: 8;" +
            "-fx-background-radius: 8; -fx-font-size: 11px; -fx-cursor: hand;" +
            "-fx-padding: 6 12;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: #fffbeb; -fx-text-fill: #b45309;" +
            "-fx-border-color: #f59e0b; -fx-border-width: 1; -fx-border-radius: 8;" +
            "-fx-background-radius: 8; -fx-font-size: 11px; -fx-cursor: hand;" +
            "-fx-padding: 6 12;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: #f9fafb; " +
            "-fx-border-color: #e5e7eb; -fx-border-width: 1; -fx-border-radius: 8;" +
            "-fx-background-radius: 8; -fx-font-size: 11px; -fx-cursor: hand;" +
            "-fx-padding: 6 12;"
        ));
        btn.setOnAction(e -> {
            ToastNotification.show(type, title, msg);
            logEntry(type, title, msg);
        });
        return btn;
    }

    private void addEmptyState() {
        Label empty = new Label("No notifications yet this session.");
        empty.setStyle("-fx-text-fill: #d1d5db; -fx-font-size: 13px;");
        logBox.getChildren().add(empty);
    }

    // ── Static log API (called by NotificationScheduler) ──────────

    /**
     * Call this to add entries to the session log.
     * Safe from any thread.
     */
    public static void logEntry(String type, String title, String message) {
        NotifEntry entry = new NotifEntry(type, title, message,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm  d MMM")));
        LOG.add(0, entry);
        javafx.application.Platform.runLater(() -> {
            if (liveInstance != null) {
                liveInstance.logBox.getChildren().removeIf(
                        n -> n instanceof Label && ((Label) n).getText().contains("No notifications"));
                liveInstance.addLogRow(entry);
            }
        });
    }

    private void addLogRow(NotifEntry entry) {
        HBox row = new HBox(12);
        row.getStyleClass().add("log-entry");
        row.setAlignment(Pos.CENTER_LEFT);

        Label ico = new Label(iconFor(entry.type));
        ico.setStyle("-fx-font-size: 16px;");

        VBox info = new VBox(2);
        Label titleLbl = new Label(entry.title);
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Label msgLbl = new Label(entry.message != null ? entry.message : "");
        msgLbl.setStyle("-fx-font-size: 11px;");
        msgLbl.setWrapText(true);
        info.getChildren().addAll(titleLbl, msgLbl);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label time = new Label(entry.timestamp);
        time.setStyle("-fx-text-fill: #d1d5db; -fx-font-size: 10px;");

        Label badge = new Label(entry.type);
        badge.setStyle(
            "-fx-background-color: " + badgeBg(entry.type) + ";" +
            "-fx-text-fill: " + badgeFg(entry.type) + ";" +
            "-fx-font-size: 10px; -fx-font-weight: bold;" +
            "-fx-padding: 2 7; -fx-background-radius: 10;"
        );

        row.getChildren().addAll(ico, info, badge, time);
        logBox.getChildren().add(0, row);
    }

    private String iconFor(String type) {
        return switch (type) {
            case "medicine" -> "💊";
            case "event"    -> "📅";
            case "warn"     -> "⚠️";
            case "error"    -> "❌";
            case "success"  -> "✅";
            default         -> "🔔";
        };
    }
    private String badgeBg(String type) {
        return switch (type) {
            case "medicine" -> "#dcfce7"; case "event" -> "#dbeafe";
            case "warn" -> "#fef3c7"; case "error" -> "#fee2e2";
            case "success" -> "#dcfce7"; default -> "#f3f4f6";
        };
    }
    private String badgeFg(String type) {
        return switch (type) {
            case "medicine","success" -> "#16a34a"; case "event" -> "#1d4ed8";
            case "warn" -> "#b45309"; case "error" -> "#dc2626"; default -> "#374151";
        };
    }

    private record NotifEntry(String type, String title, String message, String timestamp) {}
}
