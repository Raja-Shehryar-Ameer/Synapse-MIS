package com.synapse.ui;

import com.synapse.controller.JournalController;
import com.synapse.model.JournalEntry;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * UC5: Write Journal Entry — editor-style layout with mood tags.
 */
public class JournalView extends VBox {

    private final JournalController controller = new JournalController();
    private final VBox historyBox = new VBox(10);

    public JournalView() {
        setFillWidth(true);
        getStyleClass().add("root-pane");

        ScrollPane scroll = new ScrollPane(buildContent());
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);
    }

    private VBox buildContent() {
        VBox content = new VBox(24);
        content.setPadding(new Insets(36));
        content.getStyleClass().add("root-pane");

        Label title = new Label("📓 Journal");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Reflect on your day, track mood and wellness patterns");
        sub.getStyleClass().add("page-subtitle");

        HBox columns = new HBox(20);
        columns.setAlignment(Pos.TOP_LEFT);

        VBox editorCard = buildEditorCard();
        editorCard.setPrefWidth(460);
        editorCard.setMaxWidth(460);
        VBox histCard = buildHistoryCard();
        HBox.setHgrow(histCard, Priority.ALWAYS);

        columns.getChildren().addAll(editorCard, histCard);

        // Pre-load
        var p = SessionManager.getCurrentPatient();
        if (p != null) p.getJournalEntries().forEach(this::addToHistory);

        content.getChildren().addAll(title, sub, columns);
        return content;
    }

    // ── Editor ────────────────────────────────────────────────

    private VBox buildEditorCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        Label header = new Label("New Entry");
        header.getStyleClass().add("card-header");

        // Mood picker row (emoji buttons)
        Label moodLabel = new Label("How are you feeling?");
        moodLabel.getStyleClass().add("form-label");

        String[] moods = {"😊", "😐", "😔", "😤", "😴", "💪", "🤒", "😌"};
        String[] moodNames = {"Happy", "Neutral", "Sad", "Stressed", "Tired", "Energetic", "Unwell", "Calm"};
        final String[] chosenMood = {""};

        HBox moodRow = new HBox(8);
        moodRow.setAlignment(Pos.CENTER_LEFT);
        ToggleGroup tg = new ToggleGroup();
        for (int i = 0; i < moods.length; i++) {
            final String moodVal = moods[i] + " " + moodNames[i];
            ToggleButton tb = new ToggleButton(moods[i]);
            tb.setToggleGroup(tg);
            tb.setStyle(
                "-fx-background-color: #f9fafb;" +
                "-fx-border-color: #e5e7eb;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-font-size: 18px;" +
                "-fx-padding: 6 10;" +
                "-fx-cursor: hand;"
            );
            tb.selectedProperty().addListener((obs, o, sel) -> {
                if (sel) {
                    chosenMood[0] = moodVal;
                    tb.setStyle(
                        "-fx-background-color: #fef3c7;" +
                        "-fx-border-color: #f59e0b;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-size: 18px;" +
                        "-fx-padding: 6 10;" +
                        "-fx-cursor: hand;"
                    );
                } else {
                    tb.setStyle(
                        "-fx-background-color: #f9fafb;" +
                        "-fx-border-color: #e5e7eb;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-size: 18px;" +
                        "-fx-padding: 6 10;" +
                        "-fx-cursor: hand;"
                    );
                }
            });
            moodRow.getChildren().add(tb);
        }

        // Editor textarea
        Label contentLabel = new Label("Your thoughts");
        contentLabel.getStyleClass().add("form-label");

        TextArea content = new TextArea();
        content.setPromptText("Write freely… what's on your mind today?");
        content.setPrefRowCount(9);
        content.setWrapText(true);
        content.setStyle(
            "-fx-background-color: #fafafa;" +
            "-fx-border-color: #e5e7eb;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;" +
            "-fx-border-width: 1.5;" +
            "-fx-padding: 12;" +
            "-fx-font-size: 13px;" +
            ""
        );

        Label feedback = new Label();
        feedback.setWrapText(true);

        Button saveBtn = new Button("Save Entry");
        saveBtn.getStyleClass().add("button-primary");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> {
            var patient = SessionManager.getCurrentPatient();
            if (patient == null) { setFeedback(feedback, "Please log in first.", "error"); return; }
            if (content.getText().isBlank()) { setFeedback(feedback, "Write something first.", "error"); return; }
            try {
                controller.saveJournalEntry(patient, content.getText(), chosenMood[0]);
                setFeedback(feedback, "✓ Journal entry saved.", "success");
                var logs = patient.getJournalEntries();
                addToHistory(logs.get(logs.size() - 1));
                content.clear(); tg.selectToggle(null); chosenMood[0] = "";
            } catch (Exception ex) {
                setFeedback(feedback, "Error: " + ex.getMessage(), "error");
            }
        });

        card.getChildren().addAll(header, moodLabel, moodRow, contentLabel, content, feedback, saveBtn);
        return card;
    }

    // ── History ───────────────────────────────────────────────

    private VBox buildHistoryCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        Label header = new Label("Recent Entries");
        header.getStyleClass().add("card-header");

        ScrollPane scroll = new ScrollPane(historyBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        scroll.setPrefHeight(400);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        card.getChildren().addAll(header, scroll);
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private void addToHistory(JournalEntry entryObj) {
        String text = entryObj.getContentText();
        String mood = entryObj.getMoodTag() != null ? entryObj.getMoodTag() : "";

        VBox entry = new VBox(8);
        entry.getStyleClass().add("log-entry");

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        if (!mood.isBlank()) {
            Label moodBadge = new Label(mood);
            moodBadge.getStyleClass().add("badge-amber");
            top.getChildren().add(moodBadge);
        }

        Label ts = new Label(entryObj.getTimestamp() != null
                ? entryObj.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
                : "");
        ts.setStyle("-fx-text-fill: #d1d5db; -fx-font-size: 10px;");
        top.getChildren().add(ts);

        HBox.setHgrow(top, Priority.ALWAYS);

        Button del = new Button("✕");
        del.getStyleClass().add("log-delete-btn");
        del.setOnAction(e -> {
            javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.CONFIRMATION,
                    "Delete this journal entry?\n\nThis action cannot be undone.",
                    javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
            confirm.setHeaderText("Delete Journal Entry");
            if (confirm.showAndWait().orElse(javafx.scene.control.ButtonType.NO) == javafx.scene.control.ButtonType.YES) {
                var patient = SessionManager.getCurrentPatient();
                if (patient != null) {
                    controller.deleteJournalEntry(patient, entryObj.getEntryId());
                    historyBox.getChildren().remove(entry);
                }
            }
        });
        top.getChildren().addAll(new HBox(), del); // spacer + del
        HBox.setHgrow(top.getChildren().get(top.getChildren().size() - 2), Priority.ALWAYS);

        String preview = text.length() > 140 ? text.substring(0, 140) + "…" : text;
        Label textLbl = new Label(preview);
        textLbl.setStyle("-fx-font-size: 12px;");
        textLbl.setWrapText(true);

        entry.getChildren().addAll(top, textLbl);
        historyBox.getChildren().add(0, entry);
    }

    private void setFeedback(Label l, String msg, String type) {
        l.setText(msg);
        l.getStyleClass().removeAll("feedback-success", "feedback-error", "feedback-warn");
        l.getStyleClass().add("feedback-" + type);
    }
}
