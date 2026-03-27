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

package de.bushnaq.abdalla.kassandra.ai.stablediffusion;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of image generation containing both original and resized images.
 */
@Data
@NoArgsConstructor
public class GeneratedImageResult {
    /**
     * Original image at generation size (e.g., 512x512)
     */
    private byte[] originalImage;
    /**
     * The prompt used to generate the image
     */
    private String prompt;
    /**
     * Resized image at output size (e.g., 64x64)
     */
    private byte[] resizedImage;
    /**
     * The seed used by Stable Diffusion; {@code -1} means unknown (e.g., uploaded image or programmatic fallback).
     */
    private long   seed = -1L;

    /**
     * Backward-compatible 3-arg constructor. Seed defaults to {@code -1} (unknown).
     *
     * @param originalImage Original-size image bytes
     * @param prompt        Prompt used to generate the image
     * @param resizedImage  Resized image bytes
     */
    public GeneratedImageResult(byte[] originalImage, String prompt, byte[] resizedImage) {
        this(originalImage, prompt, resizedImage, -1L);
    }

    /**
     * Full constructor.
     *
     * @param originalImage Original-size image bytes
     * @param prompt        Prompt used to generate the image
     * @param resizedImage  Resized image bytes
     * @param seed          The SD seed; {@code -1} if not available
     */
    public GeneratedImageResult(byte[] originalImage, String prompt, byte[] resizedImage, long seed) {
        this.originalImage = originalImage;
        this.prompt        = prompt;
        this.resizedImage  = resizedImage;
        this.seed          = seed;
    }
}
