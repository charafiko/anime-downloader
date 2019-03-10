package org.docheinstein.animedownloader.ui.settings;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.docheinstein.animedownloader.ui.base.InstantiableController;
import org.docheinstein.commons.file.FileUtil;
import org.docheinstein.commons.logger.DocLogger;
import org.docheinstein.animedownloader.settings.Settings;
import org.docheinstein.commons.types.StringUtil;

import java.io.File;

/**
 * Controller for the settings window.
 */
public class SettingsWindowController implements InstantiableController {

    private static final DocLogger L =
        DocLogger.createForClass(SettingsWindowController.class);

    @FXML
    private Node uiRoot;


    @FXML
    private Button uiDownloadFolderButton;

    @FXML
    private Label uiDownloadFolder;


    @FXML
    private CheckBox uiRemoveAfterDownload;


    @FXML
    private CheckBox uiDownloadAutomatically;


    @FXML
    private Spinner<Integer> uiSimultaneousVideo;


    @FXML
    private CheckBox uiSimultaneousVideoForEachProvider;


    @FXML
    private Button uiChromeDriverButton;

    @FXML
    private Label uiChromeDriver;


    @FXML
    private CheckBox uiChromeDriverGhostMode;


    @FXML
    private Button uiFFmpegButton;

    @FXML
    private Label uiFFmpeg;


    @FXML
    private CheckBox uiLogging;

    @FXML
    private CheckBox uiFlush;

    @FXML
    private Button uiCancel;

    @FXML
    private Button uiOk;

    @Override
    public String getFXMLAsset() {
        return "settings_window.fxml";
    }

    @FXML
    public void initialize() {
        uiCancel.setOnMouseClicked(event -> closeWindow());

        uiOk.setOnMouseClicked(event -> {
            commitSettings();
            closeWindow();
        });

        uiDownloadFolderButton.setOnMouseClicked(event -> {
            openDownloadDirectoryChooser();
            L.debug("Change folder button clicked, opening directory chooser");
        });

        uiChromeDriverButton.setOnMouseClicked(event -> {
            openChromeDriverFileChooser();
            L.debug("Change folder button clicked, opening directory chooser");
        });

        uiFFmpegButton.setOnMouseClicked(event -> {
            openFFmpegFileChooser();
            L.debug("Change folder button clicked, opening directory chooser");
        });

        uiSimultaneousVideo.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10));

        setCurrentDownloadFolderValue(
            Settings.instance().getDownloadFolderSetting().getValue().getAbsolutePath());
        setCurrentRemoveAfterDownloadValue(
            Settings.instance().getRemoveAfterDownloadSetting().getValue());
        setCurrentDownloadAutomaticallyValue(
            Settings.instance().getDownloadAutomaticallySetting().getValue());
        setSimultaneousVideoLimitValue(
            Settings.instance().getSimultaneousVideoLimitSetting().getValue());
        setSimultaneousVideoLimitForEachProvider(
            Settings.instance().getSimultaneousVideoForEachProvider().getValue());
        setChromeDriverFile(
            Settings.instance().getChromeDriverSetting().getValue());
        setChromeDriverGhostModeValue(
            Settings.instance().getChromeDriverGhostModeSetting().getValue());
        setFFmpegFile(
            Settings.instance().getFFmpegSettings().getValue());
        setLoggingValue(
            Settings.instance().getLoggingSetting().getValue());
        setFlushValue(
            Settings.instance().getFlushSetting().getValue());
    }

    /**
     * Opens the directory chooser for the download folder and eventually
     * sets the new directory.
     */
    private void openDownloadDirectoryChooser() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select download folder");
        File dir = directoryChooser.showDialog(uiRoot.getScene().getWindow());

        if (!FileUtil.exists(dir)) {
            L.warn("Invalid download folder has been selected; it won't be changed");
            return;
        }

        L.debug("Changing download folder to: " + dir.getAbsolutePath());

        setCurrentDownloadFolderValue(dir.getAbsolutePath());
    }

    /**
     * Opens the file chooser for the chrome driver and eventually
     * sets the new chrome driver path.
     */
    private void openChromeDriverFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Chrome Driver");
        File chromeDriver = fileChooser.showOpenDialog(uiRoot.getScene().getWindow());

        if (!FileUtil.exists(chromeDriver)) {
            L.warn("Invalid chrome driver has been selected; it won't be changed");
            return;
        }

        L.debug("Changing chrome driver path to: " + chromeDriver.getAbsolutePath());

        setChromeDriverFile(chromeDriver);
    }

    /**
     * Opens the file chooser for the ffmpeg executable and eventually
     * sets the new ffmpeg path.
     */
    private void openFFmpegFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select FFmpeg");
        File ffmpeg = fileChooser.showOpenDialog(uiRoot.getScene().getWindow());

        if (!FileUtil.exists(ffmpeg)) {
            L.warn("Invalid ffmpeg has been selected; it won't be changed");
            return;
        }

        L.debug("Changing ffmpeg path to: " + ffmpeg.getAbsolutePath());

        setFFmpegFile(ffmpeg);
    }

    private void setCurrentDownloadFolderValue(String value) {
        uiDownloadFolder.setText(value);
    }

    private void setCurrentRemoveAfterDownloadValue(boolean value) {
        uiRemoveAfterDownload.selectedProperty().setValue(value);
    }

    private void setCurrentDownloadAutomaticallyValue(boolean value) {
        uiDownloadAutomatically.selectedProperty().setValue(value);
    }

    private void setSimultaneousVideoLimitValue(int value) {
        uiSimultaneousVideo.getValueFactory().setValue(value);
    }

    private void setSimultaneousVideoLimitForEachProvider(boolean value) {
        uiSimultaneousVideoForEachProvider.selectedProperty().setValue(value);
    }

    private void setChromeDriverFile(File file) {
        uiChromeDriver.setText(file != null ? file.getAbsolutePath() : "");
    }

    private void setChromeDriverGhostModeValue(boolean value) {
        uiChromeDriverGhostMode.selectedProperty().setValue(value);
    }

    private void setFFmpegFile(File file) {
        uiFFmpeg.setText(file != null ? file.getAbsolutePath() : null);
    }

    private void setLoggingValue(boolean value) {
        uiLogging.selectedProperty().setValue(value);
    }

    private void setFlushValue(boolean value) {
        uiFlush.selectedProperty().setValue(value);
    }
    /**
     * Saves every setting.
     */
    private void commitSettings() {
        // Download folder
        L.debug("Settings will be actually saved");

        Settings s = Settings.instance();

        s.getDownloadFolderSetting().updateSetting(
            new File(uiDownloadFolder.getText()));
        s.getRemoveAfterDownloadSetting().updateSetting(
            uiRemoveAfterDownload.isSelected());
        s.getDownloadAutomaticallySetting().updateSetting(
            uiDownloadAutomatically.isSelected());
        s.getSimultaneousVideoLimitSetting().updateSetting(
            uiSimultaneousVideo.getValue());
        s.getSimultaneousVideoForEachProvider().updateSetting(
            uiSimultaneousVideoForEachProvider.isSelected());
        s.getChromeDriverSetting().updateSetting(
            new File(uiChromeDriver.getText()));
        s.getChromeDriverGhostModeSetting().updateSetting(
            uiChromeDriverGhostMode.isSelected());
        s.getLoggingSetting().updateSetting(
            uiLogging.isSelected());
        s.getFlushSetting().updateSetting(
            uiFlush.isSelected());

        String ffmpeg = uiFFmpeg.getText();

        s.getFFmpegSettings().updateSetting(
            StringUtil.isValid(ffmpeg) ? new File(ffmpeg) : null
        );
    }

    /**
     * Closes this window.
     */
    private void closeWindow() {
        ((Stage) uiRoot.getScene().getWindow()).close();
    }

}
