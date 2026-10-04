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
    protected GeneratedImageResult generateDarkAvatar(String basePrompt, String negativePrompt, GeneratedImageResult lightResult)
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
        return stableDiffusionService.text2ImgWithOriginal(plainPrompt(basePrompt), negativePrompt,
                config.getAvatarOutputSize(), progress, lightResult.getSeed(), config.getCfgScale());
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
            return fallbackResult(generateDefaultDarkAvatar(fallbackIconName), plainPrompt(basePrompt), negativePrompt, null);
        }
    }

    /**
     * Generate a dark-theme header image matching its light-theme counterpart.
     *
     * @param prompt           The dark header-specific prompt
     * @param negativePrompt   Negative prompt guiding what to avoid
     * @param lightHeader      Matching light header, used to retain the generation seed
     * @param fallbackIconName Icon used by the programmatic fallback
     * @return A 4096x256 dark header image
     */
    public GeneratedImageResult generateDarkHeaderWithFallback(String prompt, String negativePrompt, GeneratedImageResult lightHeader, String fallbackIconName) {
        return generateDarkHeaderWithFallback(prompt, negativePrompt, lightHeader, fallbackIconName, null);
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
        return generateDarkHeaderWithFallback(prompt, negativePrompt, lightHeader, fallbackIconName, progress,
                TiledDiffusionOptions.forHeader(HEADER_WIDTH, HEADER_HEIGHT), style);
    }

    /**
     * Generate a dark-theme header image and report Stable Diffusion progress.
     *
     * @param prompt           The dark header-specific prompt
     * @param negativePrompt   Negative prompt guiding what to avoid
     * @param lightHeader      Matching light header, used to retain the generation seed
     * @param fallbackIconName Icon used by the programmatic fallback
     * @param progress         Optional callback invoked on each Stable Diffusion polling tick
     * @return A 4096x256 dark header image
     */
    public GeneratedImageResult generateDarkHeaderWithFallback(String prompt, String negativePrompt, GeneratedImageResult lightHeader, String fallbackIconName, StableDiffusionService.ProgressCallback progress) {
        return generateDarkHeaderWithFallback(prompt, negativePrompt, lightHeader, fallbackIconName, progress,
                TiledDiffusionOptions.forHeader(HEADER_WIDTH, HEADER_HEIGHT));
    }

    /**
     * Generates a dark tiled header with the matching light header's seed.
     *
     * @param prompt           dark header prompt
     * @param negativePrompt   negative prompt
     * @param lightHeader      matching light header result
     * @param fallbackIconName icon for the explicit programmatic fallback
     * @param progress         optional progress callback
     * @param options          tiled canvas settings matching the light header
     * @return generated header or the programmatic fallback
     */
    public GeneratedImageResult generateDarkHeaderWithFallback(String prompt, String negativePrompt, GeneratedImageResult lightHeader,
                                                               String fallbackIconName, StableDiffusionService.ProgressCallback progress,
                                                               TiledDiffusionOptions options) {
        return generateDarkHeaderWithFallback(prompt, negativePrompt, lightHeader, fallbackIconName, progress, options, null);
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
        CozyStylePrompt resolvedStyle = resolveStyle(style, lightHeader);
        return withStyle(generateHeaderWithFallback(styledPrompt(prompt, resolvedStyle), negativePrompt, lightHeader.getSeed(),
                fallbackIconName, true, progress, options), resolvedStyle);
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
            log.warn("Failed to generate {} header (icon={}): {}", dark ? "dark" : "light", fallbackIconName, e.getMessage());
            return fallbackResult(stableDiffusionService.generateDefaultHeader(fallbackIconName, dark), prompt, negativePrompt, null);
        }
    }

    /**
     * Generates a light-background AI avatar with a custom negative prompt.
     * Intended for test / data-gen callers that do not need progress reporting.
     *
     * @param basePrompt     Core prompt; the light-background suffix is appended internally.
     * @param negativePrompt Negative prompt; falls back to {@link StableDiffusionService#NEGATIVE_PROMPT} when null.
     * @return {@link GeneratedImageResult} containing the original and resized images, plus the SD seed.
     * @throws StableDiffusionException if Stable Diffusion is unavailable or generation fails.
     */
    protected GeneratedImageResult generateLightAvatar(String basePrompt, String negativePrompt) throws StableDiffusionException {
        return generateLightAvatar(basePrompt, negativePrompt, null);
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
        return stableDiffusionService.text2ImgWithOriginal(plainPrompt(prompt), negativePrompt,
                config.getAvatarOutputSize(), progress, -1, config.getCfgScale());
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
            return fallbackResult(generateDefaultLightAvatar(fallbackIconName), plainPrompt(basePrompt), negativePrompt, null);
        }
    }

    /**
     * Generate a light-theme header image from a header-specific prompt.
     *
     * @param prompt           The header-specific prompt
     * @param negativePrompt   Negative prompt guiding what to avoid
     * @param fallbackIconName Icon used by the programmatic fallback
     * @return A 4096x256 light header image
     */
    public GeneratedImageResult generateLightHeaderWithFallback(String prompt, String negativePrompt, String fallbackIconName) {
        return generateLightHeaderWithFallback(prompt, negativePrompt, fallbackIconName, null);
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
        return generateLightHeaderWithFallback(prompt, negativePrompt, fallbackIconName, progress,
                TiledDiffusionOptions.forHeader(HEADER_WIDTH, HEADER_HEIGHT), style);
    }

    /**
     * Generate a light-theme header image and report Stable Diffusion progress.
     *
     * @param prompt           The header-specific prompt
     * @param negativePrompt   Negative prompt guiding what to avoid
     * @param fallbackIconName Icon used by the programmatic fallback
     * @param progress         Optional callback invoked on each Stable Diffusion polling tick
     * @return A 4096x256 light header image
     */
    public GeneratedImageResult generateLightHeaderWithFallback(String prompt, String negativePrompt, String fallbackIconName, StableDiffusionService.ProgressCallback progress) {
        return generateLightHeaderWithFallback(prompt, negativePrompt, fallbackIconName, progress, TiledDiffusionOptions.forHeader(HEADER_WIDTH, HEADER_HEIGHT));
    }

    /**
     * Generates a tiled light header sized for the browser's header aspect ratio.
     *
     * @param prompt           light header prompt
     * @param negativePrompt   negative prompt
     * @param fallbackIconName icon for the explicit programmatic fallback
     * @param progress         optional progress callback
     * @param options          tiled canvas settings
     * @return generated header or the programmatic fallback
     */
    public GeneratedImageResult generateLightHeaderWithFallback(String prompt, String negativePrompt, String fallbackIconName, StableDiffusionService.ProgressCallback progress, TiledDiffusionOptions options) {
        return generateLightHeaderWithFallback(prompt, negativePrompt, fallbackIconName, progress, options, null);
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
        CozyStylePrompt resolvedStyle = resolveStyle(style, null);
        return withStyle(generateHeaderWithFallback(styledPrompt(prompt, resolvedStyle), negativePrompt, -1L,
                fallbackIconName, false, progress, options), resolvedStyle);
    }

    private CozyStylePrompt resolveStyle(CozyStylePrompt style, GeneratedImageResult lightResult) {
        if (style != null) {
            return style;
        }
        return lightResult != null && lightResult.getStyle() != null ? lightResult.getStyle() : CozyStylePrompt.randomStyle();
    }

    private String styledPrompt(String prompt, CozyStylePrompt style) {
        return plainPrompt(prompt) + ", " + style.getPrompt();
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

    private GeneratedImageResult withStyle(GeneratedImageResult result, CozyStylePrompt style) {
        result.setStyle(style);
        return result;
    }

    private GeneratedImageResult fallbackResult(GeneratedImageResult result, String prompt, String negativePrompt, CozyStylePrompt style) {
        // Retain the requested prompt so saving a placeholder does not discard the user's description.
        result.setPrompt(prompt);
        result.setNegativePrompt(negativePrompt != null && !negativePrompt.isBlank() ? negativePrompt : StableDiffusionService.NEGATIVE_PROMPT);
        return withStyle(result, style);
    }

}
