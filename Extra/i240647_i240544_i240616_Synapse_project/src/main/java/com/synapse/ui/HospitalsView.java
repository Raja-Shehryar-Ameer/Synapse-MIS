package com.synapse.ui;

import com.synapse.controller.HospitalsController;
import com.synapse.model.HealthcareFacility;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;

/**
 * UC10: Browse Hospitals view.
 */
public class HospitalsView extends VBox {

    private final HospitalsController controller = new HospitalsController();
    private final VBox resultsBox = new VBox(10);

    public HospitalsView() {
        getStyleClass().add("content-area");
        setSpacing(20);
        setPadding(new Insets(30));

        Label title = new Label("Hospitals & Pharmacies");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Browse healthcare facilities near you");
        subtitle.getStyleClass().add("page-subtitle");

        // Search bar
        HBox searchBar = new HBox(10);
        searchBar.getStyleClass().add("card");
        TextField location = new TextField(); location.setPromptText("Location / City");
        TextField specialty = new TextField(); specialty.setPromptText("Specialty");
        TextField services = new TextField(); services.setPromptText("Services");
        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("button-primary");
        searchBtn.setOnAction(e -> {
            List<HealthcareFacility> results = controller.filterFacilities(
                    location.getText(), specialty.getText(), services.getText());
            displayResults(results);
        });
        Button showAll = new Button("Show All");
        showAll.getStyleClass().add("button-secondary");
        showAll.setOnAction(e -> displayResults(controller.initialize()));
        searchBar.getChildren().addAll(location, specialty, services, searchBtn, showAll);

        // Results
        ScrollPane scroll = new ScrollPane(resultsBox);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("sidebar-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Load all on init
        displayResults(controller.initialize());

        getChildren().addAll(title, subtitle, searchBar, scroll);
    }

    private void displayResults(List<HealthcareFacility> facilities) {
        resultsBox.getChildren().clear();
        if (facilities == null || facilities.isEmpty()) {
            Label empty = new Label("No facilities found.");
            empty.getStyleClass().add("text-muted");
            resultsBox.getChildren().add(empty);
            return;
        }
        for (HealthcareFacility f : facilities) {
            VBox card = new VBox(6);
            card.getStyleClass().add("card");

            HBox header = new HBox(10);
            String icon = switch (f.getFacilityType() != null ? f.getFacilityType() : "") {
                case "Hospital" -> "🏥";
                case "Pharmacy" -> "💊";
                case "Diagnostic Lab" -> "🔬";
                default -> "🏢";
            };
            Label nameLabel = new Label(icon + "  " + f.getName());
            nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
            Label typeLabel = new Label(f.getFacilityType());
            typeLabel.setStyle("-fx-text-fill: #0ea5e9; -fx-font-size: 11px; -fx-padding: 2 8; " +
                    "-fx-background-color: rgba(0,210,255,0.1); -fx-background-radius: 4;");
            header.getChildren().addAll(nameLabel, typeLabel);

            Label addr = new Label("📍 " + f.getAddress());
            addr.setStyle("-fx-font-size: 12px;");
            Label phone = new Label("📞 " + f.getContactNumber());
            phone.setStyle("-fx-font-size: 12px;");
            Label spec = new Label("Specialty: " + f.getSpecialty());
            spec.setStyle("-fx-font-size: 12px;");
            Label svc = new Label("Services: " + f.getServices());
            svc.setStyle("-fx-font-size: 12px;");
            svc.setWrapText(true);

            card.getChildren().addAll(header, addr, phone, spec, svc);
            resultsBox.getChildren().add(card);
        }
    }
}
