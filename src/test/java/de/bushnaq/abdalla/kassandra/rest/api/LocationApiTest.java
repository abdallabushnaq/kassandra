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

import de.bushnaq.abdalla.kassandra.dto.Location;
import de.bushnaq.abdalla.kassandra.dto.User;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerErrorException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@Tag("UnitTest")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
public class LocationApiTest extends AbstractUiTestUtil {
    private static final long   FAKE_ID           = 999999L;
    private static final String FIRST_START_DATE  = "2024-03-14";
    private static final String SECOND_COUNTRY    = "us";
    private static final String SECOND_START_DATE = "2025-07-01";
    private static final String SECOND_STATE      = "fl";

    private User admin1;
    private User user1;
    private User user2;
    private User user3;

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void add(RandomCase randomCase, TestInfo testInfo) throws Exception {
        //create a user with Australian locale
        {
            Locale.setDefault(new Locale.Builder().setLanguage("en").setRegion("AU").build());//australian locale
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
            Locale.setDefault(Locale.getDefault());
        }

        //test if new location was persisted correctly
        {
            User user = expectedUsers.getFirst();
            assertEquals(LocalDate.parse(FIRST_START_DATE), user.getLocations().getFirst().getStart());
        }

        //add a working location in Germany
        {
            User user = expectedUsers.getFirst();
            //moving to Germany
            addLocation(user, "de", "nw", LocalDate.parse(SECOND_START_DATE));
            userApi.update(user);//persist the new location
        }

        //test the new location
        {
            User user = expectedUsers.getFirst();
            assertEquals(LocalDate.parse(SECOND_START_DATE), user.getLocations().get(1).getStart());
        }
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void anonymousSecurity(RandomCase randomCase, TestInfo testInfo) throws Exception {
        {
            setUser("admin-user", "ROLE_ADMIN");
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
            SecurityContextHolder.clearContext();
        }

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            User user = expectedUsers.getFirst();
            addLocation(user, "de", "nw", LocalDate.parse(SECOND_START_DATE));
        });

        {
            User     user            = expectedUsers.getFirst();
            Location location        = user.getLocations().getFirst();
            String   originalCountry = location.getCountry();
            String   originalState   = location.getState();
            location.setCountry(SECOND_COUNTRY);
            location.setState(SECOND_STATE);
            try {
                updateLocation(location, user);
                fail("should not be able to update");
            } catch (AuthenticationCredentialsNotFoundException e) {
                //restore fields to match db for later tests in @AfterEach
                location.setCountry(originalCountry);
                location.setState(originalState);
            }
        }

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            User     user     = expectedUsers.getFirst();
            Location location = user.getLocations().getFirst();
            removeLocation(location, user);
        });
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteFirstLocation(RandomCase randomCase, TestInfo testInfo) throws Exception {
        {
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
        }

        //try to delete the first location
        {
            User user = expectedUsers.getFirst();
            try {
                locationApi.deleteById(user.getId(), user.getLocations().getFirst().getId());
                fail("should not be able to delete the first location");
            } catch (ServerErrorException e) {
                //expected
            }
        }
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteSecondLocation(RandomCase randomCase, TestInfo testInfo) throws Exception {
        {
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
        }

        //add a working location in Germany
        {
            User user = expectedUsers.getFirst();
            //moving to Germany
            addLocation(user, "de", "nw", LocalDate.parse(SECOND_START_DATE));
            userApi.update(user);//persist the new location
        }

        //try to delete the second location
        {
            User     user     = expectedUsers.getFirst();
            Location location = user.getLocations().get(1);
            removeLocation(location, user);
        }
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteUsingFakeId(RandomCase randomCase, TestInfo testInfo) throws Exception {
        //create a user with australian locale
        {
            Locale.setDefault(new Locale.Builder().setLanguage("en").setRegion("AU").build());//australian locale
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
            Locale.setDefault(Locale.getDefault());
        }

        //add a working location in Germany
        {
            User user = expectedUsers.getFirst();
            //moving to Germany
            addLocation(user, "de", "nw", LocalDate.parse(SECOND_START_DATE));
            userApi.update(user);//persist the new location
        }

        //try to delete using fake location id
        {
            User     user       = expectedUsers.getFirst();
            Location location   = user.getLocations().getFirst();
            Long     locationId = location.getId();
            location.setId(FAKE_ID);
            try {
                locationApi.deleteById(user.getId(), user.getLocations().getFirst().getId());
                fail("should not be able to delete");
            } catch (ServerErrorException e) {
                //expected
                location.setId(locationId);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteUsingFakeUserId(RandomCase randomCase, TestInfo testInfo) throws Exception {
        //create a user with Australian locale
        {
            Locale.setDefault(new Locale.Builder().setLanguage("en").setRegion("AU").build());//australian locale
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
            Locale.setDefault(Locale.getDefault());
        }

        //add a working location in Germany
        {
            User user = expectedUsers.getFirst();
            //moving to Germany
            addLocation(user, "de", "nw", LocalDate.parse(SECOND_START_DATE));
            userApi.update(user);//persist the new location
        }

        //try to delete using fake user id
        {
            User user   = expectedUsers.getFirst();
            Long userId = user.getId();
            user.setId(FAKE_ID);
            try {
                locationApi.deleteById(user.getId(), user.getLocations().getFirst().getId());
                fail("should not be able to delete");
            } catch (ResponseStatusException e) {
                //expected
                user.setId(userId);
            }
        }
    }

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

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void updateLocation(RandomCase randomCase, TestInfo testInfo) throws Exception {
        {
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
        }

        //test if the location was persisted correctly
        {
            User user = expectedUsers.getFirst();
            assertEquals(LocalDate.parse(FIRST_START_DATE), user.getLocations().getFirst().getStart());
        }

        Thread.sleep(1000);//ensure that update time is different

        //fix location mistake
        {
            User     user     = expectedUsers.getFirst();
            Location location = user.getLocations().getFirst();
            location.setCountry(SECOND_COUNTRY);
            location.setState(SECOND_STATE);
            location.setStart(LocalDate.parse(SECOND_START_DATE));
            updateLocation(location, user);
        }
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void updateUsingFakeId(RandomCase randomCase, TestInfo testInfo) throws Exception {

        //create the user with german locale
        {
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
        }

        //test if the location was persisted correctly
        {
            User user = expectedUsers.getFirst();
            assertEquals(LocalDate.parse(FIRST_START_DATE), user.getLocations().getFirst().getStart());
        }

        //update using fake location id
        {
            User     user       = expectedUsers.getFirst();
            Location location   = user.getLocations().getFirst();
            Long     locationId = location.getId();
            String   country    = location.getCountry();
            location.setId(FAKE_ID);
            location.setCountry(SECOND_COUNTRY);
            try {
                updateLocation(location, user);
                fail("should not be able to update");
            } catch (ServerErrorException e) {
                //expected
                location.setCountry(country);
                location.setId(locationId);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void updateUsingFakeUserId(RandomCase randomCase, TestInfo testInfo) throws Exception {

        {
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
        }

        //test if the location was persisted correctly
        {
            User user = expectedUsers.getFirst();
            assertEquals(LocalDate.parse(FIRST_START_DATE), user.getLocations().getFirst().getStart());
        }

        Thread.sleep(1000);//ensure that update time is different

        //update using fake user id
        {
            User user   = expectedUsers.getFirst();
            Long userId = user.getId();
            user.setId(FAKE_ID);
            Location location = user.getLocations().getFirst();
            String   country  = location.getCountry();
            location.setCountry(SECOND_COUNTRY);
            try {
                updateLocation(location, user);
                fail("should not be able to update");
            } catch (ResponseStatusException e) {
                //expected
                location.setCountry(country);
                user.setId(userId);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void userSecurity(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        setUser(user1.getEmail(), "ROLE_USER");

        assertThrows(AccessDeniedException.class, () -> {
            addLocation(user2, "de", "nw", LocalDate.parse(SECOND_START_DATE));
        });

        {
            Location location        = user2.getLocations().getFirst();
            String   originalCountry = location.getCountry();
            String   originalState   = location.getState();
            try {
                location.setCountry(SECOND_COUNTRY);
                location.setState(SECOND_STATE);
                updateLocation(location, user2);
                fail("Should not be able to update location");
            } catch (AccessDeniedException e) {
                // Restore original values
                location.setCountry(originalCountry);
                location.setState(originalState);
            }
        }

        assertThrows(AccessDeniedException.class, () -> {
            Location location = user2.getLocations().getFirst();
            removeLocation(location, user2);
        });
    }
}

