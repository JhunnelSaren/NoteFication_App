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
    private String status; // Use status instead of completed
    private boolean reminderDone; // Track if the reminder has been done

    // Constructor for notes without reminder
    public Note(String content, String color, LocalDate date) {
        this.content = content;
        this.color = color;
        this.date = date;
        this.tasks = new ArrayList<>();
        this.status = "Pending"; // Default status
        this.reminderDone = false; // Default reminder state
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
        this.status = "Pending"; // Set status to Pending when reminder is set
        this.reminderDone = false; // Reset reminder done status
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

    // Check if the reminder is done
    public boolean isReminderDone() {
        return !reminderDone;
    }

    // Set the reminder as done
    public void setReminderDone(boolean done) {
        this.reminderDone = done;
        if (done) {
            this.status = "Completed"; // Update status when reminder is done
        }
    }

    // Set the status directly
    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public String getCreationTime() {
        return null;
    }
}