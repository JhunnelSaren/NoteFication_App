package controller;

import DateTimePicker.DateTimePicker;
import com.example.anote2.db.Database;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;
import model.Note;
import service.NotificationService;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ALL")
public class MainController implements Initializable {

    public VBox noteContainer;
    public Button colorYellow;
    public Button colorGreen;
    public Button colorBlue;
    public Button colorPurple;
    public Button colorOrange;
    @FXML private FlowPane notesContainer;
    @FXML private Button addNoteButton;
    @FXML private VBox colorPickerBox;
    @FXML private ScrollPane notesScrollPane;
    @FXML private Button backToTopButton;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private ComboBox<String> colorFilterComboBox;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private DatePicker dateFilterPicker;
    @FXML private Button bellButton;
    @FXML private Label reminderCountLabel; // Add this line

    private String selectedColor = null;
    private final List<Node> allNotes = new ArrayList<>();
    private static final Map<String, String> colorMap = Map.of(
            "Yellow", "#FFD56A",
            "Red", "#F28B6C",
            "Purple", "#B199FF",
            "Cyan", "#00CFFF",
            "Green", "#E5FF99"
    );
    private ScheduledExecutorService scheduler;
    private final ObservableList<String>    reminders = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Database.initialize();
        loadNotesFromDatabase();

        scheduler = Executors.newScheduledThreadPool(1);

        addNoteButton.setOnAction(_ -> toggleColorPicker());
        backToTopButton.setOnAction(_ -> scrollToTop());

        for (Node node : colorPickerBox.getChildren()) {
            if (node instanceof Button colorButton) {
                colorButton.setOnMouseClicked(this::onColorSelected);
            }
        }

        setupScrollPane();
        setupSearchField();
        setupFilters();

        // Start the reminder check scheduler
        startReminderCheckScheduler();
    }

    private void startReminderCheckScheduler() {
        // Schedule a task to run every minute
        scheduler.scheduleAtFixedRate(this::checkReminders, 0, 1, TimeUnit.MINUTES);
    }

    private void checkReminders() {
        Platform.runLater(() -> {
            for (Node node : allNotes) {
                if (node instanceof VBox noteBox) {
                    Note note = (Note) noteBox.getUserData();
                    if (note != null && note.hasReminder() && !note.isReminderDone()) {
                        LocalDateTime reminderDateTime = LocalDateTime.of(note.getReminderDate(), note.getReminderTime());
                        boolean reminderHasPassed = reminderDateTime.isBefore(LocalDateTime.now());

                        if (reminderHasPassed) {
                            // Reminder time has passed, mark as completed
                            note.setReminderDone(true);
                            Database.updateNote(note);
                            updateNoteInUI(note, noteBox); // Refresh the UI
                        }
                    }
                }
            }
        });
    }

    private void updateNoteInUI(Note note, VBox noteBox) {
        // Remove the old note from the UI
        notesContainer.getChildren().remove(noteBox);
        allNotes.remove(noteBox);

        // Add the updated note back to the UI
        addNoteToUI(note);
    }

    private void scheduleNotification(LocalDateTime targetDateTime, Note note) {
        long delay = Date.from(targetDateTime.atZone(ZoneId.systemDefault()).toInstant()).getTime() - System.currentTimeMillis();

        if (delay > 0) {
            scheduler.schedule(() -> {
                System.out.println("Notification: Time to check your note!");
                Platform.runLater(() -> {
                    NotificationService.showNotification("Note Reminder", "Time to check your note!");
                    playSound(); // Play sound when reminder is triggered
                });
            }, delay, TimeUnit.MILLISECONDS);
        } else {
            System.out.println("Cannot set reminder in the past.");
        }
    }

    // Method to play a sound
    private void playSound() {
        try {
            String soundFile = "src/main/resources/com/example/notefication_app/sound/alarm.wav";
            System.out.println("Attempting to play sound from: " + soundFile); // Debugging
            Media sound = new Media(new File(soundFile).toURI().toString());
            MediaPlayer mediaPlayer = new MediaPlayer(sound);
            mediaPlayer.setOnError(() -> System.err.println("MediaPlayer error: " + mediaPlayer.getError().getMessage()));
            mediaPlayer.play();
        } catch (Exception e) {
            System.err.println("Error playing sound: " + e.getMessage());
            e.printStackTrace(); // Print the full stack trace for debugging
        }
    }

    public void shutdownScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    private void scrollToTop() {
        Timeline scrollAnim = new Timeline();
        KeyValue kv = new KeyValue(notesScrollPane.vvalueProperty(), 0, Interpolator.EASE_BOTH);
        KeyFrame kf = new KeyFrame(Duration.millis(350), kv);
        scrollAnim.getKeyFrames().add(kf);
        scrollAnim.play();
    }

    private void setupScrollPane() {
        notesScrollPane.vvalueProperty().addListener((_, _, newVal) -> {
            boolean show = newVal.doubleValue() > 0.1;
            if (show && !backToTopButton.isVisible()) {
                backToTopButton.setVisible(true);
                backToTopButton.setOpacity(0);
                backToTopButton.setTranslateY(10);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), backToTopButton);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);

                TranslateTransition slideUp = new TranslateTransition(Duration.millis(200), backToTopButton);
                slideUp.setFromY(10);
                slideUp.setToY(0);

                new ParallelTransition(fadeIn, slideUp).play();
            } else if (!show && backToTopButton.isVisible()) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(150), backToTopButton);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(_ -> backToTopButton.setVisible(false));
                fadeOut.play();
            }
        });
    }

    private void setupSearchField() {
        searchField.textProperty().addListener((_, _, newValue) -> {
            String query = newValue.toLowerCase().trim();
            notesContainer.getChildren().clear();

            if (query.isEmpty()) {
                applyFilters();
            } else {
                for (Node note : allNotes) {
                    if (note instanceof VBox) {
                        for (Node child : ((VBox) note).getChildren()) {
                            if (child instanceof Text textNode) {
                                if (textNode.getText().toLowerCase().contains(query)) {
                                    notesContainer.getChildren().add(note);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    private void setupFilters() {
        colorFilterComboBox.getItems().setAll("All", "Yellow", "Red", "Purple", "Cyan", "Green");
        colorFilterComboBox.setValue("All");
        colorFilterComboBox.setOnAction(_ -> applyFilters());

        statusFilterComboBox.getItems().setAll("All", "Completed", "Pending");
        statusFilterComboBox.setValue("All");
        statusFilterComboBox.setOnAction(_ -> applyFilters());

        dateFilterPicker.setOnAction(_ -> applyFilters());
    }

    private void toggleColorPicker() {
        boolean show = !colorPickerBox.isVisible();
        if (show) {
            animateColorButtonsIn();
        } else {
            colorPickerBox.setVisible(false);
            colorPickerBox.setManaged(false);
        }
        selectedColor = null;
    }

    private void animateColorButtonsIn() {
        colorPickerBox.setManaged(true);
        colorPickerBox.setVisible(true);

        SequentialTransition staggered = new SequentialTransition();
        int delay = 0;

        for (Node node : colorPickerBox.getChildren()) {
            node.setOpacity(0);
            TranslateTransition slide = new TranslateTransition(Duration.millis(150), node);
            slide.setFromY(-10);
            slide.setToY(0);

            FadeTransition fade = new FadeTransition(Duration.millis(150), node);
            fade.setFromValue(0);
            fade.setToValue(1);

            ParallelTransition combo = new ParallelTransition(slide, fade);
            combo.setDelay(Duration.millis(delay));
            delay += 50;

            staggered.getChildren().add(combo);
        }

        staggered.play();
    }

    private void onColorSelected(MouseEvent event) {
        Button source = (Button) event.getSource();
        selectedColor = toWebColor(source.getStyle());

        VBox currentDraftCard = createDraftNoteCard(selectedColor);
        notesContainer.getChildren().addFirst(currentDraftCard);

        colorPickerBox.setVisible(false);
        colorPickerBox.setManaged(false);
    }

    private VBox createDraftNoteCard(String color) {
        VBox draftBox = new VBox(12);
        draftBox.setPrefSize(220, 180);
        draftBox.setStyle("-fx-background-color: " + color +
                "; -fx-background-radius: 15; -fx-padding: 15;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0.4, 0, 2);");

        Label titleLabel = new Label("New Note");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333333;");

        TextArea draftInput = getTextArea();

        Button saveBtn = new Button("Save");
        stylePrimaryButton(saveBtn);

        Button cancelBtn = new Button("Cancel");
        styleSecondaryButton(cancelBtn);

        cancelBtn.setOnAction(_ -> notesContainer.getChildren().remove(draftBox));

        saveBtn.setOnAction(_ -> {
            String content = draftInput.getText().trim();
            if (!content.isEmpty()) {
                Note newNote = new Note(content, color, LocalDate.now());
                Database.insertNote(newNote);
                notesContainer.getChildren().remove(draftBox);
                addNoteToUI(newNote);
            }
        });

        HBox buttonBox = new HBox(8, cancelBtn, saveBtn);
        buttonBox.setStyle("-fx-alignment: center-right;");

        draftBox.getChildren().addAll(titleLabel, draftInput, buttonBox);

        ScaleTransition bounce = new ScaleTransition(Duration.millis(250), draftBox);
        bounce.setFromX(0.9);
        bounce.setFromY(0.9);
        bounce.setToX(1);
        bounce.setToY(1);
        bounce.setInterpolator(Interpolator.EASE_OUT);
        bounce.play();

        return draftBox;
    }

    private static TextArea getTextArea() {
        TextArea draftInput = new TextArea();
        draftInput.setWrapText(true);
        draftInput.setPromptText("Write a note...");
        draftInput.setStyle(""" 
            -fx-background-color: rgba(255,255,255,0.85);
            -fx-background-radius: 12;
            -fx-padding: 8;
            -fx-font-size: 13px;
            -fx-border-color: transparent;
            -fx-focus-color: transparent;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 3, 0.1, 0, 1);
        """);
        draftInput.setPrefRowCount(4);
        draftInput.setPrefWidth(170);
        return draftInput;
    }

    private void loadNotesFromDatabase() {
        allNotes.clear();
        notesContainer.getChildren().clear();
        for (Note note : Database.getAllNotes()) {
            addNoteToUI(note);
        }
    }

    private void addNoteToUI(Note note) {
        VBox noteBox = new VBox(10);
        noteBox.setPrefSize(220, 180);
        noteBox.setStyle("-fx-background-color: " + note.getColor() +
                "; -fx-background-radius: 15; -fx-padding: 15;" +
                "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0.3, 0, 1);");

        Text content = new Text(note.getContent());
        content.setWrappingWidth(190);
        content.setStyle("-fx-font-size: 14px;");

        HBox statusBox = new HBox(5);
        if (note.hasReminder()) {
            Label statusLabel = new Label("Status:");
            statusLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #444;");

            Text status = new Text(note.hasReminder() && !note.isReminderDone() ? "Pending" : "");
            status.setStyle("-fx-font-size: 10px; -fx-fill: #444;");

            statusBox.getChildren().addAll(statusLabel, status);
        }

        HBox reminderBox = null;
        if (note.hasReminder()) {
            reminderBox = new HBox(5);
            Label reminderLabel = new Label("Reminder:");
            reminderLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #444;");

            Text reminderInfo = new Text(note.getFormattedReminder());
            reminderInfo.setStyle("-fx-font-size: 10px; -fx-fill: #444;");

            reminderBox.getChildren().addAll(reminderLabel, reminderInfo);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox bottomBar = new HBox(6);
        bottomBar.setStyle("-fx-alignment: center-right;");

        Text date = new Text(note.getFormattedDate());
        date.setStyle("-fx-font-size: 10px; -fx-fill: #444;");

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        Button editBtn = new Button("✏");
        Button deleteBtn = new Button("🗑");
        Button reminderBtn = new Button("🔔");

        styleIconButton(editBtn);
        styleIconButton(deleteBtn);
        styleIconButton(reminderBtn);

        editBtn.setOnAction(_ -> showEditNote(noteBox, note));
        deleteBtn.setOnAction(_ -> {
            Database.deleteNote(note);
            notesContainer.getChildren().remove(noteBox);
            allNotes.remove(noteBox);
            applyFilters();
        });

        reminderBtn.setOnAction(_ -> setReminderForNote(note, note.getColor()));

        bottomBar.getChildren().addAll(date, hSpacer, editBtn, deleteBtn, reminderBtn);

        noteBox.getChildren().addAll(content);
        if (!statusBox.getChildren().isEmpty()) {
            noteBox.getChildren().add(statusBox);
        }
        if (reminderBox != null) {
            noteBox.getChildren().add(reminderBox);
        }
        noteBox.getChildren().addAll(spacer, bottomBar);

        noteBox.setUserData(note);

        noteBox.setOnMouseEntered(this::onNoteHoverEnter);
        noteBox.setOnMouseExited(this::onNoteHoverExit);

        ScaleTransition bounce = new ScaleTransition(Duration.millis(250), noteBox);
        bounce.setFromX(0.9);
        bounce.setFromY(0.9);
        bounce.setToX(1);
        bounce.setToY(1);
        bounce.setInterpolator(Interpolator.EASE_OUT);

        notesContainer.getChildren().addFirst(noteBox);
        allNotes.add(noteBox);
        bounce.play();
    }

    private void setReminderForNote(Note note, String noteColor) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Set Reminder");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: " + noteColor + ";" +
                "-fx-background-radius: 15;" +
                "-fx-border-radius: 15;");

        VBox contentBox = new VBox(10);
        contentBox.setPadding(new javafx.geometry.Insets(15));
        contentBox.setStyle("-fx-background-color: rgba(255,255,255,0.7);" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 15;");

        Label headerLabel = new Label("Set Reminder for Note");
        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label instructionLabel = new Label("Select date and time for your reminder:");
        instructionLabel.setStyle("-fx-font-size: 12px;");

        DateTimePicker dateTimePicker = new DateTimePicker();
        dateTimePicker.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        contentBox.getChildren().addAll(headerLabel, instructionLabel, dateTimePicker);
        dialogPane.setContent(contentBox);

        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);

        stylePrimaryButton(okButton);
        styleSecondaryButton(cancelButton);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                LocalDate reminderDate = dateTimePicker.getSelectedDate();
                LocalTime reminderTime = dateTimePicker.getSelectedTime();
                if (reminderDate != null && reminderTime != null) {
                    note.setReminder(reminderDate, reminderTime);
                    Database.updateNote(note);

                    LocalDateTime reminderDateTime = LocalDateTime.of(reminderDate, reminderTime);
                    scheduleNotification(reminderDateTime, note);

                    for (Node node : allNotes) {
                        if (node instanceof VBox vbox && vbox.getUserData() == note) {
                            notesContainer.getChildren().remove(vbox);
                            allNotes.remove(vbox);
                            break;
                        }
                    }
                    addNoteToUI(note);

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Reminder Set");
                    alert.setHeaderText(null);
                    alert.setContentText("Reminder set for " + note.getFormattedReminder());

                    DialogPane alertPane = alert.getDialogPane();
                    alertPane.setStyle("-fx-background-color: " + noteColor + ";" +
                            "-fx-background-radius: 15;" +
                            "-fx-border-radius: 15;");

                    Button alertButton = (Button) alertPane.lookupButton(ButtonType.OK);
                    stylePrimaryButton(alertButton);

                    alert.showAndWait();
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showEditNote(VBox noteBox, Note note) {
        TextArea editArea = new TextArea(note.getContent());
        editArea.setWrapText(true);
        editArea.setPrefRowCount(4);
        editArea.setStyle(""" 
            -fx-background-color: white;
            -fx-background-radius: 10;
            -fx-padding: 8;
            -fx-font-size: 13px;
            -fx-border-color: transparent;
            -fx-focus-color: transparent;
        """);

        Button saveEdit = new Button("Update");
        stylePrimaryButton(saveEdit);

        saveEdit.setOnAction(_ -> {
            String newContent = editArea.getText().trim();
            if (!newContent.isEmpty()) {
                note.setContent(newContent);
                Database.updateNote(note);
                notesContainer.getChildren().remove(noteBox);
                allNotes.remove(noteBox);
                addNoteToUI(note);
            }
        });

        VBox editBox = new VBox(10, editArea, new HBox(saveEdit));
        editBox.setStyle("-fx-alignment: center-right;");

        noteBox.getChildren().setAll(editBox);
    }

    private void applyFilters() {
        String selectedColorName = colorFilterComboBox.getValue();
        String selectedStatus = statusFilterComboBox.getValue();
        LocalDate selectedDate = dateFilterPicker.getValue();

        notesContainer.getChildren().clear();

        for (Node node : allNotes) {
            if (!(node instanceof VBox noteBox)) continue;

            boolean matches = true;

            if (!"All".equals(selectedColorName)) {
                String selectedColorHex = colorMap.getOrDefault(selectedColorName, "");
                matches &= noteBox.getStyle().contains(selectedColorHex);
            }

            if (!"All".equals(selectedStatus)) {
                boolean isPending = false;
                boolean isCompleted = false;
                for (Node child : noteBox.getChildren()) {
                    if (child instanceof Text statusText) {
                        isPending = statusText.getText().equals("Pending");
                        isCompleted = statusText.getText().equals("Completed");
                        break;
                    }
                }
                if ("Pending".equals(selectedStatus)) {
                    matches &= isPending;
                } else if ("Completed".equals(selectedStatus)) {
                    matches &= isCompleted;
                }
            }

            if (selectedDate != null) {
                boolean dateMatched = false;
                for (Node child : noteBox.getChildren()) {
                    if (child instanceof Text dateText && dateText.getStyle().contains("date")) {
                        String noteDate = dateText.getText();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
                        LocalDate noteLocalDate = LocalDate.parse(noteDate, formatter);
                        dateMatched = noteLocalDate.equals(selectedDate);
                        break;
                    }
                }
                matches &= dateMatched;
            }

            if (matches) {
                notesContainer.getChildren().add(node);
            }
        }
    }

    private String toWebColor(String buttonStyle) {
        return buttonStyle.split(":")[1].trim();
    }

    private void styleSecondaryButton(Button button) {
        button.setStyle(""" 
            -fx-background-color: rgba(120,120,120,0.2);
            -fx-text-fill: #555;
            -fx-font-weight: normal;
            -fx-padding: 8 16;
            -fx-border-radius: 12;
        """);
    }

    private void styleIconButton(Button button) {
        button.setStyle(""" 
            -fx-background-color: transparent;
            -fx-text-fill: #555;
            -fx-font-size: 18px;
            -fx-border-radius: 5;
            -fx-padding: 5;
        """);
    }

    private void stylePrimaryButton(Button button) {
        button.setStyle(""" 
            -fx-background-color: #5C6BC0;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-padding: 8 16;
            -fx-border-radius: 12;
        """);
    }

    private void onNoteHoverEnter(MouseEvent event) {
        Node noteBox = (Node) event.getSource();
        noteBox.setStyle(noteBox.getStyle() + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0.3, 0, 0);");
    }

    private void onNoteHoverExit(MouseEvent event) {
        Node noteBox = (Node) event.getSource();
        noteBox.setStyle(noteBox.getStyle().replace(" -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0.3, 0, 0);", ""));
    }

    public void setSearchButton(Button searchButton) {
        this.searchButton = searchButton;
    }

    @FXML
    protected void onBellClicked(ActionEvent actionEvent) {
        loadReminders(); // Load reminders from notes
        if (reminders.isEmpty()) {
            showNoRemindersMessage();
        } else {
            showReminderPopup(); // Show popup with reminders
        }
    }

    private void loadReminders() {
        reminders.clear();
        int reminderCount = 0; // Counter for reminders
        for (Node node : allNotes) {
            if (node instanceof VBox noteBox) {
                Note note = (Note) noteBox.getUserData();
                if (note != null && note.hasReminder()) {
                    reminders.add(note.getContent() + " - " + note.getFormattedReminder());
                    reminderCount++; // Increment counter
                }
            }
        }
        updateReminderCountLabel(reminderCount); // Update the label
    }

    private void updateReminderCountLabel(int count) {
        if (count > 0) {
            reminderCountLabel.setText(String.valueOf(count));
            reminderCountLabel.setVisible(true);
        } else {
            reminderCountLabel.setVisible(false);
        }
    }

    private void showNoRemindersMessage() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("No Reminders");
        alert.setHeaderText(null);
        alert.setContentText("No reminders have been set.");

        // Customize the alert's style
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #E0E0E0;"); // Light gray background
        dialogPane.setHeaderText("No Reminders");
        dialogPane.setContentText("No reminders have been set.");

        // Style the OK button
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(okButtonType);
        Button okButton = (Button) alert.getDialogPane().lookupButton(okButtonType);
        stylePrimaryButton(okButton);

        alert.showAndWait();
    }

    private void showReminderPopup() {
        Popup popup = new Popup();
        popup.setAutoHide(true); // Close when clicking outside

        VBox popupContent = getVBox();
        popupContent.setStyle("""
            -fx-background-color: white;
            -fx-padding: 10;
            -fx-border-color: #757575;
            -fx-border-width: 1;
            -fx-border-radius: 5;
        """);

        popup.getContent().add(popupContent);

        // Calculate position relative to the bell button
        Window window = bellButton.getScene().getWindow();
        double x = window.getX() + bellButton.localToScene(bellButton.getBoundsInLocal()).getMinX() + 40;
        double y = window.getY() + bellButton.localToScene(bellButton.getBoundsInLocal()).getMinY() + 70;

        popup.show(window, x, y);
    }

    private VBox getVBox() {
        ListView<String> listView = new ListView<>(reminders);
        listView.setPrefWidth(300);
        listView.setPrefHeight(200);

        // Styling for the ListView
        listView.setStyle("""
            -fx-background-color: white;
            -fx-border-color: #BDBDBD;
            -fx-border-width: 1;
            -fx-border-radius: 5;
            -fx-padding: 5;
        """);

        // Styling for ListView items (optional)
        listView.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-padding: 8;");
                }
            }
        });

        return new VBox(listView);
    }
}