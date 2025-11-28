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
    }

    /**
     * Generate an image from a text prompt.
     *
     * @param prompt The text description of the image to generate
     * @return byte array containing the generated image (PNG format, resized to configured output size)
     * @throws StableDiffusionException if generation fails
     */
    public byte[] generateImage(String prompt) throws StableDiffusionException {
        return generateImage(prompt, config.getOutputSize());
    }

    /**
     * Generate an image from a text prompt with a specific output size.
     *
     * @param prompt     The text description of the image to generate
     * @param outputSize The desired output size (square image)
     * @return byte array containing the generated image (PNG format)
     * @throws StableDiffusionException if generation fails
     */
    public byte[] generateImage(String prompt, int outputSize) throws StableDiffusionException {
        return generateImage(prompt, outputSize, null);
    }

    /**
     * Generate an image from a text prompt with a specific output size and progress callback.
     *
     * @param prompt           The text description of the image to generate
     * @param outputSize       The desired output size (square image)
     * @param progressCallback Callback for progress updates (can be null)
     * @return byte array containing the generated image (PNG format)
     * @throws StableDiffusionException if generation fails
     */
    public byte[] generateImage(String prompt, int outputSize, ProgressCallback progressCallback) throws StableDiffusionException {
        log.info("Generating image with prompt: '{}' at size {}x{}", prompt, outputSize, outputSize);

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

                // Decode base64 image
                String base64Image = response.getImages().getFirst();
                byte[] imageBytes  = Base64.getDecoder().decode(base64Image);

                // Resize image to desired output size
                byte[] resizedImage = resizeImage(imageBytes, outputSize);

                log.info("Successfully generated and resized image to {}x{}", outputSize, outputSize);
                return resizedImage;
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

