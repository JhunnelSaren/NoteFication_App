package com.example.notefication_app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;

import java.io.IOException;

@SuppressWarnings("CallToPrintStackTrace")
public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Load the loading screen
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("LoadingScreen.fxml"));
        Parent loadingScreen = loader.load();

        Scene loadingScene = new Scene(loadingScreen);
        stage.setScene(loadingScene);
        stage.setTitle("Loading...");
        stage.show();

        // Simulate loading process (replace with actual loading logic)
        new Thread(() -> {
            try {
                Thread.sleep(3000); // Simulate loading time
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // After loading, switch to the main application screen
            Platform.runLater(() -> showMainApp(stage));
        }).start();
    }

    private void showMainApp(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("Main.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            stage.setTitle("NOTEFICATION");
            stage.setScene(scene);
            stage.setMaximized(true); // Start in maximized mode
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}