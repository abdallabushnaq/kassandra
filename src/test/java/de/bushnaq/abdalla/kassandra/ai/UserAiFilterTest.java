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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reviewed by: Abdalla Bushnaq
 * Integration test for User AI filtering testing real LLM regex pattern generation
 * and filtering capabilities with various search scenarios.
 * <p>
 * This test requires Ollama to be running with a model available (e.g., llama3.2:1b).
 * The test will be skipped if Ollama is not available.
 */
@Tag("AiUnitTest")
@SpringBootTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class UserAiFilterTest extends AbstractAiFilterTest<User> {

    public UserAiFilterTest(JsonMapper mapper, AiFilterService aiFilterService) {
        super(mapper, aiFilterService, LocalDate.of(2025, 8, 10));
    }

    private User createUser(Long id, String name, String email, LocalDate firstWorkingDay, LocalDate lastWorkingDay,
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
        // Create test data
        setupTestUsers();
    }

    private void setupTestUsers() {
        testProducts = new ArrayList<>();

        // Users with different names, roles, departments, and employment periods for comprehensive testing
        testProducts.add(createUser(1L, "John Doe", "john.doe@company.com",
                LocalDate.of(2020, 3, 15), null, Color.BLUE,
                OffsetDateTime.of(2020, 3, 1, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 1, 10, 14, 30, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createUser(2L, "Jane Smith", "jane.smith@company.com",
                LocalDate.of(2019, 6, 1), null, Color.RED,
                OffsetDateTime.of(2019, 5, 15, 11, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 2, 5, 16, 45, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createUser(3L, "Bob Johnson", "bob.johnson@company.com",
                LocalDate.of(2021, 1, 10), LocalDate.of(2024, 6, 30), Color.GREEN,
                OffsetDateTime.of(2020, 12, 20, 8, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 3, 16, 13, 20, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createUser(4L, "Alice Wilson", "alice.wilson@company.com",
                LocalDate.of(2022, 9, 5), null, Color.ORANGE,
                OffsetDateTime.of(2022, 8, 25, 15, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 4, 15, 12, 10, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createUser(5L, "Mike Brown", "mike.brown@company.com",
                LocalDate.of(2018, 11, 20), null, Color.magenta,
                OffsetDateTime.of(2018, 11, 1, 14, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 5, 1, 11, 40, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createUser(6L, "Sarah Davis", "sarah.davis@company.com",
                LocalDate.of(2023, 2, 14), null, Color.PINK,
                OffsetDateTime.of(2023, 1, 28, 9, 20, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 6, 10, 16, 15, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createUser(7L, "David Martinez", "david.martinez@company.com",
                LocalDate.of(2017, 4, 3), LocalDate.of(2023, 12, 15), Color.CYAN,
                OffsetDateTime.of(2017, 3, 25, 12, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 7, 29, 15, 50, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createUser(8L, "Lisa Anderson", "lisa.anderson@company.com",
                LocalDate.of(2021, 8, 16), null, Color.MAGENTA,
                OffsetDateTime.of(2021, 7, 30, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 8, 15, 17, 30, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createUser(9L, "Robert Taylor", "robert.taylor@company.com",
                LocalDate.of(2024, 1, 8), null, Color.YELLOW,
                OffsetDateTime.of(2023, 12, 28, 13, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 9, 5, 9, 20, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createUser(10L, "Emily Clark", "emily.clark@company.com",
                LocalDate.of(2020, 10, 12), null, Color.GRAY,
                OffsetDateTime.of(2020, 9, 25, 10, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 10, 22, 14, 45, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createUser(11L, "James White", "james.white@company.com",
                LocalDate.of(2019, 12, 2), null, Color.BLACK,
                OffsetDateTime.of(2019, 11, 28, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 10, 16, 30, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createUser(12L, "Maria Garcia", "maria.garcia@company.com",
                LocalDate.of(2025, 3, 1), null, Color.LIGHT_GRAY,
                OffsetDateTime.of(2025, 2, 15, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 3, 10, 12, 20, 0, 0, ZoneOffset.UTC)));
    }

    // === OTHER VALUABLE TEST CASES (preserved from original) ===
    @Test
    @DisplayName("active users")
    void testActiveUsers() throws Exception {
        List<User> results = performSearch("active users", "User");
        List<User> expected = Arrays.asList(
                testProducts.get(0), // John Doe (no lastWorkingDay)
                testProducts.get(1), // Jane Smith (no lastWorkingDay)
                testProducts.get(3), // Alice Wilson (no lastWorkingDay)
                testProducts.get(4), // Mike Brown (no lastWorkingDay)
                testProducts.get(5), // Sarah Davis (no lastWorkingDay)
                testProducts.get(7), // Lisa Anderson (no lastWorkingDay)
                testProducts.get(8), // Robert Taylor (no lastWorkingDay)
                testProducts.get(9), // Emily Clark (no lastWorkingDay)
                testProducts.get(10), // James White (no lastWorkingDay)
                testProducts.get(11)  // Maria Garcia (no lastWorkingDay)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("@company.com")
    void testAtCompanyCom() throws Exception {
        List<User> results  = performSearch("@company.com", "User");
        List<User> expected = new ArrayList<>(testProducts); // All users have company.com email

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("company.com employees")
    void testCompanyComEmployees() throws Exception {
        List<User> results  = performSearch("company.com employees", "User");
        List<User> expected = new ArrayList<>(testProducts); // All users work at company.com

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("created in 2025")
    void testCreatedIn2025() throws Exception {
        List<User> results  = performSearch("created in 2025", "User");
        List<User> expected = Collections.singletonList(testProducts.get(11)); // Maria Garcia

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("email contains alice")
    void testEmailContainsAlice() throws Exception {
        List<User> results  = performSearch("email contains alice", "User");
        List<User> expected = Collections.singletonList(testProducts.get(3)); // Alice Wilson

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("employees since over 3 years")
    void testEmployeesSinceOver3Years() throws Exception {
        List<User> results = performSearch("employees since over 3 years", "User");
//        List<User> results = performSearch(new ExampleUserFilter(now));
        List<User> expected = Arrays.asList(
                testProducts.get(0), // John Doe (started 2020-03-15) - 5+ years
                testProducts.get(1), // Jane Smith (started 2019-06-01) - 6+ years
                testProducts.get(2), // Bob Johnson (started 2021-01-10) - 4+ years
                testProducts.get(4), // Mike Brown (started 2018-11-20) - 6+ years
                testProducts.get(6), // David Martinez (started 2017-04-03) - 8+ years
                testProducts.get(7), // Lisa Anderson (started 2021-08-16) - 3+ years (just barely over 3 years)
                testProducts.get(9), // Emily Clark (started 2020-10-12) - 4+ years
                testProducts.get(10) // James White (started 2019-12-02) - 5+ years
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("employees started within last 6 months")
    void testEmployeesStartedWithinLast6Months() throws Exception {
        List<User> results = performSearch("employees started within last 6 months", "User");
        List<User> expected = Collections.singletonList(
                testProducts.get(11) // Maria Garcia (started 2025-03-01) - 5 months ago
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("empty search query")
    void testEmptySearchQuery() throws Exception {
        List<User> results  = performSearch("", "User");
        List<User> expected = new ArrayList<>(testProducts); // All users should match empty query

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("firstWorkingDay in 2018")
    void testFirstWorkingDayIn2018() throws Exception {
        List<User> results  = performSearch("firstWorkingDay in 2018", "User");
        List<User> expected = Collections.singletonList(testProducts.get(4)); // Mike Brown

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("former employees")
    void testFormerEmployees() throws Exception {
        List<User> results = performSearch("former employees", "User");
        List<User> expected = Arrays.asList(
                testProducts.get(2), // Bob Johnson (lastWorkingDay: 2024-06-30)
                testProducts.get(6)  // David Martinez (lastWorkingDay: 2023-12-15)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("John")
    void testJohn() throws Exception {
        List<User> results = performSearch("John", "User");
        List<User> expected = Arrays.asList(
                testProducts.get(0),// John Doe
                testProducts.get(2) // Bob Johnson
        ); // John Doe

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("lastWorkingDay is not null")
    void testLastWorkingDayIsNotNull() throws Exception {
        List<User> results  = performSearch("lastWorkingDay is not null", "User");
        List<User> expected = Arrays.asList(testProducts.get(2), testProducts.get(6)); // Bob Johnson, David Martinez

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    // === COLUMN-SPECIFIC TESTS (one per column) ===
    @Test
    @DisplayName("name contains Smith")
    void testNameContainsSmith() throws Exception {
        List<User> results  = performSearch("name contains Smith", "User");
        List<User> expected = Collections.singletonList(testProducts.get(1)); // Jane Smith

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("purple elephant dancing")
    void testPurpleElephantDancing() throws Exception {
        List<User> results  = performSearch("purple elephant dancing", "User");
        List<User> expected = Collections.emptyList(); // Should return empty results for nonsensical queries

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("started in 2021")
    void testStartedIn2021() throws Exception {
        List<User> results = performSearch("started in 2021", "User");
        List<User> expected = Arrays.asList(
                testProducts.get(2), // Bob Johnson (started 2021-01-10)
                testProducts.get(7)  // Lisa Anderson (started 2021-08-16)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("updated in 2025")
    void testUpdatedIn2025() throws Exception {
        List<User> results = performSearch("updated in 2025", "User");
        List<User> expected = Arrays.asList(
                testProducts.get(10), // James White (updated 2025-01-10)
                testProducts.get(11)); // Maria Garcia (updated 2025-03-10)

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("users starting after 2020")
    void testUsersStartingAfter2020() throws Exception {
        List<User> results = performSearch("users starting after 2020", "User");
        List<User> expected = Arrays.asList(
                testProducts.get(2), // Bob Johnson (started 2021-01-10)
                testProducts.get(3), // Alice Wilson (started 2022-09-05)
                testProducts.get(5), // Sarah Davis (started 2023-02-14)
                testProducts.get(7), // Lisa Anderson (started 2021-08-16)
                testProducts.get(8), // Robert Taylor (started 2024-01-08)
                testProducts.get(11) // Maria Garcia (started 2025-03-01)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("users starting before 2020")
    void testUsersStartingBefore2020() throws Exception {
        List<User> results = performSearch("users starting before 2020", "User");
        List<User> expected = Arrays.asList(
                testProducts.get(1), // Jane Smith (started 2019-06-01)
                testProducts.get(4), // Mike Brown (started 2018-11-20)
                testProducts.get(6), // David Martinez (started 2017-04-03)
                testProducts.get(10) // James White (started 2019-12-02)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("users starting in 2024")
    void testUsersStartingIn2024() throws Exception {
        List<User> results  = performSearch("users starting in 2024", "User");
        List<User> expected = Collections.singletonList(testProducts.get(8)); // Robert Taylor (started 2024-01-08)

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("users starting in summer")
    void testUsersStartingInSummer() throws Exception {
        List<User> results = performSearch("users starting in summer", "User");
        List<User> expected = Arrays.asList(
                testProducts.get(1), // Jane Smith (started June 1)
                testProducts.get(7)  // Lisa Anderson (started August 16)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("users who left")
    void testUsersWhoLeft() throws Exception {
        List<User> results = performSearch("users who left", "User");
        List<User> expected = Arrays.asList(
                testProducts.get(2), // Bob Johnson (lastWorkingDay: 2024-06-30)
                testProducts.get(6)  // David Martinez (lastWorkingDay: 2023-12-15)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    /**
     * add generated ai code to test against one of the tests.
     */
    class ExampleUserFilter implements Predicate<User> {

        private final LocalDate now;

        ExampleUserFilter(LocalDate now) {
            this.now = now;
        }

        @Override
        public boolean test(User entity) {
            if (entity == null) {
                return false;
            }

            try {
                // Execute the generated filter code
                return entity.getFirstWorkingDay() != null && entity.getFirstWorkingDay().isBefore(now.minusYears(3));
            } catch (Exception e) {
                // Log the error but don't fail the entire filter
                System.err.println("Error in filter execution: " + e.getMessage());
                return false;
            }
        }

    }
}
