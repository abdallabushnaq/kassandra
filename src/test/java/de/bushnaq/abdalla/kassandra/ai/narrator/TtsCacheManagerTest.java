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
package de.bushnaq.abdalla.kassandra.ai.narrator;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@Tag("AiUnitTest")
class TtsCacheManagerTest {

    @Test
    void prepareChronologicalAllocates001Then002AndReusesMatchingCurrentId() throws Exception {
        Path tmp = Files.createTempDirectory("tts-cache-test");
        try {
            TtsCacheManager mgr = new TtsCacheManager(tmp);

            NarratorAttribute attrs1 = new NarratorAttribute().withExaggeration(0.5f).withCfgWeight(1.0f).withTemperature(0.5f);
            NarratorAttribute attrs2 = new NarratorAttribute().withExaggeration(0.5f).withCfgWeight(1.0f).withTemperature(0.5f);

            String name1 = mgr.buildFileName("Hello", attrs1);
            String name2 = mgr.buildFileName("World", attrs2);

            // Empty dir: first call should allocate 001- and require generation
            TtsCacheManager.ChronoPlan plan1 = mgr.prepareChronological(name1);
            assertFalse(plan1.upToDate(), "First call in empty dir should not be up-to-date");
            assertTrue(plan1.path().getFileName().toString().startsWith("001-"));
            assertTrue(plan1.path().getFileName().toString().endsWith(name1));

            // Simulate we wrote the file (like Narrator would)
            Files.writeString(plan1.path(), "wav");

            // If we re-run the app and a matching file already exists for 001-, we should reuse it
            TtsCacheManager            mgr2   = new TtsCacheManager(tmp);
            TtsCacheManager.ChronoPlan plan1b = mgr2.prepareChronological(name1);
            assertTrue(plan1b.upToDate(), "Existing matching 001- should be reused");
            assertEquals(plan1.path(), plan1b.path());

            // Next unique text should allocate 002-
            TtsCacheManager.ChronoPlan plan2 = mgr2.prepareChronological(name2);
            assertFalse(plan2.upToDate());
            assertTrue(plan2.path().getFileName().toString().startsWith("002-"));
            assertTrue(plan2.path().getFileName().toString().endsWith(name2));
        } finally {
            try (var s = Files.newDirectoryStream(tmp)) {
                for (Path p : s) Files.deleteIfExists(p);
            } catch (Exception ignored) {
            }
            Files.deleteIfExists(tmp);
        }
    }
}
