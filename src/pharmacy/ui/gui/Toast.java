package pharmacy.ui.gui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class Toast {

    public static void show(StackPane root, String message) {
        show(root, message, false);
    }

    public static void showError(StackPane root, String message) {
        show(root, message, true);
    }

    private static void show(StackPane root, String message, boolean error) {
        Label label = new Label(message);
        label.getStyleClass().add("toast-label");

        StackPane toast = new StackPane(label);
        toast.getStyleClass().add(error ? "toast-error-box" : "toast-box");
        toast.setMaxWidth(500);
        toast.setMaxHeight(44);
        toast.setOpacity(0);

        StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toast, new javafx.geometry.Insets(0, 0, 40, 0));

        root.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition pause = new PauseTransition(Duration.millis(1800));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> root.getChildren().remove(toast));

        new SequentialTransition(fadeIn, pause, fadeOut).play();
    }
}
