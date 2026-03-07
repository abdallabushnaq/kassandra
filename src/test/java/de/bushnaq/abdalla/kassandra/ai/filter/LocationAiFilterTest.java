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

import de.bushnaq.abdalla.kassandra.ai.filter.dto.location.LocationFilterDto;
import de.bushnaq.abdalla.kassandra.dto.Location;
import de.bushnaq.abdalla.kassandra.dto.User;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the JavaScript AI filter generator for Location entities.
 *
 * <p>Each test verifies that the LLM-generated JS filter for a natural-language
 * query produces the same result as a hand-written reference JS predicate applied
 * to the same location list. Both the LLM filter and the reference filter operate
 * on {@link LocationFilterDto} objects, keeping the filter layer decoupled from
 * the full entity. The location list is the only place that needs to change when
 * test data is updated; no individual test hard-codes expected indices or counts.</p>
 */
@Tag("AiUnitTest")
@SpringBootTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class LocationAiFilterTest extends AbstractAiFilterTest<LocationFilterDto> {

    public LocationAiFilterTest(JsonMapper mapper, AiFilterService aiFilterService) {
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
        User johnDoe     = createUser(1L, "John Doe");
        User janeSmith   = createUser(2L, "Jane Smith");
        User bobJohnson  = createUser(3L, "Bob Johnson");
        User aliceWilson = createUser(4L, "Alice Wilson");
        User mikeBrown   = createUser(5L, "Mike Brown");

        List<Location> raw = new ArrayList<>();

        raw.add(createLocation(1L, "Germany", "Bavaria", LocalDate.of(2024, 1, 15), johnDoe,
                OffsetDateTime.of(2023, 12, 20, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 1, 10, 14, 30, 0, 0, ZoneOffset.UTC)));

        raw.add(createLocation(2L, "United States", "California", LocalDate.of(2024, 2, 1), janeSmith,
                OffsetDateTime.of(2024, 1, 15, 11, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 2, 5, 16, 45, 0, 0, ZoneOffset.UTC)));

        raw.add(createLocation(3L, "Australia", "Victoria", LocalDate.of(2024, 3, 1), bobJohnson,
                OffsetDateTime.of(2024, 2, 20, 8, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 3, 16, 13, 20, 0, 0, ZoneOffset.UTC)));

        raw.add(createLocation(4L, "United Kingdom", "England", LocalDate.of(2024, 4, 1), aliceWilson,
                OffsetDateTime.of(2024, 3, 25, 15, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 4, 15, 12, 10, 0, 0, ZoneOffset.UTC)));

        raw.add(createLocation(5L, "France", "Île-de-France", LocalDate.of(2024, 5, 1), mikeBrown,
                OffsetDateTime.of(2024, 4, 20, 14, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 5, 1, 11, 40, 0, 0, ZoneOffset.UTC)));

        raw.add(createLocation(6L, "Germany", "North Rhine-Westphalia", LocalDate.of(2024, 6, 1), johnDoe,
                OffsetDateTime.of(2024, 5, 28, 9, 20, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 6, 10, 16, 15, 0, 0, ZoneOffset.UTC)));

        raw.add(createLocation(7L, "Canada", "Ontario", LocalDate.of(2024, 7, 1), janeSmith,
                OffsetDateTime.of(2024, 6, 25, 12, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 7, 29, 15, 50, 0, 0, ZoneOffset.UTC)));

        raw.add(createLocation(8L, "Netherlands", "North Holland", LocalDate.of(2024, 8, 1), bobJohnson,
                OffsetDateTime.of(2024, 7, 30, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 8, 15, 17, 30, 0, 0, ZoneOffset.UTC)));

        raw.add(createLocation(9L, "Italy", "Lombardy", LocalDate.of(2024, 9, 1), aliceWilson,
                OffsetDateTime.of(2024, 8, 28, 13, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 9, 5, 9, 20, 0, 0, ZoneOffset.UTC)));

        raw.add(createLocation(10L, "Spain", "Catalonia", LocalDate.of(2024, 10, 1), mikeBrown,
                OffsetDateTime.of(2024, 9, 25, 10, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 10, 22, 14, 45, 0, 0, ZoneOffset.UTC)));

        raw.add(createLocation(11L, "United States", "New York", LocalDate.of(2025, 1, 1), johnDoe,
                OffsetDateTime.of(2024, 12, 28, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 10, 16, 30, 0, 0, ZoneOffset.UTC)));

        raw.add(createLocation(12L, "Australia", "New South Wales", LocalDate.of(2025, 2, 1), janeSmith,
                OffsetDateTime.of(2025, 1, 28, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 2, 10, 12, 20, 0, 0, ZoneOffset.UTC)));

        testProducts = new ArrayList<>();
        for (Location l : raw) {
            testProducts.add(LocationFilterDto.from(l));
        }
    }

    // -------------------------------------------------------------------------
    // Tests — each has an unambiguous natural-language query and a hand-written
    // reference JS predicate that is the ground truth for that query.
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("country is Australia")
    void testCountryIsAustralia() throws Exception {
        assertSearchMatchesReference(
                "country is Australia",
                "Location",
                "return entity.getCountry().toLowerCase().includes('australia');"
        );
    }

    @Test
    @DisplayName("empty search query")
    void testEmptySearchQuery() throws Exception {
        assertSearchMatchesReference(
                "",
                "Location",
                "return true;"
        );
    }

    @Test
    @DisplayName("Germany")
    void testGermany() throws Exception {
        assertSearchMatchesReference(
                "Germany",
                "Location",
                "return entity.getCountry().toLowerCase().includes('germany');"
        );
    }

    @Test
    @DisplayName("locations created in 2025")
    void testLocationsCreatedIn2025() throws Exception {
        assertSearchMatchesReference(
                "locations created in 2025",
                "Location",
                "return entity.getCreated().getYear() === 2025;"
        );
    }

    @Test
    @DisplayName("locations in Australia")
    void testLocationsInAustralia() throws Exception {
        assertSearchMatchesReference(
                "locations in Australia",
                "Location",
                "return entity.getCountry().toLowerCase().includes('australia');"
        );
    }

    @Test
    @DisplayName("locations starting after January 31 2024")
    void testLocationsStartingAfterJanuary31_2024() throws Exception {
        // Explicit day removes all ambiguity about whether "after January" means after Jan 1 or Jan 31
        assertSearchMatchesReference(
                "locations starting after January 31 2024",
                "Location",
                """
                        const boundary = Java.type('java.time.LocalDate').of(2024, 1, 31);
                        return entity.getStart().isAfter(boundary);
                        """
        );
    }

    @Test
    @DisplayName("locations starting before March 1 2024")
    void testLocationsStartingBeforeMarch1_2024() throws Exception {
        // Explicit day removes all ambiguity about whether "before March" means before Mar 1 or Mar 31
        assertSearchMatchesReference(
                "locations starting before March 1 2024",
                "Location",
                """
                        const boundary = Java.type('java.time.LocalDate').of(2024, 3, 1);
                        return entity.getStart().isBefore(boundary);
                        """
        );
    }

    @Test
    @DisplayName("locations starting in 2025")
    void testLocationsStartingIn2025() throws Exception {
        assertSearchMatchesReference(
                "locations starting in 2025",
                "Location",
                "return entity.getStart().getYear() === 2025;"
        );
    }

    @Test
    @DisplayName("locations starting in June, July or August 2024")
    void testLocationsStartingInJuneJulyOrAugust2024() throws Exception {
        // Explicit months instead of the subjective term "summer"
        assertSearchMatchesReference(
                "locations starting in June, July or August 2024",
                "Location",
                """
                        const month = entity.getStart().getMonthValue();
                        const year  = entity.getStart().getYear();
                        return year === 2024 && (month === 6 || month === 7 || month === 8);
                        """
        );
    }

    @Test
    @DisplayName("locations updated in 2025")
    void testLocationsUpdatedIn2025() throws Exception {
        assertSearchMatchesReference(
                "locations updated in 2025",
                "Location",
                "return entity.getUpdated().getYear() === 2025;"
        );
    }

    @Test
    @DisplayName("state is Bavaria")
    void testStateIsBavaria() throws Exception {
        assertSearchMatchesReference(
                "state is Bavaria",
                "Location",
                "return entity.getState().toLowerCase().includes('bavaria');"
        );
    }

    @Test
    @DisplayName("state is California")
    void testStateIsCalifornia() throws Exception {
        assertSearchMatchesReference(
                "state is California",
                "Location",
                "return entity.getState().toLowerCase().includes('california');"
        );
    }
}
