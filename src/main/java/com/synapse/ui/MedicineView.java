package com.synapse.ui;

import com.synapse.controller.MedicineController;
import com.synapse.model.DrugInteraction;
import com.synapse.model.Medicine;
import com.synapse.model.Patient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Medicine Inventory and Prescription view.
 */
public class MedicineView extends VBox {

    private final MedicineController controller = new MedicineController();
    private final VBox inventoryList = new VBox(8);
    private final VBox prescriptionList = new VBox(7);
    private final ComboBox<Medicine> medBox = new ComboBox<>();

    public MedicineView() {
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
        content.getStyleClass().add("content-area");

        Label title = new Label("Medicine Inventory");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Manage medications, dosage schedules, and prescription visibility.");
        sub.getStyleClass().add("page-subtitle");

        HBox topRow = new HBox(18);
        topRow.setAlignment(Pos.TOP_LEFT);
        VBox addCard = buildAddCard();
        VBox scheduleCard = buildScheduleCard();
        addCard.setPrefWidth(420);
        HBox.setHgrow(scheduleCard, Priority.ALWAYS);
        topRow.getChildren().addAll(addCard, scheduleCard);

        VBox inventoryCard = new VBox(14);
        inventoryCard.getStyleClass().add("card");
        Label inventoryTitle = new Label("Your Medicine Cabinet");
        inventoryTitle.getStyleClass().add("card-header");

        ScrollPane inventoryScroll = new ScrollPane(inventoryList);
        inventoryScroll.setFitToWidth(true);
        inventoryScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        inventoryScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        inventoryScroll.setPrefHeight(220);
        inventoryCard.getChildren().addAll(inventoryTitle, inventoryScroll);

        refreshMedicineViews();
        content.getChildren().addAll(title, sub, topRow, inventoryCard);
        return content;
    }

    private VBox buildAddCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        Label header = new Label("Add Medicine");
        header.getStyleClass().add("card-header");

        TextField name = field("Medicine name (e.g. Paracetamol)");
        TextField qty = field("Quantity (e.g. 30)");
        DatePicker expiry = new DatePicker();
        expiry.setPromptText("Expiry date");

        VBox warningsBox = new VBox(6);
        Label feedback = new Label();
        feedback.setWrapText(true);

        Button addBtn = new Button("Check Interactions & Add");
        addBtn.getStyleClass().add("button-primary");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setOnAction(e -> {
            Patient patient = SessionManager.getCurrentPatient();
            if (patient == null) {
                setFeedback(feedback, "Please log in first.", "error");
                return;
            }
            if (name.getText().isBlank()) {
                setFeedback(feedback, "Enter a medicine name.", "error");
                return;
            }

            try {
                int quantity = Integer.parseInt(qty.getText().trim());
                if (quantity <= 0) {
                    setFeedback(feedback, "Quantity must be positive.", "error");
                    return;
                }
                if (quantity == 1) {
                    showAlert(Alert.AlertType.WARNING, "Low Dosage Unit", "Cannot add just 1 unit. Please add a standard pack size.");
                    return;
                }

                LocalDate expiryDate = expiry.getValue();
                if (expiryDate != null && !expiryDate.isAfter(LocalDate.now())) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Expiry", "The expiry date cannot be today or in the past.");
                    return;
                }
                
                List<DrugInteraction> interactions = controller.initiateMedicineAddition(
                        patient, name.getText().trim(), quantity, expiryDate
                );

                warningsBox.getChildren().clear();
                for (DrugInteraction interaction : interactions) {
                    HBox warn = new HBox(8);
                    warn.setStyle("-fx-background-color: #fffbeb; -fx-background-radius: 7; -fx-border-color: #fde68a; -fx-border-width: 1; -fx-border-radius: 7; -fx-padding: 8 12;");
                    Label warningLabel = new Label(
                            interaction.getSeverityLevel() + " - " + interaction.getWarningMessage()
                    );
                    warningLabel.setStyle("-fx-text-fill: #b45309; -fx-font-size: 12px;");
                    warningLabel.setWrapText(true);
                    warn.getChildren().add(warningLabel);
                    warningsBox.getChildren().add(warn);
                }

                Map<String, Boolean> flags = controller.saveMedicine(patient, name.getText().trim(), quantity, expiryDate);
                StringBuilder message = new StringBuilder("✓ Medicine added to inventory.");
                if (Boolean.TRUE.equals(flags.get("isLowStock"))) {
                    message.append(" ⚠ Low stock.");
                    showAlert(Alert.AlertType.WARNING, "Low Stock Alert", "The medicine you just added has a low stock level (5 units or less).");
                }
                if (Boolean.TRUE.equals(flags.get("isNearExpiry"))) {
                    message.append(" ⚠ Near expiry.");
                    showAlert(Alert.AlertType.WARNING, "Near Expiry Alert", "The medicine you just added is nearing its expiry date (within 30 days).");
                }
                setFeedback(feedback, message.toString(), "success");

                name.clear();
                qty.clear();
                expiry.setValue(null);
                refreshMedicineViews();
            } catch (NumberFormatException ex) {
                setFeedback(feedback, "Quantity must be a valid number.", "error");
            } catch (Exception ex) {
                setFeedback(feedback, "Error: " + ex.getMessage(), "error");
            }
        });

        card.getChildren().addAll(
                header,
                lf("Medicine Name", name),
                lf("Quantity (units)", qty),
                lf("Expiry Date", expiry),
                warningsBox,
                feedback,
                addBtn
        );
        return card;
    }

    private VBox buildScheduleCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        Label header = new Label("Prescription Schedule");
        header.getStyleClass().add("card-header");

        medBox.setPromptText("Select medicine");
        medBox.setMaxWidth(Double.MAX_VALUE);
        medBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Medicine medicine, boolean empty) {
                super.updateItem(medicine, empty);
                setText(empty || medicine == null ? "" : formatMedicine(medicine));
            }
        });
        medBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Medicine medicine, boolean empty) {
                super.updateItem(medicine, empty);
                setText(empty || medicine == null ? "" : formatMedicine(medicine));
            }
        });

        TextField dosage = field("Dosage (e.g. 500mg twice)");
        ComboBox<String> freq = new ComboBox<>();
        freq.getItems().addAll("Once Daily", "Twice Daily", "Three Times", "Weekly");
        freq.setPromptText("Frequency");
        freq.setMaxWidth(Double.MAX_VALUE);

        VBox timeBoxes = new VBox(6);
        List<String> timeOptions = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            timeOptions.add(String.format("%02d:00", hour));
            timeOptions.add(String.format("%02d:30", hour));
        }

        freq.valueProperty().addListener((obs, oldValue, newValue) -> {
            timeBoxes.getChildren().clear();
            if (newValue == null) {
                return;
            }
            int count = switch (newValue) {
                case "Twice Daily" -> 2;
                case "Three Times" -> 3;
                default -> 1;
            };
            for (int i = 0; i < count; i++) {
                ComboBox<String> timeBox = new ComboBox<>();
                timeBox.getItems().addAll(timeOptions);
                timeBox.setPromptText("Dose " + (i + 1) + " time");
                timeBox.setMaxWidth(Double.MAX_VALUE);
                timeBoxes.getChildren().add(timeBox);
            }
        });

        Label feedback = new Label();

        Button saveBtn = new Button("Set Schedule");
        saveBtn.getStyleClass().add("button-secondary");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> {
            Patient patient = SessionManager.getCurrentPatient();
            if (patient == null) {
                setFeedback(feedback, "Please log in first.", "error");
                return;
            }
            if (medBox.getValue() == null) {
                setFeedback(feedback, "Select a medicine.", "error");
                return;
            }
            if (timeBoxes.getChildren().isEmpty()) {
                setFeedback(feedback, "Select a frequency first.", "error");
                return;
            }

            List<LocalTime> times = new ArrayList<>();
            for (javafx.scene.Node node : timeBoxes.getChildren()) {
                ComboBox<?> box = (ComboBox<?>) node;
                if (box.getValue() == null) {
                    setFeedback(feedback, "Select all dose times.", "error");
                    return;
                }
                times.add(LocalTime.parse(box.getValue().toString()));
            }

            try {
                controller.setPrescriptionSchedule(patient, medBox.getValue(), dosage.getText().trim(), times, freq.getValue());
                setFeedback(feedback, "✓ Schedule saved successfully.", "success");
                medBox.setValue(null);
                dosage.clear();
                freq.setValue(null);
                timeBoxes.getChildren().clear();
                refreshMedicineViews();
            } catch (Exception ex) {
                setFeedback(feedback, "Error saving schedule.", "error");
            }
        });

        Label prescTitle = new Label("Active Prescriptions");
        prescTitle.getStyleClass().add("form-label");

        card.getChildren().addAll(
                header,
                lf("Medicine", medBox),
                lf("Dosage Instructions", dosage),
                lf("Frequency", freq),
                timeBoxes,
                feedback,
                saveBtn,
                prescTitle,
                prescriptionList
        );
        return card;
    }

    private void refreshMedicineViews() {
        inventoryList.getChildren().clear();
        prescriptionList.getChildren().clear();
        medBox.getItems().clear();

        Patient patient = SessionManager.getCurrentPatient();
        if (patient == null) {
            inventoryList.getChildren().add(placeholder("Log in to manage medicines."));
            prescriptionList.getChildren().add(placeholder("No active prescriptions."));
            return;
        }

        List<Medicine> medicines = patient.getMedicineInventory();
        if (medicines.isEmpty()) {
            inventoryList.getChildren().add(placeholder("No medicines added yet."));
        } else {
            for (int i = medicines.size() - 1; i >= 0; i--) {
                inventoryList.getChildren().add(buildInventoryRow(medicines.get(i)));
            }
            medBox.getItems().addAll(medicines);
        }

        List<Medicine> activeMedicines = patient.getActiveMedicines();
        if (activeMedicines.isEmpty()) {
            prescriptionList.getChildren().add(placeholder("No active prescriptions."));
            return;
        }

        for (Medicine medicine : activeMedicines) {
            prescriptionList.getChildren().add(buildPrescriptionRow(medicine));
        }
    }

    private HBox buildInventoryRow(Medicine medicine) {
        boolean lowStock = medicine.getInventoryQuantity() != null && medicine.getInventoryQuantity() <= 5;
        boolean nearExpiry = medicine.getExpiryDate() != null && medicine.getExpiryDate().isBefore(LocalDate.now().plusDays(30));

        HBox row = new HBox(10);
        row.getStyleClass().add("log-entry");
        row.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(2);
        Label name = new Label(medicine.getName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Label detail = new Label("Qty: " + medicine.getInventoryQuantity()
                + "   Expires: " + (medicine.getExpiryDate() != null ? medicine.getExpiryDate() : "N/A"));
        detail.setStyle("-fx-font-size: 11px;");
        info.getChildren().addAll(name, detail);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox badges = new HBox(6);
        if (lowStock) {
            Label low = new Label("Low Stock");
            low.getStyleClass().add("badge-amber");
            badges.getChildren().add(low);
        }
        if (nearExpiry) {
            Label expiry = new Label("Near Expiry");
            expiry.getStyleClass().add("log-entry-badge-warn");
            badges.getChildren().add(expiry);
        }

        boolean hasSchedule = medicine.getSchedule() != null;
        Button delete = new Button("Delete");
        delete.getStyleClass().add("log-delete-btn");
        delete.setOnAction(e -> {
            String msg = hasSchedule
                    ? "Delete \"" + medicine.getName() + "\" and its linked prescription schedule?\n\nThis action cannot be undone."
                    : "Delete \"" + medicine.getName() + "\" from your inventory?\n\nThis action cannot be undone.";
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText("Delete Medicine");
            styleAlert(confirm);
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                Patient patient = SessionManager.getCurrentPatient();
                if (patient != null) {
                    controller.deleteMedicine(patient, medicine.getMedicineId());
                    refreshMedicineViews();
                }
            }
        });

        row.getChildren().addAll(info, badges, delete);
        return row;
    }

    private HBox buildPrescriptionRow(Medicine medicine) {
        HBox row = new HBox(10);
        row.getStyleClass().add("log-entry");
        row.setAlignment(Pos.CENTER_LEFT);

        String times = medicine.getSchedule().getScheduledTimes().stream()
                .map(LocalTime::toString)
                .collect(Collectors.joining(", "));

        Label info = new Label(medicine.getName() + "  ·  " + medicine.getSchedule().getFrequency() + "  ·  " + times);
        info.getStyleClass().add("log-entry-text");
        HBox.setHgrow(info, Priority.ALWAYS);

        Button delete = new Button("Remove");
        delete.getStyleClass().add("log-delete-btn");
        delete.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Remove the prescription schedule for \"" + medicine.getName() + "\"?\n\nThe medicine will remain in your inventory.",
                    ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText("Remove Prescription");
            styleAlert(confirm);
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                Patient patient = SessionManager.getCurrentPatient();
                if (patient != null) {
                    controller.removePrescriptionSchedule(patient, medicine);
                    refreshMedicineViews();
                }
            }
        });

        row.getChildren().addAll(info, delete);
        return row;
    }

    private Label placeholder(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 12px;");
        return label;
    }

    private String formatMedicine(Medicine medicine) {
        return medicine.getName() + " (Exp: " + (medicine.getExpiryDate() != null ? medicine.getExpiryDate() : "N/A") + ")";
    }

    private VBox lf(String label, javafx.scene.Node node) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("form-label");
        return new VBox(5, lbl, node);
    }

    private TextField field(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        return tf;
    }

    private void setFeedback(Label label, String message, String type) {
        label.setText(message);
        label.getStyleClass().removeAll("feedback-success", "feedback-error", "feedback-warn");
        label.getStyleClass().add("feedback-" + type);
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

    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setHeaderText(header);
        alert.setContentText(content);
        styleAlert(alert);
        alert.showAndWait();
    }
}
