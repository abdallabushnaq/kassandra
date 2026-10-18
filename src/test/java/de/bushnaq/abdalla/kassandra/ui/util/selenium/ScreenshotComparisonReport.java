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
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.ui.util.selenium;

import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Collects documentation screenshot comparisons without changing replacement decisions.
 */
@Slf4j
public final class ScreenshotComparisonReport {
    private static final ThreadLocal<ScreenshotComparisonReport> CURRENT = new ThreadLocal<>();
    private final List<Entry> entries = new ArrayList<>();

    /**
     * Collects captures on the current test thread into this report.
     *
     * @throws IllegalStateException if another collection is already active on this thread
     */
    public void startCollection() {
        if (CURRENT.get() != null) {
            throw new IllegalStateException("Screenshot comparison collection is already active");
        }
        CURRENT.set(this);
    }

    /**
     * Stops this report's collection on the current thread; safe when setup failed before collection started.
     */
    public void stopCollection() {
        if (CURRENT.get() == this) {
            CURRENT.remove();
        }
    }

    /**
     * Exports all collected captures, highest maximum RGB difference first, and logs the maximum and every tied PNG.
     *
     * @param destination the UTF-8 CSV report destination
     * @throws IOException if the report cannot be written
     */
    public synchronized void write(Path destination) throws IOException {
        List<Entry> sorted = entries.stream()
                .sorted(Comparator.comparing((Entry entry) -> entry.difference() == null ? null : entry.difference().maxRgbDifference(),
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(entry -> entry.destination().toString()))
                .toList();
        List<String> lines = new ArrayList<>();
        lines.add("png,status,tolerance,forced,previous_width,previous_height,width,height,max_rgb_difference,changed_pixels,changed_percent,alpha_changed_pixels,pixels_above_8,pixels_above_16,pixels_above_32");
        for (Entry entry : sorted) {
            Difference difference = entry.difference();
            String metrics = difference == null ? ",,,,,," : String.format(Locale.ROOT, "%d,%d,%.6f,%d,%d,%d,%d",
                    difference.maxRgbDifference(), difference.changedPixels(),
                    100.0 * difference.changedPixels() / ((long) entry.width() * entry.height()),
                    difference.alphaChangedPixels(), difference.pixelsAbove8(), difference.pixelsAbove16(), difference.pixelsAbove32());
            lines.add(String.format(Locale.ROOT, "%s,%s,%d,%s,%s,%s,%d,%d,%s",
                    quote(entry.destination().toString()), entry.status(), entry.tolerance(), entry.forced(),
                    entry.previousWidth() == 0 ? "" : entry.previousWidth(), entry.previousHeight() == 0 ? "" : entry.previousHeight(),
                    entry.width(), entry.height(), metrics));
        }
        Path target = destination.toAbsolutePath().normalize();
        Files.createDirectories(target.getParent());
        Files.write(target, lines);
        long compared = sorted.stream().filter(entry -> entry.difference() != null).count();
        log.info("Screenshot discrepancy report: {} captures, {} comparable pairs, {} without pixel comparison; CSV: {}",
                sorted.size(), compared, sorted.size() - compared, target);
        if (compared == 0) {
            log.info("No comparable screenshot pairs; maximum RGB discrepancy is unavailable");
        } else {
            int maximum = sorted.getFirst().difference().maxRgbDifference();
            log.info("Maximum screenshot RGB discrepancy: {} out of 255", maximum);
            sorted.stream().filter(entry -> entry.difference() != null && entry.difference().maxRgbDifference() == maximum)
                    .forEach(entry -> log.info("Maximum-discrepancy PNG: {}", entry.destination()));
        }
    }

    static void record(Path destination, BufferedImage candidate, BufferedImage previous, Difference difference,
                       int tolerance, boolean force, boolean updated) {
        ScreenshotComparisonReport report = CURRENT.get();
        if (report != null) {
            String status = force ? "FORCED" : previous == null ? "CREATED"
                    : difference == null ? "DIMENSIONS_CHANGED" : updated ? "UPDATED" : "RETAINED";
            synchronized (report) {
                report.entries.add(new Entry(destination.toAbsolutePath().normalize(), status, tolerance, force,
                        previous == null ? 0 : previous.getWidth(), previous == null ? 0 : previous.getHeight(),
                        candidate.getWidth(), candidate.getHeight(), difference));
            }
        }
    }

    private static String quote(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    record Difference(int maxRgbDifference, long changedPixels, long alphaChangedPixels,
                      long pixelsAbove8, long pixelsAbove16, long pixelsAbove32) {
    }

    private record Entry(Path destination, String status, int tolerance, boolean forced,
                         int previousWidth, int previousHeight, int width, int height, Difference difference) {
    }
}
