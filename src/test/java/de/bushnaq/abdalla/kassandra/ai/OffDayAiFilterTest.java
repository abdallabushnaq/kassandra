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
import de.bushnaq.abdalla.kassandra.dto.OffDay;
import de.bushnaq.abdalla.kassandra.dto.OffDayType;
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
 * Integration test for OffDay AI filtering testing real LLM regex pattern generation
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
class OffDayAiFilterTest extends AbstractAiFilterTest<OffDay> {

    public OffDayAiFilterTest(ObjectMapper mapper, AiFilterService aiFilterService) {
        super(mapper, aiFilterService, LocalDate.of(2025, 8, 10));
    }

    private OffDay createOffDay(Long id, LocalDate firstDay, LocalDate lastDay, OffDayType type,
                                User user, OffsetDateTime created, OffsetDateTime updated) {
        OffDay offDay = new OffDay();
        offDay.setId(id);
        offDay.setFirstDay(firstDay);
        offDay.setLastDay(lastDay);
        offDay.setType(type);
        offDay.setUser(user);
        offDay.setCreated(created);
        offDay.setUpdated(updated);
        return offDay;
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
        setupTestOffDays();
    }

    private void setupTestOffDays() {
        testProducts = new ArrayList<>();

        // Create test users
        User johnDoe     = createUser(1L, "John Doe");
        User janeSmith   = createUser(2L, "Jane Smith");
        User bobJohnson  = createUser(3L, "Bob Johnson");
        User aliceWilson = createUser(4L, "Alice Wilson");
        User mikeBrown   = createUser(5L, "Mike Brown");

        // OffDays with different types, durations, and dates for comprehensive testing
        testProducts.add(createOffDay(1L, LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 19), OffDayType.VACATION, johnDoe,
                OffsetDateTime.of(2023, 12, 20, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 1, 10, 14, 30, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createOffDay(2L, LocalDate.of(2024, 2, 5), LocalDate.of(2024, 2, 7), OffDayType.SICK, janeSmith,
                OffsetDateTime.of(2024, 1, 15, 11, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 2, 5, 16, 45, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createOffDay(3L, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 1), OffDayType.HOLIDAY, bobJohnson,
                OffsetDateTime.of(2024, 2, 20, 8, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 3, 1, 13, 20, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createOffDay(4L, LocalDate.of(2024, 4, 10), LocalDate.of(2024, 4, 15), OffDayType.TRIP, aliceWilson,
                OffsetDateTime.of(2024, 3, 25, 15, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 4, 15, 12, 10, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createOffDay(5L, LocalDate.of(2024, 5, 20), LocalDate.of(2024, 6, 5), OffDayType.VACATION, mikeBrown,
                OffsetDateTime.of(2024, 4, 20, 14, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 5, 1, 11, 40, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createOffDay(6L, LocalDate.of(2024, 6, 10), LocalDate.of(2024, 6, 12), OffDayType.SICK, johnDoe,
                OffsetDateTime.of(2024, 5, 28, 9, 20, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 6, 10, 16, 15, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createOffDay(7L, LocalDate.of(2024, 7, 4), LocalDate.of(2024, 7, 4), OffDayType.HOLIDAY, janeSmith,
                OffsetDateTime.of(2024, 6, 25, 12, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 7, 4, 15, 50, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createOffDay(8L, LocalDate.of(2024, 8, 15), LocalDate.of(2024, 8, 25), OffDayType.VACATION, bobJohnson,
                OffsetDateTime.of(2024, 7, 30, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 8, 15, 17, 30, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createOffDay(9L, LocalDate.of(2024, 9, 2), LocalDate.of(2024, 9, 6), OffDayType.TRIP, aliceWilson,
                OffsetDateTime.of(2024, 8, 28, 13, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 9, 5, 9, 20, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createOffDay(10L, LocalDate.of(2024, 10, 28), LocalDate.of(2024, 10, 30), OffDayType.SICK, mikeBrown,
                OffsetDateTime.of(2024, 9, 25, 10, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 10, 28, 14, 45, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createOffDay(11L, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 1), OffDayType.HOLIDAY, johnDoe,
                OffsetDateTime.of(2024, 12, 28, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 1, 16, 30, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createOffDay(12L, LocalDate.of(2025, 2, 10), LocalDate.of(2025, 2, 21), OffDayType.VACATION, janeSmith,
                OffsetDateTime.of(2025, 1, 28, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 2, 10, 12, 20, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createOffDay(13L, LocalDate.of(2025, 3, 3), LocalDate.of(2025, 3, 7), OffDayType.SICK, bobJohnson,
                OffsetDateTime.of(2025, 2, 28, 10, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 3, 3, 11, 30, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createOffDay(14L, LocalDate.of(2025, 4, 14), LocalDate.of(2025, 4, 25), OffDayType.TRIP, aliceWilson,
                OffsetDateTime.of(2025, 3, 20, 14, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 4, 14, 9, 45, 0, 0, ZoneOffset.UTC)));
    }

    @Test
    @DisplayName("created in 2025")
    void testCreatedIn2025() throws Exception {
        List<OffDay> results = performSearch("created in 2025", "OffDay");
        List<OffDay> expected = Arrays.asList(
                testProducts.get(11),
                testProducts.get(12),
                testProducts.get(13)); // Off days created in 2025

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("emergency sick")
    void testEmergencySick() throws Exception {
        List<OffDay> results = performSearch("emergency sick", "OffDay");
        List<OffDay> expected = Arrays.asList(
                testProducts.get(1),  // Sick leave - Jane Smith
                testProducts.get(5),  // Sick leave - John Doe
                testProducts.get(9),  // Sick leave - Mike Brown
                testProducts.get(12)  // Sick leave - Bob Johnson
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("empty search query")
    void testEmptySearchQuery() throws Exception {
        List<OffDay> results  = performSearch("", "OffDay");
        List<OffDay> expected = new ArrayList<>(testProducts); // All off days

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("firstDay in 2025")
    void testFirstDayIn2025() throws Exception {
        List<OffDay> results  = performSearch("firstDay in 2025", "OffDay");
        List<OffDay> expected = Arrays.asList(testProducts.get(10), testProducts.get(11), testProducts.get(12), testProducts.get(13)); // Off days starting in 2025

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("holidays")
    void testHolidays() throws Exception {
        List<OffDay> results = performSearch("holidays", "OffDay");
        List<OffDay> expected = Arrays.asList(
                testProducts.get(2),  // Holiday - Bob Johnson
                testProducts.get(6),  // Holiday - Jane Smith
                testProducts.get(10)  // Holiday - John Doe
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("lastDay is 2024-03-01")
    void testLastDayIs2024_03_01() throws Exception {
        List<OffDay> results  = performSearch("lastDay is 2024-03-01", "OffDay");
        List<OffDay> expected = Collections.singletonList(testProducts.get(2)); // Holiday on March 1st

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("long vacations")
    void testLongVacations() throws Exception {
        List<OffDay> results = performSearch("long vacations", "OffDay");
        List<OffDay> expected = Arrays.asList(
                testProducts.get(0),  // Vacation - John Doe
                testProducts.get(4),  // Vacation - Mike Brown
                testProducts.get(7),  // Vacation - Bob Johnson
                testProducts.get(11)  // Vacation - Jane Smith
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("multi day")
    void testMultiDay() throws Exception {
        List<OffDay> results = performSearch("multi day", "OffDay");
        List<OffDay> expected = Arrays.asList(
                testProducts.get(0),  // Vacation - John Doe (5 days)
                testProducts.get(1),  // Sick - Jane Smith (3 days)
                testProducts.get(3),  // Trip - Alice Wilson (6 days)
                testProducts.get(4),  // Vacation - Mike Brown (17 days)
                testProducts.get(5),  // Sick - John Doe (3 days)
                testProducts.get(7),  // Vacation - Bob Johnson (11 days)
                testProducts.get(8),  // Trip - Alice Wilson (5 days)
                testProducts.get(9),  // Sick - Mike Brown (3 days)
                testProducts.get(11), // Vacation - Jane Smith (12 days)
                testProducts.get(12), // Sick - Bob Johnson (5 days)
                testProducts.get(13)  // Trip - Alice Wilson (12 days)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("off days created in 2025")
    void testOffDaysCreatedIn2025() throws Exception {
        List<OffDay> results = performSearch("off days created in 2025", "OffDay");
        List<OffDay> expected = Arrays.asList(
                testProducts.get(11), // Vacation - Jane Smith
                testProducts.get(12), // Sick - Bob Johnson
                testProducts.get(13)  // Trip - Alice Wilson
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("off days ending before March 2024")
    void testOffDaysEndingBeforeMarch2024() throws Exception {
        List<OffDay> results = performSearch("off days ending before March 2024", "OffDay");
        List<OffDay> expected = Arrays.asList(
                testProducts.get(0),  // Vacation ending Jan 19, 2024
                testProducts.get(1)   // Sick ending Feb 7, 2024
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("off days in 2024")
    void testOffDaysIn2024() throws Exception {
        List<OffDay> results = performSearch("off days in 2024", "OffDay");
        List<OffDay> expected = Arrays.asList(
                testProducts.get(0),  // Vacation - John Doe
                testProducts.get(1),  // Sick - Jane Smith
                testProducts.get(2),  // Holiday - Bob Johnson
                testProducts.get(3),  // Trip - Alice Wilson
                testProducts.get(4),  // Vacation - Mike Brown
                testProducts.get(5),  // Sick - John Doe
                testProducts.get(6),  // Holiday - Jane Smith
                testProducts.get(7),  // Vacation - Bob Johnson
                testProducts.get(8),  // Trip - Alice Wilson
                testProducts.get(9)   // Sick - Mike Brown
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("off days in 2025")
    void testOffDaysIn2025() throws Exception {
        List<OffDay> results = performSearch("off days in 2025", "OffDay");
        List<OffDay> expected = Arrays.asList(
                testProducts.get(10), // Holiday - John Doe
                testProducts.get(11), // Vacation - Jane Smith
                testProducts.get(12), // Sick - Bob Johnson
                testProducts.get(13)  // Trip - Alice Wilson
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("off days in January 2024")
    void testOffDaysInJanuary2024() throws Exception {
        List<OffDay> results  = performSearch("off days in January 2024", "OffDay");
        List<OffDay> expected = Collections.singletonList(testProducts.get(0)); // Vacation - John Doe

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("off days in June 2024")
    void testOffDaysInJune2024() throws Exception {
        List<OffDay> results = performSearch("off days in June 2024", "OffDay");
        List<OffDay> expected = Arrays.asList(
                testProducts.get(4),  // Vacation - Mike Brown (May 20 - June 5)
                testProducts.get(5)   // Sick - John Doe (June 10-12)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("off days in summer 2024")
    void testOffDaysInSummer2024() throws Exception {
        List<OffDay> results = performSearch("off days in summer 2024", "OffDay");
        List<OffDay> expected = Arrays.asList(
                testProducts.get(4),  // Vacation - Mike Brown (May-June)
                testProducts.get(5),  // Sick - John Doe (June)
                testProducts.get(6),  // Holiday - Jane Smith (July)
                testProducts.get(7),  // Vacation - Bob Johnson (August)
                testProducts.get(8)   // Trip - Alice Wilson (September)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("off days lasting more than 5 days")
    void testOffDaysLastingMoreThan5Days() throws Exception {
        List<OffDay> results = performSearch("off days lasting more than 5 days", "OffDay");
        List<OffDay> expected = Arrays.asList(
                testProducts.get(4),  // Vacation - Mike Brown (17 days)
                testProducts.get(7),  // Vacation - Bob Johnson (11 days)
                testProducts.get(11), // Vacation - Jane Smith (12 days)
                testProducts.get(13)  // Trip - Alice Wilson (12 days)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("off days starting after February 2024")
    void testOffDaysStartingAfterFebruary2024() throws Exception {
        List<OffDay> results = performSearch("off days starting after February 2024", "OffDay");
        List<OffDay> expected = Arrays.asList(
                testProducts.get(2),  // Holiday - Bob Johnson (March)
                testProducts.get(3),  // Trip - Alice Wilson (April)
                testProducts.get(4),  // Vacation - Mike Brown (May)
                testProducts.get(5),  // Sick - John Doe (June)
                testProducts.get(6),  // Holiday - Jane Smith (July)
                testProducts.get(7),  // Vacation - Bob Johnson (August)
                testProducts.get(8),  // Trip - Alice Wilson (September)
                testProducts.get(9),  // Sick - Mike Brown (October)
                testProducts.get(10), // Holiday - John Doe (January 2025)
                testProducts.get(11), // Vacation - Jane Smith (February 2025)
                testProducts.get(12), // Sick - Bob Johnson (March 2025)
                testProducts.get(13)  // Trip - Alice Wilson (April 2025)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("off days starting in March")
    void testOffDaysStartingInMarch() throws Exception {
        List<OffDay> results = performSearch("off days starting in March", "OffDay");
        List<OffDay> expected = Arrays.asList(
                testProducts.get(2),  // Holiday - Bob Johnson (March 2024)
                testProducts.get(12)  // Sick - Bob Johnson (March 2025)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("off days updated in 2025")
    void testOffDaysUpdatedIn2025() throws Exception {
        List<OffDay> results = performSearch("off days updated in 2025", "OffDay");
        List<OffDay> expected = Arrays.asList(
                testProducts.get(10), // Holiday - John Doe
                testProducts.get(11), // Vacation - Jane Smith
                testProducts.get(12), // Sick - Bob Johnson
                testProducts.get(13)  // Trip - Alice Wilson
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("purple elephant dancing")
    void testPurpleElephantDancing() throws Exception {
        List<OffDay> results  = performSearch("purple elephant dancing", "OffDay");
        List<OffDay> expected = Collections.emptyList(); // Should return empty results for nonsensical queries

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("recent off days")
    void testRecentOffDays() throws Exception {
        List<OffDay> results = performSearch("recent off days", "OffDay");
        // Recent is interpreted as 2025 off days
        List<OffDay> expected = Arrays.asList(
                testProducts.get(10), // Holiday - John Doe
                testProducts.get(11), // Vacation - Jane Smith
                testProducts.get(12), // Sick - Bob Johnson
                testProducts.get(13)  // Trip - Alice Wilson
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("sick days")
    void testSickDays() throws Exception {
        List<OffDay> results = performSearch("sick days", "OffDay");
        List<OffDay> expected = Arrays.asList(
                testProducts.get(1),  // Sick - Jane Smith
                testProducts.get(5),  // Sick - John Doe
                testProducts.get(9),  // Sick - Mike Brown
                testProducts.get(12)  // Sick - Bob Johnson
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("single day")
    void testSingleDay() throws Exception {
        List<OffDay> results = performSearch("single day", "OffDay");
        List<OffDay> expected = Arrays.asList(
                testProducts.get(2),  // Holiday - Bob Johnson (single day)
                testProducts.get(6),  // Holiday - Jane Smith (single day)
                testProducts.get(10)  // Holiday - John Doe (single day)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("trips")
    void testTrips() throws Exception {
        List<OffDay> results = performSearch("trips", "OffDay");
        List<OffDay> expected = Arrays.asList(
                testProducts.get(3),  // Trip - Alice Wilson
                testProducts.get(8),  // Trip - Alice Wilson
                testProducts.get(13)  // Trip - Alice Wilson
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("type HOLIDAY")
    void testTypeHOLIDAY() throws Exception {
        List<OffDay> results = performSearch("type HOLIDAY", "OffDay");
        List<OffDay> expected = Arrays.asList(
                testProducts.get(2),  // Holiday - Bob Johnson
                testProducts.get(6),  // Holiday - Jane Smith
                testProducts.get(10)  // Holiday - John Doe
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    // === COLUMN-SPECIFIC TESTS (one per column) ===
    @Test
    @DisplayName("type is SICK")
    void testTypeIsSICK() throws Exception {
        List<OffDay> results  = performSearch("type is SICK", "OffDay");
        List<OffDay> expected = Arrays.asList(testProducts.get(1), testProducts.get(5), testProducts.get(9), testProducts.get(12)); // All sick off days

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("type SICK")
    void testTypeSICK() throws Exception {
        List<OffDay> results = performSearch("type SICK", "OffDay");
        List<OffDay> expected = Arrays.asList(
                testProducts.get(1),  // Sick - Jane Smith
                testProducts.get(5),  // Sick - John Doe
                testProducts.get(9),  // Sick - Mike Brown
                testProducts.get(12)  // Sick - Bob Johnson
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("type TRIP")
    void testTypeTRIP() throws Exception {
        List<OffDay> results = performSearch("type TRIP", "OffDay");
        List<OffDay> expected = Arrays.asList(
                testProducts.get(3),  // Trip - Alice Wilson
                testProducts.get(8),  // Trip - Alice Wilson
                testProducts.get(13)  // Trip - Alice Wilson
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("type VACATION")
    void testTypeVACATION() throws Exception {
        List<OffDay> results = performSearch("type VACATION", "OffDay");
        List<OffDay> expected = Arrays.asList(
                testProducts.get(0),  // Vacation - John Doe
                testProducts.get(4),  // Vacation - Mike Brown
                testProducts.get(7),  // Vacation - Bob Johnson
                testProducts.get(11)  // Vacation - Jane Smith
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("updated in 2025")
    void testUpdatedIn2025() throws Exception {
        List<OffDay> results  = performSearch("updated in 2025", "OffDay");
        List<OffDay> expected = Arrays.asList(testProducts.get(10), testProducts.get(11), testProducts.get(12), testProducts.get(13)); // Off days updated in 2025

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("vacation")
    void testVacation() throws Exception {
        List<OffDay> results = performSearch("vacation", "OffDay");
        List<OffDay> expected = Arrays.asList(
                testProducts.get(0),  // Vacation - John Doe
                testProducts.get(4),  // Vacation - Mike Brown
                testProducts.get(7),  // Vacation - Bob Johnson
                testProducts.get(11)  // Vacation - Jane Smith
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

}
