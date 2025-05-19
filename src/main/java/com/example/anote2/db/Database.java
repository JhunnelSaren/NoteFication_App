package com.example.anote2.db;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.Note;
import model.Task;

import java.io.File;
import java.lang.reflect.Type;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ALL")
public class Database {
    private static final String DB_PATH = System.getProperty("user.home") + File.separator + "anote-notes.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    private static final Gson gson = new Gson();
    private static final Type taskListType = new TypeToken<List<Task>>() {}.getType();
    private static LocalTime LocalTime;

    static {
        createTableIfNotExists();
    }

    public static void initialize() {
        System.out.println("Initializing database at: " + DB_PATH);
        createTableIfNotExists();
    }

    private static void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                content TEXT NOT NULL,
                color TEXT NOT NULL,
                date TEXT NOT NULL,
                tasks TEXT,
                reminder_date TEXT,
                reminder_time TEXT
            );
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Database table checked/created successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void insertNote(Note note) {
        String sql = "INSERT INTO notes (content, color, date, tasks, reminder_date, reminder_time) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, note.getContent());
            pstmt.setString(2, note.getColor());
            pstmt.setString(3, note.getDate().toString());
            pstmt.setString(4, gson.toJson(note.getTasks()));

            // Insert reminder date and time, handling null values
            if (note.getReminderDate() != null) {
                pstmt.setString(5, note.getReminderDate().toString());
                pstmt.setString(6, note.getReminderTime().toString());
            } else {
                pstmt.setString(5, null);
                pstmt.setString(6, null);
            }

            pstmt.executeUpdate();

            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                note.setId(generatedKeys.getInt(1));
            }

            System.out.println("Inserted note: " + note.getContent());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<Note> getAllNotes() {
        List<Note> notes = new ArrayList<>();
        String sql = "SELECT id, content, color, date, tasks, reminder_date, reminder_time FROM notes";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String content = rs.getString("content");
                String color = rs.getString("color");
                LocalDate date = LocalDate.parse(rs.getString("date"));
                String tasksJson = rs.getString("tasks");

                // Retrieve reminder date and time
                String reminderDateString = rs.getString("reminder_date");
                String reminderTimeString = rs.getString("reminder_time");
                LocalDate reminderDate = null;
                LocalTime reminderTime = null;

                if (reminderDateString != null) {
                    reminderDate = LocalDate.parse(reminderDateString);
                }
                if (reminderTimeString != null) {
                    reminderTime = LocalTime.parse(reminderTimeString);
                }

                List<Task> tasks = gson.fromJson(tasksJson, taskListType);
                if (tasks == null) tasks = new ArrayList<>();

                Note note = new Note(content, color, date);
                note.setId(id); // Set the ID
                note.setTasks(tasks); // Set the tasks
                if (reminderDate != null && reminderTime != null) {
                    note.setReminder(reminderDate, reminderTime); // Set the reminder
                }

                notes.add(note);
            }

            System.out.println("Loaded " + notes.size() + " notes from DB.");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return notes;
    }

    public static void updateNote(Note note) {
        String sql = "UPDATE notes SET content = ?, color = ?, date = ?, tasks = ?, reminder_date = ?, reminder_time = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, note.getContent());
            pstmt.setString(2, note.getColor());
            pstmt.setString(3, note.getDate().toString());
            pstmt.setString(4, gson.toJson(note.getTasks()));

            // Update reminder date and time, handling null values
            if (note.getReminderDate() != null) {
                pstmt.setString(5, note.getReminderDate().toString());
                pstmt.setString(6, note.getReminderTime().toString());
            } else {
                pstmt.setString(5, null);
                pstmt.setString(6, null);
            }

            pstmt.setInt(7, note.getId());
            pstmt.executeUpdate();

            System.out.println("Updated note ID " + note.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteNote(Note note) {
        String sql = "DELETE FROM notes WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, note.getId());
            pstmt.executeUpdate();

            System.out.println("Deleted note ID " + note.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void testInsertNote() {
    }
}