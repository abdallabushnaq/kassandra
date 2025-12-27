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

import de.bushnaq.abdalla.kassandra.ai.SyncResult;
import de.bushnaq.abdalla.kassandra.ai.TtsEngine;
import de.bushnaq.abdalla.kassandra.ai.chatterbox.ChatterboxTTS;
import de.bushnaq.abdalla.kassandra.ai.indextts.IndexTTS;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Narrator orchestrates text-to-speech synthesis and audio playback.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Builds a canonical name for the text + parameters via {@link TtsCacheManager#buildFileName(String, NarratorAttribute)}</li>
 *   <li>Requests a chronological target path from {@link TtsCacheManager#prepareChronological(String)}</li>
 *   <li>Synthesizes audio only when the current id is not up-to-date, then writes it via {@link TtsCacheManager#writeChronological(byte[], Path)}</li>
 *   <li>Queues playback with {@link AudioPlayer}</li>
 * </ul>
 */
public class Narrator {

    private static final Logger            logger = LoggerFactory.getLogger(Narrator.class);
    private final        AudioPlayer       audioPlayer;  // playback queue/handles
    private static       TtsCacheManager   cacheManager; // chronological file coordinator, static to support mixing narrators
    @Getter
    @Setter
    private              NarratorAttribute defaultAttributes; // default TTS attributes for this narrator
    @Getter
    private volatile     Playback          playback; // most recently scheduled playback for external access
    @Setter
    private              boolean           silent = false;
    @Getter
    @Setter
    private static       long              startTime;//used by VideoRecorder to sync time with audio playing (only used to log the time)
    private final        TtsEngine         ttsEngine;    // synthesis strategy
    @Getter
    private final        String            voiceReference; // narrator-level voice; may be null

    /**
     * Creates a Narrator storing audio under {@code relativeFolder} and using the default TTS engine.
     */
    public Narrator(String relativeFolder) throws Exception {
        this(relativeFolder, chatterboxTtsEngine(), null);
    }

    /**
     * Creates a Narrator storing audio under {@code relativeFolder} and using the default TTS engine and a provided voice.
     */
    public Narrator(String relativeFolder, String voiceReference) throws Exception {
        this(relativeFolder, chatterboxTtsEngine(), voiceReference);
    }

    /**
     * Creates a Narrator storing audio under {@code relativeFolder} and using a provided {@link TtsEngine}.
     *
     * @param relativeFolder output directory for chronological WAV files
     * @param engine         TTS engine implementation used to synthesize audio
     */
    public Narrator(String relativeFolder, TtsEngine engine) {
        this(relativeFolder, engine, null);
    }

    /**
     * Creates a Narrator storing audio under {@code relativeFolder}, using a provided {@link TtsEngine} and an optional voice.
     *
     * @param relativeFolder output directory for chronological WAV files
     * @param engine         TTS engine implementation used to synthesize audio
     * @param voiceReference optional voice reference to be applied for synthesis (may be null)
     */
    public Narrator(String relativeFolder, TtsEngine engine, String voiceReference) {
        Path audioDir = Path.of(relativeFolder);
        if (cacheManager == null)
            cacheManager = new TtsCacheManager(audioDir);
        this.audioPlayer       = new AudioPlayer();
        this.ttsEngine         = engine;
        this.defaultAttributes = new NarratorAttribute().withTemperature(0.5f).withCfgWeight(1.0f).withExaggeration(0.5f);
        this.voiceReference    = voiceReference; // may be null
    }

    private static TtsEngine chatterboxTtsEngine() throws Exception {
//        return (text, attrs) -> ChatterboxTTS.generateSpeech(
//                text,
//                attrs.getTemperature() != null ? attrs.getTemperature() : 0.5f,
//                attrs.getExaggeration() != null ? attrs.getExaggeration() : 0.5f,
//                attrs.getCfg_weight() != null ? attrs.getCfg_weight() : 1.0f
//        );
        ChatterboxTTS chatterboxTTS = new ChatterboxTTS();
        SyncResult    syncResult    = chatterboxTTS.syncVoiceReferences("docker\\chatterbox\\voices");
        chatterboxTTS.logSyncResult(syncResult);
        return chatterboxTTS;
    }

    // Helper to create a shallow copy of attributes and inject the narrator-level voice if present.
    private NarratorAttribute effectiveAttributes(NarratorAttribute attrs) {
        NarratorAttribute e = new NarratorAttribute();
        e.setTemperature(attrs.getTemperature());
        e.setExaggeration(attrs.getExaggeration());
        e.setCfg_weight(attrs.getCfg_weight());
        e.setSpeed(attrs.getSpeed());
        e.setEmotion_angry(attrs.getEmotion_angry());
        e.setEmotion_happy(attrs.getEmotion_happy());
        e.setEmotion_sad(attrs.getEmotion_sad());
        e.setEmotion_surprise(attrs.getEmotion_surprise());
        e.setEmotion_neutral(attrs.getEmotion_neutral());
        // Voice precedence: narrator-level voice (if set) overrides any attr-level voice.
        if (this.voiceReference != null && !this.voiceReference.isEmpty()) {
            e.setVoiceReference(ttsEngine.voiceToPath(this.voiceReference));
        } else {
            e.setVoiceReference(attrs.getVoiceReference());
        }
        return e;
    }

    public static String getElapsedNarrationTime() {
        long now          = System.currentTimeMillis();
        long elapsedMs    = now - Narrator.getStartTime();
        long secondsTotal = elapsedMs / 1000;
        long hours        = secondsTotal / 3600;
        long minutes      = (secondsTotal % 3600) / 60;
        long seconds      = secondsTotal % 60;
        // Return directly without redundant local variable
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Creates TTS engine for Index TTS with specific voice
     */
    private static TtsEngine indexTtsEngine() throws Exception {
//        return (text, attrs) -> {
//            String voiceReference  = attrs.getVoiceReference();
//            Float  speed           = attrs.getSpeed();
//            Float  emotionAngry    = attrs.getEmotion_angry();
//            Float  emotionHappy    = attrs.getEmotion_happy();
//            Float  emotionSad      = attrs.getEmotion_sad();
//            Float  emotionSurprise = attrs.getEmotion_surprise();
//            Float  emotionNeutral  = attrs.getEmotion_neutral();
//            Float  temperature     = attrs.getTemperature();
//
//            return IndexTTS.generateSpeech(text, voiceReference, speed,
//                    emotionAngry, emotionHappy, emotionSad,
//                    emotionSurprise, emotionNeutral, temperature);
//        };
        IndexTTS indexTts = new IndexTTS();
        indexTts.syncVoiceReferences("docker\\chatterbox\\voices");
        return indexTts;

    }

    /**
     * Sleeps the current thread for roughly half a second.
     */
    public void longPause() {
        pause(1000);
    }

    /**
     * Synchronously synthesize and play using per-call attributes. Blocks until playback finishes.
     */
    public Narrator narrate(NarratorAttribute attrs, String text) throws Exception {
        if (!silent) {
            narrateAsync(attrs, text);
            getPlayback().await();
        }
        return this;
    }

    /**
     * Synchronously synthesize and play the given text using current instance defaults.
     * Blocks until playback finishes.
     */
    public Narrator narrate(String text) throws Exception {
        if (!silent) {
            narrateAsync(text);
            getPlayback().await();
        }
        return this;
    }

    /**
     * Asynchronously synthesize and queue playback using per-call attributes.
     * The provided attributes are used directly without merging with defaults.
     */
    public Narrator narrateAsync(NarratorAttribute attrs, String text) throws Exception {
        if (!silent) {

            return narrateResolved(attrs, text);
        }
        return this;
    }

    /**
     * Asynchronously synthesize and queue playback using instance defaults.
     * Returns immediately with a {@link Playback} handle available via {@link #getPlayback()}.
     */
    public Narrator narrateAsync(String text) throws Exception {
        if (!silent) {
            return narrateResolved(defaultAttributes, text);
        }
        return this;
    }

    /**
     * Core flow:
     * <ol>
     *   <li>Build canonical name containing sanitized text + hash.</li>
     *   <li>Ask {@link TtsCacheManager} for the next id's plan.</li>
     *   <li>If up-to-date, reuse the file; otherwise synthesize and write to the plan path.</li>
     *   <li>Queue playback and expose the {@link Playback} handle.</li>
     * </ol>
     */
    private Narrator narrateResolved(NarratorAttribute attrs, String text) throws Exception {
        // Ensure the effective attributes include the narrator-level voice for hashing and synthesis
        NarratorAttribute eff = effectiveAttributes(attrs);

        String                     canonicalName = cacheManager.buildFileName(text, eff);
        TtsCacheManager.ChronoPlan plan          = cacheManager.prepareChronological(canonicalName);

        Path pathToPlay;
        if (plan.upToDate()) {
            // Already matches; reuse
            pathToPlay = plan.path();
            logger.debug("Narration up-to-date at {}", pathToPlay.getFileName());
        } else {

            long t0 = System.nanoTime();
            logger.info("TTS generate start: attrs={}, file={}, text=\"{}\"", eff, plan.path().getFileName(), text);

            byte[] audio = ttsEngine.synthesize(text, eff);

            cacheManager.writeChronological(audio, plan.path());
            long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
            logger.info("TTS generate done:   attrs={}, file={}, bytes={}, took={} ms", eff, plan.path().getFileName(), audio.length, tookMs);
            pathToPlay = plan.path();
        }

        File fileToPlay = cacheManager.toFile(pathToPlay);
        this.playback = audioPlayer.play(fileToPlay);
        return this;
    }

    /**
     * Sleeps the current thread for roughly one second.
     */
    public void pause() {
        logger.trace("Pausing for 500 ms.");
        pause(500);
    }

    /**
     * Sleeps the current thread for the specified duration.
     * A silenced narrator will skip the pause.
     *
     * @param millis duration in milliseconds
     */
    public void pause(long millis) {
        if (!silent) {
            try {
                logger.trace("Pausing for {} ms.", millis);
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                logger.trace(e.getMessage(), e);
            }
        }
    }

    /**
     * for debugging purposes only, has an effect if narrator is set to silent
     *
     * @param millis duration in milliseconds
     */
    public void pauseIfSilent(long millis) {
        if (silent) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                logger.trace(e.getMessage(), e);
            }
        }
    }

    public static void resetCache() {
        cacheManager.reset();
    }

    public static Narrator withChatterboxTTS(String relativeFolder) throws Exception {
        return new Narrator(relativeFolder, chatterboxTtsEngine(), "christopher");
    }

    public static Narrator withChatterboxTTS(String relativeFolder, String voiceReference) throws Exception {
        return new Narrator(relativeFolder, chatterboxTtsEngine(), voiceReference);
    }

    /**
     * Creates a Narrator using Index TTS engine with default voice
     */
    public static Narrator withIndexTTS(String relativeFolder) throws Exception {
        return new Narrator(relativeFolder, indexTtsEngine(), "christopher");
    }

    /**
     * Creates a Narrator using Index TTS engine with a provided voice
     */
    public static Narrator withIndexTTS(String relativeFolder, String voiceReference) throws Exception {
        return new Narrator(relativeFolder, indexTtsEngine(), voiceReference);
    }

}
