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

package de.bushnaq.abdalla.kassandra.rest.api;

import de.bushnaq.abdalla.kassandra.dto.Availability;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractUiTestUtil;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerErrorException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("UnitTest")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class AvailabilityApiTest extends AbstractUiTestUtil {
    private static final long   FAKE_ID             = 999999L;
    private static final String FIRST_START_DATE    = "2024-03-14";
    private static final float  SECOND_AVAILABILITY = 0.6f;
    private static final String SECOND_START_DATE   = "2025-07-01";

    private User admin1;
    private User user1;
    private User user2;
    private User user3;

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void addAvailability(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        //create a user with australian locale
        {
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
        }

        //add an availability
        {
            User user = expectedUsers.getFirst();
            //moving to Germany
            addAvailability(user, SECOND_AVAILABILITY, LocalDate.parse(SECOND_START_DATE));
        }

        printTables();
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void anonymousSecurity(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            addAvailability(user1, SECOND_AVAILABILITY, LocalDate.parse(SECOND_START_DATE));
        });

        {
            Availability availability         = user1.getAvailabilities().getFirst();
            float        originalAvailability = availability.getAvailability();
            availability.setAvailability(SECOND_AVAILABILITY);
            try {
                updateAvailability(availability, user1);
                fail("should not be able to update");
            } catch (AuthenticationCredentialsNotFoundException e) {
                //restore fields to match db for later tests in @AfterEach
                availability.setAvailability(originalAvailability);
            }
        }
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            Availability availability = availabilityApi.getById(user1.getAvailabilities().getFirst().getId());

        });

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            Availability availability = user1.getAvailabilities().getFirst();
            removeAvailability(availability, user1);
        });
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteFirstAvailability(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        //create a user with australian locale
        {
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
        }

        //try to delete the first location
        {
            User         user         = expectedUsers.getFirst();
            Availability availability = user.getAvailabilities().getFirst();
            try {
                removeAvailability(availability, user);
                fail("should not be able to delete the first availability");
            } catch (ServerErrorException e) {
                //expected
            }
        }
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteSecondAvailability(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        //create a user with australian locale
        {
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
        }

        //add an availability
        {
            User user = expectedUsers.getFirst();
            //moving to Germany
            addAvailability(user, SECOND_AVAILABILITY, LocalDate.parse(SECOND_START_DATE));
        }

        //try to delete the second availability
        {
            User         user         = expectedUsers.getFirst();
            Availability availability = user.getAvailabilities().getLast();
            removeAvailability(availability, user);
        }
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteUsingFakeId(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        //create a user with australian locale
        {
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
        }

        //add an availability
        {
            User user = expectedUsers.getFirst();
            //moving to Germany
            addAvailability(user, SECOND_AVAILABILITY, LocalDate.parse(SECOND_START_DATE));
        }

        //try to delete the second availability with fake availability id
        {
            User         user         = expectedUsers.getFirst();
            Availability availability = user.getAvailabilities().getLast();
            Long         id           = availability.getId();
            availability.setId(FAKE_ID);
            try {
                removeAvailability(availability, user);
                fail("should not be able to delete");
            } catch (ResponseStatusException e) {
                availability.setId(id);
                //expected
            }
        }
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteUsingFakeUserId(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        //create a user with australian locale
        {
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
        }

        //add an availability
        {
            User user = expectedUsers.getFirst();
            //moving to Germany
            addAvailability(user, SECOND_AVAILABILITY, LocalDate.parse(SECOND_START_DATE));
        }

        //try to delete the second availability with fake user id
        {
            User user = expectedUsers.getFirst();
            Long id   = user.getId();
            user.setId(FAKE_ID);
            Availability availability = user.getAvailabilities().getLast();
            try {
                removeAvailability(availability, user);
                fail("should not be able to delete");
            } catch (ResponseStatusException e) {
                user.setId(id);
                //expected
            }
        }
    }

    private void init(RandomCase randomCase, TestInfo testInfo) throws Exception {
        Authentication roleAdmin = setUser("admin-user", "ROLE_ADMIN");
        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateProductsIfNeeded(testInfo, randomCase);
        admin1 = userApi.getByEmail("christopher.paul@kassandra.org");
        user1  = userApi.getByEmail("kristen.hubbell@kassandra.org");
        user2  = userApi.getByEmail("claudine.fick@kassandra.org");
        user3  = userApi.getByEmail("randy.asmus@kassandra.org");

        setUser(roleAdmin);
    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(1, OffsetDateTime.parse("2025-08-11T08:00:00+01:00"), LocalDate.parse("2025-08-04"), Duration.ofDays(10), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 13)//
        };
        return Arrays.stream(randomCases).toList();
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void updateAvailability(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        //create the user with australian locale
        {
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
        }

        //user availability is fixed
        {
            User         user         = expectedUsers.getFirst();
            Availability availability = user.getAvailabilities().getFirst();
            availability.setAvailability(SECOND_AVAILABILITY);
            updateAvailability(availability, user);
        }
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void updateUsingFakeAvailabilityId(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        //create the user with australian locale
        {
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
        }

        //update availability using unknown availability id
        {
            User         user         = expectedUsers.getFirst();
            Availability availability = user.getAvailabilities().getFirst();
            float        a            = availability.getAvailability();
            Long         id           = availability.getId();
            availability.setId(FAKE_ID);
            availability.setAvailability(SECOND_AVAILABILITY);
            try {
                updateAvailability(availability, user);
                fail("should not be able to update");
            } catch (ServerErrorException e) {
                //expected
                availability.setAvailability(a);
                availability.setId(id);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void updateUsingFakeUserId(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        //create the user with australian locale
        {
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
        }

        //update availability using unknown user id
        {
            User user   = expectedUsers.getFirst();
            Long userId = user.getId();
            user.setId(FAKE_ID);
            Availability availability = user.getAvailabilities().getFirst();
            float        a            = availability.getAvailability();
            availability.setAvailability(SECOND_AVAILABILITY);
            try {
                updateAvailability(availability, user);
                fail("should not be able to update");
            } catch (ResponseStatusException e) {
                //expected
                availability.setAvailability(a);
                user.setId(userId);
            }
        }
    }

    //TODO only admin or the user can change user availability
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void userSecurity(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        setUser(user1.getEmail(), "ROLE_USER");

        assertThrows(AccessDeniedException.class, () -> {
            addAvailability(user2, SECOND_AVAILABILITY, LocalDate.parse(SECOND_START_DATE));
        });

        {
            Availability availability         = user2.getAvailabilities().getFirst();
            float        originalAvailability = availability.getAvailability();
            try {
                availability.setAvailability(SECOND_AVAILABILITY);
                updateAvailability(availability, user2);
                fail("Should not be able to update availability");
            } catch (AccessDeniedException e) {
                // Restore original values
                availability.setAvailability(originalAvailability);
            }
        }

        assertThrows(AccessDeniedException.class, () -> {
            Availability availability = user2.getAvailabilities().getFirst();
            removeAvailability(availability, user2);
        });
    }
}