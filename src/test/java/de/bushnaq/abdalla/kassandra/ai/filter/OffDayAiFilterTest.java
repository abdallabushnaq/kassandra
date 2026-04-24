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

import de.bushnaq.abdalla.kassandra.ai.filter.dto.offday.OffDayFilterDto;
import de.bushnaq.abdalla.kassandra.dto.OffDay;
import de.bushnaq.abdalla.kassandra.dto.OffDayType;
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
import java.util.UUID;

/**
 * Tests the JavaScript AI filter generator for OffDay entities.
 *
 * <p>Each test verifies that the LLM-generated JS filter for a natural-language
 * query produces the same result as a hand-written reference JS predicate applied
 * to the same off-day list. Both the LLM filter and the reference filter operate
 * on {@link OffDayFilterDto} objects, where {@code type} is a plain {@code String}
 * (e.g. {@code "VACATION"}) rather than an enum — eliminating the need for
 * {@code Java.type()} lookups in the generated JS. The off-day list is the only
 * place that needs to change when test data is updated; no individual test
 * hard-codes expected indices or counts.</p>
 */
@Tag("AiUnitTest")
@SpringBootTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class OffDayAiFilterTest extends AbstractAiFilterTest<OffDayFilterDto> {

    public OffDayAiFilterTest(JsonMapper mapper, AiFilterService aiFilterService) {
        super(mapper, aiFilterService, LocalDate.of(2025, 8, 10));
    }

    private OffDay createOffDay(UUID id, LocalDate firstDay, LocalDate lastDay, OffDayType type,
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

    private User createUser(UUID id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        return user;
    }

    @BeforeEach
    void setUp() {
        User johnDoe     = createUser(UUID.randomUUID(), "John Doe");
        User janeSmith   = createUser(UUID.randomUUID(), "Jane Smith");
        User bobJohnson  = createUser(UUID.randomUUID(), "Bob Johnson");
        User aliceWilson = createUser(UUID.randomUUID(), "Alice Wilson");
        User mikeBrown   = createUser(UUID.randomUUID(), "Mike Brown");

        List<OffDay> raw = new ArrayList<>();

        // id=1  VACATION  Jan 15–19 2024   (5 days, multi-day)
        raw.add(createOffDay(UUID.randomUUID(), LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 19), OffDayType.VACATION, johnDoe,
                OffsetDateTime.of(2023, 12, 20, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 1, 10, 14, 30, 0, 0, ZoneOffset.UTC)));

        // id=2  SICK      Feb 5–7 2024     (3 days, multi-day)
        raw.add(createOffDay(UUID.randomUUID(), LocalDate.of(2024, 2, 5), LocalDate.of(2024, 2, 7), OffDayType.SICK, janeSmith,
                OffsetDateTime.of(2024, 1, 15, 11, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 2, 5, 16, 45, 0, 0, ZoneOffset.UTC)));

        // id=3  HOLIDAY   Mar 1 2024       (single day)
        raw.add(createOffDay(UUID.randomUUID(), LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 1), OffDayType.HOLIDAY, bobJohnson,
                OffsetDateTime.of(2024, 2, 20, 8, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 3, 1, 13, 20, 0, 0, ZoneOffset.UTC)));

        // id=4  TRIP      Apr 10–15 2024   (6 days, multi-day)
        raw.add(createOffDay(UUID.randomUUID(), LocalDate.of(2024, 4, 10), LocalDate.of(2024, 4, 15), OffDayType.TRIP, aliceWilson,
                OffsetDateTime.of(2024, 3, 25, 15, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 4, 15, 12, 10, 0, 0, ZoneOffset.UTC)));

        // id=5  VACATION  May 20 – Jun 5 2024  (17 days, multi-day)
        raw.add(createOffDay(UUID.randomUUID(), LocalDate.of(2024, 5, 20), LocalDate.of(2024, 6, 5), OffDayType.VACATION, mikeBrown,
                OffsetDateTime.of(2024, 4, 20, 14, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 5, 1, 11, 40, 0, 0, ZoneOffset.UTC)));

        // id=6  SICK      Jun 10–12 2024   (3 days, multi-day)
        raw.add(createOffDay(UUID.randomUUID(), LocalDate.of(2024, 6, 10), LocalDate.of(2024, 6, 12), OffDayType.SICK, johnDoe,
                OffsetDateTime.of(2024, 5, 28, 9, 20, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 6, 10, 16, 15, 0, 0, ZoneOffset.UTC)));

        // id=7  HOLIDAY   Jul 4 2024       (single day)
        raw.add(createOffDay(UUID.randomUUID(), LocalDate.of(2024, 7, 4), LocalDate.of(2024, 7, 4), OffDayType.HOLIDAY, janeSmith,
                OffsetDateTime.of(2024, 6, 25, 12, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 7, 4, 15, 50, 0, 0, ZoneOffset.UTC)));

        // id=8  VACATION  Aug 15–25 2024   (11 days, multi-day)
        raw.add(createOffDay(UUID.randomUUID(), LocalDate.of(2024, 8, 15), LocalDate.of(2024, 8, 25), OffDayType.VACATION, bobJohnson,
                OffsetDateTime.of(2024, 7, 30, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 8, 15, 17, 30, 0, 0, ZoneOffset.UTC)));

        // id=9  TRIP      Sep 2–6 2024     (5 days, multi-day)
        raw.add(createOffDay(UUID.randomUUID(), LocalDate.of(2024, 9, 2), LocalDate.of(2024, 9, 6), OffDayType.TRIP, aliceWilson,
                OffsetDateTime.of(2024, 8, 28, 13, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 9, 5, 9, 20, 0, 0, ZoneOffset.UTC)));

        // id=10 SICK      Oct 28–30 2024   (3 days, multi-day)
        raw.add(createOffDay(UUID.randomUUID(), LocalDate.of(2024, 10, 28), LocalDate.of(2024, 10, 30), OffDayType.SICK, mikeBrown,
                OffsetDateTime.of(2024, 9, 25, 10, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 10, 28, 14, 45, 0, 0, ZoneOffset.UTC)));

        // id=11 HOLIDAY   Jan 1 2025       (single day)
        raw.add(createOffDay(UUID.randomUUID(), LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 1), OffDayType.HOLIDAY, johnDoe,
                OffsetDateTime.of(2024, 12, 28, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 1, 16, 30, 0, 0, ZoneOffset.UTC)));

        // id=12 VACATION  Feb 10–21 2025   (12 days, multi-day)
        raw.add(createOffDay(UUID.randomUUID(), LocalDate.of(2025, 2, 10), LocalDate.of(2025, 2, 21), OffDayType.VACATION, janeSmith,
                OffsetDateTime.of(2025, 1, 28, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 2, 10, 12, 20, 0, 0, ZoneOffset.UTC)));

        // id=13 SICK      Mar 3–7 2025     (5 days, multi-day)
        raw.add(createOffDay(UUID.randomUUID(), LocalDate.of(2025, 3, 3), LocalDate.of(2025, 3, 7), OffDayType.SICK, bobJohnson,
                OffsetDateTime.of(2025, 2, 28, 10, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 3, 3, 11, 30, 0, 0, ZoneOffset.UTC)));

        // id=14 TRIP      Apr 14–25 2025   (12 days, multi-day)
        raw.add(createOffDay(UUID.randomUUID(), LocalDate.of(2025, 4, 14), LocalDate.of(2025, 4, 25), OffDayType.TRIP, aliceWilson,
                OffsetDateTime.of(2025, 3, 20, 14, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 4, 14, 9, 45, 0, 0, ZoneOffset.UTC)));

        testProducts = new ArrayList<>();
        for (OffDay o : raw) {
            testProducts.add(OffDayFilterDto.from(o));
        }
    }

    // -------------------------------------------------------------------------
    // Tests — each has an unambiguous natural-language query and a hand-written
    // reference JS predicate that is the ground truth for that query.
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("empty search query")
    void testEmptySearchQuery() throws Exception {
        assertSearchMatchesReference(
                "",
                "OffDay",
                "return true;"
        );
    }

    // --- type filters ---------------------------------------------------------

    @Test
    @DisplayName("lastDay is 2024-03-01")
    void testLastDayIs2024_03_01() throws Exception {
        assertSearchMatchesReference(
                "lastDay is 2024-03-01",
                "OffDay",
                """
                        const target = Java.type('java.time.LocalDate').of(2024, 3, 1);
                        return entity.getLastDay().equals(target);
                        """
        );
    }

    @Test
    @DisplayName("multi-day off periods")
    void testMultiDayOffPeriods() throws Exception {
        assertSearchMatchesReference(
                "multi-day off periods",
                "OffDay",
                "return !entity.getFirstDay().equals(entity.getLastDay());"
        );
    }

    @Test
    @DisplayName("off days created in 2025")
    void testOffDaysCreatedIn2025() throws Exception {
        assertSearchMatchesReference(
                "off days created in 2025",
                "OffDay",
                "return entity.getCreated().getYear() === 2025;"
        );
    }

    @Test
    @DisplayName("off days ending before March 1 2024")
    void testOffDaysEndingBeforeMarch1_2024() throws Exception {
        // Explicit day removes all ambiguity about "before March"
        assertSearchMatchesReference(
                "off days ending before March 1 2024",
                "OffDay",
                """
                        const boundary = Java.type('java.time.LocalDate').of(2024, 3, 1);
                        return entity.getLastDay().isBefore(boundary);
                        """
        );
    }

    // --- single/multi-day filters --------------------------------------------

    @Test
    @DisplayName("off days lasting more than 5 days")
    void testOffDaysLastingMoreThan5Days() throws Exception {
        // duration = inclusive day count = ChronoUnit.DAYS.between(firstDay, lastDay) + 1; strictly > 5
        assertSearchMatchesReference(
                "off days lasting more than 5 days",
                "OffDay",
                """
                        const ChronoUnit = Java.type('java.time.temporal.ChronoUnit');
                        return ChronoUnit.DAYS.between(entity.getFirstDay(), entity.getLastDay()) + 1 > 5;
                        """
        );
    }

    @Test
    @DisplayName("off days starting after February 28 2024")
    void testOffDaysStartingAfterFebruary28_2024() throws Exception {
        // Explicit day removes all ambiguity about "after February"
        assertSearchMatchesReference(
                "off days starting after February 28 2024",
                "OffDay",
                """
                        const boundary = Java.type('java.time.LocalDate').of(2024, 2, 28);
                        return entity.getFirstDay().isAfter(boundary);
                        """
        );
    }

    // --- duration filter ------------------------------------------------------

    @Test
    @DisplayName("off days starting in 2025")
    void testOffDaysStartingIn2025() throws Exception {
        assertSearchMatchesReference(
                "off days starting in 2025",
                "OffDay",
                "return entity.getFirstDay().getYear() === 2025;"
        );
    }

    @Test
    @DisplayName("off days starting in January 2024")
    void testOffDaysStartingInJanuary2024() throws Exception {
        assertSearchMatchesReference(
                "off days starting in January 2024",
                "OffDay",
                "return entity.getFirstDay().getYear() === 2024 && entity.getFirstDay().getMonthValue() === 1;"
        );
    }

    // --- firstDay / lastDay date filters -------------------------------------

    @Test
    @DisplayName("off days starting in June, July or August 2024")
    void testOffDaysStartingInJuneJulyOrAugust2024() throws Exception {
        // Explicit months instead of the subjective term "summer"
        assertSearchMatchesReference(
                "off days starting in June, July or August 2024",
                "OffDay",
                """
                        const month = entity.getFirstDay().getMonthValue();
                        const year  = entity.getFirstDay().getYear();
                        return year === 2024 && (month === 6 || month === 7 || month === 8);
                        """
        );
    }

    @Test
    @DisplayName("off days starting in March")
    void testOffDaysStartingInMarch() throws Exception {
        assertSearchMatchesReference(
                "off days starting in March",
                "OffDay",
                "return entity.getFirstDay().getMonthValue() === 3;"
        );
    }

    @Test
    @DisplayName("off days updated in 2025")
    void testOffDaysUpdatedIn2025() throws Exception {
        assertSearchMatchesReference(
                "off days updated in 2025",
                "OffDay",
                "return entity.getUpdated().getYear() === 2025;"
        );
    }

    @Test
    @DisplayName("single-day off periods")
    void testSingleDayOffPeriods() throws Exception {
        assertSearchMatchesReference(
                "single-day off periods",
                "OffDay",
                "return entity.getFirstDay().equals(entity.getLastDay());"
        );
    }

    @Test
    @DisplayName("type is HOLIDAY")
    void testTypeIsHoliday() throws Exception {
        assertSearchMatchesReference(
                "type is HOLIDAY",
                "OffDay",
                "return entity.getType() === 'HOLIDAY';"
        );
    }

    @Test
    @DisplayName("type is SICK")
    void testTypeIsSick() throws Exception {
        assertSearchMatchesReference(
                "type is SICK",
                "OffDay",
                "return entity.getType() === 'SICK';"
        );
    }

    @Test
    @DisplayName("type is TRIP")
    void testTypeIsTrip() throws Exception {
        assertSearchMatchesReference(
                "type is TRIP",
                "OffDay",
                "return entity.getType() === 'TRIP';"
        );
    }

    // --- created / updated filters -------------------------------------------

    @Test
    @DisplayName("type is VACATION")
    void testTypeIsVacation() throws Exception {
        assertSearchMatchesReference(
                "type is VACATION",
                "OffDay",
                "return entity.getType() === 'VACATION';"
        );
    }

    @Test
    @DisplayName("vacations longer than 7 days")
    void testVacationsLongerThan7Days() throws Exception {
        assertSearchMatchesReference(
                "vacations longer than 7 days",
                "OffDay",
                """
                        const ChronoUnit = Java.type('java.time.temporal.ChronoUnit');
                        return entity.getType() === 'VACATION' && ChronoUnit.DAYS.between(entity.getFirstDay(), entity.getLastDay()) > 7;
                        """
        );
    }
}
