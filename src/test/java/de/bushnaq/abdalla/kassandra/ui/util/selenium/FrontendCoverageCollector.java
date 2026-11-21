/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.ui.util.selenium;

import org.openqa.selenium.WindowType;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Collects coverage counters saved by the instrumented frontend modules.
 * The browser hook saves counters across navigation; this collector flushes open
 * windows and writes a snapshot for the frontend coverage report.
 */
public final class FrontendCoverageCollector {
    private FrontendCoverageCollector() {
    }

    /**
     * Flushes open windows and saves the application's accumulated counters.
     *
     * @param driver Chrome session used by the test
     * @param origin application origin containing the coverage data
     * @throws UncheckedIOException if the coverage snapshot cannot be written
     */
    public static void collect(ChromeDriver driver, String origin) {
        if (!Boolean.getBoolean("frontend.coverage"))
            return;

        // Flush each window before reading shared storage, including detached chart windows.
        String applicationWindow = null;
        for (String handle : driver.getWindowHandles()) {
            driver.switchTo().window(handle);
            String windowOrigin = (String) driver.executeScript("return window.location.origin;");
            if (origin.equals(windowOrigin))
                applicationWindow = handle;
            driver.executeScript("""
                    if (window.__kassandraFrontendCoverage)
                        window.__kassandraFrontendCoverage.flush();
                    """);
        }
        // localStorage is origin-scoped, so return to the application if its window has navigated away.
        if (applicationWindow == null) {
            driver.switchTo().newWindow(WindowType.TAB);
            driver.get(origin + "/");
            if (!origin.equals(driver.executeScript("return window.location.origin;")))
                throw new IllegalStateException("Cannot access frontend coverage for " + origin);
        } else {
            driver.switchTo().window(applicationWindow);
        }
        String counters = (String) driver.executeScript("return localStorage.getItem('__kassandra_frontend_coverage__');");
        if (counters == null)
            return;
        try {
            Path directory = Path.of("target", "frontend-coverage");
            Files.createDirectories(directory);
            // Separate files let the report merge results from multiple browser sessions.
            Files.writeString(directory.resolve("browser-" + UUID.randomUUID() + ".json"), counters);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save frontend coverage", e);
        }
    }
}
