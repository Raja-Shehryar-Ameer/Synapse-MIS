package com.synapse.ui;

import com.synapse.controller.CalendarController;
import com.synapse.model.CalendarEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * UC9: Calendar view — month grid + event form + event list.
 */
public class CalendarView extends VBox {

    private final CalendarController controller = new CalendarController();
    private final GridPane calGrid = new GridPane();
    private final VBox eventsList = new VBox(8);
    private YearMonth currentMonth = YearMonth.now();
    private Label monthLabel;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy");

    public CalendarView() {
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

        Label title = new Label("📅 Calendar");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Schedule appointments, medications and health events");
        sub.getStyleClass().add("page-subtitle");

        HBox columns = new HBox(20);
        columns.setAlignment(Pos.TOP_LEFT);

        VBox calCard  = buildCalendarCard();
        VBox formCard = buildEventFormCard();
        calCard.setPrefWidth(480);
        formCard.setPrefWidth(320);
        HBox.setHgrow(formCard, Priority.ALWAYS);

        columns.getChildren().addAll(calCard, formCard);

        // Pre-load events
        var patient = SessionManager.getCurrentPatient();
        if (patient != null) patient.getCalendarEvents().forEach(this::addEventRow);

        content.getChildren().addAll(title, sub, columns);
        return content;
    }

    // ── Calendar Grid ─────────────────────────────────────────

    private VBox buildCalendarCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        // Month navigator
        Button prev = new Button("◀");
        prev.getStyleClass().add("button-secondary");
        prev.setStyle("-fx-padding: 5 12;");
        Button next = new Button("▶");
        next.getStyleClass().add("button-secondary");
        next.setStyle("-fx-padding: 5 12;");

        monthLabel = new Label(formatMonth(currentMonth));
        monthLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; ");

        Region spacer1 = new Region(); HBox.setHgrow(spacer1, Priority.ALWAYS);
        Region spacer2 = new Region(); HBox.setHgrow(spacer2, Priority.ALWAYS);

        prev.setOnAction(e -> { currentMonth = currentMonth.minusMonths(1); monthLabel.setText(formatMonth(currentMonth)); renderCalendar(); });
        next.setOnAction(e -> { currentMonth = currentMonth.plusMonths(1); monthLabel.setText(formatMonth(currentMonth)); renderCalendar(); });

        HBox nav = new HBox(10, prev, spacer1, monthLabel, spacer2, next);
        nav.setAlignment(Pos.CENTER);

        calGrid.setHgap(5); calGrid.setVgap(5);
        renderCalendar();

        // Events list
        Label evTitle = new Label("Upcoming Events");
        evTitle.getStyleClass().add("form-label");
        evTitle.setPadding(new Insets(8, 0, 0, 0));

        ScrollPane evScroll = new ScrollPane(eventsList);
        evScroll.setFitToWidth(true);
        evScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        evScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        evScroll.setPrefHeight(200);

        card.getChildren().addAll(nav, calGrid, new Separator(), evTitle, evScroll);
        return card;
    }

    private void renderCalendar() {
        calGrid.getChildren().clear();

        // Day headers
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int i = 0; i < 7; i++) {
            Label lbl = new Label(days[i]);
            lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
            lbl.setPrefWidth(52);
            lbl.setAlignment(Pos.CENTER);
            calGrid.add(lbl, i, 0);
        }

        LocalDate first = currentMonth.atDay(1);
        int startCol     = first.getDayOfWeek().getValue() - 1;
        int daysInMonth  = currentMonth.lengthOfMonth();
        LocalDate today  = LocalDate.now();

        int row = 1, col = startCol;
        for (int d = 1; d <= daysInMonth; d++) {
            LocalDate date = currentMonth.atDay(d);
            boolean isToday = date.equals(today);
            boolean isPast  = date.isBefore(today);

            Label cell = new Label(String.valueOf(d));
            cell.setPrefSize(52, 36);
            cell.setAlignment(Pos.CENTER);

            if (isToday) {
                cell.setStyle(
                    "-fx-background-color: #22c55e;" +
                    "-fx-background-radius: 8;" +
                    "-fx-text-fill: #ffffff;" +
                    "-fx-font-weight: bold;" +
                    "-fx-font-size: 13px;"
                );
            } else if (isPast) {
                cell.setStyle(
                    "-fx-background-color: #f9fafb;" +
                    "-fx-background-radius: 8;" +
                    "-fx-text-fill: #d1d5db;" +
                    "-fx-font-size: 13px;"
                );
            } else {
                cell.setStyle(
                    "-fx-background-color: #f9fafb;" +
                    "-fx-background-radius: 8;" +
                    "" +
                    "-fx-font-size: 13px;" +
                    "-fx-cursor: hand;"
                );
                cell.setOnMouseEntered(e -> cell.setStyle(
                    "-fx-background-color: #fef3c7;" +
                    "-fx-background-radius: 8;" +
                    "-fx-text-fill: #b45309;" +
                    "-fx-font-size: 13px;" +
                    "-fx-cursor: hand;"
                ));
                cell.setOnMouseExited(e -> cell.setStyle(
                    "-fx-background-color: #f9fafb;" +
                    "-fx-background-radius: 8;" +
                    "" +
                    "-fx-font-size: 13px;" +
                    "-fx-cursor: hand;"
                ));
            }

            calGrid.add(cell, col, row);
            col++;
            if (col > 6) { col = 0; row++; }
        }
    }

    private String formatMonth(YearMonth ym) {
        return ym.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
                + " " + ym.getYear();
    }

    // ── Event Form ────────────────────────────────────────────

    private VBox buildEventFormCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        Label header = new Label("📅 New Event");
        header.getStyleClass().add("card-header");

        TextField eventTitle = new TextField();
        eventTitle.setPromptText("e.g. Dr. Smith Appointment");

        ComboBox<String> eventType = new ComboBox<>();
        eventType.getItems().addAll("Appointment", "Medication", "Exercise", "Follow-up", "Other");
        eventType.setPromptText("Event type");
        eventType.setMaxWidth(Double.MAX_VALUE);

        DatePicker eventDate = new DatePicker(LocalDate.now());
        eventDate.setMaxWidth(Double.MAX_VALUE);

        TextField eventTime = new TextField();
        eventTime.setPromptText("Time (HH:MM e.g. 09:30)");

        Label feedback = new Label();
        feedback.setWrapText(true);

        Button addBtn = new Button("Add Event");
        addBtn.getStyleClass().add("button-primary");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setOnAction(e -> {
            var patient = SessionManager.getCurrentPatient();
            if (patient == null) { setFeedback(feedback, "Please log in first.", "error"); return; }
            if (eventTitle.getText().isBlank()) { setFeedback(feedback, "Enter an event title.", "error"); return; }
            if (eventDate.getValue() == null) { setFeedback(feedback, "Select a date.", "error"); return; }
            try {
                LocalTime time = LocalTime.parse(eventTime.getText().trim());
                if (eventDate.getValue().equals(LocalDate.now()) && time.isBefore(LocalTime.now())) {
                    showAlert(Alert.AlertType.WARNING, "Invalid Time", "The time selected has already passed for today. Please select a future time.");
                    return;
                }
                
                CalendarEvent ev = controller.addEvent(patient, eventTitle.getText().trim(),
                        eventType.getValue(), eventDate.getValue(), time, "15min");
                if (ev == null) {
                    showAlert(Alert.AlertType.WARNING, "Scheduling Conflict", "There is a scheduling conflict at that time!");
                } else {
                    setFeedback(feedback, "✓ Event added and reminder scheduled.", "success");
                    addEventRow(ev);
                }
                eventTitle.clear(); eventTime.clear(); eventDate.setValue(LocalDate.now()); eventType.setValue(null);
            } catch (Exception ex) {
                setFeedback(feedback, "Invalid time. Use HH:MM format.", "error");
            }
        });

        // Event type color tip
        Label tip = new Label("💡  Events with conflicts will not be saved.");
        tip.setStyle("-fx-text-fill: #d1d5db; -fx-font-size: 11px;");
        tip.setWrapText(true);

        card.getChildren().addAll(header,
                lf("Event Title", eventTitle),
                lf("Type", eventType),
                lf("Date", eventDate),
                lf("Time (24hr)", eventTime),
                feedback, addBtn, tip);
        return card;
    }

    private void addEventRow(CalendarEvent event) {
        HBox row = new HBox(12);
        row.getStyleClass().add("log-entry");
        row.setAlignment(Pos.CENTER_LEFT);

        // Type badge color
        String type   = event.getEventType() != null ? event.getEventType() : "Other";
        String bgCol  = eventTypeColor(type);

        Label typeBadge = new Label(type);
        typeBadge.setStyle(
            "-fx-background-color: " + bgCol + ";" +
            "" +
            "-fx-font-size: 11px; -fx-font-weight: bold;" +
            "-fx-padding: 2 8; -fx-background-radius: 10;"
        );

        VBox info = new VBox(2);
        Label titleLbl = new Label(event.getTitle());
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        String dateStr = event.getEventDate() != null ? event.getEventDate().format(DATE_FMT) : "";
        String timeStr = event.getEventTime() != null ? event.getEventTime().toString() : "";
        Label dateLbl  = new Label(dateStr + "  ·  " + timeStr);
        dateLbl.setStyle("-fx-font-size: 11px;");
        info.getChildren().addAll(titleLbl, dateLbl);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button del = new Button("✕");
        del.getStyleClass().add("log-delete-btn");
        del.setOnAction(e -> {
            var patient = SessionManager.getCurrentPatient();
            if (patient != null) {
                controller.deleteCalendarEvent(patient, event.getEventId());
                eventsList.getChildren().remove(row);
            }
        });

        row.getChildren().addAll(typeBadge, info, del);
        eventsList.getChildren().add(0, row);
    }

    private String eventTypeColor(String type) {
        return switch (type) {
            case "Appointment" -> "#dcfce7";
            case "Medication"  -> "#dbeafe";
            case "Exercise"    -> "#fef9c3";
            case "Follow-up"   -> "#fce7f3";
            default            -> "#f3f4f6";
        };
    }

    private VBox lf(String label, javafx.scene.Node node) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("form-label");
        return new VBox(5, lbl, node);
    }

    private void setFeedback(Label l, String msg, String type) {
        l.setText(msg);
        l.getStyleClass().removeAll("feedback-success","feedback-error","feedback-warn");
        l.getStyleClass().add("feedback-" + type);
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
