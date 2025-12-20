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
import de.bushnaq.abdalla.kassandra.util.AbstractEntityGenerator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerErrorException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("UnitTest")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class AvailabilityApiTest extends AbstractEntityGenerator {
    private static final long   FAKE_ID             = 999999L;
    private static final String FIRST_START_DATE    = "2024-03-14";
    private static final float  SECOND_AVAILABILITY = 0.6f;
    private static final String SECOND_START_DATE   = "2025-07-01";

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void addAvailability() throws Exception {
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

    @Test
    public void anonymousSecurity() {
        {
            setUser("admin-user", "ROLE_ADMIN");
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
            SecurityContextHolder.clearContext();
        }

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            User user = expectedUsers.getFirst();
            addAvailability(user, SECOND_AVAILABILITY, LocalDate.parse(SECOND_START_DATE));
        });

        {
            User         user                 = expectedUsers.getFirst();
            Availability availability         = user.getAvailabilities().getFirst();
            float        originalAvailability = availability.getAvailability();
            availability.setAvailability(SECOND_AVAILABILITY);
            try {
                updateAvailability(availability, user);
                fail("should not be able to update");
            } catch (AuthenticationCredentialsNotFoundException e) {
                //restore fields to match db for later tests in @AfterEach
                availability.setAvailability(originalAvailability);
            }
        }
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            Availability availability = availabilityApi.getById(expectedAvailabilities.getFirst().getId());

        });

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            User         user         = expectedUsers.getFirst();
            Availability availability = user.getAvailabilities().getFirst();
            removeAvailability(availability, user);
        });
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteFirstAvailability() throws Exception {
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

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteSecondAvailability() throws Exception {
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

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteUsingFakeId() throws Exception {
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

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteUsingFakeUserId() throws Exception {
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

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void updateAvailability() throws Exception {
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

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void updateUsingFakeAvailabilityId() throws Exception {
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

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void updateUsingFakeUserId() throws Exception {
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

    @Test
    public void userSecurity() {
        {
            setUser("admin-user", "ROLE_ADMIN");
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
            setUser("user", "ROLE_USER");
        }

        assertThrows(AccessDeniedException.class, () -> {
            User user = expectedUsers.getFirst();
            addAvailability(user, SECOND_AVAILABILITY, LocalDate.parse(SECOND_START_DATE));
        });

        {
            User         user                 = expectedUsers.getFirst();
            Availability availability         = user.getAvailabilities().getFirst();
            float        originalAvailability = availability.getAvailability();
            try {
                availability.setAvailability(SECOND_AVAILABILITY);
                updateAvailability(availability, user);
                fail("Should not be able to update availability");
            } catch (AccessDeniedException e) {
                // Restore original values
                availability.setAvailability(originalAvailability);
            }
        }

        assertThrows(AccessDeniedException.class, () -> {
            User         user         = expectedUsers.getFirst();
            Availability availability = user.getAvailabilities().getFirst();
            removeAvailability(availability, user);
        });
    }
}