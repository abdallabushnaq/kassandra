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
    public static final String                DARK_PROMPT_SUFFIX  = ", only the background is dark gray matching the dark mode of the UI, no gradients, no textures";
    /**
     * Prompt suffix appended to every light-avatar SD call.
     */
    public static final String                LIGHT_PROMPT_SUFFIX = ", (background is white), no shadows, no gradients, no textures";
    /**
     * -- GETTER --
     * Exposes the Stable Diffusion configuration so non-Spring callers (e.g. UI dialogs)
     * can read values such as the API URL without a direct
     * dependency.
     * <p>
     * Returns the current {@link StableDiffusionConfig} instance.
     */
    @Getter
    @Autowired
    private             StableDiffusionConfig config;

    @Autowired
    private StableDiffusionService stableDiffusionService;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

//    /**
//     * Creates a solid-black canvas whose dimensions are derived from the light-avatar original.
//     * Falls back to {@link StableDiffusionConfig#getGenerationSize()} if the original cannot be read.
//     *
//     * @param lightOriginal Original (generation-size) bytes from the light avatar result.
//     * @return PNG-encoded bytes of the black canvas.
//     * @throws StableDiffusionException if canvas creation fails.
//     */
//    private byte[] createBlackCanvas(byte[] lightOriginal) throws StableDiffusionException {
//        int size = config.getGenerationSize();
//        if (lightOriginal != null && lightOriginal.length > 0) {
//            try {
//                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(lightOriginal));
//                if (bi != null) {
//                    size = bi.getWidth();
//                }
//            } catch (IOException e) {
//                log.warn("Could not read light-avatar original to determine canvas size, using generationSize={}", size);
//            }
//        }
//        Color bgColor = Color.BLACK;
//        try {
//            bgColor = Color.decode(config.getAvatarDarkBackgroundColor());
//        } catch (Exception e) {
//            log.debug("Could not parse configured dark avatar background color '{}', using black", config.getAvatarDarkBackgroundColor());
//        }
//        try {
//            return createSolidCanvas(size, bgColor);
//        } catch (IOException e) {
//            throw new StableDiffusionException("Failed to create black canvas: " + e.getMessage(), e);
//        }
//    }

//    /**
//     * Creates a solid-colour square canvas of the requested size and returns it as PNG bytes.
//     *
//     * @param size  Width and height of the canvas in pixels.
//     * @param color Fill colour.
//     * @return PNG-encoded bytes of the filled canvas.
//     * @throws IOException if writing to the in-memory stream fails.
//     */
//    private byte[] createSolidCanvas(int size, Color color) throws IOException {
//        BufferedImage bi  = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
//        Graphics2D    g2d = bi.createGraphics();
//        g2d.setColor(color);
//        g2d.fillRect(0, 0, size, size);
//        g2d.dispose();
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        ImageIO.write(bi, "png", baos);
//        return baos.toByteArray();
//    }

//    /**
//     * Generates a dark-background AI avatar using a solid-black img2img canvas, pinned to the
//     * seed of the matching light avatar so the two variants are visually consistent.
//     * Intended for test / data-gen callers that do not need progress reporting.
//     * Uses {@link StableDiffusionService#NEGATIVE_PROMPT} as the negative prompt.
//     *
//     * @param basePrompt  Core prompt; the dark-background suffix is appended internally.
//     * @param lightResult Full result from the corresponding {@link #generateLightAvatar} call;
//     *                    its {@code originalImage} bytes and {@code seed} are reused.
//     * @return {@link GeneratedImageResult} containing the original and resized dark images.
//     * @throws StableDiffusionException if Stable Diffusion is unavailable or generation fails.
//     */
//    public GeneratedImageResult generateDarkAvatar(String basePrompt, GeneratedImageResult lightResult)
//            throws StableDiffusionException {
//        return generateDarkAvatar(basePrompt, StableDiffusionService.NEGATIVE_PROMPT, lightResult, null);
//    }

    /**
     * Generates a dark-background AI avatar with a custom negative prompt.
     * Intended for test / data-gen callers that do not need progress reporting.
     *
     * @param basePrompt     Core prompt; the dark-background suffix is appended internally.
     * @param negativePrompt Negative prompt; falls back to {@link StableDiffusionService#NEGATIVE_PROMPT} when null.
     * @param lightResult    Full result from the corresponding {@link #generateLightAvatar} call.
     * @return {@link GeneratedImageResult} containing the original and resized dark images.
     * @throws StableDiffusionException if Stable Diffusion is unavailable or generation fails.
     */
    public GeneratedImageResult generateDarkAvatar(String basePrompt, String negativePrompt, GeneratedImageResult lightResult)
            throws StableDiffusionException {
        return generateDarkAvatar(basePrompt, negativePrompt, lightResult, null);
    }

//    /**
//     * Generates a dark-background AI avatar using a solid-black img2img canvas, pinned to the
//     * seed of the matching light avatar so the two variants are visually consistent.
//     * Uses {@link StableDiffusionService#NEGATIVE_PROMPT} as the negative prompt.
//     * Intended for UI callers that want to stream generation progress.
//     *
//     * @param basePrompt  Core prompt; the dark-background suffix is appended internally.
//     * @param lightResult Full result from the corresponding {@link #generateLightAvatar} call;
//     *                    its {@code originalImage} bytes and {@code seed} are reused.
//     * @param progress    Optional callback invoked on each SD polling tick; may be {@code null}.
//     * @return {@link GeneratedImageResult} containing the original and resized dark images.
//     * @throws StableDiffusionException if Stable Diffusion is unavailable or generation fails.
//     */
//    public GeneratedImageResult generateDarkAvatar(String basePrompt, GeneratedImageResult lightResult,
//                                                   StableDiffusionService.ProgressCallback progress)
//            throws StableDiffusionException {
//        return generateDarkAvatar(basePrompt, StableDiffusionService.NEGATIVE_PROMPT, lightResult, progress);
//    }

    /**
     * Generates a dark-background AI avatar with a custom negative prompt and optional progress callback.
     *
     * @param basePrompt     Core prompt; the dark-background suffix is appended internally.
     * @param negativePrompt Negative prompt; falls back to {@link StableDiffusionService#NEGATIVE_PROMPT} when null.
     * @param lightResult    Full result from the corresponding {@link #generateLightAvatar} call.
     * @param progress       Optional callback invoked on each SD polling tick; may be {@code null}.
     * @return {@link GeneratedImageResult} containing the original and resized dark images.
     * @throws StableDiffusionException if Stable Diffusion is unavailable or generation fails.
     */
    public GeneratedImageResult generateDarkAvatar(String basePrompt, String negativePrompt, GeneratedImageResult lightResult,
                                                   StableDiffusionService.ProgressCallback progress)
            throws StableDiffusionException {
        if (!stableDiffusionService.isAvailable()) {
            throw new StableDiffusionException("Stable Diffusion is not available");
        }
        return stableDiffusionService.text2ImgWithOriginal(basePrompt, negativePrompt, config.getAvatarOutputSize(), progress,
                lightResult.getSeed(), config.getCfgScale());
    }

//    /**
//     * Generates a dark-background AI avatar, falling back to a programmatic placeholder if
//     * Stable Diffusion is unavailable or generation fails.
//     * Uses {@link StableDiffusionService#NEGATIVE_PROMPT} as the negative prompt.
//     *
//     * @param basePrompt       Core prompt passed to {@link #generateDarkAvatar(String, GeneratedImageResult)}.
//     * @param lightResult      Full result from the matching light-avatar generation.
//     * @param fallbackIconName Icon name forwarded to {@link #generateDefaultDarkAvatar(String)}
//     *                         when the AI generation fails; may be {@code null}.
//     * @return {@link GeneratedImageResult} — either AI-generated or the programmatic default.
//     */
//    public GeneratedImageResult generateDarkAvatarWithFallback(String basePrompt, GeneratedImageResult lightResult,
//                                                               String fallbackIconName) {
//        return generateDarkAvatarWithFallback(basePrompt, StableDiffusionService.NEGATIVE_PROMPT, lightResult, fallbackIconName);
//    }

    /**
     * Generates a dark-background AI avatar with a custom negative prompt, falling back to a
     * programmatic placeholder if Stable Diffusion is unavailable or generation fails.
     *
     * @param basePrompt       Core prompt passed to {@link #generateDarkAvatar(String, String, GeneratedImageResult)}.
     * @param negativePrompt   Negative prompt; falls back to {@link StableDiffusionService#NEGATIVE_PROMPT} when null.
     * @param lightResult      Full result from the matching light-avatar generation.
     * @param fallbackIconName Icon name forwarded to {@link #generateDefaultDarkAvatar(String)}; may be {@code null}.
     * @return {@link GeneratedImageResult} — either AI-generated or the programmatic default.
     */
    public GeneratedImageResult generateDarkAvatarWithFallback(String basePrompt, String negativePrompt,
                                                               GeneratedImageResult lightResult, String fallbackIconName) {
        try {
            return generateDarkAvatar(basePrompt, negativePrompt, lightResult);
        } catch (StableDiffusionException e) {
            log.warn("Failed to generate dark avatar (icon={}): {}", fallbackIconName, e.getMessage());
            return generateDefaultDarkAvatar(fallbackIconName);
        }
    }

    /**
     * Generates a programmatic dark-background placeholder avatar without using Stable Diffusion.
     * Use this as the fallback when {@link #generateDarkAvatar} throws.
     *
     * @param iconName Optional icon resource name (e.g. {@code "user"}, {@code "cube"}); may be {@code null}.
     * @return {@link GeneratedImageResult} containing the dark placeholder images.
     */
    public GeneratedImageResult generateDefaultDarkAvatar(String iconName) {
        return stableDiffusionService.generateDefaultDarkAvatar(iconName, config.getAvatarDarkBackgroundColor());
    }

    /**
     * Generates a programmatic light-background placeholder avatar without using Stable Diffusion.
     * Use this as the fallback when {@link #generateLightAvatar} throws.
     *
     * @param iconName Optional icon resource name (e.g. {@code "user"}, {@code "cube"}); may be {@code null}.
     * @return {@link GeneratedImageResult} containing the placeholder images.
     */
    public GeneratedImageResult generateDefaultLightAvatar(String iconName) {
        return stableDiffusionService.generateDefaultAvatar(iconName, config.getAvatarLightBackgroundColor());
    }

//    /**
//     * Generates a light-background AI avatar using a solid-white img2img canvas.
//     * Uses {@link StableDiffusionService#NEGATIVE_PROMPT} as the negative prompt.
//     * Intended for test / data-gen callers that do not need progress reporting.
//     *
//     * @param basePrompt Core prompt; the light-background suffix is appended internally.
//     * @return {@link GeneratedImageResult} containing the original and resized images, plus the SD seed.
//     * @throws StableDiffusionException if Stable Diffusion is unavailable or generation fails.
//     */
//    public GeneratedImageResult generateLightAvatar(String basePrompt) throws StableDiffusionException {
//        return generateLightAvatar(basePrompt, StableDiffusionService.NEGATIVE_PROMPT, null);
//    }

    /**
     * Generates a light-background AI avatar with a custom negative prompt.
     * Intended for test / data-gen callers that do not need progress reporting.
     *
     * @param basePrompt     Core prompt; the light-background suffix is appended internally.
     * @param negativePrompt Negative prompt; falls back to {@link StableDiffusionService#NEGATIVE_PROMPT} when null.
     * @return {@link GeneratedImageResult} containing the original and resized images, plus the SD seed.
     * @throws StableDiffusionException if Stable Diffusion is unavailable or generation fails.
     */
    public GeneratedImageResult generateLightAvatar(String basePrompt, String negativePrompt) throws StableDiffusionException {
        return generateLightAvatar(basePrompt, negativePrompt, null);
    }

//    /**
//     * Generates a light-background AI avatar using a solid-white img2img canvas.
//     * Uses {@link StableDiffusionService#NEGATIVE_PROMPT} as the negative prompt.
//     * Intended for UI callers that want to stream generation progress.
//     *
//     * @param basePrompt Core prompt; the light-background suffix is appended internally.
//     * @param progress   Optional callback invoked on each SD polling tick; may be {@code null}.
//     * @return {@link GeneratedImageResult} containing the original and resized images, plus the SD seed.
//     * @throws StableDiffusionException if Stable Diffusion is unavailable or generation fails.
//     */
//    public GeneratedImageResult generateLightAvatar(String basePrompt, StableDiffusionService.ProgressCallback progress)
//            throws StableDiffusionException {
//        return generateLightAvatar(basePrompt, StableDiffusionService.NEGATIVE_PROMPT, progress);
//    }

    /**
     * Generates a light-background AI avatar with a custom negative prompt and optional progress callback.
     *
     * @param prompt         Core prompt; the light-background suffix is appended internally.
     * @param negativePrompt Negative prompt; falls back to {@link StableDiffusionService#NEGATIVE_PROMPT} when null.
     * @param progress       Optional callback invoked on each SD polling tick; may be {@code null}.
     * @return {@link GeneratedImageResult} containing the original and resized images, plus the SD seed.
     * @throws StableDiffusionException if Stable Diffusion is unavailable or generation fails.
     */
    public GeneratedImageResult generateLightAvatar(String prompt, String negativePrompt, StableDiffusionService.ProgressCallback progress)
            throws StableDiffusionException {
        if (!stableDiffusionService.isAvailable()) {
            throw new StableDiffusionException("Stable Diffusion is not available");
        }
        return stableDiffusionService.text2ImgWithOriginal(prompt, negativePrompt, config.getAvatarOutputSize(), progress, -1, config.getCfgScale());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

//    /**
//     * Generates a light-background AI avatar, falling back to a programmatic placeholder if
//     * Stable Diffusion is unavailable or generation fails.
//     * Uses {@link StableDiffusionService#NEGATIVE_PROMPT} as the negative prompt.
//     *
//     * @param basePrompt       Core prompt passed to {@link #generateLightAvatar(String)}.
//     * @param fallbackIconName Icon name forwarded to {@link #generateDefaultLightAvatar(String)}; may be {@code null}.
//     * @return {@link GeneratedImageResult} — either AI-generated or the programmatic default.
//     */
//    public GeneratedImageResult generateLightAvatarWithFallback(String basePrompt, String fallbackIconName) {
//        return generateLightAvatarWithFallback(basePrompt, StableDiffusionService.NEGATIVE_PROMPT, fallbackIconName);
//    }

    /**
     * Generates a light-background AI avatar with a custom negative prompt, falling back to a
     * programmatic placeholder if Stable Diffusion is unavailable or generation fails.
     *
     * @param basePrompt       Core prompt passed to {@link #generateLightAvatar(String, String)}.
     * @param negativePrompt   Negative prompt; falls back to {@link StableDiffusionService#NEGATIVE_PROMPT} when null.
     * @param fallbackIconName Icon name forwarded to {@link #generateDefaultLightAvatar(String)}; may be {@code null}.
     * @return {@link GeneratedImageResult} — either AI-generated or the programmatic default.
     */
    public GeneratedImageResult generateLightAvatarWithFallback(String basePrompt, String negativePrompt, String fallbackIconName) {
        try {
            return generateLightAvatar(basePrompt, negativePrompt);
        } catch (StableDiffusionException e) {
            log.warn("Failed to generate light avatar (icon={}): {}", fallbackIconName, e.getMessage());
            return generateDefaultLightAvatar(fallbackIconName);
        }
    }

}



