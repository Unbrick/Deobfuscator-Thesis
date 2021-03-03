package org.thesis.ui;

import com.jfoenix.controls.JFXSnackbar;
import com.jfoenix.controls.JFXSnackbarLayout;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.thesis.dexprocessor.statistics.SanitizerStatistics;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class StatisticsController implements Initializable {

    private JFXSnackbar mSnackbar = null;
    @FXML public TableColumn<SanitizerStatistics, String> method;
    @FXML public TableColumn<SanitizerStatistics, String> instrBefore;
    @FXML public TableColumn<SanitizerStatistics, String> instrAfter;
    @FXML public TableColumn<SanitizerStatistics, String> mathOptimized;
    @FXML public TableColumn<SanitizerStatistics, String> mathUnoptimized;
    @FXML public TableColumn<SanitizerStatistics, String> mathTime;
    @FXML public TableColumn<SanitizerStatistics, String> branchOptimized;
    @FXML public TableColumn<SanitizerStatistics, String> branchUnoptimized;
    @FXML public TableColumn<SanitizerStatistics, String> branchTime;
    @FXML public TableColumn<SanitizerStatistics, String> swCaseOptimized;
    @FXML public TableColumn<SanitizerStatistics, String> swCaseUnoptimized;
    @FXML public TableColumn<SanitizerStatistics, String> swCaseTime;
    @FXML public TableColumn<SanitizerStatistics, String> tryOptimized;
    @FXML public TableColumn<SanitizerStatistics, String> tryUnoptimized;
    @FXML public TableColumn<SanitizerStatistics, String> tryTime;
    @FXML public TableColumn<SanitizerStatistics, String> nopRemoved;
    @FXML public TableColumn<SanitizerStatistics, String> nopTime;
    @FXML public TableView<SanitizerStatistics> statisticsTable;
    @FXML public Button btnClose;
    @FXML public Button btnCpyCsv;
    @FXML public AnchorPane rootPane;
    private List<SanitizerStatistics> statistics;
    private Stage stage = null;
    private double yOffset;
    private double xOffset;

    public StatisticsController(List<SanitizerStatistics> statistics) {
        this.statistics = statistics;
        Parent root;
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Objects.requireNonNull(getClass().getResource("statistics.fxml")));
            loader.setControllerFactory(e -> this);

            root = loader.load();

            mSnackbar = new JFXSnackbar(rootPane);


            stage = new Stage();
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setTitle("Statistics");
            Scene mScene = new Scene(root, 1210, 530);
            stage.setScene(mScene);
            stage.show();

            mScene.getStylesheets().add(Style.getStyle());

            rootPane.setOnMousePressed(event -> {
                xOffset = stage.getX() - event.getScreenX();
                yOffset = stage.getY() - event.getScreenY();
            });

            rootPane.setOnMouseDragged(event -> {
                stage.setX(event.getScreenX() + xOffset);
                stage.setY(event.getScreenY() + yOffset);
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void snackbar(String message) {
        mSnackbar.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout(message), Duration.seconds(3), null));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        method.setCellValueFactory(new PropertyValueFactory<>("Method"));
        instrBefore.setCellValueFactory(new PropertyValueFactory<>("InstrBefore"));
        instrAfter.setCellValueFactory(new PropertyValueFactory<>("InstrAfter"));
        mathOptimized.setCellValueFactory(new PropertyValueFactory<>("MathOptimized"));
        mathUnoptimized.setCellValueFactory(new PropertyValueFactory<>("MathUnoptimized"));
        mathTime.setCellValueFactory(new PropertyValueFactory<>("MathTime"));
        branchOptimized.setCellValueFactory(new PropertyValueFactory<>("BranchOptimized"));
        branchUnoptimized.setCellValueFactory(new PropertyValueFactory<>("BranchUnoptimized"));
        branchTime.setCellValueFactory(new PropertyValueFactory<>("BranchTime"));
        swCaseOptimized.setCellValueFactory(new PropertyValueFactory<>("SwCaseOptimized"));
        swCaseUnoptimized.setCellValueFactory(new PropertyValueFactory<>("SwCaseUnoptimized"));
        swCaseTime.setCellValueFactory(new PropertyValueFactory<>("SwCaseTime"));
        tryOptimized.setCellValueFactory(new PropertyValueFactory<>("TryOptimized"));
        tryUnoptimized.setCellValueFactory(new PropertyValueFactory<>("TryUnoptimized"));
        tryTime.setCellValueFactory(new PropertyValueFactory<>("TryTime"));
        nopRemoved.setCellValueFactory(new PropertyValueFactory<>("NopsRemoved"));
        nopTime.setCellValueFactory(new PropertyValueFactory<>("NopsTime"));

        statisticsTable.setItems(FXCollections.observableList(statistics));

        btnClose.setOnAction(event -> stage.close());

        btnCpyCsv.setOnAction(event -> {
            // a little hacky but works!
            statisticsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            statisticsTable.getSelectionModel().selectAll();
            copySelectionToClipboard(statisticsTable);
            statisticsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            statisticsTable.getSelectionModel().select(null);
            snackbar("Copied to clipboard!");
        });
    }

    public void copySelectionToClipboard(final TableView<SanitizerStatistics> table) {
        final Set<Integer> rows = new TreeSet<>();
        for (final TablePosition tablePosition : table.getSelectionModel().getSelectedCells()) {
            rows.add(tablePosition.getRow());
        }
        final StringBuilder strb = new StringBuilder();
        table.getColumns().forEach(sanitizerStatisticsTableColumn -> strb.append(sanitizerStatisticsTableColumn.getText().replace("\r", "").replace("\n", "")).append("\t"));
        for (Integer row : rows) {
            strb.append("\n");
            boolean firstCol = true;
            for (final TableColumn<?, ?> column : table.getColumns()) {
                if (!firstCol) {
                    strb.append('\t');
                }
                firstCol = false;
                final Object cellData = column.getCellData(row);
                strb.append(cellData == null ? "" : cellData.toString());
            }
        }
        final ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(strb.toString());
        Clipboard.getSystemClipboard().setContent(clipboardContent);
    }
}
