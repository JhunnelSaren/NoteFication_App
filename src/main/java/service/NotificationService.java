package service;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NotificationService {

    private static final List<Popup> activeNotifications = new ArrayList<>();
    private static final double NOTIFICATION_WIDTH = 350;
    private static final double NOTIFICATION_HEIGHT = 120;
    private static final double NOTIFICATION_SPACING = 15;

    public static void showNotification(String title, String text) {
        Platform.runLater(() -> createStyledNotification(title, text));
    }

    private static void createStyledNotification(String title, String text) {
        // Create popup window
        Popup popup = new Popup();
        popup.setAutoHide(false);
        popup.setConsumeAutoHidingEvents(false);

        // Main container
        VBox mainContainer = new VBox();
        mainContainer.setPrefSize(NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT);
        mainContainer.setMaxSize(NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT);
        mainContainer.setMinSize(NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT);

        // Create gradient background
        VBox contentContainer = getVBox();

        // Icon and title container
        HBox headerContainer = new HBox(12);
        headerContainer.setAlignment(Pos.CENTER_LEFT);

        // Bell icon with styling
        Label iconLabel = new Label("🔔");
        iconLabel.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-text-fill: white;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 3, 0.5, 0, 1);"
        );

        // Animated pulse effect for icon
        ScaleTransition iconPulse = new ScaleTransition(Duration.millis(800), iconLabel);
        iconPulse.setFromX(1.0);
        iconPulse.setFromY(1.0);
        iconPulse.setToX(1.15);
        iconPulse.setToY(1.15);
        iconPulse.setCycleCount(2);
        iconPulse.setAutoReverse(true);
        iconPulse.play();

        // Title label
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-font-size: 18px;" +
                        "-fx-font-weight: 700;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-family: 'Segoe UI', 'Helvetica Neue', 'Arial', sans-serif;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 2, 0.5, 0, 1);"
        );

        headerContainer.getChildren().addAll(iconLabel, titleLabel);

        // Message text
        Label messageLabel = new Label(text);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(NOTIFICATION_WIDTH - 48);
        messageLabel.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-text-fill: rgba(255, 255, 255, 0.95);" +
                        "-fx-font-family: 'Segoe UI', 'Helvetica Neue', 'Arial', sans-serif;" +
                        "-fx-line-spacing: 2px;"
        );

        // Progress bar indicator (optional visual element)
        Rectangle progressBar = new Rectangle();
        progressBar.setWidth(0);
        progressBar.setHeight(3);
        progressBar.setFill(Color.WHITE);
        progressBar.setOpacity(0.8);

        // Animate progress bar
        Timeline progressAnimation = new Timeline();
        KeyValue progressKeyValue = new KeyValue(progressBar.widthProperty(), NOTIFICATION_WIDTH - 48);
        KeyFrame progressKeyFrame = new KeyFrame(Duration.seconds(5), progressKeyValue);
        progressAnimation.getKeyFrames().add(progressKeyFrame);

        // Add all elements to content container
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        contentContainer.getChildren().addAll(
                headerContainer,
                messageLabel,
                spacer,
                progressBar
        );

        mainContainer.getChildren().add(contentContainer);
        popup.getContent().add(mainContainer);

        // Position notification - Modified to show in bottom-right area
        positionNotificationBottomRight(popup);

        // Entry animation
        mainContainer.setTranslateX(400);
        mainContainer.setOpacity(0);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(350), mainContainer);
        slideIn.setFromX(400);
        slideIn.setToX(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(350), mainContainer);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ParallelTransition entryAnimation = new ParallelTransition(slideIn, fadeIn);

        // Show notification
        Window window = getPrimaryWindow();
        // Fallback if no window available
        popup.show(Objects.requireNonNullElseGet(window, Stage::new));
        activeNotifications.add(popup);

        // Start animations
        entryAnimation.play();
        progressAnimation.play();

        // Auto-hide after 5 seconds
        Timeline autoHide = new Timeline();
        autoHide.getKeyFrames().add(new KeyFrame(Duration.seconds(5), _ -> hideNotification(popup, mainContainer)));
        autoHide.play();

        // Click to dismiss
        mainContainer.setOnMouseClicked(_ -> hideNotification(popup, mainContainer));

        // Hover effects
        mainContainer.setOnMouseEntered(_ -> {
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(100), mainContainer);
            scaleUp.setToX(1.02);
            scaleUp.setToY(1.02);
            scaleUp.play();

            progressAnimation.pause();
        });

        mainContainer.setOnMouseExited(_ -> {
            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(100), mainContainer);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);
            scaleDown.play();

            progressAnimation.play();
        });
    }

    private static VBox getVBox() {
        VBox contentContainer = new VBox(8);
        contentContainer.setPadding(new Insets(20, 24, 20, 24));
        contentContainer.setAlignment(Pos.TOP_LEFT);
        contentContainer.setPrefSize(NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT);

        // Modern gradient styling
        contentContainer.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #667eea 0%, #764ba2 100%);" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-color: rgba(255, 255, 255, 0.2);" +
                        "-fx-border-width: 1.5;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.25), 20, 0.3, 0, 8);"
        );
        return contentContainer;
    }

    private static void hideNotification(Popup popup, VBox container) {
        if (!activeNotifications.contains(popup)) return;

        // Exit animation
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), container);
        slideOut.setToX(400);
        slideOut.setInterpolator(Interpolator.EASE_IN);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), container);
        fadeOut.setToValue(0);

        ParallelTransition exitAnimation = new ParallelTransition(slideOut, fadeOut);
        exitAnimation.setOnFinished(_ -> {
            popup.hide();
            activeNotifications.remove(popup);
            repositionNotifications(); // Re-enable repositioning to maintain proper stacking
        });

        exitAnimation.play();
    }

    // Position notifications exactly like ControlsFX Pos.BOTTOM_RIGHT
    private static void positionNotificationBottomRight(Popup popup) {
        Window window = getPrimaryWindow();
        Screen screen = (window != null) ? Screen.getScreensForRectangle(window.getX(), window.getY(), window.getWidth(), window.getHeight()).getFirst()
                : Screen.getPrimary();

        double screenWidth = screen.getVisualBounds().getWidth();
        double screenHeight = screen.getVisualBounds().getHeight();
        double screenMinX = screen.getVisualBounds().getMinX();
        double screenMinY = screen.getVisualBounds().getMinY();

        // Position in bottom-right corner - new notifications appear at the bottom
        double x = screenMinX + screenWidth - NOTIFICATION_WIDTH - 20;
        double y = screenMinY + screenHeight - NOTIFICATION_HEIGHT - 20;

        popup.setX(x);
        popup.setY(y);

        // Move existing notifications up to make room for the new one
        moveExistingNotificationsUp();
    }

    // Move all existing notifications up when a new one is added
    private static void moveExistingNotificationsUp() {
        Window window = getPrimaryWindow();
        Screen screen = (window != null) ? Screen.getScreensForRectangle(window.getX(), window.getY(), window.getWidth(), window.getHeight()).getFirst()
                : Screen.getPrimary();

        double screenWidth = screen.getVisualBounds().getWidth();
        double screenMinX = screen.getVisualBounds().getMinX();

        // Move each existing notification up by one position (except the newest one we just added)
        for (int i = 0; i < activeNotifications.size() - 1; i++) {
            Timeline moveUpAnim = getTimeline(i, screenMinX, screenWidth);
            moveUpAnim.play();
        }
    }

    private static Timeline getTimeline(int i, double screenMinX, double screenWidth) {
        Popup popup = activeNotifications.get(i);
        double targetX = screenMinX + screenWidth - NOTIFICATION_WIDTH - 20;
        double currentY = popup.getY();
        double targetY = currentY - (NOTIFICATION_HEIGHT + NOTIFICATION_SPACING);

        // Smooth transition upward
        return new Timeline(
                new KeyFrame(Duration.millis(200), _ -> {
                    popup.setX(targetX);
                    popup.setY(targetY);
                })
        );
    }

    // Reposition all notifications when one is removed (move them down)
    private static void repositionNotifications() {
        Window window = getPrimaryWindow();
        Screen screen = (window != null) ? Screen.getScreensForRectangle(window.getX(), window.getY(), window.getWidth(), window.getHeight()).getFirst()
                : Screen.getPrimary();

        double screenWidth = screen.getVisualBounds().getWidth();
        double screenHeight = screen.getVisualBounds().getHeight();
        double screenMinX = screen.getVisualBounds().getMinX();
        double screenMinY = screen.getVisualBounds().getMinY();

        // Reposition from bottom to top (bottom-right behavior)
        for (int i = 0; i < activeNotifications.size(); i++) {
            Popup popup = activeNotifications.get(i);
            double targetX = screenMinX + screenWidth - NOTIFICATION_WIDTH - 20;
            // Bottom notification is at index 0, stack upward from there
            double targetY = screenMinY + screenHeight - NOTIFICATION_HEIGHT - 20 - (i * (NOTIFICATION_HEIGHT + NOTIFICATION_SPACING));

            // Smooth transition to new position
            Timeline repositionAnim = new Timeline(
                    new KeyFrame(Duration.millis(200), _ -> {
                        popup.setX(targetX);
                        popup.setY(targetY);
                    })
            );
            repositionAnim.play();
        }
    }

    private static Window getPrimaryWindow() {
        // Try to get any available window that's currently showing
        return Stage.getWindows().stream()
                .filter(Window::isShowing)
                .findFirst()
                .orElse(null);
    }

    // Alternative method with custom styling options
    public static void showCustomNotification(String title, String text, String color) {
        Platform.runLater(() -> createCustomStyledNotification(title, text, color));
    }

    private static void createCustomStyledNotification(String title, String text, String colorScheme) {
        // Similar to above but with custom color scheme
        Popup popup = new Popup();
        popup.setAutoHide(false);

        VBox mainContainer = new VBox();
        mainContainer.setPrefSize(NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT);

        VBox contentContainer = getVBox(colorScheme);

        // Rest of the implementation similar to above...
        HBox headerContainer = new HBox(12);
        headerContainer.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("🔔");
        iconLabel.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-text-fill: white;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 3, 0.5, 0, 1);"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-font-size: 18px;" +
                        "-fx-font-weight: 700;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-family: 'Segoe UI', 'Helvetica Neue', 'Arial', sans-serif;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 2, 0.5, 0, 1);"
        );

        headerContainer.getChildren().addAll(iconLabel, titleLabel);

        Label messageLabel = new Label(text);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(NOTIFICATION_WIDTH - 48);
        messageLabel.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-text-fill: rgba(255, 255, 255, 0.95);" +
                        "-fx-font-family: 'Segoe UI', 'Helvetica Neue', 'Arial', sans-serif;"
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        contentContainer.getChildren().addAll(headerContainer, messageLabel, spacer);
        mainContainer.getChildren().add(contentContainer);
        popup.getContent().add(mainContainer);

        positionNotificationBottomRight(popup);

        // Entry animation
        mainContainer.setTranslateX(400);
        mainContainer.setOpacity(0);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(350), mainContainer);
        slideIn.setFromX(400);
        slideIn.setToX(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(350), mainContainer);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ParallelTransition entryAnimation = new ParallelTransition(slideIn, fadeIn);

        popup.show(getPrimaryWindow());
        activeNotifications.add(popup);
        entryAnimation.play();

        Timeline autoHide = new Timeline();
        autoHide.getKeyFrames().add(new KeyFrame(Duration.seconds(4), _ -> hideNotification(popup, mainContainer)));
        autoHide.play();

        mainContainer.setOnMouseClicked(_ -> hideNotification(popup, mainContainer));
    }

    private static VBox getVBox(String colorScheme) {
        VBox contentContainer = new VBox(8);
        contentContainer.setPadding(new Insets(20, 24, 20, 24));
        contentContainer.setAlignment(Pos.TOP_LEFT);
        contentContainer.setPrefSize(NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT);

        // Custom color schemes
        String backgroundGradient = switch (colorScheme.toLowerCase()) {
            case "success" -> "linear-gradient(to bottom right, #11998e 0%, #38ef7d 100%)";
            case "warning" -> "linear-gradient(to bottom right, #f093fb 0%, #f5576c 100%)";
            case "info" -> "linear-gradient(to bottom right, #4facfe 0%, #00f2fe 100%)";
            default -> "linear-gradient(to bottom right, #667eea 0%, #764ba2 100%)";
        };

        contentContainer.setStyle(
                "-fx-background-color: " + backgroundGradient + ";" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-color: rgba(255, 255, 255, 0.2);" +
                        "-fx-border-width: 1.5;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.25), 20, 0.3, 0, 8);"
        );
        return contentContainer;
    }

    // Method to clear all notifications
    public static void clearAllNotifications() {
        for (Popup popup : new ArrayList<>(activeNotifications)) {
            popup.hide();
        }
        activeNotifications.clear();
    }
}