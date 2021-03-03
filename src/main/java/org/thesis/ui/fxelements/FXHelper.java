package org.thesis.ui.fxelements;

import javafx.scene.control.Alert;
import javafx.stage.StageStyle;
import org.thesis.ui.Style;

public class FXHelper {

    public static Alert showInformationDialog(String message, String title) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initStyle(StageStyle.UNDECORATED);
        alert.getDialogPane().getStylesheets().add(Style.getStyle());
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.getButtonTypes().clear();

        return alert;
    }
}
