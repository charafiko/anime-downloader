package org.docheinstein.animedownloader.commons.utils;

import org.docheinstein.animedownloader.settings.Setting;
import org.docheinstein.animedownloader.settings.Settings;
import org.docheinstein.commons.utils.file.FileUtil;
import org.docheinstein.commons.utils.logger.DocLogger;
import org.docheinstein.animedownloader.commons.constants.Config;
import org.docheinstein.commons.utils.time.TimeUtil;
import org.docheinstein.commons.utils.zip.ZipUtil;

import java.io.File;
import java.io.IOException;

/** Contains utility method for the application. */
public class ApplicationUtil {

    private static final DocLogger L =
        DocLogger.createForClass(ApplicationUtil.class);

    /** Performs the operation needed at the startup of this application. */
    public static void init() {

        ensureFolderExistence(Config.Folders.LOGS);

        DocLogger.enableLogLevel(DocLogger.LogLevel.Verbose, true, false);
        enableLoggingOnFiles(Settings.instance().getLoggingSetting().getValue());

        Settings.instance().getLoggingSetting().addListener((setting, value) -> {
            L.debug("Logging setting is changed; updating DocLogger accordingly");
            enableLoggingOnFiles(value);
        });

        ensureFolderExistence(Config.Folders.TMP);
        ensureFolderExistence(Config.Folders.VIDEOS);
        ensureFolderExistence(Config.Folders.SETTINGS);
        ensureFolderExistence(Config.Folders.CHROME_DRIVER);

        ensureSettingsExistence();
        ensureChromeDriverExistence();
    }

    /**
     * Ensures that the given folder exists
     * @param folder the folder that has to be checked
     */
    private static void ensureFolderExistence(File folder) {
        L.debug("Ensuring that " + folder.getAbsolutePath() + " exists");

        if (!FileUtil.ensureFolderExistence(folder)) {
            L.warn("Folder creation has failed for " + folder.getAbsolutePath());
        }
    }

    /**
     * Ensures that every setting file exist
     */
    private static void ensureSettingsExistence() {
        L.debug("Ensuring that settings files exist");

        for (Setting setting : Settings.instance().getSettings()) {
            File settingFile = setting.getSettingFile();
            if (!FileUtil.ensureFileExistence(settingFile)) {
                L.warn(
                    "Settings file " + settingFile +
                        " creation failed");
            }
        }
    }

    /**
     * Ensures that the default chrome driver has been extracted from
     * the resources.
     */
    private static void ensureChromeDriverExistence() {
        L.debug("Ensuring that default chrome driver exists");

        L.debug("Chrome driver resource chosen based on the current OS: " + Config.Resources.CHROME_DRIVER);

        if (!FileUtil.exists(Config.Files.DEFAULT_CHROME_DRIVER)) {
            try {
                L.debug("Unzipping chrome driver to " +
                    Config.Folders.CHROME_DRIVER.getAbsolutePath());
                ZipUtil.unzip(
                    ResourceUtil.getResourceStream(Config.Resources.CHROME_DRIVER),
                    Config.Folders.CHROME_DRIVER
                );
            } catch (IOException e) {
                L.warn("Chrome driver unzipping failed");
            }
        }


        L.debug("Ensuring that chrome driver is executable");
        if (!Config.Files.DEFAULT_CHROME_DRIVER.canExecute()) {
            if (Config.Files.DEFAULT_CHROME_DRIVER.setExecutable(true)) {
                L.debug(Config.Files.DEFAULT_CHROME_DRIVER + " is now executable");
            }
            else {
                L.warn("Executable flag can't be set on default chrome driver");
            }
        }
    }

    /**
     * Enables/disables logging of files.
     * @param enable whether enable logging on files
     */
    private static void enableLoggingOnFiles(boolean enable) {
        if (enable)
            DocLogger.enableLoggingOnFiles(
                Config.Folders.LOGS,
                () -> TimeUtil.dateToString(TimeUtil.Patterns.DATE_CHRONOLOGICALLY_SORTABLE),
                false
            );
        else
            DocLogger.disableLoggingOnFiles();
    }
}
