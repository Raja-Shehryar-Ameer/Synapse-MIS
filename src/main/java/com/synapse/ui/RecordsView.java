package com.synapse.ui;

import com.synapse.controller.RecordsController;
import com.synapse.model.MedicalRecord;
import com.synapse.model.Patient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Organize Medical Records - upload, view, open, and delete records.
 */
public class RecordsView extends VBox {

    private final RecordsController controller = new RecordsController();
    private final VBox recordsList = new VBox(8);

    private File selectedFile;
    private Label fileLabel;

    public RecordsView() {
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

        Label title = new Label("Medical Records");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Upload documents, open saved files, and remove outdated records.");
        sub.getStyleClass().add("page-subtitle");

        HBox columns = new HBox(20);
        columns.setAlignment(Pos.TOP_LEFT);

        VBox uploadCard = buildUploadCard();
        uploadCard.setPrefWidth(400);
        uploadCard.setMaxWidth(400);

        VBox listCard = buildListCard();
        HBox.setHgrow(listCard, Priority.ALWAYS);
        columns.getChildren().addAll(uploadCard, listCard);

        content.getChildren().addAll(title, sub, columns);
        return content;
    }

    private VBox buildUploadCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        Label header = new Label("Upload Record");
        header.getStyleClass().add("card-header");

        TextField docTitle = new TextField();
        docTitle.setPromptText("Document title (e.g. Blood Test 2024)");

        ComboBox<String> category = new ComboBox<>();
        category.getItems().addAll(RecordsController.CATEGORIES);
        category.setPromptText("Select category");
        category.setMaxWidth(Double.MAX_VALUE);

        VBox dropZone = new VBox(10);
        dropZone.setAlignment(Pos.CENTER);
        dropZone.setStyle(
                "-fx-background-color: #f9fafb;" +
                "-fx-border-color: #e5e7eb;" +
                "-fx-border-style: dashed;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-border-width: 2;" +
                "-fx-padding: 28 20;" +
                "-fx-cursor: hand;"
        );

        Label dropIcon = new Label("\uD83D\uDCC4");
        dropIcon.setStyle("-fx-font-size: 28px;");
        fileLabel = new Label("Click to browse a file");
        fileLabel.setStyle("-fx-font-size: 12px;");
        dropZone.getChildren().addAll(dropIcon, fileLabel);
        dropZone.setOnMouseClicked(e -> browseFile());

        Label feedback = new Label();
        feedback.setWrapText(true);

        Button uploadBtn = new Button("Upload Record");
        uploadBtn.getStyleClass().add("button-primary");
        uploadBtn.setMaxWidth(Double.MAX_VALUE);
        uploadBtn.setOnAction(e -> {
            Patient patient = SessionManager.getCurrentPatient();
            if (patient == null) {
                setFeedback(feedback, "Please log in first.", "error");
                return;
            }
            if (docTitle.getText().isBlank() || category.getValue() == null) {
                setFeedback(feedback, "Fill in title and select a category.", "error");
                return;
            }
            if (selectedFile == null) {
                setFeedback(feedback, "Select a file before uploading.", "error");
                return;
            }

            controller.saveMedicalRecord(patient, docTitle.getText().trim(), category.getValue(), selectedFile);
            setFeedback(feedback, "Record saved successfully.", "success");
            reloadRecords();

            docTitle.clear();
            category.setValue(null);
            selectedFile = null;
            fileLabel.setText("Click to browse a file");
            fileLabel.setStyle("-fx-font-size: 12px;");
        });

        card.getChildren().addAll(
                header,
                lf("Document Title", docTitle),
                lf("Category", category),
                dropZone,
                feedback,
                uploadBtn
        );
        return card;
    }

    private VBox buildListCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);

        Label header = new Label("Saved Records");
        header.getStyleClass().add("card-header");

        ScrollPane scroll = new ScrollPane(recordsList);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        scroll.setPrefHeight(320);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        card.getChildren().addAll(header, scroll);
        reloadRecords();
        return card;
    }

    private void browseFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Medical Document");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.jpg", "*.png", "*.doc", "*.docx"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            selectedFile = file;
            fileLabel.setText("Selected: " + file.getName());
            fileLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 12px; -fx-font-weight: bold;");
        }
    }

    private void reloadRecords() {
        recordsList.getChildren().clear();

        Patient patient = SessionManager.getCurrentPatient();
        if (patient == null) {
            addPlaceholder("Log in to view your records.");
            return;
        }

        List<MedicalRecord> records = new ArrayList<>(controller.getMedicalRecords(patient));
        if (records.isEmpty()) {
            addPlaceholder("No records uploaded yet.");
            return;
        }

        for (int i = records.size() - 1; i >= 0; i--) {
            recordsList.getChildren().add(buildRecordRow(records.get(i)));
        }
    }

    private HBox buildRecordRow(MedicalRecord record) {
        HBox row = new HBox(12);
        row.getStyleClass().add("log-entry");
        row.setAlignment(Pos.CENTER_LEFT);

        String fileName = extractFileName(record.getFilePath());
        boolean hasFile = record.getFilePath() != null && !record.getFilePath().isBlank();

        Label typeIcon = new Label(iconFor(fileName));
        typeIcon.setStyle("-fx-font-size: 20px;");

        VBox info = new VBox(2);
        Label title = new Label(record.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Label detail = new Label(record.getCategory() + (hasFile ? "  ·  " + fileName : "  ·  No file path"));
        detail.setStyle("-fx-font-size: 11px;");
        info.getChildren().addAll(title, detail);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label category = new Label(record.getCategory());
        category.getStyleClass().add("badge-blue");

        HBox actions = new HBox(6);

        Button openBtn = new Button("Open");
        openBtn.setDisable(!hasFile);
        openBtn.setOnAction(e -> openRecord(record));

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("button-danger");
        deleteBtn.setOnAction(e -> deleteRecord(record));

        actions.getChildren().addAll(openBtn, deleteBtn);
        row.getChildren().addAll(typeIcon, info, category, actions);
        return row;
    }

    private void openRecord(MedicalRecord record) {
        String path = record.getFilePath();
        if (path == null || path.isBlank()) {
            showMessage(Alert.AlertType.ERROR, "Open Failed", "This record does not have a saved file path.");
            return;
        }

        File file = Path.of(path).toFile();
        if (!file.exists()) {
            showMessage(Alert.AlertType.ERROR, "Open Failed", "The saved file could not be found on disk.");
            return;
        }

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                showMessage(Alert.AlertType.ERROR, "Open Failed", "Desktop file opening is not supported on this system.");
            }
        } catch (Exception ex) {
            showMessage(Alert.AlertType.ERROR, "Open Failed", "Unable to open the selected record.");
        }
    }

    private void deleteRecord(MedicalRecord record) {
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete this record and remove its stored file from disk?",
                ButtonType.YES,
                ButtonType.NO
        );
        confirm.setHeaderText("Delete Medical Record");
        styleAlert(confirm);

        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        Patient patient = SessionManager.getCurrentPatient();
        controller.deleteMedicalRecord(patient, record, true);
        reloadRecords();
    }

    private void addPlaceholder(String text) {
        Label empty = new Label(text);
        empty.setStyle("-fx-text-fill: #d1d5db; -fx-font-size: 13px;");
        recordsList.getChildren().add(empty);
    }

    private String extractFileName(String path) {
        if (path == null || path.isBlank()) {
            return "No file";
        }
        return Path.of(path).getFileName().toString();
    }

    private String iconFor(String fileName) {
        String upper = fileName.toUpperCase();
        if (upper.endsWith(".PDF")) {
            return "\uD83D\uDCC4";
        }
        if (upper.endsWith(".PNG") || upper.endsWith(".JPG") || upper.endsWith(".JPEG")) {
            return "\uD83D\uDDBC";
        }
        return "\uD83D\uDCC1";
    }

    private VBox lf(String label, javafx.scene.Node node) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("form-label");
        return new VBox(5, lbl, node);
    }

    private void setFeedback(Label label, String message, String type) {
        label.setText(message);
        label.getStyleClass().removeAll("feedback-success", "feedback-error", "feedback-warn");
        label.getStyleClass().add("feedback-" + type);
    }

    private void showMessage(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setHeaderText(header);
        styleAlert(alert);
        alert.showAndWait();
    }

    private void styleAlert(Alert alert) {
        try {
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/synapse.css").toExternalForm());
            String addCss = com.synapse.ui.theme.ThemeManager.getInstance().getAdditionalStylesheet();
            if (addCss != null) {
                alert.getDialogPane().getStylesheets().add(getClass().getResource(addCss).toExternalForm());
            }
        } catch (Exception ignored) {}
    }
}
