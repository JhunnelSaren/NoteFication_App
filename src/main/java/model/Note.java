package model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Note {
    private int id;
    private String content;
    private final String color;
    private final LocalDate date;
    private LocalDate reminderDate;
    private LocalTime reminderTime;
    private List<Task> tasks;

    // Constructor for notes without reminder
    public Note(String content, String color, LocalDate date) {
        this.content = content;
        this.color = color;
        this.date = date;
        this.tasks = new ArrayList<>();
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getColor() {
        return color;
    }

    public LocalDate getDate() {
        return date;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public LocalDate getReminderDate() {
        return reminderDate;
    }

    public LocalTime getReminderTime() {
        return reminderTime;
    }

    // Method for formatted date display
    public String getFormattedDate() {
        return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
    }

    // Method to set reminder for the note
    public void setReminder(LocalDate date, LocalTime time) {
        this.reminderDate = date;
        this.reminderTime = time;
    }

    // Method to check if the note has a reminder
    public boolean hasReminder() {
        return reminderDate != null && reminderTime != null;
    }

    // Method to get formatted reminder date and time
    public String getFormattedReminder() {
        if (hasReminder()) {
            return reminderDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) + " " +
                    reminderTime.format(DateTimeFormatter.ofPattern("hh:mm a"));
        }
        return "No reminder set";
    }

    public boolean isReminderDone() {
        return true;
    }

    public void setReminderDone(boolean ignoredB) {
    }
}