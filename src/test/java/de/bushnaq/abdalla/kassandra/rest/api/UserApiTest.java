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
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@Tag("UnitTest")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class UserApiTest extends AbstractEntityGenerator {
    private static final long   FAKE_ID           = 999999L;
    public static final  String FIRST_START_DATE  = "2024-03-14";
    public static final  String SECOND_START_DATE = "2025-07-01";

    @Test
    public void anonymousSecurity() {
        {
            setUser("admin-user", "ROLE_ADMIN");
            addRandomUsers(1);
            SecurityContextHolder.clearContext();
        }

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            addRandomUser(LocalDate.parse(FIRST_START_DATE));
        });

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            List<User> allUsers = userApi.getAll();
        });

        {
            User      user           = expectedUsers.getFirst();
            LocalDate lastWorkingDay = user.getLastWorkingDay();
            try {
                user.setLastWorkingDay(LocalDate.parse(SECOND_START_DATE));
                updateUser(user);
                fail("should not be able to update");
            } catch (AuthenticationCredentialsNotFoundException e) {
                //restore fields to match db for later tests in @AfterEach
                user.setLastWorkingDay(lastWorkingDay);
            }
        }

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            removeUser(expectedUsers.getFirst().getId());
        });

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            User user = userApi.getById(expectedUsers.getFirst().getId());
        });
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void create() throws Exception {
        //create the users
        addRandomUsers(1);

        testUsers();
        printTables();
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void delete() throws Exception {
        //create the users
        addRandomUsers(2);
        removeUser(expectedUsers.getFirst().getId());

    }

    @Test
    public void getAllEmpty() throws Exception {
        //get empty list
        setUser("admin-user", "ROLE_ADMIN");
        List<User> allUsers = userApi.getAll();
        assertEquals(0, allUsers.size());
    }

    @Test
    public void getById() throws Exception {
        {
            setUser("admin-user", "ROLE_ADMIN");
            //create the users
            addRandomUsers(1);
            setUser("user", "ROLE_USER");
        }

        //get user by id
        {
            User user = userApi.getById(expectedUsers.first().getId());
            assertUserEquals(expectedUsers.first(), user);
        }

        testUsers();
        printTables();
    }

    @Test
    public void getByUnknownId() throws Exception {
        {
            setUser("admin-user", "ROLE_ADMIN");
            //create the users
            addRandomUsers(1);
            setUser("user", "ROLE_USER");
        }

        //get by unknown id
        {
            try {
                User user = userApi.getById(FAKE_ID);
                fail("User should not exist");
            } catch (ResponseStatusException e) {
                //expected
            }
        }
        testUsers();
        printTables();
    }

    @Test
    public void search() throws Exception {
        {
            setUser("admin-user", "ROLE_ADMIN");
            //create the users
            addRandomUsers(10);
            setUser("user", "ROLE_USER");
        }

        //get user by id
        {
            List<User> users = userApi.searchByName("ri");
            assertEquals(3, users.size());
        }

        testUsers();
        printTables();
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void update() throws Exception {
        Long id;

        //create the user with australian locale
        {
            User user = addRandomUser(LocalDate.parse(FIRST_START_DATE));
            id = user.getId();
        }

        //test if the location was persisted correctly
        {
            User user = userApi.getById(id);
            assertEquals(LocalDate.parse(FIRST_START_DATE), user.getLocations().getFirst().getStart());
        }

        Thread.sleep(1000);//ensure that update time is different

        //user leaves the company
        {
            User user = userApi.getById(id);
            user.setLastWorkingDay(LocalDate.parse(SECOND_START_DATE));
            updateUser(user);
        }

        testUsers();
        printTables();
    }

    @Test
    public void userSecurity() {
        {
            setUser("admin-user", "ROLE_ADMIN");
            addRandomUsers(1);
            setUser("user", "ROLE_USER");
        }

        assertThrows(AccessDeniedException.class, () -> {
            addRandomUser(LocalDate.parse(FIRST_START_DATE));
        });

        {
            User      user           = expectedUsers.getFirst();
            LocalDate lastWorkingDay = user.getLastWorkingDay();
            try {
                user.setLastWorkingDay(LocalDate.parse(SECOND_START_DATE));
                updateUser(user);
                fail("should not be able to update");
            } catch (AccessDeniedException e) {
                //restore fields to match db for later tests in @AfterEach
                user.setLastWorkingDay(lastWorkingDay);
            }
        }

        assertThrows(AccessDeniedException.class, () -> {
            removeUser(expectedUsers.getFirst().getId());
        });
    }
}