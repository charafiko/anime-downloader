package org.docheinstein.animedownloader.downloader.openload;

import org.docheinstein.animedownloader.downloader.base.VideoFileMarionette;
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
public class OpenloadMarionette extends VideoFileMarionette {

    private static final DocLogger L =
        DocLogger.createForClass(OpenloadMarionette.class);

    /** Template of the video's direct link */
    private static final String DIRECT_LINK_CURRENT_TEMPLATE = "https://openload.co/stream/%s";

    /** ID of the container of the video's direct link. */
    private static final String DIRECT_LINK_CURRENT_CONTAINER_ID = "DtsBlkVFQx";

    public OpenloadMarionette(String downloadUrl,
                              File outputPath,
                              File driverPath,
                              boolean ghostMode,
                              VideoDownloadObserver downloadObserver) {
        super(downloadUrl, outputPath, driverPath, ghostMode, downloadObserver);
    }

    @Override
    public String getVideoLink() {
        final String cdnLink = getCDNLink();

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
     * Retrieves the CDN link of the video.
     * <p>
     * The CDN link is "more external" than the stream link provided by
     * {@link #getVideoLink()}
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

    @Override
    public VideoInfo getVideoInfo(HttpRequester.Response headResponse) {
        VideoInfo videoInfo = new VideoInfo();

        Map<String, List<String>> headerFields = headResponse.getHeaderFields();

        // Size

        videoInfo.size = headResponse.getContentLength();

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

}