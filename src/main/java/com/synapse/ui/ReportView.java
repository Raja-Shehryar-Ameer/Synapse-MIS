package com.synapse.ui;

import com.synapse.controller.ReportController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * UC15: Generate Health Report — config + structured preview + PDF export.
 */
public class ReportView extends VBox {

    private final ReportController controller = new ReportController();
    private final VBox previewBox = new VBox(8);

    public ReportView() {
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

        Label title = new Label("📋 Health Reports");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Generate comprehensive PDF health summaries for your doctor");
        sub.getStyleClass().add("page-subtitle");

        HBox columns = new HBox(20);
        columns.setAlignment(Pos.TOP_LEFT);
        VBox configCard = buildConfigCard();
        configCard.setPrefWidth(380);
        configCard.setMaxWidth(380);
        VBox previewCard = buildPreviewCard();
        HBox.setHgrow(previewCard, Priority.ALWAYS);
        columns.getChildren().addAll(configCard, previewCard);

        content.getChildren().addAll(title, sub, columns);
        return content;
    }

    // ── Config Card ───────────────────────────────────────────

    private VBox buildConfigCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        Label header = new Label("Report Configuration");
        header.getStyleClass().add("card-header");

        DatePicker startDate = new DatePicker(LocalDate.now().minusDays(30));
        DatePicker endDate   = new DatePicker(LocalDate.now());
        startDate.setMaxWidth(Double.MAX_VALUE);
        endDate.setMaxWidth(Double.MAX_VALUE);

        // Category checkboxes (styled)
        Label catLabel = new Label("Include in Report");
        catLabel.getStyleClass().add("form-label");

        CheckBox cbVitals    = styledCheck("❤️  Vitals",           true);
        CheckBox cbSymptoms  = styledCheck("🔍  Symptoms",          true);
        CheckBox cbDiet      = styledCheck("🥗  Diet",              true);
        CheckBox cbHydration = styledCheck("💧  Hydration",          false);
        CheckBox cbWeight    = styledCheck("⚖️  Weight & BMI",       false);

        VBox checksBox = new VBox(10, cbVitals, cbSymptoms, cbDiet, cbHydration, cbWeight);
        checksBox.setStyle("-fx-padding: 2 0;");

        Label feedback = new Label();
        feedback.setWrapText(true);

        Button previewBtn = new Button("Generate Preview");
        previewBtn.getStyleClass().add("button-secondary");
        previewBtn.setMaxWidth(Double.MAX_VALUE);
        previewBtn.setOnAction(e -> {
            var patient = SessionManager.getCurrentPatient();
            if (patient == null) { setFeedback(feedback, "Please log in first.", "error"); return; }
            List<String> cats = getSelected(cbVitals, cbSymptoms, cbDiet, cbHydration, cbWeight);
            Map<String, Object> preview = controller.generatePreview(patient, startDate.getValue(), endDate.getValue(), cats);
            showPreview(preview);
            setFeedback(feedback, "✓ Preview generated.", "success");
        });

        Button exportBtn = new Button("📄  Export as PDF");
        exportBtn.getStyleClass().add("button-primary");
        exportBtn.setMaxWidth(Double.MAX_VALUE);
        exportBtn.setOnAction(e -> {
            var patient = SessionManager.getCurrentPatient();
            if (patient == null) { setFeedback(feedback, "Please log in first.", "error"); return; }
            List<String> cats = getSelected(cbVitals, cbSymptoms, cbDiet, cbHydration, cbWeight);
            exportBtn.setText("⏳  Generating PDF...");
            exportBtn.setDisable(true);
            previewBtn.setDisable(true);
            LocalDate start = startDate.getValue();
            LocalDate end   = endDate.getValue();
            new Thread(() -> {
                String path = controller.exportReport(patient, start, end, cats);
                javafx.application.Platform.runLater(() -> {
                    exportBtn.setText("📄  Export as PDF");
                    exportBtn.setDisable(false);
                    previewBtn.setDisable(false);
                    if (path != null) {
                        setFeedback(feedback, "✓ Report saved: " + path, "success");
                        ToastNotification.show("success", "Health Report Exported",
                                "PDF saved and opened: " + path);
                    } else {
                        setFeedback(feedback, "Export failed.", "error");
                        ToastNotification.show("error", "Export Failed",
                                "Could not generate health report PDF.");
                    }
                });
            }, "pdf-export").start();
        });

        card.getChildren().addAll(header,
                lf("Start Date", startDate),
                lf("End Date", endDate),
                catLabel, checksBox,
                feedback, previewBtn, exportBtn);
        return card;
    }

    // ── Preview Card ──────────────────────────────────────────

    private VBox buildPreviewCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);

        Label header = new Label("Report Preview");
        header.getStyleClass().add("card-header");

        // Placeholder
        VBox placeholder = new VBox(12);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setStyle("-fx-padding: 40 0;");
        Label plIcon = new Label("📋");
        plIcon.setStyle("-fx-font-size: 36px;");
        Label plText = new Label("Click \"Generate Preview\" to see\nyour health data summary here.");
        plText.setStyle("-fx-text-fill: #d1d5db; -fx-font-size: 13px; -fx-text-alignment: center;");
        plText.setWrapText(true);
        placeholder.getChildren().addAll(plIcon, plText);
        previewBox.getChildren().add(placeholder);

        ScrollPane scroll = new ScrollPane(previewBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        card.getChildren().addAll(header, scroll);
        return card;
    }

    private void showPreview(Map<String, Object> preview) {
        previewBox.getChildren().clear();
        if (preview == null || preview.isEmpty()) {
            Label empty = new Label("No data available for the selected period.");
            empty.setStyle("-fx-font-size: 13px;");
            previewBox.getChildren().add(empty);
            return;
        }
        for (Map.Entry<String, Object> entry : preview.entrySet()) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 8 0; -fx-border-color: transparent transparent #f3f4f6 transparent; -fx-border-width: 1;");
            Label key = new Label(entry.getKey());
            key.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-min-width: 160;");
            Label val = new Label(String.valueOf(entry.getValue()));
            val.setStyle("-fx-font-size: 13px;");
            val.setWrapText(true);
            row.getChildren().addAll(key, val);
            previewBox.getChildren().add(row);
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private CheckBox styledCheck(String text, boolean selected) {
        CheckBox cb = new CheckBox(text);
        cb.setSelected(selected);
        return cb;
    }

    private VBox lf(String label, javafx.scene.Node node) {
        Label lbl = new Label(label); lbl.getStyleClass().add("form-label");
        return new VBox(5, lbl, node);
    }

    private void setFeedback(Label l, String msg, String type) {
        l.setText(msg);
        l.getStyleClass().removeAll("feedback-success","feedback-error","feedback-warn");
        l.getStyleClass().add("feedback-" + type);
    }

    private List<String> getSelected(CheckBox... boxes) {
        String[] names = {"vitals","symptoms","diet","hydration","weight"};
        List<String> sel = new ArrayList<>();
        for (int i = 0; i < boxes.length; i++) if (boxes[i].isSelected()) sel.add(names[i]);
        return sel;
    }
}
