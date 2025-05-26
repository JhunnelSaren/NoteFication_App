module com.example.noteifcation_app {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires java.sql;
    requires quartz;
    requires org.controlsfx.controls;
    requires javafx.media;


    exports com.example.notefication_app;
    exports controller;
    opens model to com.google.gson;
    exports com.example.anote2.db;
    opens com.example.anote2.db to javafx.fxml;
    opens controller to com.google.gson, javafx.fxml;
    opens service to com.google.gson;
    opens com.example.notefication_app to com.google.gson, javafx.fxml;

}
