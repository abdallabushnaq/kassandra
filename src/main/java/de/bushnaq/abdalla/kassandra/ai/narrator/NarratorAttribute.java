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

import de.bushnaq.abdalla.kassandra.ai.chatterbox.ChatterboxTTS;
import lombok.Getter;
import lombok.Setter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Getter
@Setter
public class NarratorAttribute {
    // Static cache for voice references: filename -> full path
    private static final Map<String, String> voiceCache = new HashMap<>();
    Float cfg_weight;
    Float emotion_angry;    // Anger emotion level (0.0 to 1.0)
    Float emotion_happy;    // Happiness emotion level (0.0 to 1.0)
    Float emotion_neutral;  // Neutral emotion level (0.0 to 1.0)
    Float emotion_sad;      // Sadness emotion level (0.0 to 1.0)
    Float emotion_surprise; // Surprise emotion level (0.0 to 1.0)
    Float exaggeration;
    Float speed;            // Speech speed multiplier (0.5 to 2.0, default 1.0)
    Float temperature;
    private static boolean voiceCacheInit = false;
    String voiceReference;  // Voice reference path for cloning (e.g., "/opt/index-tts/voices/my_voice.wav")

    public NarratorAttribute() {
    }

    /**
     * Ensures the voice cache is loaded. Thread-safe lazy initialization.
     */
    private static synchronized void ensureVoiceCacheLoaded() {
        if (!voiceCacheInit) {
            refreshVoiceCache();
        }
    }

    /**
     * Helper method to format available voice names from cache for error messages
     */
    private static String getAvailableVoiceNamesFromCache() {
        if (voiceCache.isEmpty()) {
            return "(none)";
        }
        return String.join(", ", voiceCache.keySet());
    }

    /**
     * Refreshes the voice cache by querying the Index TTS server.
     * Call this if voices are added or removed while the application is running.
     */
    public static synchronized void refreshVoiceCache() {
        try {
            voiceCache.clear();
            ChatterboxTTS.VoiceReference[] refs = ChatterboxTTS.listVoiceReferences();
            for (ChatterboxTTS.VoiceReference ref : refs) {
                voiceCache.put(ref.filename(), ref.path());
            }
            voiceCacheInit = true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load voice references from Index TTS server: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a string representation of all non-null attributes suitable for hashing.
     * Uses consistent formatting for floating-point values.
     *
     * @return formatted string with all non-null attributes for hash computation
     */
    public String toHashString() {
        DecimalFormat df = new DecimalFormat("0.########", DecimalFormatSymbols.getInstance(Locale.ROOT));
        StringBuilder sb = new StringBuilder();

        if (temperature != null) {
            sb.append("|temp=").append(df.format(temperature));
        }
        if (exaggeration != null) {
            sb.append("|ex=").append(df.format(exaggeration));
        }
        if (cfg_weight != null) {
            sb.append("|cfg=").append(df.format(cfg_weight));
        }
        if (speed != null) {
            sb.append("|speed=").append(df.format(speed));
        }
        if (emotion_angry != null) {
            sb.append("|angry=").append(df.format(emotion_angry));
        }
        if (emotion_happy != null) {
            sb.append("|happy=").append(df.format(emotion_happy));
        }
        if (emotion_sad != null) {
            sb.append("|sad=").append(df.format(emotion_sad));
        }
        if (emotion_surprise != null) {
            sb.append("|surprise=").append(df.format(emotion_surprise));
        }
        if (emotion_neutral != null) {
            sb.append("|neutral=").append(df.format(emotion_neutral));
        }
        if (voiceReference != null) {
            sb.append("|voice=").append(voiceReference);
        }

        return sb.toString();
    }

    /**
     * Formats attributes for logging or display, showing only non-null values.
     *
     * @return formatted string with all non-null attributes
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (temperature != null) sb.append("temp=").append(temperature).append(" ");
        if (exaggeration != null) sb.append("ex=").append(exaggeration).append(" ");
        if (cfg_weight != null) sb.append("cfg=").append(cfg_weight).append(" ");
        if (speed != null) sb.append("speed=").append(speed).append(" ");
        if (emotion_angry != null) sb.append("angry=").append(emotion_angry).append(" ");
        if (emotion_happy != null) sb.append("happy=").append(emotion_happy).append(" ");
        if (emotion_sad != null) sb.append("sad=").append(emotion_sad).append(" ");
        if (emotion_surprise != null) sb.append("surprise=").append(emotion_surprise).append(" ");
        if (emotion_neutral != null) sb.append("neutral=").append(emotion_neutral).append(" ");
        if (voiceReference != null) sb.append("voice=").append(voiceReference).append(" ");
        return sb.toString().trim();
    }

    public NarratorAttribute withAngry(Float level) {
        this.emotion_angry = level;
        return this;
    }

    public NarratorAttribute withCfgWeight(Float cfgWeight) {
        this.cfg_weight = cfgWeight;
        return this;
    }

    public NarratorAttribute withExaggeration(Float exaggeration) {
        this.exaggeration = exaggeration;
        return this;
    }

    public NarratorAttribute withHappy(Float level) {
        this.emotion_happy = level;
        return this;
    }

    public NarratorAttribute withNeutral(Float level) {
        this.emotion_neutral = level;
        return this;
    }

    public NarratorAttribute withSad(Float level) {
        this.emotion_sad = level;
        return this;
    }

    public NarratorAttribute withSpeed(Float speed) {
        this.speed = speed;
        return this;
    }

    public NarratorAttribute withSurprise(Float level) {
        this.emotion_surprise = level;
        return this;
    }

    public NarratorAttribute withTemperature(Float temperature) {
        this.temperature = temperature;
        return this;
    }

    /**
     * Convenience method to set voice by filename instead of full path.
     * Looks up the voice reference from the cached list (loaded on first use).
     *
     * @param filename The WAV filename (e.g., "chatterbox.wav" or just "chatterbox")
     * @return this NarratorAttribute for method chaining
     * @throws RuntimeException if the voice file is not found on the server
     */
    public NarratorAttribute withVoice(String filename) {
        // Ensure voice cache is initialized
        ensureVoiceCacheLoaded();

        // Add .wav extension if not present
        String searchFilename = filename.endsWith(".wav") ? filename : filename + ".wav";

        // Look up in cache
        String path = voiceCache.get(searchFilename);
        if (path != null) {
            this.voiceReference = path;
            return this;
        }

        // Not found - throw exception with helpful message
        throw new RuntimeException("Voice file '" + searchFilename + "' not found. " +
                "Available voices: " + getAvailableVoiceNamesFromCache());
    }

    public NarratorAttribute withVoiceReference(String voiceReference) {
        this.voiceReference = voiceReference;
        return this;
    }
}
