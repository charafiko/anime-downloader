package org.docheinstein.animedownloader.settings;

import org.docheinstein.commons.types.StringUtil;

import java.io.File;

/**
 * Setting for integer value.
 */
public class IntegerSetting extends SettingImpl<Integer> {

    public IntegerSetting(File settingFile, Integer defaultValue, String debugName) {
        super(settingFile, defaultValue, debugName);
    }

    @Override
    public Integer createValueFromSettingFileContent(String fileContent) {
        return StringUtil.isValid(fileContent) ? Integer.valueOf(fileContent) : null;
    }

    @Override
    public String createSettingFileContentFromValue(Integer value) {
        return value != null ? String.valueOf(value) : "";
    }
}
