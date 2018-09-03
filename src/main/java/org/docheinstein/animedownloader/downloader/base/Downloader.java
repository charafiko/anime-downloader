package org.docheinstein.animedownloader.downloader.base;

/**
 * Interface that represents a downloader of resource.
 */
public interface Downloader {

    /** Starts the download. */
    void startDownload();

    /** Aborts the download. */
    void abortDownload();
}
