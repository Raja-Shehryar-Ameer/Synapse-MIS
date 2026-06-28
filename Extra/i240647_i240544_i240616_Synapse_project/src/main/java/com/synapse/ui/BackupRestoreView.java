package com.synapse.ui;

import com.synapse.controller.BackupRestoreController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.io.File;

/**
 * UC13: Backup and Restore — polished two-column layout.
 */
public class BackupRestoreView extends VBox {

    private final BackupRestoreController controller = new BackupRestoreController();

    public BackupRestoreView() {
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

        Label title = new Label("☁️ Backup & Restore");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Protect your health data — export backups and restore from file");
        sub.getStyleClass().add("page-subtitle");

        HBox columns = new HBox(20);
        columns.setAlignment(Pos.TOP_LEFT);
        VBox backupCard   = buildBackupCard();
        VBox restoreCard  = buildRestoreCard();
        HBox.setHgrow(backupCard,  Priority.ALWAYS);
        HBox.setHgrow(restoreCard, Priority.ALWAYS);
        columns.getChildren().addAll(backupCard, restoreCard);

        // Info card
        VBox infoCard = buildInfoCard();

        content.getChildren().addAll(title, sub, columns, infoCard);
        return content;
    }

    private VBox buildBackupCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        Label header = new Label("☁️ Create Backup");
        header.getStyleClass().add("card-header");

        // Icon zone
        VBox iconZone = new VBox(10);
        iconZone.setAlignment(Pos.CENTER);
        iconZone.setStyle("-fx-background-color: #f0fdf4; -fx-background-radius: 10; -fx-padding: 28 20;");
        Label icon = new Label("📦");
        icon.setStyle("-fx-font-size: 36px;");
        Label desc = new Label("Export all your health records,\nvitals, journal and medicines to a\nsingle compressed backup file.");
        desc.setStyle("-fx-text-fill: #166534; -fx-font-size: 12px; -fx-text-alignment: center;");
        desc.setWrapText(true);
        iconZone.getChildren().addAll(icon, desc);

        Label feedback = new Label();
        feedback.setWrapText(true);

        Button backupBtn = new Button("Create Backup Now");
        backupBtn.getStyleClass().add("button-primary");
        backupBtn.setMaxWidth(Double.MAX_VALUE);
        backupBtn.setOnAction(e -> {
            var patient = SessionManager.getCurrentPatient();
            if (patient == null) { setFeedback(feedback, "Please log in first.", "error"); return; }
            String path = controller.initiateBackup(patient);
            if (path != null) {
                setFeedback(feedback, "✓ Backup created: " + path, "success");
                try { java.awt.Desktop.getDesktop().open(new java.io.File(path).getParentFile()); } catch (Exception ex) { /* ignore */ }
            } else {
                setFeedback(feedback, "Backup failed — check permissions.", "error");
            }
        });

        card.getChildren().addAll(header, iconZone, feedback, backupBtn);
        return card;
    }

    private VBox buildRestoreCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        Label header = new Label("🔄 Restore Data");
        header.getStyleClass().add("card-header");

        VBox iconZone = new VBox(10);
        iconZone.setAlignment(Pos.CENTER);
        iconZone.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 10; -fx-padding: 28 20;");
        Label icon = new Label("🔁");
        icon.setStyle("-fx-font-size: 36px;");
        Label desc = new Label("Select a previously created Synapse\nbackup (.syn) file to restore your\nhealth data to this account.");
        desc.setStyle("-fx-font-size: 12px; -fx-text-alignment: center;");
        desc.setWrapText(true);
        iconZone.getChildren().addAll(icon, desc);

        Label warning = new Label("⚠  Restoring will overwrite existing data.");
        warning.setStyle("-fx-text-fill: #b45309; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label feedback = new Label();
        feedback.setWrapText(true);

        Button restoreBtn = new Button("Select File & Restore");
        restoreBtn.getStyleClass().add("button-secondary");
        restoreBtn.setMaxWidth(Double.MAX_VALUE);
        restoreBtn.setOnAction(e -> {
            var patient = SessionManager.getCurrentPatient();
            if (patient == null) { setFeedback(feedback, "Please log in first.", "error"); return; }
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Synapse Backup");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Synapse Backup", "*.syn"));
            File file = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (file != null) {
                boolean ok = controller.initiateRestore(patient, file);
                if (ok) {
                    setFeedback(feedback, "✓ Data restored successfully — all views refreshed.", "success");
                    ViewFactory factory = ViewFactory.getInstance();
                    if (factory != null) {
                        factory.invalidateAll();
                    }
                } else {
                    setFeedback(feedback, "Restore failed. Ensure the backup belongs to this account.", "error");
                }
            }
        });

        card.getChildren().addAll(header, iconZone, warning, feedback, restoreBtn);
        return card;
    }

    private VBox buildInfoCard() {
        VBox card = new VBox(12);
        card.setStyle(
            "-fx-background-color: #fffbeb; -fx-background-radius: 12;" +
            "-fx-border-color: #fde68a; -fx-border-width: 1; -fx-border-radius: 12; -fx-padding: 20 24;"
        );
        Label title = new Label("💡  Backup Best Practices");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #b45309;");
        String[] tips = {
            "• Create a backup before making major changes to your data",
            "• Store backups in a secure cloud location (OneDrive, Google Drive)",
            "• Backup files use .syn extension — keep them private",
            "• Regular monthly backups are recommended"
        };
        VBox tipList = new VBox(6);
        for (String t : tips) {
            Label l = new Label(t);
            l.setStyle("-fx-text-fill: #b45309; -fx-font-size: 12px;");
            tipList.getChildren().add(l);
        }
        card.getChildren().addAll(title, tipList);
        return card;
    }

    private void setFeedback(Label l, String msg, String type) {
        l.setText(msg);
        l.getStyleClass().removeAll("feedback-success","feedback-error","feedback-warn");
        l.getStyleClass().add("feedback-" + type);
    }
}
