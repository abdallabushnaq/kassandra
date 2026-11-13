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

package de.bushnaq.abdalla.kassandra.util;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag("UnitTest")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RandomGenerationDeterminismTest extends AbstractGanttTestUtil {
    private static final Path TRACE_DIRECTORY = Path.of("test-results", "random-generation");

    private RandomCase randomCase() {
        return RandomCase.builder()
                .testCaseIndex(4)
                .now(OffsetDateTime.parse("2026-03-23T08:00:00+01:00"))
                .startDate(LocalDate.parse("2026-02-01"))
                .numberOfProducts(1, 1)
                .numberOfVersions(1, 1)
                .numberOfFeatures(1, 1)
                .numberOfSprints(2, 2)
                .taskDurationPerDay(1, 5)
                .numberOfStories(1, 1)
                .numberOfUsers(4, 4)
                .numberOfUsersPerSprint(2, 4)
                .numberOfTasks(1, 1)
                .seed(7)
                .build();
    }

    @RepeatedTest(2)
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    void generatedRandomValuesMatchAcrossRuns(TestInfo testInfo, RepetitionInfo repetitionInfo) throws Exception {
        int  run       = repetitionInfo.getCurrentRepetition();
        Path traceFile = TRACE_DIRECTORY.resolve("generation-run-" + run + ".txt");
        try (LoggingRandom.Recording ignored = LoggingRandom.recordTo(traceFile)) {
            generateProductsIfNeeded(testInfo, randomCase(), true);
        }

        String trace = Files.readString(traceFile);
        assertFalse(trace.isBlank(), "The generation should produce random values");
        if (run == 2) {
            long firstDifference = Files.mismatch(TRACE_DIRECTORY.resolve("generation-run-1.txt"), traceFile);
            assertEquals(-1L, firstDifference,
                    "Random values generated for the same dataset differ at byte " + firstDifference);
        }
    }
}
