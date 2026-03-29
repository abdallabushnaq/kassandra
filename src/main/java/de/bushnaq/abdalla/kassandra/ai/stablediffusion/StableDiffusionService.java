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

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;

/**
 * Service for generating images using Stable Diffusion WebUI API.
 */
@Service
@Slf4j
public class StableDiffusionService {

    public static final String                  NEGATIVE_PROMPT = "blurry, (nsfw), distorted, low quality, ugly, deformed, bad anatomy, (worst quality, low quality:2), cartoon, painting, illustration";
    private final       StableDiffusionConfig   config;
    private final       ResourcePatternResolver resourcePatternResolver;
    private final       WebClient               webClient;

    public StableDiffusionService(StableDiffusionConfig config, ResourcePatternResolver resourcePatternResolver) {
        this.config                  = config;
        this.resourcePatternResolver = resourcePatternResolver;

        // Configure WebClient with increased buffer size for large image responses
        // Stable Diffusion returns base64-encoded images which can be large (several MB)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();

        this.webClient = WebClient.builder()
                .baseUrl(config.getApiUrl())
                .exchangeStrategies(strategies)
                .build();

        String currentModel = getCurrentModel();
        if (currentModel != null) {
            if (!currentModel.startsWith(config.getModelName())) {
                selectModel(config.getModelName());
            } else {
                log.info("Stable Diffusion model '{}' is already loaded", currentModel);
            }
        }
        getOptions();
    }

    /**
     * Generate a default avatar image with white background and dotted light gray border.
     * This is used when Stable Diffusion is not available or when the user doesn't want to use AI image generation.
     * Uses the same sizes as configured for Stable Diffusion (generationSize and outputSize).
     *
     * @param iconName Optional icon name (without .png extension) to render in the center of the image.
     *                 Icons are loaded from META-INF/resources/ui/icons/ directory.
     *                 Pass null for no icon. Examples: "user", "cube", "lightbulb", "exit"
     * @return GeneratedImageResult containing original and resized images
     */
    public GeneratedImageResult generateDefaultAvatar(String iconName) {
        log.warn("Stable Diffusion not available, using default avatar" + (iconName != null ? " with icon: " + iconName : ""));
        byte[] pngImageBytes = null;
        if (iconName != null && !iconName.trim().isEmpty()) {
            pngImageBytes = loadPngResource("META-INF/resources/ui/icons/" + iconName + ".png");
            if (pngImageBytes == null) {
                log.warn("Failed to load icon '{}', generating avatar without icon", iconName);
            }
        }
        return generateDefaultAvatarInternal(pngImageBytes, false);
    }

    /**
     * Generate a default avatar image with a custom background color and dotted light gray border.
     * This is used when Stable Diffusion is not available or when the user doesn't want to use AI image generation.
     * Uses the same sizes as configured for Stable Diffusion (generationSize and outputSize).
     *
     * @param iconName        Optional icon name (without .png extension) to render in the center of the image.
     *                        Icons are loaded from META-INF/resources/ui/icons/ directory.
     * @param backgroundColor CSS hex string (e.g., "#FFFFFF") for the avatar background. If null or invalid, falls back to default.
     * @return GeneratedImageResult containing original and resized images
     */
    public GeneratedImageResult generateDefaultAvatar(String iconName, String backgroundColor) {
        log.warn("Stable Diffusion not available, using default avatar{} with custom background color {}", iconName != null ? " with icon: " + iconName : "", backgroundColor);
        byte[] pngImageBytes = null;
        if (iconName != null && !iconName.trim().isEmpty()) {
            pngImageBytes = loadPngResource("META-INF/resources/ui/icons/" + iconName + ".png");
            if (pngImageBytes == null) {
                log.warn("Failed to load icon '{}', generating avatar without icon", iconName);
            }
        }
        return generateDefaultAvatarInternal(pngImageBytes, false, backgroundColor);
    }

    /**
     * Internal method to generate a default avatar image.
     *
     * @param pngImageBytes Optional PNG image bytes to render in the center of the image
     * @param dark          {@code true} to use a dark background (near-black) and mid-gray border;
     *                      {@code false} for the classic white background and light-gray border
     * @return GeneratedImageResult containing original and resized images
     */
    private GeneratedImageResult generateDefaultAvatarInternal(byte[] pngImageBytes, boolean dark) {
        log.warn("Stable Diffusion not available, using default {} avatar", dark ? "dark" : "light");
        try {
            int originalSize = config.getGenerationSize();

            int   targetDisplaySize                = 24;
            float scalingRatioToSmallest           = (float) targetDisplaySize / (float) originalSize;
            float minBorderThicknessInSmallestSize = 1.0f;
            float borderThickness                  = Math.max(2.0f, minBorderThicknessInSmallestSize / scalingRatioToSmallest);
            float dashLength                       = 5.0f / scalingRatioToSmallest;

            BufferedImage originalImage = new BufferedImage(originalSize, originalSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D    graphics      = originalImage.createGraphics();

            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Background: near-black for dark mode, white for light mode
            graphics.setColor(dark ? new Color(30, 30, 30) : Color.WHITE);
            graphics.fillRect(0, 0, originalSize, originalSize);

            // Border: mid-gray for dark mode, light gray for light mode
            graphics.setColor(dark ? new Color(120, 120, 120) : new Color(211, 211, 211));
            float[] dashPattern = {dashLength, dashLength};
            graphics.setStroke(new BasicStroke(borderThickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dashPattern, 0.0f));

            float halfStroke  = borderThickness / 2.0f;
            int   borderInset = (int) Math.ceil(halfStroke) + 2;
            graphics.drawRect(borderInset, borderInset, originalSize - (2 * borderInset), originalSize - (2 * borderInset));

            if (pngImageBytes != null && pngImageBytes.length > 0) {
                try {
                    BufferedImage pngImage = ImageIO.read(new ByteArrayInputStream(pngImageBytes));
                    if (pngImage != null) {
                        double widthRatio   = (double) originalSize / pngImage.getWidth();
                        double heightRatio  = (double) originalSize / pngImage.getHeight();
                        double scalingRatio = Math.min(widthRatio, heightRatio);
                        int    scaledWidth  = (int) (pngImage.getWidth() * scalingRatio);
                        int    scaledHeight = (int) (pngImage.getHeight() * scalingRatio);
                        int    imageX       = (originalSize - scaledWidth) / 2;
                        int    imageY       = (originalSize - scaledHeight) / 2;
                        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        graphics.drawImage(pngImage, imageX, imageY, scaledWidth, scaledHeight, null);
                    } else {
                        log.warn("Failed to read PNG image from provided bytes");
                    }
                } catch (IOException e) {
                    log.error("Failed to load PNG image: {}", e.getMessage(), e);
                }
            }

            graphics.dispose();

            ByteArrayOutputStream originalOutputStream = new ByteArrayOutputStream();
            ImageIO.write(originalImage, "PNG", originalOutputStream);
            byte[] originalBytes = originalOutputStream.toByteArray();

            BufferedImage resizedImage    = new BufferedImage(config.getOutputSize(), config.getOutputSize(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D    resizedGraphics = resizedImage.createGraphics();
            resizedGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            resizedGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            resizedGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            resizedGraphics.drawImage(originalImage, 0, 0, config.getOutputSize(), config.getOutputSize(), null);
            resizedGraphics.dispose();

            ByteArrayOutputStream resizedOutputStream = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "PNG", resizedOutputStream);
            byte[] resizedBytes = resizedOutputStream.toByteArray();

            return new GeneratedImageResult(originalBytes, dark ? "Default Dark Avatar" : "Default Avatar", resizedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate default avatar: " + e.getMessage(), e);
        }
    }

    /**
     * Internal method to generate a default avatar image with optional custom background color.
     *
     * @param pngImageBytes   Optional PNG image bytes to render in the center of the image
     * @param dark            {@code true} to use a dark background (near-black) and mid-gray border;
     *                        {@code false} for the classic white background and light-gray border
     * @param backgroundColor CSS hex string for the background, or null to use default
     * @return GeneratedImageResult containing original and resized images
     */
    private GeneratedImageResult generateDefaultAvatarInternal(byte[] pngImageBytes, boolean dark, String backgroundColor) {
        log.warn("Stable Diffusion not available, using default {} avatar (custom background: {})", dark ? "dark" : "light", backgroundColor);
        try {
            int originalSize = config.getGenerationSize();

            int   targetDisplaySize                = 24;
            float scalingRatioToSmallest           = (float) targetDisplaySize / (float) originalSize;
            float minBorderThicknessInSmallestSize = 1.0f;
            float borderThickness                  = Math.max(2.0f, minBorderThicknessInSmallestSize / scalingRatioToSmallest);
            float dashLength                       = 5.0f / scalingRatioToSmallest;

            BufferedImage originalImage = new BufferedImage(originalSize, originalSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D    graphics      = originalImage.createGraphics();

            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Background: use provided color if valid, else fallback to default
            Color bgColor = dark ? new Color(30, 30, 30) : Color.WHITE;
            if (backgroundColor != null) {
                try {
                    bgColor = Color.decode(backgroundColor);
                } catch (Exception e) {
                    log.debug("Could not parse custom background color '{}', using default", backgroundColor);
                }
            }
            graphics.setColor(bgColor);
            graphics.fillRect(0, 0, originalSize, originalSize);

            // Border: mid-gray for dark mode, light gray for light mode
            graphics.setColor(dark ? new Color(120, 120, 120) : new Color(211, 211, 211));
            float[] dashPattern = {dashLength, dashLength};
            graphics.setStroke(new BasicStroke(borderThickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dashPattern, 0.0f));

            float halfStroke  = borderThickness / 2.0f;
            int   borderInset = (int) Math.ceil(halfStroke) + 2;
            graphics.drawRect(borderInset, borderInset, originalSize - (2 * borderInset), originalSize - (2 * borderInset));

            if (pngImageBytes != null && pngImageBytes.length > 0) {
                try {
                    BufferedImage pngImage = ImageIO.read(new ByteArrayInputStream(pngImageBytes));
                    if (pngImage != null) {
                        double widthRatio   = (double) originalSize / pngImage.getWidth();
                        double heightRatio  = (double) originalSize / pngImage.getHeight();
                        double scalingRatio = Math.min(widthRatio, heightRatio);
                        int    scaledWidth  = (int) (pngImage.getWidth() * scalingRatio);
                        int    scaledHeight = (int) (pngImage.getHeight() * scalingRatio);
                        int    imageX       = (originalSize - scaledWidth) / 2;
                        int    imageY       = (originalSize - scaledHeight) / 2;
                        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        graphics.drawImage(pngImage, imageX, imageY, scaledWidth, scaledHeight, null);
                    } else {
                        log.warn("Failed to read PNG image from provided bytes");
                    }
                } catch (IOException e) {
                    log.error("Failed to load PNG image: {}", e.getMessage(), e);
                }
            }

            graphics.dispose();

            ByteArrayOutputStream originalOutputStream = new ByteArrayOutputStream();
            ImageIO.write(originalImage, "PNG", originalOutputStream);
            byte[] originalBytes = originalOutputStream.toByteArray();

            BufferedImage resizedImage    = new BufferedImage(config.getOutputSize(), config.getOutputSize(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D    resizedGraphics = resizedImage.createGraphics();
            resizedGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            resizedGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            resizedGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            resizedGraphics.drawImage(originalImage, 0, 0, config.getOutputSize(), config.getOutputSize(), null);
            resizedGraphics.dispose();

            ByteArrayOutputStream resizedOutputStream = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "PNG", resizedOutputStream);
            byte[] resizedBytes = resizedOutputStream.toByteArray();

            return new GeneratedImageResult(originalBytes, dark ? "Default Dark Avatar" : "Default Avatar", resizedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate default avatar: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a default dark-background avatar image with dark background and dotted mid-gray border.
     * Used as a fallback when Stable Diffusion is not available for dark-mode avatar generation.
     * Uses the same sizes as configured for Stable Diffusion (generationSize and outputSize).
     *
     * @param iconName Optional icon name (without .png extension) to render in the center of the image.
     *                 Icons are loaded from META-INF/resources/ui/icons/ directory.
     *                 Pass null for no icon. Examples: "user", "cube", "lightbulb", "exit"
     * @return GeneratedImageResult containing original and resized images
     */
    public GeneratedImageResult generateDefaultDarkAvatar(String iconName) {
        log.warn("Stable Diffusion not available, using default dark avatar" + (iconName != null ? " with icon: " + iconName : ""));
        byte[] pngImageBytes = null;
        if (iconName != null && !iconName.trim().isEmpty()) {
            pngImageBytes = loadPngResource("META-INF/resources/ui/icons/" + iconName + ".png");
            if (pngImageBytes == null) {
                log.warn("Failed to load icon '{}', generating dark avatar without icon", iconName);
            }
        }
        return generateDefaultAvatarInternal(pngImageBytes, true);
    }

    /**
     * Generate a default dark-background avatar image with a custom background color and dotted mid-gray border.
     * Used as a fallback when Stable Diffusion is not available for dark-mode avatar generation.
     * Uses the same sizes as configured for Stable Diffusion (generationSize and outputSize).
     *
     * @param iconName        Optional icon name (without .png extension) to render in the center of the image.
     *                        Icons are loaded from META-INF/resources/ui/icons/ directory.
     * @param backgroundColor CSS hex string (e.g., "#000000") for the avatar background. If null or invalid, falls back to default.
     * @return GeneratedImageResult containing original and resized images
     */
    public GeneratedImageResult generateDefaultDarkAvatar(String iconName, String backgroundColor) {
        log.warn("Stable Diffusion not available, using default dark avatar{} with custom background color {}", iconName != null ? " with icon: " + iconName : "", backgroundColor);
        byte[] pngImageBytes = null;
        if (iconName != null && !iconName.trim().isEmpty()) {
            pngImageBytes = loadPngResource("META-INF/resources/ui/icons/" + iconName + ".png");
            if (pngImageBytes == null) {
                log.warn("Failed to load icon '{}', generating dark avatar without icon", iconName);
            }
        }
        return generateDefaultAvatarInternal(pngImageBytes, true, backgroundColor);
    }

    /**
     * Get the currently loaded Stable Diffusion model checkpoint name.
     *
     * @return The name of the currently loaded model, or null if unavailable
     */
    public String getCurrentModel() {
        StableDiffusionOptions options = getOptions();
        return options != null ? options.getSd_model_checkpoint() : null;
    }

    /**
     * Get the full Stable Diffusion options/configuration from the API.
     *
     * @return StableDiffusionOptions object, or null if unavailable
     */
    public StableDiffusionOptions getOptions() {
        try {
            StableDiffusionOptions options = webClient.get()
                    .uri("/sdapi/v1/options")
                    .retrieve()
                    .bodyToMono(StableDiffusionOptions.class)
                    .timeout(Duration.ofSeconds(2))
                    .block();
            if (options != null) {
//                log.info("Stable Diffusion options: {}", options);
            }
            return options;
        } catch (Exception e) {
            log.debug("Failed to get Stable Diffusion options: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get current progress of image generation.
     *
     * @return ProgressResponse with current progress, or null if not available
     */
    public ProgressResponse getProgress() {
        try {
            return webClient.get()
                    .uri("/sdapi/v1/progress")
                    .retrieve()
                    .bodyToMono(ProgressResponse.class)
                    .timeout(Duration.ofSeconds(2))
                    .block();
        } catch (Exception e) {
            log.debug("Failed to get progress: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generate an image from an initial image and a text prompt (img2img) using the configured output size.
     * Convenience overload — seed defaults to {@code -1} (random).
     *
     * @param initImage The initial image bytes (PNG or JPG)
     * @param prompt    The text description guiding the transformation
     * @return GeneratedImageResult containing original, resized images, the prompt, and the actual seed used
     * @throws StableDiffusionException if generation fails
     */
    public GeneratedImageResult img2imgWithOriginal(byte[] initImage, String prompt) throws StableDiffusionException {
        return img2imgWithOriginal(initImage, prompt, config.getOutputSize(), null, -1L);
    }

    /**
     * Generate an image from an initial image and a text prompt (img2img) with a specific seed.
     * Uses the configured output size and no progress callback.
     *
     * @param initImage The initial image bytes (PNG or JPG)
     * @param prompt    The text description guiding the transformation
     * @param seed      Seed to use; pass {@code -1} for a random seed
     * @return GeneratedImageResult containing original, resized images, the prompt, and the actual seed used
     * @throws StableDiffusionException if generation fails
     */
    public GeneratedImageResult img2imgWithOriginal(byte[] initImage, String prompt, long seed) throws StableDiffusionException {
        return img2imgWithOriginal(initImage, prompt, config.getOutputSize(), null, seed);
    }

    /**
     * Generate an image from an initial image and a text prompt (img2img) with both original and resized versions.
     * Seed defaults to {@code -1} (random).
     *
     * @param initImage        The initial image bytes (PNG or JPG)
     * @param prompt           The text description of the image to generate
     * @param outputSize       The desired output size (square image)
     * @param progressCallback Callback for progress updates (can be null)
     * @return GeneratedImageResult containing original, resized images, the prompt, and the actual seed used
     * @throws StableDiffusionException if generation fails
     */
    public GeneratedImageResult img2imgWithOriginal(byte[] initImage, String prompt, int outputSize, ProgressCallback progressCallback) throws StableDiffusionException {
        return img2imgWithOriginal(initImage, prompt, outputSize, progressCallback, -1L);
    }

    /**
     * Generate an image from an initial image and a text prompt (img2img) using a custom negative prompt.
     * Seed defaults to {@code -1} (random).
     *
     * @param initImage        The initial image bytes (PNG or JPG)
     * @param prompt           The text description of the image to generate
     * @param negativePrompt   Negative prompt; falls back to {@link #NEGATIVE_PROMPT} when null or blank
     * @param outputSize       The desired output size (square image)
     * @param progressCallback Callback for progress updates (can be null)
     * @return GeneratedImageResult containing original, resized images, the prompt, and the actual seed used
     * @throws StableDiffusionException if generation fails
     */
    public GeneratedImageResult img2imgWithOriginal(byte[] initImage, String prompt, String negativePrompt, int outputSize, ProgressCallback progressCallback) throws StableDiffusionException {
        return img2imgWithOriginal(initImage, prompt, negativePrompt, outputSize, progressCallback, -1L, config.getDefaultDenoisingStrength(), config.getCfgScale());
    }

    /**
     * Generate an image from an initial image and a text prompt (img2img) with both original and resized versions.
     *
     * @param initImage        The initial image bytes (PNG or JPG)
     * @param prompt           The text description of the image to generate
     * @param outputSize       The desired output size (square image)
     * @param progressCallback Callback for progress updates (can be null)
     * @param seed             Seed to use; pass {@code -1} for a random seed
     * @return GeneratedImageResult containing original, resized images, the prompt, and the actual seed used
     * @throws StableDiffusionException if generation fails
     */
    public GeneratedImageResult img2imgWithOriginal(byte[] initImage, String prompt, int outputSize, ProgressCallback progressCallback, long seed) throws StableDiffusionException {
        return img2imgWithOriginal(initImage, prompt, outputSize, progressCallback, seed, config.getDefaultDenoisingStrength());
    }

    /**
     * Generate an image from an initial image and a text prompt (img2img) with both original and resized versions.
     * Uses the configured CFG scale.
     *
     * @param initImage         The initial image bytes (PNG or JPG)
     * @param prompt            The text description of the image to generate
     * @param outputSize        The desired output size (square image)
     * @param progressCallback  Callback for progress updates (can be null)
     * @param seed              Seed to use; pass {@code -1} for a random seed
     * @param denoisingStrength How much to alter the init image: 0.0 = no change, 1.0 = completely new image
     * @return GeneratedImageResult containing original, resized images, the prompt, and the actual seed used
     * @throws StableDiffusionException if generation fails
     */
    public GeneratedImageResult img2imgWithOriginal(byte[] initImage, String prompt, int outputSize, ProgressCallback progressCallback, long seed, double denoisingStrength) throws StableDiffusionException {
        return img2imgWithOriginal(initImage, prompt, outputSize, progressCallback, seed, denoisingStrength, config.getCfgScale());
    }

    /**
     * Generate an image from an initial image and a text prompt (img2img) with both original and resized versions.
     *
     * @param initImage         The initial image bytes (PNG or JPG)
     * @param prompt            The text description of the image to generate
     * @param outputSize        The desired output size (square image)
     * @param progressCallback  Callback for progress updates (can be null)
     * @param seed              Seed to use; pass {@code -1} for a random seed
     * @param denoisingStrength How much to alter the init image: 0.0 = no change, 1.0 = completely new image
     * @param cfgScale          Classifier Free Guidance scale; higher values follow the prompt more strictly
     * @return GeneratedImageResult containing original, resized images, the prompt, and the actual seed used
     * @throws StableDiffusionException if generation fails
     */
    public GeneratedImageResult img2imgWithOriginal(byte[] initImage, String prompt, int outputSize, ProgressCallback progressCallback, long seed, double denoisingStrength, double cfgScale) throws StableDiffusionException {
        return img2imgWithOriginal(initImage, prompt, NEGATIVE_PROMPT, outputSize, progressCallback, seed, denoisingStrength, cfgScale);
    }

    /**
     * Generate an image from an initial image and a text prompt (img2img) using a custom negative prompt.
     *
     * @param initImage         The initial image bytes (PNG or JPG)
     * @param prompt            The text description of the image to generate
     * @param negativePrompt    Negative prompt guiding what to avoid; falls back to {@link #NEGATIVE_PROMPT} when null or blank
     * @param outputSize        The desired output size (square image)
     * @param progressCallback  Callback for progress updates (can be null)
     * @param seed              Seed to use; pass {@code -1} for a random seed
     * @param denoisingStrength How much to alter the init image: 0.0 = no change, 1.0 = completely new image
     * @param cfgScale          Classifier Free Guidance scale; higher values follow the prompt more strictly
     * @return GeneratedImageResult containing original, resized images, the prompt, and the actual seed used
     * @throws StableDiffusionException if generation fails
     */
    public GeneratedImageResult img2imgWithOriginal(byte[] initImage, String prompt, String negativePrompt, int outputSize, ProgressCallback progressCallback, long seed, double denoisingStrength, double cfgScale) throws StableDiffusionException {
        log.info("Generating image-to-image with prompt: '{}', seed: {}, output: {}x{}, denoising: {}, cfgScale: {}", prompt, seed, outputSize, outputSize, denoisingStrength, cfgScale);
        try {
            String resolvedNegativePrompt = (negativePrompt != null && !negativePrompt.isBlank()) ? negativePrompt : NEGATIVE_PROMPT;
            String base64Init = java.util.Base64.getEncoder().encodeToString(initImage);
            ImageToImageRequest request = ImageToImageRequest.builder()
                    .prompt(prompt)
                    .negativePrompt(resolvedNegativePrompt)
                    .steps(config.getDefaultSteps())
                    .samplerName(config.getDefaultSampler())
                    .cfgScale(cfgScale)
                    .width(config.getGenerationSize())
                    .height(config.getGenerationSize())
                    .batchSize(1)
                    .seed(seed)
                    .denoisingStrength(denoisingStrength)
                    .initImages(new String[]{base64Init})
                    .build();

            Thread progressThread = null;
            if (progressCallback != null) {
                progressThread = new Thread(() -> pollProgress(progressCallback));
                progressThread.setDaemon(true);
                progressThread.start();
            }

            try {
                ImageGenerationResponse response = webClient.post()
                        .uri("/sdapi/v1/img2img")
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(ImageGenerationResponse.class)
                        .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                        .block();

                if (response == null || response.getImages() == null || response.getImages().isEmpty()) {
                    throw new StableDiffusionException("No image returned from Stable Diffusion API (img2img)");
                }

                String base64Image   = response.getImages().getFirst();
                byte[] originalImage = java.util.Base64.getDecoder().decode(base64Image);
                byte[] resizedImage  = resizeImage(originalImage, outputSize);
                long   actualSeed    = parseSeedFromInfo(response.getInfo());
                log.info("Successfully generated and resized image-to-image to {}x{} with seed {}", outputSize, outputSize, actualSeed);
                GeneratedImageResult result = new GeneratedImageResult(originalImage, prompt, resizedImage, actualSeed);
                result.setNegativePrompt(resolvedNegativePrompt);
                return result;
            } finally {
                if (progressThread != null) {
                    progressThread.interrupt();
                }
            }
        } catch (Exception e) {
            log.error("Error generating image-to-image with Stable Diffusion", e);
            throw new StableDiffusionException("Failed to generate image-to-image: " + e.getMessage(), e);
        }
    }

    /**
     * Check if the Stable Diffusion API is available.
     *
     * @return true if the API is reachable, false otherwise
     */
    public boolean isAvailable() {
        try {
            String response = webClient.get()
                    .uri("/internal/ping")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(e -> Mono.just("error"))
                    .block();

            return response != null && !response.equals("error");
        } catch (Exception e) {
            log.debug("Stable Diffusion API not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Load a PNG image from classpath resources.
     *
     * @param resourcePath The path to the PNG resource (e.g., "META-INF/resources/ui/icons/cube.png")
     * @return PNG image bytes, or null if resource not found
     */
    private byte[] loadPngResource(String resourcePath) {
        try {
            Resource resource = resourcePatternResolver.getResource("classpath:" + resourcePath);
            if (resource.exists()) {
                try (java.io.InputStream inputStream = resource.getInputStream()) {
                    return inputStream.readAllBytes();
                }
            } else {
                log.warn("PNG resource not found: {}", resourcePath);
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to load PNG resource '{}': {}", resourcePath, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Parse the {@code seed} value from the JSON string returned in {@code ImageGenerationResponse.info}.
     * The info field contains a JSON object such as {@code {"seed": 1234567890, ...}}.
     *
     * @param info The raw info JSON string from the SD API response
     * @return The parsed seed, or {@code -1} if parsing fails or info is null
     */
    private long parseSeedFromInfo(String info) {
        if (info == null || info.isEmpty()) {
            return -1L;
        }
        try {
            int idx = info.indexOf("\"seed\":");
            if (idx < 0) {
                return -1L;
            }
            // Skip past "seed": and any whitespace
            String        rest = info.substring(idx + 7).stripLeading();
            StringBuilder sb   = new StringBuilder();
            for (char c : rest.toCharArray()) {
                if (Character.isDigit(c) || (sb.isEmpty() && c == '-')) {
                    sb.append(c);
                } else if (!sb.isEmpty()) {
                    break;
                }
            }
            return sb.isEmpty() ? -1L : Long.parseLong(sb.toString());
        } catch (Exception e) {
            log.debug("Failed to parse seed from SD info: {}", e.getMessage());
            return -1L;
        }
    }

    /**
     * Poll progress and call callback with updates.
     */
    private void pollProgress(ProgressCallback callback) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                ProgressResponse progress = getProgress();
                if (progress != null && progress.getState() != null) {
                    callback.onProgress(
                            progress.getProgress(),
                            progress.getState().getSamplingStep(),
                            progress.getState().getSamplingSteps()
                    );
                }
                Thread.sleep(500); // Poll every 500ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.debug("Error polling progress: {}", e.getMessage());
            }
        }
    }

    /**
     * Resize an image to the specified size (square).
     *
     * @param imageBytes The original image bytes
     * @param targetSize The target size (width and height)
     * @return Resized image as byte array (PNG format)
     * @throws IOException if image processing fails
     */
    private byte[] resizeImage(byte[] imageBytes, int targetSize) throws IOException {
        // Read original image
        ByteArrayInputStream inputStream   = new ByteArrayInputStream(imageBytes);
        BufferedImage        originalImage = ImageIO.read(inputStream);

        if (originalImage == null) {
            throw new IOException("Failed to read image from byte array");
        }

        // Create resized image
        BufferedImage resizedImage = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D    graphics     = resizedImage.createGraphics();

        // Use high-quality rendering
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw and scale image
        graphics.drawImage(originalImage, 0, 0, targetSize, targetSize, null);
        graphics.dispose();

        // Convert to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, "PNG", outputStream);

        return outputStream.toByteArray();
    }

    /**
     * Selects and loads a new Stable Diffusion model by name.
     *
     * @param modelName The filename of the model to load (e.g., "realisticVisionV60B1_v51HyperVAE.safetensors")
     * @return true if the model was set successfully, false otherwise
     */
    public boolean selectModel(String modelName) {
        try {
            log.info("Loading Stable Diffusion model '{}' (timeout: {}s) …", modelName, config.getModelLoadTimeoutSeconds());
            var requestBody = java.util.Map.of("sd_model_checkpoint", modelName);
            webClient.post()
                    .uri("/sdapi/v1/options")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(config.getModelLoadTimeoutSeconds()))
                    .block();
            log.info("Model '{}' loaded successfully.", modelName);
            return true;
        } catch (Exception e) {
            log.error("Failed to select model '{}': {}", modelName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generate an image from a text prompt with both original and resized versions.
     *
     * @param prompt The text description of the image to generate
     * @return GeneratedImageResult containing original, resized images, and the prompt
     * @throws StableDiffusionException if generation fails
     */
    public GeneratedImageResult text2ImgWithOriginal(String prompt) throws StableDiffusionException {
        return text2ImgWithOriginal(prompt, config.getOutputSize(), null, -1, config.getCfgScale());
    }

    /**
     * Generate an image from a text prompt with both original and resized versions.
     *
     * @param prompt           The text description of the image to generate
     * @param outputSize       The desired output size for the resized image (square image)
     * @param progressCallback Callback for progress updates (can be null)
     * @return GeneratedImageResult containing original, resized images, and the prompt
     * @throws StableDiffusionException if generation fails
     */
    public GeneratedImageResult text2ImgWithOriginal(String prompt, int outputSize, ProgressCallback progressCallback, long seed, double cfgScale) throws StableDiffusionException {
        return text2ImgWithOriginal(prompt, NEGATIVE_PROMPT, outputSize, progressCallback, seed, cfgScale);
    }

    /**
     * Generate an image from a text prompt with both original and resized versions using a custom negative prompt.
     *
     * @param prompt           The text description of the image to generate
     * @param negativePrompt   Negative prompt guiding what to avoid; falls back to {@link #NEGATIVE_PROMPT} when null or blank
     * @param outputSize       The desired output size for the resized image (square image)
     * @param progressCallback Callback for progress updates (can be null)
     * @param seed             Seed to use; pass {@code -1} for a random seed
     * @param cfgScale         Classifier Free Guidance scale
     * @return GeneratedImageResult containing original, resized images, and the prompt
     * @throws StableDiffusionException if generation fails
     */
    public GeneratedImageResult text2ImgWithOriginal(String prompt, String negativePrompt, int outputSize, ProgressCallback progressCallback, long seed, double cfgScale) throws StableDiffusionException {
        log.info("Generating image with original at size {}x{} and resized to {}x{}",
                config.getGenerationSize(), config.getGenerationSize(), outputSize, outputSize);

        try {
            // Build request
            String resolvedNegativePrompt = (negativePrompt != null && !negativePrompt.isBlank()) ? negativePrompt : NEGATIVE_PROMPT;
            ImageGenerationRequest request = ImageGenerationRequest.builder()
                    .prompt(prompt)
                    .negativePrompt(resolvedNegativePrompt)
                    .steps(config.getDefaultSteps())
                    .samplerName(config.getDefaultSampler())
                    .cfgScale(cfgScale)
                    .width(config.getGenerationSize())
                    .height(config.getGenerationSize())
                    .batchSize(1)
                    .seed(seed) // Random seed
                    .build();

            // Start progress polling if callback provided
            Thread progressThread = null;
            if (progressCallback != null) {
                progressThread = new Thread(() -> pollProgress(progressCallback));
                progressThread.setDaemon(true);
                progressThread.start();
            }

            try {
                // Call Stable Diffusion API
                ImageGenerationResponse response = webClient.post()
                        .uri("/sdapi/v1/txt2img")
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(ImageGenerationResponse.class)
                        .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                        .block();

                if (response == null || response.getImages() == null || response.getImages().isEmpty()) {
                    throw new StableDiffusionException("No image returned from Stable Diffusion API");
                }

                // Decode base64 image (original size)
                String base64Image   = response.getImages().getFirst();
                byte[] originalImage = Base64.getDecoder().decode(base64Image);

                // Resize image to desired output size
                byte[] resizedImage = resizeImage(originalImage, outputSize);

                long responseSeed = parseSeedFromInfo(response.getInfo());
                log.info("Successfully generated original ({}x{}) and resized ({}x{}) images with seed {}",
                        config.getGenerationSize(), config.getGenerationSize(), outputSize, outputSize, responseSeed);

                GeneratedImageResult result = new GeneratedImageResult(originalImage, prompt, resizedImage, responseSeed);
                result.setNegativePrompt(resolvedNegativePrompt);
                return result;
            } finally {
                // Stop progress polling
                if (progressThread != null) {
                    progressThread.interrupt();
                }
            }

        } catch (Exception e) {
            log.error("Error generating image with Stable Diffusion", e);
            throw new StableDiffusionException("Failed to generate image: " + e.getMessage(), e);
        }
    }

    /**
     * Functional interface for progress callbacks.
     */
    @FunctionalInterface
    public interface ProgressCallback {
        /**
         * Called when progress updates.
         *
         * @param progress   Current progress (0.0 to 1.0)
         * @param step       Current sampling step
         * @param totalSteps Total sampling steps
         */
        void onProgress(double progress, int step, int totalSteps);
    }


}
