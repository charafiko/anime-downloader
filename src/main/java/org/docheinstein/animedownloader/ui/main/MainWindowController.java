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
import org.docheinstein.animedownloader.video.VideoProvider;
import org.docheinstein.commons.javafx.FXUtil;
import org.docheinstein.commons.thread.ThreadUtil;
import org.docheinstein.commons.file.FileUtil;
import org.docheinstein.commons.file.KeyValueFileHandler;
import org.docheinstein.commons.logger.DocLogger;
import org.docheinstein.animedownloader.commons.constants.Config;
import org.docheinstein.animedownloader.ui.settings.SettingsWindowController;
import org.docheinstein.animedownloader.ui.video.VideoRowController;
import org.docheinstein.commons.types.StringUtil;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
    private Button uiPaste;

    @FXML
    private Button uiOpenDownloadFolder;

    @FXML
    private Button uiStartDownload;

    @FXML
    private ListView<Node> uiDownloadList;

    @Override
    public String getFXMLAsset() {
        return "main_window.fxml";
    }

    private AtomicInteger mInDownloadCount = new AtomicInteger(0);

    @FXML
    private void initialize() {

        uiOpenDownloadFolder.setOnMouseClicked(event ->
            ThreadUtil.start(() -> {
                if (!Desktop.isDesktopSupported()) {
                    L.warn("Desktop is not supported: folder can't be opened");
                    return;
                }

                try {
                    // Open the current download folder
                    File downloadFolder = Settings.instance().getDownloadFolderSetting().getValue();
                    L.debug("Trying to open " + downloadFolder + " via default file explorer");
                    Desktop.getDesktop().open(downloadFolder);
                } catch (IOException e) {
                    L.warn("Current download folder can't be opened");
                }
            }
        ));

        uiSettings.setOnMouseClicked(event -> {
            Stage stage = FXUtil.createWindow(
                new SettingsWindowController().createNode(),
                Config.App.SETTINGS_TITLE
            );

            stage.setAlwaysOnTop(true);
            stage.setResizable(false);
            stage.show();
        });

        // Paste listener
        uiPaste.setOnMouseClicked(event -> handlePaste());

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

        // Start download button
        uiStartDownload.setOnMouseClicked(event -> {
            L.debug("Starting automatic download");
            checkForNextVideoToDownload();
        });

        // Enable/disable automatic download based on the automatic download
        // setting
        uiStartDownload.setDisable(
            !Settings.instance().getDownloadAutomaticallySetting().getValue());

        Settings.instance().getDownloadAutomaticallySetting().addListener(
            (setting, enabled) ->  uiStartDownload.setDisable(!enabled));

        loadVideosFromCache();
    }

    /**
     * Handles the CTRL + V event by adding a row for the pasted url.
     */
    private void handlePaste() {
        L.debug("Detected CTRL + V or Paste action");

        try {
            String url = (String) Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .getContents(DataFlavor.stringFlavor)
                .getTransferData(DataFlavor.stringFlavor);


            String identifier = String.valueOf(System.currentTimeMillis());

            if (!StringUtil.isValid(identifier)) {
                L.warn("Can't add null string to video list");
                return;
            }

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

        // Scroll to bottom
        uiDownloadList.scrollTo(uiDownloadList.getItems().size() - 1);

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

        Settings.AutomaticDownloadStrategy strategy =
            Settings.instance().getAutomaticDownloadStrategySetting().getValue();

        boolean stillToDownload;
        do {
            if (strategy == Settings.AutomaticDownloadStrategy.Static) {
                L.debug("Checking for automatic download using 'Static' strategy");
                stillToDownload = checkForNextVideoToDownloadStaticStrategy();
            } else if (strategy == Settings.AutomaticDownloadStrategy.Adaptive) {
                L.debug("Checking for automatic download using 'Adaptive' strategy");
                stillToDownload = checkForNextVideoToDownloadAdaptiveStrategy();
            } else {
                L.warn("Do not know strategy: " + strategy);
                stillToDownload = false;
            }
        } while (stillToDownload);
    }

    /*
     * The logic of the static strategy is just download until the current
     * download count is lower than the specified limit, eventually referred
     * to each provider.
     */
    private VideoRowController getNextVideoToDownloadUsingStaticStrategy() {
        int downloadLimit  = Settings.instance().getSimultaneousVideoLimitSetting().getValue();
        boolean forEachProvider = Settings.instance().getSimultaneousVideoForEachProvider().getValue();

        Map<VideoProvider, Integer> inDownloadVideos = getInDownloadVideos();
        int inDownloadCount = 0;

        for (Integer count : inDownloadVideos.values()) {
            inDownloadCount += count;
        }

        L.debug("There are " + inDownloadCount + " video in download");
        L.debug("The download limit is: " + downloadLimit);
        L.debug("Referred to each provider: " + forEachProvider);

        // Check if the download count exceed the limit, but only if forEachProvider
        // is not true and so the limit is referred to a global limit
        if (!forEachProvider && inDownloadCount >= downloadLimit) {
            L.debug("Video won't be started automatically since it would exceed the limit");
            return null;
        }

        L.debug("Searching for the first available video to start automatically");

        for (Map.Entry<VideoRowController, Node> row : mVideoRows.entrySet()) {
            VideoRowController video = row.getKey();
            if (video.getState() == VideoRowController.VideoDownloadState.ToDownload) {

                int inDownloadForProvider = inDownloadVideos.getOrDefault(video.getProvider(), 0);

                if (inDownloadForProvider < downloadLimit) {
                    L.debug("Found video still to download, automatically downloading it");
                    return video;
                }
            }
        }

        L.debug("No video to download found, doing nothing");
        return null;
    }

    /*
     * The logic of the adaptive strategy is start to download videos until
     * one of the following conditions is met.
     * 1) Just like static download, if the current download count is not lower
     * then the specified limit, nothing else is automatically put in download
     * 2) Moreover, if the current sum of the download bandwidth exceed the
     * specified limit, nothing else is automatically put in download
     */
    private boolean checkForNextVideoToDownloadAdaptiveStrategy() {
        VideoRowController videoToDownload = getNextVideoToDownloadUsingStaticStrategy();
        if (videoToDownload == null)
            return false; // Nothing even with static strategy, doing nothing

        L.debug("Found video that could be download, checking for bandwidth" +
                " before proceed");

        int bandwidthLimit = Settings.instance().getBandwidthLimit().getValue();
        L.debug("Bandwidth limit is: " + (bandwidthLimit / 1000) + "KB/s");

        // Check for at least a few second if the current bandwidth is below the threshold
        for (int i = 0; i < Config.Download.ADAPTIVE_STRATEGY_SECONDS_BEFORE_PROCEED; i++) {
            int currentBandwidth = 0;

            // Unlike static strategy, here we have to check for bandwidth too
            for (Map.Entry<VideoRowController, Node> row : mVideoRows.entrySet()) {
                VideoRowController video = row.getKey();
                int bw = video.getInstantBandwidth();
                L.debug("Bandwidth of video " + video.getVideoInfo().title + " is " + (bw / 1000) + "KB/s");
                currentBandwidth += bw;
            }

            L.debug("#" + (i + 1) + " Total current bandwidth is " + (currentBandwidth / 1000)+ "KB/s");
            if (currentBandwidth > bandwidthLimit) {
                L.debug("Can't download since bandwidth limit would be exceeded");
                return false; // Current bandwidth is above the threshold, doing nothing
            }

            if (i < Config.Download.ADAPTIVE_STRATEGY_SECONDS_BEFORE_PROCEED - 1) {
                L.debug("Current bandwidth is below the threshold, waiting 1s before next check...");
                ThreadUtil.sleep(1000);
            }
        }

        // If we are here, then for at least a few seconds the current bandwidth has been
        // below than the limit, thus actually download the video

        L.debug("Actually downloading video using 'Adaptive' strategy");
        videoToDownload.download();
        return true;
    }

    private boolean checkForNextVideoToDownloadStaticStrategy() {
        VideoRowController videoToDownload = getNextVideoToDownloadUsingStaticStrategy();
        if (videoToDownload == null)
            return false;
        videoToDownload.download();
        return true;
    }

    private Map<VideoProvider, Integer> getInDownloadVideos() {
        Map<VideoProvider, Integer> inDownloadVideos = new HashMap<>();

        for (Map.Entry<VideoRowController, Node> row : mVideoRows.entrySet()) {
            VideoRowController videoRow = row.getKey();
            if (videoRow.getState() == VideoRowController.VideoDownloadState.Downloading) {
                inDownloadVideos.put(
                    videoRow.getProvider(),
                    inDownloadVideos.getOrDefault(videoRow.getProvider(), 0) + 1
                );
            }
        }

        return inDownloadVideos;
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
