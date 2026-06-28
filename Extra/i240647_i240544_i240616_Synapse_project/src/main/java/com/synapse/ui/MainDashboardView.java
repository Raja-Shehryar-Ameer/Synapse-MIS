package com.synapse.ui;

import com.synapse.model.Patient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Main Dashboard — personalized greeting, live stats, feature grid.
 */
public class MainDashboardView extends VBox implements UpdatableView {

    private ScrollPane scroll;

    public MainDashboardView() {
        setFillWidth(true);
        getStyleClass().add("root-pane");

        scroll = new ScrollPane(buildContent());
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);
    }

    @Override
    public void updateView() {
        scroll.setContent(buildContent());
    }

    private VBox buildContent() {
        VBox content = new VBox(28);
        content.setPadding(new Insets(36));
        content.getStyleClass().add("root-pane");

        Patient patient = SessionManager.getCurrentPatient();
        String firstName = patient != null
                ? patient.getFullName().split(" ")[0]
                : "there";

        // ── Greeting header ────────────────────────────────────
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"));
        Label greeting = new Label(greetingText() + ", " + firstName + " 👋");
        greeting.getStyleClass().add("dash-greeting");
        Label dateLbl = new Label(date);
        dateLbl.getStyleClass().add("dash-greeting-sub");
        VBox greetingBox = new VBox(4, greeting, dateLbl);

        // ── Live stats bar ─────────────────────────────────────
        HBox statsBar = new HBox(14);
        statsBar.setAlignment(Pos.CENTER_LEFT);
        if (patient != null) {
            statsBar.getChildren().addAll(
                    liveStatTile("❤️", "Vitals",     count(patient.getVitalLogs()),   "#dcfce7", "#16a34a"),
                    liveStatTile("🔍", "Symptoms",   count(patient.getSymptomLogs()),  "#fef3c7", "#b45309"),
                    liveStatTile("🥗", "Meals",       count(patient.getDietLogs()),     "#dbeafe", "#1d4ed8"),
                    liveStatTile("💊", "Medicines",   count(patient.getMedicineInventory()), "#fce7f3", "#9d174d"),
                    liveStatTile("📓", "Journal",     count(patient.getJournalEntries()), "#f3f4f6", "#374151")
            );
        } else {
            statsBar.getChildren().addAll(
                    staticStatTile("❤️", "Vitals",    "Log Readings"),
                    staticStatTile("🔍", "Symptoms",  "Track Patterns"),
                    staticStatTile("🥗", "Diet",      "Cal & Water"),
                    staticStatTile("💊", "Medicine",  "Reminders"),
                    staticStatTile("📅", "Calendar",  "Events")
            );
        }

        // ── Latest Vitals quick card ───────────────────────────
        VBox quickSection = null;
        if (patient != null && !patient.getVitalLogs().isEmpty()) {
            var logs = patient.getVitalLogs();
            var latest = logs.get(logs.size() - 1);
            quickSection = buildLatestVitalsCard(latest);
        }

        // ── Feature grid ───────────────────────────────────────
        Label navTitle = sectionLabel("FEATURES");

        HBox row1 = featureRow(
                featureCard("❤️",  "Log Vitals",       "BP, heart rate, temperature",         "vitals"),
                featureCard("🔍",  "Symptoms",          "Track recurring symptoms",             "symptoms"),
                featureCard("🥗",  "Diet & Hydration",  "Meals, calories, water",              "diet"),
                featureCard("💊",  "Medicine",          "Inventory & schedules",               "medicine")
        );
        HBox row2 = featureRow(
                featureCard("📅",  "Calendar",          "Health events & appointments",        "calendar"),
                featureCard("📓",  "Journal",           "Mood & daily reflections",            "journal"),
                featureCard("🏥",  "Hospitals",         "Find nearby hospitals",               "hospitals"),
                featureCard("🚨",  "Emergency",         "Quick emergency info",                "emergency")
        );
        HBox row3 = featureRow(
                featureCard("📁",  "Medical Records",   "Upload & organise files",             "records"),
                featureCard("📊",  "Analytics",         "Charts & health trends",              "analytics"),
                featureCard("📋",  "Health Reports",    "Generate PDF reports",                "reports"),
                featureCard("☁️",  "Backup & Restore",  "Cloud sync your data",               "backup")
        );

        // ── Tip card ───────────────────────────────────────────
        HBox tip = buildTipCard();

        content.getChildren().addAll(greetingBox, statsBar);
        if (quickSection != null) content.getChildren().add(quickSection);
        content.getChildren().addAll(navTitle, row1, row2, row3, tip);
        return content;
    }

    // ── Live stat tile (with real numbers) ────────────────────

    private VBox liveStatTile(String icon, String label, String value, String bg, String textColor) {
        VBox card = new VBox(4);
        card.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: transparent;" +
            "-fx-padding: 16 20;" +
            "-fx-cursor: hand;"
        );
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(160);

        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size: 20px;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");
        Label nm  = new Label(label);
        nm.setStyle("-fx-font-size: 11px; -fx-text-fill: " + textColor + "; opacity: 0.7;");
        card.getChildren().addAll(ico, val, nm);
        return card;
    }

    private VBox staticStatTile(String icon, String name, String desc) {
        VBox card = new VBox(4);
        card.getStyleClass().add("stat-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(160);
        Label ico = new Label(icon); ico.setStyle("-fx-font-size: 20px;");
        Label nm  = new Label(name); nm.getStyleClass().add("stat-value"); nm.setStyle("-fx-font-size: 13px;");
        Label ds  = new Label(desc); ds.getStyleClass().add("stat-label");
        card.getChildren().addAll(ico, nm, ds);
        return card;
    }

    private String count(java.util.List<?> list) {
        return list == null ? "0" : String.valueOf(list.size());
    }

    // ── Latest Vitals inline card ─────────────────────────────

    private VBox buildLatestVitalsCard(com.synapse.model.VitalLog log) {
        VBox card = new VBox(10);
        card.setStyle(
            "-fx-background-color: #ffffff;" +
            "-fx-background-radius: 14;" +
            "-fx-border-color: #eaecef;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 14;" +
            "-fx-padding: 20 24;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 10, 0, 0, 2);"
        );

        Label header = new Label("Latest Vitals");
        header.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 1.5px;");

        HBox vitals = new HBox(36);
        vitals.setAlignment(Pos.CENTER_LEFT);
        vitals.getChildren().addAll(
                vMini("❤️",  "Blood Pressure",  nvl(log.getBloodPressure()), "#ef4444"),
                vMini("💓",  "Heart Rate",       nvl(log.getHeartRateBpm()) + " bpm", "#f59e0b"),
                vMini("🌡️", "Temperature",       nvl(log.getTemperatureF()) + "°F", "#3b82f6")
        );

        boolean abnormal = Boolean.TRUE.equals(log.getIsAbnormal());
        if (abnormal) {
            Label warn = new Label("⚠  Abnormal readings detected — consider consulting a doctor.");
            warn.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
            card.getChildren().addAll(header, vitals, warn);
        } else {
            card.getChildren().addAll(header, vitals);
        }
        return card;
    }

    private VBox vMini(String icon, String label, String value, String color) {
        VBox v = new VBox(3);
        Label ico = new Label(icon + "  " + label);
        ico.setStyle("-fx-font-size: 11px;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        v.getChildren().addAll(ico, val);
        return v;
    }

    // ── Feature grid ──────────────────────────────────────────

    private Label sectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("dash-section-title");
        lbl.setPadding(new Insets(4, 0, 0, 0));
        return lbl;
    }

    private HBox featureRow(VBox... cards) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        for (VBox c : cards) { HBox.setHgrow(c, Priority.ALWAYS); c.setMaxWidth(Double.MAX_VALUE); }
        row.getChildren().addAll(cards);
        return row;
    }

    private VBox featureCard(String icon, String name, String desc, String viewId) {
        VBox card = new VBox(10);
        card.getStyleClass().add("dash-feature-card");
        Label ico = new Label(icon); ico.getStyleClass().add("dash-feature-icon");
        Label nm  = new Label(name); nm.getStyleClass().add("dash-feature-name");
        Label ds  = new Label(desc); ds.getStyleClass().add("dash-feature-desc"); ds.setWrapText(true);
        card.getChildren().addAll(ico, nm, ds);
        card.setOnMouseClicked(e -> Navigation.navigateTo(viewId));
        return card;
    }

    private HBox buildTipCard() {
        HBox tip = new HBox(16);
        tip.setAlignment(Pos.CENTER_LEFT);
        tip.setStyle(
            "-fx-background-color: #f0fdf4; -fx-background-radius: 12;" +
            "-fx-border-color: #bbf7d0; -fx-border-width: 1; -fx-border-radius: 12; -fx-padding: 18 22;"
        );
        Label bulb = new Label("💡"); bulb.setStyle("-fx-font-size: 22px;");
        VBox text = new VBox(3);
        Label heading = new Label("Getting Started");
        heading.setStyle("-fx-text-fill: #166534; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label body = new Label(
            "Navigate using the sidebar. Start with Account ▸ Log Vitals ▸ Track diet & hydration daily. " +
            "Synapse learns your patterns over time and alerts you to anything unusual."
        );
        body.setStyle("-fx-text-fill: #166534; -fx-font-size: 12px;");
        body.setWrapText(true);
        text.getChildren().addAll(heading, body);
        HBox.setHgrow(text, Priority.ALWAYS);
        tip.getChildren().addAll(bulb, text);
        return tip;
    }

    private String greetingText() {
        int h = LocalTime.now().getHour();
        return h < 12 ? "Good morning" : h < 17 ? "Good afternoon" : "Good evening";
    }

    private String nvl(Object o) { return o != null ? o.toString() : "—"; }
}
