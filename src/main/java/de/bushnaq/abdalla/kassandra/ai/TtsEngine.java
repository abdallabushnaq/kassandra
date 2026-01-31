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

package de.bushnaq.abdalla.kassandra.ai;

import de.bushnaq.abdalla.kassandra.ai.narrator.NarratorAttribute;

/**
 * TtsEngine defines an abstraction for text-to-speech synthesis.
 * Implementations must return a WAV byte array for the provided input.
 */
public interface TtsEngine {
    void logSyncResult(String audioDirectory, SyncResult result) throws Exception;

    SyncResult syncVoiceReferences(String localVoicesDir) throws Exception;

    /**
     * Synthesizes speech for the provided text and attributes.
     *
     * @param text       input text
     * @param attributes engine-specific attributes (temperature, exaggeration, etc.)
     * @return WAV audio bytes
     * @throws Exception on synthesis failure
     */
    byte[] synthesize(String text, NarratorAttribute attributes) throws Exception;

    String voiceToPath(String voice);

}

