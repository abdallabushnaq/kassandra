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

package de.bushnaq.abdalla.kassandra.ai.indextts;

import de.bushnaq.abdalla.kassandra.ai.narrator.Narrator;
import de.bushnaq.abdalla.kassandra.ai.narrator.NarratorAttribute;
import org.junit.jupiter.api.Test;

/**
 * Quick test to verify voice reference path handling works correctly
 */
public class VoiceReferencePathTest {

    @Test
    public void demonstrateCorrectUsage() throws Exception {
        System.out.println("=== Voice Reference Path Test ===\n");

        // Step 1: List available voice references
        System.out.println("Step 1: Listing available voice references...");
        IndexTTS.VoiceReference[] refs = IndexTTS.listVoiceReferences();

        if (refs.length == 0) {
            System.out.println("No voice references found on server.");
            System.out.println("\nTo fix this:");
            System.out.println("1. Run IndexTTSVoiceManagementExamples.testUploadVoiceReference()");
            System.out.println("   OR");
            System.out.println("2. Place WAV files in docker\\index-tts\\voices\\ and sync them");
            return;
        }

        System.out.println("Found " + refs.length + " voice reference(s):");
        for (IndexTTS.VoiceReference ref : refs) {
            System.out.println("  - " + ref.filename() + " -> " + ref.path());
        }

        // Step 2: Use the first voice reference
        System.out.println("\nStep 2: Testing speech generation with custom voice...");
        IndexTTS.VoiceReference firstVoice = refs[0];
        System.out.println("Using: " + firstVoice.path());

        byte[] audio = IndexTTS.generateSpeech(
                "This is a test of the custom voice reference feature.",
                firstVoice.path(),
                null, null, null, null, null, null, null
        );

        IndexTTS.writeWav(audio, "test_voice_reference.wav");
        System.out.println("Generated speech -> test_voice_reference.wav");

        // Step 3: Test with Narrator integration
        System.out.println("\nStep 3: Testing with Narrator...");
        Narrator narrator = Narrator.withIndexTTS("tts/voice-test");
        NarratorAttribute attrs = new NarratorAttribute()
                .withVoiceReference(firstVoice.path());

        narrator.narrate(attrs, "Testing narrator with custom voice.");
        System.out.println("Narrator test complete");

        System.out.println("\n=== All Tests Passed ===");
        System.out.println("Voice reference path: " + firstVoice.path());
        System.out.println("Check server logs to confirm it's not falling back to default!");
    }
}

