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

import de.bushnaq.abdalla.kassandra.ai.chatterbox.ChatterboxTTS;

import java.io.File;
import java.util.List;

public abstract class AbstractTtsEngine implements TtsEngine {
    ChatterboxTTS.VoiceReference[] finalRefs;

    public void logSyncResult(SyncResult result) throws Exception {
        // Display detailed results
        System.out.println("\n📊 Sync Summary:");
        System.out.println("  Local files: " + result.localFileCount());
        System.out.println("  Server files (before): " + result.serverFileCountBefore());
        System.out.println("  Server files (after): " + result.getServerFileCountAfter());
        System.out.println("  Uploaded: " + result.uploadedCount());
        System.out.println("  Deleted: " + result.deletedCount());

        if (result.hasErrors()) {
            System.out.println("\n⚠️  Errors encountered:");
            for (String error : result.errors()) {
                System.out.println("  - " + error);
            }
        }

        if (result.uploadedCount() == 0 && result.deletedCount() == 0) {
            System.out.println("\n✅ Already in sync - no changes needed!");
        } else {
            System.out.println("\n✅ Sync complete!");
        }

        // Show final voice references on server
        finalRefs = ChatterboxTTS.listVoiceReferences();
        if (finalRefs.length > 0) {
            System.out.println("\n📋 Voice references on server:");
            for (ChatterboxTTS.VoiceReference ref : finalRefs) {
                System.out.println("  - " + ref.filename() + " (" + ref.sizeBytes() + " bytes)");
            }
        }

    }

    /**
     * Synchronize voice references between local directory and server
     * - Uploads files present locally but not on server
     * - Deletes files present on server but not locally
     *
     * @param localVoicesDir Path to local directory containing WAV files
     * @return SyncResult containing statistics about the sync operation
     * @throws Exception if sync operation fails
     */
    public SyncResult syncVoiceReferences(String localVoicesDir) throws Exception {
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
        List<File> filesToUpload = new java.util.ArrayList<>();
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

    public String voiceToPath(String voice) {
        for (ChatterboxTTS.VoiceReference finalRef : finalRefs) {
            if (finalRef.filename().toLowerCase().startsWith(voice.toLowerCase()))
                return finalRef.path();
        }
        return null;
    }
}
