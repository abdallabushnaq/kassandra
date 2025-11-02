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
import de.bushnaq.abdalla.kassandra.dto.Location;
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
 * Integration test for Location AI filtering testing real LLM regex pattern generation
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
class LocationAiFilterTest extends AbstractAiFilterTest<Location> {

    public LocationAiFilterTest(ObjectMapper mapper, AiFilterService aiFilterService) {
        super(mapper, aiFilterService, LocalDate.of(2025, 8, 10));
    }

    private Location createLocation(Long id, String country, String state, LocalDate start,
                                    User user, OffsetDateTime created, OffsetDateTime updated) {
        Location location = new Location();
        location.setId(id);
        location.setCountry(country);
        location.setState(state);
        location.setStart(start);
        location.setUser(user);
        location.setCreated(created);
        location.setUpdated(updated);
        return location;
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
        setupTestLocations();
    }

    private void setupTestLocations() {
        testProducts = new ArrayList<>();

        // Create test users
        User johnDoe     = createUser(1L, "John Doe");
        User janeSmith   = createUser(2L, "Jane Smith");
        User bobJohnson  = createUser(3L, "Bob Johnson");
        User aliceWilson = createUser(4L, "Alice Wilson");
        User mikeBrown   = createUser(5L, "Mike Brown");

        // Locations with different countries, states, and dates for comprehensive testing
        testProducts.add(createLocation(1L, "Germany", "Bavaria", LocalDate.of(2024, 1, 15), johnDoe,
                OffsetDateTime.of(2023, 12, 20, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 1, 10, 14, 30, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createLocation(2L, "United States", "California", LocalDate.of(2024, 2, 1), janeSmith,
                OffsetDateTime.of(2024, 1, 15, 11, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 2, 5, 16, 45, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createLocation(3L, "Australia", "Victoria", LocalDate.of(2024, 3, 1), bobJohnson,
                OffsetDateTime.of(2024, 2, 20, 8, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 3, 16, 13, 20, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createLocation(4L, "United Kingdom", "England", LocalDate.of(2024, 4, 1), aliceWilson,
                OffsetDateTime.of(2024, 3, 25, 15, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 4, 15, 12, 10, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createLocation(5L, "France", "Île-de-France", LocalDate.of(2024, 5, 1), mikeBrown,
                OffsetDateTime.of(2024, 4, 20, 14, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 5, 1, 11, 40, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createLocation(6L, "Germany", "North Rhine-Westphalia", LocalDate.of(2024, 6, 1), johnDoe,
                OffsetDateTime.of(2024, 5, 28, 9, 20, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 6, 10, 16, 15, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createLocation(7L, "Canada", "Ontario", LocalDate.of(2024, 7, 1), janeSmith,
                OffsetDateTime.of(2024, 6, 25, 12, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 7, 29, 15, 50, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createLocation(8L, "Netherlands", "North Holland", LocalDate.of(2024, 8, 1), bobJohnson,
                OffsetDateTime.of(2024, 7, 30, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 8, 15, 17, 30, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createLocation(9L, "Italy", "Lombardy", LocalDate.of(2024, 9, 1), aliceWilson,
                OffsetDateTime.of(2024, 8, 28, 13, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 9, 5, 9, 20, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createLocation(10L, "Spain", "Catalonia", LocalDate.of(2024, 10, 1), mikeBrown,
                OffsetDateTime.of(2024, 9, 25, 10, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 10, 22, 14, 45, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createLocation(11L, "United States", "New York", LocalDate.of(2025, 1, 1), johnDoe,
                OffsetDateTime.of(2024, 12, 28, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 10, 16, 30, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createLocation(12L, "Australia", "New South Wales", LocalDate.of(2025, 2, 1), janeSmith,
                OffsetDateTime.of(2025, 1, 28, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 2, 10, 12, 20, 0, 0, ZoneOffset.UTC)));
    }

    @Test
    @DisplayName("California")
    void testCalifornia() throws Exception {
        List<Location> results  = performSearch("California", "Location");
        List<Location> expected = Collections.singletonList(testProducts.get(1)); // United States, California

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("country is Australia")
    void testCountryIsAustralia() throws Exception {
        List<Location> results  = performSearch("country is Australia", "Location");
        List<Location> expected = Arrays.asList(testProducts.get(2), testProducts.get(11)); // Australia Victoria, Australia New South Wales

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("created in 2025")
    void testCreatedIn2025() throws Exception {
        List<Location> results  = performSearch("created in 2025", "Location");
        List<Location> expected = Collections.singletonList(testProducts.get(11)); // Australia, New South Wales (created 2025-01-28)

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("empty search query")
    void testEmptySearchQuery() throws Exception {
        List<Location> results  = performSearch("", "Location");
        List<Location> expected = new ArrayList<>(testProducts); // All locations should match empty query

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("German states")
    void testGermanStates() throws Exception {
        List<Location> results = performSearch("German states", "Location");
        List<Location> expected = Arrays.asList(
                testProducts.get(0), // Germany, Bavaria
                testProducts.get(5)  // Germany, North Rhine-Westphalia
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    // === SIMPLE TEST CASE (keeping only ONE) ===
    @Test
    @DisplayName("Germany")
    void testGermany() throws Exception {
        List<Location> results  = performSearch("Germany", "Location");
        List<Location> expected = Arrays.asList(testProducts.get(0), testProducts.get(5)); // Germany Bavaria, Germany North Rhine-Westphalia

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("locations created in 2025")
    void testLocationsCreatedIn2025() throws Exception {
        List<Location> results  = performSearch("locations created in 2025", "Location");
        List<Location> expected = Collections.singletonList(testProducts.get(11)); // Australia, New South Wales (created 2025-01-28)

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("locations in Australia")
    void testLocationsInAustralia() throws Exception {
        List<Location> results = performSearch("locations in Australia", "Location");
        List<Location> expected = Arrays.asList(
                testProducts.get(2),  // Australia, Victoria
                testProducts.get(11)  // Australia, New South Wales
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("locations starting after January 2024")
    void testLocationsStartingAfterJanuary2024() throws Exception {
        List<Location> results = performSearch("locations starting after January 2024", "Location");
        List<Location> expected = Arrays.asList(
                testProducts.get(1),  // United States, California (Feb 2024)
                testProducts.get(2),  // Australia, Victoria (Mar 2024)
                testProducts.get(3),  // United Kingdom, England (Apr 2024)
                testProducts.get(4),  // France, Île-de-France (May 2024)
                testProducts.get(5),  // Germany, North Rhine-Westphalia (Jun 2024)
                testProducts.get(6),  // Canada, Ontario (Jul 2024)
                testProducts.get(7),  // Netherlands, North Holland (Aug 2024)
                testProducts.get(8),  // Italy, Lombardy (Sep 2024)
                testProducts.get(9),  // Spain, Catalonia (Oct 2024)
                testProducts.get(10), // United States, New York (Jan 2025)
                testProducts.get(11)  // Australia, New South Wales (Feb 2025)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("locations starting before March 2024")
    void testLocationsStartingBeforeMarch2024() throws Exception {
        List<Location> results = performSearch("locations starting before March 2024", "Location");
        List<Location> expected = Arrays.asList(
                testProducts.get(0), // Germany, Bavaria (Jan 2024)
                testProducts.get(1)  // United States, California (Feb 2024)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("locations starting in 2025")
    void testLocationsStartingIn2025() throws Exception {
        List<Location> results = performSearch("locations starting in 2025", "Location");
        List<Location> expected = Arrays.asList(
                testProducts.get(10), // United States, New York (Jan 2025)
                testProducts.get(11)  // Australia, New South Wales (Feb 2025)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("locations starting in summer 2024")
    void testLocationsStartingInSummer2024() throws Exception {
        List<Location> results = performSearch("locations starting in summer 2024", "Location");
        List<Location> expected = Arrays.asList(
                testProducts.get(5), // Germany, North Rhine-Westphalia (Jun 2024)
                testProducts.get(6), // Canada, Ontario (Jul 2024)
                testProducts.get(7)  // Netherlands, North Holland (Aug 2024)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("locations updated in 2025")
    void testLocationsUpdatedIn2025() throws Exception {
        List<Location> results = performSearch("locations updated in 2025", "Location");
        List<Location> expected = Arrays.asList(
                testProducts.get(10), // United States, New York (updated 2025-01-10)
                testProducts.get(11)  // Australia, New South Wales (updated 2025-02-10)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("purple elephant dancing")
    void testPurpleElephantDancing() throws Exception {
        List<Location> results  = performSearch("purple elephant dancing", "Location");
        List<Location> expected = Collections.emptyList(); // Should return empty results for nonsensical queries

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("start date in 2025")
    void testStartDateIn2025() throws Exception {
        List<Location> results  = performSearch("start date in 2025", "Location");
        List<Location> expected = Arrays.asList(testProducts.get(10), testProducts.get(11)); // United States New York, Australia New South Wales

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("updated in 2025")
    void testUpdatedIn2025() throws Exception {
        List<Location> results  = performSearch("updated in 2025", "Location");
        List<Location> expected = Arrays.asList(testProducts.get(10), testProducts.get(11)); // United States New York, Australia New South Wales

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

}
