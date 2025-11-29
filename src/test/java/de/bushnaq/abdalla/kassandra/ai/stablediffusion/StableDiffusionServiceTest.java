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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for StableDiffusionService.
 * Tests will only run if the Stable Diffusion API is available.
 */
@SpringBootTest
@Slf4j
public class StableDiffusionServiceTest {

    @Autowired
    private StableDiffusionConfig  config;
    private Path                   outputDir;
    private StableDiffusionService stableDiffusionService;

    /**
     * Helper method to save test images for visual verification.
     */
    private void saveTestImage(byte[] imageBytes, String filename) throws IOException {
        Path outputPath = outputDir.resolve(filename);
        Files.write(outputPath, imageBytes);
        log.info("Saved test image to: {}", outputPath.toAbsolutePath());
    }

    @BeforeEach
    public void setUp() throws IOException {
        stableDiffusionService = new StableDiffusionService(config);

        // Create output directory for test images
        outputDir = Paths.get("target", "test-output", "stablediffusion");
        Files.createDirectories(outputDir);
        log.info("Test output directory: {}", outputDir.toAbsolutePath());
    }

    @Test
    public void testGenerateImage_Avatar() throws Exception {
        // Generate avatar-style image
        String prompt     = "portrait of a friendly robot character, cartoon style, simple background";
        byte[] imageBytes = stableDiffusionService.generateImage(prompt);

        // Verify image was generated
        assertNotNull(imageBytes, "Generated image should not be null");
        assertTrue(imageBytes.length > 0, "Generated image should have content");

        // Verify image dimensions
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        assertNotNull(image, "Image should be readable");
        assertEquals(config.getOutputSize(), image.getWidth(), "Image width should match output size");
        assertEquals(config.getOutputSize(), image.getHeight(), "Image height should match output size");

        // Save image for visual verification
        saveTestImage(imageBytes, "test-avatar-robot.png");
        log.info("Generated avatar with prompt: '{}'", prompt);
    }

    @Test
    public void testGenerateImage_CustomSize() throws Exception {
        // Generate image with custom output size
        String prompt     = "a majestic lion, wildlife photography";
        int    customSize = 128;
        byte[] imageBytes = stableDiffusionService.generateImage(prompt, customSize);

        // Verify image was generated
        assertNotNull(imageBytes, "Generated image should not be null");
        assertTrue(imageBytes.length > 0, "Generated image should have content");

        // Verify image dimensions match custom size
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        assertNotNull(image, "Image should be readable");
        assertEquals(customSize, image.getWidth(), "Image width should match custom size");
        assertEquals(customSize, image.getHeight(), "Image height should match custom size");

        // Save image for visual verification
        saveTestImage(imageBytes, "test-custom-size-128.png");
        log.info("Generated image with custom size: {}x{} pixels", customSize, customSize);
    }

    @Test
    public void testGenerateImage_MultipleImages() throws Exception {
        // Generate multiple images to test consistency
        String[] prompts = {
                "a red apple on a wooden table",
                "a blue ocean wave",
                "a green forest landscape"
        };

        for (int i = 0; i < prompts.length; i++) {
            String prompt     = prompts[i];
            byte[] imageBytes = stableDiffusionService.generateImage(prompt);

            // Verify each image
            assertNotNull(imageBytes, "Generated image " + i + " should not be null");
            assertTrue(imageBytes.length > 0, "Generated image " + i + " should have content");

            // Save for verification
            saveTestImage(imageBytes, "test-multiple-" + i + ".png");
            log.info("Generated image {}: '{}'", i + 1, prompt);

            // Small delay between requests to avoid overwhelming the API
            Thread.sleep(1000);
        }
    }

    @Test
    public void testGenerateImage_SimplePrompt() throws Exception {
        // Generate image
        String prompt     = "a beautiful sunset over mountains, professional photography";
        byte[] imageBytes = stableDiffusionService.generateImage(prompt);

        // Verify image was generated
        assertNotNull(imageBytes, "Generated image should not be null");
        assertTrue(imageBytes.length > 0, "Generated image should have content");

        // Verify image can be read and has correct dimensions
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        assertNotNull(image, "Image should be readable");
        assertEquals(config.getOutputSize(), image.getWidth(), "Image width should match output size");
        assertEquals(config.getOutputSize(), image.getHeight(), "Image height should match output size");

        // Save image for visual verification
        saveTestImage(imageBytes, "test-sunset.png");
        log.info("Generated image with prompt: '{}'", prompt);
        log.info("Image size: {}x{} pixels, {} bytes", image.getWidth(), image.getHeight(), imageBytes.length);
    }

    @Test
    public void testIsAvailable() {
        assertTrue(stableDiffusionService.isAvailable(), "Stable Diffusion API should be available");
    }
}
