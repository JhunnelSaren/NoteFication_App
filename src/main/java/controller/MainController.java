package controller;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.util.Duration;
import model.Note;
import com.example.anote2.db.Database;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MainController implements Initializable {

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

    private String selectedColor = null;
    private VBox currentDraftCard = null;
    private List<Node> allNotes = new ArrayList<>();

    private static final Map<String, String> colorMap = Map.of(
            "Yellow", "#FFD56A",
            "Red", "#F28B6C",
            "Purple", "#B199FF",
            "Cyan", "#00CFFF",
            "Green", "#E5FF99"
    );

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Database.initialize();
        loadNotesFromDatabase();

        addNoteButton.setOnAction(e -> toggleColorPicker());

        backToTopButton.setOnAction(e -> {
            Timeline scrollAnim = new Timeline();
            KeyValue kv = new KeyValue(notesScrollPane.vvalueProperty(), 0, Interpolator.EASE_BOTH);
            KeyFrame kf = new KeyFrame(Duration.millis(350), kv);
            scrollAnim.getKeyFrames().add(kf);
            scrollAnim.play();
        });

        for (Node node : colorPickerBox.getChildren()) {
            if (node instanceof Button colorButton) {
                colorButton.setOnMouseClicked(this::onColorSelected);
            }
        }

        notesScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
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
                fadeOut.setOnFinished(evt -> backToTopButton.setVisible(false));
                fadeOut.play();
            }
        });

        backToTopButton.setOnMouseEntered(e -> {
            backToTopButton.setScaleX(1.1);
            backToTopButton.setScaleY(1.1);
        });
        backToTopButton.setOnMouseExited(e -> {
            backToTopButton.setScaleX(1);
            backToTopButton.setScaleY(1);
        });

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
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

        colorFilterComboBox.getItems().setAll("All", "Yellow", "Red", "Purple", "Cyan", "Green");
        colorFilterComboBox.setValue("All");
        colorFilterComboBox.setOnAction(e -> applyFilters());

        statusFilterComboBox.getItems().setAll("All", "Completed", "Pending");
        statusFilterComboBox.setValue("All");
        statusFilterComboBox.setOnAction(e -> applyFilters());

        dateFilterPicker.setOnAction(e -> applyFilters());
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

        currentDraftCard = createDraftNoteCard(selectedColor);
        notesContainer.getChildren().add(0, currentDraftCard);

        colorPickerBox.setVisible(false);
        colorPickerBox.setManaged(false);
    }

    private VBox createDraftNoteCard(String color) {
        VBox draftBox = new VBox(12);
        draftBox.setPrefSize(220, 180);
        draftBox.setStyle("-fx-background-color: " + color +
                "; -fx-background-radius: 15; -fx-padding: 15;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0.3, 0, 1);");

        TextArea draftInput = new TextArea();
        draftInput.setWrapText(true);
        draftInput.setPromptText("Write a note...");
        draftInput.setStyle(""" 
            -fx-background-color: white;
            -fx-background-radius: 12;
            -fx-padding: 8;
            -fx-font-size: 13px;
            -fx-border-color: transparent;
            -fx-focus-color: transparent;
        """);
        draftInput.setPrefRowCount(4);
        draftInput.setPrefWidth(170);

        Button saveBtn = new Button("Save");
        stylePrimaryButton(saveBtn);

        saveBtn.setOnAction(e -> {
            String content = draftInput.getText().trim();
            if (!content.isEmpty()) {
                Note newNote = new Note(content, color, LocalDate.now());
                Database.insertNote(newNote);
                notesContainer.getChildren().remove(draftBox);
                addNoteToUI(newNote);
            }
        });

        HBox buttonBox = new HBox(saveBtn);
        buttonBox.setStyle("-fx-alignment: center-right;");

        draftBox.getChildren().addAll(draftInput, buttonBox);

        ScaleTransition bounce = new ScaleTransition(Duration.millis(250), draftBox);
        bounce.setFromX(0.9);
        bounce.setFromY(0.9);
        bounce.setToX(1);
        bounce.setToY(1);
        bounce.setInterpolator(Interpolator.EASE_OUT);
        bounce.play();

        return draftBox;
    }

    private void loadNotesFromDatabase() {
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

        styleIconButton(editBtn);
        styleIconButton(deleteBtn);

        editBtn.setOnAction(e -> showEditNote(noteBox, note));
        deleteBtn.setOnAction(e -> {
            Database.deleteNote(note);
            notesContainer.getChildren().remove(noteBox);
            allNotes.remove(noteBox);
            applyFilters();
        });

        bottomBar.getChildren().addAll(date, hSpacer, editBtn, deleteBtn);
        noteBox.getChildren().addAll(content, spacer, bottomBar);

        noteBox.setOnMouseEntered(this::onNoteHoverEnter);
        noteBox.setOnMouseExited(this::onNoteHoverExit);

        ScaleTransition bounce = new ScaleTransition(Duration.millis(250), noteBox);
        bounce.setFromX(0.9);
        bounce.setFromY(0.9);
        bounce.setToX(1);
        bounce.setToY(1);
        bounce.setInterpolator(Interpolator.EASE_OUT);

        notesContainer.getChildren().add(0, noteBox);
        allNotes.add(noteBox);
        bounce.play();
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

        saveEdit.setOnAction(e -> {
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

    // ✅ Updated applyFilters()
    private void applyFilters() {
        String selectedColorName = colorFilterComboBox.getValue();
        String selectedStatus = statusFilterComboBox.getValue();
        LocalDate selectedDate = dateFilterPicker.getValue();

        notesContainer.getChildren().clear();

        for (Node node : allNotes) {
            if (!(node instanceof VBox noteBox)) continue;

            boolean matches = true;

            // Filter by color
            if (!"All".equals(selectedColorName)) {
                String selectedColorHex = colorMap.getOrDefault(selectedColorName, "");
                matches &= noteBox.getStyle().contains(selectedColorHex);
            }

            // Filter by status
            if (!"All".equals(selectedStatus)) {
                boolean isCompleted = false;
                for (Node child : noteBox.getChildren()) {
                    if (child instanceof Text content) {
                        isCompleted = content.getText().contains("✔");
                        break;
                    }
                }
                matches &= "Completed".equals(selectedStatus) ? isCompleted : !isCompleted;
            }

            // Filter by date
            if (selectedDate != null) {
                boolean dateMatched = false;
                for (Node child : noteBox.lookupAll(".text")) {
                    if (child instanceof Text text && text.getStyle().contains("date")) {
                        String noteDate = text.getText();
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
}
