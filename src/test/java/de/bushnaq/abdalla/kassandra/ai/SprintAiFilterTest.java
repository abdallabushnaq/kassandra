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

package de.bushnaq.abdalla.kassandra.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Status;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;

import java.time.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Integration test for Sprint AI filtering testing real LLM regex pattern generation
 * and filtering capabilities with various search scenarios.
 * <p>
 * This test requires Ollama to be running with a model available (e.g., llama3.2:1b).
 * The test will be skipped if Ollama is not available.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.DisplayName.class)
@Disabled
class SprintAiFilterTest extends AbstractAiFilterTest<Sprint> {

    public SprintAiFilterTest(ObjectMapper mapper, AiFilterService aiFilterService) {
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
        // Create test data
        setupTestSprints();
    }

    private void setupTestSprints() {
        testProducts = new ArrayList<>();

        // Sprints with different patterns, statuses, and time estimates for comprehensive testing
        testProducts.add(createSprint(1L, "Sprint 1.0.0-Alpha", Status.CREATED, 1L, 1L,
                LocalDateTime.of(2024, 1, 15, 9, 0),
                LocalDateTime.of(2024, 1, 29, 17, 0),
                LocalDateTime.of(2024, 1, 29, 17, 0),
                Duration.ofHours(80), Duration.ofHours(0), Duration.ofHours(80),
                OffsetDateTime.of(2023, 12, 20, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 1, 10, 14, 30, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createSprint(2L, "Sprint 1.2.3-Beta", Status.STARTED, 1L, 2L,
                LocalDateTime.of(2024, 2, 1, 9, 0),
                LocalDateTime.of(2024, 2, 14, 17, 0),
                LocalDateTime.of(2024, 2, 14, 17, 0),
                Duration.ofHours(120), Duration.ofHours(60), Duration.ofHours(60),
                OffsetDateTime.of(2024, 1, 15, 11, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 2, 5, 16, 45, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createSprint(3L, "Sprint 2.0.0-RC1", Status.CLOSED, 2L, 1L,
                LocalDateTime.of(2024, 3, 1, 9, 0),
                LocalDateTime.of(2024, 3, 15, 17, 0),
                LocalDateTime.of(2024, 3, 15, 17, 0),
                Duration.ofHours(100), Duration.ofHours(100), Duration.ofHours(0),
                OffsetDateTime.of(2024, 2, 20, 8, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 3, 16, 13, 20, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createSprint(4L, "Authentication Sprint", Status.STARTED, 3L, 3L,
                LocalDateTime.of(2024, 4, 1, 9, 0),
                LocalDateTime.of(2024, 4, 30, 17, 0),
                LocalDateTime.of(2024, 4, 30, 17, 0),
                Duration.ofHours(160), Duration.ofHours(80), Duration.ofHours(80),
                OffsetDateTime.of(2024, 3, 25, 15, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 4, 15, 12, 10, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createSprint(5L, "Payment Integration Sprint", Status.CREATED, 4L, 2L,
                LocalDateTime.of(2024, 5, 1, 9, 0),
                LocalDateTime.of(2024, 5, 21, 17, 0),
                LocalDateTime.of(2024, 5, 21, 17, 0),
                Duration.ofHours(140), Duration.ofHours(0), Duration.ofHours(140),
                OffsetDateTime.of(2024, 4, 20, 14, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 5, 1, 11, 40, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createSprint(6L, "Dashboard Development", Status.STARTED, 5L, 4L,
                LocalDateTime.of(2024, 6, 3, 9, 0),
                LocalDateTime.of(2024, 6, 24, 17, 0),
                LocalDateTime.of(2024, 6, 24, 17, 0),
                Duration.ofHours(180), Duration.ofHours(90), Duration.ofHours(90),
                OffsetDateTime.of(2024, 5, 28, 9, 20, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 6, 10, 16, 15, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createSprint(7L, "Mobile App Sprint", Status.CLOSED, 6L, 1L,
                LocalDateTime.of(2024, 7, 1, 9, 0),
                LocalDateTime.of(2024, 7, 28, 17, 0),
                LocalDateTime.of(2024, 7, 28, 17, 0),
                Duration.ofHours(200), Duration.ofHours(200), Duration.ofHours(0),
                OffsetDateTime.of(2024, 6, 25, 12, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 7, 29, 15, 50, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createSprint(8L, "Security Enhancement", Status.STARTED, 7L, 3L,
                LocalDateTime.of(2024, 8, 5, 9, 0),
                LocalDateTime.of(2024, 8, 26, 17, 0),
                LocalDateTime.of(2024, 8, 26, 17, 0),
                Duration.ofHours(150), Duration.ofHours(50), Duration.ofHours(100),
                OffsetDateTime.of(2024, 7, 30, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 8, 15, 17, 30, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createSprint(9L, "API Documentation Sprint", Status.CREATED, 8L, 4L,
                LocalDateTime.of(2024, 9, 2, 9, 0),
                LocalDateTime.of(2024, 9, 16, 17, 0),
                LocalDateTime.of(2024, 9, 16, 17, 0),
                Duration.ofHours(80), Duration.ofHours(0), Duration.ofHours(80),
                OffsetDateTime.of(2024, 8, 28, 13, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 9, 5, 9, 20, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createSprint(10L, "Performance Optimization", Status.CLOSED, 9L, 2L,
                LocalDateTime.of(2024, 10, 1, 9, 0),
                LocalDateTime.of(2024, 10, 21, 17, 0),
                LocalDateTime.of(2024, 10, 21, 17, 0),
                Duration.ofHours(120), Duration.ofHours(120), Duration.ofHours(0),
                OffsetDateTime.of(2024, 9, 25, 10, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 10, 22, 14, 45, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createSprint(11L, "Sprint 3.0.0-SNAPSHOT", Status.CREATED, 10L, 1L,
                LocalDateTime.of(2025, 1, 6, 9, 0),
                LocalDateTime.of(2025, 1, 27, 17, 0),
                LocalDateTime.of(2025, 1, 27, 17, 0),
                Duration.ofHours(160), Duration.ofHours(0), Duration.ofHours(160),
                OffsetDateTime.of(2024, 12, 28, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 10, 16, 30, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createSprint(12L, "Bug Fix Sprint", Status.STARTED, 11L, 5L,
                LocalDateTime.of(2025, 2, 3, 9, 0),
                LocalDateTime.of(2025, 2, 17, 17, 0),
                LocalDateTime.of(2025, 2, 17, 17, 0),
                Duration.ofHours(60), Duration.ofHours(30), Duration.ofHours(30),
                OffsetDateTime.of(2025, 1, 28, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 2, 10, 12, 20, 0, 0, ZoneOffset.UTC)));
    }

    @Test
    @DisplayName("active sprints")
    void testActiveSprints() throws Exception {
        List<Sprint> results = performSearch("active sprints", "Sprint");
        List<Sprint> expected = Arrays.asList(
                testProducts.get(1), // Sprint 1.2.3-Beta (STARTED)
                testProducts.get(3), // Authentication Sprint (STARTED)
                testProducts.get(5), // Dashboard Development (STARTED)
                testProducts.get(7), // Security Enhancement (STARTED)
                testProducts.get(11) // Bug Fix Sprint (STARTED)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("alpha")
    void testAlpha() throws Exception {
        List<Sprint> results  = performSearch("alpha", "Sprint");
        List<Sprint> expected = Collections.singletonList(testProducts.get(0)); // Sprint 1.0.0-Alpha

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("beta")
    void testBeta() throws Exception {
        List<Sprint> results  = performSearch("beta", "Sprint");
        List<Sprint> expected = Collections.singletonList(testProducts.get(1)); // Sprint 1.2.3-Beta

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("completed sprints")
    void testCompletedSprints() throws Exception {
        List<Sprint> results = performSearch("completed sprints", "Sprint");
        List<Sprint> expected = Arrays.asList(
                testProducts.get(2), // Sprint 2.0.0-RC1 (CLOSED)
                testProducts.get(6), // Mobile App Sprint (CLOSED)
                testProducts.get(9)  // Performance Optimization (CLOSED)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("created in 2025")
    void testCreatedIn2025() throws Exception {
        List<Sprint> results  = performSearch("created in 2025", "Sprint");
        List<Sprint> expected = Collections.singletonList(testProducts.get(11)); // Bug Fix Sprint

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("empty search query")
    void testEmptySearchQuery() throws Exception {
        List<Sprint> results  = performSearch("", "Sprint");
        List<Sprint> expected = new ArrayList<>(testProducts); // All sprints should match empty query

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("end date in January 2024")
    void testEndDateInJanuary2024() throws Exception {
        List<Sprint> results  = performSearch("end date in January 2024", "Sprint");
        List<Sprint> expected = Collections.singletonList(testProducts.get(0)); // Sprint 1.0.0-Alpha

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("integration")
    void testIntegration() throws Exception {
        List<Sprint> results  = performSearch("integration", "Sprint");
        List<Sprint> expected = Collections.singletonList(testProducts.get(4)); // Payment Integration Sprint

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("name contains development")
    void testNameContainsDevelopment() throws Exception {
        List<Sprint> results  = performSearch("name contains development", "Sprint");
        List<Sprint> expected = Collections.singletonList(testProducts.get(5)); // Dashboard Development

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    // === COLUMN-SPECIFIC TESTS (one per column) ===
    @Test
    @DisplayName("name contains Payment")
    void testNameContainsPayment() throws Exception {
        List<Sprint> results  = performSearch("name contains Payment", "Sprint");
        List<Sprint> expected = Collections.singletonList(testProducts.get(4)); // Payment Integration Sprint

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("originalEstimation über 180 Stunden")
    void testOriginalEstimationOver180Hours() throws Exception {
        List<Sprint> results  = performSearch("originalEstimation über 180 Stunden", "Sprint");
        List<Sprint> expected = Collections.singletonList(testProducts.get(6)); // Mobile App Sprint (200 hours)

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("purple elephant dancing")
    void testPurpleElephantDancing() throws Exception {
        List<Sprint> results  = performSearch("purple elephant dancing", "Sprint");
        List<Sprint> expected = Collections.emptyList(); // Should return empty results for nonsensical queries

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("rc")
    void testRc() throws Exception {
        List<Sprint> results  = performSearch("rc", "Sprint");
        List<Sprint> expected = Collections.singletonList(testProducts.get(2)); // Sprint 2.0.0-RC1

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("remaining is 0 hours")
    void testRemainingIs0Hours() throws Exception {
        List<Sprint> results  = performSearch("remaining is 0 hours", "Sprint");
        List<Sprint> expected = Arrays.asList(testProducts.get(2), testProducts.get(6), testProducts.get(9)); // Completed sprints

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("snapshot")
    void testSnapshot() throws Exception {
        List<Sprint> results  = performSearch("snapshot", "Sprint");
        List<Sprint> expected = Collections.singletonList(testProducts.get(10)); // Sprint 3.0.0-SNAPSHOT

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("sprints between 80 and 150 hours")
    void testSprintsBetween80And150Hours() throws Exception {
        List<Sprint> results = performSearch("sprints between 80 and 150 hours", "Sprint");
        List<Sprint> expected = Arrays.asList(
                testProducts.get(0), // Sprint 1.0.0-Alpha (80h)
                testProducts.get(1), // Sprint 1.2.3-Beta (120h)
                testProducts.get(2), // Sprint 2.0.0-RC1 (100h)
                testProducts.get(4), // Payment Integration Sprint (140h)
                testProducts.get(7), // Security Enhancement (150h)
                testProducts.get(8), // API Documentation Sprint (80h)
                testProducts.get(9)  // Performance Optimization (120h)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("sprints created in 2025")
    void testSprintsCreatedIn2025() throws Exception {
        List<Sprint> results  = performSearch("sprints created in 2025", "Sprint");
        List<Sprint> expected = Collections.singletonList(testProducts.get(11)); // Bug Fix Sprint (created 2025-01-28)

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("sprints ending before March 2024")
    void testSprintsEndingBeforeMarch2024() throws Exception {
        List<Sprint> results = performSearch("sprints ending before March 2024", "Sprint");
        List<Sprint> expected = Arrays.asList(
                testProducts.get(0), // Sprint 1.0.0-Alpha (ends 2024-01-29)
                testProducts.get(1)  // Sprint 1.2.3-Beta (ends 2024-02-14)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("sprints over 100 hours estimation")
    void testSprintsOver100HoursEstimation() throws Exception {
        List<Sprint> results = performSearch("sprints over 100 hours estimation", "Sprint");
        List<Sprint> expected = Arrays.asList(
                testProducts.get(1), // Sprint 1.2.3-Beta (120h)
                testProducts.get(3), // Authentication Sprint (160h)
                testProducts.get(4), // Payment Integration Sprint (140h)
                testProducts.get(5), // Dashboard Development (180h)
                testProducts.get(6), // Mobile App Sprint (200h)
                testProducts.get(7), // Security Enhancement (150h)
                testProducts.get(9), // Performance Optimization (120h)
                testProducts.get(10) // Sprint 3.0.0-SNAPSHOT (160h)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("sprints starting after February 2024")
    void testSprintsStartingAfterFebruary2024() throws Exception {
        List<Sprint> results = performSearch("sprints starting after February 2024", "Sprint");
        List<Sprint> expected = Arrays.asList(
                testProducts.get(2), // Sprint 2.0.0-RC1 (starts 2024-03-01)
                testProducts.get(3), // Authentication Sprint (starts 2024-04-01)
                testProducts.get(4), // Payment Integration Sprint (starts 2024-05-01)
                testProducts.get(5), // Dashboard Development (starts 2024-06-03)
                testProducts.get(6), // Mobile App Sprint (starts 2024-07-01)
                testProducts.get(7), // Security Enhancement (starts 2024-08-05)
                testProducts.get(8), // API Documentation Sprint (starts 2024-09-02)
                testProducts.get(9), // Performance Optimization (starts 2024-10-01)
                testProducts.get(10), // Sprint 3.0.0-SNAPSHOT (starts 2025-01-06)
                testProducts.get(11)  // Bug Fix Sprint (starts 2025-02-03)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("sprints updated in 2025")
    void testSprintsUpdatedIn2025() throws Exception {
        List<Sprint> results = performSearch("sprints updated in 2025", "Sprint");
        List<Sprint> expected = Arrays.asList(
                testProducts.get(10), // Sprint 3.0.0-SNAPSHOT (updated 2025-01-10)
                testProducts.get(11)  // Bug Fix Sprint (updated 2025-02-10)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("sprints with remaining work")
    void testSprintsWithRemainingWork() throws Exception {
        List<Sprint> results = performSearch("sprints with remaining work", "Sprint");
        List<Sprint> expected = Arrays.asList(
                testProducts.get(0), // Sprint 1.0.0-Alpha (80h remaining)
                testProducts.get(1), // Sprint 1.2.3-Beta (60h remaining)
                testProducts.get(3), // Authentication Sprint (80h remaining)
                testProducts.get(4), // Payment Integration Sprint (140h remaining)
                testProducts.get(5), // Dashboard Development (90h remaining)
                testProducts.get(7), // Security Enhancement (100h remaining)
                testProducts.get(8), // API Documentation Sprint (80h remaining)
                testProducts.get(10), // Sprint 3.0.0-SNAPSHOT (160h remaining)
                testProducts.get(11)  // Bug Fix Sprint (30h remaining)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("start date in 2025")
    void testStartDateIn2025() throws Exception {
        List<Sprint> results  = performSearch("start date in 2025", "Sprint");
        List<Sprint> expected = Arrays.asList(testProducts.get(10), testProducts.get(11)); // Sprint 3.0.0-SNAPSHOT, Bug Fix Sprint

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("status is CLOSED")
    void testStatusIsClosed() throws Exception {
        List<Sprint> results  = performSearch("status is CLOSED", "Sprint");
        List<Sprint> expected = Arrays.asList(testProducts.get(2), testProducts.get(6), testProducts.get(9)); // Sprint 2.0.0-RC1, Mobile App Sprint, Performance Optimization

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("updated in 2025")
    void testUpdatedIn2025() throws Exception {
        List<Sprint> results  = performSearch("updated in 2025", "Sprint");
        List<Sprint> expected = Arrays.asList(testProducts.get(10), testProducts.get(11)); // Sprint 3.0.0-SNAPSHOT, Bug Fix Sprint

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("worked is 0 hours")
    void testWorkedIs0Hours() throws Exception {
        List<Sprint> results  = performSearch("worked is 0 hours", "Sprint");
        List<Sprint> expected = Arrays.asList(testProducts.get(0), testProducts.get(4), testProducts.get(8), testProducts.get(10)); // Sprints with no work done yet

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    /**
     * add generated ai code to test against one of the tests.
     */
    class ExampleSprintFilter implements Predicate<Sprint> {

        @Override
        public boolean test(Sprint entity) {
            if (entity == null) {
                return false;
            }

            try {
                return entity.getOriginalEstimation() != null && entity.getOriginalEstimation().toHours() > 180;
            } catch (Exception e) {
                // Log the error but don't fail the entire filter
                System.err.println("Error in filter execution: " + e.getMessage());
                return false;
            }
        }

    }
}
