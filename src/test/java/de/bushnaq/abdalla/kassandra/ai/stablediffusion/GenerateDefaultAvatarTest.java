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

import de.bushnaq.abdalla.kassandra.util.AbstractEntityGenerator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for the generateDefaultAvatar method in AbstractEntityGenerator.
 * Uses Spring Boot context to properly inject StableDiffusionConfig.
 */
@Tag("AiUnitTest")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
public class GenerateDefaultAvatarTest extends AbstractEntityGenerator {

    @Autowired
    private StableDiffusionConfig config;

    /**
     * Save test images to disk for manual inspection.
     */
    private void saveTestImages(GeneratedImageResult result) throws IOException {
        File testDir = new File("test-results/default-avatar");
        testDir.mkdirs();

        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(result.getOriginalImage()));
        ImageIO.write(originalImage, "PNG", new File(testDir, "default-avatar-original-" + config.getGenerationSize() + "x" + config.getGenerationSize() + ".png"));

        BufferedImage resizedImage = ImageIO.read(new ByteArrayInputStream(result.getResizedImage()));
        ImageIO.write(resizedImage, "PNG", new File(testDir, "default-avatar-resized-" + config.getOutputSize() + "x" + config.getOutputSize() + ".png"));
    }

    @Test
    public void testGenerateDefaultAvatar() throws IOException {
        // Generate default avatar without icon (pass null)
        GeneratedImageResult result = stableDiffusionService.generateDefaultAvatar(null);

        // Verify result is not null
        assertNotNull(result, "Generated result should not be null");
        assertNotNull(result.getOriginalImage(), "Original image should not be null");
        assertNotNull(result.getResizedImage(), "Resized image should not be null");
        assertNotNull(result.getPrompt(), "Prompt should not be null");

        // Verify the prompt
        assertEquals("Default Avatar", result.getPrompt(), "Prompt should be 'Default Avatar'");

        // Verify original image size matches config
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(result.getOriginalImage()));
        assertNotNull(originalImage, "Original image should be readable");
        assertEquals(config.getGenerationSize(), originalImage.getWidth(), "Original image width should match config.generationSize");
        assertEquals(config.getGenerationSize(), originalImage.getHeight(), "Original image height should match config.generationSize");

        // Verify resized image size matches config
        BufferedImage resizedImage = ImageIO.read(new ByteArrayInputStream(result.getResizedImage()));
        assertNotNull(resizedImage, "Resized image should be readable");
        assertEquals(config.getOutputSize(), resizedImage.getWidth(), "Resized image width should match config.outputSize");
        assertEquals(config.getOutputSize(), resizedImage.getHeight(), "Resized image height should match config.outputSize");

        // Verify that images are PNG format (contains PNG magic number)
        assertTrue(result.getOriginalImage().length > 0, "Original image should have content");
        assertTrue(result.getResizedImage().length > 0, "Resized image should have content");

        // Save images to disk for manual inspection (optional)
        saveTestImages(result);
    }

    @Test
    public void testGenerateDefaultAvatarWithIcon() throws IOException {
        // Generate default avatar with user icon
        GeneratedImageResult result = stableDiffusionService.generateDefaultAvatar("user");

        // Verify result is not null
        assertNotNull(result, "Generated result should not be null");
        assertNotNull(result.getOriginalImage(), "Original image should not be null");
        assertNotNull(result.getResizedImage(), "Resized image should not be null");

        // Save images to disk for manual inspection
        File testDir = new File("test-results/default-avatar");
        testDir.mkdirs();
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(result.getOriginalImage()));
        ImageIO.write(originalImage, "PNG", new File(testDir, "default-avatar-icon-original.png"));
    }

    @Test
    public void testListSvgResources() throws IOException {
        System.out.println("Listing all SVG resources:");
        org.springframework.core.io.support.ResourcePatternResolver resolver  = new org.springframework.core.io.support.PathMatchingResourcePatternResolver();
        org.springframework.core.io.Resource[]                      resources = resolver.getResources("classpath*:**/*.svg");
        for (org.springframework.core.io.Resource resource : resources) {
            System.out.println(resource.getURL());
        }
        System.out.println("End of SVG resources list.");
    }
}
