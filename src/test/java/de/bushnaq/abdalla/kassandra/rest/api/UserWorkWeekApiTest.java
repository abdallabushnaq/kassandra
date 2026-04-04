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

package de.bushnaq.abdalla.kassandra.rest.api;

import de.bushnaq.abdalla.kassandra.config.DefaultEntitiesInitializer;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.UserWorkWeek;
import de.bushnaq.abdalla.kassandra.dto.WorkWeek;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractUiTestUtil;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerErrorException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * API tests for {@link de.bushnaq.abdalla.kassandra.rest.controller.UserWorkWeekController}.
 * Mirrors the structure of {@link LocationApiTest}.
 */
@Tag("UnitTest")
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
public class UserWorkWeekApiTest extends AbstractUiTestUtil {

    private static final long   FAKE_ID           = 999999L;
    private static final String FIRST_START_DATE  = "2024-03-14";
    private static final String SECOND_START_DATE = "2025-07-01";
    /**
     * The second work-week used when adding or switching an assignment.
     */
    private static final String SECOND_WORK_WEEK  = DefaultEntitiesInitializer.WORK_WEEK_ISLAMIC_5X8;

    private User admin1;
    private User user1;
    private User user2;
    private User user3;

    // -----------------------------------------------------------------------
    // Test cases
    // -----------------------------------------------------------------------

    /**
     * Creates a user (which automatically receives the default Western 5×8 work week),
     * verifies the start date, then adds a second work-week assignment and checks it.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void add(RandomCase randomCase, TestInfo testInfo) throws Exception {
        // Create user with default work week (Western 5x8)
        {
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
        }

        // Verify the first work week was persisted with the correct start date
        {
            User user = expectedUsers.getFirst();
            assertEquals(LocalDate.parse(FIRST_START_DATE), user.getUserWorkWeeks().getFirst().getStart());
        }

        // Add a second work-week assignment (Islamic Sun-Thu 5×8) effective on SECOND_START_DATE
        {
            User user = expectedUsers.getFirst();
            WorkWeek islamicWorkWeek = workWeekApi.getAll().stream()
                    .filter(ww -> SECOND_WORK_WEEK.equals(ww.getName()))
                    .findFirst().orElseThrow();
            addWorkWeek(user, islamicWorkWeek, LocalDate.parse(SECOND_START_DATE));
        }

        // Verify the second assignment
        {
            User user = expectedUsers.getFirst();
            assertEquals(LocalDate.parse(SECOND_START_DATE), user.getUserWorkWeeks().get(1).getStart());
        }
    }

    /**
     * Verifies that all mutating operations (add, update, delete) fail with
     * {@link AuthenticationCredentialsNotFoundException} when called without authentication.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void anonymousSecurity(RandomCase randomCase, TestInfo testInfo) throws Exception {
        // Set up data as admin; fetch the second work week while still authenticated
        final WorkWeek secondWorkWeek;
        {
            setUser("admin-user", "ROLE_ADMIN");
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
            // Add a second assignment so we have something to attempt to delete later
            secondWorkWeek = workWeekApi.getAll().stream()
                    .filter(ww -> SECOND_WORK_WEEK.equals(ww.getName()))
                    .findFirst().orElseThrow();
            addWorkWeek(user, secondWorkWeek, LocalDate.parse(SECOND_START_DATE));
            SecurityContextHolder.clearContext();
        }

        // Try to add a third assignment as anonymous – must fail
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            User user = expectedUsers.getFirst();
            addWorkWeek(user, secondWorkWeek, LocalDate.parse("2026-01-01"));
        });

        // Try to update as anonymous – must fail
        {
            User         user          = expectedUsers.getFirst();
            UserWorkWeek uww           = user.getUserWorkWeeks().getFirst();
            LocalDate    originalStart = uww.getStart();
            uww.setStart(LocalDate.parse(SECOND_START_DATE));
            try {
                updateWorkWeek(uww, user);
                fail("should not be able to update");
            } catch (AuthenticationCredentialsNotFoundException e) {
                // Restore field so the @AfterEach state checks still pass
                uww.setStart(originalStart);
            }
        }

        // Try to delete as anonymous – must fail
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            User         user = expectedUsers.getFirst();
            UserWorkWeek uww  = user.getUserWorkWeeks().get(1);
            removeWorkWeek(uww, user);
        });
    }

    /**
     * Attempts to delete the work-week assignment with the earliest start date (the "first" one),
     * which the controller must reject.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteFirstWorkWeek(RandomCase randomCase, TestInfo testInfo) throws Exception {
        {
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
        }

        // Attempting to delete the first (and only) work-week assignment must fail
        {
            User user = expectedUsers.getFirst();
            try {
                userWorkWeekApi.deleteById(user.getId(), user.getUserWorkWeeks().getFirst().getId());
                fail("should not be able to delete the first work-week assignment");
            } catch (ServerErrorException e) {
                // expected
            }
        }
    }

    /**
     * Adds a second work-week assignment then deletes it successfully.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteSecondWorkWeek(RandomCase randomCase, TestInfo testInfo) throws Exception {
        {
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
        }

        // Add a second assignment
        {
            User user = expectedUsers.getFirst();
            WorkWeek islamicWorkWeek = workWeekApi.getAll().stream()
                    .filter(ww -> SECOND_WORK_WEEK.equals(ww.getName()))
                    .findFirst().orElseThrow();
            addWorkWeek(user, islamicWorkWeek, LocalDate.parse(SECOND_START_DATE));
        }

        // Delete the second assignment – must succeed
        {
            User         user = expectedUsers.getFirst();
            UserWorkWeek uww  = user.getUserWorkWeeks().get(1);
            removeWorkWeek(uww, user);
        }
    }

    /**
     * Attempts to delete using a non-existing assignment ID; expects {@link ServerErrorException}.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteUsingFakeId(RandomCase randomCase, TestInfo testInfo) throws Exception {
        {
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
        }

        // Add a second assignment so there is a deletable one
        {
            User user = expectedUsers.getFirst();
            WorkWeek islamicWorkWeek = workWeekApi.getAll().stream()
                    .filter(ww -> SECOND_WORK_WEEK.equals(ww.getName()))
                    .findFirst().orElseThrow();
            addWorkWeek(user, islamicWorkWeek, LocalDate.parse(SECOND_START_DATE));
        }

        // Try to delete the second assignment using a fake assignment ID
        {
            User         user   = expectedUsers.getFirst();
            UserWorkWeek uww    = user.getUserWorkWeeks().get(1);
            Long         realId = uww.getId();
            uww.setId(FAKE_ID);
            try {
                userWorkWeekApi.deleteById(user.getId(), uww.getId());
                fail("should not be able to delete with fake id");
            } catch (ServerErrorException e) {
                // expected
                uww.setId(realId);
            }
        }
    }

    /**
     * Attempts to delete using a non-existing user ID; expects {@link ResponseStatusException}.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteUsingFakeUserId(RandomCase randomCase, TestInfo testInfo) throws Exception {
        {
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
        }

        // Add a second assignment so there is a deletable one
        {
            User user = expectedUsers.getFirst();
            WorkWeek islamicWorkWeek = workWeekApi.getAll().stream()
                    .filter(ww -> SECOND_WORK_WEEK.equals(ww.getName()))
                    .findFirst().orElseThrow();
            addWorkWeek(user, islamicWorkWeek, LocalDate.parse(SECOND_START_DATE));
        }

        // Try to delete using a fake user ID
        {
            User user       = expectedUsers.getFirst();
            Long realUserId = user.getId();
            user.setId(FAKE_ID);
            try {
                userWorkWeekApi.deleteById(user.getId(), user.getUserWorkWeeks().get(1).getId());
                fail("should not be able to delete with fake user id");
            } catch (ResponseStatusException e) {
                // expected
                user.setId(realUserId);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Initialisation helper used by the security tests
    // -----------------------------------------------------------------------

    private void init(RandomCase randomCase, TestInfo testInfo) throws Exception {
        Authentication roleAdmin = setUser("admin-user", "ROLE_ADMIN");
        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateProductsIfNeeded(testInfo, randomCase);
        admin1 = userApi.getByEmail("christopher.paul@kassandra.org").get();
        user1  = userApi.getByEmail("kristen.hubbell@kassandra.org").get();
        user2  = userApi.getByEmail("claudine.fick@kassandra.org").get();
        user3  = userApi.getByEmail("randy.asmus@kassandra.org").get();

        setUser(roleAdmin);
    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(1, OffsetDateTime.parse("2025-08-11T08:00:00+01:00"), LocalDate.parse("2025-08-04"), Duration.ofDays(10), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 13)//
        };
        return Arrays.stream(randomCases).toList();
    }

    /**
     * Attempts to update using a non-existing assignment ID; expects {@link ServerErrorException}.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void updateUsingFakeId(RandomCase randomCase, TestInfo testInfo) throws Exception {
        {
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
        }

        // Verify the assignment exists
        {
            User user = expectedUsers.getFirst();
            assertEquals(LocalDate.parse(FIRST_START_DATE), user.getUserWorkWeeks().getFirst().getStart());
        }

        // Attempt to update using a fake assignment ID
        {
            User         user      = expectedUsers.getFirst();
            UserWorkWeek uww       = user.getUserWorkWeeks().getFirst();
            Long         realId    = uww.getId();
            LocalDate    realStart = uww.getStart();
            uww.setId(FAKE_ID);
            uww.setStart(LocalDate.parse(SECOND_START_DATE));
            try {
                updateWorkWeek(uww, user);
                fail("should not be able to update with fake id");
            } catch (ServerErrorException e) {
                // expected
                uww.setStart(realStart);
                uww.setId(realId);
            }
        }
    }

    /**
     * Attempts to update using a non-existing user ID; expects {@link ResponseStatusException}.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void updateUsingFakeUserId(RandomCase randomCase, TestInfo testInfo) throws Exception {
        {
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
        }

        // Verify the assignment exists
        {
            User user = expectedUsers.getFirst();
            assertEquals(LocalDate.parse(FIRST_START_DATE), user.getUserWorkWeeks().getFirst().getStart());
        }

        Thread.sleep(1000); // ensure updated timestamp differs

        // Attempt to update using a fake user ID
        {
            User user       = expectedUsers.getFirst();
            Long realUserId = user.getId();
            user.setId(FAKE_ID);
            UserWorkWeek uww       = user.getUserWorkWeeks().getFirst();
            LocalDate    realStart = uww.getStart();
            uww.setStart(LocalDate.parse(SECOND_START_DATE));
            try {
                updateWorkWeek(uww, user);
                fail("should not be able to update with fake user id");
            } catch (ResponseStatusException e) {
                // expected
                uww.setStart(realStart);
                user.setId(realUserId);
            }
        }
    }

    /**
     * Changes the start date and work-week reference of the first assignment, then updates it.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void updateWorkWeek(RandomCase randomCase, TestInfo testInfo) throws Exception {
        {
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
        }

        // Verify that the first assignment has the expected start date
        {
            User user = expectedUsers.getFirst();
            assertEquals(LocalDate.parse(FIRST_START_DATE), user.getUserWorkWeeks().getFirst().getStart());
        }

        Thread.sleep(1000); // ensure updated timestamp differs

        // Switch to a different work week and change the start date
        {
            User user = expectedUsers.getFirst();
            WorkWeek islamicWorkWeek = workWeekApi.getAll().stream()
                    .filter(ww -> SECOND_WORK_WEEK.equals(ww.getName()))
                    .findFirst().orElseThrow();
            UserWorkWeek uww = user.getUserWorkWeeks().getFirst();
            uww.setWorkWeek(islamicWorkWeek);
            uww.setStart(LocalDate.parse(SECOND_START_DATE));
            updateWorkWeek(uww, user);
        }
    }

    /**
     * Verifies that a regular user cannot add, update, or delete another user's
     * work-week assignments.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void userSecurity(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        setUser(user1.getEmail(), "ROLE_USER");

        // user1 must not be able to add a work-week assignment for user2
        assertThrows(AccessDeniedException.class, () -> {
            WorkWeek islamicWorkWeek = workWeekApi.getAll().stream()
                    .filter(ww -> SECOND_WORK_WEEK.equals(ww.getName()))
                    .findFirst().orElseThrow();
            addWorkWeek(user2, islamicWorkWeek, LocalDate.parse(SECOND_START_DATE));
        });

        // user1 must not be able to update user2's first work-week assignment
        {
            UserWorkWeek uww           = user2.getUserWorkWeeks().getFirst();
            LocalDate    originalStart = uww.getStart();
            try {
                uww.setStart(LocalDate.parse(SECOND_START_DATE));
                updateWorkWeek(uww, user2);
                fail("Should not be able to update another user's work-week");
            } catch (AccessDeniedException e) {
                // Restore original value so @AfterEach state remains consistent
                uww.setStart(originalStart);
            }
        }

        // user1 must not be able to delete user2's first work-week assignment
        assertThrows(AccessDeniedException.class, () -> {
            UserWorkWeek uww = user2.getUserWorkWeeks().getFirst();
            removeWorkWeek(uww, user2);
        });
    }
}

