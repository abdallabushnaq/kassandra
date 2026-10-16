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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("UnitTest")
class ScreenshotImageWriterTest {
    @TempDir
    private Path directory;

    @Test
    void retainsIdenticalPixelsDespiteDifferentImageTypesAndPngBytes() throws IOException {
        BufferedImage previous    = image(20, 20, BufferedImage.TYPE_3BYTE_BGR, 0xff808080);
        BufferedImage candidate   = image(20, 20, BufferedImage.TYPE_INT_ARGB, 0xff808080);
        Path          destination = baseline(previous);
        byte[]        before      = Files.readAllBytes(destination);
        assertFalse(Arrays.equals(before, png(candidate)), "The test must exercise different PNG encodings");
        FileTime timestamp = FileTime.fromMillis(1_600_000_000_000L);
        Files.setLastModifiedTime(destination, timestamp);

        assertFalse(ScreenshotImageWriter.writeIfChanged(destination, candidate, 0, false));

        assertArrayEquals(before, Files.readAllBytes(destination));
        assertEquals(timestamp, Files.getLastModifiedTime(destination));
    }

    @ParameterizedTest
    @CsvSource({"0,4,false", "8,4,false", "16,4,false", "0,5,true", "8,5,true", "16,5,true"})
    void appliesToleranceToEveryColourChannel(int shift, int difference, boolean expectedWrite) throws IOException {
        Path          destination = baseline(image(20, 20, BufferedImage.TYPE_INT_ARGB, 0xff808080));
        BufferedImage candidate   = image(20, 20, BufferedImage.TYPE_INT_ARGB, 0xff808080);
        candidate.setRGB(10, 10, 0xff808080 + (difference << shift));

        assertEquals(expectedWrite, ScreenshotImageWriter.writeIfChanged(destination, candidate, 4, false));
        assertEquals(expectedWrite ? candidate.getRGB(10, 10) : 0xff808080, ImageIO.read(destination.toFile()).getRGB(10, 10));
    }

    @Test
    void replacesEvenOneHighContrastPixelInALargeImage() throws IOException {
        BufferedImage candidate   = image(1000, 1000, BufferedImage.TYPE_INT_ARGB, 0xff808080);
        Path          destination = baseline(candidate);
        candidate.setRGB(500, 500, 0xff000000);

        assertTrue(ScreenshotImageWriter.writeIfChanged(destination, candidate, 4, false));
        assertEquals(0xff000000, ImageIO.read(destination.toFile()).getRGB(500, 500));
    }

    @Test
    void comparesCumulativeDriftAgainstRetainedBaseline() throws IOException {
        Path destination = baseline(image(2, 2, BufferedImage.TYPE_INT_ARGB, 0xff808080));
        assertFalse(ScreenshotImageWriter.writeIfChanged(destination, image(2, 2, BufferedImage.TYPE_INT_ARGB, 0xff848484), 4, false));
        assertTrue(ScreenshotImageWriter.writeIfChanged(destination, image(2, 2, BufferedImage.TYPE_INT_ARGB, 0xff858585), 4, false));
    }

    @Test
    void exactModeReplacesASingleChannelStep() throws IOException {
        Path destination = baseline(image(2, 2, BufferedImage.TYPE_INT_ARGB, 0xff808080));
        assertTrue(ScreenshotImageWriter.writeIfChanged(destination, image(2, 2, BufferedImage.TYPE_INT_ARGB, 0xff808081), 0, false));
    }

    @Test
    void alphaMustMatchExactlyEvenAtMaximumColourTolerance() throws IOException {
        Path destination = baseline(image(2, 2, BufferedImage.TYPE_INT_ARGB, 0xff808080));
        assertTrue(ScreenshotImageWriter.writeIfChanged(destination, image(2, 2, BufferedImage.TYPE_INT_ARGB, 0xfe808080), 255, false));
        assertEquals(0xfe808080, ImageIO.read(destination.toFile()).getRGB(0, 0));
    }

    @ParameterizedTest
    @CsvSource({"3,2", "2,3"})
    void replacesChangedDimensions(int width, int height) throws IOException {
        Path destination = baseline(image(2, 2, BufferedImage.TYPE_INT_ARGB, 0xff808080));
        assertTrue(ScreenshotImageWriter.writeIfChanged(destination, image(width, height, BufferedImage.TYPE_INT_ARGB, 0xff808080), 4, false));
        BufferedImage saved = ImageIO.read(destination.toFile());
        assertEquals(width, saved.getWidth());
        assertEquals(height, saved.getHeight());
    }

    @Test
    void createsMissingImageAndParentDirectories() throws IOException {
        Path destination = directory.resolve("new").resolve("screenshot.png");
        assertTrue(ScreenshotImageWriter.writeIfChanged(destination, image(2, 2, BufferedImage.TYPE_INT_RGB, 0xff808080), 4, false));
        assertEquals(0xff808080, ImageIO.read(destination.toFile()).getRGB(0, 0));
    }

    @Test
    void forceRefreshReplacesEquivalentImage() throws IOException {
        Path   destination = baseline(image(2, 2, BufferedImage.TYPE_3BYTE_BGR, 0xff808080));
        byte[] previous    = Files.readAllBytes(destination);
        assertTrue(ScreenshotImageWriter.writeIfChanged(destination, image(2, 2, BufferedImage.TYPE_INT_ARGB, 0xff808080), 4, true));
        assertFalse(Arrays.equals(previous, Files.readAllBytes(destination)));
    }

    @Test
    void rejectsUnreadableBaselineWithoutOverwritingIt() throws IOException {
        Path destination = directory.resolve("screenshot.png");
        Files.writeString(destination, "not a PNG");
        assertThrows(IOException.class, () -> ScreenshotImageWriter.writeIfChanged(destination, image(2, 2, BufferedImage.TYPE_INT_ARGB, 0xff808080), 4, false));
        assertEquals("not a PNG", Files.readString(destination));
    }

    @Test
    void rejectsInvalidToleranceAndUndecodableCandidate() {
        Path          destination = directory.resolve("screenshot.png");
        BufferedImage candidate   = image(2, 2, BufferedImage.TYPE_INT_ARGB, 0xff808080);
        assertThrows(IllegalArgumentException.class, () -> ScreenshotImageWriter.writeIfChanged(destination, candidate, -1, false));
        assertThrows(IllegalArgumentException.class, () -> ScreenshotImageWriter.writeIfChanged(destination, candidate, 256, false));
        assertThrows(IOException.class, () -> ScreenshotImageWriter.writeIfChanged(destination, null, 4, false));
        assertFalse(Files.exists(destination));
    }

    @Test
    void cleansTemporaryFileAndPreservesDestinationWhenReplacementFails() throws IOException {
        Path destination = Files.createDirectory(directory.resolve("screenshot.png"));
        Path existing    = Files.writeString(destination.resolve("keep.txt"), "keep");
        assertThrows(IOException.class, () -> ScreenshotImageWriter.writeIfChanged(destination, image(2, 2, BufferedImage.TYPE_INT_ARGB, 0xff808080), 4, true));
        assertEquals("keep", Files.readString(existing));
        try (var files = Files.list(directory)) {
            assertEquals(List.of(destination), files.toList());
        }
    }

    @Test
    void scopesComparisonToDocumentationDirectories() {
        SeleniumHandler handler = new SeleniumHandler();
        Path            help    = Path.of("src", "main", "resources", "META-INF", "resources", "ui", "images", "help");
        assertNull(handler.documentationScreenshotTolerance(help.resolve("light").resolve("control.png")));
        handler.setDocumentationScreenshotTolerance(4);
        for (Path root : List.of(help, Path.of("..", "kassandra.wiki", "light-screenshots"), Path.of("..", "kassandra.wiki", "dark-screenshots"))) {
            assertEquals(4, handler.documentationScreenshotTolerance(root.resolve("control.png")));
            assertNull(handler.documentationScreenshotTolerance(root.resolve("..").resolve("outside.png")));
        }
        assertNull(handler.documentationScreenshotTolerance(Path.of("target", "externalFiles", "screenshots", "failure.png")));
        handler.setDocumentationScreenshotTolerance(null);
        assertNull(handler.documentationScreenshotTolerance(help.resolve("dark").resolve("control.png")));
    }

    @Test
    void fullPageCaptureRetainsEquivalentImageAndReportsComparisonErrors() throws IOException {
        BufferedImage candidate   = image(2, 2, BufferedImage.TYPE_INT_ARGB, 0xff808080);
        Path          destination = baseline(candidate);
        byte[]        before      = Files.readAllBytes(destination);
        WebDriver     driver      = mock(WebDriver.class, withSettings().extraInterfaces(TakesScreenshot.class));
        when(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)).thenReturn(png(candidate));

        ScreenShotCreator.takeScreenShot(driver, destination.toString(), 4, false);
        assertArrayEquals(before, Files.readAllBytes(destination));
        Files.writeString(destination, "not a PNG");
        assertThrows(UncheckedIOException.class, () -> ScreenShotCreator.takeScreenShot(driver, destination.toString(), 4, false));
        assertEquals("not a PNG", Files.readString(destination));
        verify((TakesScreenshot) driver, never()).getScreenshotAs(OutputType.FILE);
    }

    @Test
    void diagnosticCaptureStillOverwritesWithoutComparing() throws IOException {
        Path   source   = directory.resolve("captured.png");
        byte[] captured = png(image(2, 2, BufferedImage.TYPE_INT_ARGB, 0xff808080));
        Files.write(source, captured);
        Path destination = directory.resolve("failure.png");
        Files.writeString(destination, "old diagnostic");
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(TakesScreenshot.class));
        when(((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE)).thenReturn(source.toFile());

        ScreenShotCreator.takeScreenShot(driver, destination.toString(), null, false);

        assertArrayEquals(captured, Files.readAllBytes(destination));
        verify((TakesScreenshot) driver, never()).getScreenshotAs(OutputType.BYTES);
    }

    @Test
    void dialogCaptureCropsAndRetainsEquivalentImageWithoutFallbackOnErrors() throws IOException {
        Path full = directory.resolve("full.png");
        ImageIO.write(image(4, 4, BufferedImage.TYPE_INT_ARGB, 0xff808080), "png", full.toFile());
        Path     destination = baseline(image(2, 2, BufferedImage.TYPE_INT_ARGB, 0xff808080));
        FileTime timestamp   = FileTime.fromMillis(1_600_000_000_000L);
        Files.setLastModifiedTime(destination, timestamp);
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(TakesScreenshot.class));
        when(((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE)).thenReturn(full.toFile());
        WebElement element = mock(WebElement.class);
        when(element.getLocation()).thenReturn(new Point(1, 1));
        when(element.getSize()).thenReturn(new Dimension(2, 2));

        ScreenShotCreator.takeElementScreenshot(driver, element, "dialog", destination.toString(), 4, false);
        assertEquals(timestamp, Files.getLastModifiedTime(destination));
        Files.writeString(destination, "not a PNG");
        assertThrows(UncheckedIOException.class, () -> ScreenShotCreator.takeElementScreenshot(driver, element, "dialog", destination.toString(), 4, false));
        assertEquals("not a PNG", Files.readString(destination));
        verify((TakesScreenshot) driver, times(2)).getScreenshotAs(OutputType.FILE);
        assertThrows(UncheckedIOException.class, () -> ScreenShotCreator.takeElementScreenshot(driver, null, "dialog", destination.toString(), 4, false));
    }

    private Path baseline(BufferedImage image) throws IOException {
        Path destination = directory.resolve("screenshot.png");
        assertTrue(ImageIO.write(image, "png", destination.toFile()));
        return destination;
    }

    private static BufferedImage image(int width, int height, int type, int argb) {
        BufferedImage image = new BufferedImage(width, height, type);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, argb);
            }
        }
        return image;
    }

    private static byte[] png(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", output));
        return output.toByteArray();
    }
}
