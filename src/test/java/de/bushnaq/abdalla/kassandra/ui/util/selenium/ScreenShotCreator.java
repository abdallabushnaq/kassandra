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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

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
     * @param driver         the browser to capture
     * @param overlayElement The element to capture
     * @param dialogId       the dialog identifier for logging
     * @param fileName       The filename where the screenshot should be saved
     */
    public static void takeElementScreenshot(WebDriver driver, WebElement overlayElement, String dialogId, String fileName) {
        if (fileName != null) {
            try {
                if (overlayElement != null) {

                    BufferedImage elementImg = captureElementImage(driver, overlayElement, dialogId);

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

    static void takeElementScreenshot(WebDriver driver, WebElement overlayElement, String dialogId, String fileName,
                                      Integer channelTolerance, boolean force) {
        if (channelTolerance == null) {
            takeElementScreenshot(driver, overlayElement, dialogId, fileName);
            return;
        }
        try {
            ScreenshotImageWriter.writeIfChanged(Path.of(fileName), captureElementImage(driver, overlayElement, dialogId), channelTolerance, force);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save documentation screenshot " + fileName, e);
        }
    }

    private static BufferedImage captureElementImage(WebDriver driver, WebElement element, String dialogId) throws IOException {
        if (element == null) {
            throw new IOException("Could not find overlay element for dialog " + dialogId);
        }
        wait(1000);
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        wait(1000);
        BufferedImage image = ImageIO.read(screenshot);
        Point location = element.getLocation();
        Dimension size = element.getSize();
        if (image == null || size.getWidth() <= 0 || size.getHeight() <= 0) {
            throw new IOException("Invalid dialog screenshot dimensions for " + dialogId);
        }
        logger.info("Taking screenshot of dialog overlay: {} at x: {}, y: {}, width: {}, height: {}", dialogId,
                location.x, location.y, size.getWidth(), size.getHeight());
        return image.getSubimage(location.getX(), location.getY(), size.getWidth(), size.getHeight());
    }

    static void takeScreenShot(WebDriver driver, String fileName, Integer channelTolerance, boolean force) {
        if (channelTolerance == null) {
            takeScreenShot(driver, fileName);
            return;
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)));
            ScreenshotImageWriter.writeIfChanged(Path.of(fileName), image, channelTolerance, force);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save documentation screenshot " + fileName, e);
        }
    }

    /**
     * Captures the current browser page without comparing it to previous captures.
     *
     * @param driver the browser to capture
     * @param fileName the destination PNG filename
     */
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