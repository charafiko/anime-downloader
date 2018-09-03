package org.docheinstein.animedownloader.settings;

import java.io.File;

/**
 * Represents a single setting that wraps a value of a generic type.
 * @param <T> the type of the setting
 */
public interface Setting<T> {

    /**
     * Returns the current value of the setting.
     * @return the setting's value
     */
    T getValue();

    /**
     * Returns the default value of the setting.
     * @return the setting's default value
     */
    T getDefaultValue();

    /**
     * Returns the underlying file used to save this setting value.
     * @return the setting file
     */
    File getSettingFile();

    /**
     * Returns the value of setting by parsing the setting's file content.
     * @param fileContent the file content
     * @return the setting value
     */
    T createValueFromSettingFileContent(String fileContent);

    /**
     * Returns the string that can be saved in setting's file from the given value.
     * @param value the value of the setting
     * @return the string that represent the given value
     */
    String createSettingFileContentFromValue(T value);

    /**
     * Updates the setting with the new value.
     * @param value the new setting value
     */
    void updateSetting(T value);

    /**
     * Reloads the setting's value from the underlying file.
     */
    void reloadSetting();

    /**
     * Adds a listener that will notified when the setting value changes.
     * @param listener the listener
     */
    void addListener(SettingListener<T> listener);

    /**
     * Removes the listener from the listener set.
     * @param listener the listener to remove from the listener set
     */
    void removeListener(SettingListener<T> listener);

}
