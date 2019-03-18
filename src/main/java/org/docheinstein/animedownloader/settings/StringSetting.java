package org.docheinstein.animedownloader.settings;

import java.io.File;

/**
 * Setting for store the path of a file.
 */
public class StringSetting extends SettingImpl<String> {

    protected StringSetting(File settingFile, String defaultValue, String debugName) {
        super(settingFile, defaultValue, debugName);
    }

    @Override
    public String createValueFromSettingFileContent(String fileContent) {
        return fileContent;
    }

    @Override
    public String createSettingFileContentFromValue(String value) {
        return value;
    }
}