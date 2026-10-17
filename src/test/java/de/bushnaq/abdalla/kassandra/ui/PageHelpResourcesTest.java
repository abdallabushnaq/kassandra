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

package de.bushnaq.abdalla.kassandra.ui;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks packaged help resources without starting the application or a browser.
 */
@Tag("UnitTest")
class PageHelpResourcesTest {
    private String readBundledHelp(String path) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing bundled help: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void testBundledDialogHelpResources() throws IOException {
        Pattern images = Pattern.compile("!\\[[^]]+]\\((images/help/light/[^)]+)\\)");
        for (String dialog : List.of("availability", "dependency", "feature", "image-prompt", "location",
                "off-day", "product", "sprint", "task", "undo-history-confirmation", "user",
                "user-group", "user-work-week", "version", "worklog", "work-week")) {
            String markdown = readBundledHelp("help/" + dialog + "-dialog.md");
            assertTrue(markdown.contains("## "), dialog);
            assertTrue(markdown.contains("**Expand all**"), dialog);
            assertTrue(markdown.contains("**Collapse all**"), dialog);
            assertFalse(markdown.contains("{{include:main-menu}}"), dialog);
            Matcher image = images.matcher(markdown);
            int imageCount = 0;
            while (image.find()) {
                imageCount++;
                for (String theme : List.of("light", "dark")) {
                    String path = image.group(1).replace("/light/", "/" + theme + "/");
                    assertNotNull(getClass().getClassLoader().getResource("META-INF/resources/ui/" + path),
                            dialog + ": missing bundled image " + path);
                }
            }
            assertTrue(imageCount > 0, dialog);
        }
    }

    @Test
    void testBundledPageHelpResources() throws IOException {
        Pattern includes = Pattern.compile("\\{\\{include(?:-admin)?:([a-z0-9-]+)}}");
        Pattern images   = Pattern.compile("!\\[[^]]*]\\((images/help/light/[^)]+)\\)");
        for (String page : List.of("about", "active-sprints", "audit-log", "availability-list", "backlog",
                "feature-list", "kassandra", "location-list", "off-day-list", "oidc-provider-management",
                "product-list", "quality-board", "server-settings", "sprint-list", "user-group-list",
                "user-list", "user-profile", "user-work-week-list", "version-list", "work-week-list")) {
            String markdown = readBundledHelp("help/" + page + ".md");
            assertTrue(markdown.contains("## "), page);
            assertTrue(markdown.contains("{{include:main-menu}}"), page);
            assertTrue(markdown.contains("{{include-admin:main-menu-admin}}"), page);
            StringBuilder expanded = new StringBuilder(markdown);
            Matcher       include  = includes.matcher(markdown);
            while (include.find()) {
                expanded.append(readBundledHelp("help/shared/" + include.group(1) + ".md"));
            }
            Matcher image      = images.matcher(expanded);
            int     imageCount = 0;
            while (image.find()) {
                imageCount++;
                for (String audience : List.of("admin", "user")) {
                    for (String theme : List.of("light", "dark")) {
                        String path = image.group(1).replace("{{audience}}", audience)
                                .replace("/light/", "/" + theme + "/");
                        assertNotNull(getClass().getClassLoader().getResource("META-INF/resources/ui/" + path),
                                page + ": missing bundled image " + path);
                    }
                }
            }
            assertTrue(imageCount > 0, page);
        }
    }
}
