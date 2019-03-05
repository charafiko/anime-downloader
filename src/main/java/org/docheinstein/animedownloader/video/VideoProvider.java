package org.docheinstein.animedownloader.video;

import javafx.scene.image.Image;
import org.docheinstein.animedownloader.downloader.streamango.StreamangoMarionette;
import org.docheinstein.animedownloader.downloader.vvvvid.VVVVIDMarionette;
import org.docheinstein.commons.types.StringUtil;
import org.docheinstein.animedownloader.commons.constants.Resources;
import org.docheinstein.animedownloader.downloader.base.ChromeMarionetteDownloader;
import org.docheinstein.animedownloader.downloader.base.VideoDownloadObserver;
import org.docheinstein.animedownloader.downloader.openload.OpenloadMarionette;

import java.io.File;

/**
 * Represents a site that provides video streams.
 */
public enum  VideoProvider {
    Openload,   // openload.co
    VVVVID,     // vvvvid.it
    Streamango  // streamango.com
    ;

    /**
     * Returns the provider associated with the given url or null if the
     * url is not provided by any of the known providers.
     * @param url the video url
     * @return the provider of the video
     */
    public static VideoProvider getProviderForURL(String url) {
        if (!StringUtil.isValid(url))
            return null;

        if (url.contains("openload"))
            return Openload;
        if (url.contains("vvvvid"))
            return VVVVID;
        if (url.contains("streamango"))
            return Streamango;

        return null;
    }

    /**
     * Returns a downloader able to download a video that belongs
     * to this provider.
     * @param downloadUrl the video url
     * @param downloadFolder the download folder
     * @param driverPath the path of the chrome driver
     * @param ghostMode whether the chrome driver should be started in ghost mode
     * @param downloadObserver the observer of the download
     * @return the appropriate downloader for this provider
     */
    public ChromeMarionetteDownloader createDownloader(
        String downloadUrl,
        File downloadFolder,
        File driverPath,
        boolean ghostMode,
        VideoDownloadObserver downloadObserver
    ) {
        switch (this) {
        case Openload:
            return new OpenloadMarionette(
                downloadUrl, downloadFolder,
                driverPath, ghostMode,
                downloadObserver);
        case VVVVID:
            return new VVVVIDMarionette(
                downloadUrl, downloadFolder,
                driverPath, ghostMode,
                downloadObserver);
        case Streamango:
            return new StreamangoMarionette(
                downloadUrl, downloadFolder,
                driverPath, ghostMode,
                downloadObserver);
        }

        return null;
    }

    /**
     * Returns the logo associated with this provider.
     * @return the provider's logo
     */
    public Image getLogo() {
        switch (this) {
        case Openload:
            return Resources.UI.OPENLOAD;
        case VVVVID:
            return Resources.UI.VVVVID;
        case Streamango:
            return Resources.UI.STREAMANGO;
        }
        return null;
    }
}
