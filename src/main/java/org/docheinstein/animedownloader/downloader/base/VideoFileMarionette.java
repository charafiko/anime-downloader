package org.docheinstein.animedownloader.downloader.base;

import org.docheinstein.animedownloader.video.VideoInfo;
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
public abstract class VideoFileMarionette extends ChromeMarionetteDownloader {

    private static final DocLogger L =
        DocLogger.createForClass(VideoFileMarionette.class);

    private HttpDownloader mDownloader;

    public VideoFileMarionette( String downloadUrl,
                                File outputPath,
                                File driverPath,
                                boolean ghostMode,
                                VideoDownloadObserver downloadObserver) {
        super(downloadUrl, outputPath, driverPath, ghostMode, downloadObserver);
    }

    public abstract String getVideoLink();
    public abstract VideoInfo getVideoInfo(HttpRequester.Response headResponse);

    @Override
    public void startDownload() {
        if (!isInitialized())
            initMarionette();

        String videoLink = getVideoLink();
        VideoInfo videoInfo = retrieveVideoInfo(videoLink);
        notifyObserver(videoInfo);
        doDownload(videoLink, videoInfo);
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

    /**
     * Retrieves the video info from the video's direct link.
     * @param videoLink the stream the link
     * @return the video info
     */
    private VideoInfo retrieveVideoInfo(String videoLink) {
        L.debug("Retrieving video info (size, filename) of: " + videoLink);

        return getVideoInfo( HttpRequester
            .head(videoLink)
            .allowRedirect(true)
            .initialized()
            .userAgent("curl/7.52.1")
            .accept("*/*")
            .send());
    }


    /**
     * Actually starts the download of the video from the given link.
     * @param videoLink the direct link of the video stream
     * @param videoInfo the video info
     */
    private void doDownload(String videoLink, VideoInfo videoInfo) {
        L.info("Downloading video from direct link: " + videoLink);

        File outputFile;

        if (FileUtil.exists(mDownloadFolder))
            outputFile = new File(mDownloadFolder, videoInfo.filename);
        else
            outputFile = new File(videoInfo.filename);

        L.info("Video will be downloaded to: " + outputFile.getAbsolutePath());

        if (mObserver != null)
            mObserver.onVideoDownloadStarted();

        try {
            mDownloader = new HttpDownloader();

            mDownloader.download(
                videoLink,
                outputFile.getAbsolutePath(),
                downloadedBytes -> {
                    long curMillis = System.currentTimeMillis();
                    if (mObserver != null)
                        mObserver.onVideoDownloadProgress(downloadedBytes, curMillis);
                },
                (int) M
            );

            if (mObserver != null)
                mObserver.onVideoDownloadFinished();
        } catch (IOException e) {
            L.error("Error occurred while startDownload the video", e);
        }

    }


    /**
     * Notifies the observes about the new video info.
     * @param videoInfo the video info to notify
     */
    private void notifyObserver(VideoInfo videoInfo) {
        L.debug("Video size detected: " + videoInfo.size / M + "MB");
        if (mObserver != null)
            mObserver.onVideoSizeDetected(videoInfo.size, true);

        L.debug("Video title detected: " + videoInfo.title);
        if (mObserver != null)
            mObserver.onVideoTitleDetected(videoInfo.title);
    }
}
