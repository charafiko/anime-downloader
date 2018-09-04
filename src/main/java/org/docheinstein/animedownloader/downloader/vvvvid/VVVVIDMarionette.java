package org.docheinstein.animedownloader.downloader.vvvvid;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.docheinstein.animedownloader.commons.constants.Config;
import org.docheinstein.animedownloader.downloader.base.ChromeMarionetteDownloader;
import org.docheinstein.animedownloader.downloader.base.VideoDownloadObserver;
import org.docheinstein.animedownloader.settings.Settings;
import org.docheinstein.animedownloader.ui.alert.AlertInstance;
import org.docheinstein.animedownloader.video.VideoInfo;
import org.docheinstein.commons.utils.file.FileUtil;
import org.docheinstein.commons.utils.http.HttpDownloader;
import org.docheinstein.commons.utils.http.HttpRequester;
import org.docheinstein.commons.utils.logger.DocLogger;
import org.docheinstein.commons.utils.thread.ThreadUtil;
import org.docheinstein.commons.utils.types.StringUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.docheinstein.animedownloader.commons.constants.Const.Math.M;

/**
 * Specific marionette able to download video from "https://vvvvid.it".
 */
public class VVVVIDMarionette
    extends ChromeMarionetteDownloader {

    private static final DocLogger L =
        DocLogger.createForTag("{VVVVID_MARIONETTE}");

    /** Whether download is enabled. */
    private boolean mDownloadEnabled;

    /** Video info container. */
    private final VideoInfo mVideoInfo = new VideoInfo();

    /** Amount of downloaded bytes. */
    private long mDownloadedBytes = 0;

    /** Current segment in download incremental number. */
    private int mSegmentIncrementalNumber = 1;

    public VVVVIDMarionette(String downloadUrl,
                            File outputPath,
                            File driverPath,
                            boolean ghost,
                            VideoDownloadObserver downloadObserver) {
        super(downloadUrl, outputPath, driverPath, ghost, downloadObserver);
    }

    @Override
    public void startDownload() {
        mDownloadEnabled = true;

        if (!isInitialized())
            initMarionette();

        mDriver.get(mDownloadUrl);

        passRobotCheck();

        skipAds();

        mVideoInfo.title = retrieveVideoTitle();
        notifyTitleToObserver(mVideoInfo);

        // Replaces bad characters with _
        mVideoInfo.filename = mVideoInfo.title.replaceAll("\\W+", "_");

        String indexLink = getIndexFileLink();

        if (!StringUtil.isValid(indexLink)) {
            L.error("Index link can't be retrieved from network logs; giving up");
            return;
        }

        mDriver.quit();

        String indexContent = getIndexContent(indexLink);

        if (!StringUtil.isValid(indexContent)) {
            L.error("Index content is invalid; giving up");
            return;
        }

        List<String> segmentLinks = getSegmentsFromIndexContent(indexContent);

        if (!StringUtil.isValid(indexContent)) {
            L.error("Can't figure out valid segment links from index file; giving up");
            return;
        }

        File temporarySegmentsFolder =
            new File(Config.Folders.TMP, String.valueOf(System.currentTimeMillis()));

        if (!FileUtil.ensureFolderExistence(temporarySegmentsFolder)) {
            L.warn("Temporary folder can't be created");
            return;
        }

        if (doDownload(segmentLinks, temporarySegmentsFolder)) {
            String mergeFileName = mVideoInfo.filename + ".ts";

            File mergeFile = new File(temporarySegmentsFolder, mergeFileName);

            joinSegments(mergeFile, temporarySegmentsFolder);

            String outFileName = mVideoInfo.filename + ".mp4";

            convertToMP4(mergeFile, new File(mDownloadFolder, outFileName));
        }

        // Clean up temporary folder
        L.debug("Removing temporary folder (" + temporarySegmentsFolder + ")");
        FileUtil.deleteRecursive(temporarySegmentsFolder);
    }

    @Override
    public void abortDownload() {
        if (mObserver != null)
            mObserver.onVideoDownloadAborted();

        mDownloadEnabled = false;
    }

    /**
     * Passes the robot check by clicking on the #apCheckContainer element.
     */
    private void passRobotCheck() {
        if (!mDownloadEnabled)
            return;

        WebElement robotChecker = mDriver.findElement(By.id("apCheckContainer"));
        if (robotChecker != null) {
            L.debug("Clicking on #apCheckContainer");
            robotChecker.click();
        }
        else {
            L.debug("Allright, VVVVID doesn't think we're a robot");
        }
    }

    /**
     * Reloads the page until no ad is loaded and thus the video is loaded.
     * <p>
     * Another option would be wait for the ad to finish, but reload is actually
     * faster.
     */
    private void skipAds() {
        final int MAX_ATTEMPTS = 20;
        final int ESTIMATED_LOAD_MILLIS = 5000;

        for (int attemptCount = 1; attemptCount <= MAX_ATTEMPTS && mDownloadEnabled; attemptCount++) {
            L.debug("Loading attempt [" + attemptCount + "]");
            ThreadUtil.sleep(ESTIMATED_LOAD_MILLIS);

            WebElement playerInfoShow = null;
            try {
                playerInfoShow = mDriver.findElement(By.className("player-info-show"));
            }
            catch (Exception e) {}

            // The last refresh didn't lead to ads
            if (playerInfoShow != null)
                return;

            L.debug("Ads have been loaded, trying to refresh for skip those");
            mDriver.navigate().refresh();
        }
    }

    /**
     * Retrieves the video title from the page.
     * @return the video title
     */
    private String retrieveVideoTitle() {
        return mDriver.findElement(By.className("player-info-show")).getText();
    }

    /**
     * Returns the link of the index file which contains the url of every
     * segment that compose the video.
     * @return the url of the segments index file
     */
    private String getIndexFileLink() {
        if (!mDownloadEnabled)
            return null;

        L.debug("Seeking for index file url");
        LogEntries logEntries = mDriver.manage().logs().get(LogType.PERFORMANCE);
        Gson gson = new Gson();

        L.verbose("Printing log entries");

        for (LogEntry logEntry : logEntries) {
            L.verbose(logEntry.getMessage());

            JsonObject logEntryJson;

            try {
                logEntryJson = gson.fromJson(
                    logEntry.getMessage(), JsonObject.class);
            } catch (Exception e) {
                L.verbose("Can't cast log entry to valid json object");
                continue;
            }

            if (logEntryJson == null) {
                L.verbose("Can't cast log entry to valid json object");
                continue;
            }

            JsonObject messageJson = logEntryJson.getAsJsonObject("message");

            if (messageJson == null) {
                L.verbose("Log entry doesn't have a 'message' field");
                continue;
            }

            JsonObject paramsJson = messageJson.getAsJsonObject("params");

            if (paramsJson == null) {
                L.verbose("Log entry doesn't have a 'params' field");
                continue;
            }

            JsonObject requestJson = paramsJson.getAsJsonObject("request");

            if (requestJson == null) {
                L.verbose("Log entry doesn't have a 'request' field");
                continue;
            }

            JsonElement urlJson = requestJson.get("url");

            if (urlJson == null) {
                L.verbose("Log entry doesn't have a 'url' field");
                continue;
            }

            String urlString = urlJson.getAsString();

            if (!StringUtil.isValid(urlString)) {
                L.verbose("Log entry doesn't have a valid 'url' field");
                continue;
            }

            L.verbose("Found entry with url field. Value is: " + urlString);

            if (urlString.contains("index")) {
                L.debug("Found index url in logs: " + urlString);
                return urlString;
            }
        }

        return null;
    }

    /**
     * Retrieves the content of the index file at the given url
     * @param indexLink the url of the index file
     * @return the content of the index file retrieved from the given url
     */
    private String getIndexContent(String indexLink) {
        if (!mDownloadEnabled)
            return null;

        String normalizedindexLink = indexLink.replaceFirst("\\?null=0", "");
        L.debug("Going to retrieve content of index file from " + normalizedindexLink);

        HttpRequester.Response response = HttpRequester.get(normalizedindexLink).send();

        if (!response.hasBeenPerformed()) {
            L.error("Index file can't be retrieved");
            return null;
        }

        return response.getResponseBody();
    }

    /**
     * Returns the list of segment that compose the video as specified
     * by the content of the index file
     * @param indexContent the content of the index file to parse
     * @return the segments of the vide
     */
    private List<String> getSegmentsFromIndexContent(String indexContent) {
        if (!mDownloadEnabled)
            return null;

        L.debug("Reading segments from index file");
        List<String> segments = new ArrayList<>();

        Scanner scanner = new Scanner(indexContent);

        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            L.verbose(line);
            if (!line.startsWith("#"))
                segments.add(line);
        }

        return segments;
    }

    /**
     * Actually starts the download of the segments
     * @param segmentLinks the segments link
     * @param segmentsFolder the output folder for the segment
     * @return whether the download has been completed successfully
     */
    private boolean doDownload(List<String> segmentLinks, File segmentsFolder) {
        if (!mDownloadEnabled)
            return false;

        int segmentCount = segmentLinks.size();

        L.debug("Downloading " + segmentCount + " segments");

        mSegmentIncrementalNumber = 1;

        if (mObserver != null)
            mObserver.onVideoDownloadStarted();

        for (String segmentLink : segmentLinks) {
            if (!mDownloadEnabled) {
                L.debug("Download has been aborted");
                return false;
            }

            L.debug("Downloading segment: " + segmentLink);
            String segmentFilename =
                String.format("%05d", mSegmentIncrementalNumber) + ".ts";

            try {
                File segmentOutputFile  = new File(segmentsFolder, segmentFilename);

                new HttpDownloader().download(
                    segmentLink,
                    segmentOutputFile.getAbsolutePath(),
                    downloadedBytes -> {
                        long curMillis = System.currentTimeMillis();

                        mDownloadedBytes += segmentOutputFile.length();

                        int remainingSegmentCount = segmentCount - mSegmentIncrementalNumber;

                        L.verbose("Downloaded segment is " + segmentOutputFile.length() + " bytes");
                        L.verbose("Already downloaded bytes are so " + mDownloadedBytes);
                        long estimatedVideoSize =
                            remainingSegmentCount * (mDownloadedBytes / mSegmentIncrementalNumber)
                                + mDownloadedBytes;

                        L.verbose("Estimated video size: " + estimatedVideoSize);

                        mVideoInfo.size = estimatedVideoSize;

                        notifySizeToObserver(mVideoInfo);

                        if (mObserver != null)
                            mObserver.onVideoDownloadProgress(mDownloadedBytes, curMillis);
                    },
                    Integer.MAX_VALUE
                );


            } catch (IOException e) {
                L.error("Segment startDownload failed!");
                return false;
            }

            mSegmentIncrementalNumber++;
        }

        if (mObserver != null)
            mObserver.onVideoDownloadFinished();

        return true;
    }

    /**
     * Joins the segment into the given output file.
     * <p>
     * The segments will be joined in alphabetically order
     * @param outputFile the output file
     * @param segmentsFolder the folder containing the segment to join
     */
    private void joinSegments(File outputFile, File segmentsFolder) {
        File[] segments = segmentsFolder.listFiles();

        if (segments == null) {
            L.warn("No segment to join has been found");
            return;
        }

        Arrays.sort(segments, File::compareTo);

        L.debug("Joining " + segments.length + " segments into one");

        FileUtil.mergeFiles(
            outputFile,
            segments
        );
    }

    /**
     * Converts the given .ts input file to an .mp4 output file
     * @param input the .ts file
     * @param output the output .mp4 file
     */
    private void convertToMP4(File input, File output) {
        File ffmpegExecutable = Settings.instance().getFFmpegSettings().getValue();
        String ffmpegExecutableString =
            ffmpegExecutable != null ?
                ffmpegExecutable.getAbsolutePath() :
                "ffmpeg"; // Search in $PATH

        String[] mergeCommand = new String[] {
            ffmpegExecutableString,
            "-y",
            "-i", input.getAbsolutePath(),
            "-acodec", "copy",
            "-vcodec", "copy",
            output.getAbsolutePath()
        };

        L.debug("Converting " + input.getAbsolutePath() + " to MP4");
        L.debug("Conversion command: " + String.join(" ", mergeCommand));

        boolean conversionOk = false;

        try {
            L.debug("Starting conversion...");
            Runtime.getRuntime().exec(mergeCommand).waitFor();
            L.debug("Conversion finished!");

            if (FileUtil.exists(output))
                conversionOk = true;

        } catch (InterruptedException | IOException e) {}

        if (!conversionOk) {
            L.error("Conversion failed. Maybe ffmpeg is not available?");
            AlertInstance.MP4ConversionFailed.show();
        }
    }

    /**
     * Notifies the observes about the new video title.
     * @param videoInfo the title info to notify
     */
    private void notifyTitleToObserver(VideoInfo videoInfo) {
        L.debug("Video title detected: " + videoInfo.title);
        if (mObserver != null)
            mObserver.onVideoTitleDetected(videoInfo.title);
    }

    /**
     * Notifies the observes about the new video size.
     * @param videoInfo the size info to notify
     */
    private void notifySizeToObserver(VideoInfo videoInfo) {
        L.debug("Estimated video size: " + videoInfo.size / M + "MB");
        if (mObserver != null)
            mObserver.onVideoSizeDetected(videoInfo.size, false);
    }
}
