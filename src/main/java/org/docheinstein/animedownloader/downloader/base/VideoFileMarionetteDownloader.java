package org.docheinstein.animedownloader.downloader.base;

import org.docheinstein.animedownloader.video.DownloadableVideoInfo;
import org.docheinstein.commons.utils.file.FileUtil;
import org.docheinstein.commons.utils.http.HttpDownloader;
import org.docheinstein.commons.utils.http.HttpRequester;
import org.docheinstein.commons.utils.logger.DocLogger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.docheinstein.animedownloader.commons.constants.Const.Math.M;

/**
 * Basic marionette that figures out the direct link of the video from the page
 * and thus is able to retrieve info about it and download it.
 * <p>
 * Marionettes that download the video from a single stream link should inherit
 * from this whereas most complex marionettes which use multiple-segments video
 * or similiar stuff can ignore this abstraction.
 */
public abstract class VideoFileMarionetteDownloader extends ChromeMarionetteDownloader {

    private static final DocLogger L =
        DocLogger.createForClass(VideoFileMarionetteDownloader.class);

    private HttpDownloader mDownloader;

    private DownloadableVideoInfo mVideoInfo;

    public VideoFileMarionetteDownloader(String downloadUrl,
                                         File outputPath,
                                         File driverPath,
                                         boolean ghostMode,
                                         VideoDownloadObserver downloadObserver) {
        super(downloadUrl, outputPath, driverPath, ghostMode, downloadObserver);
    }

    public abstract String getVideoLink();
    public abstract DownloadableVideoInfo getVideoInfo(HttpRequester.Response headResponse);

    @Override
    public void startDownload() {
        initMarionette();

        notifyTitleToObserver();
        notifySizeToObserver();

        doDownload();

    }

    @Override
    public void abortDownload() {
        // Notifies anyhow
        if (mObserver != null)
            mObserver.onVideoDownloadAborted();

        if (mDownloader == null) {
            L.warn("Can't stop download since underlying HttpDownloader is null");
            return;
        }

        mDownloader.enableDownload(false);
    }

    @Override
    public DownloadableVideoInfo retrieveVideoInfo() {
        initMarionette();
        return mVideoInfo;
    }

    @Override
    public void useVideoInfo(DownloadableVideoInfo videoInfo) {
        mVideoInfo = videoInfo;
    }

    /**
     * Initializes the underlying driver and actually retrieved the video info
     * without starting the download.
     * <p>
     * This method is thread-safe.
     */
    private synchronized void initMarionette() {
        if (mVideoInfo == null) {

            if (!isInitialized())
                initDriver();

            String directLink = getVideoLink();
            mVideoInfo = retrieveVideoInfo(directLink);
            mVideoInfo.directLink = directLink;
        } else {
            L.debug("Skipping marionette initialization since video info is not null");
        }
    }

    /**
     * Retrieves the video info from the video's direct link.
     * @param directLink link the the video resource
     * @return the video info
     */
    private DownloadableVideoInfo retrieveVideoInfo(String directLink) {
        L.debug("Retrieving video info (size, filename) of: " + directLink);

        return getVideoInfo(HttpRequester
            .head(directLink)
            .allowRedirect(true)
            .initialized()
            .userAgent("curl/7.52.1")
            .accept("*/*")
            .send()
        );
    }


    /**
     * Actually starts the download of the video.
     */
    private void doDownload() {
        L.info("Downloading video from direct link: " + mVideoInfo.directLink);

        File outputFile;

        if (FileUtil.exists(mDownloadFolder))
            outputFile = new File(mDownloadFolder, mVideoInfo.filename);
        else
            // Save in current directory as B plan
            outputFile = new File(mVideoInfo.filename);

        L.info("Video will be downloaded to: " + outputFile.getAbsolutePath());

        boolean fileAlreadyExists = FileUtil.exists(outputFile);

        if (fileAlreadyExists)
            L.info("Video already exists, it will be resumed if possible");

        final long alreadyDownloadedBytes =
            fileAlreadyExists ? outputFile.length() : 0;

        if (mObserver != null)
            mObserver.onVideoDownloadStarted();

        try {
            mDownloader = new HttpDownloader();

            boolean downloadFinished = mDownloader.download(
                mVideoInfo.directLink,
                outputFile.getAbsolutePath(),
                downloadedBytes -> {
                    long curMillis = System.currentTimeMillis();
                    if (mObserver != null)
                        mObserver.onVideoDownloadProgress(
                            alreadyDownloadedBytes + downloadedBytes,
                            curMillis);
                },
                (int) M
            );

            if (downloadFinished && mObserver != null)
                mObserver.onVideoDownloadFinished();
        } catch (IOException e) {
            L.error("Error occurred while download the video", e);
        }

    }

    /**
     * Notifies the observes about the new video title.
     */
    private void notifyTitleToObserver() {
        L.debug("Video title detected: " + mVideoInfo.title);
        if (mObserver != null)
            mObserver.onVideoTitleDetected(mVideoInfo.title);
    }

    /**
     * Notifies the observes about the new video size.
     */
    private void notifySizeToObserver() {
        L.debug("Estimated video size: " + mVideoInfo.size / M + "MB");
        if (mObserver != null)
            mObserver.onVideoSizeDetected(mVideoInfo.size, true);
    }


    /**
     * Prints the header fields; for debugging purpose.
     * @param hf the header fields of an HTTP packet
     */
    protected void printHeaderFields(Map<String, List<String>> hf) {
        hf.forEach((k, vs) -> {
            L.debug("Key = " + k);
            vs.forEach(v -> L.debug("--Value = " + v));
        });
    }
}
