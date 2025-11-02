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

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Example tests demonstrating Index TTS voice cloning and voice reference management
 */
public class ChatterboxVoiceManagementTest {

    /**
     * Synchronize voice references between local directory and server
     * - Uploads files present locally but not on server
     * - Deletes files present on server but not locally
     *
     * @param localVoicesDir Path to local directory containing WAV files
     * @return SyncResult containing statistics about the sync operation
     * @throws Exception if sync operation fails
     */
    public static SyncResult syncVoiceReferences(String localVoicesDir) throws Exception {
        java.io.File voicesDir = new java.io.File(localVoicesDir);

        if (!voicesDir.exists() || !voicesDir.isDirectory()) {
            throw new IllegalArgumentException("Local voices directory not found: " + localVoicesDir);
        }

        // Get list of local WAV files
        java.io.File[] localFiles = voicesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".wav"));
        if (localFiles == null) {
            localFiles = new java.io.File[0];
        }

        // Get list of server voice references
        ChatterboxTTS.VoiceReference[] serverRefs = ChatterboxTTS.listVoiceReferences();

        // Create sets for comparison
        java.util.Set<String> localFilenames = new java.util.HashSet<>();
        for (java.io.File file : localFiles) {
            localFilenames.add(file.getName());
        }

        java.util.Set<String> serverFilenames = new java.util.HashSet<>();
        for (ChatterboxTTS.VoiceReference ref : serverRefs) {
            serverFilenames.add(ref.filename());
        }

        // Find files to upload (present locally but not on server)
        List<java.io.File> filesToUpload = new java.util.ArrayList<>();
        for (java.io.File file : localFiles) {
            if (!serverFilenames.contains(file.getName())) {
                filesToUpload.add(file);
            }
        }

        // Find files to delete (present on server but not locally)
        List<String> filesToDelete = new java.util.ArrayList<>();
        for (ChatterboxTTS.VoiceReference ref : serverRefs) {
            if (!localFilenames.contains(ref.filename())) {
                filesToDelete.add(ref.filename());
            }
        }

        int          uploadedCount = 0;
        int          deletedCount  = 0;
        List<String> errors        = new java.util.ArrayList<>();

        // Upload missing files
        for (java.io.File file : filesToUpload) {
            try {
                ChatterboxTTS.uploadVoiceReference(file.getAbsolutePath());
                uploadedCount++;
            } catch (Exception e) {
                errors.add("Failed to upload " + file.getName() + ": " + e.getMessage());
            }
        }

        // Delete files no longer present locally
        for (String filename : filesToDelete) {
            try {
                ChatterboxTTS.deleteVoiceReference(filename);
                deletedCount++;
            } catch (Exception e) {
                errors.add("Failed to delete " + filename + ": " + e.getMessage());
            }
        }

        return new SyncResult(localFiles.length, serverRefs.length, uploadedCount, deletedCount, errors);
    }

//    /**
//     * Example 5: Complete upload and use workflow
//     */
//    @Test
//    public void testCompleteWorkflow() throws Exception {
//        System.out.println("=== Example 5: Complete Workflow ===\n");
//
//        // Step 1: List existing references
//        System.out.println("Step 1: Checking existing voice references...");
//        ChatterboxTTS.VoiceReference[] refs = ChatterboxTTS.listVoiceReferences();
//        System.out.println("Found " + refs.length + " voice reference(s)\n");
//
//        // Step 2: Upload if needed (commented for safety)
//        /*
//        if (refs.length == 0) {
//            System.out.println("Step 2: Uploading new voice reference...");
//            String localFile = "E:\\path\\to\\your\\voice.wav";
//            IndexTTS.VoiceReference uploaded = IndexTTS.uploadVoiceReference(localFile);
//            System.out.println("✅ Uploaded: " + uploaded.getFilename() + "\n");
//            refs = new IndexTTS.VoiceReference[]{uploaded};
//        } else {
//            System.out.println("Step 2: Using existing voice reference\n");
//        }
//        */
//
//        // Step 3: Generate speech
//        if (refs.length > 0) {
//            System.out.println("Step 3: Generating speech with voice reference...");
//            byte[] audio = ChatterboxTTS.generateSpeech(
//                    "This is a complete workflow demonstration.",
//                    refs[0].path(),
//                    null, null, null, null, null, null, null
//            );
//            ChatterboxTTS.writeWav(audio, "test_workflow.wav");
//            System.out.println("✅ Generated -> test_workflow.wav\n");
//        } else {
//            System.out.println("Step 3: No voice references available, using default voice...");
//            byte[] audio = ChatterboxTTS.generateSpeech("This is a complete workflow demonstration.");
//            ChatterboxTTS.writeWav(audio, "test_workflow.wav");
//            System.out.println("✅ Generated -> test_workflow.wav\n");
//        }
//    }

    /**
     * Example 4: Delete a voice reference
     */
    @Test
    public void testDeleteVoiceReference() throws Exception {
        System.out.println("=== Example 4: Delete Voice Reference ===\n");

        ChatterboxTTS.VoiceReference[] refs = ChatterboxTTS.listVoiceReferences();

        if (refs.length == 0) {
            System.out.println("No voice references to delete.");
        } else {
            System.out.println("Available voice references:");
            for (int i = 0; i < refs.length; i++) {
                System.out.println("  [" + i + "] " + refs[i].filename());
            }

            System.out.println("\nTo delete a voice reference:");
            System.out.println("  IndexTTS.deleteVoiceReference(\"filename.wav\");");

            // Example (commented out for safety):
            /*
            String filenameToDelete = refs[0].getFilename();
            System.out.println("\nDeleting: " + filenameToDelete);
            IndexTTS.deleteVoiceReference(filenameToDelete);
            System.out.println("✅ Deleted successfully");
            */
        }
    }

    /**
     * Example 1: List voice references on the server
     */
    @Test
    public void testListVoiceReferences() throws Exception {
        System.out.println("=== Example 1: List Voice References ===\n");

        ChatterboxTTS.VoiceReference[] refs = ChatterboxTTS.listVoiceReferences();

        if (refs.length == 0) {
            System.out.println("No voice references found on server.");
            System.out.println("You can upload one using uploadVoiceReference()");
        } else {
            System.out.println("Found " + refs.length + " voice reference(s):");
            for (ChatterboxTTS.VoiceReference ref : refs) {
                System.out.println("  - " + ref);
            }
        }
    }

    /**
     * Example 2: Sync voice references with server
     * Uploads missing files from local directory and deletes files no longer present locally
     */
    @Test
    public void testUploadVoiceReference() throws Exception {
        System.out.println("=== Example 2: Sync Voice References ===\n");

        String       localVoicesDir = "docker\\chatterbox\\voices";
        java.io.File voicesDir      = new java.io.File(localVoicesDir);

        if (!voicesDir.exists() || !voicesDir.isDirectory()) {
            System.out.println("❌ Local voices directory not found: " + localVoicesDir);
            System.out.println("Create the directory and add WAV files to sync.");
            return;
        }

        System.out.println("📁 Local voices directory: " + localVoicesDir);

        // Perform sync using the reusable method
        SyncResult result = syncVoiceReferences(localVoicesDir);

        // Display detailed results
        System.out.println("\n📊 Sync Summary:");
        System.out.println("  Local files: " + result.localFileCount);
        System.out.println("  Server files (before): " + result.serverFileCountBefore);
        System.out.println("  Server files (after): " + result.getServerFileCountAfter());
        System.out.println("  Uploaded: " + result.uploadedCount);
        System.out.println("  Deleted: " + result.deletedCount);

        if (result.hasErrors()) {
            System.out.println("\n⚠️  Errors encountered:");
            for (String error : result.errors) {
                System.out.println("  - " + error);
            }
        }

        if (result.uploadedCount == 0 && result.deletedCount == 0) {
            System.out.println("\n✅ Already in sync - no changes needed!");
        } else {
            System.out.println("\n✅ Sync complete!");
        }

        // Show final voice references on server
        ChatterboxTTS.VoiceReference[] finalRefs = ChatterboxTTS.listVoiceReferences();
        if (finalRefs.length > 0) {
            System.out.println("\n📋 Voice references on server:");
            for (ChatterboxTTS.VoiceReference ref : finalRefs) {
                System.out.println("  - " + ref.filename() + " (" + ref.sizeBytes() + " bytes)");
            }
        }
    }

    /**
     * Example 3: Use an uploaded voice reference
     */
    @Test
    public void testUseVoiceReference() throws Exception {
        System.out.println("=== Example 3: Use Voice Reference ===\n");

        ChatterboxTTS.VoiceReference[] refs = ChatterboxTTS.listVoiceReferences();

        if (refs.length == 0) {
            System.out.println("No voice references available.");
            System.out.println("Upload one first using testUploadVoiceReference()");
            System.out.println("\nUsing default voice instead:");

            byte[] audio = ChatterboxTTS.generateSpeech("This uses the default voice.",
                    0.5f,
                    0.5f,
                    .5f
            );
            ChatterboxTTS.writeWav(audio, "test_default_voice.wav");
            System.out.println("✅ Generated -> test_default_voice.wav");
        } else {
            System.out.println("Using first available voice: " + refs[0].filename());

            byte[] audio = ChatterboxTTS.generateSpeech(
                    "This is using a custom voice reference from the server.",
                    refs[0].path(),
                    "en",
                    0.5f,
                    0.5f,
                    .5f
            );
            ChatterboxTTS.writeWav(audio, "test_custom_voice.wav");
            System.out.println("✅ Generated -> test_custom_voice.wav");
        }
    }

//    /**
//     * Example 6: Voice cloning with emotions
//     */
//    @Test
//    public void testVoiceCloningWithEmotions() throws Exception {
//        System.out.println("=== Example 6: Voice Cloning + Emotions ===\n");
//
//        ChatterboxTTS.VoiceReference[] refs     = ChatterboxTTS.listVoiceReferences();
//        String                         voiceRef = refs.length > 0 ? refs[0].path() : null;
//
//        if (voiceRef != null) {
//            System.out.println("Using voice reference: " + refs[0].filename());
//        } else {
//            System.out.println("Using default voice (no custom references available)");
//        }
//
//        // Happy emotion
//        System.out.println("\nGenerating happy speech...");
//        byte[] happyAudio = ChatterboxTTS.generateSpeech(
//                "I am so excited! This is wonderful news!",
//                voiceRef,
//                null,    // speed
//                null,    // angry
//                0.8f,    // happy (80%)
//                null,    // sad
//                null,    // surprise
//                0.2f,    // neutral (20%)
//                null     // temperature
//        );
//        ChatterboxTTS.writeWav(happyAudio, "test_happy_emotion.wav");
//        System.out.println("✅ test_happy_emotion.wav");
//
//        // Sad emotion
//        System.out.println("\nGenerating sad speech...");
//        byte[] sadAudio = ChatterboxTTS.generateSpeech(
//                "I am disappointed. This is unfortunate.",
//                voiceRef,
//                null, null, null,
//                0.7f,    // sad (70%)
//                null,
//                0.3f,    // neutral (30%)
//                null
//        );
//        ChatterboxTTS.writeWav(sadAudio, "test_sad_emotion.wav");
//        System.out.println("✅ test_sad_emotion.wav");
//
//        // Angry emotion
//        System.out.println("\nGenerating angry speech...");
//        byte[] angryAudio = ChatterboxTTS.generateSpeech(
//                "This is unacceptable! I demand an explanation!",
//                voiceRef,
//                null,
//                0.8f,    // angry (80%)
//                null, null, null,
//                0.2f,    // neutral (20%)
//                null
//        );
//        ChatterboxTTS.writeWav(angryAudio, "test_angry_emotion.wav");
//        System.out.println("✅ test_angry_emotion.wav");
//    }

    /**
     * Result of a voice reference sync operation
     */
    public record SyncResult(int localFileCount, int serverFileCountBefore, int uploadedCount, int deletedCount,
                             List<String> errors) {

        public int getServerFileCountAfter() {
            return serverFileCountBefore - deletedCount + uploadedCount;
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        @Override
        public String toString() {
            return String.format("SyncResult{local=%d, server=%d->%d, uploaded=%d, deleted=%d, errors=%d}",
                    localFileCount, serverFileCountBefore, getServerFileCountAfter(), uploadedCount, deletedCount, errors.size());
        }
    }
}

