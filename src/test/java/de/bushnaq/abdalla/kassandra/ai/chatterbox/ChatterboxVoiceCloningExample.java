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

package de.bushnaq.abdalla.kassandra.ai.chatterbox;

/**
 * Example demonstrating voice cloning with Chatterbox TTS
 * <p>
 * This example shows how to:
 * 1. Upload voice reference files for cloning
 * 2. List available voice references
 * 3. Generate speech using a specific voice reference
 * 4. Delete voice references
 */
public class ChatterboxVoiceCloningExample {

    public static void main(String[] args) {
        try {
            // Set service URL (optional, defaults to http://localhost:4123)
            ChatterboxTTS.setServiceUrl("http://localhost:4123");

            System.out.println("=== Chatterbox Voice Cloning Example ===\n");

            // Example 1: List available voice references
            System.out.println("1. Listing available voice references:");
            ChatterboxTTS.VoiceReference[] voices = ChatterboxTTS.listVoiceReferences();
            if (voices.length == 0) {
                System.out.println("   No voice references found on server.");
            } else {
                for (ChatterboxTTS.VoiceReference voice : voices) {
                    System.out.println("   - " + voice);
                }
            }
            System.out.println();

            // Example 2: Upload a voice reference (if you have a WAV file)
            // Uncomment and provide your own WAV file path
            /*
            System.out.println("2. Uploading voice reference:");
            String localWavFile = "path/to/your/voice_sample.wav";
            ChatterboxTTS.VoiceReference uploaded = ChatterboxTTS.uploadVoiceReference(localWavFile);
            System.out.println("   Uploaded: " + uploaded);
            System.out.println();
            */

            // Example 3: Generate speech with default voice (English)
            System.out.println("3. Generating speech with default voice:");
            String text      = "Hello! This is a test of the Chatterbox text-to-speech system.";
            byte[] audioData = ChatterboxTTS.generateSpeech(text, 0.7f, 1.0f, 3.0f);
            ChatterboxTTS.writeWav(audioData, "test-default-voice.wav");
            System.out.println("   Generated: test-default-voice.wav");
            System.out.println();

            // Example 4: Generate speech with voice cloning
            // Uncomment if you have uploaded a voice reference
            /*
            System.out.println("4. Generating speech with voice cloning:");
            String voiceRefPath = "/opt/chatterbox/voices/your_voice.wav"; // Server-side path
            byte[] clonedAudio = ChatterboxTTS.generateSpeech(
                text,
                voiceRefPath,  // audio_prompt_path for voice cloning
                null,          // language (null for English)
                0.7f,          // temperature (unused by Chatterbox)
                1.0f,          // exaggeration (unused)
                3.0f           // cfg_weight (unused)
            );
            ChatterboxTTS.writeWav(clonedAudio, "test-cloned-voice.wav");
            System.out.println("   Generated: test-cloned-voice.wav");
            System.out.println();
            */

            // Example 5: Generate speech in different language (French)
            System.out.println("5. Generating speech in French:");
            String frenchText = "Bonjour, comment ça va? Ceci est le modèle de synthèse vocale multilingue Chatterbox.";
            byte[] frenchAudio = ChatterboxTTS.generateSpeech(
                    frenchText,
                    null,     // audio_prompt_path (null for default voice)
                    "fr",     // language code for French
                    0.7f,
                    1.0f,
                    3.0f
            );
            ChatterboxTTS.writeWav(frenchAudio, "test-french.wav");
            System.out.println("   Generated: test-french.wav");
            System.out.println();

            // Example 6: Generate speech in Chinese with voice cloning
            /*
            System.out.println("6. Generating speech in Chinese with voice cloning:");
            String chineseText = "你好，今天天气真不错，希望你有一个愉快的周末。";
            String chineseVoiceRef = "/opt/chatterbox/voices/chinese_voice.wav";
            byte[] chineseAudio = ChatterboxTTS.generateSpeech(
                chineseText,
                chineseVoiceRef,  // Use a Chinese voice reference for better results
                "zh",             // language code for Chinese
                0.7f,
                1.0f,
                3.0f
            );
            ChatterboxTTS.writeWav(chineseAudio, "test-chinese-cloned.wav");
            System.out.println("   Generated: test-chinese-cloned.wav");
            System.out.println();
            */

            // Example 7: Get available languages
            System.out.println("7. Available languages:");
            String[] languages = ChatterboxTTS.getLanguages();
            System.out.println("   Supported languages: " + String.join(", ", languages));
            System.out.println();

            // Example 8: Delete a voice reference
            // Uncomment to delete a voice reference
            /*
            System.out.println("8. Deleting voice reference:");
            String filenameToDelete = "your_voice.wav";
            ChatterboxTTS.deleteVoiceReference(filenameToDelete);
            System.out.println("   Deleted: " + filenameToDelete);
            System.out.println();
            */

            System.out.println("=== Example completed successfully ===");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

