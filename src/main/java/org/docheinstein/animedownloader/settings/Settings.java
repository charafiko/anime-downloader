package org.docheinstein.animedownloader.settings;

import org.docheinstein.animedownloader.commons.constants.Config;

import java.io.File;

/**
 * Contains all the settings used by the application.
 */
public class Settings {

    public enum AutomaticDownloadStrategy {
        Static("static"),
        Adaptive("adaptive")
        ;

        private String mName;

        AutomaticDownloadStrategy(String name) {
            mName = name;
        }

        public static AutomaticDownloadStrategy fromName(String name) {
            if (name == null)
                return null;
            switch (name) {
                case "static":
                    return Static;
                case "adaptive":
                    return Adaptive;
                default:
                    return null;
            }
        }

        public String getName() {
            return mName;
        }

    }

    private static final Settings INSTANCE = new Settings();

    private FileSetting mDownloadFolder = new FileSetting(
        Config.Files.SETTING_DOWNLOAD_FOLDER,
        new File("."),
        "download_folder"
    );

    private BooleanSetting mRemoveAfterDownload = new BooleanSetting (
        Config.Files.SETTING_REMOVE_AFTER_DOWNLOAD,
        false,
        "remove_after_download"
    );

    private BooleanSetting mDownloadAutomatically = new BooleanSetting(
        Config.Files.SETTING_DOWNLOAD_AUTOMATICALLY,
        false,
        "download_automatically"
    );

    private Setting<AutomaticDownloadStrategy> mAutomaticDownloadStrategy =
        new SettingImpl<AutomaticDownloadStrategy>(
            Config.Files.SETTING_AUTOMATIC_DOWNLOAD_STRATEGY,
            AutomaticDownloadStrategy.Static,
            "automatic_download_strategy"
        ) {
            @Override
            public AutomaticDownloadStrategy createValueFromSettingFileContent(String fileContent) {
                return AutomaticDownloadStrategy.fromName(fileContent);
            }

            @Override
            public String createSettingFileContentFromValue(AutomaticDownloadStrategy value) {
                return value == null ? null : value.getName();
            }
        };

    private IntegerSetting mSimultaneousVideoLimit = new IntegerSetting(
        Config.Files.SETTING_SIMULTANEOUS_LIMIT,
        1,
        "simultaneous_video_limit"
    );

    private BooleanSetting mSimultaneousVideoLimitForEachProvider = new BooleanSetting(
        Config.Files.SETTING_SIMULTANEOUS_LIMIT_FOR_EACH_PROVIDER,
        true,
        "simultaneous_video_limit_for_each_provider"
    );

    private IntegerSetting mBandwidthLimit = new IntegerSetting(
        Config.Files.SETTING_BANDWIDTH_LIMIT,
        1,
        "bandwidth_limit"
    );

    // Executables path

    private FileSetting mChromeDriver = new FileSetting(
        Config.Files.SETTING_CHROME_DRIVER,
        Config.Files.DEFAULT_CHROME_DRIVER,
        "chrome_driver");


    private FileSetting mFFmpeg = new FileSetting(
        Config.Files.SETTING_FFMPEG,
        null,
        "ffmpeg");

    // Debug

    private BooleanSetting mChromeDriverGhostMode = new BooleanSetting(
        Config.Files.SETTING_CHROME_DRIVER_GHOST_MODE,
        true,
        "chrome_driver_ghost_mode"
    );

    private BooleanSetting mLoggingSetting = new BooleanSetting(
        Config.Files.SETTING_LOGGING,
        false,
        "logging"
    );

    private BooleanSetting mFlushSetting = new BooleanSetting(
        Config.Files.SETTING_FLUSH,
        false,
        "flush"
    );

    private final Setting[] mSettings = new Setting[] {
        mDownloadFolder, mRemoveAfterDownload, mDownloadAutomatically,
        mAutomaticDownloadStrategy,
        mSimultaneousVideoLimit,
        mSimultaneousVideoLimitForEachProvider,
        mBandwidthLimit,
        mChromeDriver, mChromeDriverGhostMode,
        mFFmpeg, mLoggingSetting, mFlushSetting
    };

    /**
     * Returns the unique instance of this class.
     * @return the instance of this class.
     */
    public static Settings instance() {
        return INSTANCE;
    }

    private Settings() {
        for (Setting setting : getSettings())
            setting.reloadSetting();
    }

    /**
     * Returns all the setting handled by this application.
     * <p>
     * This is convenient for do the same action on multiple settings.
     * @return all the setting
     */
    public Setting[] getSettings() {
        return mSettings;
    }

    /**
     * Returns the setting that reminds the download folder path.
     * @return the download folder setting
     */
    public Setting<File> getDownloadFolderSetting() {
        return mDownloadFolder;
    }

    /**
     * Returns the setting that reminds whether the download should be
     * removed from the list after the download.
     * @return the remove after download setting
     */
    public Setting<Boolean> getRemoveAfterDownloadSetting() {
        return mRemoveAfterDownload;
    }

    /**
     * Returns the setting that reminds whether video should be automatically
     * downloaded when pasted or when another video ends and starts a new
     * download doesn't exceed the simultaneous download limit.
     * @return the download automatically setting
     */
    public Setting<Boolean> getDownloadAutomaticallySetting() {
        return mDownloadAutomatically;
    }

    /**
     * Returns the setting that reminds the download strategy
     * @return the download strategy setting
     */
    public Setting<AutomaticDownloadStrategy> getAutomaticDownloadStrategySetting() {
        return mAutomaticDownloadStrategy;
    }

    /**
     * Returns the setting that reminds the limit of simultaneous download.
     * <p>
     * Is valid only if the download strategy is set to 'Static'.
     * @return the simultaneous video limit setting
     */
    public Setting<Integer> getSimultaneousVideoLimitSetting() {
        return mSimultaneousVideoLimit;
    }

    /**
     * Returns the setting that reminds whether the simultaneous video limit
     * is referred to each provider or is global.
     * <p>
     * Is valid only if the download strategy is set to 'Static'.
     * @return the simultaneous video limit setting
     */
    public Setting<Boolean> getSimultaneousVideoForEachProvider() {
        return mSimultaneousVideoLimitForEachProvider;
    }

    /**
     * Returns the setting that reminds the bandwidth limit for download
     * expressed in bytes per seconds.
     * <p>
     * Is valid only if the download strategy is set to 'Adaptive'.
     * @return the expected bandwidth setting
     */
    public Setting<Integer> getBandwidthLimit() {
        return mBandwidthLimit;
    }

    /**
     * Returns the setting that reminds the path of the chrome driver
     * @return the chrome driver setting
     */
    public Setting<File> getChromeDriverSetting() {
        return mChromeDriver;
    }

    /**
     * Returns the setting that reminds whether chrome driver should be started
     * in ghost mode.
     * @return the chrome driver ghost mode setting
     */
    public Setting<Boolean> getChromeDriverGhostModeSetting() {
        return mChromeDriverGhostMode;
    }

    /**
     * Returns the setting that reminds the path of the ffmpeg executable or
     * null if it should be supposed to be available from the current path.
     * @return the ffmpeg setting
     */
    public Setting<File> getFFmpegSettings() {
        return mFFmpeg;
    }

    /**
     * Returns the setting that reminds whether logging on files should be enabled.
     * @return the logging setting
     */
    public Setting<Boolean> getLoggingSetting() {
        return mLoggingSetting;
    }

    /**
     * Returns the setting that reminds whether flush after every message.
     * @return the logging setting
     */
    public Setting<Boolean> getFlushSetting() {
        return mFlushSetting;
    }
}
