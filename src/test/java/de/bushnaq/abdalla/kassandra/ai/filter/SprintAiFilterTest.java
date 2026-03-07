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

package de.bushnaq.abdalla.kassandra.ai.filter;

import de.bushnaq.abdalla.kassandra.ai.filter.dto.sprint.SprintFilterDto;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Status;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import tools.jackson.databind.json.JsonMapper;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the JavaScript AI filter generator for Sprint entities.
 *
 * <p>Each test verifies that the LLM-generated JS filter for a natural-language
 * query produces the same result as a hand-written reference JS predicate applied
 * to the same sprint list. Both the LLM filter and the reference filter operate
 * on {@link SprintFilterDto} objects, where:
 * <ul>
 *   <li>{@code status} is a plain {@code String} (e.g. {@code "STARTED"}) — no
 *       {@code Java.type()} enum lookup needed in the JS.</li>
 *   <li>{@code originalEstimationHours}, {@code workedHours}, {@code remainingHours}
 *       are plain {@code long} values — no {@code .toHours()} call needed in the JS.</li>
 * </ul>
 * The sprint list is the only place that needs to change when test data is updated;
 * no individual test hard-codes expected indices or counts.</p>
 */
@Tag("AiUnitTest")
@SpringBootTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class SprintAiFilterTest extends AbstractAiFilterTest<SprintFilterDto> {

    public SprintAiFilterTest(JsonMapper mapper, AiFilterService aiFilterService) {
        super(mapper, aiFilterService, LocalDate.of(2025, 8, 10));
    }

    private Sprint createSprint(Long id, String name, Status status, Long featureId, Long userId,
                                LocalDateTime start, LocalDateTime end, LocalDateTime releaseDate,
                                Duration originalEstimation, Duration worked, Duration remaining,
                                OffsetDateTime created, OffsetDateTime updated) {
        Sprint sprint = new Sprint();
        sprint.setId(id);
        sprint.setName(name);
        sprint.setStatus(status);
        sprint.setFeatureId(featureId);
        sprint.setUserId(userId);
        sprint.setStart(start);
        sprint.setEnd(end);
        sprint.setReleaseDate(releaseDate);
        sprint.setOriginalEstimation(originalEstimation);
        sprint.setWorked(worked);
        sprint.setRemaining(remaining);
        sprint.setCreated(created);
        sprint.setUpdated(updated);
        return sprint;
    }

    @BeforeEach
    void setUp() {
        List<Sprint> raw = new ArrayList<>();

        // id=1  CREATED  80h est  0h worked  80h remaining  starts 2024-01-15  ends 2024-01-29
        raw.add(createSprint(1L, "Sprint 1.0.0-Alpha", Status.CREATED, 1L, 1L,
                LocalDateTime.of(2024, 1, 15, 9, 0), LocalDateTime.of(2024, 1, 29, 17, 0), LocalDateTime.of(2024, 1, 29, 17, 0),
                Duration.ofHours(80), Duration.ofHours(0), Duration.ofHours(80),
                OffsetDateTime.of(2023, 12, 20, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 1, 10, 14, 30, 0, 0, ZoneOffset.UTC)));

        // id=2  STARTED  120h est  60h worked  60h remaining  starts 2024-02-01  ends 2024-02-14
        raw.add(createSprint(2L, "Sprint 1.2.3-Beta", Status.STARTED, 1L, 2L,
                LocalDateTime.of(2024, 2, 1, 9, 0), LocalDateTime.of(2024, 2, 14, 17, 0), LocalDateTime.of(2024, 2, 14, 17, 0),
                Duration.ofHours(120), Duration.ofHours(60), Duration.ofHours(60),
                OffsetDateTime.of(2024, 1, 15, 11, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 2, 5, 16, 45, 0, 0, ZoneOffset.UTC)));

        // id=3  CLOSED   100h est  100h worked  0h remaining  starts 2024-03-01  ends 2024-03-15
        raw.add(createSprint(3L, "Sprint 2.0.0-RC1", Status.CLOSED, 2L, 1L,
                LocalDateTime.of(2024, 3, 1, 9, 0), LocalDateTime.of(2024, 3, 15, 17, 0), LocalDateTime.of(2024, 3, 15, 17, 0),
                Duration.ofHours(100), Duration.ofHours(100), Duration.ofHours(0),
                OffsetDateTime.of(2024, 2, 20, 8, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 3, 16, 13, 20, 0, 0, ZoneOffset.UTC)));

        // id=4  STARTED  160h est  80h worked  80h remaining  starts 2024-04-01  ends 2024-04-30
        raw.add(createSprint(4L, "Authentication Sprint", Status.STARTED, 3L, 3L,
                LocalDateTime.of(2024, 4, 1, 9, 0), LocalDateTime.of(2024, 4, 30, 17, 0), LocalDateTime.of(2024, 4, 30, 17, 0),
                Duration.ofHours(160), Duration.ofHours(80), Duration.ofHours(80),
                OffsetDateTime.of(2024, 3, 25, 15, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 4, 15, 12, 10, 0, 0, ZoneOffset.UTC)));

        // id=5  CREATED  140h est  0h worked  140h remaining  starts 2024-05-01  ends 2024-05-21
        raw.add(createSprint(5L, "Payment Integration Sprint", Status.CREATED, 4L, 2L,
                LocalDateTime.of(2024, 5, 1, 9, 0), LocalDateTime.of(2024, 5, 21, 17, 0), LocalDateTime.of(2024, 5, 21, 17, 0),
                Duration.ofHours(140), Duration.ofHours(0), Duration.ofHours(140),
                OffsetDateTime.of(2024, 4, 20, 14, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 5, 1, 11, 40, 0, 0, ZoneOffset.UTC)));

        // id=6  STARTED  180h est  90h worked  90h remaining  starts 2024-06-03  ends 2024-06-24
        raw.add(createSprint(6L, "Dashboard Development", Status.STARTED, 5L, 4L,
                LocalDateTime.of(2024, 6, 3, 9, 0), LocalDateTime.of(2024, 6, 24, 17, 0), LocalDateTime.of(2024, 6, 24, 17, 0),
                Duration.ofHours(180), Duration.ofHours(90), Duration.ofHours(90),
                OffsetDateTime.of(2024, 5, 28, 9, 20, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 6, 10, 16, 15, 0, 0, ZoneOffset.UTC)));

        // id=7  CLOSED   200h est  200h worked  0h remaining  starts 2024-07-01  ends 2024-07-28
        raw.add(createSprint(7L, "Mobile App Sprint", Status.CLOSED, 6L, 1L,
                LocalDateTime.of(2024, 7, 1, 9, 0), LocalDateTime.of(2024, 7, 28, 17, 0), LocalDateTime.of(2024, 7, 28, 17, 0),
                Duration.ofHours(200), Duration.ofHours(200), Duration.ofHours(0),
                OffsetDateTime.of(2024, 6, 25, 12, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 7, 29, 15, 50, 0, 0, ZoneOffset.UTC)));

        // id=8  STARTED  150h est  50h worked  100h remaining  starts 2024-08-05  ends 2024-08-26
        raw.add(createSprint(8L, "Security Enhancement", Status.STARTED, 7L, 3L,
                LocalDateTime.of(2024, 8, 5, 9, 0), LocalDateTime.of(2024, 8, 26, 17, 0), LocalDateTime.of(2024, 8, 26, 17, 0),
                Duration.ofHours(150), Duration.ofHours(50), Duration.ofHours(100),
                OffsetDateTime.of(2024, 7, 30, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 8, 15, 17, 30, 0, 0, ZoneOffset.UTC)));

        // id=9  CREATED  80h est  0h worked  80h remaining  starts 2024-09-02  ends 2024-09-16
        raw.add(createSprint(9L, "API Documentation Sprint", Status.CREATED, 8L, 4L,
                LocalDateTime.of(2024, 9, 2, 9, 0), LocalDateTime.of(2024, 9, 16, 17, 0), LocalDateTime.of(2024, 9, 16, 17, 0),
                Duration.ofHours(80), Duration.ofHours(0), Duration.ofHours(80),
                OffsetDateTime.of(2024, 8, 28, 13, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 9, 5, 9, 20, 0, 0, ZoneOffset.UTC)));

        // id=10 CLOSED   120h est  120h worked  0h remaining  starts 2024-10-01  ends 2024-10-21
        raw.add(createSprint(10L, "Performance Optimization", Status.CLOSED, 9L, 2L,
                LocalDateTime.of(2024, 10, 1, 9, 0), LocalDateTime.of(2024, 10, 21, 17, 0), LocalDateTime.of(2024, 10, 21, 17, 0),
                Duration.ofHours(120), Duration.ofHours(120), Duration.ofHours(0),
                OffsetDateTime.of(2024, 9, 25, 10, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 10, 22, 14, 45, 0, 0, ZoneOffset.UTC)));

        // id=11 CREATED  160h est  0h worked  160h remaining  starts 2025-01-06  ends 2025-01-27
        raw.add(createSprint(11L, "Sprint 3.0.0-SNAPSHOT", Status.CREATED, 10L, 1L,
                LocalDateTime.of(2025, 1, 6, 9, 0), LocalDateTime.of(2025, 1, 27, 17, 0), LocalDateTime.of(2025, 1, 27, 17, 0),
                Duration.ofHours(160), Duration.ofHours(0), Duration.ofHours(160),
                OffsetDateTime.of(2024, 12, 28, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 10, 16, 30, 0, 0, ZoneOffset.UTC)));

        // id=12 STARTED  60h est  30h worked  30h remaining  starts 2025-02-03  ends 2025-02-17
        raw.add(createSprint(12L, "Bug Fix Sprint", Status.STARTED, 11L, 5L,
                LocalDateTime.of(2025, 2, 3, 9, 0), LocalDateTime.of(2025, 2, 17, 17, 0), LocalDateTime.of(2025, 2, 17, 17, 0),
                Duration.ofHours(60), Duration.ofHours(30), Duration.ofHours(30),
                OffsetDateTime.of(2025, 1, 28, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 2, 10, 12, 20, 0, 0, ZoneOffset.UTC)));

        testProducts = new ArrayList<>();
        for (Sprint s : raw) {
            testProducts.add(SprintFilterDto.from(s));
        }
    }

    // -------------------------------------------------------------------------
    // Tests — each has an unambiguous natural-language query and a hand-written
    // reference JS predicate that is the ground truth for that query.
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("alpha")
    void testAlpha() throws Exception {
        assertSearchMatchesReference(
                "alpha",
                "Sprint",
                "return entity.getName().toLowerCase().includes('alpha');"
        );
    }

    // --- name filters ---------------------------------------------------------

    @Test
    @DisplayName("beta")
    void testBeta() throws Exception {
        assertSearchMatchesReference(
                "beta",
                "Sprint",
                "return entity.getName().toLowerCase().includes('beta');"
        );
    }

    @Test
    @DisplayName("empty search query")
    void testEmptySearchQuery() throws Exception {
        assertSearchMatchesReference(
                "",
                "Sprint",
                "return true;"
        );
    }

    @Test
    @DisplayName("end date in January 2024")
    void testEndDateInJanuary2024() throws Exception {
        assertSearchMatchesReference(
                "end date in January 2024",
                "Sprint",
                "return entity.getEnd() !== null && entity.getEnd().getYear() === 2024 && entity.getEnd().getMonthValue() === 1;"
        );
    }

    @Test
    @DisplayName("integration")
    void testIntegration() throws Exception {
        assertSearchMatchesReference(
                "integration",
                "Sprint",
                "return entity.getName().toLowerCase().includes('integration');"
        );
    }

    @Test
    @DisplayName("name contains development")
    void testNameContainsDevelopment() throws Exception {
        assertSearchMatchesReference(
                "name contains development",
                "Sprint",
                "return entity.getName().toLowerCase().includes('development');"
        );
    }

    @Test
    @DisplayName("name contains Payment")
    void testNameContainsPayment() throws Exception {
        assertSearchMatchesReference(
                "name contains Payment",
                "Sprint",
                "return entity.getName().toLowerCase().includes('payment');"
        );
    }

    @Test
    @DisplayName("originalEstimation over 180 hours")
    void testOriginalEstimationOver180Hours() throws Exception {
        assertSearchMatchesReference(
                "originalEstimation over 180 hours",
                "Sprint",
                "return entity.getOriginalEstimationHours() > 180;"
        );
    }

    // --- status filters -------------------------------------------------------

    @Test
    @DisplayName("rc")
    void testRc() throws Exception {
        assertSearchMatchesReference(
                "rc",
                "Sprint",
                "return entity.getName().toLowerCase().includes('rc');"
        );
    }

    @Test
    @DisplayName("snapshot")
    void testSnapshot() throws Exception {
        assertSearchMatchesReference(
                "snapshot",
                "Sprint",
                "return entity.getName().toLowerCase().includes('snapshot');"
        );
    }

    @Test
    @DisplayName("sprints between 80 and 150 hours original estimation")
    void testSprintsBetween80And150HoursEstimation() throws Exception {
        assertSearchMatchesReference(
                "sprints between 80 and 150 hours original estimation",
                "Sprint",
                "return entity.getOriginalEstimationHours() >= 80 && entity.getOriginalEstimationHours() <= 150;"
        );
    }

    // --- effort filters -------------------------------------------------------

    @Test
    @DisplayName("sprints created in 2025")
    void testSprintsCreatedIn2025() throws Exception {
        assertSearchMatchesReference(
                "sprints created in 2025",
                "Sprint",
                "return entity.getCreated().getYear() === 2025;"
        );
    }

    @Test
    @DisplayName("sprints ending before March 1 2024")
    void testSprintsEndingBeforeMarch1_2024() throws Exception {
        // Explicit day removes all ambiguity about "before March"
        assertSearchMatchesReference(
                "sprints ending before March 1 2024",
                "Sprint",
                """
                        const boundary = Java.type('java.time.LocalDateTime').of(2024, 3, 1, 0, 0, 0);
                        return entity.getEnd() !== null && entity.getEnd().isBefore(boundary);
                        """
        );
    }

    @Test
    @DisplayName("sprints over 100 hours original estimation")
    void testSprintsOver100HoursEstimation() throws Exception {
        assertSearchMatchesReference(
                "sprints over 100 hours original estimation",
                "Sprint",
                "return entity.getOriginalEstimationHours() > 100;"
        );
    }

    @Test
    @DisplayName("sprints starting after February 28 2024")
    void testSprintsStartingAfterFebruary28_2024() throws Exception {
        // Explicit day removes all ambiguity about "after February"
        assertSearchMatchesReference(
                "sprints starting after February 28 2024",
                "Sprint",
                """
                        const boundary = Java.type('java.time.LocalDateTime').of(2024, 2, 28, 23, 59, 59);
                        return entity.getStart() !== null && entity.getStart().isAfter(boundary);
                        """
        );
    }

    @Test
    @DisplayName("sprints starting in 2025")
    void testSprintsStartingIn2025() throws Exception {
        assertSearchMatchesReference(
                "sprints starting in 2025",
                "Sprint",
                "return entity.getStart() !== null && entity.getStart().getYear() === 2025;"
        );
    }

    @Test
    @DisplayName("sprints updated in 2025")
    void testSprintsUpdatedIn2025() throws Exception {
        assertSearchMatchesReference(
                "sprints updated in 2025",
                "Sprint",
                "return entity.getUpdated().getYear() === 2025;"
        );

    }

    // --- start / end date filters ---------------------------------------------

    @Test
    @DisplayName("sprints with no remaining work")
    void testSprintsWithNoRemainingWork() throws Exception {
        assertSearchMatchesReference(
                "sprints with no remaining work",
                "Sprint",
                "return entity.getRemainingHours() === 0;"
        );
    }

    @Test
    @DisplayName("sprints with remaining work")
    void testSprintsWithRemainingWork() throws Exception {
        assertSearchMatchesReference(
                "sprints with remaining work",
                "Sprint",
                "return entity.getRemainingHours() > 0;"
        );
    }

    @Test
    @DisplayName("status is CLOSED")
    void testStatusIsClosed() throws Exception {
        assertSearchMatchesReference(
                "status is CLOSED",
                "Sprint",
                "return entity.getStatus() === 'CLOSED';"
        );
    }

    @Test
    @DisplayName("status is CREATED")
    void testStatusIsCreated() throws Exception {
        assertSearchMatchesReference(
                "status is CREATED",
                "Sprint",
                "return entity.getStatus() === 'CREATED';"
        );
    }

    // --- created / updated filters -------------------------------------------

    @Test
    @DisplayName("status is STARTED")
    void testStatusIsStarted() throws Exception {
        assertSearchMatchesReference(
                "status is STARTED",
                "Sprint",
                "return entity.getStatus() === 'STARTED';"
        );
    }

    @Test
    @DisplayName("worked is 0 hours")
    void testWorkedIs0Hours() throws Exception {
        assertSearchMatchesReference(
                "worked is 0 hours",
                "Sprint",
                "return entity.getWorkedHours() === 0;"
        );
    }
}
