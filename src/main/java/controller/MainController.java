package controller;

import com.example.anote2.db.Database;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Window;
import javafx.util.Duration;
import model.Note;
import service.NotificationService;

import java.io.File;
import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@SuppressWarnings("ALL")
public class MainController implements Initializable {

    public VBox noteContainer;
    public Button colorYellow;
    public Button colorGreen;
    public Button colorBlue;
    public Button colorPurple;
    public Button colorOrange;

    @FXML
    private FlowPane notesContainer;
    @FXML
    private Button addNoteButton;
    @FXML
    private VBox colorPickerBox;
    @FXML
    private ScrollPane notesScrollPane;
    @FXML
    private Button backToTopButton;
    @FXML
    private TextField searchField;
    @FXML
    private Button searchButton;
    @FXML
    private ComboBox<String> colorFilterComboBox;
    @FXML
    private ComboBox<String> statusFilterComboBox;
    @FXML
    private ComboBox<String> dateFilterComboBox;
    @FXML
    private Button bellButton;
    @FXML
    private Label reminderCountLabel;

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
    private final ObservableList<ReminderItem> reminders = FXCollections.observableArrayList();
    private final Map<Note, Boolean> notificationShown = new HashMap<>();

    private int notificationCount = 0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Database.initialize();
        Database.testInsertNote();
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
        startReminderCheckScheduler();
    }

    private void startReminderCheckScheduler() {
        scheduler.scheduleAtFixedRate(this::checkReminders, 0, 1, TimeUnit.MINUTES);
    }

    private void checkReminders() {
        Platform.runLater(() -> {
            for (Node node : allNotes) {
                if (node instanceof VBox noteBox) {
                    Note note = (Note) noteBox.getUserData();
                    if (note != null && note.hasReminder() && note.isReminderDone()) {
                        LocalDateTime reminderDateTime = LocalDateTime.of(note.getReminderDate(), note.getReminderTime());
                        if (reminderDateTime.isBefore(LocalDateTime.now())) {
                            note.setReminderDone(true);
                            note.setStatus("Completed");
                            Database.updateNote(note);
                            updateNoteInUI(note, noteBox);
                        }
                    }
                }
            }
        });
    }

    private void updateNoteInUI(Note note, VBox noteBox) {
        notesContainer.getChildren().remove(noteBox);
        allNotes.remove(noteBox);
        addNoteToUI(note);
    }

    private void scheduleNotification(LocalDateTime targetDateTime, Note note) {
        long delay = Date.from(targetDateTime.atZone(ZoneId.systemDefault()).toInstant()).getTime() - System.currentTimeMillis();

        if (delay > 0) {
            scheduler.schedule(() -> {
                Platform.runLater(() -> {
                    NotificationService.showNotification("Notification", "Time to check your note!");
                    playSound();
                    note.setStatus("Completed");
                    Database.updateNote(note);
                });
            }, delay, TimeUnit.MILLISECONDS);
        }
    }

    private void showNotificationForNote(Note note) {
        if (!notificationShown.containsKey(note) || !notificationShown.get(note)) {
            notificationShown.put(note, true);
            updateReminderCountLabel(getNotificationCount());
        }
    }

    private int getNotificationCount() {
        int count = 0;
        for (Node node : allNotes) {
            if (node instanceof VBox noteBox) {
                Note note = (Note) noteBox.getUserData();
                if (note != null && note.hasReminder() && notificationShown.getOrDefault(note, false)) {
                    count++;
                }
            }
        }
        return count;
    }

    private void playSound() {
        try {
            String soundFile = "src/main/resources/com/example/notefication_app/sound/alarm.wav";
            Media sound = new Media(new File(soundFile).toURI().toString());
            MediaPlayer mediaPlayer = new MediaPlayer(sound);
            mediaPlayer.setOnError(() -> System.err.println("MediaPlayer error: " + mediaPlayer.getError().getMessage()));
            mediaPlayer.play();

            notificationCount++;
            updateReminderCountLabel(notificationCount);
        } catch (Exception e) {
            System.err.println("Error playing sound: " + e.getMessage());
            e.printStackTrace();
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

        dateFilterComboBox.getItems().setAll("All", "Today", "Yesterday", "This Week", "This Month", "Older");
        dateFilterComboBox.setValue("All");
        dateFilterComboBox.setOnAction(_ -> applyFilters());
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
                LocalTime currentTime = LocalTime.now();
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
        notificationShown.clear();
        dateFilterComboBox.getItems().clear();

        Set<LocalDate> uniqueDates = new HashSet<>();

        for (Note note : Database.getAllNotes()) {
            addNoteToUI(note);
            uniqueDates.add(note.getDate());
        }

        updateReminderCountLabel(0);
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

            Text status = new Text(note.hasReminder() && note.isReminderDone() ? "Pending" : "Completed");
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

        LocalTime currentTime = LocalTime.now();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        String formattedTime = currentTime.format(timeFormatter);

        if (note.getCreationTime() !=null) {
            formattedTime = note.getCreationTime().format(String.valueOf(timeFormatter));
        } else {
            formattedTime = LocalTime.now().format(timeFormatter);
        }

        String dateTimeString = note.getFormattedDate() + " " + formattedTime;
        Text dateTimeText = new Text(dateTimeString);
        dateTimeText.setStyle("-fx-font-size: 9.5px; -fx-fill: #444;");

        Region hSpacer = new Region();
        bottomBar = new HBox(6); bottomBar.setHgrow(hSpacer, Priority.ALWAYS);

        Button editBtn = new Button("✏");
        Button deleteBtn = new Button("🗑");
        Button reminderBtn = new Button("🔔");

        styleIconButton(editBtn);
        styleIconButton(deleteBtn);
        styleIconButton(reminderBtn);

        editBtn.setStyle(editBtn.getStyle() + "-fx-font-size: 11px;");
        deleteBtn.setStyle(deleteBtn.getStyle() + "-fx-font-size: 12.5px;");
        reminderBtn.setStyle(reminderBtn.getStyle() + "-fx-font-size: 12.5px;");

        editBtn.setOnAction(_ -> showEditNote(noteBox, note));
        deleteBtn.setOnAction(_ -> {
            Database.deleteNote(note);
            notesContainer.getChildren().remove(noteBox);
            allNotes.remove(noteBox);
            applyFilters();
        });

        reminderBtn.setOnAction(_ -> setReminderForNote(note, note.getColor()));

        bottomBar.getChildren().addAll(dateTimeText, hSpacer, editBtn, deleteBtn, reminderBtn);

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
        dialogPane.setMinWidth(400); // Expanded width
        dialogPane.setMinHeight(320); // Expanded height
        dialogPane.setStyle("-fx-background-color: " + noteColor + ";" +
                "-fx-background-radius: 20;" +
                "-fx-border-radius: 20;" +
                "-fx-padding: 20;");

        VBox contentBox = new VBox(20); // Increased spacing
        contentBox.setPadding(new Insets(25));
        contentBox.setStyle("-fx-background-color: rgba(255,255,255,0.9);" +
                "-fx-background-radius: 15;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        Label headerLabel = new Label("🔔 Set Reminder for This Note");
        headerLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #222;");

        Label instructionLabel = new Label("Please select a date and time for your reminder:");
        instructionLabel.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #444;");

        // Date Picker
        DatePicker datePicker = new DatePicker();
        datePicker.setPrefWidth(260);
        datePicker.setStyle("-fx-font-size: 15px; -fx-background-radius: 8; -fx-padding: 8;");

        // Hour ComboBox (1–12)
        ComboBox<Integer> hourBox = new ComboBox<>();
        IntStream.rangeClosed(1, 12).forEach(hourBox.getItems()::add);
        hourBox.setValue(12);
        hourBox.setPrefWidth(80);
        hourBox.setStyle("-fx-font-size: 14px; -fx-background-radius: 8;");

        // Minute ComboBox (00–59)
        ComboBox<String> minuteBox = new ComboBox<>();
        IntStream.range(0, 60).forEach(min -> minuteBox.getItems().add(String.format("%02d", min)));
        minuteBox.setValue("00");
        minuteBox.setPrefWidth(80);
        minuteBox.setStyle("-fx-font-size: 14px; -fx-background-radius: 8;");

        // AM/PM ComboBox
        ComboBox<String> amPmBox = new ComboBox<>();
        amPmBox.getItems().addAll("AM", "PM");
        amPmBox.setValue("AM");
        amPmBox.setPrefWidth(80);
        amPmBox.setStyle("-fx-font-size: 14px; -fx-background-radius: 8;");

        HBox timeBox = new HBox(15, hourBox, minuteBox, amPmBox);
        timeBox.setAlignment(Pos.CENTER);
        timeBox.setPadding(new Insets(10, 0, 0, 0));

        VBox dateTimeBox = new VBox(15, datePicker, timeBox);
        dateTimeBox.setAlignment(Pos.CENTER);

        contentBox.getChildren().addAll(headerLabel, instructionLabel, dateTimeBox);
        contentBox.setAlignment(Pos.TOP_CENTER);

        dialogPane.setContent(contentBox);
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Style buttons
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        stylePrimaryButton(okButton);
        styleSecondaryButton(cancelButton);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                LocalDate reminderDate = datePicker.getValue();
                Integer hour = hourBox.getValue();
                String minuteStr = minuteBox.getValue();
                String amPm = amPmBox.getValue();

                if (reminderDate != null && hour != null && minuteStr != null && amPm != null) {
                    int minute = Integer.parseInt(minuteStr);
                    if (amPm.equals("PM") && hour != 12) hour += 12;
                    if (amPm.equals("AM") && hour == 12) hour = 0;

                    LocalTime reminderTime = LocalTime.of(hour, minute);
                    note.setReminder(reminderDate, reminderTime);
                    note.setStatus("Pending");
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
        String selectedDateFilter = dateFilterComboBox.getValue();

        notesContainer.getChildren().clear();

        for (Node node : allNotes) {
            if (!(node instanceof VBox noteBox)) continue;

            boolean matches = true;

            // Color filter
            if (!"All".equals(selectedColorName)) {
                String selectedColorHex = colorMap.getOrDefault(selectedColorName, "");
                matches &= noteBox.getStyle().contains(selectedColorHex);
            }

            // Status filter
            if (!"All".equals(selectedStatus)) {
                Note note = (Note) noteBox.getUserData();
                if (note != null) {
                    if (note.hasReminder()) {
                        matches &= note.getStatus().equals(selectedStatus);
                    } else {
                        matches = false;
                    }
                }
            }

            // Date filter
            Note note = (Note) noteBox.getUserData();
            if (note != null) {
                LocalDate noteDate = note.getDate();
                LocalDate today = LocalDate.now();

                switch (selectedDateFilter) {
                    case "Today":
                        matches &= noteDate.isEqual(today);
                        break;
                    case "Yesterday":
                        matches &= noteDate.isEqual(today.minusDays(1));
                        break;
                    case "This Week":
                        matches &= !noteDate.isBefore(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
                                && !noteDate.isAfter(today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
                        break;
                    case "This Month":
                        matches &= noteDate.getMonth() == today.getMonth() && noteDate.getYear() == today.getYear();
                        break;
                    case "Older":
                        matches &= noteDate.isBefore(today.minusDays(1));
                        break;
                    default: // "All"
                        break;
                }
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
                    -fx-background-color: #5C6BC0;
                    -fx-text-fill: White;
                    -fx-font-weight: Bold;
                    -fx-padding: 8 16;
                    -fx-border-radius: 12;
                """);
    }

    private void styleIconButton(Button button) {
        button.setStyle(""" 
                    -fx-background-color: transparent;
                    -fx-text-fill: #555;
                    -fx-font-size: 14px;
                    -fx-border-radius: 5;
                    -fx-padding: 5;
                    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0.5, 0, 1);
                """);

        button.setOnMouseEntered(e -> {
            button.setStyle(button.getStyle() + "-fx-text-fill: #000;");
        });

        button.setOnMouseExited(e -> {
            button.setStyle(button.getStyle().replace("-fx-text-fill: #000;", "-fx-text-fill: #555;"));
        });
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
        resetNotificationCount(); // Resets the count and updates the label
        loadReminders(); // Load reminders from notes
        if (reminders.isEmpty()) {
            showNoRemindersMessage();
        } else {
            showReminderPopup(); // Show popup with reminders
        }
    }

    private void resetNotificationCount() {
        notificationCount = 0; // Reset the count
        updateReminderCountLabel(notificationCount); // Update the label
    }

    private void loadReminders() {
        reminders.clear();
        for (Node node : allNotes) {
            if (node instanceof VBox noteBox) {
                Note note = (Note) noteBox.getUserData();
                if (note != null && note.hasReminder()) {
                    String content = note.getContent();
                    if (content.length() > 100) {
                        content = content.substring(0, 100) + "..."; // Truncate and add ellipsis
                    }
                    reminders.add(new ReminderItem(content + " - " + note.getFormattedReminder(), note.getColor(), note));
                }
            }
        }
    }

    private void updateReminderCountLabel(int count) {
        reminderCountLabel.setText(String.valueOf(count)); // Update with the current count
        reminderCountLabel.setVisible(count > 0); // Show label if count is greater than 0
    }

    private void showNoRemindersMessage() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("No Reminders");
        alert.setHeaderText(null);
        alert.setContentText("No reminders have been set.");

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #E0E0E0;"); // Light gray background

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(okButtonType);
        Button okButton = (Button) alert.getDialogPane().lookupButton(okButtonType);
        stylePrimaryButton(okButton);

        alert.showAndWait();
    }

    private void showReminderPopup() {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setConsumeAutoHidingEvents(false);

        VBox popupContent = new VBox(20);
        popupContent.setPadding(new Insets(28));
        popupContent.setPrefWidth(460);
        popupContent.setMinWidth(300);
        popupContent.setMaxWidth(460);
        popupContent.setStyle("""
        -fx-background-color: linear-gradient(to bottom right, #FFFFFF, #F8FAFF);
        -fx-background-radius: 18;
        -fx-border-radius: 18;
        -fx-border-color: #3F51B5;
        -fx-border-width: 1.8;
        -fx-effect: dropshadow(gaussian, rgba(63, 81, 181, 0.25), 16, 0.3, 0, 8);
    """);

        Label titleLabel = new Label("Notifications");
        titleLabel.setGraphic(createIconLabel("\uD83D\uDD14")); // Bell icon
        titleLabel.setContentDisplay(ContentDisplay.LEFT);
        titleLabel.setGraphicTextGap(12);
        titleLabel.setStyle("""
        -fx-font-size: 22px;
        -fx-font-weight: 700;
        -fx-text-fill: #3F51B5;
        -fx-font-family: "Segoe UI Semibold", "Arial", sans-serif;
        -fx-effect: dropshadow(one-pass-box, rgba(63,81,181,0.5), 1, 0.0, 0, 1);
    """);

        ListView<ReminderItem> listView = new ListView<>(reminders);
        listView.setPrefHeight(320);
        listView.setStyle("""
        -fx-background-color: transparent;
        -fx-border-color: transparent;
    """);

        VBox emptyPlaceholder = new VBox(10);
        emptyPlaceholder.setAlignment(Pos.CENTER);
        Label emptyIcon = createIconLabel("\u2705");
        emptyIcon.setStyle("-fx-font-size: 48px; -fx-text-fill: #B0BEC5;");
        Label emptyText = new Label("There's No Notifications Today!\nRelax and Enjoy your Day.");
        emptyText.setTextAlignment(TextAlignment.CENTER);
        emptyText.setStyle("""
        -fx-font-size: 14px;
        -fx-text-fill: #78909C;
        -fx-font-style: italic;
    """);
        emptyPlaceholder.getChildren().addAll(emptyIcon, emptyText);
        listView.setPlaceholder(emptyPlaceholder);

        listView.setCellFactory(lv -> new ListCell<>() {
            private final HBox card = new HBox(18);
            private final VBox textContainer = new VBox(4);
            private final Label reminderText = new Label();
            private final Label reminderTime = new Label();
            private final Button deleteButton = new Button();

            {
                card.setAlignment(Pos.CENTER_LEFT);
                card.setPadding(new Insets(14, 18, 14, 18));
                card.setStyle("""
                -fx-background-color: #E8ECFF;
                -fx-background-radius: 14;
                -fx-border-radius: 14;
                -fx-effect: dropshadow(gaussian, rgba(63, 81, 181, 0.15), 4, 0, 0, 1);
            """);
                card.setMaxWidth(Double.MAX_VALUE);

                reminderText.setWrapText(true);
                reminderText.setMaxWidth(280);
                reminderText.setStyle("""
                -fx-font-size: 16px;
                -fx-font-weight: 600;
                -fx-text-fill: #212121;
                -fx-font-family: "Segoe UI", "Arial", sans-serif;
            """);

                reminderTime.setStyle("""
                -fx-font-size: 12.5px;
                -fx-text-fill: #616161;
                -fx-font-family: "Segoe UI", "Arial", sans-serif;
            """);

                textContainer.getChildren().addAll(reminderText, reminderTime);

                Label trashIcon = new Label("🗑️");
                trashIcon.setStyle("""
                -fx-font-size: 17px;
                -fx-text-fill: #D32F2F;
                -fx-alignment: center;
                -fx-font-family: "Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji", sans-serif;
            """);
                trashIcon.setMouseTransparent(true);

                deleteButton.setGraphic(trashIcon);
                deleteButton.setStyle("""
                -fx-background-color: transparent;
                -fx-padding: 6px;
                -fx-border-radius: 50%;
                -fx-background-radius: 50%;
                -fx-cursor: hand;
            """);
                deleteButton.setTooltip(new Tooltip("Delete this reminder"));
                deleteButton.setFocusTraversable(false);

                deleteButton.setOnMouseEntered(e ->
                        trashIcon.setStyle("""
                    -fx-font-size: 17px;
                    -fx-text-fill: #B71C1C;
                    -fx-alignment: center;
                    -fx-font-family: "Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji", sans-serif;
                """)
                );
                deleteButton.setOnMouseExited(e ->
                        trashIcon.setStyle("""
                    -fx-font-size: 17px;
                    -fx-text-fill: #D32F2F;
                    -fx-alignment: center;
                    -fx-font-family: "Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji", sans-serif;
                """)
                );

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                card.getChildren().addAll(textContainer, spacer, deleteButton);
            }

            @Override
            protected void updateItem(ReminderItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    String cleanText = item.text.contains(" - ") ? item.text.split(" - ")[0] : item.text;
                    reminderText.setText(cleanText);
                    reminderTime.setText("⏰ " + item.note.getFormattedReminder());

                    deleteButton.setOnAction(e -> {
                        Note note = item.note;
                        if (note != null) {
                            note.setReminder(null, null);
                            Database.updateNote(note);
                            notificationShown.remove(note);
                            loadReminders();
                            updateReminderCountLabel(getNotificationCount());
                            listView.getItems().remove(item);
                        }
                    });

                    setGraphic(card);
                }
            }
        });

        popupContent.getChildren().addAll(titleLabel, listView);

        Window window = bellButton.getScene().getWindow();

        Point2D bellScreenPos = bellButton.localToScreen(0, 0);
        double x = bellScreenPos.getX() + bellButton.getWidth() - popupContent.getPrefWidth() - 12;
        double y = bellScreenPos.getY() + bellButton.getHeight() + 10;

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        x = Math.min(Math.max(screenBounds.getMinX() + 10, x), screenBounds.getMaxX() - popupContent.getPrefWidth() - 10);

        popupContent.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(280), popupContent);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        popup.getContent().add(popupContent);
        popup.show(window, x, y);
    }

    private Label createIconLabel(String icon) {
        Label label = new Label(icon);
        label.setStyle("-fx-font-size: 22px; -fx-text-fill: #3F51B5;");
        label.setAccessibleText("Icon: " + icon);
        return label;
    }



    private VBox getVBox() {
        ListView<ReminderItem> listView = new ListView<>(reminders);
        listView.setPrefWidth(380);
        listView.setFixedCellSize(70);
        listView.setPrefHeight(reminders.size() * 70 + 10);

        listView.setStyle("""
                -fx-background-color: transparent;
                -fx-border-color: transparent;
                -fx-border-width: 0;
                -fx-border-radius: 0;
                -fx-padding: 0;
        """);

        listView.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(ReminderItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(null);
                } else {
                    HBox cellContent = new HBox(10);
                    cellContent.setAlignment(Pos.CENTER_LEFT);
                    Text text = new Text(item.text);
                    text.setStyle("-fx-font-size: 14px;");

                    Button deleteButton = new Button("🗑️");
                    styleIconButton(deleteButton);
                    deleteButton.setOnAction(event -> {
                        Note note = item.note;
                        if (note != null) {
                            note.setReminder(null, null);
                            Database.updateNote(note);
                            notificationShown.remove(note);
                            loadReminders();
                            updateReminderCountLabel(getNotificationCount());
                            ((ListView<ReminderItem>) getListView()).getItems().remove(item);
                        }
                    });
                    cellContent.getChildren().addAll(text, deleteButton);
                    setGraphic(cellContent);
                    setStyle("-fx-padding: 8; -fx-background-color: " + item.color + "; -fx-text-fill: #333333; -fx-font-size: 16px;");
                }
            }
        });

        return new VBox(listView);
    }

    private static class ReminderItem {
        String text;
        String color;
        Note note;

        public ReminderItem(String text, String color, Note note) {
            this.text = text;
            this.color = color;
            this.note = note;
        }
    }

    public void saveApplicationState() {
        for (Node node : allNotes) {
            if (node instanceof VBox noteBox) {
                Note note = (Note) noteBox.getUserData();
                if (note != null) {
                    Database.updateNote(note);
                }
            }
        }
    }
}