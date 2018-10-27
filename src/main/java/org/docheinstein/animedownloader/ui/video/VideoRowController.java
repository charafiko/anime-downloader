package org.docheinstein.animedownloader.ui.video;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import org.docheinstein.animedownloader.downloader.base.VideoDownloader;
import org.docheinstein.animedownloader.video.DownloadableVideoInfo;
import org.docheinstein.commons.utils.javafx.FXUtil;
import org.docheinstein.commons.utils.logger.DocLogger;
import org.docheinstein.animedownloader.commons.constants.Resources;
import org.docheinstein.animedownloader.downloader.base.VideoDownloadObserver;
import org.docheinstein.animedownloader.settings.Settings;
import org.docheinstein.animedownloader.ui.base.InstantiableController;
import org.docheinstein.animedownloader.video.VideoProvider;
import org.docheinstein.commons.utils.thread.ThreadUtil;
import org.docheinstein.commons.utils.types.StringUtil;


import java.awt.*;
import java.io.File;
import java.io.IOException;

import static org.docheinstein.animedownloader.commons.constants.Const.Math.M;

/**
 * Controller for a video row
 */
public class VideoRowController
    implements InstantiableController, VideoDownloadObserver {

    /**
     * Observer of this video row.
     */
    public interface VideoRowObserver {
        /**
         * Called when the download ends.
         * @param row the row controller to remove
         */
        void onDownloadEnd(VideoRowController row);

        /**
         * Called when the video row is asked to be removed
         * @param row the row controller to remove
         */
        void onRowRemovalRequired(VideoRowController row);
    }

    private static final DocLogger L =
        DocLogger.createForClass(VideoRowController.class);

    /**
     * State of the video download.
     */
    public enum VideoDownloadState {
        /** The video should still be downloaded.* */
        ToDownload,

        /** The download of the video is in progress. */
        Downloading,

        /** The video has been download successfully. */
        Downloaded,

        /** The video began but has been aborted. */
        Aborted
    }

    /** Current video state. */
    private VideoDownloadState mCurrentVideoState = VideoDownloadState.ToDownload;

    /** Identifier of the video represented by this controller. */
    private final String mIdentifier;

    /** Observers of this video row. */
    private final VideoRowObserver mObserver;

    private DownloadableVideoInfo mVideoInfo;

    /** Link of the video to download. */
    private final String mUrl;

    /** Time of the last chunk download (kept for calculate the download speed). */
    private long mLastDownloadChunkMillis;

    /** Bytes downloaded as of the last chunk download (kept for calculate the download speed). */
    private long mLastDownloadChunkBytes = 0;

    /** Underlying download used for actually download the video.*/
    private VideoDownloader mDownloader;

    /** Detected provider of the video to download. */
    private VideoProvider mProvider;

    /** Folder the video will be download to. */
    private File mDownloadFolder;


    @FXML
    private AnchorPane uiRoot;

    @FXML
    private Label uiTitle;

    @FXML
    private Label uiLink;

    @FXML
    private Button uiStartStopOpen;

    @FXML
    private ImageView uiStartStopOpenImage;

    @FXML
    private Button uiRemove;

    @FXML
    private Region uiPercentage;

    @FXML
    private Pane uiDownloadInfo;

    @FXML
    private Label uiSize;

    @FXML
    private Label uiCurrent;

    @FXML
    private Label uiSpeed;

    @FXML
    private Pane uiSpeedContainer;

    @FXML
    private ImageView uiProviderLogo;

    @FXML
    private ProgressIndicator uiPreDownloadSpinner;


    @Override
    public String getFXMLAsset() {
        return "video_row.fxml";
    }

    public VideoRowController(String url, String identifier,
                              VideoRowObserver observer) {
        this(url, identifier, observer, null);
    }


    public VideoRowController(String url, String identifier,
                              VideoRowObserver observer, DownloadableVideoInfo videoInfo) {
        mUrl = url;
        mIdentifier = identifier;
        mObserver = observer;
        mVideoInfo = videoInfo;
    }


    @FXML
    private void initialize() {
        mProvider = VideoProvider.getProviderForURL(mUrl);

        // Stuff initialized even if the provider is wrong

        // Remove button
        uiRemove.setOnMouseClicked(event -> {
            L.debug("Removing video row");

            if (mCurrentVideoState == VideoDownloadState.Downloading)
                abortVideoDownload();

            // Notify the observer
            notifyRowRemovalRequired();
        });

        // Link
        uiLink.setText(mUrl);

        if (mProvider == null) {
            L.warn("The pasted link doesn't belong to any valid provider");
            // Download is not allowed if the provider is not recognized
            uiStartStopOpen.setVisible(false);
            uiStartStopOpen.setManaged(false);
            return;
        }

        // Stuff initialized only if the provider is valid

        // Logo
        uiProviderLogo.setImage(mProvider.getLogo());

        // Start/Stop button
        uiStartStopOpen.setOnMouseClicked(event -> {
            if (mCurrentVideoState == VideoDownloadState.ToDownload ||
                mCurrentVideoState == VideoDownloadState.Aborted)
                download();
            else if (mCurrentVideoState == VideoDownloadState.Downloading)
                abortVideoDownload();
            else if (mCurrentVideoState == VideoDownloadState.Downloaded)
                openDownloadFolder();
        });

        updateMultiActionButton();

        // Initializes video info if provided

        initUIVideoInfo();
    }


    /**
     * Whether the download url belongs to a known provider.
     * @return whether this video url can be download with a known provider
     */
    public boolean hasValidProvider() {
        return mProvider != null;
    }

    /**
     * Returns the current state for the download of the video.
     * @return the video download state
     */
    public VideoDownloadState getState() {
        return mCurrentVideoState;
    }

    /**
     * Returns the identifier of the video.
     * @return the identifier of the video
     */
    public String getIdentifier() {
        return mIdentifier;
    }

    /**
     * Starts the video download and changes the controller video state.
     */
    public void download() {
        initDownloader();

        mCurrentVideoState = VideoDownloadState.Downloading;

        L.debug("Going to seek for direct video link from URL: " + mUrl);

        // Don't let the user do something in this transactional phase
        uiStartStopOpen.setVisible(false);
        uiStartStopOpen.setManaged(false);
        uiPreDownloadSpinner.setVisible(true);
        uiPreDownloadSpinner.setManaged(true);

        // Use the given video info if provided instead of reload it
        // This may be useful for skip the selenium step and directly
        // download the video which information have already been retrieved
        mDownloader.useVideoInfo(mVideoInfo);

        initUIVideoInfo();

        ThreadUtil.start(() -> mDownloader.startDownload());
    }

    /**
     * Retrieves the video info.
     * <p>
     * The info is retrieved from the page, thus the selenium driver is
     * used for perform the action.
     * @return the video info
     */
    public DownloadableVideoInfo retrieveVideoInfo() {
        initDownloader();
        mVideoInfo = mDownloader.retrieveVideoInfo();
        initUIVideoInfo();
        return mVideoInfo;
    }

    private synchronized void initDownloader() {
        if (!hasValidProvider()) {
            L.warn("Can't start video download is provider is invalid");
            return;
        }

        // Initializes if needed, only the first time
        if (mDownloader == null) {
            mDownloadFolder = Settings.instance().getDownloadFolderSetting().getValue();

            mDownloader = mProvider.createDownloader(
                mUrl,
                mDownloadFolder,
                Settings.instance().getChromeDriverSetting().getValue(),
                Settings.instance().getChromeDriverGhostModeSetting().getValue(),
                VideoRowController.this
            );
        }
    }

    /**
     * Initializes the title and the size of the row accordingly to the
     * current video info.
     */
    private void initUIVideoInfo() {
        if (mVideoInfo != null) {
            if (StringUtil.isValid(mVideoInfo.title))
                onVideoTitleDetected(mVideoInfo.title);
            if (mVideoInfo.size > 0)
                onVideoSizeDetected(mVideoInfo.size, true);
        }
    }

    /**
     * Stops the download of the video.
     */
    private void abortVideoDownload() {
        if (mDownloader == null) {
            L.warn("Can't abort download since it is not started yet!");
            return;
        }

        mDownloader.abortDownload();
    }

    /**
     * Opens the folder where video has been download to.
     */
    private void openDownloadFolder() {
        ThreadUtil.start(() -> {
            try {
                if (Desktop.isDesktopSupported()) {
                    // mDownloadFolder is kept instead of retrieving the
                    // path from setting since the setting could have been changed
                    // after the video download
                    L.debug("Trying to open " + mDownloadFolder
                        + " via default file explorer");
                    Desktop.getDesktop().open(mDownloadFolder);
                }
                else
                    L.warn("Desktop is not supported: folder can't be opened");
            } catch (IOException e) {
                L.warn("Folder " + mDownloadFolder.getAbsolutePath() + " can't be opened");
            }
        });
    }

    /**
     * Updates the open/start/stop button accordingly to the current video state.
     */
    private void updateMultiActionButton() {
        if (mCurrentVideoState == VideoDownloadState.ToDownload ||
            mCurrentVideoState == VideoDownloadState.Aborted) {
            Tooltip.install(uiStartStopOpen, new Tooltip("Download"));
            uiStartStopOpenImage.setImage(Resources.UI.START);
        }
        else if (mCurrentVideoState == VideoDownloadState.Downloading) {
            Tooltip.install(uiStartStopOpen, new Tooltip("Stop download"));
            uiStartStopOpenImage.setImage(Resources.UI.STOP);
        }
        else if (mCurrentVideoState == VideoDownloadState.Downloaded) {
            Tooltip.install(uiStartStopOpen, new Tooltip("Open video folder"));
            uiStartStopOpenImage.setImage(Resources.UI.OPEN_FOLDER);
        }
    }

    @Override
    public void onVideoDownloadStarted() {
        L.info("Download of " + mVideoInfo.title + " is started");
        mLastDownloadChunkMillis = System.currentTimeMillis();
        Platform.runLater(() -> {
            uiDownloadInfo.setVisible(true);
            uiPreDownloadSpinner.setVisible(false);
            uiPreDownloadSpinner.setManaged(false);
            uiStartStopOpen.setVisible(true);
            uiStartStopOpen.setManaged(true);

            // Removes any style (in case of resumed download the bar was orange)
            FXUtil.setClass(uiPercentage, "percentage-bar-background");

            updateMultiActionButton();
        });
    }

    @Override
    public void onVideoDownloadProgress(long downloadedBytes, long millis) {
        if (mCurrentVideoState != VideoDownloadState.Downloading) {
            L.warn("Video download progress callback received even if the " +
                   "video is not downloading; doing nothing");
            return;
        }

        L.verbose("Downloaded bytes: " + downloadedBytes + " of " + mVideoInfo.size);

        long deltaMillis = millis - mLastDownloadChunkMillis;
        long deltaBytes = downloadedBytes - mLastDownloadChunkBytes;

        mLastDownloadChunkBytes = downloadedBytes;
        mLastDownloadChunkMillis = millis;

        L.verbose("Delta time: " + deltaMillis);
        L.verbose("Delta bytes: " + deltaBytes);

        double kilobytesPerSecond = deltaBytes / deltaMillis;

        L.verbose("KB/s = dt/dbytes = " + kilobytesPerSecond);

        double rateo = (double) downloadedBytes / (double) mVideoInfo.size;
        double percentage = rateo * 100;

        L.verbose("Download percentage = " + String.format("%1$,.2f", percentage) + "%");

        double parentWidth = uiRoot.getWidth();
        double percentageBarWidth = parentWidth *
            /* rateo */ (double) downloadedBytes / (double) mVideoInfo.size;

        Platform.runLater(() -> {
            uiCurrent.setText(String.valueOf(downloadedBytes / M));

            uiSpeed.setText(String.valueOf(kilobytesPerSecond));

            double newPercentageBarAnchor = parentWidth - percentageBarWidth;

            // Progress bar should always go forward
            // Double rightAnchor = AnchorPane.getRightAnchor(uiPercentage);

            // if (rightAnchor == null
                // Allow negative progress in case of resumed download
                // that doesn't support resume (VVVVID)
                /*|| newPercentageBarAnchor < rightAnchor*/
            //    )
                AnchorPane.setRightAnchor(
                    uiPercentage,
                    parentWidth - percentageBarWidth
                );
        });
    }

    @Override
    public void onVideoDownloadFinished() {
        L.info("Download of " + mVideoInfo.title + " is finished");
        mCurrentVideoState = VideoDownloadState.Downloaded;

        Platform.runLater(() -> {
            uiCurrent.setText(String.valueOf(mVideoInfo.size / M));

            uiSpeedContainer.setVisible(false);
            updateMultiActionButton();

            // Attach to right
            AnchorPane.setRightAnchor(uiPercentage, (double) 0);
            FXUtil.addClass(uiPercentage, "finished");

            notifyDownloadEnd();
        });
    }

    @Override
    public void onVideoDownloadAborted() {
        L.info("Download of " + mVideoInfo.title + " has been aborted");

        mCurrentVideoState = VideoDownloadState.Aborted;

        Platform.runLater(() -> {
            // Still show the download info since the file is still on the disk
            // uiDownloadInfo.setVisible(false);
            // AnchorPane.setRightAnchor(uiPercentage, null);
            FXUtil.addClass(uiPercentage, "aborted");

            updateMultiActionButton();
        });
    }

    @Override
    public void onVideoTitleDetected(String title) {
        if (mVideoInfo == null)
            mVideoInfo = new DownloadableVideoInfo();
        mVideoInfo.title = title;
        Platform.runLater(() -> uiTitle.setText(title));
    }

    @Override
    public void onVideoSizeDetected(long videoSizeBytes, boolean certainly) {
        if (mVideoInfo == null)
            mVideoInfo = new DownloadableVideoInfo();
        mVideoInfo.size = videoSizeBytes;
        final String videoSizeString = String.valueOf(mVideoInfo.size / M);

        Platform.runLater(() ->
            uiSize.setText(
                certainly ?
                    videoSizeString :
                    "~" + videoSizeString));
    }

    /**
     * Notifies the observer about download end.
     */
    private void notifyDownloadEnd() {
        // Notify the observer
        if (mObserver == null) {
            L.warn("Null observer of video row, can't notify");
            return;
        }

        L.debug("Notifying observer about download end");

        mObserver.onDownloadEnd(this);
    }

    /**
     * Notifies the observer that row removal has been required.
     */
    private void notifyRowRemovalRequired() {
        // Notify the observer
        if (mObserver == null) {
            L.warn("Null observer of video row, can't notify");
            return;
        }

        L.debug("Notifying observer about forced row removal");

        mObserver.onRowRemovalRequired(this);
    }
}
