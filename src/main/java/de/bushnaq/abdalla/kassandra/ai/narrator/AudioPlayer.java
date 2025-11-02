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

package de.bushnaq.abdalla.kassandra.ai.narrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.io.File;

/**
 * Handles serialized audio playback using {@link Playback} handles.
 */
public class AudioPlayer {
    private static final Logger   logger       = LoggerFactory.getLogger(AudioPlayer.class);
    private              Playback lastPlayback = null;
    private final        Object   queueLock    = new Object();

    /**
     * Queue a file for playback. Each playback waits for the previous to finish.
     * Returns the current Playback handle which can be awaited or canceled.
     */
    public Playback play(File file) {
        final Playback prevPlayback;
        final Playback current = new Playback();
        synchronized (queueLock) {
            prevPlayback = lastPlayback;
            lastPlayback = current;
        }

        Thread t = new Thread(() -> {
            try {
                if (prevPlayback != null) prevPlayback.await();

                if (current.isCanceled()) {
                    current.finishEarly();
                    return;
                }

                // Mirror the WAV to any registered sink (e.g., VideoRecorder)
                try {
                    AudioMirrorRegistry.AudioMirror mirror = AudioMirrorRegistry.get();
                    if (mirror != null) mirror.mirror(file);
                } catch (Throwable mirrorEx) {
                    logger.debug("Audio mirror ignored: {}", mirrorEx.toString());
                }
                logger.info("Playing audio file: {} - {}", Narrator.getElapsedNarrationTime(), file.getAbsolutePath());

                Clip clip = AudioSystem.getClip();
                current.setClip(clip);

                clip.addLineListener(ev -> {
                    if (ev.getType() == LineEvent.Type.STOP) {
                        current.countDown();
                    }
                });

                clip.open(AudioSystem.getAudioInputStream(file));

                if (current.isCanceled()) {
                    current.finishEarly();
                    return;
                }

                clip.start();
                current.await();
            } catch (Exception e) {
                logger.warn("Playback failed for {}", file, e);
                current.countDown();
            } finally {
                current.closeQuietly();
            }
        }, "AudioPlayer-Playback");
        t.setDaemon(true);
        t.start();
        return current;
    }
}
