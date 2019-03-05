package org.docheinstein.animedownloader.downloader.base;

import org.docheinstein.commons.logger.DocLogger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.util.logging.Level;

/**
 * Entity that represents a download of video that use {@link WebDriver}
 * to retrieve video info/link, more specifically a Chrome Driver.
 */
public abstract class ChromeMarionetteDownloader implements VideoDownloader {

    private static final DocLogger L =
        DocLogger.createForClass(ChromeMarionetteDownloader.class);

    /** Selenium web driver. */
    protected WebDriver mDriver;

    /** Whether the chrome driver should be started silently. */
    protected boolean mGhost;

    /** Observer of the video download. */
    protected VideoDownloadObserver mObserver;

    /** URL of the site (it is not the definitive link of the video). */
    protected String mDownloadUrl;

    /** Download folder. */
    protected File mDownloadFolder;

    /** Chrome driver path. */
    protected File mDriverPath;

    public ChromeMarionetteDownloader(String downloadUrl,
                                      File outputPath,
                                      File driverPath,
                                      boolean ghost,
                                      VideoDownloadObserver downloadObserver) {
        mDownloadUrl = downloadUrl;
        mDownloadFolder = outputPath;
        mDriverPath = driverPath;
        mGhost = ghost;
        mObserver = downloadObserver;
    }

    /**
     * Initializes the marionette and thus the webdriver.
     */
    public void initDriver() {
        System.setProperty(
            "webdriver.chrome.driver",
            mDriverPath.getAbsolutePath()
        );

        ChromeOptions co = new ChromeOptions();

        // Invisibility
        if (mGhost) {
            L.debug("Ghost chrome driver required");
            co.addArguments("--headless");
            co.addArguments("--mute-audio");
        }
        else {
            L.debug("Visible chrome driver required");
        }

        // Logging, for access network resources
        DesiredCapabilities caps = DesiredCapabilities.chrome();
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.PERFORMANCE, Level.ALL);
        caps.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);

        co.merge(caps);

        mDriver = new ChromeDriver(co);
    }

    /**
     * Returns whether this marionette is initialized.
     * @return whether this is initialized
     */
    public boolean isInitialized() {
        return mDriver != null;
    }
}
