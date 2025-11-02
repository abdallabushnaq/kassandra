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

import java.io.File;

/**
 * Global hook to mirror audio files being played to another sink (e.g., video recorder).
 * This allows injecting TTS audio directly into recordings without OS loopback.
 */
public final class AudioMirrorRegistry {
    private static volatile AudioMirror mirror;

    private AudioMirrorRegistry() {
    }

    public static AudioMirror get() {
        return mirror;
    }

    public static void set(AudioMirror m) {
        mirror = m;
    }

    public interface AudioMirror {
        void mirror(File audioFile);
    }
}

