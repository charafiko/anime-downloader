package org.docheinstein.animedownloader.commons.constants;

import org.docheinstein.commons.utils.system.OSType;
import org.docheinstein.commons.utils.system.SystemUtil;

import java.io.File;


/** Contains application's config. */
public class Config {

    /** Values concern the application itself. */
    public static class App {
        public static final String TITLE = "Anime Downloader";
        public static final String SETTINGS_TITLE = "Settings";

        public static final int MIN_WIDTH = 480;
        public static final int MIN_HEIGHT = 320;
    }

    /** Contains the directories used by the application. */
    public static class Folders {
        public static final File LOGS = new File("logs/");
        public static final File SETTINGS = new File("settings/");
        public static final File VIDEOS = new File("videos/");
        public static final File TMP = new File("tmp/");
        public static final File CHROME_DRIVER = new File("chromedriver/");
    }

    /** Contains the files used by the application. */
    public static class Files {

        public static final File DEFAULT_CHROME_DRIVER;

        static {
            OSType os = SystemUtil.getCurrentOperatingSystemType();

            if (os == OSType.Linux)
                DEFAULT_CHROME_DRIVER = new File(Folders.CHROME_DRIVER, "chromedriver");
            else if (os == OSType.Windows)
                DEFAULT_CHROME_DRIVER = new File(Folders.CHROME_DRIVER, "chromedriver.exe");
            else if (os == OSType.Mac)
                DEFAULT_CHROME_DRIVER = new File(Folders.CHROME_DRIVER, "chromedriver");
            else
                DEFAULT_CHROME_DRIVER = null;
        }


        public static final File SETTING_DOWNLOAD_FOLDER = new File(Folders.SETTINGS, "download_folder");
        public static final File SETTING_REMOVE_AFTER_DOWNLOAD = new File(Folders.SETTINGS, "remove_after_download");
        public static final File SETTING_DOWNLOAD_AUTOMATICALLY = new File(Folders.SETTINGS, "download_automatically");
        public static final File SETTING_SIMULTANEOUS_LIMIT = new File(Folders.SETTINGS, "simultaneous_video_limit");

        public static final File SETTING_CHROME_DRIVER = new File(Folders.SETTINGS, "chrome_driver");
        public static final File SETTING_CHROME_DRIVER_GHOST_MODE = new File(Folders.SETTINGS, "chrome_driver_ghost_mode");

        public static final File SETTING_FFMPEG = new File(Folders.SETTINGS, "ffmpeg");

        public static final File SETTING_LOGGING = new File(Folders.SETTINGS, "logging");
    }

    /** Contains the relative paths of the resources of this application. */
    public static class Resources {
        public static final String ASSETS = "assets/";
        public static final String IMAGES = "images/";
        public static final String CSS = "css/";

        public static final String CHROME_DRIVER;

        static {
            OSType os = SystemUtil.getCurrentOperatingSystemType();

            if (os == OSType.Linux)
                CHROME_DRIVER = "chromedriver/chromedriver_linux.zip";
            else if (os == OSType.Windows)
                CHROME_DRIVER = "chromedriver/chromedriver_windows.zip";
            else if (os == OSType.Mac)
                CHROME_DRIVER = "chromedriver/chromedriver_mac.zip";
            else
                CHROME_DRIVER = null;
        }
    }
}
