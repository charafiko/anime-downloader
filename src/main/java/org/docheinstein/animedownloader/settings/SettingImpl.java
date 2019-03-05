package org.docheinstein.animedownloader.settings;

import org.docheinstein.commons.file.FileUtil;
import org.docheinstein.commons.logger.DocLogger;
import org.docheinstein.commons.types.StringUtil;

import java.io.File;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * A convenient implementation for {@link Setting}.
 * @param <T> the type of the setting
 */
public abstract class SettingImpl<T> implements Setting<T>{

    private static final DocLogger L =
        DocLogger.createForClass(SettingImpl.class);

    /** Current value of the setting. */
    private T mValue;

    /** Listeners of the setting value. */
    private final Set<SettingListener<T>> mListeners = new CopyOnWriteArraySet<>();

    /** Name of the setting used only for debug purpose. */
    private final String mDebugName;

    /** Default value. */
    private final T mDefaultValue;

    /** Underlying setting file. */
    private final File mSettingFile;

    protected SettingImpl(File settingFile, T defaultValue, String debugName) {
        mSettingFile = settingFile;
        mDefaultValue = defaultValue;
        mDebugName = debugName;
    }

    @Override
    public T getValue() {
        return mValue;
    }

    @Override
    public File getSettingFile() {
        return mSettingFile;
    }

    @Override
    public T getDefaultValue() {
        return mDefaultValue;
    }

    @Override
    public void updateSetting(T value) {
        L.info("Updating setting; '" + mDebugName + "' = '" + value + "'");

        FileUtil.write(
            mSettingFile.getAbsolutePath(),
            createSettingFileContentFromValue(value)
        );

        reloadSetting();
    }

    @Override
    public void reloadSetting() {
        L.debug("Reloading setting '" + mDebugName + "' value");

        mValue = mDefaultValue;

        if (FileUtil.exists(mSettingFile)) {
            String fileContent = FileUtil.readFile(mSettingFile);
            if (StringUtil.isValid(fileContent)) {
                L.debug("Value actually loaded from setting file: " + fileContent);
                // Load the setting from the setting file content
                mValue = createValueFromSettingFileContent(fileContent);
            }
        }

        L.debug("Setting value '" + mDebugName + "' = '" + mValue + "'");

        mListeners.forEach(l -> l.onSettingValueChanged(this, mValue));
    }

    @Override
    public void addListener(SettingListener<T> listener) {
        if (listener != null)
            mListeners.add(listener);
    }

    @Override
    public void removeListener(SettingListener<T> listener) {
        mListeners.remove(listener);
    }
}
