package org.docheinstein.animedownloader.commons.constants;

import javafx.scene.image.Image;
import org.docheinstein.animedownloader.commons.utils.ResourceUtil;
import org.docheinstein.commons.utils.javafx.FXUtil;

/** Contains preloaded resources. */
public class Resources {

    /** Contains resource of the UI (e.g. images) */
    public static class UI {

        public static final Image ICONS[];
        public static final Image START;
        public static final Image STOP;
        public static final Image OPEN_FOLDER;
        public static final Image OPENLOAD;
        public static final Image VVVVID;

        static {
            ICONS = new Image[] {
                FXUtil.createImage(
                    ResourceUtil.getImageStream("logo.png"))
            };

            START = FXUtil.createImage(
                ResourceUtil.getImageStream("start.png"));

            STOP = FXUtil.createImage(
                ResourceUtil.getImageStream("stop.png"));

            OPEN_FOLDER = FXUtil.createImage(
                ResourceUtil.getImageStream("folder.png"));

            OPENLOAD = FXUtil.createImage(
                ResourceUtil.getImageStream("openload.png"));

            VVVVID = FXUtil.createImage(
                ResourceUtil.getImageStream("vvvvid.jpg"));
        }

    }
}
