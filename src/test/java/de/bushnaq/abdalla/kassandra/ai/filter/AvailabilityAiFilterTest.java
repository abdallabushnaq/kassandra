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

import de.bushnaq.abdalla.kassandra.ai.filter.dto.availability.AvailabilityFilterDto;
import de.bushnaq.abdalla.kassandra.dto.Availability;
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
 * Tests the JavaScript AI filter generator for Availability entities.
 *
 * <p>Each test verifies that the LLM-generated JS filter for a natural-language
 * query produces the same result as a hand-written reference JS predicate applied
 * to the same availability list. Both the LLM filter and the reference filter operate
 * on {@link AvailabilityFilterDto} objects — which expose availability as an integer
 * percentage (0–100) — to avoid floating-point precision mismatches.</p>
 *
 * <p>The availability list is the only place that needs to change when test data is
 * updated; no individual test hard-codes expected indices or counts.</p>
 */
@Tag("AiUnitTest")
@SpringBootTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class AvailabilityAiFilterTest extends AbstractAiFilterTest<AvailabilityFilterDto> {

    public AvailabilityAiFilterTest(JsonMapper mapper, AiFilterService aiFilterService) {
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
        setupTestAvailabilities();
    }

    private void setupTestAvailabilities() {
        User johnDoe     = createUser(1L, "John Doe");
        User janeSmith   = createUser(2L, "Jane Smith");
        User bobJohnson  = createUser(3L, "Bob Johnson");
        User aliceWilson = createUser(4L, "Alice Wilson");
        User mikeBrown   = createUser(5L, "Mike Brown");

        List<Availability> raw = new ArrayList<>();

        raw.add(createAvailability(1L, 1.0f, LocalDate.of(2024, 1, 15), johnDoe,
                OffsetDateTime.of(2023, 12, 20, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 1, 10, 14, 30, 0, 0, ZoneOffset.UTC)));

        raw.add(createAvailability(2L, 0.8f, LocalDate.of(2024, 2, 1), janeSmith,
                OffsetDateTime.of(2024, 1, 15, 11, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 2, 5, 16, 45, 0, 0, ZoneOffset.UTC)));

        raw.add(createAvailability(3L, 0.5f, LocalDate.of(2024, 3, 1), bobJohnson,
                OffsetDateTime.of(2024, 2, 20, 8, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 3, 16, 13, 20, 0, 0, ZoneOffset.UTC)));

        raw.add(createAvailability(4L, 0.75f, LocalDate.of(2024, 4, 1), aliceWilson,
                OffsetDateTime.of(2024, 3, 25, 15, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 4, 15, 12, 10, 0, 0, ZoneOffset.UTC)));

        raw.add(createAvailability(5L, 0.9f, LocalDate.of(2024, 5, 1), mikeBrown,
                OffsetDateTime.of(2024, 4, 20, 14, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 5, 1, 11, 40, 0, 0, ZoneOffset.UTC)));

        raw.add(createAvailability(6L, 0.6f, LocalDate.of(2024, 6, 1), johnDoe,
                OffsetDateTime.of(2024, 5, 28, 9, 20, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 6, 10, 16, 15, 0, 0, ZoneOffset.UTC)));

        raw.add(createAvailability(7L, 0.25f, LocalDate.of(2024, 7, 1), janeSmith,
                OffsetDateTime.of(2024, 6, 25, 12, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 7, 29, 15, 50, 0, 0, ZoneOffset.UTC)));

        raw.add(createAvailability(8L, 0.85f, LocalDate.of(2024, 8, 1), bobJohnson,
                OffsetDateTime.of(2024, 7, 30, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 8, 15, 17, 30, 0, 0, ZoneOffset.UTC)));

        raw.add(createAvailability(9L, 0.0f, LocalDate.of(2024, 9, 1), aliceWilson,
                OffsetDateTime.of(2024, 8, 28, 13, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 9, 5, 9, 20, 0, 0, ZoneOffset.UTC)));

        raw.add(createAvailability(10L, 0.95f, LocalDate.of(2024, 10, 1), mikeBrown,
                OffsetDateTime.of(2024, 9, 25, 10, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 10, 22, 14, 45, 0, 0, ZoneOffset.UTC)));

        raw.add(createAvailability(11L, 0.7f, LocalDate.of(2025, 1, 1), johnDoe,
                OffsetDateTime.of(2024, 12, 28, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 10, 16, 30, 0, 0, ZoneOffset.UTC)));

        raw.add(createAvailability(12L, 0.4f, LocalDate.of(2025, 2, 1), janeSmith,
                OffsetDateTime.of(2025, 1, 28, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 2, 10, 12, 20, 0, 0, ZoneOffset.UTC)));

        // Convert each Availability to a DTO so the LLM sees integer percentages
        testProducts = new ArrayList<>();
        for (Availability a : raw) {
            testProducts.add(AvailabilityFilterDto.from(a));
        }
    }

    // -------------------------------------------------------------------------
    // Tests — each has an unambiguous natural-language query and a hand-written
    // reference JS predicate (ground truth) using integer availabilityPercent.
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("0% availability")
    void test0PercentAvailability() throws Exception {
        assertSearchMatchesReference(
                "0% availability",
                "Availability",
                "return entity.getAvailabilityPercent() === 0;"
        );
    }

    @Test
    @DisplayName("100% availability")
    void test100PercentAvailability() throws Exception {
        assertSearchMatchesReference(
                "100% availability",
                "Availability",
                "return entity.getAvailabilityPercent() === 100;"
        );
    }

    @Test
    @DisplayName("50% availability")
    void test50PercentAvailability() throws Exception {
        assertSearchMatchesReference(
                "50% availability",
                "Availability",
                "return entity.getAvailabilityPercent() === 50;"
        );
    }

    @Test
    @DisplayName("80% availability")
    void test80PercentAvailability() throws Exception {
        assertSearchMatchesReference(
                "80% availability",
                "Availability",
                "return entity.getAvailabilityPercent() === 80;"
        );
    }

    @Test
    @DisplayName("95% availability")
    void test95PercentAvailability() throws Exception {
        assertSearchMatchesReference(
                "95% availability",
                "Availability",
                "return entity.getAvailabilityPercent() === 95;"
        );
    }

    @Test
    @DisplayName("availability between 60% and 80% inclusive")
    void testAvailabilityBetween60And80PercentInclusive() throws Exception {
        assertSearchMatchesReference(
                "availability between 60% and 80% inclusive",
                "Availability",
                "return entity.getAvailabilityPercent() >= 60 && entity.getAvailabilityPercent() <= 80;"
        );
    }

    @Test
    @DisplayName("availability created in 2025")
    void testAvailabilityCreatedIn2025() throws Exception {
        assertSearchMatchesReference(
                "availability created in 2025",
                "Availability",
                "return entity.getCreated().getYear() === 2025;"
        );
    }

    @Test
    @DisplayName("availability greater than 50%")
    void testAvailabilityGreaterThan50Percent() throws Exception {
        assertSearchMatchesReference(
                "availability greater than 50%",
                "Availability",
                "return entity.getAvailabilityPercent() > 50;"
        );
    }

    @Test
    @DisplayName("availability greater than or equal to 70%")
    void testAvailabilityGreaterThanOrEqualTo70Percent() throws Exception {
        assertSearchMatchesReference(
                "availability greater than or equal to 70%",
                "Availability",
                "return entity.getAvailabilityPercent() >= 70;"
        );
    }

    @Test
    @DisplayName("availability less than 90%")
    void testAvailabilityLessThan90Percent() throws Exception {
        assertSearchMatchesReference(
                "availability less than 90%",
                "Availability",
                "return entity.getAvailabilityPercent() < 90;"
        );
    }

    @Test
    @DisplayName("availability less than or equal to 40%")
    void testAvailabilityLessThanOrEqualTo40Percent() throws Exception {
        assertSearchMatchesReference(
                "availability less than or equal to 40%",
                "Availability",
                "return entity.getAvailabilityPercent() <= 40;"
        );
    }

    @Test
    @DisplayName("availability starting after January 2024")
    void testAvailabilityStartingAfterJanuary2024() throws Exception {
        assertSearchMatchesReference(
                "availability starting after January 2024",
                "Availability",
                """
                        const boundary = Java.type('java.time.LocalDate').of(2024, 1, 31);
                        return entity.getStart().isAfter(boundary);
                        """
        );
    }

    @Test
    @DisplayName("availability starting before March 2024")
    void testAvailabilityStartingBeforeMarch2024() throws Exception {
        assertSearchMatchesReference(
                "availability starting before March 2024",
                "Availability",
                """
                        const boundary = Java.type('java.time.LocalDate').of(2024, 3, 1);
                        return entity.getStart().isBefore(boundary);
                        """
        );
    }

    @Test
    @DisplayName("availability starting in January 2025")
    void testAvailabilityStartingInJanuary2025() throws Exception {
        assertSearchMatchesReference(
                "availability starting in January 2025",
                "Availability",
                "return entity.getStart().getYear() === 2025 && entity.getStart().getMonthValue() === 1;"
        );
    }

    @Test
    @DisplayName("availability starting in June, July or August 2024")
    void testAvailabilityStartingInJuneJulyOrAugust2024() throws Exception {
        assertSearchMatchesReference(
                "availability starting in June, July or August 2024",
                "Availability",
                """
                        const month = entity.getStart().getMonthValue();
                        const year  = entity.getStart().getYear();
                        return year === 2024 && (month === 6 || month === 7 || month === 8);
                        """
        );
    }

    @Test
    @DisplayName("availability updated in 2025")
    void testAvailabilityUpdatedIn2025() throws Exception {
        assertSearchMatchesReference(
                "availability updated in 2025",
                "Availability",
                "return entity.getUpdated().getYear() === 2025;"
        );
    }

    @Test
    @DisplayName("empty search query")
    void testEmptySearchQuery() throws Exception {
        assertSearchMatchesReference(
                "",
                "Availability",
                "return true;"
        );
    }
}
