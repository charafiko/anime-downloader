package org.docheinstein.animedownloader.downloader.base;

import org.docheinstein.animedownloader.video.DownloadableVideoInfo;

/**
 * Interface that represents a downloader of video.
 */
public interface VideoDownloader extends Downloader {

    /**
     * Returns the title of the video.
     * @return the video's title
     */
    DownloadableVideoInfo retrieveVideoInfo();

    /**
     * Uses the given video info for retrieve needed info (title, directLink)
     * instead of retrieve it from the web page.
     * @param videoInfo the video info
     */
    void useVideoInfo(DownloadableVideoInfo videoInfo);
}
