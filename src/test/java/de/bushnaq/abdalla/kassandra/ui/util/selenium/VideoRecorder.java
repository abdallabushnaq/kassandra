/*
 *
 * Copyright (C) 2025-2025 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.ui.util.selenium;

import de.bushnaq.abdalla.kassandra.ai.narrator.AudioMirrorRegistry;
import de.bushnaq.abdalla.kassandra.ai.narrator.Narrator;
import lombok.Getter;
import lombok.Setter;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility class for recording screen during UI tests using JavaCV (FFmpeg)
 */
public class VideoRecorder {
    @Setter
    private       String                                                    audioDevice      = System.getProperty("videoRecorder.audioDevice", "auto");//the preferred audio loopback device name (Windows dshow, macOS avfoundation, Linux pulse/alsa).
    private       FFmpegFrameGrabber                                        audioGrabber;
    private       Thread                                                    audioThread;
    private       Rectangle                                                 captureArea;  // Optional explicit capture area
    private final boolean                                                   cbrRateControl   = Boolean.parseBoolean(System.getProperty("videoRecorder.cbr", "true"));// Video rate control config
    private       String                                                    currentTestName;
    private final int                                                       frameRate        = Integer.getInteger("videoRecorder.fps", 60);
    @Setter
    private       boolean                                                   includeAudio     = Boolean.parseBoolean(System.getProperty("videoRecorder.audioEnabled", "true"));// whether to include system audio in the recording.
    @Getter
    private       boolean                                                   isRecording      = false;
    private       long                                                      lastAudioPtsUs;
    private       long                                                      lastVideoTimestampUs;
    private final Logger                                                    logger           = LoggerFactory.getLogger(this.getClass());
    private       long                                                      mirroredAudioFramesRecorded; // sample frames (per time step), not raw samples
    private       Thread                                                    mirroredAudioThread; // thread pumping mirrored WAVs
    private final LinkedBlockingQueue<javax.sound.sampled.AudioInputStream> mirroredQueue    = new LinkedBlockingQueue<>();// Audio mirror queue (PCM frames)
    private       File                                                      outputDirectory;
    private final String                                                    queryAudioDevice = System.getProperty("videoRecorder.queryAudioDevice", "false");//if true, we will try to find an audio device, otherwise not.
    private       FFmpegFrameRecorder                                       recorder;
    private       long                                                      recordingStartNanos;// Recording metrics
    private final File                                                      rootDirectory;
    private final AtomicBoolean                                             running          = new AtomicBoolean(false);
    private       FFmpegFrameGrabber                                        screenGrabber;// JavaCV/FFmpeg members
    private final int                                                       videoBitrateKbps = Math.max(25000, Integer.getInteger("videoRecorder.videoBitrateKbps", 64000));
    private       long                                                      videoFramesRecorded;
    private       Thread                                                    videoThread;
    /**
     * -- SETTER --
     * Sets the WebDriver to use for content capture
     *
     */
    @Setter
    private       WebDriver                                                 webDriver;    // Used to compute content-only area
    private final String                                                    windowsCapture   = System.getProperty("videoRecorder.winCapture", "gdigrab");
    private final String                                                    x264Preset       = System.getProperty("videoRecorder.preset", "veryslow");

    public VideoRecorder() {
        this(new File("test-recordings"));
    }

    public VideoRecorder(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        // Create directory if it doesn't exist
        rootDirectory.mkdirs();
    }

    private void closeQuietly(FFmpegFrameGrabber grabber) {
        if (grabber == null) return;
        try {
            grabber.stop();
        } catch (Exception ignored) {
        }
        try {
            grabber.release();
        } catch (Exception ignored) {
        }
    }

    private Rectangle computeRecordingArea(boolean contentOnly) {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

        if (contentOnly && webDriver != null) {
            try {
                JavascriptExecutor jsExecutor      = (JavascriptExecutor) webDriver;
                Point              browserPosition = webDriver.manage().window().getPosition();

                Long viewportX      = (Long) jsExecutor.executeScript("return window.pageXOffset + (window.outerWidth - window.innerWidth);");
                Long viewportY      = (Long) jsExecutor.executeScript("return window.pageYOffset + (window.outerHeight - window.innerHeight);");
                Long viewportWidth  = (Long) jsExecutor.executeScript("return window.innerWidth;");
                Long viewportHeight = (Long) jsExecutor.executeScript("return window.innerHeight;");
                Long dpr            = (Long) jsExecutor.executeScript("return window.devicePixelRatio || 1.0;");

                int contentX = (int) Math.round(browserPosition.getX() + viewportX.doubleValue() * dpr - 8);
                int contentY = (int) Math.round(browserPosition.getY() + viewportY.doubleValue() * dpr - 8);
                int width    = (int) Math.round(viewportWidth.doubleValue() * dpr);
                int height   = (int) Math.round(viewportHeight.doubleValue() * dpr);

                Rectangle r = new Rectangle(contentX, contentY, width, height);
                logger.info("Recording browser content area: x={}, y={}, width={}, height={} (dpr={})", contentX, contentY, width, height, dpr);
                return r;
            } catch (Exception e) {
                logger.warn("Failed to determine browser content dimensions, falling back to browser window", e);
                return captureArea != null ? captureArea : new Rectangle(gc.getBounds());
            }
        } else {
            return captureArea != null ? captureArea : new Rectangle(gc.getBounds());
        }
    }

    private boolean initAudio() {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (queryAudioDevice.equals("false"))
                return false;
            String device = resolveWindowsAudioDevice();
            if (device == null) return false;
            audioGrabber = new FFmpegFrameGrabber(device);
            audioGrabber.setFormat("dshow");
            audioGrabber.setSampleRate(44100);
            audioGrabber.setAudioChannels(2);
            audioGrabber.start();
            return true;
        } catch (Exception e) {
            logger.warn("Failed to initialize audio grabber: {}", e.toString());
            closeQuietly(audioGrabber);
            audioGrabber = null;
            return false;
        }
    }

    private void initVideo(Rectangle area) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            String fmt = windowsCapture; // "gdigrab" or "ddagrab"
            screenGrabber = new FFmpegFrameGrabber("desktop");
            screenGrabber.setFormat(fmt);
            screenGrabber.setFrameRate(frameRate);
            screenGrabber.setOption("framerate", Integer.toString(frameRate));
            screenGrabber.setOption("draw_mouse", "1");
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            double    width      = screenSize.getWidth();
            double    height     = screenSize.getHeight();
            screenGrabber.setOption("video_size", (int) Math.min(width - Math.max(0, area.x), area.width) + "x" + (int) Math.min(height - Math.max(0, area.y), area.height));
//            screenGrabber.setOption("video_size", area.width + "x" + area.height);
            // ddagrab and gdigrab both support offsets in modern ffmpeg
            screenGrabber.setOption("offset_x", Integer.toString(Math.max(0, area.x)));
            screenGrabber.setOption("offset_y", Integer.toString(Math.max(0, area.y)));
            logger.info("Windows screen capture using {} at {} fps at {} Kbps", fmt, frameRate, videoBitrateKbps);
            logger.info("Recording browser content area: x={}, y={}, width={}, height={}", Math.max(0, area.x), Math.max(0, area.y), (int) Math.min(width, area.width), (int) Math.min(height, area.height));
            screenGrabber.start();
        } catch (Exception e) {
            closeQuietly(screenGrabber);
            throw new IOException("Failed to initialize screen grabber", e);
        }
    }

    private void pumpAudio() {
        while (running.get()) {
            try {
                Frame af = audioGrabber.grabSamples();
                if (af != null) {
                    synchronized (this) {
                        recorder.record(af);
                    }
                }
            } catch (Exception e) {
                if (running.get()) {
                    logger.warn("Audio capture error: {}", e.toString());
                }
                break;
            }
        }
    }

    private void pumpMirroredAudio() {
        while (running.get()) {
            try {
                AudioInputStream ais = mirroredQueue.take();
                if (ais == null) continue;
//                logger.info("received mirrored audio file: {}", "" + ais);
                // Target format for recorder
                AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100f, 16, 2, 4, 44100f, false);
                try {
                    if (!ais.getFormat().matches(target)) {
                        ais = AudioSystem.getAudioInputStream(target, ais);
                    }
                } catch (Exception convEx) {
                    logger.warn("Failed to convert audio stream: {}", convEx.toString());
                }

                AudioFormat fmt              = ais.getFormat();
                int         sampleRate       = (int) fmt.getSampleRate();
                int         channels         = fmt.getChannels();
                boolean     bigEndian        = fmt.isBigEndian();
                int         sampleSizeInBits = fmt.getSampleSizeInBits();

                // If no audio track, we cannot mux; drop with warning once
                if (recorder == null || recorder.getAudioChannels() == 0) {
                    logger.warn("Recorder has no audio track; mirrored audio cannot be recorded. Enable includeAudio to allocate an audio track.");
                    try {
                        ais.close();
                    } catch (Exception ignored) {
                    }
                    continue;
                }

                long clipStartUs = (System.nanoTime() - recordingStartNanos) / 1000L;

                // If there's a gap from lastAudioPtsUs to clipStartUs, insert silence
                synchronized (this) {
                    if (clipStartUs > lastAudioPtsUs) {
                        long gapUs     = clipStartUs - lastAudioPtsUs;
                        long gapFrames = (gapUs * sampleRate) / 1_000_000L; // per channel
                        if (gapFrames > 0) {
                            final int     chunkFrames = 4096; // per channel
                            final short[] zeroSamples = new short[chunkFrames * channels];
                            ShortBuffer   zeroBuf     = ShortBuffer.wrap(zeroSamples);
                            long          filled      = 0;
                            logger.info("Feeding silence audio into recorder: {} sampleRate={}, channels={}, bigEndian={}", Narrator.getElapsedNarrationTime(), sampleRate, channels, bigEndian);
                            while (running.get() && filled < gapFrames) {
                                int thisFrames = (int) Math.min(chunkFrames, gapFrames - filled);
                                zeroBuf.rewind();
                                zeroBuf.limit(thisFrames * channels);
                                recorder.setTimestamp(lastAudioPtsUs + (filled * 1_000_000L) / sampleRate);
                                recorder.recordSamples(sampleRate, channels, zeroBuf);
                                filled += thisFrames;
                            }
                            lastAudioPtsUs = clipStartUs;
                        } else {
                            // No positive gap; clamp
                            lastAudioPtsUs = Math.max(lastAudioPtsUs, clipStartUs);
                        }
                    } else {
                        // Initialize first audio PTS
                        lastAudioPtsUs = clipStartUs;
                    }
                    // Stamp to clip start before feeding clip samples
                    recorder.setTimestamp(lastAudioPtsUs);
                }

                // Read PCM bytes and feed to recorder as samples
                byte[] buffer            = new byte[8192];
                int    read;
                long   clipFramesWritten = 0;
                logger.info("Feeding mirrored audio into recorder: {} sampleRate={}, channels={}, bigEndian={}", Narrator.getElapsedNarrationTime(), sampleRate, channels, bigEndian);
                while (running.get() && (read = ais.read(buffer)) != -1) {
                    if (read <= 0) continue;
                    // Convert to little-endian 16-bit samples
                    if (sampleSizeInBits == 16 && bigEndian) {
                        for (int i = 0; i < read; i += 2) {
                            byte tmp = buffer[i];
                            buffer[i]     = buffer[i + 1];
                            buffer[i + 1] = tmp;
                        }
                    }
                    int         framesThisChunk = read / (2 * channels); // 2 bytes per sample per channel
                    ShortBuffer sb              = ShortBuffer.wrap(new short[framesThisChunk * channels]);
                    for (int i = 0; i < framesThisChunk * channels; i++) {
                        int byteIndex = i * 2;
                        int lo        = buffer[byteIndex] & 0xff;
                        int hi        = buffer[byteIndex + 1];
                        sb.put(i, (short) ((hi << 8) | lo));
                    }
                    sb.rewind();
                    synchronized (this) {
                        long tsUs = lastAudioPtsUs + (clipFramesWritten * 1_000_000L) / sampleRate;
                        recorder.setTimestamp(tsUs);
                        recorder.recordSamples(sampleRate, channels, sb);
                    }
                    clipFramesWritten += framesThisChunk;
                    mirroredAudioFramesRecorded += framesThisChunk; // per-frame (stereo accounted by channels)
                }
                // Advance lastAudioPtsUs by the duration of this clip
                lastAudioPtsUs += (clipFramesWritten * 1_000_000L) / sampleRate;
                try {
                    ais.close();
                } catch (Exception ignored) {
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                if (running.get()) logger.warn("Audio mirror error: {}", e.toString());
            }
        }
    }

    private void pumpVideo() {
        while (running.get()) {
            try {
                Frame vf = screenGrabber.grab();
                if (vf != null) {
                    long tsUs = (System.nanoTime() - recordingStartNanos) / 1000L;
                    synchronized (this) {
                        recorder.setTimestamp(tsUs);
                        recorder.record(vf);
                        videoFramesRecorded++;
                        lastVideoTimestampUs = tsUs;
                    }
                } else {
                    Thread.sleep(1);
                }
            } catch (Exception e) {
                if (running.get()) {
                    logger.warn("Video capture error: {}", e.toString());
                }
                break;
            }
        }
    }

    private String resolveGenericAudioDevice(String format) {
        if (audioDevice != null && !audioDevice.equalsIgnoreCase("auto")) {
            return audioDevice;
        }
        // We cannot enumerate devices reliably here without spawning ffmpeg; require configuration
        logger.warn("Audio device not specified for format '{}'. Set -DvideoRecorder.audioDevice=<device> to enable.", format);
        return null;
    }

    // --- Internal helpers ---

    private String resolveWindowsAudioDevice() {
        if (audioDevice != null && !audioDevice.equalsIgnoreCase("auto")) {
            return audioDevice.startsWith("audio=") ? audioDevice : ("audio=" + audioDevice);
        }
        // Try common loopback devices in order
        String[] candidates = new String[]{
                "audio=virtual-audio-capturer", // screen-capture-recorder
                "audio=Stereo Mix (Realtek(R) Audio)",
                "audio=CABLE Output (VB-Audio Virtual Cable)",
                "audio=VoiceMeeter Input (VB-Audio VoiceMeeter VAIO)"
        };
        for (String c : candidates) {
            try (FFmpegFrameGrabber probe = new FFmpegFrameGrabber(c)) {
                probe.setFormat("dshow");
                probe.setAudioChannels(2);
                probe.setSampleRate(44100);
                probe.start();
                probe.stop();
                logger.info("Using audio device: {}", c);
                return c;
            } catch (Exception ignore) {
                // try next
            }
        }
        logger.warn("No suitable Windows loopback audio device found. Set -DvideoRecorder.audioDevice=audio=<device name> to enable.");
        return null;
    }

    /**
     * Sets the area to be captured during recording
     *
     * @param x      X coordinate of the top-left corner
     * @param y      Y coordinate of the top-left corner
     * @param width  Width of the area to capture
     * @param height Height of the area to capture
     */
    public void setCaptureArea(int x, int y, int width, int height) {
        this.captureArea = new Rectangle(x, y, width, height);
    }

    /**
     * Start recording with the given test name
     *
     * @return true if recording started successfully, false if in headless mode
     */
    public boolean startRecording(String subFolderName, String testName) throws IOException, AWTException {
        return startRecording(subFolderName, testName, false);
    }

    /**
     * Start recording with the given test name
     *
     * @param subFolderName Name of the subfolder to store the video
     * @param testName      Name to use for the video file
     * @param contentOnly   If true, will attempt to capture only the webpage content
     * @return true if recording started successfully, false if in headless mode
     */
    public boolean startRecording(String subFolderName, String testName, boolean contentOnly) throws IOException, AWTException {
        Narrator.setStartTime(System.currentTimeMillis());
        this.outputDirectory = new File(rootDirectory, subFolderName);
        outputDirectory.mkdirs();

        if (SeleniumHandler.isSeleniumHeadless()) {
            logger.warn("WARNING: Running in headless mode. Video recording disabled.");
            return false; // Skip recording in headless environments
        }

        this.currentTestName = testName;

        // Determine the area to record
        Rectangle recordingArea = computeRecordingArea(contentOnly);

        // Prepare output file
        Path output = Paths.get(rootDirectory.getPath(), outputDirectory.getName(), currentTestName + ".mp4");
        Files.createDirectories(output.getParent());
        if (Files.exists(output)) {
            try {
                Files.delete(output);
            } catch (IOException e) {
                logger.warn("Could not delete existing video file: {}", output.toAbsolutePath());
            }
        }

        // Initialize recorder and grabbers
        initVideo(recordingArea);
        boolean audioInitialized = false;
        if (includeAudio) {
            audioInitialized = initAudio();
            if (!audioInitialized) {
                logger.warn("Audio capture not initialized. Proceeding with mirror-audio only.");
            }
        }

        try {
            // If includeAudio is true, always allocate an audio track so mirrored audio can be muxed even if OS loopback is absent
            boolean addAudioTrack = includeAudio || audioInitialized;
            int     channels      = audioInitialized ? audioGrabber.getAudioChannels() : 2;
            int     sampleRate    = audioInitialized ? audioGrabber.getSampleRate() : 44100;

            int audioChannels = addAudioTrack ? channels : 0;
            recorder = new FFmpegFrameRecorder(output.toFile(), recordingArea.width, recordingArea.height, audioChannels);
            recorder.setFormat("mp4");
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setFrameRate(frameRate);
            recorder.setPixelFormat(0); // yuv420p
            recorder.setVideoOption("preset", x264Preset);
            recorder.setVideoOption("tune", "zerolatency");

            // --- Rate control: CBR vs. CRF/VBR ---
            if (cbrRateControl) {
                int bps = Math.max(25000, videoBitrateKbps) * 1000; // ensure >= 25,000 kbps
                recorder.setVideoBitrate(bps);
                recorder.setVideoOption("b:v", Integer.toString(bps));
                recorder.setVideoOption("minrate", Integer.toString(bps));
                recorder.setVideoOption("maxrate", Integer.toString(bps));
                recorder.setVideoOption("bufsize", Integer.toString(bps * 2));
                // Encourage strict CBR with HRD
                recorder.setVideoOption("x264-params", "nal-hrd=cbr");
                // Prefer constant frame rate for tighter CBR control
                recorder.setVideoOption("vsync", "cfr");
                logger.info("Video rate control: CBR @ {} kbps (preset={})", bps / 1000, x264Preset);
            } else {
                // Quality-based VBR using CRF
                recorder.setVideoOption("crf", System.getProperty("videoRecorder.crf", "23"));
                recorder.setVideoOption("vsync", "vfr");
                logger.info("Video rate control: CRF={} (preset={})", System.getProperty("videoRecorder.crf", "23"), x264Preset);
            }

            if (addAudioTrack) {
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
                recorder.setAudioBitrate(192_000);
                recorder.setSampleRate(sampleRate);
                recorder.setAudioChannels(channels);
            }
            recorder.start();
        } catch (Exception e) {
            // Cleanup
            closeQuietly(screenGrabber);
            closeQuietly(audioGrabber);
            throw new IOException("Failed to start recorder", e);
        }

        videoFramesRecorded         = 0L;
        mirroredAudioFramesRecorded = 0L;
        recordingStartNanos         = System.nanoTime();
        lastAudioPtsUs              = 0;
        running.set(true);
        isRecording = true;

        // Register audio mirror to receive WAVs during test run
        AudioMirrorRegistry.set(file -> {
            try {
                AudioInputStream ais = AudioSystem.getAudioInputStream(file);
                // Attempt to fetch clip duration for diagnostics
                double clipSec = -1;
                try {
                    AudioFileFormat aff = AudioSystem.getAudioFileFormat(file);
                    AudioFormat     f   = aff.getFormat();
                    if (aff.getFrameLength() > 0 && f.getFrameRate() > 0) {
                        clipSec = aff.getFrameLength() / f.getFrameRate();
                    }
                } catch (Exception ignored) {
                }
//                if (clipSec > 0) {
//                    logger.info("received mirrored audio file: {} ({} s)", file.getAbsolutePath(), String.format("%.3f", clipSec));
//                } else {
//                    logger.info("received mirrored audio file: {}", file.getAbsolutePath());
//                }
                mirroredQueue.offer(ais);
            } catch (Exception e) {
                logger.warn("Failed to mirror audio {}: {}", file, e.toString());
            }
        });

        // Start threads
        videoThread = new Thread(this::pumpVideo, "javacv-video");
        videoThread.setDaemon(true);
        videoThread.start();
        if (includeAudio && audioGrabber != null) {
            audioThread = new Thread(this::pumpAudio, "javacv-audio");
            audioThread.setDaemon(true);
            audioThread.start();
        }
        // Start mirrored audio thread regardless; it will be no-op if no audio arrives
        mirroredAudioThread = new Thread(this::pumpMirroredAudio, "javacv-audio-mirror");
        mirroredAudioThread.setDaemon(true);
        mirroredAudioThread.start();

        logger.info("Started recording {}x{} at {} fps to {} (audio: {})", recordingArea.width, recordingArea.height, frameRate, output.toAbsolutePath(), includeAudio && audioGrabber != null);
        return true;
    }

    /**
     * Stop recording and return the recorded video file
     */
    public File stopRecording() throws IOException {
        if (!isRecording) return null;

        running.set(false);
        // Interrupt threads that may block on queue
        try {
            if (mirroredAudioThread != null) mirroredAudioThread.interrupt();
        } catch (Exception ignored) {
        }
        // Join threads
        try {
            if (videoThread != null) videoThread.join(3000);
            if (audioThread != null) audioThread.join(3000);
            if (mirroredAudioThread != null) mirroredAudioThread.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Stop recorder and grabbers
        try {
            if (recorder != null) recorder.stop();
        } catch (Exception e) {
            logger.warn("Error stopping recorder", e);
        }
        try {
            if (recorder != null) recorder.release();
        } catch (Exception ignored) {
        }
        closeQuietly(screenGrabber);
        closeQuietly(audioGrabber);

        // Unregister audio mirror to avoid leaking into other tests
        AudioMirrorRegistry.set(null);

        isRecording = false;
        Path output = Paths.get(rootDirectory.getPath(), outputDirectory.getName(), currentTestName + ".mp4");

        // Compute and log durations
        long   endNanos             = System.nanoTime();
        double wallClockSeconds     = (endNanos - recordingStartNanos) / 1_000_000_000.0;
        double videoSecondsByFrames = frameRate > 0 ? (videoFramesRecorded / (double) frameRate) : 0.0;
        double videoSecondsByTs     = lastVideoTimestampUs / 1_000_000.0;
        double mirroredAudioSeconds = mirroredAudioFramesRecorded / 44100.0; // target sample rate used in mirror
        double containerSeconds     = -1;
        try (FFmpegFrameGrabber verify = new FFmpegFrameGrabber(output.toFile())) {
            verify.start();
            long lenUs = verify.getLengthInTime();
            if (lenUs > 0) containerSeconds = lenUs / 1_000_000.0;
            verify.stop();
        } catch (Exception e) {
            logger.warn("Failed to read container duration: {}", e.toString());
        }
        logger.info("Recording saved: {}", output.toAbsolutePath());
        logger.info("Recording metrics: wallClock={} s, container={} s, videoFrames={}, videoApprox={} s, videoByTs={} s, mirroredAudioFrames={}, mirroredApprox={} s",
                String.format("%.3f", wallClockSeconds),
                containerSeconds >= 0 ? String.format("%.3f", containerSeconds) : "n/a",
                videoFramesRecorded,
                String.format("%.3f", videoSecondsByFrames),
                String.format("%.3f", videoSecondsByTs),
                mirroredAudioFramesRecorded,
                String.format("%.3f", mirroredAudioSeconds));

        return output.toFile();
    }
}
