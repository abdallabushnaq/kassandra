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
import org.openqa.selenium.JavascriptExecutor;
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
    void exportsSortedComparisonsWithPixelCountsAndExactThresholdBoundaries() throws IOException {
        ScreenshotComparisonReport report = new ScreenshotComparisonReport();
        BufferedImage previous = image(7, 1, BufferedImage.TYPE_INT_ARGB, 0xff808080);
        Path high = directory.resolve("b,detail.png");
        Path tied = directory.resolve("a.png");
        Path low = directory.resolve("c.png");
        for (Path destination : List.of(high, tied, low)) {
            assertTrue(ImageIO.write(previous, "png", destination.toFile()));
        }
        BufferedImage candidate = image(7, 1, BufferedImage.TYPE_INT_ARGB, 0xff808080);
        int[] differences = {8, 9, 16, 17, 32, 33};
        for (int x = 0; x < differences.length; x++) {
            int delta = differences[x];
            candidate.setRGB(x, 0, 0xff808080 + delta + (delta << 8) + (delta << 16));
        }
        candidate.setRGB(6, 0, 0xfe808080);
        report.startCollection();
        try {
            assertTrue(ScreenshotImageWriter.writeIfChanged(high, candidate, 8, false, 12.5, 34.75));
            report.stopCollection();
            report.startCollection();
            BufferedImage singlePixel = image(7, 1, BufferedImage.TYPE_INT_ARGB, 0xff808080);
            singlePixel.setRGB(0, 0, 0xff8080a1);
            assertTrue(ScreenshotImageWriter.writeIfChanged(tied, singlePixel, 8, false));
            singlePixel.setRGB(0, 0, 0xff808088);
            assertFalse(ScreenshotImageWriter.writeIfChanged(low, singlePixel, 8, false));
        } finally {
            report.stopCollection();
        }

        Path csv = directory.resolve("reports").resolve("discrepancies.csv");
        report.write(csv);
        List<String> lines = Files.readAllLines(csv);
        assertEquals(4, lines.size());
        assertEquals("png,status,tolerance,forced,previous_width,previous_height,width,height,capture_x_css,capture_y_css,"
                + "max_rgb_difference,changed_pixels,changed_percent,alpha_changed_pixels,pixels_above_8,pixels_above_16,pixels_above_32",
                lines.getFirst());
        assertTrue(lines.get(1).startsWith("\"" + tied.toAbsolutePath() + "\","));
        assertTrue(lines.get(1).endsWith(",UPDATED,8,false,7,1,7,1,,,33,1,14.285714,0,1,1,1"));
        assertTrue(lines.get(2).startsWith("\"" + high.toAbsolutePath() + "\","));
        assertTrue(lines.get(2).endsWith(",UPDATED,8,false,7,1,7,1,12.5,34.75,33,7,100.000000,1,5,3,1"));
        assertTrue(lines.get(3).endsWith(",RETAINED,8,false,7,1,7,1,,,8,1,14.285714,0,0,0,0"));
    }

    @Test
    void reportsNonComparableCapturesSeparatelyWithoutChangingForceRefresh() throws IOException {
        ScreenshotComparisonReport report = new ScreenshotComparisonReport();
        Path created = directory.resolve("a-new.png");
        Path resized = directory.resolve("b-resized.png");
        Path forced = directory.resolve("c-forced.png");
        assertTrue(ImageIO.write(image(2, 1, BufferedImage.TYPE_INT_ARGB, 0xff808080), "png", resized.toFile()));
        Files.writeString(forced, "undecodable baseline deliberately bypassed by force refresh");
        report.startCollection();
        try {
            assertTrue(ScreenshotImageWriter.writeIfChanged(created, image(3, 1, BufferedImage.TYPE_INT_ARGB, 0xff808080), 8, false));
            assertTrue(ScreenshotImageWriter.writeIfChanged(resized, image(3, 1, BufferedImage.TYPE_INT_ARGB, 0xff808080), 8, false));
            assertTrue(ScreenshotImageWriter.writeIfChanged(forced, image(3, 1, BufferedImage.TYPE_INT_ARGB, 0xff808080), 8, true));
        } finally {
            report.stopCollection();
        }
        Path csv = directory.resolve("discrepancies.csv");
        report.write(csv);
        List<String> lines = Files.readAllLines(csv);
        assertEquals(4, lines.size());
        assertTrue(lines.get(1).endsWith(",CREATED,8,false,,,3,1,,,,,,,,,"));
        assertTrue(lines.get(2).endsWith(",DIMENSIONS_CHANGED,8,false,2,1,3,1,,,,,,,,,"));
        assertTrue(lines.get(3).endsWith(",FORCED,8,true,,,3,1,,,,,,,,,"));
        for (String line : lines) {
            assertEquals(17, line.split(",", -1).length);
        }
    }

    @Test
    void reportsAlphaOnlyChangesAndRetainsCompletedResultsAfterFailure() throws IOException {
        ScreenshotComparisonReport report = new ScreenshotComparisonReport();
        Path destination = baseline(image(2, 2, BufferedImage.TYPE_INT_ARGB, 0xff808080));
        report.startCollection();
        try {
            assertTrue(ScreenshotImageWriter.writeIfChanged(destination, image(2, 2, BufferedImage.TYPE_INT_ARGB, 0xfe808080), 255, false));
            Path invalid = directory.resolve("invalid.png");
            Files.writeString(invalid, "not a PNG");
            assertThrows(IOException.class, () -> ScreenshotImageWriter.writeIfChanged(invalid,
                    image(2, 2, BufferedImage.TYPE_INT_ARGB, 0xff808080), 8, false));
        } finally {
            report.stopCollection();
        }
        Path csv = directory.resolve("discrepancies.csv");
        report.write(csv);
        List<String> lines = Files.readAllLines(csv);
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).endsWith(",UPDATED,255,false,2,2,2,2,,,0,4,100.000000,4,0,0,0"));
    }

    @Test
    void scopesCollectionAndOverwritesPreviousReportIncludingEmptyRuns() throws IOException {
        ScreenshotComparisonReport report = new ScreenshotComparisonReport();
        report.startCollection();
        try {
            assertThrows(IllegalStateException.class, report::startCollection);
            ScreenshotImageWriter.writeIfChanged(directory.resolve("collected.png"), image(1, 1, BufferedImage.TYPE_INT_ARGB, 0xff808080), 8, false);
        } finally {
            report.stopCollection();
        }
        ScreenshotImageWriter.writeIfChanged(directory.resolve("not-collected.png"), image(1, 1, BufferedImage.TYPE_INT_ARGB, 0xff808080), 8, false);
        Path csv = directory.resolve("discrepancies.csv");
        report.write(csv);
        assertEquals(2, Files.readAllLines(csv).size());
        new ScreenshotComparisonReport().write(csv);
        assertEquals(1, Files.readAllLines(csv).size());
        assertThrows(IOException.class, () -> report.write(directory));
    }

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
        WebDriver     driver      = mock(WebDriver.class, withSettings().extraInterfaces(TakesScreenshot.class, JavascriptExecutor.class));
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
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(TakesScreenshot.class, JavascriptExecutor.class));
        when(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)).thenReturn(captured);

        ScreenShotCreator.takeScreenShot(driver, destination.toString(), null, false);

        assertArrayEquals(captured, Files.readAllBytes(destination));
        verify((TakesScreenshot) driver, times(1)).getScreenshotAs(OutputType.BYTES);
    }

    @Test
    void dialogCaptureCropsAndRetainsEquivalentImageWithoutFallbackOnErrors() throws IOException {
        Path full = directory.resolve("full.png");
        ImageIO.write(image(4, 4, BufferedImage.TYPE_INT_ARGB, 0xff808080), "png", full.toFile());
        Path     destination = baseline(image(2, 2, BufferedImage.TYPE_INT_ARGB, 0xff808080));
        FileTime timestamp   = FileTime.fromMillis(1_600_000_000_000L);
        Files.setLastModifiedTime(destination, timestamp);
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(TakesScreenshot.class, JavascriptExecutor.class));
        when(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)).thenReturn(png(ImageIO.read(full.toFile())));
        WebElement element = mock(WebElement.class);
        when(element.getLocation()).thenReturn(new Point(1, 1));
        when(element.getSize()).thenReturn(new Dimension(2, 2));

        ScreenshotComparisonReport report = new ScreenshotComparisonReport();
        report.startCollection();
        try {
            ScreenShotCreator.takeElementScreenshot(driver, element, "dialog", destination.toString(), 4, false);
        } finally {
            report.stopCollection();
        }
        assertEquals(timestamp, Files.getLastModifiedTime(destination));
        Path csv = directory.resolve("dialog-discrepancies.csv");
        report.write(csv);
        assertTrue(Files.readAllLines(csv).get(1).endsWith(",RETAINED,4,false,2,2,2,2,1.0,1.0,0,0,0.000000,0,0,0,0"));
        Files.writeString(destination, "not a PNG");
        assertThrows(UncheckedIOException.class, () -> ScreenShotCreator.takeElementScreenshot(driver, element, "dialog", destination.toString(), 4, false));
        assertEquals("not a PNG", Files.readString(destination));
        verify((TakesScreenshot) driver, times(2)).getScreenshotAs(OutputType.BYTES);
        assertThrows(UncheckedIOException.class, () -> ScreenShotCreator.takeElementScreenshot(driver, null, "dialog", destination.toString(), 4, false));
    }

    @Test
    void cropsVisibleHelpFromViewportWithoutScrollingOrNativeElementCapture() throws IOException {
        BufferedImage viewport = image(10, 8, BufferedImage.TYPE_INT_ARGB, 0xff808080);
        viewport.setRGB(2, 3, 0xff123456);
        viewport.setRGB(3, 4, 0xffabcdef);
        WebDriver driver = helpCaptureDriver(viewport);
        WebElement element = mock(WebElement.class);
        JavascriptExecutor javascript = (JavascriptExecutor) driver;
        when(javascript.executeAsyncScript(contains("done([rect.left"), same(element), eq(2.0), eq(2.0)))
                .thenReturn(List.of(2, 3, 2, 2, 10, 8, true, true));

        BufferedImage crop = ScreenShotCreator.captureHelpElementImage(driver, element, 2, 2);

        assertEquals(2, crop.getWidth());
        assertEquals(2, crop.getHeight());
        assertEquals(0xff123456, crop.getRGB(0, 0));
        assertEquals(0xffabcdef, crop.getRGB(1, 1));
        verify(javascript, never()).executeScript(contains("scrollBy"), any(), any(), any());
        verify(javascript).executeAsyncScript(contains("requestAnimationFrame"));
        verify(element, never()).getScreenshotAs(any());
    }

    @Test
    void cropsHelpUsingViewportPixelScaleAndOutwardRoundedFractionalBounds() throws IOException {
        BufferedImage viewport = image(12, 10, BufferedImage.TYPE_INT_ARGB, 0xff808080);
        viewport.setRGB(2, 1, 0xff123456);
        viewport.setRGB(6, 4, 0xffabcdef);
        WebDriver driver = helpCaptureDriver(viewport);
        WebElement element = mock(WebElement.class);
        when(((JavascriptExecutor) driver).executeAsyncScript(contains("done([rect.left"), same(element), eq(2.0), eq(2.0)))
                .thenReturn(List.of(1.25, 0.5, 2, 2, 6, 5, true, true));

        BufferedImage crop = ScreenShotCreator.captureHelpElementImage(driver, element, 2, 2);

        assertEquals(5, crop.getWidth());
        assertEquals(4, crop.getHeight());
        assertEquals(0xff123456, crop.getRGB(0, 0));
        assertEquals(0xffabcdef, crop.getRGB(4, 3));
    }

    @Test
    void scrollsOffscreenHelpOnceThenUsesUpdatedViewportCoordinates() throws IOException {
        BufferedImage viewport = image(10, 8, BufferedImage.TYPE_INT_ARGB, 0xff808080);
        viewport.setRGB(2, 3, 0xff123456);
        WebDriver driver = helpCaptureDriver(viewport);
        WebElement element = mock(WebElement.class);
        JavascriptExecutor javascript = (JavascriptExecutor) driver;
        when(javascript.executeAsyncScript(contains("done([rect.left"), same(element), eq(2.0), eq(2.0)))
                .thenReturn(List.of(2, 12, 2, 2, 10, 8, false, true), List.of(2, 3, 2, 2, 10, 8, true, true));

        BufferedImage crop = ScreenShotCreator.captureHelpElementImage(driver, element, 2, 2);

        assertEquals(0xff123456, crop.getRGB(0, 0));
        verify(javascript).executeScript(contains("scrollBy"), same(element), eq(2.0), eq(2.0));
        verify(element, never()).getScreenshotAs(any());
    }

    @Test
    void rejectsClippedHelpInsteadOfSavingPartialImages() throws IOException {
        WebDriver driver = helpCaptureDriver(image(10, 8, BufferedImage.TYPE_INT_ARGB, 0xff808080));
        WebElement element = mock(WebElement.class);
        when(((JavascriptExecutor) driver).executeAsyncScript(contains("done([rect.left"), same(element), eq(2.0), eq(2.0)))
                .thenReturn(List.of(2, 12, 2, 2, 10, 8, false, true));

        IOException error = assertThrows(IOException.class, () -> ScreenShotCreator.captureHelpElementImage(driver, element, 2, 2));

        assertTrue(error.getMessage().contains("clipped"));
        verify((TakesScreenshot) driver, never()).getScreenshotAs(any());
    }

    @Test
    void capturesTopLeftVisibleAreaOnlyForExplicitlyOversizedPanels() throws IOException {
        BufferedImage viewport = image(10, 8, BufferedImage.TYPE_INT_ARGB, 0xff808080);
        viewport.setRGB(1, 2, 0xff123456);
        viewport.setRGB(4, 7, 0xffabcdef);
        WebDriver driver = helpCaptureDriver(viewport);
        WebElement element = mock(WebElement.class);
        JavascriptExecutor javascript = (JavascriptExecutor) driver;
        when(javascript.executeAsyncScript(contains("done([rect.left"), same(element), eq(4.0), eq(20.0)))
                .thenReturn(List.of(1, 15, 4, 20, 10, 8, false, true), List.of(1, 2, 4, 6, 10, 8, true, true));

        BufferedImage crop = ScreenShotCreator.captureHelpElementImage(driver, element, 4, 20, true);

        assertEquals(4, crop.getWidth());
        assertEquals(6, crop.getHeight());
        assertEquals(0xff123456, crop.getRGB(0, 0));
        assertEquals(0xffabcdef, crop.getRGB(3, 5));
        verify(javascript).executeScript(contains("visiblePanel = true"), same(element), eq(4.0), eq(20.0));
        verify(javascript).executeAsyncScript(contains("const visiblePanel = true"), same(element), eq(4.0), eq(20.0));
        verify(element, never()).getScreenshotAs(any());
    }

    @Test
    void visiblePanelModeDoesNotScrollWhenTheWholePanelAlreadyFits() throws IOException {
        WebDriver driver = helpCaptureDriver(image(10, 8, BufferedImage.TYPE_INT_ARGB, 0xff123456));
        WebElement element = mock(WebElement.class);
        JavascriptExecutor javascript = (JavascriptExecutor) driver;
        when(javascript.executeAsyncScript(contains("done([rect.left"), same(element), eq(2.0), eq(2.0)))
                .thenReturn(List.of(2, 3, 2, 2, 10, 8, true, true));

        BufferedImage crop = ScreenShotCreator.captureHelpElementImage(driver, element, 2, 2, true);

        assertEquals(2, crop.getWidth());
        assertEquals(2, crop.getHeight());
        assertEquals(0xff123456, crop.getRGB(0, 0));
        verify(javascript, never()).executeScript(contains("scrollBy"), any(), any(), any());
    }

    @Test
    void rejectsVisiblePanelCaptureWhenItsTopLeftRemainsClipped() throws IOException {
        WebDriver driver = helpCaptureDriver(image(10, 8, BufferedImage.TYPE_INT_ARGB, 0xff808080));
        WebElement element = mock(WebElement.class);
        when(((JavascriptExecutor) driver).executeAsyncScript(contains("done([rect.left"), same(element), eq(2.0), eq(20.0)))
                .thenReturn(List.of(2, -3, 2, 20, 10, 8, false, true), List.of(2, -3, 2, 8, 10, 8, false, true));

        IOException error = assertThrows(IOException.class, () -> ScreenShotCreator.captureHelpElementImage(driver, element, 2, 20, true));

        assertTrue(error.getMessage().contains("clipped"));
        verify((TakesScreenshot) driver, never()).getScreenshotAs(any());
    }

    @Test
    void scalesVisiblePanelCropsAtFractionalCoordinates() throws IOException {
        BufferedImage viewport = image(20, 16, BufferedImage.TYPE_INT_ARGB, 0xff808080);
        viewport.setRGB(2, 5, 0xff123456);
        viewport.setRGB(10, 15, 0xffabcdef);
        WebDriver driver = helpCaptureDriver(viewport);
        WebElement element = mock(WebElement.class);
        when(((JavascriptExecutor) driver).executeAsyncScript(contains("done([rect.left"), same(element), eq(4.0), eq(20.0)))
                .thenReturn(List.of(1.25, -10, 4, 20, 10, 8, false, true), List.of(1.25, 2.5, 4, 5.5, 10, 8, true, true));

        ScreenShotCreator.HelpElementCapture capture = ScreenShotCreator.captureHelpElement(driver, element, 4, 20, true);
        BufferedImage crop = capture.image();

        assertEquals(1.25, capture.x());
        assertEquals(2.5, capture.y());
        assertEquals(9, crop.getWidth());
        assertEquals(11, crop.getHeight());
        assertEquals(0xff123456, crop.getRGB(0, 0));
        assertEquals(0xffabcdef, crop.getRGB(8, 10));
    }

    @Test
    void rejectsInvalidHelpDimensionsBoundsAndUndecodableViewports() throws IOException {
        WebDriver driver = helpCaptureDriver(image(10, 8, BufferedImage.TYPE_INT_ARGB, 0xff808080));
        WebElement element = mock(WebElement.class);
        for (double invalid : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IOException.class, () -> ScreenShotCreator.captureHelpElementImage(driver, element, invalid, 2));
            assertThrows(IOException.class, () -> ScreenShotCreator.captureHelpElementImage(driver, element, 2, invalid));
        }
        JavascriptExecutor javascript = (JavascriptExecutor) driver;
        when(javascript.executeAsyncScript(contains("done([rect.left"), same(element), eq(2.0), eq(2.0)))
                .thenReturn(List.of(2, 3, 2, 2, 10, 8, true, false));
        assertThrows(IOException.class, () -> ScreenShotCreator.captureHelpElementImage(driver, element, 2, 2));
        when(javascript.executeAsyncScript(contains("done([rect.left"), same(element), eq(2.0), eq(2.0)))
                .thenReturn(List.of(2, 3, 2, 2, 10, 8, true, true));
        when(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)).thenReturn(new byte[]{1, 2, 3});
        assertThrows(IOException.class, () -> ScreenShotCreator.captureHelpElementImage(driver, element, 2, 2));
    }

    private WebDriver helpCaptureDriver(BufferedImage viewport) throws IOException {
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(TakesScreenshot.class, JavascriptExecutor.class));
        when(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)).thenReturn(png(viewport));
        return driver;
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
