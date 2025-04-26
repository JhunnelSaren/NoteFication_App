package DateTimePicker;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalTime;

public class DateTimePicker extends VBox {
    private final DatePicker datePicker;
    private final ComboBox<String> hourPicker;
    private final ComboBox<String> minutePicker;
    private final ComboBox<String> amPmPicker;

    public DateTimePicker() {
        datePicker = new DatePicker();

        // Hour Picker (1-12)
        hourPicker = new ComboBox<>();
        for (int hour = 1; hour <= 12; hour++) {
            hourPicker.getItems().add(String.format("%02d", hour));
        }
        hourPicker.setValue("12"); // Default hour

        // Minute Picker (00-59)
        minutePicker = new ComboBox<>();
        for (int minute = 0; minute < 60; minute++) {
            minutePicker.getItems().add(String.format("%02d", minute));
        }
        minutePicker.setValue("00"); // Default minute

        // AM/PM Picker
        amPmPicker = new ComboBox<>();
        amPmPicker.getItems().addAll("AM", "PM");
        amPmPicker.setValue("AM"); // Default AM/PM

        // Adding labels for the dropdowns
        getChildren().addAll(new Label("Select Date:"), datePicker,
                new Label("Select Hour:"), hourPicker,
                new Label("Select Minute:"), minutePicker,
                new Label("Select AM/PM:"), amPmPicker);
    }

    public LocalDate getSelectedDate() {
        return datePicker.getValue();
    }

    public LocalTime getSelectedTime() {
        try {
            int hour = Integer.parseInt(hourPicker.getValue());
            int minute = Integer.parseInt(minutePicker.getValue());
            String amPm = amPmPicker.getValue();

            if (amPm.equals("PM") && hour != 12) {
                hour += 12; // Convert to 24-hour format
            } else if (amPm.equals("AM") && hour == 12) {
                hour = 0; // Convert 12 AM to 0 hours
            }

            return LocalTime.of(hour, minute);
        } catch (NumberFormatException e) {
            System.err.println("Invalid input in time pickers.");
            return null; // Or handle the error as appropriate for your application
        }
    }
}