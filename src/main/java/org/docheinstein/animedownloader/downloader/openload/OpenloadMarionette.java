package org.docheinstein.animedownloader.downloader.openload;

import org.docheinstein.commons.utils.file.FileUtil;
import org.docheinstein.commons.utils.http.HttpDownloader;
import org.docheinstein.commons.utils.http.HttpRequester;
import org.docheinstein.commons.utils.logger.DocLogger;
import org.docheinstein.commons.utils.types.StringUtil;

import org.docheinstein.animedownloader.downloader.base.ChromeMarionetteDownloader;
import org.docheinstein.animedownloader.downloader.base.VideoDownloadObserver;
import org.docheinstein.animedownloader.video.VideoInfo;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;

import java.util.List;
import java.util.Map;

import static org.docheinstein.animedownloader.commons.constants.Const.Math.M;

/**
 * Specific marionette able to download video from "https://openload.co".
 */
public class OpenloadMarionette
    extends ChromeMarionetteDownloader {

    private static final DocLogger L =
        DocLogger.createForClass(OpenloadMarionette.class);

    /** Template of the video's direct link */
    private static final String DIRECT_LINK_CURRENT_TEMPLATE = "https://openload.co/stream/%s";

    /** ID of the container of the video's direct link. */
    private static final String DIRECT_LINK_CURRENT_CONTAINER_ID = "DtsBlkVFQx";

    private HttpDownloader mDownloader;

    public OpenloadMarionette(String downloadUrl,
                              File outputPath,
                              File driverPath,
                              boolean ghostMode,
                              VideoDownloadObserver downloadObserver) {
        super(downloadUrl, outputPath, driverPath, ghostMode, downloadObserver);
    }

    @Override
    public void startDownload() {
        if (!isInitialized())
            initMarionette();

        String directLink = getStreamDirectLink(getCDNLink());
        VideoInfo videoInfo = retrieveVideoInfo(directLink);
        notifyObserver(videoInfo);
        doDownload(directLink, videoInfo);
    }

    @Override
    public void abortDownload() {
        // Notifies anyhow
        if (mObserver != null)
            mObserver.onVideoDownloadAborted();

        if (mDownloader == null) {
            L.warn("Can't stop startDownload since underlying HttpDownloader is null");
            return;
        }

        mDownloader.enableDownload(false);
    }

    /**
     * Retrieves the CDN link of the video.
     * <p>
     * The CDN link is "more external" than the stream link provided by
     * {@link #getStreamDirectLink(String)}
     * @return the cdn link of the video.
     */
    private String getCDNLink() {
        L.debug("Going to fetch page from " + mDownloadUrl);

        mDriver.get(mDownloadUrl);

        L.verbose("Fetched paged:\n" + mDriver.getPageSource());

        L.debug("Waiting for video CDN link container to be added to the page");

        (new WebDriverWait(mDriver, 10L)).until((ExpectedCondition<Boolean>) d -> {
            WebElement videoElement = mDriver.findElement(By.id(DIRECT_LINK_CURRENT_CONTAINER_ID));
            String videoElementContent = videoElement.getAttribute("textContent");
            L.verbose("Current content of direct link container is: " + videoElementContent);
            return
                !videoElementContent.isEmpty() &&
                !videoElementContent.contains("HERE IS THE LINK");
        });

        WebElement partialCDNLinkContainer = mDriver.findElement(By.id(DIRECT_LINK_CURRENT_CONTAINER_ID));
        String partialCDNLink = partialCDNLinkContainer.getAttribute("textContent");

        L.debug("Container of the CDN link found; content is: " + partialCDNLink);

        String cdnLink = String.format(DIRECT_LINK_CURRENT_TEMPLATE, partialCDNLink);

        mDriver.quit();

        return cdnLink;
    }

    /**
     * Returns the stream direct link of the video.
     * @param cdnLink the cdn link to visit for figure out the direct link
     * @return the video direct link
     */
    private String getStreamDirectLink(String cdnLink) {
        Map<String, List<String>> headerFields = HttpRequester
            .head(cdnLink)
            .allowRedirect(false)
            .send()
            .getHeaderFields();

        printHeaderFields(headerFields);

        List<String> directLinks = headerFields.get("Location");

        if (directLinks == null || directLinks.size() < 1) {
            L.error("Can't retrieve 'Location' header");
            return null;
        }

        String streamDirectLink = directLinks.get(0);

        L.debug("Direct link location resolved to: " + streamDirectLink);

        return streamDirectLink;
    }

    /**
     * Retrieves the video info from the stream direct link.
     * @param streamDirectLink the stream the link
     * @return the video info
     */
    private VideoInfo retrieveVideoInfo(String streamDirectLink) {
        VideoInfo videoInfo = new VideoInfo();

        // Size

        L.debug("Retrieving video info (size, filename) from: " + streamDirectLink);

        HttpRequester.Response resp = HttpRequester
            .head(streamDirectLink)
            .allowRedirect(false)
            .initialized()
            .userAgent("curl/7.52.1")
            .accept("*/*")
            .send();

        Map<String, List<String>> headerFields = resp.getHeaderFields();

        printHeaderFields(headerFields);

        videoInfo.size = resp.getContentLength();

        // Filename

        // e.g. attachment; filename="BlackClover_Ep_03_SUB_ITA.mp4"
        List<String> contentDispositions = headerFields.get("Content-Disposition");

        if (contentDispositions == null || contentDispositions.size() < 1) {
            L.error("Can't retrieve 'Content-Disposition' header");
            return videoInfo;
        }

        String contentDisposition = contentDispositions.get(0);

        videoInfo.title = videoInfo.filename = contentDisposition;

        if (StringUtil.isValid(contentDisposition))
            videoInfo.title  = videoInfo.filename =
                contentDisposition.split("filename=\"")[1].split("\"")[0];


        return videoInfo;
    }

    /**
     * Actually starts the download of the video from the given stream link.
     * @param streamDirectLink the direct link of the video stream
     * @param videoInfo the video info
     */
    private void doDownload(String streamDirectLink, VideoInfo videoInfo) {
        L.info("Downloading video from direct link stream: " + streamDirectLink);

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
                streamDirectLink,
                outputFile.getAbsolutePath(),
                new HttpDownloader.DownloadObserver() {
                    @Override
                    public void onProgress(long downloadedBytes,
                                           long millis) {
                        if (mObserver != null)
                            mObserver.onVideoDownloadProgress(downloadedBytes, millis);
                    }

                    @Override
                    public void onEnd() {
                        if (mObserver != null)
                            mObserver.onVideoDownloadFinished();
                    }
                },
                (int) M
            );
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

    /**
     * Prints the header fields; for debugging purpose.
     * @param hf the header fields of an HTTP packet
     */
    private static void printHeaderFields(Map<String, List<String>> hf) {
        hf.forEach((k, vs) -> {
            L.debug("Key = " + k);
            vs.forEach(v -> L.debug("--Value = " + v));
        });
    }
}