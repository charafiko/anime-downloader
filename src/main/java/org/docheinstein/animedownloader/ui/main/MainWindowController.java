package org.docheinstein.animedownloader.ui.main;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.*;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.docheinstein.animedownloader.settings.Settings;
import org.docheinstein.animedownloader.ui.base.InstantiableController;
import org.docheinstein.animedownloader.video.DownloadableVideoInfo;
import org.docheinstein.commons.utils.file.FileUtil;
import org.docheinstein.commons.utils.file.KeyValueFileHandler;
import org.docheinstein.commons.utils.javafx.FXUtil;
import org.docheinstein.commons.utils.logger.DocLogger;
import org.docheinstein.animedownloader.commons.constants.Config;
import org.docheinstein.animedownloader.ui.settings.SettingsWindowController;
import org.docheinstein.animedownloader.ui.video.VideoRowController;
import org.docheinstein.commons.utils.thread.ThreadUtil;
import org.docheinstein.commons.utils.types.StringUtil;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller of {@link MainWindow}
 */
public class MainWindowController
    implements InstantiableController, VideoRowController.VideoRowObserver {

    private static final DocLogger L =
        DocLogger.createForClass(MainWindowController.class);

    /**
     * Contains the visible video rows associated to their controller.
     */
    private Map<VideoRowController, Node> mVideoRows = new LinkedHashMap<>();

    @FXML
    private Node uiRoot;

    @FXML
    private Button uiSettings;

    @FXML
    private ListView<Node> uiDownloadList;

    @Override
    public String getFXMLAsset() {
        return "main_window.fxml";
    }

    @FXML
    private void initialize() {
        uiSettings.setOnMouseClicked(event -> {
            Stage stage = FXUtil.createWindow(
                new SettingsWindowController().createNode(),
                Config.App.SETTINGS_TITLE
            );

            stage.setAlwaysOnTop(true);
            stage.setResizable(false);
            stage.show();
        });

        // CTRL + V listener
        final KeyCombination CTRL_V_COMBINATION =
            new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN);

        uiRoot.addEventHandler(
            KeyEvent.KEY_PRESSED,
            event -> {
                if (CTRL_V_COMBINATION.match(event))
                    handlePaste();
            });

        uiDownloadList.setCellFactory(new Callback<ListView<Node>, ListCell<Node>>() {
            @Override
            public ListCell<Node> call(ListView<Node> param) {
                ListCell<Node> cell = new ListCell<Node>() {
                    @Override
                    protected void updateItem(Node item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setText(null);
                            setGraphic(null);
                        }
                        else {
                            setText(null);
                            setGraphic(item);
                        }
                    }
                };

                // Makes the cell fit its parent
                cell.prefWidthProperty().bind(uiDownloadList.widthProperty().subtract(50));

                return cell;
            }
        });

        loadVideosFromCache();
    }

    /**
     * Handles the CTRL + V event by adding a row for the pasted url.
     */
    private void handlePaste() {
        L.debug("Detected CTRL + V");

        try {
            String url = (String) Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .getContents(DataFlavor.stringFlavor)
                .getTransferData(DataFlavor.stringFlavor);


            String identifier = String.valueOf(System.currentTimeMillis());
            VideoRowController rowController = addVideoToDownloadList(identifier, url, null);

            if (rowController.hasValidProvider()) {

                // Automatically retrieves the video info and stores it
                ThreadUtil.start(() -> {
                    DownloadableVideoInfo videoInfo = rowController.retrieveVideoInfo();

                    saveVideoToCache(identifier, url, videoInfo);

                    // Starts the next possible video, if needed
                    checkForNextVideoToDownload();
                });
            }

        } catch (UnsupportedFlavorException | IOException e) {
            L.error("Error occurred while trying to copy data from system clipboard", e);
        }

    }

    /**
     * Adds the video row for the given url and identifier.
     * @param identifier the identifier of the video row
     * @param url the url of the video
     * @param videoInfo additional video info used for initialize the video row
     * @return the controller of the added row
     */
    private VideoRowController addVideoToDownloadList(
        String identifier, String url, DownloadableVideoInfo videoInfo) {
        L.info("Adding video [" + identifier + "] with URL: " + url);

        VideoRowController videoController = new VideoRowController(
            url, identifier, this, videoInfo
        );

        Node videoRow = videoController.createNode();

        mVideoRows.put(videoController, videoRow);

        uiDownloadList.getItems().add(videoRow);

        return videoController;
    }

    /**
     * Loads the video rows from the file system.
     */
    private void loadVideosFromCache() {
        File[] videos = Config.Folders.VIDEOS.listFiles();
        if (videos == null)
            return;

        Arrays.sort(videos);

        if (videos.length == 0) {
            L.debug("No video to load from cache");
            return;
        }

        for (File video : videos) {
            L.debug("Loading video from cache with identifier: " + video.getName());

            Map<String, String> videoKeyVals = new KeyValueFileHandler(
                video, Config.VideoCache.SEPARATOR
            ).readAll();

            DownloadableVideoInfo videoInfo = new DownloadableVideoInfo();

            videoInfo.title = videoKeyVals.get(Config.VideoCache.KEY_TITLE);
            videoInfo.filename = videoKeyVals.get(Config.VideoCache.KEY_FILENAME);
            videoInfo.directLink = videoKeyVals.get(Config.VideoCache.KEY_DIRECT_LINK);

            String sizeStr = videoKeyVals.get(Config.VideoCache.KEY_SIZE);
            videoInfo.size = StringUtil.isValid(sizeStr) ? Long.valueOf(sizeStr) : 0;

            String url = videoKeyVals.get(Config.VideoCache.KEY_URL);

            addVideoToDownloadList(video.getName(), url, videoInfo);
        }
    }

    /**
     * Saves the video to the file system.
     * @param identifier the identifier of the video, used as filename
     * @param url the url of the video, used as file's content
     * @param videoInfo other video info
     */
    private void saveVideoToCache(String identifier, String url,
                                  DownloadableVideoInfo videoInfo) {
        L.debug("Saving video to cache with following details\n" +
                "[ID] " + identifier + "\n" +
                "[URL] " + url + "\n" +
                videoInfo
        );

        File outputFile = new File(Config.Folders.VIDEOS, identifier);

        if (FileUtil.exists(outputFile)) {
            L.warn("Video already exists in cache; are you tyring to download the same video twice?");
            return;
        }

        Map<String, String> keyvals = new HashMap<>();
        keyvals.put(Config.VideoCache.KEY_URL, url);
        keyvals.put(Config.VideoCache.KEY_TITLE, videoInfo.title);
        keyvals.put(Config.VideoCache.KEY_FILENAME, videoInfo.filename);
        keyvals.put(Config.VideoCache.KEY_DIRECT_LINK, videoInfo.directLink);
        keyvals.put(Config.VideoCache.KEY_SIZE, String.valueOf(videoInfo.size));

        new KeyValueFileHandler(
            outputFile,
            Config.VideoCache.SEPARATOR
        ).writeAll(keyvals);
    }

    /**
     * Deletes the video from the file system.
     * @param identifier the identifier of the video to remove
     */
    private void deleteVideoFromCache(String identifier) {
        L.debug("Deleting video from cache with identifier: " + identifier);
        FileUtil.delete(new File(Config.Folders.VIDEOS, identifier));
    }

    /**
     * Eventually starts the next available video if the download automatically
     * setting is enabled and it won't exceed the simultaneous video limit.
     */
    private void checkForNextVideoToDownload() {
        // Automatically start the first available video if required
        if (!Settings.instance().getDownloadAutomaticallySetting().getValue()) {
            L.debug("Automatic download disabled, nothing to start");
            return;
        }

        int inDownloadCount = 0;

        for (Map.Entry<VideoRowController, Node> row : mVideoRows.entrySet()) {
            if (row.getKey().getState() == VideoRowController.VideoDownloadState.Downloading)
                inDownloadCount++;
        }

        int downloadLimit  = Settings.instance().getSimultaneousVideoLimitSetting().getValue();

        L.debug("There are " + inDownloadCount + " video in download");
        L.debug("The download limit is "+ downloadLimit);

        if (inDownloadCount >= downloadLimit) {
            L.debug("Video won't be started automatically since it would exceed the limit");
            return;
        }

        L.debug("Searching for the first available video to start automatically");

        for (Map.Entry<VideoRowController, Node> row : mVideoRows.entrySet()) {
            VideoRowController video = row.getKey();
            if (video.getState() == VideoRowController.VideoDownloadState.ToDownload) {
                L.debug("Found video still to download, automatically downloading it");
                video.download();
                return;
            }
        }

        L.debug("No video to download found, doing nothing");
    }

    @Override
    public void onRowRemovalRequired(VideoRowController row) {
        L.debug("Removing row of video");
        uiDownloadList.getItems().remove(mVideoRows.remove(row));

        deleteVideoFromCache(row.getIdentifier());

        checkForNextVideoToDownload();
    }

    @Override
    public void onDownloadEnd(VideoRowController row) {
        if (Settings.instance().getRemoveAfterDownloadSetting().getValue()) {
            L.debug("Removing row of downloaded video");
            uiDownloadList.getItems().remove(mVideoRows.remove(row));
        }

        // Delete even if remove_after_download is disabled since there is
        // no need to keep a downloaded video in the cache
        deleteVideoFromCache(row.getIdentifier());

        checkForNextVideoToDownload();
    }
}
