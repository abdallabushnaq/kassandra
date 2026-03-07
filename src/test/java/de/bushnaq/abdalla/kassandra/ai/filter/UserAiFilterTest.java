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

import de.bushnaq.abdalla.kassandra.ai.filter.dto.user.UserFilterDto;
import de.bushnaq.abdalla.kassandra.dto.User;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import tools.jackson.databind.json.JsonMapper;

import java.awt.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the JavaScript AI filter generator for User entities.
 *
 * <p>Each test verifies that the LLM-generated JS filter for a natural-language
 * query produces the same result as a hand-written reference JS predicate applied
 * to the same user list. Both the LLM filter and the reference filter operate on
 * {@link UserFilterDto} objects. {@code Color} is excluded from the DTO — it is
 * not meaningful for any text- or date-based filter query.</p>
 *
 * <p>The {@code now} date is pinned to {@code 2025-08-10} so tenure-based
 * reference predicates that use {@code entity.getNow()} are fully deterministic
 * against the fixed test data.</p>
 *
 * <p>The user list is the only place that needs to change when test data is updated;
 * no individual test hard-codes expected indices or counts.</p>
 */
@Tag("AiUnitTest")
@SpringBootTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class UserAiFilterTest extends AbstractAiFilterTest<UserFilterDto> {

    public UserAiFilterTest(JsonMapper mapper, AiFilterService aiFilterService) {
        // now = 2025-08-10 — pinned so tenure reference predicates are deterministic
        super(mapper, aiFilterService, LocalDate.of(2025, 8, 10));
    }

    private User createUser(Long id, String name, String email,
                            LocalDate firstWorkingDay, LocalDate lastWorkingDay,
                            Color color, OffsetDateTime created, OffsetDateTime updated) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setFirstWorkingDay(firstWorkingDay);
        user.setLastWorkingDay(lastWorkingDay);
        user.setColor(color);
        user.setCreated(created);
        user.setUpdated(updated);
        return user;
    }

    @BeforeEach
    void setUp() {
        List<User> raw = new ArrayList<>();

        // id=1  John Doe        active  started 2020-03-15  (5+ years at now)
        raw.add(createUser(1L, "John Doe", "john.doe@company.com",
                LocalDate.of(2020, 3, 15), null, Color.BLUE,
                OffsetDateTime.of(2020, 3, 1, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 1, 10, 14, 30, 0, 0, ZoneOffset.UTC)));

        // id=2  Jane Smith      active  started 2019-06-01  (6+ years at now)
        raw.add(createUser(2L, "Jane Smith", "jane.smith@company.com",
                LocalDate.of(2019, 6, 1), null, Color.RED,
                OffsetDateTime.of(2019, 5, 15, 11, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 2, 5, 16, 45, 0, 0, ZoneOffset.UTC)));

        // id=3  Bob Johnson     former  started 2021-01-10  left 2024-06-30
        raw.add(createUser(3L, "Bob Johnson", "bob.johnson@company.com",
                LocalDate.of(2021, 1, 10), LocalDate.of(2024, 6, 30), Color.GREEN,
                OffsetDateTime.of(2020, 12, 20, 8, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 3, 16, 13, 20, 0, 0, ZoneOffset.UTC)));

        // id=4  Alice Wilson    active  started 2022-09-05  (2+ years at now — NOT > 3 years)
        raw.add(createUser(4L, "Alice Wilson", "alice.wilson@company.com",
                LocalDate.of(2022, 9, 5), null, Color.ORANGE,
                OffsetDateTime.of(2022, 8, 25, 15, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 4, 15, 12, 10, 0, 0, ZoneOffset.UTC)));

        // id=5  Mike Brown      active  started 2018-11-20  (6+ years at now)
        raw.add(createUser(5L, "Mike Brown", "mike.brown@company.com",
                LocalDate.of(2018, 11, 20), null, Color.MAGENTA,
                OffsetDateTime.of(2018, 11, 1, 14, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 5, 1, 11, 40, 0, 0, ZoneOffset.UTC)));

        // id=6  Sarah Davis     active  started 2023-02-14  (2+ years at now — NOT > 3 years)
        raw.add(createUser(6L, "Sarah Davis", "sarah.davis@company.com",
                LocalDate.of(2023, 2, 14), null, Color.PINK,
                OffsetDateTime.of(2023, 1, 28, 9, 20, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 6, 10, 16, 15, 0, 0, ZoneOffset.UTC)));

        // id=7  David Martinez  former  started 2017-04-03  left 2023-12-15
        raw.add(createUser(7L, "David Martinez", "david.martinez@company.com",
                LocalDate.of(2017, 4, 3), LocalDate.of(2023, 12, 15), Color.CYAN,
                OffsetDateTime.of(2017, 3, 25, 12, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 7, 29, 15, 50, 0, 0, ZoneOffset.UTC)));

        // id=8  Lisa Anderson   active  started 2021-08-16  (3+ years at now — exactly > 3y)
        raw.add(createUser(8L, "Lisa Anderson", "lisa.anderson@company.com",
                LocalDate.of(2021, 8, 16), null, Color.MAGENTA,
                OffsetDateTime.of(2021, 7, 30, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 8, 15, 17, 30, 0, 0, ZoneOffset.UTC)));

        // id=9  Robert Taylor   active  started 2024-01-08  (1+ year at now)
        raw.add(createUser(9L, "Robert Taylor", "robert.taylor@company.com",
                LocalDate.of(2024, 1, 8), null, Color.YELLOW,
                OffsetDateTime.of(2023, 12, 28, 13, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 9, 5, 9, 20, 0, 0, ZoneOffset.UTC)));

        // id=10 Emily Clark     active  started 2020-10-12  (4+ years at now)
        raw.add(createUser(10L, "Emily Clark", "emily.clark@company.com",
                LocalDate.of(2020, 10, 12), null, Color.GRAY,
                OffsetDateTime.of(2020, 9, 25, 10, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 10, 22, 14, 45, 0, 0, ZoneOffset.UTC)));

        // id=11 James White     active  started 2019-12-02  (5+ years at now)
        raw.add(createUser(11L, "James White", "james.white@company.com",
                LocalDate.of(2019, 12, 2), null, Color.BLACK,
                OffsetDateTime.of(2019, 11, 28, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 10, 16, 30, 0, 0, ZoneOffset.UTC)));

        // id=12 Maria Garcia    active  started 2025-03-01  (< 6 months at now)
        raw.add(createUser(12L, "Maria Garcia", "maria.garcia@company.com",
                LocalDate.of(2025, 3, 1), null, Color.LIGHT_GRAY,
                OffsetDateTime.of(2025, 2, 15, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 3, 10, 12, 20, 0, 0, ZoneOffset.UTC)));

        testProducts = new ArrayList<>();
        for (User u : raw) {
            testProducts.add(UserFilterDto.from(u));
        }
    }

    // -------------------------------------------------------------------------
    // Tests — each has an unambiguous natural-language query and a hand-written
    // reference JS predicate that is the ground truth for that query.
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("email contains @company.com")
    void testAtCompanyCom() throws Exception {
        assertSearchMatchesReference(
                "email contains @company.com",
                "User",
                "return entity.getEmail().toLowerCase().includes('@company.com');"
        );
    }

    // --- name / email filters ------------------------------------------------

    @Test
    @DisplayName("created in 2025")
    void testCreatedIn2025() throws Exception {
        assertSearchMatchesReference(
                "created in 2025",
                "User",
                "return entity.getCreated().getYear() === 2025;"
        );
    }

    @Test
    @DisplayName("email contains alice")
    void testEmailContainsAlice() throws Exception {
        assertSearchMatchesReference(
                "email contains alice",
                "User",
                "return entity.getEmail().toLowerCase().includes('alice');"
        );
    }

    @Test
    @DisplayName("employees hired before January 1 2022")
    void testEmployeesHiredBeforeJanuary1_2022() throws Exception {
        assertSearchMatchesReference(
                "employees hired before January 1 2022",
                "User",
                """
                        const boundary = Java.type('java.time.LocalDate').of(2022, 1, 1);
                        return entity.getFirstWorkingDay().isBefore(boundary);
                        """
        );
    }

    @Test
    @DisplayName("employees hired more than 3 years ago")
    void testEmployeesHiredMoreThan3YearsAgo() throws Exception {
        // now=2025-08-10 → boundary=2022-08-10; firstWorkingDay must be strictly before that
        assertSearchMatchesReference(
                "employees hired more than 3 years ago",
                "User",
                "return entity.getFirstWorkingDay().isBefore(entity.getNow().minusYears(3));"
        );
    }

    // --- employment status filters -------------------------------------------

    @Test
    @DisplayName("employees started within last 6 months")
    void testEmployeesStartedWithinLast6Months() throws Exception {
        // now=2025-08-10 → boundary=2025-02-10; firstWorkingDay must be after that
        assertSearchMatchesReference(
                "employees started within last 6 months",
                "User",
                "return entity.getFirstWorkingDay().isAfter(entity.getNow().minusMonths(6));"
        );
    }

    @Test
    @DisplayName("empty search query")
    void testEmptySearchQuery() throws Exception {
        assertSearchMatchesReference(
                "",
                "User",
                "return true;"
        );
    }

    @Test
    @DisplayName("firstWorkingDay in 2018")
    void testFirstWorkingDayIn2018() throws Exception {
        assertSearchMatchesReference(
                "firstWorkingDay in 2018",
                "User",
                "return entity.getFirstWorkingDay().getYear() === 2018;"
        );
    }

    // --- firstWorkingDay filters ---------------------------------------------

    @Test
    @DisplayName("former employees")
    void testFormerEmployees() throws Exception {
        assertSearchMatchesReference(
                "former employees",
                "User",
                "return entity.getLastWorkingDay() !== null;"
        );
    }

    @Test
    @DisplayName("John")
    void testJohn() throws Exception {
        // "John" appears in both "John Doe" and "Bob Johnson"
        assertSearchMatchesReference(
                "John",
                "User",
                "return entity.getName().toLowerCase().includes('john');"
        );
    }

    @Test
    @DisplayName("lastWorkingDay is not null")
    void testLastWorkingDayIsNotNull() throws Exception {
        assertSearchMatchesReference(
                "lastWorkingDay is not null",
                "User",
                "return entity.getLastWorkingDay() !== null;"
        );
    }

    @Test
    @DisplayName("name contains Smith")
    void testNameContainsSmith() throws Exception {
        assertSearchMatchesReference(
                "name contains Smith",
                "User",
                "return entity.getName().toLowerCase().includes('smith');"
        );
    }

    @Test
    @DisplayName("updated in 2025")
    void testUpdatedIn2025() throws Exception {
        assertSearchMatchesReference(
                "updated in 2025",
                "User",
                "return entity.getUpdated().getYear() === 2025;"
        );

    }

    @Test
    @DisplayName("users ending employment in 2024")
    void testUsersEndingEmploymentIn2024() throws Exception {
        assertSearchMatchesReference(
                "users ending employment in 2024",
                "User",
                "return entity.getLastWorkingDay() !== null && entity.getLastWorkingDay().getYear() === 2024;"
        );
    }

    // --- tenure filters (use entity.getNow() so the reference JS is consistent
    //     with whatever 'now' the test injects — 2025-08-10) -------------------

    @Test
    @DisplayName("users starting after December 31 2020")
    void testUsersStartingAfterDecember31_2020() throws Exception {
        // Explicit date removes ambiguity: strictly after 2020-12-31 means firstWorkingDay >= 2021-01-01
        assertSearchMatchesReference(
                "users starting after December 31 2020",
                "User",
                """
                        const boundary = Java.type('java.time.LocalDate').of(2020, 12, 31);
                        return entity.getFirstWorkingDay().isAfter(boundary);
                        """
        );
    }

    @Test
    @DisplayName("users starting before January 1 2020")
    void testUsersStartingBeforeJanuary1_2020() throws Exception {
        // Explicit date removes ambiguity: strictly before 2020-01-01 means firstWorkingDay <= 2019-12-31
        assertSearchMatchesReference(
                "users starting before January 1 2020",
                "User",
                """
                        const boundary = Java.type('java.time.LocalDate').of(2020, 1, 1);
                        return entity.getFirstWorkingDay().isBefore(boundary);
                        """
        );
    }

    // --- created / updated filters -------------------------------------------

    @Test
    @DisplayName("users starting in 2021")
    void testUsersStartingIn2021() throws Exception {
        assertSearchMatchesReference(
                "users starting in 2021",
                "User",
                "return entity.getFirstWorkingDay().getYear() === 2021;"
        );
    }

    @Test
    @DisplayName("users starting in 2024")
    void testUsersStartingIn2024() throws Exception {
        assertSearchMatchesReference(
                "users starting in 2024",
                "User",
                "return entity.getFirstWorkingDay().getYear() === 2024;"
        );
    }
}
