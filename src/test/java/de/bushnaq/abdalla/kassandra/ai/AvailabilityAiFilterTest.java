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
import de.bushnaq.abdalla.kassandra.dto.Availability;
import de.bushnaq.abdalla.kassandra.dto.User;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reviewed by: Abdalla Bushnaq
 * Integration test for Availability AI filtering testing real LLM regex pattern generation
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
class AvailabilityAiFilterTest extends AbstractAiFilterTest<Availability> {

    public AvailabilityAiFilterTest(ObjectMapper mapper, AiFilterService aiFilterService) {
        super(mapper, aiFilterService, LocalDate.of(2025, 8, 10));
    }

    private Availability createAvailability(Long id, float availability, LocalDate start,
                                            User user, OffsetDateTime created, OffsetDateTime updated) {
        Availability avail = new Availability();
        avail.setId(id);
        avail.setAvailability(availability);
        avail.setStart(start);
        avail.setUser(user);
        avail.setCreated(created);
        avail.setUpdated(updated);
        return avail;
    }

    private User createUser(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        return user;
    }

    @BeforeEach
    void setUp() {
        // Create test data
        setupTestAvailabilities();
    }

    private void setupTestAvailabilities() {
        testProducts = new ArrayList<>();

        // Create test users
        User johnDoe     = createUser(1L, "John Doe");
        User janeSmith   = createUser(2L, "Jane Smith");
        User bobJohnson  = createUser(3L, "Bob Johnson");
        User aliceWilson = createUser(4L, "Alice Wilson");
        User mikeBrown   = createUser(5L, "Mike Brown");

        // Availabilities with different percentage values and dates for comprehensive testing
        testProducts.add(createAvailability(1L, 1.0f, LocalDate.of(2024, 1, 15), johnDoe,
                OffsetDateTime.of(2023, 12, 20, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 1, 10, 14, 30, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createAvailability(2L, 0.8f, LocalDate.of(2024, 2, 1), janeSmith,
                OffsetDateTime.of(2024, 1, 15, 11, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 2, 5, 16, 45, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createAvailability(3L, 0.5f, LocalDate.of(2024, 3, 1), bobJohnson,
                OffsetDateTime.of(2024, 2, 20, 8, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 3, 16, 13, 20, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createAvailability(4L, 0.75f, LocalDate.of(2024, 4, 1), aliceWilson,
                OffsetDateTime.of(2024, 3, 25, 15, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 4, 15, 12, 10, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createAvailability(5L, 0.9f, LocalDate.of(2024, 5, 1), mikeBrown,
                OffsetDateTime.of(2024, 4, 20, 14, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 5, 1, 11, 40, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createAvailability(6L, 0.6f, LocalDate.of(2024, 6, 1), johnDoe,
                OffsetDateTime.of(2024, 5, 28, 9, 20, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 6, 10, 16, 15, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createAvailability(7L, 0.25f, LocalDate.of(2024, 7, 1), janeSmith,
                OffsetDateTime.of(2024, 6, 25, 12, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 7, 29, 15, 50, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createAvailability(8L, 0.85f, LocalDate.of(2024, 8, 1), bobJohnson,
                OffsetDateTime.of(2024, 7, 30, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 8, 15, 17, 30, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createAvailability(9L, 0.0f, LocalDate.of(2024, 9, 1), aliceWilson,
                OffsetDateTime.of(2024, 8, 28, 13, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 9, 5, 9, 20, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createAvailability(10L, 0.95f, LocalDate.of(2024, 10, 1), mikeBrown,
                OffsetDateTime.of(2024, 9, 25, 10, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 10, 22, 14, 45, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createAvailability(11L, 0.7f, LocalDate.of(2025, 1, 1), johnDoe,
                OffsetDateTime.of(2024, 12, 28, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 10, 16, 30, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createAvailability(12L, 0.4f, LocalDate.of(2025, 2, 1), janeSmith,
                OffsetDateTime.of(2025, 1, 28, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 2, 10, 12, 20, 0, 0, ZoneOffset.UTC)));
    }

    @Test
    @DisplayName("50% availability")
    void test50PercentAvailability() throws Exception {
        List<Availability> results  = performSearch("50% availability", "Availability");
        List<Availability> expected = Collections.singletonList(testProducts.get(2)); // 0.5f

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("80% availability")
    void test80PercentAvailability() throws Exception {
        List<Availability> results = performSearch("80% availability", "Availability");
//        List<Availability> results  = applyJavaScriptSearchQuery("return Math.round(entity.getAvailability() * 100) === 80;", now);
        List<Availability> expected = Collections.singletonList(testProducts.get(1)); // 0.8f

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("95% availability")
    void test95PercentAvailability() throws Exception {
        List<Availability> results = performSearch("95% availability", "Availability");

        List<Availability> expected = Collections.singletonList(testProducts.get(9)); // 0.95f

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("availability between 60% included and 80% included")
    void testAvailabilityBetween60IncludedAndAnd80PercentIncluded() throws Exception {
        List<Availability> results = performSearch("availability between 60% included and 80% included", "Availability");
        List<Availability> expected = Arrays.asList(
                testProducts.get(1),  // 0.8f - Jane Smith
                testProducts.get(3),  // 0.75f - Alice Wilson
                testProducts.get(5),  // 0.6f - John Doe
                testProducts.get(10)  // 0.7f - John Doe
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("availability created in 2025")
    void testAvailabilityCreatedIn2025() throws Exception {
        List<Availability> results  = performSearch("availability created in 2025", "Availability");
        List<Availability> expected = Collections.singletonList(testProducts.get(11)); // February 2025 availability

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("availability greater than 50%")
    void testAvailabilityGreaterThan50Percent() throws Exception {
        List<Availability> results = performSearch("availability greater than 50%", "Availability");
        List<Availability> expected = Arrays.asList(
                testProducts.get(0),  // 1.0f
                testProducts.get(1),  // 0.8f
                testProducts.get(3),  // 0.75f
                testProducts.get(4),  // 0.9f
                testProducts.get(5),  // 0.6f
                testProducts.get(7),  // 0.85f
                testProducts.get(9),  // 0.95f
                testProducts.get(10)  // 0.7f
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("availability greater than or equal to 70%")
    void testAvailabilityGreaterThanOrEqual70Percent() throws Exception {
        List<Availability> results = performSearch("availability greater than or equal to 70%", "Availability");
        List<Availability> expected = Arrays.asList(
                testProducts.get(0),  // 1.0f
                testProducts.get(1),  // 0.8f
                testProducts.get(3),  // 0.75f
                testProducts.get(4),  // 0.9f
                testProducts.get(7),  // 0.85f
                testProducts.get(9),  // 0.95f
                testProducts.get(10)  // 0.7f
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    // === COLUMN-SPECIFIC TESTS (one per column) ===
    @Test
    @DisplayName("availability is 0.5")
    void testAvailabilityIs0_5() throws Exception {
        List<Availability> results  = performSearch("availability is 0.5", "Availability");
        List<Availability> expected = Collections.singletonList(testProducts.get(2)); // 50% availability

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("availability less than 90%")
    void testAvailabilityLessThan90Percent() throws Exception {
        List<Availability> results = performSearch("availability less than 90%", "Availability");
        List<Availability> expected = Arrays.asList(
                testProducts.get(1),  // 0.8f
                testProducts.get(2),  // 0.5f
                testProducts.get(3),  // 0.75f
                testProducts.get(5),  // 0.6f
                testProducts.get(6),  // 0.25f
                testProducts.get(7),  // 0.85f
                testProducts.get(8),  // 0.0f
                testProducts.get(10), // 0.7f
                testProducts.get(11)  // 0.4f
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("availability less than or equal to 40%")
    void testAvailabilityLessThanOrEqual40Percent() throws Exception {
        List<Availability> results = performSearch("availability less than or equal to 40%", "Availability");
        List<Availability> expected = Arrays.asList(
                testProducts.get(6),  // 0.25f
                testProducts.get(8),  // 0.0f
                testProducts.get(11)  // 0.4f
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("availability starting after January 2024")
    void testAvailabilityStartingAfterJanuary2024() throws Exception {
        List<Availability> results = performSearch("availability starting after January 2024", "Availability");
        List<Availability> expected = Arrays.asList(
                testProducts.get(1),  // February 2024
                testProducts.get(2),  // March 2024
                testProducts.get(3),  // April 2024
                testProducts.get(4),  // May 2024
                testProducts.get(5),  // June 2024
                testProducts.get(6),  // July 2024
                testProducts.get(7),  // August 2024
                testProducts.get(8),  // September 2024
                testProducts.get(9),  // October 2024
                testProducts.get(10), // January 2025
                testProducts.get(11)  // February 2025
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("availability starting before March 2024")
    void testAvailabilityStartingBeforeMarch2024() throws Exception {
        List<Availability> results = performSearch("availability starting before March 2024", "Availability");
        List<Availability> expected = Arrays.asList(
                testProducts.get(0),  // January 2024
                testProducts.get(1)   // February 2024
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("availability starting in January 2025")
    void testAvailabilityStartingInJanuary2025() throws Exception {
        List<Availability> results  = performSearch("availability starting in January 2025", "Availability");
        List<Availability> expected = Collections.singletonList(testProducts.get(10)); // January 2025

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("availability starting in summer 2024")
    void testAvailabilityStartingSummer2024() throws Exception {
        List<Availability> results = performSearch("availability starting in summer 2024", "Availability");
        List<Availability> expected = Arrays.asList(
                testProducts.get(5),  // June 2024
                testProducts.get(6),  // July 2024
                testProducts.get(7)   // August 2024
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("availability updated in 2025")
    void testAvailabilityUpdatedIn2025() throws Exception {
        List<Availability> results = performSearch("availability updated in 2025", "Availability");
        List<Availability> expected = Arrays.asList(
                testProducts.get(10), // January 2025
                testProducts.get(11)  // February 2025
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("empty search query")
    void testEmptySearchQuery() throws Exception {
        List<Availability> results  = performSearch("", "Availability");
        List<Availability> expected = new ArrayList<>(testProducts); // All availabilities

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("full availability")
    void testFullAvailability() throws Exception {
        List<Availability> results  = performSearch("full availability", "Availability");
        List<Availability> expected = Collections.singletonList(testProducts.get(0)); // 1.0f

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("purple elephant dancing")
    void testPurpleElephantDancing() throws Exception {
        List<Availability> results  = performSearch("purple elephant dancing", "Availability");
        List<Availability> expected = Collections.emptyList(); // Should return empty results for nonsensical queries

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("zero availability")
    void testZeroAvailability() throws Exception {
        List<Availability> results  = performSearch("zero availability", "Availability");
        List<Availability> expected = Collections.singletonList(testProducts.get(8)); // 0.0f

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }
}
