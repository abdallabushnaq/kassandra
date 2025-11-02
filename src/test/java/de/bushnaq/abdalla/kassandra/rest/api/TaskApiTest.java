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

import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.util.AbstractEntityGenerator;
import org.assertj.core.api.Assertions;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;


@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class TaskApiTest extends AbstractEntityGenerator {
    private static final long   FAKE_ID     = 999999L;
    private static final String SECOND_NAME = "SECOND_NAME";

    @Test
    public void anonymousSecurity() {
        {
            setUser("admin-user", "ROLE_ADMIN");

            User    user1   = addRandomUser();
            Product product = addProduct("Product");
            Version version = addVersion(product, "1.0.0");
            Feature feature = addRandomFeature(version);
            Sprint  sprint  = addRandomSprint(feature);
            Task    task    = addTask(sprint, null, "Project Phase 1", LocalDateTime.now(), Duration.ofDays(10), null, null, null);

            SecurityContextHolder.clearContext();
        }

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            Task task = addTask(expectedSprints.getFirst(), null, "Project Phase 1", LocalDateTime.now(), Duration.ofDays(10), null, null, null);
        });
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            List<Task> allTasks = taskApi.getAll();
        });

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            Task task = taskApi.getById(expectedTasks.getFirst().getId());
        });

        {
            Task   task = expectedTasks.getFirst();
            String name = task.getName();
            task.setName(SECOND_NAME);
            try {
                updateTask(task);
                Assertions.fail("should not be able to update");
            } catch (AuthenticationCredentialsNotFoundException e) {
                //restore fields to match db for later tests in @AfterEach
                task.setName(name);
            }
        }

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            removeTaskTree(expectedTasks.getFirst());
        });
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void create() throws Exception {
        User user1 = addRandomUser();

        for (int i = 0; i < 1; i++) {
            Product product = addProduct("Product " + i);
            Version version = addVersion(product, String.format("1.%d.0", i));
            Feature feature = addRandomFeature(version);
            Sprint  sprint  = addRandomSprint(feature);

            Task task1 = addTask(sprint, null, "Project Phase 1", LocalDateTime.now(), Duration.ofDays(10), null, null, null);
            Task task2 = addTask(sprint, task1, "Design", LocalDateTime.now(), Duration.ofDays(4), null, user1, null);
            Task task3 = addTask(sprint, task1, "Implementation", LocalDateTime.now().plusDays(4), null, Duration.ofDays(6), user1, task1);
        }

        printTables();
        testAllAndPrintTables();
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void update() throws Exception {
        User user1 = addRandomUser();

        for (int i = 0; i < 1; i++) {
            Product product = addProduct("Product " + i);
            Version version = addVersion(product, String.format("1.%d.0", i));
            Feature feature = addRandomFeature(version);
            Sprint  sprint  = addRandomSprint(feature);
            Task    task1   = addTask(sprint, null, "Project Phase 1", LocalDateTime.now(), Duration.ofDays(10), null, null, null);
            Task    task2   = addTask(sprint, task1, "Design", LocalDateTime.now(), Duration.ofDays(4), null, user1, null);
            Task    task3   = addTask(sprint, task1, "Implementation", LocalDateTime.now().plusDays(4), Duration.ofDays(6), null, user1, task1);
        }

        testAllAndPrintTables();

        //update
        {
            move(expectedSprints.getFirst(), expectedTasks.get(2), expectedTasks.get(1));
        }

        printTables();
        testAllAndPrintTables();
    }

    @Test
    public void userSecurity() {
        {
            setUser("admin-user", "ROLE_ADMIN");

            User    user1   = addRandomUser();
            Product product = addProduct("Product");
            Version version = addVersion(product, "1.0.0");
            Feature feature = addRandomFeature(version);
            Sprint  sprint  = addRandomSprint(feature);
            Task    task    = addTask(sprint, null, "Project Phase 1", LocalDateTime.now(), Duration.ofDays(10), null, null, null);

            setUser("user", "ROLE_USER");
        }

        // Regular users should be able to view tasks
        List<Task> allTasks = taskApi.getAll();
        Task       task     = taskApi.getById(expectedTasks.getFirst().getId());

        // But not modify them
        {
            Task   taskToModify = expectedTasks.getFirst();
            String originalName = taskToModify.getName();
            try {
                taskToModify.setName("Updated by regular user");
                updateTask(taskToModify);
                fail("Should not be able to update task");
            } catch (AccessDeniedException e) {
                // Expected exception
                // Restore original name to prevent test failures
                taskToModify.setName(originalName);
            }
        }

        assertThrows(AccessDeniedException.class, () -> {
            removeTaskTree(expectedTasks.getFirst());
        });
    }
}