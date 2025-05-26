package controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class LoadingScreenController {

    @FXML
    private Label statusLabel;

    @FXML
    private Text loadingText;

    @FXML
    private ImageView logoImage;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private Text versionText;

    private double progress = 0.0;

    @FXML
    public void initialize() {
        progressIndicator.setProgress(0);

        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(50), e -> {
            progress += 0.01;
            progressIndicator.setProgress(progress);

            if (progress < 0.3) {
                statusLabel.setText("Initializing modules...");
            } else if (progress < 0.6) {
                statusLabel.setText("Loading resources...");
            } else if (progress < 0.9) {
                statusLabel.setText("Preparing interface...");
            } else {
                statusLabel.setText("Finishing up...");
            }

            if (progress >= 1.0) {
                ((Timeline) e.getSource()).stop();
            }
        }));

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
}
