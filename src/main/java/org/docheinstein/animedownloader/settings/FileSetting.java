package org.docheinstein.animedownloader.settings;

import org.docheinstein.commons.utils.types.StringUtil;

import java.io.File;

/**
 * Setting for store the path of a file.
 */
public class FileSetting extends SettingImpl<File> {

    public FileSetting(File settingFile, File defaultValue, String debugName) {
        super(settingFile, defaultValue, debugName);
    }

    @Override
    public File createValueFromSettingFileContent(String fileContent) {
        return StringUtil.isValid(fileContent) ? new File(fileContent) : null;
    }

    @Override
    public String createSettingFileContentFromValue(File value) {
        return value != null ? value.getAbsolutePath() : "";
    }
}
