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

import de.bushnaq.abdalla.kassandra.dto.OffDayType;
import org.junit.jupiter.api.*;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Tag("UnitTest")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RandomGenerationDeterminismTest extends AbstractGanttTestUtil {
    private static final Path TRACE_DIRECTORY = Path.of("test-results", "random-generation");

    @Test
    void generatedNameIncludesDataGenerationParameters() {
        RandomCase randomCase    = randomCase();
        String     generatedName = randomCase.generateName();
        String     expectedName  = "P{01-01}_V{01-01}_F{01-01}_S{02-02}_St{01-01}_T{01-01}_TD{01-05}_U{04-04}_UpS{02-04}_%100_S{2026-02-01}_N{2026-03-23T08-00}_s07.snapshot";
        assertEquals(expectedName, generatedName);
        assertEquals(generatedName, randomCase(1, 1, 7, OffsetDateTime.parse("2026-03-23T08:00:00.123456789+01:00")).generateName());
        assertEquals(generatedName, randomCase(1, 1, 7, OffsetDateTime.parse("2026-03-23T08:00:59.123456789+01:00")).generateName());
        assertTrue(RandomCase.builder().sickSprintProbability(0.5f).build().generateName().contains("_%050_"));
        assertEquals(generatedName.length(), randomCase(10, 12, 42, OffsetDateTime.parse("2026-03-23T08:00:00+01:00")).generateName().length());
    }

    @Test
    void generatedNameParametersAreValidatedForFixedWidth() {
        assertThrows(IllegalArgumentException.class, () -> new MinMaxNumber(0, 100));
        assertThrows(IllegalArgumentException.class, () -> RandomCase.builder().seed(100));
        assertThrows(IllegalArgumentException.class, () -> RandomCase.builder().seed(-1));
        assertThrows(IllegalArgumentException.class, () -> RandomCase.builder().startDate(LocalDate.of(10000, 1, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> RandomCase.builder().now(OffsetDateTime.parse("+10000-01-01T00:00:00Z")));
    }

    @RepeatedTest(2)
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    void generatedRandomValuesMatchAcrossRuns(TestInfo testInfo, RepetitionInfo repetitionInfo) throws Exception {
        int  run       = repetitionInfo.getCurrentRepetition();
        Path traceFile = TRACE_DIRECTORY.resolve("generation-run-" + run + ".txt");
        try (LoggingRandom.Recording ignored = LoggingRandom.recordTo(traceFile)) {
            generateProductsIfNeeded(testInfo, randomCase(), true);
        }

        assertTrue(Files.exists(Path.of("test-database-snapshots", randomCase().generateName() + ".log")),
                "The sick-day log should be stored beside its snapshot");
        assertTrue(peg.getUsers().stream().flatMap(user -> user.getOffDays().stream())
                .anyMatch(offDay -> offDay.getType() == OffDayType.SICK), "The generated dataset should include sick leave");
        String trace = Files.readString(traceFile);
        assertFalse(trace.isBlank(), "The generation should produce random values");
        if (run == 2) {
            long firstDifference = Files.mismatch(TRACE_DIRECTORY.resolve("generation-run-1.txt"), traceFile);
            assertEquals(-1L, firstDifference,
                    "Random values generated for the same dataset differ at byte " + firstDifference);
        }
    }

    private RandomCase randomCase(int minProducts, int maxProducts, long seed, OffsetDateTime now) {
        return RandomCase.builder()
                .testCaseIndex(4)
                .now(now)
                .startDate(LocalDate.parse("2026-02-01"))
                .numberOfProducts(minProducts, maxProducts)
                .numberOfVersions(1, 1)
                .numberOfFeatures(1, 1)
                .numberOfSprints(2, 2)
                .taskDurationPerDay(1, 5)
                .numberOfStories(1, 1)
                .numberOfUsers(4, 4)
                .numberOfUsersPerSprint(2, 4)
                .numberOfTasks(1, 1)
                .seed(seed)
                .sickSprintProbability(1f)
                .build();
    }

    private RandomCase randomCase() {
        return randomCase(1, 1, 7, OffsetDateTime.parse("2026-03-23T08:00:00+01:00"));
    }
}
