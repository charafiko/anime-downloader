package org.docheinstein.animedownloader.settings;


/**
 * Interface used to listen to change to the setting value.
 * @param <U> the type
 */
public interface SettingListener<U> {
    void onSettingValueChanged(Setting setting, U value);
}
