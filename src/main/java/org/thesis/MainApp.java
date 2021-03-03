package org.thesis;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.thesis.ui.MainAppController;
import org.thesis.ui.Style;


public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.initStyle(StageStyle.UNDECORATED);
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("ui/scene.fxml"));

        Parent root = loader.load();

        primaryStage.setTitle("SmaliVM");

        Scene mScene = new Scene(root, 1000, 700);

        mScene.getStylesheets().add(Style.getStyle());
        primaryStage.setScene(mScene);



        primaryStage.show();

        MainAppController mController = loader.getController();
        mController.setStage(primaryStage);

        primaryStage.setOnCloseRequest(event -> {
            try {
                stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
