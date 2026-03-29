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

import de.bushnaq.abdalla.kassandra.dto.Feature;
import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for StableDiffusionService.
 * Tests will only run if the Stable Diffusion API is available.
 */
@Tag("AiUnitTest")
@SpringBootTest
@Slf4j
public class StableDiffusionServiceTest {

    @Autowired
    private AvatarService           avatarService;
    @Autowired
    private StableDiffusionConfig   config;
    private Path                    outputDir;
    @Autowired
    private ResourcePatternResolver resourcePatternResolver;
    private StableDiffusionService  stableDiffusionService;

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
        stableDiffusionService = new StableDiffusionService(config, resourcePatternResolver);

        // Create output directory for test images
        outputDir = Paths.get("target", "test-output", "stablediffusion");
        Files.createDirectories(outputDir);
        log.info("Test output directory: {}", outputDir.toAbsolutePath());
    }

//    @Test
//    public void testGenerateImage_Avatar() throws Exception {
//        // Generate avatar-style image
//        String               prompt     = "portrait of a friendly robot character, cartoon style, simple background";
//        GeneratedImageResult result     = stableDiffusionService.generateImageWithOriginal(prompt);
//        byte[]               imageBytes = result.getResizedImage();
//
//        // Verify image was generated
//        assertNotNull(imageBytes, "Generated image should not be null");
//        assertTrue(imageBytes.length > 0, "Generated image should have content");
//
//        // Verify image dimensions
//        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
//        assertNotNull(image, "Image should be readable");
//        assertEquals(config.getOutputSize(), image.getWidth(), "Image width should match output size");
//        assertEquals(config.getOutputSize(), image.getHeight(), "Image height should match output size");
//
//        // Save image for visual verification
//        saveTestImage(imageBytes, "test-avatar-robot.png");
//        log.info("Generated avatar with prompt: '{}'", prompt);
//    }

//    @Test
//    public void testGenerateImage_CustomSize() throws Exception {
//        // Generate image with custom output size
//        String               prompt     = "a majestic lion, wildlife photography";
//        int                  customSize = 128;
//        GeneratedImageResult result     = stableDiffusionService.generateImageWithOriginal(prompt, customSize, null);
//        byte[]               imageBytes = result.getResizedImage();
//
//        // Verify image was generated
//        assertNotNull(imageBytes, "Generated image should not be null");
//        assertTrue(imageBytes.length > 0, "Generated image should have content");
//
//        // Verify image dimensions match custom size
//        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
//        assertNotNull(image, "Image should be readable");
//        assertEquals(customSize, image.getWidth(), "Image width should match custom size");
//        assertEquals(customSize, image.getHeight(), "Image height should match custom size");
//
//        // Save image for visual verification
//        saveTestImage(imageBytes, "test-custom-size-128.png");
//        log.info("Generated image with custom size: {}x{} pixels", customSize, customSize);
//    }

//    @Test
//    public void testGenerateImage_MultipleImages() throws Exception {
//        // Generate multiple images to test consistency
//        String[] prompts = {
//                "a red apple on a wooden table",
//                "a blue ocean wave",
//                "a green forest landscape"
//        };
//
//        for (int i = 0; i < prompts.length; i++) {
//            String               prompt     = prompts[i];
//            GeneratedImageResult result     = stableDiffusionService.generateImageWithOriginal(prompt);
//            byte[]               imageBytes = result.getResizedImage();
//
//            // Verify each image
//            assertNotNull(imageBytes, "Generated image " + i + " should not be null");
//            assertTrue(imageBytes.length > 0, "Generated image " + i + " should have content");
//
//            // Save for verification
//            saveTestImage(imageBytes, "test-multiple-" + i + ".png");
//            log.info("Generated image {}: '{}'", i + 1, prompt);
//
//            // Small delay between requests to avoid overwhelming the API
//            Thread.sleep(1000);
//        }
//    }

//    @Test
//    public void testGenerateImage_SimplePrompt() throws Exception {
//        // Generate image
//        String               prompt     = "a beautiful sunset over mountains, professional photography";
//        GeneratedImageResult result     = stableDiffusionService.generateImageWithOriginal(prompt);
//        byte[]               imageBytes = result.getResizedImage();
//
//        // Verify image was generated
//        assertNotNull(imageBytes, "Generated image should not be null");
//        assertTrue(imageBytes.length > 0, "Generated image should have content");
//
//        // Verify image can be read and has correct dimensions
//        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
//        assertNotNull(image, "Image should be readable");
//        assertEquals(config.getOutputSize(), image.getWidth(), "Image width should match output size");
//        assertEquals(config.getOutputSize(), image.getHeight(), "Image height should match output size");
//
//        // Save image for visual verification
//        saveTestImage(imageBytes, "test-sunset.png");
//        log.info("Generated image with prompt: '{}'", prompt);
//        log.info("Image size: {}x{} pixels, {} bytes", image.getWidth(), image.getHeight(), imageBytes.length);
//    }

    /**
     * Test avatar generation for a Feature entity using the default prompt.
     * Mirrors the flow used by ImagePromptDialog for features.
     *
     * @throws Exception if image generation fails
     */
    @Test
    public void testGenerateImage_FeatureAvatar() throws Exception {
        String featureName        = "AI Assistant";
        String lightBasePrompt    = Feature.getDefaultAvatarPrompt(featureName);
        String darkBasePrompt     = Feature.getDefaultDarkAvatarPrompt(featureName);
        String negativePrompt     = Feature.getDefaultAvatarNegativePrompt();
        String darkNegativePrompt = Feature.getDefaultDarkAvatarNegativePrompt();

        // Light avatar via AvatarService
        GeneratedImageResult lightResult = avatarService.generateLightAvatar(lightBasePrompt, negativePrompt);
        assertNotNull(lightResult.getResizedImage(), "Light feature avatar should not be null");
        assertTrue(lightResult.getResizedImage().length > 0, "Light feature avatar should have content");
        saveTestImage(lightResult.getResizedImage(), "feature-" + featureName + ".png");
        log.info("Generated light feature avatar for '{}' with seed {} and prompt: '{}'", featureName, lightResult.getSeed(), lightResult.getPrompt());

        // Dark avatar via AvatarService, pinned to the light seed
        GeneratedImageResult darkResult = avatarService.generateDarkAvatar(darkBasePrompt, darkNegativePrompt, lightResult);
        assertNotNull(darkResult.getResizedImage(), "Dark feature avatar should not be null");
        assertTrue(darkResult.getResizedImage().length > 0, "Dark feature avatar should have content");
        saveTestImage(darkResult.getResizedImage(), "feature-" + featureName + "-dark.png");
        log.info("Generated dark feature avatar for '{}' with seed {} and prompt: '{}'", featureName, darkResult.getSeed(), darkResult.getPrompt());
    }

    /**
     * Test avatar generation for a Product entity using the default prompt.
     * Mirrors the flow used by ImagePromptDialog for products.
     *
     * @throws Exception if image generation fails
     */
    @Test
    public void testGenerateImage_ProductAvatar() throws Exception {
        String productName        = "Kassandra";
        String lightBasePrompt    = Product.getDefaultAvatarPrompt(productName);
        String darkBasePrompt     = Product.getDefaultDarkAvatarPrompt(productName);
        String negativePrompt     = Product.getDefaultAvatarNegativePrompt();
        String darkNegativePrompt = Product.getDefaultDarkAvatarNegativePrompt();

        // Light avatar via AvatarService
        GeneratedImageResult lightResult = avatarService.generateLightAvatar(lightBasePrompt, negativePrompt);
        assertNotNull(lightResult.getResizedImage(), "Light product avatar should not be null");
        assertTrue(lightResult.getResizedImage().length > 0, "Light product avatar should have content");
        saveTestImage(lightResult.getResizedImage(), "product-" + productName + ".png");
        log.info("Generated light product avatar for '{}' with seed {} and prompt: '{}'", productName, lightResult.getSeed(), lightResult.getPrompt());

        // Dark avatar via AvatarService, pinned to the light seed
        GeneratedImageResult darkResult = avatarService.generateDarkAvatar(darkBasePrompt, darkNegativePrompt, lightResult);
        assertNotNull(darkResult.getResizedImage(), "Dark product avatar should not be null");
        assertTrue(darkResult.getResizedImage().length > 0, "Dark product avatar should have content");
        saveTestImage(darkResult.getResizedImage(), "product-" + productName + "-dark.png");
        log.info("Generated dark product avatar for '{}' with seed {} and prompt: '{}'", productName, darkResult.getSeed(), darkResult.getPrompt());
    }

    /**
     * Test avatar generation for a Sprint entity using the default prompt.
     * Mirrors the flow used by ImagePromptDialog for sprints.
     *
     * @throws Exception if image generation fails
     */
    @Test
    public void testGenerateImage_SprintAvatar() throws Exception {
        String sprintName         = "frankfurt 4";
        String lightBasePrompt    = Sprint.getDefaultAvatarPrompt(sprintName);
        String darkBasePrompt     = Sprint.getDefaultDarkAvatarPrompt(sprintName);
        String negativePrompt     = Sprint.getDefaultAvatarNegativePrompt();
        String darkNegativePrompt = Sprint.getDefaultDarkAvatarNegativePrompt();

        // Light avatar via AvatarService
        GeneratedImageResult lightResult = avatarService.generateLightAvatar(lightBasePrompt, negativePrompt);
        assertNotNull(lightResult.getResizedImage(), "Light sprint avatar should not be null");
        assertTrue(lightResult.getResizedImage().length > 0, "Light sprint avatar should have content");
        saveTestImage(lightResult.getResizedImage(), "sprint-" + sprintName + ".png");
        log.info("Generated light sprint avatar for '{}' with seed {} and prompt: '{}'", sprintName, lightResult.getSeed(), lightResult.getPrompt());

        // Dark avatar via AvatarService, pinned to the light seed
        GeneratedImageResult darkResult = avatarService.generateDarkAvatar(darkBasePrompt, darkNegativePrompt, lightResult);
        assertNotNull(darkResult.getResizedImage(), "Dark sprint avatar should not be null");
        assertTrue(darkResult.getResizedImage().length > 0, "Dark sprint avatar should have content");
        saveTestImage(darkResult.getResizedImage(), "sprint-" + sprintName + "-dark.png");
        log.info("Generated dark sprint avatar for '{}' with seed {} and prompt: '{}'", sprintName, darkResult.getSeed(), darkResult.getPrompt());
    }

    @Test
    public void testGenerateImage_UserAvatarAbdalla() throws Exception {
        // Mirror the exact flow used by ImagePromptDialog for a user named "Abdalla",
        // delegated through AvatarService.
        String userName           = "Abdalla";
        String lightPrompt        = User.getDefaultAvatarPrompt(userName);
        String darkPrompt         = User.getDefaultDarkAvatarPrompt(userName);
        String negativePrompt     = User.getDefaultAvatarNegativePrompt();
        String darkNegativePrompt = User.getDefaultDarkAvatarNegativePrompt();

        // ── Light avatar via AvatarService ────────────────────────────────────
        GeneratedImageResult lightResult = avatarService.generateLightAvatar(lightPrompt, negativePrompt);

        assertNotNull(lightResult.getResizedImage(), "Light avatar should not be null");
        assertTrue(lightResult.getResizedImage().length > 0, "Light avatar should have content");
        saveTestImage(lightResult.getResizedImage(), "user-" + userName + ".png");
        log.info("Generated light avatar for '{}' with seed {} and prompt: '{}'", userName, lightResult.getSeed(), lightResult.getPrompt());

        // ── Dark avatar via AvatarService, pinned to the light seed ───────────
        GeneratedImageResult darkResult = avatarService.generateDarkAvatar(darkPrompt, darkNegativePrompt, lightResult);

        assertNotNull(darkResult.getResizedImage(), "Dark avatar should not be null");
        assertTrue(darkResult.getResizedImage().length > 0, "Dark avatar should have content");
        saveTestImage(darkResult.getResizedImage(), "user-" + userName + "-dark.png");
        log.info("Generated dark avatar for '{}' with seed {} and prompt: '{}'", userName, darkResult.getSeed(), darkResult.getPrompt());
    }

    @Test
    public void testIsAvailable() {
        assertTrue(stableDiffusionService.isAvailable(), "Stable Diffusion API should be available");
    }
}
