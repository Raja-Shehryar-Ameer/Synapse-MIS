package com.synapse.ui;

import com.synapse.controller.SymptomsController;
import com.synapse.model.SymptomLog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * UC3: Log Symptoms view — clean, two-column layout.
 */
public class SymptomsView extends VBox {

    private final SymptomsController controller = new SymptomsController();
    private final VBox historyBox = new VBox(8);

    public SymptomsView() {
        getStyleClass().add("content-area");
        setSpacing(24);
        setPadding(new Insets(36));

        Label title = new Label("Log Symptoms");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Track your symptoms and identify recurring patterns");
        subtitle.getStyleClass().add("page-subtitle");

        HBox columns = new HBox(20);
        columns.setAlignment(Pos.TOP_LEFT);

        VBox formCard = buildFormCard();
        formCard.setMaxWidth(440);
        formCard.setPrefWidth(440);

        VBox histCard = buildHistoryCard();
        HBox.setHgrow(histCard, Priority.ALWAYS);

        columns.getChildren().addAll(formCard, histCard);

        // Pre-load
        var p = SessionManager.getCurrentPatient();
        if (p != null) p.getSymptomLogs().forEach(this::addToHistory);

        getChildren().addAll(title, subtitle, columns);
    }

    // ── Form ──────────────────────────────────────────────────

    private VBox buildFormCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        Label header = new Label("New Symptom Entry");
        header.getStyleClass().add("card-header");

        TextField symptomName = new TextField();
        symptomName.setPromptText("e.g. Headache, Fatigue, Nausea");

        // Severity slider with live value label
        Label sevValue = new Label("5 / 10");
        sevValue.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold; -fx-font-size: 13px;");

        Slider severity = new Slider(1, 10, 5);
        severity.setShowTickLabels(true);
        severity.setShowTickMarks(true);
        severity.setMajorTickUnit(3);
        severity.setMinorTickCount(2);
        severity.setSnapToTicks(true);
        severity.valueProperty().addListener((obs, o, n) -> {
            int v = n.intValue();
            sevValue.setText(v + " / 10");
            String color = v <= 3 ? "#22c55e" : v <= 6 ? "#f59e0b" : "#ef4444";
            sevValue.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 13px;");
        });

        HBox sevRow = new HBox(10, severity, sevValue);
        sevRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(severity, Priority.ALWAYS);

        TextArea notes = new TextArea();
        notes.setPromptText("Additional notes — optional");
        notes.setPrefRowCount(3);

        Label feedback = new Label();
        feedback.setWrapText(true);

        Button submit = new Button("Log Symptom");
        submit.getStyleClass().add("button-primary");
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setOnAction(e -> {
            var patient = SessionManager.getCurrentPatient();
            if (patient == null) { setFeedback(feedback, "Please log in first.", "error"); return; }
            if (symptomName.getText().isBlank()) { setFeedback(feedback, "Enter a symptom name.", "error"); return; }
            try {
                boolean trend = controller.logSymptom(
                        patient, symptomName.getText().trim(),
                        (int) severity.getValue(), notes.getText());
                if (trend) {
                    setFeedback(feedback, "⚠ Logged — recurring pattern detected! Consult a doctor.", "warn");
                } else {
                    setFeedback(feedback, "✓ Symptom logged successfully.", "success");
                }
                var logs = patient.getSymptomLogs();
                addToHistory(logs.get(logs.size() - 1));
                symptomName.clear(); notes.clear(); severity.setValue(5);
            } catch (Exception ex) {
                setFeedback(feedback, "Error: " + ex.getMessage(), "error");
            }
        });

        card.getChildren().addAll(
                header,
                labeledField("Symptom", symptomName),
                labeledNode("Severity (1 = mild, 10 = severe)", sevRow),
                labeledNode("Notes", notes),
                feedback, submit
        );
        return card;
    }

    // ── History ───────────────────────────────────────────────

    private VBox buildHistoryCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        Label header = new Label("Symptom Log");
        header.getStyleClass().add("card-header");

        ScrollPane scroll = new ScrollPane(historyBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        scroll.setPrefHeight(340);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        card.getChildren().addAll(header, scroll);
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private void addToHistory(SymptomLog log) {
        HBox entry = new HBox(10);
        entry.getStyleClass().add("log-entry");
        entry.setAlignment(Pos.CENTER_LEFT);

        int sev = log.getSeverityLevel() != null ? log.getSeverityLevel() : 0;

        VBox info = new VBox(2);
        Label name = new Label(log.getSymptomName());
        name.getStyleClass().add("log-entry-text");
        name.setStyle("-fx-font-weight: bold; ");
        Label detail = new Label("Severity: " + sev + "/10"
                + (log.getNotes() != null && !log.getNotes().isBlank()
                   ? "  ·  " + truncate(log.getNotes(), 40) : ""));
        detail.setStyle("-fx-font-size: 11px;");
        Label ts = new Label(log.getTimestamp() != null
                ? log.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
                : "");
        ts.setStyle("-fx-text-fill: #d1d5db; -fx-font-size: 10px;");
        info.getChildren().addAll(name, detail, ts);
        HBox.setHgrow(info, Priority.ALWAYS);

        String badgeClass = sev <= 3 ? "log-entry-badge"
                          : sev <= 6 ? "log-entry-badge-warn"
                          : "log-entry-badge-red";
        Label badge = new Label(sev + "/10");
        badge.getStyleClass().add(badgeClass);

        Button del = new Button("✕");
        del.getStyleClass().add("log-delete-btn");
        del.setOnAction(e -> {
            javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.CONFIRMATION,
                    "Delete this symptom log for \"" + log.getSymptomName() + "\"?\n\nThis action cannot be undone.",
                    javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
            confirm.setHeaderText("Delete Symptom Log");
            if (confirm.showAndWait().orElse(javafx.scene.control.ButtonType.NO) == javafx.scene.control.ButtonType.YES) {
                var patient = SessionManager.getCurrentPatient();
                if (patient != null) {
                    controller.deleteSymptomLog(patient, log.getLogId());
                    historyBox.getChildren().remove(entry);
                }
            }
        });

        entry.getChildren().addAll(info, badge, del);
        historyBox.getChildren().add(0, entry);
    }

    // ── Helpers ───────────────────────────────────────────────

    private VBox labeledField(String label, TextField tf) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("form-label");
        return new VBox(5, lbl, tf);
    }

    private VBox labeledNode(String label, javafx.scene.Node node) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("form-label");
        return new VBox(5, lbl, node);
    }

    private void setFeedback(Label lbl, String msg, String type) {
        lbl.setText(msg);
        lbl.getStyleClass().removeAll("feedback-success", "feedback-error", "feedback-warn");
        lbl.getStyleClass().add("feedback-" + type);
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
