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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Central service for AI avatar generation.
 * <p>
 * Encapsulates the white/black canvas creation, prompt augmentation, and img2img wiring that
 * would otherwise be duplicated across UI dialogs, test data generators and test classes.
 * Both light and dark variants are generated via Stable Diffusion img2img; when SD is
 * unavailable the service throws {@link StableDiffusionException} so callers can fall back
 * to the programmatic defaults provided by {@link #generateDefaultLightAvatar} /
 * {@link #generateDefaultDarkAvatar}.
 * </p>
 */
@Service
@Slf4j
public class AvatarService {

    /**
     * Prompt suffix appended to every dark-avatar SD call.
     */
    private static final String                DARK_PROMPT_SUFFIX  = ", background is totally night black, no gradients, no textures";
    /**
     * Prompt suffix appended to every light-avatar SD call.
     */
    private static final String                LIGHT_PROMPT_SUFFIX = ", background is totally white, no shadows, no gradients, no textures";
    /**
     * -- GETTER --
     * Exposes the Stable Diffusion configuration so non-Spring callers (e.g. UI dialogs)
     * can read values such as the API URL without a direct
     * dependency.
     *
     * @return the current {@link StableDiffusionConfig} instance.
     */
    @Getter
    @Autowired
    private              StableDiffusionConfig config;

    @Autowired
    private StableDiffusionService stableDiffusionService;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a solid-black canvas whose dimensions are derived from the light-avatar original.
     * Falls back to {@link StableDiffusionConfig#getGenerationSize()} if the original cannot be read.
     *
     * @param lightOriginal Original (generation-size) bytes from the light avatar result.
     * @return PNG-encoded bytes of the black canvas.
     * @throws StableDiffusionException if canvas creation fails.
     */
    private byte[] createBlackCanvas(byte[] lightOriginal) throws StableDiffusionException {
        int size = config.getGenerationSize();
        if (lightOriginal != null && lightOriginal.length > 0) {
            try {
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(lightOriginal));
                if (bi != null) {
                    size = bi.getWidth();
                }
            } catch (IOException e) {
                log.warn("Could not read light-avatar original to determine canvas size, using generationSize={}", size);
            }
        }
        try {
            return createSolidCanvas(size, Color.BLACK);
        } catch (IOException e) {
            throw new StableDiffusionException("Failed to create black canvas: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a solid-colour square canvas of the requested size and returns it as PNG bytes.
     *
     * @param size  Width and height of the canvas in pixels.
     * @param color Fill colour.
     * @return PNG-encoded bytes of the filled canvas.
     * @throws IOException if writing to the in-memory stream fails.
     */
    private byte[] createSolidCanvas(int size, Color color) throws IOException {
        BufferedImage bi  = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D    g2d = bi.createGraphics();
        g2d.setColor(color);
        g2d.fillRect(0, 0, size, size);
        g2d.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi, "png", baos);
        return baos.toByteArray();
    }

    /**
     * Generates a dark-background AI avatar using a solid-black img2img canvas, pinned to the
     * seed of the matching light avatar so the two variants are visually consistent.
     * Intended for test / data-gen callers that do not need progress reporting.
     *
     * @param basePrompt  Core prompt; the dark-background suffix is appended internally.
     * @param lightResult Full result from the corresponding {@link #generateLightAvatar} call;
     *                    its {@code originalImage} bytes and {@code seed} are reused.
     * @return {@link GeneratedImageResult} containing the original and resized dark images.
     * @throws StableDiffusionException if Stable Diffusion is unavailable or generation fails.
     */
    public GeneratedImageResult generateDarkAvatar(String basePrompt, GeneratedImageResult lightResult)
            throws StableDiffusionException {
        return generateDarkAvatar(basePrompt, lightResult, null);
    }

    /**
     * Generates a dark-background AI avatar using a solid-black img2img canvas, pinned to the
     * seed of the matching light avatar so the two variants are visually consistent.
     * Intended for UI callers that want to stream generation progress.
     *
     * @param basePrompt  Core prompt; the dark-background suffix is appended internally.
     * @param lightResult Full result from the corresponding {@link #generateLightAvatar} call;
     *                    its {@code originalImage} bytes and {@code seed} are reused.
     * @param progress    Optional callback invoked on each SD polling tick; may be {@code null}.
     * @return {@link GeneratedImageResult} containing the original and resized dark images.
     * @throws StableDiffusionException if Stable Diffusion is unavailable or generation fails.
     */
    public GeneratedImageResult generateDarkAvatar(String basePrompt, GeneratedImageResult lightResult,
                                                   StableDiffusionService.ProgressCallback progress)
            throws StableDiffusionException {
        if (!stableDiffusionService.isAvailable()) {
            throw new StableDiffusionException("Stable Diffusion is not available");
        }
        String sdPrompt    = basePrompt + DARK_PROMPT_SUFFIX;
        byte[] blackCanvas = createBlackCanvas(lightResult.getOriginalImage());
        return stableDiffusionService.img2imgWithOriginal(blackCanvas, sdPrompt, config.getAvatarOutputSize(), progress,
                lightResult.getSeed(), 1.0);
    }

    /**
     * Generates a programmatic dark-background placeholder avatar without using Stable Diffusion.
     * Use this as the fallback when {@link #generateDarkAvatar} throws.
     *
     * @param iconName Optional icon resource name (e.g. {@code "user"}, {@code "cube"}); may be {@code null}.
     * @return {@link GeneratedImageResult} containing the dark placeholder images.
     */
    public GeneratedImageResult generateDefaultDarkAvatar(String iconName) {
        return stableDiffusionService.generateDefaultDarkAvatar(iconName);
    }

    /**
     * Generates a programmatic light-background placeholder avatar without using Stable Diffusion.
     * Use this as the fallback when {@link #generateLightAvatar} throws.
     *
     * @param iconName Optional icon resource name (e.g. {@code "user"}, {@code "cube"}); may be {@code null}.
     * @return {@link GeneratedImageResult} containing the placeholder images.
     */
    public GeneratedImageResult generateDefaultLightAvatar(String iconName) {
        return stableDiffusionService.generateDefaultAvatar(iconName);
    }

    /**
     * Generates a light-background AI avatar using a solid-white img2img canvas.
     * The {@link StableDiffusionConfig#getAvatarOutputSize()} value is used for the output dimensions.
     * Intended for test / data-gen callers that do not need progress reporting.
     *
     * @param basePrompt Core prompt; the light-background suffix is appended internally.
     * @return {@link GeneratedImageResult} containing the original (generation-size) and the
     * resized (avatar-output-size) images, plus the actual SD seed.
     * @throws StableDiffusionException if Stable Diffusion is unavailable or generation fails.
     */
    public GeneratedImageResult generateLightAvatar(String basePrompt) throws StableDiffusionException {
        return generateLightAvatar(basePrompt, null);
    }

    /**
     * Generates a light-background AI avatar, falling back to a programmatic placeholder if
     * Stable Diffusion is unavailable or generation fails.
     * Intended for batch / data-gen callers that always need a result regardless of SD availability.
     *
     * @param basePrompt       Core prompt passed to {@link #generateLightAvatar(String)}.
     * @param fallbackIconName Icon name forwarded to {@link #generateDefaultLightAvatar(String)}
     *                         when the AI generation fails; may be {@code null}.
     * @return {@link GeneratedImageResult} — either AI-generated or the programmatic default.
     */
    public GeneratedImageResult generateLightAvatarWithFallback(String basePrompt, String fallbackIconName) {
        try {
            return generateLightAvatar(basePrompt);
        } catch (StableDiffusionException e) {
            log.warn("Failed to generate light avatar (icon={}): {}", fallbackIconName, e.getMessage());
            return generateDefaultLightAvatar(fallbackIconName);
        }
    }

    /**
     * Generates a dark-background AI avatar, falling back to a programmatic placeholder if
     * Stable Diffusion is unavailable or generation fails.
     * Intended for batch / data-gen callers that always need a result regardless of SD availability.
     *
     * @param basePrompt       Core prompt passed to {@link #generateDarkAvatar(String, GeneratedImageResult)}.
     * @param lightResult      Full result from the matching light-avatar generation.
     * @param fallbackIconName Icon name forwarded to {@link #generateDefaultDarkAvatar(String)}
     *                         when the AI generation fails; may be {@code null}.
     * @return {@link GeneratedImageResult} — either AI-generated or the programmatic default.
     */
    public GeneratedImageResult generateDarkAvatarWithFallback(String basePrompt, GeneratedImageResult lightResult,
                                                               String fallbackIconName) {
        try {
            return generateDarkAvatar(basePrompt, lightResult);
        } catch (StableDiffusionException e) {
            log.warn("Failed to generate dark avatar (icon={}): {}", fallbackIconName, e.getMessage());
            return generateDefaultDarkAvatar(fallbackIconName);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates a light-background AI avatar using a solid-white img2img canvas.
     * The {@link StableDiffusionConfig#getAvatarOutputSize()} value is used for the output dimensions.
     * Intended for UI callers that want to stream generation progress.
     *
     * @param basePrompt Core prompt; the light-background suffix is appended internally.
     * @param progress   Optional callback invoked on each SD polling tick; may be {@code null}.
     * @return {@link GeneratedImageResult} containing the original and resized images, plus the SD seed.
     * @throws StableDiffusionException if Stable Diffusion is unavailable or generation fails.
     */
    public GeneratedImageResult generateLightAvatar(String basePrompt, StableDiffusionService.ProgressCallback progress)
            throws StableDiffusionException {
        if (!stableDiffusionService.isAvailable()) {
            throw new StableDiffusionException("Stable Diffusion is not available");
        }
        String sdPrompt    = basePrompt + LIGHT_PROMPT_SUFFIX;
        byte[] whiteCanvas = null;
        try {
            whiteCanvas = createSolidCanvas(config.getGenerationSize(), Color.WHITE);
        } catch (IOException e) {
            log.warn("Failed to create white canvas, falling back to txt2img: {}", e.getMessage());
        }
        if (whiteCanvas != null) {
            return stableDiffusionService.img2imgWithOriginal(whiteCanvas, sdPrompt, config.getAvatarOutputSize(), progress, -1L, 1.0);
        } else {
            return stableDiffusionService.generateImageWithOriginal(sdPrompt, config.getAvatarOutputSize(), progress);
        }
    }

}

