package org.docheinstein.animedownloader.video;

/**
 * Encapsulates some of the info related to a video.
 */
public class DownloadableVideoInfo {

    /** Direct link to the video resource. */
    public String directLink = null;

    /** Filename of the video. */
    public String filename = null;

    /** Title of the video. */
    public String title = null;

    /** Size in byte of the video. */
    public long size = 0; // bytes

    @Override
    public String toString() {
        return
            "[DIRECT_LINK]: " + directLink + "\n" +
            "[FILENAME]: " + filename + "\n" +
            "[TITLE]: " + title + "\n" +
            "[SIZE]: " + size;
    }
}
