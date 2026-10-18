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
import java.util.List;

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

    static BufferedImage captureHelpElementImage(WebDriver driver, WebElement element, double cssWidth, double cssHeight) throws IOException {
        return captureHelpElementImage(driver, element, cssWidth, cssHeight, false);
    }

    static BufferedImage captureHelpElementImage(WebDriver driver, WebElement element, double cssWidth, double cssHeight,
                                                 boolean visiblePanel) throws IOException {
        if (!Double.isFinite(cssWidth) || !Double.isFinite(cssHeight) || cssWidth <= 0 || cssHeight <= 0) {
            throw new IOException("Invalid help screenshot dimensions: " + cssWidth + "x" + cssHeight);
        }
        JavascriptExecutor javascript = (JavascriptExecutor) driver;
        HelpCaptureArea area = helpCaptureArea(javascript, element, cssWidth, cssHeight, false);
        if (!area.visible()) {
            logger.debug("Scrolling off-screen help capture into view");
            javascript.executeScript("""
                    const element = arguments[0], visiblePanel = %s;
                    let width = arguments[1], height = arguments[2];
                    const parentOf = node => node.parentElement || node.getRootNode().host;
                    const offset = (start, end, low, high) =>
                        visiblePanel && end - start > high - low ? Math.floor(start - low)
                            : start < low ? Math.floor(start - low) : Math.max(0, Math.ceil(end - high));
                    const scroll = (target, left, top, right, bottom, horizontal, vertical) => {
                        const rect = element.getBoundingClientRect();
                        target.scrollBy({
                            left: horizontal ? offset(rect.left, rect.left + Math.min(width, rect.width), left, right) : 0,
                            top: vertical ? offset(rect.top, rect.top + Math.min(height, rect.height), top, bottom) : 0,
                            behavior: 'instant'
                        });
                    };
                    for (let parent = parentOf(element); parent; parent = parentOf(parent)) {
                        if (parent === document.body || parent === document.documentElement) continue;
                        const style = getComputedStyle(parent), rect = parent.getBoundingClientRect();
                        // client dimensions are integers and can round beyond fractional clipping edges.
                        const right = Math.min(rect.right - parseFloat(style.borderRightWidth),
                            rect.left + parent.clientLeft + parent.clientWidth);
                        const bottom = Math.min(rect.bottom - parseFloat(style.borderBottomWidth),
                            rect.top + parent.clientTop + parent.clientHeight);
                        scroll(parent, rect.left + parent.clientLeft, rect.top + parent.clientTop,
                            right, bottom,
                            /^(auto|scroll|hidden)$/.test(style.overflowX), /^(auto|scroll|hidden)$/.test(style.overflowY));
                        if (visiblePanel) {
                            if (/^(auto|scroll|hidden|clip)$/.test(style.overflowX)) width = Math.min(width, parent.clientWidth);
                            if (/^(auto|scroll|hidden|clip)$/.test(style.overflowY)) height = Math.min(height, parent.clientHeight);
                        }
                    }
                    scroll(window, 0, 0, innerWidth, innerHeight, true, true);
                    """.formatted(visiblePanel), element, cssWidth, cssHeight);
        }
        javascript.executeAsyncScript("""
                const done = arguments[arguments.length - 1];
                requestAnimationFrame(() => requestAnimationFrame(() => done()));
                """);
        area = helpCaptureArea(javascript, element, cssWidth, cssHeight, visiblePanel);
        if (!area.visible()) {
            throw new IOException("Help screenshot area is clipped or does not fit in the viewport (visible panel: "
                    + visiblePanel + ", area: " + area + "): " + element);
        }
        if (visiblePanel) {
            logger.info("Capturing visible help panel {} ({}x{} of requested {}x{} CSS pixels)",
                    element, area.width(), area.height(), cssWidth, cssHeight);
        }
        BufferedImage viewport = ImageIO.read(new ByteArrayInputStream(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)));
        if (viewport == null) {
            throw new IOException("Could not decode viewport screenshot for " + element);
        }
        double scaleX = viewport.getWidth() / area.viewportWidth();
        double scaleY = viewport.getHeight() / area.viewportHeight();
        int left = (int) Math.floor(area.x() * scaleX);
        int top = (int) Math.floor(area.y() * scaleY);
        int right = (int) Math.ceil((area.x() + area.width()) * scaleX);
        int bottom = (int) Math.ceil((area.y() + area.height()) * scaleY);
        if (left < 0 || top < 0 || right > viewport.getWidth() || bottom > viewport.getHeight() || right <= left || bottom <= top) {
            throw new IOException("Help screenshot crop is outside the captured viewport: " + element);
        }
        return viewport.getSubimage(left, top, right - left, bottom - top);
    }

    private static HelpCaptureArea helpCaptureArea(JavascriptExecutor javascript, WebElement element,
                                                   double cssWidth, double cssHeight, boolean visiblePanel) throws IOException {
        Object result = javascript.executeAsyncScript("""
                const element = arguments[0], requestedWidth = arguments[1], requestedHeight = arguments[2];
                const visiblePanel = %s;
                const done = arguments[arguments.length - 1];
                const observer = new IntersectionObserver(entries => {
                    observer.disconnect();
                    const rect = entries[0].boundingClientRect, clip = entries[0].intersectionRect;
                    const width = Math.min(requestedWidth, rect.width, visiblePanel ? clip.right - rect.left : Infinity);
                    const height = Math.min(requestedHeight, rect.height, visiblePanel ? clip.bottom - rect.top : Infinity);
                    done([rect.left, rect.top, width, height, innerWidth, innerHeight,
                        rect.left >= clip.left && rect.top >= clip.top && rect.left + width <= clip.right && rect.top + height <= clip.bottom,
                        rect.width > 0 && rect.height > 0 && requestedWidth <= Math.ceil(rect.width) && requestedHeight <= Math.ceil(rect.height)]);
                });
                observer.observe(element);
                """.formatted(visiblePanel), element, cssWidth, cssHeight);
        if (!(result instanceof List<?> values) || values.size() != 8
                || !(values.get(6) instanceof Boolean visible) || !Boolean.TRUE.equals(values.get(7))) {
            throw new IOException("Invalid help screenshot bounds for " + element);
        }
        HelpCaptureArea area = new HelpCaptureArea(coordinate(values, 0), coordinate(values, 1),
                coordinate(values, 2), coordinate(values, 3), coordinate(values, 4), coordinate(values, 5), visible);
        if (area.width() <= 0 || area.height() <= 0 || area.viewportWidth() <= 0 || area.viewportHeight() <= 0) {
            throw new IOException("Empty help screenshot bounds for " + element);
        }
        return area;
    }

    private static double coordinate(List<?> values, int index) throws IOException {
        if (values.get(index) instanceof Number value && Double.isFinite(value.doubleValue())) {
            return value.doubleValue();
        }
        throw new IOException("Invalid help screenshot coordinate: " + values.get(index));
    }

    private record HelpCaptureArea(double x, double y, double width, double height,
                                   double viewportWidth, double viewportHeight, boolean visible) {
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