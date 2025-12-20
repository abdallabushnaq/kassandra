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

    private final StableDiffusionConfig config;
    private final WebClient             webClient;

    public StableDiffusionService(StableDiffusionConfig config) {
        this.config = config;

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
        if (currentModel != null && !currentModel.startsWith(config.getModelName())) {
            selectModel(config.getModelName());
        }
        getOptions();
    }

    /**
     * Generate an image from a text prompt with both original and resized versions.
     *
     * @param prompt The text description of the image to generate
     * @return GeneratedImageResult containing original, resized images, and the prompt
     * @throws StableDiffusionException if generation fails
     */
    public GeneratedImageResult generateImageWithOriginal(String prompt) throws StableDiffusionException {
        return generateImageWithOriginal(prompt, config.getOutputSize(), null);
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
    public GeneratedImageResult generateImageWithOriginal(String prompt, int outputSize, ProgressCallback progressCallback) throws StableDiffusionException {
        log.info("Generating image with original at size {}x{} and resized to {}x{}",
                config.getGenerationSize(), config.getGenerationSize(), outputSize, outputSize);

        try {
            // Build request
            ImageGenerationRequest request = ImageGenerationRequest.builder()
                    .prompt(prompt)
                    .negativePrompt("blurry, distorted, low quality, ugly, deformed, bad anatomy")
                    .steps(config.getDefaultSteps())
                    .samplerName(config.getDefaultSampler())
                    .cfgScale(config.getCfgScale())
                    .width(config.getGenerationSize())
                    .height(config.getGenerationSize())
                    .batchSize(1)
                    .seed(-1L) // Random seed
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

                log.info("Successfully generated original ({}x{}) and resized ({}x{}) images",
                        config.getGenerationSize(), config.getGenerationSize(), outputSize, outputSize);

                return new GeneratedImageResult(originalImage, prompt, resizedImage);
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
                log.info("Stable Diffusion options: {}", options);
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
     * Generate an image from an initial image and a text prompt (img2img) with both original and resized versions.
     *
     * @param initImage        The initial image bytes (PNG or JPG)
     * @param prompt           The text description of the image to generate
     * @param outputSize       The desired output size (square image)
     * @param progressCallback Callback for progress updates (can be null)
     * @return GeneratedImageResult containing original, resized images, and the prompt
     * @throws StableDiffusionException if generation fails
     */
    public GeneratedImageResult img2imgWithOriginal(byte[] initImage, String prompt, int outputSize, ProgressCallback progressCallback) throws StableDiffusionException {
        log.info("Generating image-to-image with prompt: '{}' at size {}x{}", prompt, outputSize, outputSize);
        try {
            String base64Init = java.util.Base64.getEncoder().encodeToString(initImage);
            ImageToImageRequest request = ImageToImageRequest.builder()
                    .prompt(prompt)
                    .negativePrompt("blurry, distorted, low quality, ugly, deformed, bad anatomy")
                    .steps(config.getDefaultSteps())
                    .samplerName(config.getDefaultSampler())
                    .cfgScale(config.getCfgScale())
                    .width(config.getGenerationSize())
                    .height(config.getGenerationSize())
                    .batchSize(1)
                    .seed(-1L)
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
                log.info("Successfully generated and resized image-to-image to {}x{}", outputSize, outputSize);
                return new GeneratedImageResult(originalImage, prompt, resizedImage);
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
            var requestBody = java.util.Map.of("sd_model_checkpoint", modelName);
            webClient.post()
                    .uri("/sdapi/v1/options")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            log.info("Requested model switch to: {}", modelName);
            return true;
        } catch (Exception e) {
            log.error("Failed to select model '{}': {}", modelName, e.getMessage());
            return false;
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
