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

import de.bushnaq.abdalla.kassandra.dto.Sprint;
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
import org.springframework.web.server.ServerErrorException;

import java.util.List;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.*;

@Tag("UnitTest")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class SprintApiTest extends AbstractEntityGenerator {
    private static final long   FAKE_ID     = 999999L;
    private static final String SECOND_NAME = "SECOND_NAME";

    @Test
    public void anonymousSecurity() {
        {
            setUser("admin-user", "ROLE_ADMIN");
            addRandomProducts(1);
            SecurityContextHolder.clearContext();
        }

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            List<Sprint> allSprints = sprintApi.getAll();
        });

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            Sprint sprint = sprintApi.getById(expectedSprints.getFirst().getId());
        });

        {
            Sprint sprint = expectedSprints.getFirst();
            String name   = sprint.getName();
            try {
                sprint.setName(SECOND_NAME);
                updateSprint(sprint);
                fail("should not be able to update");
            } catch (AuthenticationCredentialsNotFoundException e) {
                //restore fields to match db for later tests in @AfterEach
                sprint.setName(name);
            }
        }

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            removeSprint(expectedSprints.getFirst().getId());
        });
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void create() throws Exception {
        addRandomProducts(1);
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void createDuplicateNameFails() throws Exception {
        // Create product, version, and feature first
        addRandomProducts(1);

        // Create first sprint
        Sprint sprint1 = addSprint(expectedFeatures.get(0), "Sprint1");

        try {
            // Try to create a second sprint with the same name
            Sprint sprint2 = addSprint(expectedFeatures.get(0), "Sprint1");
            fail("Should not be able to create a sprint with duplicate name");
        } catch (Exception e) {
            // Expected exception for duplicate name
            assertTrue(e.getMessage().contains("CONFLICT") || e.getMessage().contains("already exists"));
        }
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void delete() throws Exception {
        // Create products with sprints
        addRandomProducts(2);
        // Delete the first sprint
        removeSprint(expectedSprints.getFirst().getId());
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteUsingFakeId() throws Exception {
        addRandomProducts(2);
        try {
            removeSprint(FAKE_ID);
        } catch (ServerErrorException e) {
            // Expected exception
        }
    }

    @Test
    public void getAll() throws Exception {
        {
            setUser("admin-user", "ROLE_ADMIN");
            addRandomProducts(3);
            setUser("user", "ROLE_USER");
        }
        List<Sprint> allSprints = sprintApi.getAll();
        assertEquals(3, allSprints.size());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getAllEmpty() throws Exception {
        List<Sprint> allSprints = sprintApi.getAll();
        assertEquals(0, allSprints.size());
    }

    @Test
    public void getByFakeId() throws Exception {
        {
            setUser("admin-user", "ROLE_ADMIN");
            addRandomProducts(1);
            setUser("user", "ROLE_USER");
        }
        try {
            sprintApi.getById(FAKE_ID);
            fail("Sprint should not exist");
        } catch (ServerErrorException e) {
            // Expected exception
        }
    }

    @Test
    public void getById() throws Exception {
        {
            setUser("admin-user", "ROLE_ADMIN");
            addRandomProducts(1);
            setUser("user", "ROLE_USER");
        }
        Sprint sprint = sprintApi.getById(expectedSprints.getFirst().getId());
        assertSprintEquals(expectedSprints.getFirst(), sprint, true);
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void update() throws Exception {
        addRandomProducts(2);
        Sprint sprint = expectedSprints.getFirst();
        sprint.setName(SECOND_NAME);
        updateSprint(sprint);
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void updateToDuplicateNameFails() throws Exception {
        // Create product, version, and feature first
        addRandomProducts(1);

        // Create two sprints
        Sprint sprint1 = addSprint(expectedFeatures.get(0), "Sprint1");
        Sprint sprint2 = addSprint(expectedFeatures.get(0), "Sprint2");

        // Try to update sprint2 to have the same name as sprint1
        String originalName = sprint2.getName();
        sprint2.setName(sprint1.getName());

        try {
            updateSprint(sprint2);
            fail("Should not be able to update a sprint to have a duplicate name");
        } catch (Exception e) {
            // Expected exception for duplicate name
            assertTrue(e.getMessage().contains("CONFLICT") || e.getMessage().contains("already exists"));
        }

        // Restore original name for cleanup
        sprint2.setName(originalName);
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void updateUsingFakeId() throws Exception {
        addRandomProducts(2);
        Sprint sprint = expectedSprints.getFirst();
        Long   id     = sprint.getId();
        String name   = sprint.getName();
        sprint.setId(FAKE_ID);
        sprint.setName(SECOND_NAME);
        try {
            updateSprint(sprint);
            fail("should not be able to update");
        } catch (ServerErrorException e) {
            // Expected exception
            sprint.setId(id);
            sprint.setName(name);
        }
    }

    @Test
    public void userSecurity() {
        {
            setUser("admin-user", "ROLE_ADMIN");
            addRandomProducts(1);
            setUser("user", "ROLE_USER");
        }

        // Regular users should be able to view sprints
        List<Sprint> allSprints = sprintApi.getAll();
        Sprint       sprint     = sprintApi.getById(expectedSprints.getFirst().getId());

        // But not modify them
        {
            Sprint sprintToModify = expectedSprints.getFirst();
            String originalName   = sprintToModify.getName();
            try {
                sprintToModify.setName(SECOND_NAME);
                updateSprint(sprintToModify);
                fail("Should not be able to update sprint");
            } catch (AccessDeniedException e) {
                // Expected exception
                // Restore original name to prevent test failures
                sprintToModify.setName(originalName);
            }
        }

        assertThrows(AccessDeniedException.class, () -> {
            removeSprint(expectedSprints.getFirst().getId());
        });
    }
}
