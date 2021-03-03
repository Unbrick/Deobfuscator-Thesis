package org.thesis.ui;

import com.google.common.collect.Lists;
import com.jfoenix.controls.*;
import com.sdklite.aapt.ApkFile;
import com.sdklite.aapt.Xml;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.jf.dexlib2.iface.ClassDef;
import org.thesis.Logger;
import org.thesis.dexprocessor.DexProcessor;
import org.thesis.dexprocessor.DexProcessorThread;
import org.thesis.ui.fxelements.FXHelper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;

public class MainAppController implements Initializable {
    @FXML
    public TreeView<String> treeView;
    @FXML
    public JFXButton btnOpenFile;
    @FXML
    public Label lblClassName;
    @FXML
    public JFXCheckBox cbMathDeobf;
    @FXML
    public JFXCheckBox cbBranchDeobf;
    @FXML
    public JFXCheckBox cbSwitchDeobf;
    @FXML
    public JFXCheckBox cbNopsDeobf;
    @FXML
    public JFXCheckBox cbRemoveAllTryBlocks;
    @FXML
    public JFXSpinner spinner;
    @FXML
    public JFXButton btnDeobfuscate;
    @FXML
    public JFXButton btnStatistics;
    @FXML
    public JFXButton btnSaveToFile;
    @FXML
    public Pane paneDragAndDrop;
    @FXML
    public Button btnClose;
    @FXML
    public Pane rootPane;
    @FXML
    public JFXTextArea logTextArea;
    @FXML
    public JFXToggleButton tgMathDeobfuscator;
    @FXML
    public JFXToggleButton tgSwitchUnsafe;
    @FXML
    public JFXToggleButton tgSwitchLunatic;
    @FXML
    public JFXToggleButton tgDarkMode;
    @FXML public Label lblPackage;
    @FXML public Label lblVersion;

    private Stage primaryStage = null;
    private double xOffset;
    private double yOffset;

    private DexProcessor deobfuscatorApp;
    private JFXSnackbar mSnackbar;
    private boolean isPackage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Logger.create(logTextArea);

        logTextArea.setFont(Font.font("Mono", FontWeight.LIGHT, 12));

        Logger.info("Logger", "Initialized logger");
        mSnackbar = new JFXSnackbar(rootPane);


        paneDragAndDrop.setOnMousePressed(event -> {
            xOffset = primaryStage.getX() - event.getScreenX();
            yOffset = primaryStage.getY() - event.getScreenY();
        });

        paneDragAndDrop.setOnMouseDragged(event -> {
            primaryStage.setX(event.getScreenX() + xOffset);
            primaryStage.setY(event.getScreenY() + yOffset);
        });

        treeView.addEventHandler(MOUSE_CLICKED, event -> {
            Node node = event.getPickResult().getIntersectedNode();
            // Accept clicks only on node cells, and not on empty spaces of the TreeView
            if (node instanceof Text || (node instanceof TreeCell && ((TreeCell) node).getText() != null)) {
                String displayName = "";
                TreeItem<String> mSelectedItem = treeView.getSelectionModel().getSelectedItem();
                mSelectedItem.setExpanded(!mSelectedItem.isExpanded());
                if (mSelectedItem.isLeaf()) {
                    String className = getClassNameFromTreeItem(mSelectedItem);
                    ClassDef selectedClass = deobfuscatorApp.getClassFromName(className);
                    if (selectedClass != null) {
                        displayName = selectedClass.getType().substring(1);
                        btnDeobfuscate.setDisable(false);
                        btnDeobfuscate.setText("Deobfuscate class");
                        isPackage = false;
                    } else {
                        showErrorMessage("Could not select class from tree model");
                    }
                } else {
                    displayName = deobfuscatorApp.getPackageFromName(mSelectedItem);
                    btnDeobfuscate.setText("Deobfuscate package");
                    isPackage = true;
                }
                btnDeobfuscate.setDisable(false);
                lblClassName.setText(displayName.replace("/", "."));
            }
        });

        logTextArea.setEditable(false);
        logTextArea.setStyle("-fx-display-caret: false;");

        installTooltips();
    }

    private void installTooltips() {
        Tooltip switchUnsafeTooltip = new Tooltip("Enable backtrace of const instructions for switch-case conditions. Will take more time and may omit code branches leading to wrong code.");
        switchUnsafeTooltip.setWrapText(true);
        switchUnsafeTooltip.setShowDelay(Duration.millis(300));
        tgSwitchUnsafe.setTooltip(switchUnsafeTooltip);

        Tooltip mathStrategyTooltip = new Tooltip("Deobfuscation strategy\r\nTry to defeat obfuscation with logic (faster) or with emulation (slower). Results are identical.");
        mathStrategyTooltip.setShowDelay(Duration.millis(300));

        tgMathDeobfuscator.setTooltip(mathStrategyTooltip);

        Tooltip mathDeobfuscatorTooltip = new Tooltip("Enable or disable deobfuscation of mathematical branch conditions");
        mathDeobfuscatorTooltip.setShowDelay(Duration.millis(300));
        cbMathDeobf.setTooltip(mathDeobfuscatorTooltip);

        Tooltip branchPathTooltip = new Tooltip("Enable or disable sanitisation of known branch paths");
        branchPathTooltip.setShowDelay(Duration.millis(300));
        cbBranchDeobf.setTooltip(branchPathTooltip);

        Tooltip switchCaseTooltip = new Tooltip("Enable or disable deobfuscation switch case conditions");
        switchCaseTooltip.setShowDelay(Duration.millis(300));
        cbSwitchDeobf.setTooltip(switchCaseTooltip);

        Tooltip nopTooltip = new Tooltip("Enable or disable removal of nop instructions (overwritten instruction)");
        nopTooltip.setShowDelay(Duration.millis(300));
        cbNopsDeobf.setTooltip(nopTooltip);

        Tooltip tryCatchTooltip = new Tooltip("Enable or disable the removal of try-catch block obfuscation");
        tryCatchTooltip.setShowDelay(Duration.millis(300));
        cbRemoveAllTryBlocks.setTooltip(tryCatchTooltip);

        Tooltip switchLunaticTooltip = new Tooltip("Backtrace of packed-switch conditions\r\n If no correct branch condition is found, it is assumed the branch is not taken and written as such.\r\nWill break your code, do not use unless you dont care about the integrity of your code!");
        switchLunaticTooltip.setShowDelay(Duration.millis(300));
        tgSwitchLunatic.setTooltip(switchLunaticTooltip);
    }

    public void setStage(Stage mStage) {
        this.primaryStage = mStage;
    }

    @FXML
    public void tgSwitchUnsafeHandle(ActionEvent event) {
        tgSwitchUnsafe.setText(tgSwitchUnsafe.isSelected() ? "Unsafe: Enabled" : "Unsafe: Disabled");
        if (tgSwitchUnsafe.isSelected()) {
            tgSwitchLunatic.setDisable(false);
        } else {
            tgSwitchLunatic.setSelected(false);
            tgSwitchLunatic.setDisable(true);
            tgSwitchLunatic.setText("Lunatic mode: Disabled");
        }
    }

    @FXML
    public void cbMathDeobfHandle(ActionEvent event) {
        tgMathDeobfuscator.setDisable(!cbMathDeobf.isSelected());
    }

    @FXML
    public void cbSwitchDeobfHandle(ActionEvent event) {
        tgSwitchUnsafe.setDisable(!cbSwitchDeobf.isSelected());
        if (!cbSwitchDeobf.isSelected()) {
            tgSwitchLunatic.setSelected(false);
            tgSwitchLunatic.setDisable(true);
            tgSwitchUnsafe.setSelected(false);

            tgSwitchUnsafe.setText("Unsafe: Disabled");
            tgSwitchLunatic.setText("Lunatic mode: Disabled");
        }
    }

    @FXML
    public void tgMathDeobfuscatorHandle(ActionEvent event) {
        tgMathDeobfuscator.setText(tgMathDeobfuscator.isSelected() ? "Deobfuscation strategy: Emulation" : "Deobfuscation strategy: Logic");
    }

    @FXML
    public void btnCloseHandle(ActionEvent event) {
        primaryStage.close();
        event.consume();
    }

    @FXML
    public void btnStatisticsHandle(ActionEvent event) {
        new StatisticsController(deobfuscatorApp.getStatistics());
        event.consume();
    }

    @FXML
    public void btnOpenFileHandle(ActionEvent event) throws IOException {
        File file = openFileChooser();
        if (file != null) {

            ApkFile apk = new ApkFile(file);
            Xml manifest = apk.getAndroidManifest();

            Xml.Element mainElement = manifest.getDocumentElement();
            String pkg = mainElement.getAttribute("package");
            String version = Lists.newArrayList(mainElement.attributes()).get(1).getValue();
            String versionCode = mainElement.getAttribute("versionCode");

            lblPackage.setText(pkg);
            lblVersion.setText(version + " (" + versionCode + ")");

            Alert mDialog = FXHelper.showInformationDialog("Loading dex file, please wait...", "Loading...");

            deobfuscatorApp = new DexProcessor();
            deobfuscatorApp.loadDexFile(file, new DexProcessorThread.DexLoadingListener() {
                @Override
                public void update(int current, int total) {
                    Platform.runLater(() -> mDialog.setContentText("Processing class " + current + " of " + total));
                }

                @Override
                public void loaded(TreeItem<String> mRootTreeItem) {
                    Platform.runLater(() -> {
                        treeView.setRoot(mRootTreeItem);
                        treeView.getRoot().setExpanded(true);
                        snackbar("Loaded classes...");
                        mDialog.setResult(ButtonType.CLOSE);
                        mDialog.close();
                    });
                }
            });

            mDialog.showAndWait();

        }
        event.consume();
    }

    @FXML
    public void btnSaveDexFileHandle(ActionEvent event) {
        File path = null;
        if (deobfuscatorApp.isFinished()) {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setInitialDirectory(new File("C:\\Users\\Admin\\Downloads\\"));
            path = directoryChooser.showDialog(primaryStage);
        }

        if (path != null)
            deobfuscatorApp.saveFile(path, filename -> snackbar("Wrote to directory " + filename));
        event.consume();
    }

    @FXML
    public void btnDeobfuscateHandle(ActionEvent event) {
        if (deobfuscatorApp != null) {
            boolean mathEnabled = cbMathDeobf.isSelected();
            boolean branchEnabled = cbBranchDeobf.isSelected();
            boolean switchCaseEnabled = cbSwitchDeobf.isSelected();
            boolean tryEnabled = cbRemoveAllTryBlocks.isSelected();
            boolean removeNops = cbNopsDeobf.isSelected();
            boolean mathLogic = !tgMathDeobfuscator.isSelected();
            boolean swichCaseUnsafe = tgSwitchUnsafe.isSelected();
            boolean switchCaseLunatic = tgSwitchLunatic.isSelected();
            if (isPackage) {
                deobfuscatorApp.deobfuscatePackage(mathEnabled, branchEnabled, switchCaseEnabled, tryEnabled, removeNops, mathLogic, swichCaseUnsafe, switchCaseLunatic);
            } else {
                deobfuscatorApp.deobfuscateClass(mathEnabled, branchEnabled, switchCaseEnabled, tryEnabled, removeNops, mathLogic, swichCaseUnsafe, switchCaseLunatic);
            }
            deobfuscatorApp.addListener(() -> {
                btnDeobfuscate.setVisible(true);
                btnStatistics.setDisable(false);
                btnSaveToFile.setDisable(false);
                spinner.setVisible(false);
                snackbar("Deobfuscator finished!");
            });
            btnDeobfuscate.setDisable(true);
            spinner.setVisible(true);
        }
        event.consume();
    }

    private void snackbar(String message) {
        mSnackbar.enqueue(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout(message), Duration.seconds(3), null));
    }

    private void showErrorMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.getDialogPane().getStylesheets().add(Style.getStyle());
        alert.showAndWait();
    }

    private String getClassNameFromTreeItem(TreeItem<String> oldValue) {
        StringBuilder className = new StringBuilder("L");
        // oldValue = oldValue.getParent();
        while (!oldValue.getParent().equals(treeView.getRoot())) {
            className.insert(1, "/");
            className.insert(1, oldValue.getValue());
            oldValue = oldValue.getParent();
        }
        className.insert(1, "/");
        className.insert(1, oldValue.getValue());
        className.deleteCharAt(className.length() - 1).append(";");
        return className.toString();
    }


    private File openFileChooser() {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File("C:\\Users\\Admin\\Downloads\\"));
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("*.apk / *.dex", "*.apk", "*.dex");
        fileChooser.getExtensionFilters().add(extFilter);
        return fileChooser.showOpenDialog(primaryStage);
    }

    public void tgSwitchLunaticHandle(ActionEvent event) {
        tgSwitchLunatic.setText(!tgSwitchLunatic.isSelected() ? "Lunatic mode: Disabled" : "Lunatic mode: Enabled");
    }

    public void tgDarkMode(ActionEvent event) {
        if (!tgDarkMode.isSelected()) {
            new Thread(() -> {
                try {
                    int delay = ThreadLocalRandom.current().nextInt(200, 751);
                    TimeUnit.MILLISECONDS.sleep(delay);
                    tgDarkMode.setSelected(true);
                    snackbar("Nope...");
                } catch (InterruptedException ignored) {}
            }).start();
        }
    }
}
