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

import javax.sound.sampled.Clip;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Playback represents an active or pending audio playback. It allows callers to
 * await completion or cancel playback. Instances are created by {@link Narrator}.
 */
public class Playback {
    private volatile boolean        canceled = false;
    private volatile Clip           clip;
    private volatile boolean        closed   = false;
    private final    CountDownLatch done     = new CountDownLatch(1);

    // Package-private constructor; Narrator creates instances
    Playback() {
    }

    /**
     * Blocks until playback finishes, then closes the underlying audio resources.
     *
     * @throws InterruptedException if the waiting thread is interrupted
     */
    public void await() throws InterruptedException {
        done.await();
        closeQuietly();
    }

    /**
     * Blocks until playback finishes or the given timeout elapses.
     * Closes the underlying audio resources on successful completion.
     *
     * @param timeout maximum time to wait
     * @param unit    time unit of the timeout
     * @return true if playback finished within the timeout; false otherwise
     * @throws InterruptedException if the waiting thread is interrupted
     */
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        boolean finished = done.await(timeout, unit);
        if (finished) closeQuietly();
        return finished;
    }

    void closeQuietly() {
        if (!closed) {
            closed = true;
            Clip c = this.clip;
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    void countDown() {
        if (done.getCount() > 0) done.countDown();
    }

    void finishEarly() {
        countDown();
        closeQuietly();
    }

    boolean isCanceled() {
        return canceled;
    }

    /**
     * @return true if playback already finished.
     */
    public boolean isDone() {
        return done.getCount() == 0;
    }

    void setClip(Clip clip) {
        this.clip = clip;
    }

    /**
     * Requests playback to stop as soon as possible.
     * If a clip is already open, it is stopped; otherwise the handle is completed immediately.
     */
    public void stop() {
        canceled = true;
        Clip c = this.clip;
        if (c != null) {
            try {
                c.stop();
            } catch (Exception ignored) {
            }
        } else {
            countDown();
            closeQuietly();
        }
    }
}
