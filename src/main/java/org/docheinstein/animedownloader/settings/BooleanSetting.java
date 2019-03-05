package org.docheinstein.animedownloader.settings;

import org.docheinstein.commons.types.StringUtil;

import java.io.File;

/**
 * Setting for boolean value.
 */
public class BooleanSetting extends SettingImpl<Boolean> {

    public BooleanSetting(File settingFile, Boolean defaultValue, String debugName) {
        super(settingFile, defaultValue, debugName);
    }

    @Override
    public Boolean createValueFromSettingFileContent(String fileContent) {
        return StringUtil.isValid(fileContent) ? fileContent.equals("1") : null;
    }

    @Override
    public String createSettingFileContentFromValue(Boolean value) {
        return value != null ? (value ? "1" : "0") : null;
    }
}
