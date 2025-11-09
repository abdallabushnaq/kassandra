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

package de.bushnaq.abdalla.kassandra.ui.util.selenium;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ScreenShotCreator {
    private static final Logger logger = LoggerFactory.getLogger(ScreenShotCreator.class);

    static String fileNameGenerator(String displayName, String testmethod) {
        displayName = displayName.replace("'", "");
        String path = getScreenshotFolder();
        return path + testmethod + "(" + displayName + ")" + ".png";
    }

    private static String getScreenshotFolder() {
        return "target" + File.separator + "externalFiles" + File.separator + "screenshots" + File.separator;
    }

    /**
     * Takes a screenshot of a specific element identified by its ID
     *
     * @param driver
     * @param overlayElement The element to capture
     * @param fileName       The filename where the screenshot should be saved
     */
    public static void takeElementScreenshot(WebDriver driver, WebElement overlayElement, String dialogId, String fileName) {
        if (fileName != null) {
            try {
                if (overlayElement != null) {

                    // Take screenshot of the entire page
                    wait(1000);
                    File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                    wait(1000);
                    // Read the screenshot file into a BufferedImage
                    BufferedImage fullImg = ImageIO.read(screenshot);

                    // Get the location and size of the element
                    org.openqa.selenium.Point     location = overlayElement.getLocation();
                    org.openqa.selenium.Dimension size     = overlayElement.getSize();

                    logger.info("Taking screenshot of dialog overlay: {} at x: {}, y: {}, width: {}, height: {}", dialogId, location.x, location.y, size.getWidth(), size.getHeight());
                    // Make sure width and height are positive
                    if (size.getWidth() <= 0 || size.getHeight() <= 0) {
                        logger.warn("Dialog overlay has invalid dimensions: {}x{}", size.getWidth(), size.getHeight());
                        // Fallback to taking a full screenshot
                        takeScreenShot(driver, fileName);
                        return;
                    }
                    // Crop the image to only include the dialog
                    BufferedImage elementImg = fullImg.getSubimage(
                            location.getX(),
                            location.getY(),
                            size.getWidth(),
                            size.getHeight()
                    );

                    // Ensure the directory exists
                    File file = new File(fileName);
                    File dir  = file.getParentFile();
                    if (dir != null && !dir.exists()) {
                        dir.mkdirs();
                    }

                    // Save the cropped image
                    ImageIO.write(elementImg, "png", file);
                    logger.info("Dialog screenshot saved to: {}", fileName);
                } else {
                    logger.warn("Could not find overlay element for dialog: {}", dialogId);
                    takeScreenShot(driver, fileName);
                }
            } catch (Exception e) {
                logger.error("Failed to capture dialog screenshot", e);
                // Fallback to taking a full screenshot
                try {
                    takeScreenShot(driver, fileName);
                } catch (Exception ex) {
                    logger.error("Failed to take fallback screenshot", ex);
                }
            }
        }
    }

    public static void takeScreenShot(WebDriver driver, String fileName) {
        try {
            File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshotFile, new File(fileName));
            logger.info("Screenshot saved to: {}", fileName);
        } catch (IOException | NoSuchSessionException e) {
            throw new RuntimeException("Could not make screenshot {}" + fileName, e);
        }
    }

//    public static void takeScreenShot(WebDriver driver, String displayName, String testMethod) {
//        String fileName = fileNameGenerator(displayName, testMethod);
//        try {
//            File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
//            FileUtils.copyFile(screenshotFile, new File(fileName));
//            logger.info("Screenshot saved to: {}", fileName);
//        } catch (IOException | NoSuchSessionException e) {
//            throw new RuntimeException("Could not make screenshot {}" + fileName, e);
//        }
//    }

    static void wait(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Thread interrupted while waiting", e);
        }
    }
}