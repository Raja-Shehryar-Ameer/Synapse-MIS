package com.synapse.ui;

import com.synapse.controller.AccountController;
import com.synapse.model.Patient;
import com.synapse.service.NotificationScheduler;
import javafx.animation.FadeTransition;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import netscape.javascript.JSObject;

import java.net.URL;
import java.util.Optional;

/**
 * Account / Auth view.
 */
public class AccountView extends VBox {

    private final AccountController controller = new AccountController();

    private WebView webView;
    private WebEngine engine;
    private AuthBridge bridge;
    private VBox profilePane;
    private StackPane rootStack;

    public AccountView() {
        setFillWidth(true);
        setSpacing(0);
        setPadding(Insets.EMPTY);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        rootStack = new StackPane();
        rootStack.setAlignment(Pos.CENTER);
        VBox.setVgrow(rootStack, Priority.ALWAYS);

        buildProfilePane();

        if (SessionManager.isLoggedIn()) {
            controller.setCurrentPatient(SessionManager.getCurrentPatient());
            refreshProfile();
            rootStack.getChildren().add(profilePane);
        } else {
            buildWebView();
            profilePane.setVisible(false);
            rootStack.getChildren().addAll(webView, profilePane);
        }

        getChildren().add(rootStack);
    }

    private void buildWebView() {
        webView = new WebView();
        webView.setContextMenuEnabled(false);
        webView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                bridge = new AuthBridge(controller, engine, this::onLoginSuccess);
                window.setMember("bridge", bridge);
            }
        });

        URL url = getClass().getResource("/auth.html");
        if (url != null) {
            engine.load(url.toExternalForm());
        }
    }

    private void onLoginSuccess() {
        if (webView != null) {
            webView.setVisible(false);
        }
        refreshProfile();
        profilePane.setVisible(true);

        FadeTransition ft = new FadeTransition(Duration.millis(250), profilePane);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void buildProfilePane() {
        profilePane = new VBox(0);
        profilePane.setFillWidth(true);
        profilePane.getStyleClass().add("root-pane");
    }

    private void refreshProfile() {
        profilePane.getChildren().clear();
        Patient patient = getActivePatient();
        if (patient == null) {
            return;
        }

        ScrollPane scroll = new ScrollPane(buildProfileContent(patient));
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        profilePane.getChildren().add(scroll);

        NotificationScheduler.startBackgroundTasks(patient);
    }

    private Patient getActivePatient() {
        Patient patient = SessionManager.getCurrentPatient();
        if (patient != null) {
            controller.setCurrentPatient(patient);
            return patient;
        }
        return controller.getCurrentPatient();
    }

    private VBox buildProfileContent(Patient patient) {
        VBox content = new VBox(24);
        content.setPadding(new Insets(36));
        content.getStyleClass().add("root-pane");

        Label title = new Label("My Account");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Manage your personal details and emergency profile");
        sub.getStyleClass().add("page-subtitle");

        HBox avatarCard = new HBox(20);
        avatarCard.getStyleClass().add("card");
        avatarCard.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label(getInitials(patient.getFullName()));
        avatar.getStyleClass().add("profile-avatar");

        VBox identity = new VBox(4);
        Label name = new Label(patient.getFullName());
        name.getStyleClass().add("profile-name");
        Label email = new Label(patient.getEmail());
        email.getStyleClass().add("profile-email");

        Label status = new Label("Active Account");
        status.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 10; -fx-background-radius: 12;");
        identity.getChildren().addAll(name, email, status);
        avatarCard.getChildren().addAll(avatar, identity);

        VBox infoCard = new VBox(16);
        infoCard.getStyleClass().add("card");
        Label infoHeader = new Label("Personal Details");
        infoHeader.getStyleClass().add("card-header");

        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(14);
        addGridRow(grid, 0, "Gender", nvl(patient.getGender()));
        addGridRow(grid, 1, "Date of Birth", nvl(patient.getDateOfBirth()));
        addGridRow(grid, 2, "Height", patient.getHeightCm() != null ? patient.getHeightCm() + " cm" : "-");
        addGridRow(grid, 3, "BMI", patient.getCurrentBMI() != null ? String.format("%.1f", patient.getCurrentBMI()) : "-");
        infoCard.getChildren().addAll(infoHeader, grid);

        VBox emergencyCard = new VBox(16);
        emergencyCard.getStyleClass().add("card");
        Label emergencyHeader = new Label("Emergency Profile");
        emergencyHeader.getStyleClass().add("card-header");

        GridPane emergencyGrid = new GridPane();
        emergencyGrid.setHgap(24);
        emergencyGrid.setVgap(14);

        var profile = patient.getEmergencyProfile();
        addGridRow(emergencyGrid, 0, "Blood Type", profile != null ? nvl(profile.getBloodType()) : "-");
        addGridRow(emergencyGrid, 1, "Allergies", profile != null ? nvl(profile.getAllergies()) : "-");
        addGridRow(emergencyGrid, 2, "Chronic Conditions", profile != null ? nvl(profile.getChronicConditions()) : "-");
        addGridRow(emergencyGrid, 3, "Emergency Contact", profile != null ? nvl(profile.getEmergencyContactName()) : "-");
        addGridRow(emergencyGrid, 4, "Contact Phone", profile != null ? nvl(profile.getEmergencyContactPhone()) : "-");
        emergencyCard.getChildren().addAll(emergencyHeader, emergencyGrid);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button signOut = new Button("Sign Out");
        signOut.getStyleClass().add("button-secondary");
        signOut.setOnAction(e -> logoutToLogin());

        Button delete = new Button("Delete Account");
        delete.getStyleClass().add("button-danger");
        delete.setOnAction(e -> confirmAndDeleteAccount());

        Button editProfile = new Button("Edit Profile");
        editProfile.getStyleClass().add("button-primary");
        editProfile.setOnAction(e -> editProfileDialog());

        actions.getChildren().addAll(editProfile, signOut, delete);
        content.getChildren().addAll(title, sub, avatarCard, infoCard, emergencyCard, actions);
        return content;
    }

    private void logoutToLogin() {
        NotificationScheduler.stopBackgroundTasks();
        ViewFactory factory = ViewFactory.getInstance();
        if (factory != null) {
            factory.invalidateAll();
        }
        controller.logout();
        SessionManager.logout();
    }

    private void editProfileDialog() {
        Patient patient = getActivePatient();
        if (patient == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Profile");
        dialog.setHeaderText("Update your personal and emergency details");
        styleDialog(dialog);

        ButtonType saveType = new ButtonType("Save Changes", ButtonType.OK.getButtonData());
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(520);

        // ── Personal Info Section ──
        Label personalTitle = new Label("PERSONAL INFORMATION");
        personalTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1.5;");

        javafx.scene.control.TextField heightField = new javafx.scene.control.TextField(patient.getHeightCm() != null ? String.valueOf(patient.getHeightCm()) : "");
        heightField.setPromptText("e.g. 175");
        javafx.scene.control.TextField bmiField = new javafx.scene.control.TextField(patient.getCurrentBMI() != null ? String.valueOf(patient.getCurrentBMI()) : "");
        bmiField.setPromptText("e.g. 22.5");

        ComboBox<String> genderBox = new ComboBox<>();
        genderBox.getItems().addAll("Male", "Female", "Other");
        String currentGender = nvl(patient.getGender());
        if (!currentGender.equals("-")) genderBox.setValue(currentGender);
        genderBox.setPromptText("Select gender");
        genderBox.setMaxWidth(Double.MAX_VALUE);

        javafx.scene.control.DatePicker dobPicker = new javafx.scene.control.DatePicker(patient.getDateOfBirth());
        dobPicker.setPromptText("Select date");

        GridPane personalGrid = new GridPane();
        personalGrid.setHgap(14);
        personalGrid.setVgap(12);
        addEditRow(personalGrid, 0, "Height (cm)", heightField);
        addEditRow(personalGrid, 1, "BMI", bmiField);
        addEditRow(personalGrid, 2, "Gender", genderBox);
        addEditRow(personalGrid, 3, "Date of Birth", dobPicker);

        // ── Emergency Section ──
        Separator sep = new Separator();
        sep.setPadding(new Insets(8, 0, 4, 0));

        Label emergencyTitle = new Label("EMERGENCY PROFILE");
        emergencyTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1.5;");

        var profile = patient.getEmergencyProfile();
        javafx.scene.control.TextField bloodTypeField = new javafx.scene.control.TextField(profile != null ? (nvl(profile.getBloodType()).equals("-") ? "" : profile.getBloodType()) : "");
        bloodTypeField.setPromptText("e.g. O+");
        javafx.scene.control.TextField allergiesField = new javafx.scene.control.TextField(profile != null ? (nvl(profile.getAllergies()).equals("-") ? "" : profile.getAllergies()) : "");
        allergiesField.setPromptText("e.g. Penicillin, Peanuts");
        javafx.scene.control.TextField chronicField = new javafx.scene.control.TextField(profile != null ? (nvl(profile.getChronicConditions()).equals("-") ? "" : profile.getChronicConditions()) : "");
        chronicField.setPromptText("e.g. Diabetes, Hypertension");
        javafx.scene.control.TextField contactNameField = new javafx.scene.control.TextField(profile != null ? (nvl(profile.getEmergencyContactName()).equals("-") ? "" : profile.getEmergencyContactName()) : "");
        contactNameField.setPromptText("Contact full name");
        javafx.scene.control.TextField contactPhoneField = new javafx.scene.control.TextField(profile != null ? (nvl(profile.getEmergencyContactPhone()).equals("-") ? "" : profile.getEmergencyContactPhone()) : "");
        contactPhoneField.setPromptText("+1 234 567 8900");

        GridPane emergencyGrid = new GridPane();
        emergencyGrid.setHgap(14);
        emergencyGrid.setVgap(12);
        addEditRow(emergencyGrid, 0, "Blood Type", bloodTypeField);
        addEditRow(emergencyGrid, 1, "Allergies", allergiesField);
        addEditRow(emergencyGrid, 2, "Chronic Conditions", chronicField);
        addEditRow(emergencyGrid, 3, "Emergency Contact", contactNameField);
        addEditRow(emergencyGrid, 4, "Contact Phone", contactPhoneField);

        VBox dialogContent = new VBox(14, personalTitle, personalGrid, sep, emergencyTitle, emergencyGrid);
        dialogContent.setPadding(new Insets(16, 4, 8, 4));

        dialog.getDialogPane().setContent(dialogContent);

        // Style the Save button as primary
        javafx.scene.Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        if (saveButton instanceof Button btn) {
            btn.getStyleClass().add("button-primary");
        }

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveType) {
            boolean hasExistingData = (patient.getHeightCm() != null || patient.getCurrentBMI() != null || patient.getGender() != null || patient.getDateOfBirth() != null || profile != null);
            if (hasExistingData) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "You are overwriting existing profile information. Do you want to proceed?", ButtonType.YES, ButtonType.NO);
                confirm.setHeaderText("Confirm Update");
                styleAlert(confirm);
                Optional<ButtonType> cRes = confirm.showAndWait();
                if (cRes.isEmpty() || cRes.get() != ButtonType.YES) {
                    return;
                }
            }

            java.util.Map<String, Object> data = new java.util.HashMap<>();
            try { if (!heightField.getText().isBlank()) data.put("heightCm", Double.parseDouble(heightField.getText())); } catch (Exception ignored) {}
            try { if (!bmiField.getText().isBlank()) data.put("currentBMI", Double.parseDouble(bmiField.getText())); } catch (Exception ignored) {}
            if (genderBox.getValue() != null) data.put("gender", genderBox.getValue());
            if (dobPicker.getValue() != null) data.put("dateOfBirth", dobPicker.getValue());
            
            controller.updatePatientProfile(data);
            controller.updateEmergencyProfile(bloodTypeField.getText(), allergiesField.getText(), chronicField.getText(), contactNameField.getText(), contactPhoneField.getText());
            
            refreshProfile();
        }
    }

    private void addEditRow(GridPane grid, int row, String labelText, javafx.scene.Node field) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("profile-edit-label");
        lbl.setMinWidth(130);
        grid.add(lbl, 0, row);
        grid.add(field, 1, row);
        if (field instanceof javafx.scene.layout.Region r) {
            GridPane.setHgrow(r, Priority.ALWAYS);
            r.setMaxWidth(Double.MAX_VALUE);
        }
    }

    private void confirmAndDeleteAccount() {
        Patient patient = getActivePatient();
        if (patient == null) {
            return;
        }

        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Deleting this account will permanently remove vitals, symptoms, medicines, prescriptions, medical records, reports, backups, and emergency details.\n\nDo you want to continue deleting this account and all important logging details?",
                ButtonType.YES,
                ButtonType.NO
        );
        confirm.setHeaderText("Confirm Account Deletion");
        styleAlert(confirm);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) {
            return;
        }

        Optional<String> password = promptForCurrentPassword();
        if (password.isEmpty()) {
            return;
        }
        if (!controller.verifyPassword(password.get())) {
            Alert error = new Alert(Alert.AlertType.ERROR, "Password does not match the current account.");
            error.setHeaderText("Deletion Cancelled");
            styleAlert(error);
            error.showAndWait();
            return;
        }

        try {
            ViewFactory factory = ViewFactory.getInstance();
            NotificationScheduler.stopBackgroundTasks();
            if (factory != null) {
                factory.invalidateAll();
            }
            controller.deleteAccount();
            controller.logout();
            SessionManager.logout();
        } catch (Exception ex) {
            Alert error = new Alert(Alert.AlertType.ERROR, "Account deletion failed: " + ex.getMessage());
            error.setHeaderText("Deletion Failed");
            styleAlert(error);
            error.showAndWait();
        }
    }

    private Optional<String> promptForCurrentPassword() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Confirm Password");
        dialog.setHeaderText("Enter the current login password to confirm deletion.");
        styleDialog(dialog);

        ButtonType confirmType = new ButtonType("Delete Account", ButtonType.OK.getButtonData());
        dialog.getDialogPane().getButtonTypes().addAll(confirmType, ButtonType.CANCEL);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Current password");
        VBox box = new VBox(10, new Label("Password"), passwordField);
        box.setPadding(new Insets(10, 0, 0, 0));
        dialog.getDialogPane().setContent(box);

        javafx.scene.Node confirmButton = dialog.getDialogPane().lookupButton(confirmType);
        confirmButton.disableProperty().bind(passwordField.textProperty().isEmpty());

        dialog.setResultConverter(buttonType -> buttonType == confirmType ? passwordField.getText() : null);
        return dialog.showAndWait();
    }

    private void addGridRow(GridPane grid, int row, String key, String value) {
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add("profile-field-key");
        keyLabel.setMinWidth(140);

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("profile-field-value");

        grid.add(keyLabel, 0, row);
        grid.add(valueLabel, 1, row);
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        return parts.length > 1
                ? String.valueOf(parts[0].charAt(0)) + parts[parts.length - 1].charAt(0)
                : String.valueOf(parts[0].charAt(0));
    }

    private String nvl(Object value) {
        return value != null ? value.toString() : "-";
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

    private void styleDialog(Dialog<?> dialog) {
        try {
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/synapse.css").toExternalForm());
        } catch (Exception ignored) {}
    }

    public AccountController getController() {
        return controller;
    }
}
