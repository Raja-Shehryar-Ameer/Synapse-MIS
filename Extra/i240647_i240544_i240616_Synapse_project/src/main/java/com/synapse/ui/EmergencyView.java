package com.synapse.ui;

import com.synapse.controller.EmergencyController;
import com.synapse.model.EmergencyProfile;
import com.synapse.model.Medicine;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.List;

/**
 * UC11: Trigger Emergency Protocol — high-contrast, pulse-animated UI.
 */
public class EmergencyView extends VBox {

    private final EmergencyController controller = new EmergencyController();
    private final VBox infoBox = new VBox(12);

    public EmergencyView() {
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

        Label title = new Label("🚨 Emergency Protocol");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Instantly access your critical medical information");
        sub.getStyleClass().add("page-subtitle");

        // ── SOS hero card ──────────────────────────────────────
        VBox heroCard = new VBox(16);
        heroCard.setAlignment(Pos.CENTER);
        heroCard.setStyle(
            "-fx-background-color: #ffffff;" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: #fecaca;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 16;" +
            "-fx-padding: 36;" +
            "-fx-effect: dropshadow(gaussian, rgba(239,68,68,0.08), 16, 0, 0, 4);"
        );

        Label icon = new Label("🆘");
        icon.setStyle("-fx-font-size: 48px;");

        Label heroText = new Label("Activate Emergency Protocol");
        heroText.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; ");
        Label heroSub = new Label("This will display your blood type, allergies, medications\nand emergency contact for first responders.");
        heroSub.setStyle("-fx-font-size: 13px; -fx-text-alignment: center;");
        heroSub.setWrapText(true);

        Button activateBtn = new Button("  🚨  ACTIVATE SOS  ");
        activateBtn.getStyleClass().add("emergency-btn");
        activateBtn.setOnAction(e -> activateProtocol());

        heroCard.getChildren().addAll(icon, heroText, heroSub, activateBtn);

        // ── Info box (shown after activation) ─────────────────
        infoBox.setVisible(false);
        infoBox.setManaged(false);

        content.getChildren().addAll(title, sub, heroCard, infoBox);
        return content;
    }

    private void activateProtocol() {
        var patient = SessionManager.getCurrentPatient();
        if (patient == null) { showError("Please log in to use emergency features."); return; }

        try {
            boolean complete = controller.initiateEmergencyProtocol(patient);
            EmergencyProfile profile = controller.getCachedProfile();
            List<Medicine> meds = controller.getCachedMedicines();

            infoBox.getChildren().clear();
            infoBox.setVisible(true);
            infoBox.setManaged(true);

            // Rebuild infoBox styling
            infoBox.setStyle(
                "-fx-background-color: #ffffff;" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: #fecaca;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 14;" +
                "-fx-padding: 28;" +
                "-fx-effect: dropshadow(gaussian, rgba(239,68,68,0.07), 12, 0, 0, 3);"
            );

            if (!complete) {
                HBox warn = new HBox(10);
                warn.setStyle("-fx-background-color: #fffbeb; -fx-background-radius: 8; -fx-border-color: #fde68a; -fx-border-radius: 8; -fx-border-width: 1; -fx-padding: 12;");
                Label wl = new Label("⚠  Emergency profile is incomplete — please update your account details.");
                wl.setStyle("-fx-text-fill: #b45309; -fx-font-size: 12px;");
                wl.setWrapText(true);
                warn.getChildren().add(wl);
                infoBox.getChildren().add(warn);
            }

            Label header = new Label("🏥  Critical Medical Information");
            header.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");
            infoBox.getChildren().add(header);

            if (profile != null) {
                addInfoRow("Blood Type",          profile.getBloodType(),         true);
                addInfoRow("Allergies",            profile.getAllergies(),          false);
                addInfoRow("Chronic Conditions",   profile.getChronicConditions(), false);
                addInfoRow("Emergency Contact",
                        (profile.getEmergencyContactName() != null ? profile.getEmergencyContactName() : "—")
                        + "  📞  "
                        + (profile.getEmergencyContactPhone() != null ? profile.getEmergencyContactPhone() : "—"),
                        false);
            }

            if (meds != null && !meds.isEmpty()) {
                Separator sep = new Separator();
                Label medHeader = new Label("💊  Active Medications");
                medHeader.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 0 0 0;");
                infoBox.getChildren().addAll(sep, medHeader);
                for (Medicine m : meds) {
                    addInfoRow(m.getName(), "Qty: " + m.getInventoryQuantity(), false);
                }
            }

            Label feedback = new Label();
            Button exportBtn = new Button("📄  Export Emergency Document (PDF)");
            exportBtn.getStyleClass().add("button-primary");
            exportBtn.setOnAction(e -> {
                exportBtn.setText("⏳  Generating PDF...");
                exportBtn.setDisable(true);
                new Thread(() -> {
                    String path = controller.generateEmergencyExport();
                    javafx.application.Platform.runLater(() -> {
                        exportBtn.setText("📄  Export Emergency Document (PDF)");
                        exportBtn.setDisable(false);
                        if (path != null) {
                            setFeedback(feedback, "✓ Saved & opened: " + path, "success");
                            ToastNotification.show("success", "Emergency PDF Exported",
                                    "SOS document saved to: " + path);
                        } else {
                            setFeedback(feedback, "Export failed. Check console for details.", "error");
                            ToastNotification.show("error", "Export Failed",
                                    "Could not generate emergency PDF.");
                        }
                    });
                }, "pdf-export").start();
            });

            infoBox.getChildren().addAll(new Separator(), feedback, exportBtn);

        } catch (Exception ex) {
            showError("Error: " + ex.getMessage());
        }
    }

    private void addInfoRow(String label, String value, boolean highlight) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 8 0;");

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-min-width: 160;");
        Label val = new Label(value != null ? value : "—");
        val.setStyle("-fx-text-fill: " + (highlight ? "#dc2626" : "#111827") + "; -fx-font-size: " + (highlight ? "15" : "13") + "px; -fx-font-weight: " + (highlight ? "bold" : "normal") + ";");
        val.setWrapText(true);

        row.getChildren().addAll(lbl, val);
        infoBox.getChildren().add(row);
    }

    private void showError(String msg) {
        infoBox.getChildren().clear();
        infoBox.setVisible(true);
        infoBox.setManaged(true);
        infoBox.setStyle("-fx-background-color: #fef2f2; -fx-background-radius: 10; -fx-border-color: #fecaca; -fx-border-radius: 10; -fx-border-width: 1; -fx-padding: 20;");
        Label err = new Label("⚠  " + msg);
        err.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 13px;");
        err.setWrapText(true);
        infoBox.getChildren().add(err);
    }

    private void setFeedback(Label l, String msg, String type) {
        l.setText(msg);
        l.getStyleClass().removeAll("feedback-success", "feedback-error", "feedback-warn");
        l.getStyleClass().add("feedback-" + type);
    }
}
