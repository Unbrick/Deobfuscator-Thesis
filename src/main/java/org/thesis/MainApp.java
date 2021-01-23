package org.thesis;

import com.jfoenix.controls.*;
import com.sun.javafx.collections.ObservableListWrapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import lanchon.multidexlib2.BasicDexFileNamer;
import lanchon.multidexlib2.MultiDexIO;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.thesis.dexprocessor.Deobfuscator;
import org.thesis.dexprocessor.DexProcessorThread;
import org.thesis.dexprocessor.statistics.SanitizerStatistics;
import org.thesis.fxelements.FXHelper;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;


@SuppressWarnings("unchecked")
public class MainApp extends Application {


    private TreeView<String> mTreeView;
    private Scene mScene;
    private DexFile mDexFile;
    private String className;
    private Pane rootPane;
    private Label mClassNameLabel;
    private JFXCheckBox cbMathDeobfuscator;
    private JFXCheckBox cbBranchSanitizer;
    private JFXCheckBox cbSwitchCaseSanitizer;
    private JFXCheckBox cbRemoveReplacedNops;
    private JFXCheckBox cbMultiplePasses;
    private JFXCheckBox cbRemoveAllTryBlocks;
    private Spinner<Integer> spDeobfuscationPasses;
    private JFXSnackbar mSnackbar;
    private JFXSpinner mSpinner;
    private Deobfuscator mDeobf;

    private JFXComboBox<SanitizerStatistics> comboBoxMethods;
    private Label lblMathUnsimplified;
    private Label lblMathSimplified;
    private Label lblOptimizedBranches;
    private Label lblOptimizedSwitch;
    private Label lblUnoptimizedSwitch;
    private Label lblRemovedNops;
    private Label lblOverallReducedInstructions;
    private JFXButton btnDeobfuscateAll;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("scene.fxml"));
        primaryStage.setTitle("SmaliVM");
        Scene mScene = new Scene(root, 1000, 700);
        mScene.getStylesheets().add(MainApp.class.getResource("styles.css").toExternalForm());
        primaryStage.setScene(mScene);
        primaryStage.show();

        mScene = primaryStage.getScene();

        rootPane = (Pane) mScene.lookup("#rootPane");

        mTreeView = (TreeView<String>) mScene.lookup("#treeView");
        mClassNameLabel = (Label) mScene.lookup("#lblClassName");
        cbMathDeobfuscator = (JFXCheckBox) mScene.lookup("#cbMathDeobf");
        cbBranchSanitizer = (JFXCheckBox) mScene.lookup("#cbBranchDeobf");
        cbSwitchCaseSanitizer = (JFXCheckBox) mScene.lookup("#cbSwitchDeobf");
        cbRemoveReplacedNops = (JFXCheckBox) mScene.lookup("#cbNopsDeobf");
        cbMultiplePasses = (JFXCheckBox) mScene.lookup("#cbMultiplePasses");
        cbRemoveAllTryBlocks = (JFXCheckBox) mScene.lookup("#cbRemoveAllTryBlocks");
        spDeobfuscationPasses = (Spinner) mScene.lookup("#spDeobfPasses");

        lblMathSimplified = (Label) mScene.lookup("#lblMathSimplified");
        lblMathUnsimplified = (Label) mScene.lookup("#lblMathUnsimplified");
        lblOptimizedBranches = (Label) mScene.lookup("#lblOptimizedBranches");
        lblOptimizedSwitch = (Label) mScene.lookup("#lblOptimizedSwitch");
        lblUnoptimizedSwitch = (Label) mScene.lookup("#lblUnoptimizedSwitch");
        lblRemovedNops = (Label) mScene.lookup("#lblRemovedNops");
        lblOverallReducedInstructions = (Label) mScene.lookup("#lblOverallReducedInstructions");

        comboBoxMethods = (JFXComboBox<SanitizerStatistics>) mScene.lookup("#comboboxMethods");
        comboBoxMethods.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                MainApp.this.lblMathSimplified.setText(String.valueOf(newValue.mMathStatistics.optimizedJumps));
                MainApp.this.lblMathUnsimplified.setText(String.valueOf(newValue.mMathStatistics.unoptimizedJumps));
                MainApp.this.lblOptimizedBranches.setText(String.valueOf(newValue.mBranchStatistics.optimizedBranches));
                MainApp.this.lblOptimizedSwitch.setText(String.valueOf(newValue.mSwitchCaseStatistics.optimizedSwitchCases));
                MainApp.this.lblUnoptimizedSwitch.setText(String.valueOf(newValue.mSwitchCaseStatistics.unoptimizedSwitchCases));
                MainApp.this.lblRemovedNops.setText(String.valueOf(newValue.mNopStatistics.replacedNops));
                if (newValue.instructionsAfterOptimisation > 0 && newValue.instructionsBeforeOptimisation > 0) {
                    double mPercentage = (float) newValue.instructionsAfterOptimisation / (float) newValue.instructionsBeforeOptimisation;
                    BigDecimal bd = new BigDecimal(Double.toString(100 - (mPercentage * 100)));
                    MainApp.this.lblOverallReducedInstructions.setText(bd.setScale(2, BigDecimal.ROUND_HALF_UP).toString() + "%");
                } else {
                    MainApp.this.lblOverallReducedInstructions.setText("0.00%");
                }

            }
        });

        mSpinner = (JFXSpinner) mScene.lookup("#spinner");

        cbMultiplePasses.selectedProperty().addListener((observable, oldValue, newValue) -> spDeobfuscationPasses.setDisable(!newValue));

        spDeobfuscationPasses.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 5, 2));

        mSnackbar = new JFXSnackbar(rootPane);

        final FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File("C:\\Users\\Admin\\Downloads\\"));
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("*.apk / *.dex", "*.apk", "*.dex");
        fileChooser.getExtensionFilters().add(extFilter);
        Button openFileButton = (Button) mScene.lookup("#openFileButton");
        openFileButton.setOnAction(e -> {
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                loadDexFile(file);
            }
        });

        Button btnSaveDexFile = (Button) mScene.lookup("#btnSaveToFile");
        btnSaveDexFile.setOnAction(event -> {
            if (mDeobf != null && !mDeobf.isAlive()) {
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setInitialDirectory(new File("C:\\Users\\Admin\\Downloads\\"));
                File selectedDirectory = directoryChooser.showDialog(primaryStage);
                mDeobf.writeToFile(selectedDirectory, thread -> snackbar("Wrote to directory " + selectedDirectory.getAbsolutePath()));
            }
        });

        Button btnDeobfuscateAll = (Button) mScene.lookup("#btnDeobfuscateAll");
        btnDeobfuscateAll.setOnAction(event -> {
            mDeobf = new Deobfuscator(mDexFile);
            mDeobf.setMathSanitizerEnabled(cbMathDeobfuscator.isSelected());
            mDeobf.setBranchSanitizerEnabled(cbBranchSanitizer.isSelected());
            mDeobf.setSwitchCaseSanitizerEnabled(cbSwitchCaseSanitizer.isSelected());
            mDeobf.setRemoveReplacedNops(cbRemoveReplacedNops.isSelected());
            mDeobf.setRemoveAllTryBlocks(cbRemoveAllTryBlocks.isSelected());
            mDeobf.addListener(thread -> {
                mSpinner.setVisible(false);
                snackbar("Deobfuscator finished!");
                updateStats(mDeobf.getStatistics());
            });
            mDeobf.start();
            mSpinner.setVisible(true);
        });

        Button btnDeobfuscate = (Button) mScene.lookup("#btnDeobfuscate");
        btnDeobfuscate.setOnAction(event -> {
            if (className != null) {
                mDeobf = new Deobfuscator(mDexFile, className, cbMultiplePasses.isSelected(), spDeobfuscationPasses.getValue());
                mDeobf.setMathSanitizerEnabled(cbMathDeobfuscator.isSelected());
                mDeobf.setBranchSanitizerEnabled(cbBranchSanitizer.isSelected());
                mDeobf.setSwitchCaseSanitizerEnabled(cbSwitchCaseSanitizer.isSelected());
                mDeobf.setRemoveReplacedNops(cbRemoveReplacedNops.isSelected());
                mDeobf.setRemoveAllTryBlocks(cbRemoveAllTryBlocks.isSelected());
                mDeobf.addListener(thread -> {
                    btnDeobfuscate.setVisible(true);
                    mSpinner.setVisible(false);
                    snackbar("Deobfuscator finished!");
                    updateStats(mDeobf.getStatistics());
                });
                mDeobf.start();
                btnDeobfuscate.setVisible(false);
                mSpinner.setVisible(true);
            }
        });

        mTreeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null && oldValue != newValue) {
                oldValue.setExpanded(false);
            }
            if (newValue != null) {
                if (newValue.getChildren().size() > 0) {
                    newValue.setExpanded(!newValue.isExpanded());
                } else {
                    className = getClassNameFromTreeItem(newValue);
                    Optional<? extends ClassDef> mClassDef = mDexFile.getClasses().stream().filter(x -> x.getType().equals(className)).findFirst();
                    if (mClassDef.isPresent()) {
                        btnDeobfuscate.setDisable(false);
                        String name = mClassDef.get().getType();
                        mClassNameLabel.setText(name.substring(name.lastIndexOf("/") + 1, name.length() - 1));
                    } else
                        showErrorMessage("Could not select class from tree model");
                }
            }
        });

        openFileButton.fire();

        char[] cArr;
        char c2;
        int i3 = 1;
        if (0 != 0) {
            cArr = new char[5];
        } else {
            cArr = new char[5];
            i3 = 0;
        }

    }


    private void updateStats(List<SanitizerStatistics> statistics) {
        Platform.runLater(() -> {
            ObservableList<SanitizerStatistics> mFxList = FXCollections.observableList(statistics);
            MainApp.this.comboBoxMethods.setItems(mFxList);
        });

    }

    public static void log(String module, String message) {
        System.out.println(module + ": " + message);
        System.out.flush();
    }

    private void loadDexFile(File file) {
        Alert mDialog = FXHelper.showInformationDialog("Loading dex file, please wait...", "Loading...");
        try {
            mDexFile = MultiDexIO.readDexFile(true, file, new BasicDexFileNamer(), null, null);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        DexProcessorThread mDexProcessor = new DexProcessorThread(mTreeView, file, mDialog);
        mDexProcessor.start();
        mDialog.showAndWait();
        mTreeView.getRoot().setExpanded(true);
        snackbar("Loaded " + mDexFile.getClasses().size() +" classes...");
    }

    private void snackbar(String message) {
        mSnackbar.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout(message), Duration.seconds(3), null));
    }

    private void showErrorMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.showAndWait();
    }

    private String getClassNameFromTreeItem(TreeItem<String> oldValue) {
        StringBuilder className = new StringBuilder("L");
        // oldValue = oldValue.getParent();
        while (!oldValue.getParent().equals(mTreeView.getRoot())) {
            className.insert(1, "/");
            className.insert(1, oldValue.getValue());
            oldValue = oldValue.getParent();
        }
        className.insert(1, "/");
        className.insert(1, oldValue.getValue());
        className.deleteCharAt(className.length() - 1).append(";");
        return className.toString();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
