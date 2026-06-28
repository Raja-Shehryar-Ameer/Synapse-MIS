package com.synapse.ui;

import com.synapse.controller.AnalyticsController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.Map;

/**
 * UC14: Health Analytics — summary stats + bar chart with filter bar.
 */
public class AnalyticsView extends VBox {

    private final AnalyticsController controller = new AnalyticsController();
    private final VBox chartsArea = new VBox(20);

    public AnalyticsView() {
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

        Label title = new Label("📊 Health Analytics");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Visualise your health trends over time");
        sub.getStyleClass().add("page-subtitle");

        // ── Filter bar ─────────────────────────────────────────
        HBox filterBar = new HBox(14);
        filterBar.getStyleClass().add("card");
        filterBar.setAlignment(Pos.CENTER_LEFT);

        Label catLbl = new Label("Category");
        catLbl.getStyleClass().add("form-label");
        ComboBox<String> category = new ComboBox<>();
        category.getItems().addAll("all", "vitals", "symptoms", "diet", "hydration");
        category.setValue("all");

        Label rangeLbl = new Label("Time Range");
        rangeLbl.getStyleClass().add("form-label");
        ComboBox<String> timeRange = new ComboBox<>();
        timeRange.getItems().addAll("7days", "30days", "90days", "1year");
        timeRange.setValue("30days");

        Button refreshBtn = new Button("Load Analytics");
        refreshBtn.getStyleClass().add("button-primary");
        refreshBtn.setOnAction(e -> loadAnalytics(category.getValue(), timeRange.getValue(), content));

        filterBar.getChildren().addAll(catLbl, category, rangeLbl, timeRange, refreshBtn);

        content.getChildren().addAll(title, sub, filterBar, chartsArea);

        // Load on first open
        loadAnalytics("all", "30days", content);
        return content;
    }

    private void loadAnalytics(String category, String timeRange, VBox content) {
        chartsArea.getChildren().clear();

        var patient = SessionManager.getCurrentPatient();
        if (patient == null) {
            Label err = new Label("Please log in to view analytics.");
            err.getStyleClass().add("feedback-error");
            chartsArea.getChildren().add(err);
            return;
        }

        Map<String, Object> report = controller.updateAnalytics(patient, category, timeRange);

        // ── Summary stat row ───────────────────────────────────
        HBox stats = new HBox(14);
        stats.setAlignment(Pos.CENTER_LEFT);

        if (report.containsKey("vitalCount"))
            stats.getChildren().add(statTile("❤️", "Vitals", report.get("vitalCount").toString()));
        if (report.containsKey("symptomCount"))
            stats.getChildren().add(statTile("🔍", "Symptoms", report.get("symptomCount").toString()));
        if (report.containsKey("dietCount"))
            stats.getChildren().add(statTile("🥗", "Diet Logs", report.get("dietCount").toString()));
        if (report.containsKey("hydrationCount"))
            stats.getChildren().add(statTile("💧", "Hydration", report.get("hydrationCount").toString()));
        if (report.containsKey("avgHeartRate"))
            stats.getChildren().add(statTile("💓", "Avg HR", report.get("avgHeartRate") + " bpm"));
        if (report.containsKey("avgCalories"))
            stats.getChildren().add(statTile("🔥", "Avg Calories", report.get("avgCalories") + " cal"));
        if (report.get("bmi") != null)
            stats.getChildren().add(statTile("⚖️", "BMI", report.get("bmi").toString()));

        chartsArea.getChildren().add(stats);

        // ── Bar chart ──────────────────────────────────────────
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Health Category");
        yAxis.setLabel("Count");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Health Activity — " + timeRange);
        barChart.setPrefHeight(300);
        barChart.setLegendVisible(false);
        barChart.setAnimated(true);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Data");

        if (report.containsKey("vitalCount"))
            series.getData().add(new XYChart.Data<>("Vitals", (Number) report.get("vitalCount")));
        if (report.containsKey("symptomCount"))
            series.getData().add(new XYChart.Data<>("Symptoms", (Number) report.get("symptomCount")));
        if (report.containsKey("dietCount"))
            series.getData().add(new XYChart.Data<>("Diet", (Number) report.get("dietCount")));
        if (report.containsKey("hydrationCount"))
            series.getData().add(new XYChart.Data<>("Hydration", (Number) report.get("hydrationCount")));

        barChart.getData().add(series);

        VBox chartCard = new VBox(barChart);
        chartCard.getStyleClass().add("card");

        chartsArea.getChildren().add(chartCard);
    }

    private VBox statTile(String icon, String label, String value) {
        VBox card = new VBox(6);
        card.getStyleClass().add("stat-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(140);

        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size: 20px;");
        Label val = new Label(value);
        val.getStyleClass().add("stat-value");
        val.setStyle("-fx-font-size: 18px;");
        Label nm = new Label(label);
        nm.getStyleClass().add("stat-label");

        card.getChildren().addAll(ico, val, nm);
        return card;
    }
}
