package com.synapse.ui;

import com.synapse.controller.VitalsController;
import com.synapse.model.VitalLog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * UC2: Log Vitals — blood pressure, heart rate, temperature.
 */
public class VitalsView extends VBox {

    private final VitalsController controller = new VitalsController();
    private final VBox historyBox = new VBox(8);

    public VitalsView() {
        getStyleClass().add("content-area");
        setSpacing(24);
        setPadding(new Insets(36));

        // ── Header ─────────────────────────────────────────────
        Label title = new Label("Log Vitals");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Record your blood pressure, heart rate and temperature");
        subtitle.getStyleClass().add("page-subtitle");

        // ── Two-column layout ──────────────────────────────────
        HBox columns = new HBox(20);
        columns.setAlignment(Pos.TOP_LEFT);

        VBox formCard = buildFormCard();
        formCard.setMaxWidth(420);
        formCard.setPrefWidth(420);

        VBox histCard = buildHistoryCard();
        HBox.setHgrow(histCard, Priority.ALWAYS);

        columns.getChildren().addAll(formCard, histCard);

        // Pre-load history
        var patient = SessionManager.getCurrentPatient();
        if (patient != null) {
            patient.getVitalLogs().forEach(this::addToHistory);
        }

        getChildren().addAll(title, subtitle, columns);
    }

    // ── Form Card ─────────────────────────────────────────────

    private VBox buildFormCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        Label header = new Label("New Reading");
        header.getStyleClass().add("card-header");

        TextField bp   = field("e.g. 120/80");
        TextField hr   = field("e.g. 72");
        TextField temp = field("e.g. 98.6");

        Label feedback = new Label();

        Button submit = new Button("Log Vitals");
        submit.getStyleClass().add("button-primary");
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setOnAction(e -> {
            var patient = SessionManager.getCurrentPatient();
            if (patient == null) { setFeedback(feedback, "Please log in first.", "error"); return; }
            try {
                Integer heartRate   = hr.getText().isBlank()   ? null : Integer.parseInt(hr.getText().trim());
                Double temperature  = temp.getText().isBlank() ? null : Double.parseDouble(temp.getText().trim());

                VitalLog log = controller.logVitals(patient, bp.getText().trim(), heartRate, temperature);
                if (log == null) {
                    setFeedback(feedback, "Invalid readings — check your values.", "error");
                } else if (Boolean.TRUE.equals(log.getIsAbnormal())) {
                    setFeedback(feedback, "⚠ Logged — ABNORMAL readings detected! Consult a doctor.", "warn");
                    addToHistory(log);
                } else {
                    setFeedback(feedback, "✓ Vitals logged successfully.", "success");
                    addToHistory(log);
                }
                bp.clear(); hr.clear(); temp.clear();
            } catch (NumberFormatException ex) {
                setFeedback(feedback, "Please enter valid numbers.", "error");
            } catch (Exception ex) {
                setFeedback(feedback, "Error: " + ex.getMessage(), "error");
            }
        });

        card.getChildren().addAll(
                header,
                labeledField("Blood Pressure", bp),
                labeledField("Heart Rate (BPM)", hr),
                labeledField("Temperature (°F)", temp),
                feedback, submit
        );
        return card;
    }

    // ── History Card ──────────────────────────────────────────

    private VBox buildHistoryCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        Label header = new Label("Recent Readings");
        header.getStyleClass().add("card-header");

        ScrollPane scroll = new ScrollPane(historyBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        scroll.setPrefHeight(320);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        card.getChildren().addAll(header, scroll);
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private void addToHistory(VitalLog log) {
        HBox entry = new HBox(10);
        entry.getStyleClass().add("log-entry");
        entry.setAlignment(Pos.CENTER_LEFT);

        boolean abnormal = Boolean.TRUE.equals(log.getIsAbnormal());

        VBox info = new VBox(2);
        Label main = new Label(
            "BP: " + nvl(log.getBloodPressure()) +
            "   HR: " + nvl(log.getHeartRateBpm()) + " bpm" +
            "   Temp: " + nvl(log.getTemperatureF()) + "°F"
        );
        main.getStyleClass().add("log-entry-text");
        Label ts = new Label(log.getTimestamp() != null
                ? log.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
                : "");
        ts.setStyle("-fx-text-fill: #d1d5db; -fx-font-size: 10px;");
        info.getChildren().addAll(main, ts);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label badge = new Label(abnormal ? "⚠ Abnormal" : "✓ Normal");
        badge.getStyleClass().add(abnormal ? "log-entry-badge-warn" : "log-entry-badge");

        Button del = new Button("✕");
        del.getStyleClass().add("log-delete-btn");
        del.setOnAction(e -> {
            javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.CONFIRMATION,
                    "Delete this vital log entry?\n\nThis action cannot be undone.",
                    javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
            confirm.setHeaderText("Delete Vital Log");
            if (confirm.showAndWait().orElse(javafx.scene.control.ButtonType.NO) == javafx.scene.control.ButtonType.YES) {
                var patient = SessionManager.getCurrentPatient();
                if (patient != null) {
                    controller.deleteVitalLog(patient, log.getLogId());
                    historyBox.getChildren().remove(entry);
                }
            }
        });

        entry.getChildren().addAll(info, badge, del);
        historyBox.getChildren().add(0, entry);
    }

    // ── Helpers ───────────────────────────────────────────────

    private TextField field(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        return tf;
    }

    private VBox labeledField(String label, TextField tf) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("form-label");
        VBox box = new VBox(5, lbl, tf);
        return box;
    }

    private void setFeedback(Label lbl, String msg, String type) {
        lbl.setText(msg);
        lbl.getStyleClass().removeAll("feedback-success", "feedback-error", "feedback-warn");
        lbl.getStyleClass().add("feedback-" + type);
    }

    private String nvl(Object o) { return o != null ? o.toString() : "—"; }
}
