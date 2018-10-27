package org.docheinstein.animedownloader.ui.main;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.docheinstein.animedownloader.commons.constants.Config;

import org.docheinstein.animedownloader.commons.constants.Resources;
import org.docheinstein.animedownloader.commons.utils.ApplicationUtil;
import org.docheinstein.commons.utils.javafx.FXUtil;


/**
 * Main application class
 */
public class MainWindow extends Application {

    private static MainWindow INSTANCE;

    private Stage mWindow;

    public MainWindow() {
        INSTANCE = this;
    }

    /**
     * Returns the unique instance of this application.
     * @return the instance of this application
     */
    public static MainWindow instance() {
        return INSTANCE;
    }

    public static void main(String[] args) {
        ApplicationUtil.init();
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        mWindow = FXUtil.showWindow(
            new MainWindowController().createNode(),
            Config.App.TITLE);
        mWindow.getIcons().setAll(Resources.UI.ICONS);
        mWindow.setMinWidth(Config.App.MIN_WIDTH);
        mWindow.setMinHeight(Config.App.MIN_HEIGHT);

    }

    /**
     * Returns the window of this application.
     * @return the main window
     */
    public Window getWindow() {
        return mWindow;
    }
}
