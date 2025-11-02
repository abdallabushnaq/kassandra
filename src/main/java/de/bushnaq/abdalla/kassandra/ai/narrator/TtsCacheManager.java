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

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * Manages chronological files for generated TTS audio without maintaining a separate cache.
 * <p>
 * Behavior:
 * <ul>
 *   <li>Each narration call is assigned an incrementing id prefix: 001-, 002-, ...</li>
 *   <li>Chronological file names keep a sanitized text prefix and a short hash of text+parameters: {@code NNN-<prefix>_<hash>.wav}</li>
 *   <li>If a file already exists for the current id and its embedded hash matches the requested text+parameters, it is reused.</li>
 *   <li>Otherwise, any file with that id is deleted and the new audio is written to the target path.</li>
 * </ul>
 */
public class TtsCacheManager {
    private static final Logger logger = LoggerFactory.getLogger(TtsCacheManager.class);

    @Getter
    private final  Path   audioDir; // root for chronological files
    @Getter
    private static int    cacheMiss = 0;
    private final  Object idLock    = new Object();
    private        int    nextId;

    /**
     * Creates a new manager bound to a directory for chronological files. Ensures the directory exists.
     * The internal id counter starts at 1 for each instance (001-, 002-, ...).
     *
     * @param audioDir directory where chronological WAV files will be written and looked up
     * @throws RuntimeException when the directory cannot be created
     */
    public TtsCacheManager(Path audioDir) {
        this.audioDir = audioDir;
        try {
            Files.createDirectories(this.audioDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create audio directory under " + audioDir, e);
        }
        initNextId();
    }

    /**
     * Builds a canonical file name that captures the text and TTS parameters.
     * The resulting name has the shape {@code <prefix>_<hash>.wav} and is intended to be appended
     * to an id prefix (e.g., {@code 001-}) for chronological files.
     *
     * @param text       input text
     * @param attributes TTS attributes (all non-null fields will be included in the hash)
     * @return a stable canonical name including a short SHA-256 hash of text+parameters
     */
    public String buildFileName(String text, NarratorAttribute attributes) {
        String prefix = sanitizePrefix(crop(text, 20));
        if (prefix.isBlank()) prefix = "tts";

        String toHash = text + attributes.toHashString();
        String hash   = shortSha256Hex(toHash, 12);
        return prefix + "_" + hash + ".wav";
    }

    /**
     * Crops a string to a maximum length; returns an empty string for null input.
     *
     * @param s      input string or null
     * @param maxLen maximum number of characters to keep
     * @return input cropped to {@code maxLen} characters, or an empty string if input is null
     */
    public static String crop(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    private void deleteByIdPrefix(String idPrefix) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(audioDir, idPrefix + "*.wav")) {
            for (Path p : stream) {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ex) {
                    logger.warn("Failed to delete {}", p.getFileName(), ex);
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to list {} for cleanup of prefix {}", audioDir, idPrefix, e);
        }
    }

    private String extractHash(String name) {
        if (!name.endsWith(".wav")) return "";
        int dot = name.lastIndexOf('.');
        int us  = name.lastIndexOf('_', dot);
        if (us <= 0) return "";
        return name.substring(us + 1, dot);
    }

    // region helpers

    private Path findExistingByIdPrefix(String idPrefix) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(audioDir, idPrefix + "*.wav")) {
            for (Path p : stream) {
                return p; // return first match
            }
        } catch (IOException e) {
            logger.warn("Failed to scan {} for prefix {}", audioDir, idPrefix, e);
        }
        return null;
    }

    private boolean hasSameHash(String canonicalName, String existingName) {
        String h1 = extractHash(canonicalName);
        String h2 = extractHash(existingName);
        return !h1.isEmpty() && h1.equals(h2);
    }

    private void initNextId() {
        this.nextId = 1;
        logger.debug("Initialized nextId={} for directory {}", this.nextId, audioDir);
    }

    private String nextIdPrefix() {
        int id = nextId++;
        return (id < 1000) ? String.format(Locale.ROOT, "%03d-", id) : id + "-";
    }

    /**
     * Determines the chronological target for the next narration call.
     * <p>Algorithm:</p>
     * <ol>
     *   <li>Allocate the next id prefix (e.g., {@code 001-}).</li>
     *   <li>If a file already exists for this prefix and its hash matches {@code canonicalName}, return it as up-to-date.</li>
     *   <li>Otherwise, delete any files with this id prefix and return the target path for writing a new file.</li>
     * </ol>
     *
     * @param canonicalName a name produced by {@link #buildFileName(String, NarratorAttribute)} (no id prefix)
     * @return a plan describing whether synthesis is needed and which path to use
     */
    public ChronoPlan prepareChronological(String canonicalName) {
        final String idPrefix;
        synchronized (idLock) {
            idPrefix = nextIdPrefix();
        }
        Path existing = findExistingByIdPrefix(idPrefix);
        if (existing != null && hasSameHash(canonicalName, existing.getFileName().toString())) {
            // Matching, reuse existing file
            return new ChronoPlan(existing, true, idPrefix);
        }
        cacheMiss++;
        // Not matching: ensure no conflicting file remains for that id
        deleteByIdPrefix(idPrefix);
        Path target = targetPathFor(idPrefix, canonicalName);
        return new ChronoPlan(target, false, idPrefix);
    }

    /**
     * Sanitizes a file name prefix derived from user text to be filesystem-friendly.
     * Collapses whitespace to underscores and removes non-alphanumeric characters except [._-].
     * Returns "tts" when the result would be empty or reserved.
     *
     * @param s input text
     * @return sanitized prefix suitable for file names
     */
    public static String sanitizePrefix(String s) {
        String cleaned = s
                .replaceAll("\\s+", "_")
                .replaceAll("[^A-Za-z0-9._-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
        if (cleaned.isBlank() || cleaned.equals(".") || cleaned.equals("..")) return "tts";
        return cleaned;
    }

    private static String shortSha256Hex(String input, int hexLen) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[]        d  = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format(Locale.ROOT, "%02x", b));
            String hex = sb.toString();
            return hexLen > 0 && hexLen < hex.length() ? hex.substring(0, hexLen) : hex;
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    /**
     * Resolves the path for a chronological file combining the id prefix and canonical name.
     *
     * @param idPrefix      allocated id prefix such as {@code 001-}
     * @param canonicalName name from {@link #buildFileName(String, NarratorAttribute)}
     * @return target full path under {@link #audioDir}
     */
    private Path targetPathFor(String idPrefix, String canonicalName) {
        return audioDir.resolve(idPrefix + canonicalName);
    }

    /**
     * Converts a {@link Path} to a {@link File} for APIs that require files.
     *
     * @param p path to convert
     * @return a File representing {@code p}
     */
    public File toFile(Path p) {
        return p.toFile();
    }

    /**
     * Writes audio bytes directly to the provided chronological path.
     * The caller must have obtained this path from {@link #prepareChronological(String)}.
     *
     * @param wavBytes audio data in WAV format
     * @param target   full path to write to
     * @return the same {@code target} for convenience
     * @throws IOException if writing fails
     */
    public Path writeChronological(byte[] wavBytes, Path target) throws IOException {
        Files.write(target, wavBytes);
        return target;
    }

    /**
     * Small immutable description of the next chronological action.
     *
     * @param path     existing file to reuse or target path to write
     * @param upToDate true if an existing file matches the requested hash and can be reused
     * @param idPrefix the allocated id prefix (e.g., {@code 001-})
     */
    public record ChronoPlan(Path path, boolean upToDate, String idPrefix) {
    }

    // endregion
}
