package service;

import com.google.gson.GsonBuilder;
import org.controlsfx.control.Notifications;
import javafx.geometry.Pos;
import javafx.util.Duration;

public class NotificationService {

    private static GsonBuilder gsonBuilder; // Renamed to avoid conflict

    public static void showNotification(String title, String text) {
        Notifications.create()
                .title(title)
                .text(text)
                .position(Pos.BOTTOM_RIGHT)
                .hideAfter(Duration.seconds(5))
                .showInformation();
    }

    public static GsonBuilder getGsonBuilder() {
        return gsonBuilder;
    }

    public static void setGsonBuilder(GsonBuilder gsonBuilder) {
        NotificationService.gsonBuilder = gsonBuilder;
    }
}