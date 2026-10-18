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

package de.bushnaq.abdalla.kassandra.ui.util.selenium;

import de.bushnaq.abdalla.kassandra.ui.util.selenium.ScreenshotComparisonReport.Difference;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
final class ScreenshotImageWriter {
    private ScreenshotImageWriter() {
    }

    static void validateTolerance(int channelTolerance) {
        if (channelTolerance < 0 || channelTolerance > 255) {
            throw new IllegalArgumentException("Screenshot channel tolerance must be between 0 and 255: " + channelTolerance);
        }
    }

    static boolean writeIfChanged(Path destination, BufferedImage candidate, int channelTolerance, boolean force) throws IOException {
        validateTolerance(channelTolerance);
        if (candidate == null) {
            throw new IOException("Could not decode screenshot for " + destination);
        }
        Path target = destination.toAbsolutePath().normalize();
        BufferedImage previous = null;
        Difference difference = null;
        if (!force && Files.exists(target)) {
            previous = ImageIO.read(target.toFile());
            if (previous == null) {
                throw new IOException("Could not decode existing screenshot " + target);
            }
            if (previous.getWidth() == candidate.getWidth() && previous.getHeight() == candidate.getHeight()) {
                int     maxDifference = 0;
                long changedPixels = 0;
                long alphaChangedPixels = 0;
                long pixelsAbove8 = 0;
                long pixelsAbove16 = 0;
                long pixelsAbove32 = 0;
                for (int y = 0; y < candidate.getHeight(); y++) {
                    for (int x = 0; x < candidate.getWidth(); x++) {
                        int oldPixel = previous.getRGB(x, y);
                        int newPixel = candidate.getRGB(x, y);
                        if (oldPixel != newPixel) {
                            changedPixels++;
                        }
                        if ((oldPixel >>> 24) != (newPixel >>> 24)) {
                            alphaChangedPixels++;
                        }
                        int pixelDifference = 0;
                        for (int shift = 0; shift <= 16; shift += 8) {
                            pixelDifference = Math.max(pixelDifference, Math.abs(((oldPixel >>> shift) & 255) - ((newPixel >>> shift) & 255)));
                        }
                        maxDifference = Math.max(maxDifference, pixelDifference);
                        if (pixelDifference > 8) {
                            pixelsAbove8++;
                        }
                        if (pixelDifference > 16) {
                            pixelsAbove16++;
                        }
                        if (pixelDifference > 32) {
                            pixelsAbove32++;
                        }
                    }
                }
                difference = new Difference(maxDifference, changedPixels, alphaChangedPixels,
                        pixelsAbove8, pixelsAbove16, pixelsAbove32);
                if (alphaChangedPixels == 0 && maxDifference <= channelTolerance) {
                    log.info("Retained screenshot {} (maximum RGB difference {}, tolerance {})", destination, maxDifference, channelTolerance);
                    ScreenshotComparisonReport.record(destination, candidate, previous, difference, channelTolerance, force, false);
                    return false;
                }
                log.info("Updating screenshot {} (maximum RGB difference {}, alpha changed: {})", destination, maxDifference, alphaChangedPixels != 0);
            } else {
                log.info("Updating screenshot {} (dimensions changed from {}x{} to {}x{})", destination,
                        previous.getWidth(), previous.getHeight(), candidate.getWidth(), candidate.getHeight());
            }
        }
        BufferedImage normalized = new BufferedImage(candidate.getWidth(), candidate.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < candidate.getHeight(); y++) {
            normalized.setRGB(0, y, candidate.getWidth(), 1, candidate.getRGB(0, y, candidate.getWidth(), 1, null, 0, candidate.getWidth()), 0, candidate.getWidth());
        }
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".screenshot-", ".png");
        try {
            if (!ImageIO.write(normalized, "png", temporary.toFile())) {
                throw new IOException("No PNG writer available for " + destination);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                log.debug("Atomic screenshot replacement is unavailable for {}; using a regular replacement", destination);
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        log.info("Saved screenshot {} (force refresh: {})", destination, force);
        ScreenshotComparisonReport.record(destination, candidate, previous, difference, channelTolerance, force, true);
        return true;
    }
}
