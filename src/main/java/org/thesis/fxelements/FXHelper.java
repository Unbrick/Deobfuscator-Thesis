package org.thesis.fxelements;

import javafx.scene.control.Alert;

public class FXHelper {

    public static Alert showInformationDialog(String message, String title) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.getButtonTypes().clear();

        return alert;
    }
}
