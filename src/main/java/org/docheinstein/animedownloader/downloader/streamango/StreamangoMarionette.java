package org.docheinstein.animedownloader.downloader.streamango;

import org.docheinstein.animedownloader.downloader.base.VideoDownloadObserver;
import org.docheinstein.animedownloader.downloader.base.VideoFileMarionetteDownloader;
import org.docheinstein.animedownloader.video.DownloadableVideoInfo;
import org.docheinstein.commons.http.HttpRequester;
import org.docheinstein.commons.logger.DocLogger;
import org.docheinstein.commons.types.StringUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;

/**
 * Specific marionette able to download video from "https://streamango.com".
 */
public class StreamangoMarionette extends VideoFileMarionetteDownloader {

    private static final DocLogger L =
        DocLogger.createForClass(StreamangoMarionette.class);

    private static final String DIRECT_LINK_CONTAINER_ID = "mgvideo_html5_api";

    public StreamangoMarionette(String downloadUrl,
                                File outputPath,
                                File driverPath,
                                boolean ghostMode,
                                VideoDownloadObserver downloadObserver) {
        super(downloadUrl, outputPath, driverPath, ghostMode, downloadObserver);
    }

    // <meta name="og:url" content="https://streamango.com/f/eakfkcppkkrlsost/My_Hero_Academia_01_HD_ITA_mp4">
    @Override
    public String getVideoLink() {
        L.debug("Going to fetch page from " + mDownloadUrl);

        mDriver.get(mDownloadUrl);

        L.verbose("Fetched paged:\n" + mDriver.getPageSource());

        L.debug("Waiting for video link container to be added to the page");

        (new WebDriverWait(mDriver, 10L)).until((ExpectedCondition<Boolean>) d -> {
            WebElement videoElement = mDriver.findElement(By.id(DIRECT_LINK_CONTAINER_ID));
            String videoElementContent = videoElement.getAttribute("src");
            L.verbose("Current 'src' of direct link container is: " + videoElementContent);
            return !videoElementContent.isEmpty();
        });

        WebElement directLinkContainer = mDriver.findElement(By.id(DIRECT_LINK_CONTAINER_ID));
        String directLinkContent = directLinkContainer.getAttribute("src");

        String normalizedDirectLink =
            directLinkContent.startsWith("https://") ?
                directLinkContent :
                "https://" + directLinkContent;


        L.debug("Container of the direct link found; (normalized) 'src' is: " + normalizedDirectLink);

        return normalizedDirectLink;
    }

    @Override
    public DownloadableVideoInfo getVideoInfo(HttpRequester.Response headResponse) {
        DownloadableVideoInfo videoInfo = new DownloadableVideoInfo();

        // Size

        videoInfo.size = headResponse.getContentLength();

        // Filename & Title

        String videoTitle = mDriver.findElement(
            By.className("page-title")).getAttribute("textContent");

        if (StringUtil.isValid(videoTitle)) {
            videoInfo.filename = videoInfo.title = videoTitle.trim();
        }

        mDriver.quit();

        return videoInfo;
    }
}

