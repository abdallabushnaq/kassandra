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

import de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.prompt.CozyStylePrompt;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Central service for AI avatar and header generation.
 * Cozy styles apply only to headers; avatars omit them to remain recognizable at small sizes.
 */
@Service
@Slf4j
public class AvatarService {

    /**
     * Prompt suffix appended to every dark-avatar SD call.
     */
    public static final String                DARK_PROMPT_SUFFIX       = ", simple dark background";
    /**
     * Base header generation height before Tiled Diffusion overwrites the canvas size.
     */
    public static final int                   HEADER_GENERATION_HEIGHT = 1024;
    /**
     * Minimum tiled header canvas height in pixels.
     */
    public static final int                   HEADER_HEIGHT            = 256;
    /**
     * Default header canvas width for callers without browser dimensions.
     */
    public static final int                   HEADER_WIDTH             = 4096;
    /**
     * Prompt suffix appended to every light-avatar SD call.
     */
    public static final String                LIGHT_PROMPT_SUFFIX      = ", simple white background";
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

    private GeneratedImageResult fallbackResult(GeneratedImageResult result, String prompt, String negativePrompt, CozyStylePrompt style) {
        // Retain the requested prompt so saving a placeholder does not discard the user's description.
        result.setPrompt(prompt);
        result.setNegativePrompt(negativePrompt != null && !negativePrompt.isBlank() ? negativePrompt : StableDiffusionService.NEGATIVE_PROMPT);
        result.setStyle(style);
        return result;
    }

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
        return stableDiffusionService.text2ImgWithOriginal(plainPrompt(basePrompt), negativePrompt,
                config.getAvatarOutputSize(), progress, lightResult.getSeed(), config.getCfgScale());
    }

    /**
     * Generates a dark-background AI avatar with a custom negative prompt, falling back to a
     * programmatic placeholder if Stable Diffusion is unavailable or generation fails.
     *
     * @param basePrompt       Core prompt passed to {@link #generateDarkAvatar(String, String, GeneratedImageResult, StableDiffusionService.ProgressCallback)}.
     * @param negativePrompt   Negative prompt; falls back to {@link StableDiffusionService#NEGATIVE_PROMPT} when null.
     * @param lightResult      Full result from the matching light-avatar generation.
     * @param fallbackIconName Icon name used to generate default image; may be {@code null}.
     * @return {@link GeneratedImageResult} — either AI-generated or the programmatic default.
     */
    public GeneratedImageResult generateDarkAvatarWithFallback(String basePrompt, String negativePrompt,
                                                               GeneratedImageResult lightResult, String fallbackIconName, StableDiffusionService.ProgressCallback progress) {
        try {
            return generateDarkAvatar(basePrompt, negativePrompt, lightResult, progress);
        } catch (StableDiffusionException e) {
            log.warn("Failed to generate dark avatar (icon={}): {}", fallbackIconName, e.getMessage());
            return fallbackResult(stableDiffusionService.generateDefaultDarkAvatar(fallbackIconName, config.getAvatarDarkBackgroundColor()), plainPrompt(basePrompt), negativePrompt, null);
        }
    }

    /**
     * Generates a default-size dark header with the shared style and matching light seed.
     *
     * @param prompt           editable header description
     * @param negativePrompt   things to avoid
     * @param lightHeader      matching light header
     * @param fallbackIconName placeholder icon
     * @param progress         optional progress callback
     * @param style            explicit style; null inherits the light style
     * @return generated or fallback header
     */
    public GeneratedImageResult generateDarkHeaderWithFallback(String prompt, String negativePrompt, GeneratedImageResult lightHeader,
                                                               String fallbackIconName, StableDiffusionService.ProgressCallback progress,
                                                               CozyStylePrompt style) {
        return generateDarkHeaderWithFallback(prompt, negativePrompt, lightHeader, fallbackIconName, progress, TiledDiffusionOptions.forHeader(HEADER_WIDTH, HEADER_HEIGHT), style);
    }

    /**
     * Generates a dark tiled header with the matching light seed and a shared Cozy style.
     *
     * @param prompt           editable header description
     * @param negativePrompt   things to avoid
     * @param lightHeader      matching light header
     * @param fallbackIconName placeholder icon
     * @param progress         optional progress callback
     * @param options          tiled canvas settings
     * @param style            explicit style; null inherits the light style or selects randomly
     * @return generated or fallback header with resolved style metadata
     */
    public GeneratedImageResult generateDarkHeaderWithFallback(String prompt, String negativePrompt, GeneratedImageResult lightHeader,
                                                               String fallbackIconName, StableDiffusionService.ProgressCallback progress,
                                                               TiledDiffusionOptions options, CozyStylePrompt style) {
        CozyStylePrompt      resolvedStyle = resolveStyle(style, lightHeader);
        String               styledPrompt  = styledPrompt(prompt, resolvedStyle);
        GeneratedImageResult imageResult   = generateHeaderWithFallback(styledPrompt, negativePrompt, lightHeader.getSeed(), fallbackIconName, true, progress, options);
        imageResult.setStyle(resolvedStyle);
        return imageResult;
    }

    private GeneratedImageResult generateHeaderWithFallback(String prompt, String negativePrompt, long seed,
                                                            String fallbackIconName, boolean dark, StableDiffusionService.ProgressCallback progress,
                                                            TiledDiffusionOptions options) {
        try {
            if (!stableDiffusionService.isAvailable()) {
                throw new StableDiffusionException("Stable Diffusion is not available");
            }
            return stableDiffusionService.text2ImgWithOriginal(
                    prompt, negativePrompt, 1024, HEADER_GENERATION_HEIGHT,
                    progress, seed, config.getCfgScale(), options);
        } catch (StableDiffusionException e) {
            log.warn("Stable Diffusion not available, using default {} header {} with custom background color {}", dark ? "dark" : "light", fallbackIconName != null ? " with icon: " + fallbackIconName : "", dark ? config.getAvatarDarkBackgroundColor() : config.getAvatarLightBackgroundColor());
            return fallbackResult(stableDiffusionService.generateDefaultHeader(fallbackIconName, dark), prompt, negativePrompt, null);
        }
    }

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
        return stableDiffusionService.text2ImgWithOriginal(plainPrompt(prompt), negativePrompt, config.getAvatarOutputSize(), progress, -1, config.getCfgScale());
    }

    /**
     * Generates a light-background AI avatar with a custom negative prompt, falling back to a
     * programmatic placeholder if Stable Diffusion is unavailable or generation fails.
     *
     * @param prompt           Core prompt passed to {@link #generateLightAvatar(String, String, StableDiffusionService.ProgressCallback)}.
     * @param negativePrompt   Negative prompt; falls back to {@link StableDiffusionService#NEGATIVE_PROMPT} when null.
     * @param fallbackIconName Icon name used to generate default image; may be {@code null}.
     * @return {@link GeneratedImageResult} — either AI-generated or the programmatic default.
     */
    public GeneratedImageResult generateLightAvatarWithFallback(String prompt, String negativePrompt, String fallbackIconName, StableDiffusionService.ProgressCallback progress) {
        try {
            return generateLightAvatar(prompt, negativePrompt, progress);
        } catch (StableDiffusionException e) {
            log.warn("Failed to generate light avatar (icon={}): {}", fallbackIconName, e.getMessage());
            return fallbackResult(stableDiffusionService.generateDefaultLightAvatar(fallbackIconName, config.getAvatarLightBackgroundColor()), plainPrompt(prompt), negativePrompt, null);

        }
    }

    /**
     * Generates a default-size light header with a shared explicit style.
     *
     * @param prompt           editable header description
     * @param negativePrompt   things to avoid
     * @param fallbackIconName placeholder icon
     * @param progress         optional progress callback
     * @param style            explicit style; null selects randomly
     * @return generated or fallback header
     */
    public GeneratedImageResult generateLightHeaderWithFallback(String prompt, String negativePrompt, String fallbackIconName,
                                                                StableDiffusionService.ProgressCallback progress, CozyStylePrompt style) {
        return generateLightHeaderWithFallback(prompt, negativePrompt, fallbackIconName, progress, TiledDiffusionOptions.forHeader(HEADER_WIDTH, HEADER_HEIGHT), style);
    }

    /**
     * Generates a tiled light header using an explicit or randomly selected Cozy style.
     *
     * @param prompt           editable header description
     * @param negativePrompt   things to avoid
     * @param fallbackIconName placeholder icon
     * @param progress         optional progress callback
     * @param options          tiled canvas settings
     * @param style            explicit style; null selects randomly
     * @return generated or fallback header with resolved style metadata
     */
    public GeneratedImageResult generateLightHeaderWithFallback(String prompt, String negativePrompt, String fallbackIconName,
                                                                StableDiffusionService.ProgressCallback progress,
                                                                TiledDiffusionOptions options, CozyStylePrompt style) {
        CozyStylePrompt      resolvedStyle = resolveStyle(style, null);
        String               styledPrompt  = styledPrompt(prompt, resolvedStyle);
        GeneratedImageResult imageResult   = generateHeaderWithFallback(styledPrompt, negativePrompt, -1L, fallbackIconName, false, progress, options);
        imageResult.setStyle(resolvedStyle);
        return imageResult;
    }

    private String plainPrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Image description cannot be null or blank");
        }
        String subject = prompt.trim();
        for (CozyStylePrompt previousStyle : CozyStylePrompt.values()) {
            subject = subject.replace(", " + previousStyle.getPrompt(), "");
        }
        return subject;
    }

    /**
     * Resolves the Cozy style to use for a header generation.
     * If an explicit style is provided, it is returned.
     * If no explicit style is provided, but a light header result exists with a style, that style is returned.
     * Otherwise, a random Cozy style is selected.
     *
     * @param style       Explicitly requested style; may be null.
     * @param lightResult Matching light header result; may be null.
     * @return Resolved CozyStylePrompt to use for header generation.
     */
    private CozyStylePrompt resolveStyle(CozyStylePrompt style, GeneratedImageResult lightResult) {
        if (style != null) {
            return style;
        }
        return lightResult != null && lightResult.getStyle() != null ? lightResult.getStyle() : CozyStylePrompt.randomStyle();
    }

    private String styledPrompt(String prompt, CozyStylePrompt style) {
        return plainPrompt(prompt) + ", " + style.getPrompt();
    }

}
