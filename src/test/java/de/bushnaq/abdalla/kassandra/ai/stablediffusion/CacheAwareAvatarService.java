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

package de.bushnaq.abdalla.kassandra.ai.stablediffusion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Persists successful avatar and header generations for reuse by tests.
 */
@Component
@Primary
@Slf4j
public class CacheAwareAvatarService extends AvatarService {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final Object[]   LOCKS  = new Object[64];

    static {
        for (int i = 0; i < LOCKS.length; i++) {
            LOCKS[i] = new Object();
        }
    }

    private final Path cacheDirectory;

    /**
     * Creates a cache that persists across test runs.
     */
    public CacheAwareAvatarService() {
        this(Path.of("public", "testCache", "stable-diffusion"));
    }

    /**
     * Creates a cache using a specified directory.
     *
     * @param cacheDirectory directory for cached results
     */
    public CacheAwareAvatarService(Path cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    @Override
    protected GeneratedImageResult generateAvatarImage(String prompt, String negativePrompt,
                                                        StableDiffusionService.ProgressCallback progress, long seed)
            throws StableDiffusionException {
        if (!StableDiffusionService.enabled) {
            return super.generateAvatarImage(prompt, negativePrompt, progress, seed);
        }
        StableDiffusionConfig config = getConfig();
        int outputSize = config.getAvatarOutputSize();
        int generationSize = Math.max(config.getGenerationSize(), outputSize);
        List<Object> key = commonKey("avatar", prompt, negativePrompt, seed);
        key.addAll(List.of(generationSize, generationSize, outputSize, outputSize));
        return cached(key, () -> super.generateAvatarImage(prompt, negativePrompt, progress, seed));
    }

    @Override
    protected GeneratedImageResult generateHeaderImage(String prompt, String negativePrompt,
                                                        StableDiffusionService.ProgressCallback progress, long seed,
                                                        TiledDiffusionOptions options) throws StableDiffusionException {
        if (!StableDiffusionService.enabled) {
            return super.generateHeaderImage(prompt, negativePrompt, progress, seed, options);
        }
        List<Object> key = commonKey("header", prompt, negativePrompt, seed);
        key.addAll(List.of(1024, HEADER_GENERATION_HEIGHT));
        boolean tiled = options != null && options.isEnabled();
        key.add(tiled);
        if (tiled) {
            key.addAll(List.of(options.isOverwriteImageSize(), options.getImageWidth(), options.getImageHeight(),
                    options.getLatentTileWidth(), options.getLatentTileHeight(), options.getLatentTileOverlap(),
                    options.getLatentTileBatchSize()));
        }
        return cached(key, () -> super.generateHeaderImage(prompt, negativePrompt, progress, seed, options));
    }

    private List<Object> commonKey(String kind, String prompt, String negativePrompt, long seed) {
        StableDiffusionConfig config = getConfig();
        String resolvedNegative = negativePrompt != null && !negativePrompt.isBlank()
                ? negativePrompt : StableDiffusionService.NEGATIVE_PROMPT;
        List<Object> key = new ArrayList<>();
        key.addAll(List.of("stable-diffusion-test-cache-v1", kind, prompt, resolvedNegative,
                config.getApiUrl(), config.getModelName(), config.getDefaultSteps(), config.getDefaultSampler(),
                config.getCfgScale(), seed));
        return key;
    }

    private GeneratedImageResult cached(List<Object> key, Generation generation) throws StableDiffusionException {
        String hash;
        try {
            hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(MAPPER.writeValueAsBytes(key)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
        Path entry = cacheDirectory.resolve(hash + ".json");
        Object lock = LOCKS[(hash.hashCode() & Integer.MAX_VALUE) % LOCKS.length];
        synchronized (lock) {
            try {
                Files.createDirectories(cacheDirectory);
                try (FileChannel channel = FileChannel.open(cacheDirectory.resolve(hash + ".lock"),
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                     FileLock ignored = channel.lock()) {
                    if (Files.exists(entry)) {
                        GeneratedImageResult result = MAPPER.readValue(Files.readAllBytes(entry), GeneratedImageResult.class);
                        if (result == null || result.getOriginalImage() == null || result.getOriginalImage().length == 0
                                || result.getResizedImage() == null || result.getResizedImage().length == 0) {
                            throw new IllegalStateException("Invalid Stable Diffusion cache entry: " + entry);
                        }
                        log.debug("Using Stable Diffusion test cache entry {}", entry);
                        return result;
                    }
                    GeneratedImageResult result = generation.generate();
                    if (result == null || result.getOriginalImage() == null || result.getOriginalImage().length == 0
                            || result.getResizedImage() == null || result.getResizedImage().length == 0) {
                        throw new IllegalStateException("Stable Diffusion returned an incomplete result for cache entry: " + entry);
                    }
                    Path temporary = Files.createTempFile(cacheDirectory, hash + "-", ".tmp");
                    try {
                        Files.write(temporary, MAPPER.writeValueAsBytes(result));
                        Files.move(temporary, entry, StandardCopyOption.ATOMIC_MOVE);
                    } finally {
                        Files.deleteIfExists(temporary);
                    }
                    return result;
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to access Stable Diffusion test cache entry: " + entry, e);
            } catch (tools.jackson.core.JacksonException e) {
                throw new IllegalStateException("Invalid Stable Diffusion test cache entry: " + entry, e);
            }
        }
    }

    @FunctionalInterface
    private interface Generation {
        GeneratedImageResult generate() throws StableDiffusionException;
    }
}
