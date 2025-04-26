package com.example.notefication_app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("Main.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("NOTEFICATION");
        stage.setScene(scene);
        stage.setMaximized(true); // Start in maximized mode
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
