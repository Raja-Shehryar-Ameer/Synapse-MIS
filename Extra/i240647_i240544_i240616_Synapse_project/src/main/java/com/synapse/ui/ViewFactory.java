package com.synapse.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates and caches UI views for the main content area.
 */
public class ViewFactory {

    private static ViewFactory instance;

    private final Map<String, Region> viewCache = new HashMap<>();

    public ViewFactory() {
        instance = this;
    }

    public static ViewFactory getInstance() {
        return instance;
    }

    public Region getView(String viewId) {
        return viewCache.computeIfAbsent(viewId, this::createView);
    }

    public void invalidate(String viewId) {
        viewCache.remove(viewId);
    }

    public void invalidateAll() {
        viewCache.clear();
    }

    private Region createView(String viewId) {
        return switch (viewId) {
            case "dashboard" -> new MainDashboardView();
            case "account" -> new AccountView();
            case "vitals" -> new VitalsView();
            case "symptoms" -> new SymptomsView();
            case "diet" -> new DietHydrationView();
            case "journal" -> new JournalView();
            case "medicine" -> new MedicineView();
            case "calendar" -> new CalendarView();
            case "hospitals" -> new HospitalsView();
            case "emergency" -> new EmergencyView();
            case "records" -> new RecordsView();
            case "analytics" -> new AnalyticsView();
            case "reports" -> new ReportView();
            case "backup" -> new BackupRestoreView();
            default -> createPlaceholder(viewId);
        };
    }

    private Region createPlaceholder(String viewId) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        box.getStyleClass().add("content-area");

        Label lbl = new Label("View not found: " + viewId);
        lbl.getStyleClass().add("page-title");
        box.getChildren().add(lbl);
        return box;
    }
}
