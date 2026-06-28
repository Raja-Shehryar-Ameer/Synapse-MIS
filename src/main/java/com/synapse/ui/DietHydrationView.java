package com.synapse.ui;

import com.synapse.controller.DietHydrationController;
import com.synapse.model.FoodItem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

/**
 * UC4: Track Diet, BMI and Hydration.
 */
public class DietHydrationView extends VBox {

    private final DietHydrationController controller = new DietHydrationController();
    private final VBox foodItemsList = new VBox(6);
    private final List<FoodItem> pendingItems = new ArrayList<>();

    public DietHydrationView() {
        getStyleClass().add("content-area");
        setSpacing(24);
        setPadding(new Insets(36));

        Label title = new Label("Diet & Hydration");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Track your daily nutrition, calories and water intake");
        sub.getStyleClass().add("page-subtitle");

        HBox columns = new HBox(18);
        columns.setAlignment(Pos.TOP_LEFT);

        VBox dietCard = buildDietCard();
        VBox hydCard  = buildHydrationCard();
        VBox wgtCard  = buildWeightCard();
        HBox.setHgrow(dietCard, Priority.ALWAYS);
        HBox.setHgrow(hydCard,  Priority.ALWAYS);
        HBox.setHgrow(wgtCard,  Priority.ALWAYS);
        columns.getChildren().addAll(dietCard, hydCard, wgtCard);

        getChildren().addAll(title, sub, columns);
    }

    // ── Diet Card ─────────────────────────────────────────────

    private VBox buildDietCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        Label header = new Label("🥗 Log Diet");
        header.getStyleClass().add("card-header");

        TextField foodName = field("Food item name");
        TextField portion  = field("Portion (e.g. 1 cup)");
        TextField calories = field("Estimated calories");

        Label feedback = new Label();
        feedback.setWrapText(true);

        Button addItem = new Button("+ Add Item");
        addItem.getStyleClass().add("button-secondary");
        addItem.setMaxWidth(Double.MAX_VALUE);
        addItem.setOnAction(e -> {
            if (foodName.getText().isBlank()) { setFeedback(feedback, "Enter a food item name.", "error"); return; }
            if (calories.getText().isBlank()) { setFeedback(feedback, "Enter the estimated calories.", "error"); return; }
            try {
                int cal = Integer.parseInt(calories.getText().trim());
                if (cal <= 0) { setFeedback(feedback, "Calories must be > 0.", "error"); return; }
                FoodItem item = new FoodItem(foodName.getText().trim(), portion.getText().trim(), cal);
                pendingItems.add(item);
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color: #f0fdf4; -fx-background-radius: 6; -fx-padding: 6 10; -fx-border-color: #bbf7d0; -fx-border-radius: 6; -fx-border-width: 1;");
                Label lbl = new Label("• " + item.getName() + " — " + cal + " cal");
                lbl.setStyle("-fx-text-fill: #166534; -fx-font-size: 12px;");
                row.getChildren().add(lbl);
                foodItemsList.getChildren().add(row);
                foodName.clear(); portion.clear(); calories.clear(); feedback.setText("");
            } catch (NumberFormatException ex) { setFeedback(feedback, "Calories must be a number.", "error"); }
        });

        Button submit = new Button("Save Meal");
        submit.getStyleClass().add("button-primary");
        submit.setMaxWidth(Double.MAX_VALUE);

        // History
        VBox histBox = new VBox(6);
        Runnable reloadHist = () -> {
            histBox.getChildren().clear();
            var p = SessionManager.getCurrentPatient();
            if (p != null) {
                p.getDietLogs().forEach(log -> {
                    HBox row = new HBox(10);
                    row.getStyleClass().add("log-entry");
                    row.setAlignment(Pos.CENTER_LEFT);

                    String items = log.getFoodItems().stream()
                            .map(com.synapse.model.FoodItem::getName).toList().toString();
                    items = items.replaceAll("[\\[\\]]", "");
                    if (items.length() > 35) items = items.substring(0, 35) + "…";

                    VBox info = new VBox(2);
                    Label mainLbl = new Label(log.getTotalCalories() + " cal  ·  " + items);
                    mainLbl.getStyleClass().add("log-entry-text");
                    Label dateLbl = new Label(log.getLogDate() != null
                            ? log.getLogDate().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))
                            : "");
                    dateLbl.setStyle("-fx-text-fill: #d1d5db; -fx-font-size: 10px;");
                    info.getChildren().addAll(mainLbl, dateLbl);
                    HBox.setHgrow(info, Priority.ALWAYS);

                    Button del = new Button("✕");
                    del.getStyleClass().add("log-delete-btn");
                    del.setOnAction(ev -> {
                        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.CONFIRMATION,
                                "Delete this diet log?\n\nThis action cannot be undone.",
                                javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
                        confirm.setHeaderText("Delete Diet Log");
                        if (confirm.showAndWait().orElse(javafx.scene.control.ButtonType.NO) == javafx.scene.control.ButtonType.YES) {
                            controller.deleteDietLog(p, log.getLogId());
                            histBox.getChildren().remove(row);
                        }
                    });
                    row.getChildren().addAll(info, del);
                    histBox.getChildren().add(row);
                });
            }
        };
        reloadHist.run();

        submit.setOnAction(e -> {
            var patient = SessionManager.getCurrentPatient();
            if (patient == null) { setFeedback(feedback, "Please log in first.", "error"); return; }
            if (pendingItems.isEmpty()) { setFeedback(feedback, "Add at least one food item.", "error"); return; }
            try {
                var log = controller.logDiet(patient, new ArrayList<>(pendingItems));
                setFeedback(feedback, "✓ Meal saved — " + log.getTotalCalories() + " total calories", "success");
                pendingItems.clear(); foodItemsList.getChildren().clear();
                reloadHist.run();
            } catch (Exception ex) { setFeedback(feedback, "Error: " + ex.getMessage(), "error"); }
        });

        Separator sep = new Separator();
        Label recentLabel = new Label("Recent Meals");
        recentLabel.getStyleClass().add("form-label");

        card.getChildren().addAll(
                header,
                labeledField("Food Name", foodName),
                labeledField("Portion", portion),
                labeledField("Calories", calories),
                addItem, foodItemsList, feedback, submit,
                sep, recentLabel, histBox
        );
        return card;
    }

    // ── Hydration Card ────────────────────────────────────────

    private VBox buildHydrationCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        Label header = new Label("💧 Hydration");
        header.getStyleClass().add("card-header");

        TextField goal   = field("e.g. 2000");
        TextField intake = field("e.g. 500");

        Label feedback = new Label();
        feedback.setWrapText(true);

        Button submit = new Button("Log Water Intake");
        submit.getStyleClass().add("button-primary");
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setOnAction(e -> {
            var patient = SessionManager.getCurrentPatient();
            if (patient == null) { setFeedback(feedback, "Please log in first.", "error"); return; }
            try {
                int g = Integer.parseInt(goal.getText().trim());
                int i = Integer.parseInt(intake.getText().trim());
                var log = controller.logHydration(patient, g, i);
                double pct = log.checkGoalProgress();
                String msg = String.format("✓ Logged — %.0f%% of daily goal", pct);
                setFeedback(feedback, msg, pct >= 100 ? "success" : "warn");
                goal.clear(); intake.clear();
            } catch (NumberFormatException ex) {
                setFeedback(feedback, "Enter valid numbers (ml).", "error");
            } catch (Exception ex) {
                setFeedback(feedback, "Error: " + ex.getMessage(), "error");
            }
        });

        // Tip box
        VBox tip = new VBox(4);
        tip.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 8; -fx-border-color: #bfdbfe; -fx-border-radius: 8; -fx-border-width: 1; -fx-padding: 12;");
        Label tipTitle = new Label("Daily Recommendation");
        tipTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        Label tipText = new Label("Adults should drink 2,000–3,000 ml of water daily. Adjust for exercise and temperature.");
        tipText.setStyle("-fx-font-size: 11px;");
        tipText.setWrapText(true);
        tip.getChildren().addAll(tipTitle, tipText);

        card.getChildren().addAll(
                header,
                labeledField("Daily Goal (ml)", goal),
                labeledField("Water Consumed (ml)", intake),
                feedback, submit, tip
        );
        return card;
    }

    // ── Weight & BMI Card ─────────────────────────────────────

    private VBox buildWeightCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        Label header = new Label("⚖️ Weight & BMI");
        header.getStyleClass().add("card-header");

        TextField weight = field("e.g. 70.5");

        Label feedback = new Label();
        feedback.setWrapText(true);

        // BMI result display
        Label bmiResult = new Label();
        bmiResult.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #22c55e;");
        Label bmiLabel  = new Label("BMI");
        bmiLabel.setStyle("-fx-font-size: 11px;");

        VBox bmiDisplay = new VBox(2, bmiResult, bmiLabel);
        bmiDisplay.setAlignment(Pos.CENTER);
        bmiDisplay.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 10; -fx-border-color: #eaecef; -fx-border-radius: 10; -fx-border-width: 1; -fx-padding: 16;");

        Button submit = new Button("Update Weight");
        submit.getStyleClass().add("button-primary");
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setOnAction(e -> {
            var patient = SessionManager.getCurrentPatient();
            if (patient == null) { setFeedback(feedback, "Please log in first.", "error"); return; }
            try {
                double w = Double.parseDouble(weight.getText().trim());
                Double bmi = controller.updateWeight(patient, w);
                if (bmi != null) {
                    bmiResult.setText(String.format("%.1f", bmi));
                    String cat = bmi < 18.5 ? "Underweight" : bmi < 25 ? "Normal" : bmi < 30 ? "Overweight" : "Obese";
                    String color = bmi < 18.5 ? "#3b82f6" : bmi < 25 ? "#22c55e" : bmi < 30 ? "#f59e0b" : "#ef4444";
                    bmiResult.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
                    setFeedback(feedback, "✓ Weight updated — Category: " + cat, "success");
                } else {
                    setFeedback(feedback, "✓ Weight updated.", "success");
                }
                weight.clear();
            } catch (NumberFormatException ex) {
                setFeedback(feedback, "Enter a valid number (kg).", "error");
            } catch (Exception ex) {
                setFeedback(feedback, "Error: " + ex.getMessage(), "error");
            }
        });

        // BMI reference
        VBox ref = buildBmiReference();

        card.getChildren().addAll(
                header, bmiDisplay,
                labeledField("Weight (kg)", weight),
                feedback, submit, ref
        );
        return card;
    }

    private VBox buildBmiReference() {
        VBox box = new VBox(6);
        box.setStyle("-fx-padding: 4 0 0 0;");
        Label title = new Label("BMI Reference");
        title.getStyleClass().add("form-label");
        box.getChildren().add(title);
        String[][] refs = {
            {"< 18.5", "Underweight", "#3b82f6"},
            {"18.5 – 24.9", "Normal", "#22c55e"},
            {"25 – 29.9", "Overweight", "#f59e0b"},
            {"≥ 30", "Obese", "#ef4444"}
        };
        for (String[] r : refs) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            Label range = new Label(r[0]);
            range.setStyle("-fx-font-size: 11px; -fx-min-width: 80;");
            Label cat = new Label(r[1]);
            cat.setStyle("-fx-text-fill: " + r[2] + "; -fx-font-size: 11px; -fx-font-weight: bold;");
            row.getChildren().addAll(range, cat);
            box.getChildren().add(row);
        }
        return box;
    }

    // ── Helpers ───────────────────────────────────────────────

    private TextField field(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        return tf;
    }

    private VBox labeledField(String label, TextField tf) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("form-label");
        return new VBox(5, lbl, tf);
    }

    private Button delBtn(Runnable action) {
        Button b = new Button("✕");
        b.getStyleClass().add("log-delete-btn");
        b.setOnAction(e -> action.run());
        return b;
    }

    private void setFeedback(Label lbl, String msg, String type) {
        lbl.setText(msg);
        lbl.getStyleClass().removeAll("feedback-success", "feedback-error", "feedback-warn");
        lbl.getStyleClass().add("feedback-" + type);
    }
}
