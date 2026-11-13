/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
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

package de.bushnaq.abdalla.kassandra.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

final class LoggingRandom extends Random {
    private static final long serialVersionUID = 1L;
    private static final ThreadLocal<Recording> ACTIVE_RECORDING = new ThreadLocal<>();

    private final String source;

    LoggingRandom(String source) {
        this.source = source;
    }

    static Recording recordTo(Path path) {
        if (ACTIVE_RECORDING.get() != null) {
            throw new IllegalStateException("A random-value recording is already active on this thread");
        }
        Recording recording = new Recording(path);
        ACTIVE_RECORDING.set(recording);
        return recording;
    }

    @Override
    protected synchronized int next(int bits) {
        int value = super.next(bits);
        Recording recording = ACTIVE_RECORDING.get();
        if (recording != null) {
            recording.append(source, bits, value);
        }
        return value;
    }

    static final class Recording implements AutoCloseable {
        private final Path          path;
        private final StringBuilder values = new StringBuilder();

        private Recording(Path path) {
            this.path = path;
        }

        /**
         * Writes the captured random values to the configured file.
         *
         * @throws IOException if the file cannot be created or written
         */
        @Override
        public void close() throws IOException {
            if (ACTIVE_RECORDING.get() != this) {
                throw new IllegalStateException("Random-value recording is not active on this thread");
            }
            ACTIVE_RECORDING.remove();
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, values, StandardCharsets.UTF_8);
        }

        private synchronized void append(String source, int bits, int value) {
            values.append(source).append('\t').append(bits).append('\t').append(value).append('\n');
        }
    }
}
