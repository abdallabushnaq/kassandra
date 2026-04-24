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

import java.util.UUID;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractUiTestUtil;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;


@Tag("UnitTest")
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
@Slf4j
public class TaskApiTest extends AbstractUiTestUtil {
    private static final UUID   FAKE_ID     = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String SECOND_NAME = "SECOND_NAME";

    private User admin1;
    private User user1;
    private User user2;
    private User user3;

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

    /**
     * Verifies that deleting a leaf task (no children, no predecessor relations) removes it from the server and
     * leaves all other data intact.
     *
     * @throws Exception if the REST call fails unexpectedly
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteLeafTask() throws Exception {
        Product product = addProduct("Product");
        Version version = addVersion(product, "1.0.0");
        Feature feature = addRandomFeature(version);
        Sprint  sprint  = addRandomSprint(feature);
        Task    leaf    = addTask(sprint, null, "Leaf Task", LocalDateTime.now(), Duration.ofDays(3), null, null, null);

        UUID leafId = leaf.getId();

        taskApi.deleteById(leafId);

        List<UUID> remainingIds = taskApi.getAll().stream().map(Task::getId).toList();
        Assertions.assertThat(remainingIds).doesNotContain(leafId);

        // Sync local state so @AfterEach validation passes
        expectedTasks.remove(leaf);
        sprint.getTasks().remove(leaf);
    }

    /**
     * Verifies that deleting a parent task cascades to all descendants (children and grandchildren).
     * After deleting the root, none of the descendant IDs should appear in {@code taskApi.getAll()}.
     *
     * @throws Exception if the REST call fails unexpectedly
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteParentTaskDeletesDescendants() throws Exception {
        Product product    = addProduct("Product");
        Version version    = addVersion(product, "1.0.0");
        Feature feature    = addRandomFeature(version);
        Sprint  sprint     = addRandomSprint(feature);
        Task    parent     = addTask(sprint, null, "Parent", LocalDateTime.now(), Duration.ofDays(10), null, null, null);
        Task    child1     = addTask(sprint, parent, "Child 1", LocalDateTime.now(), Duration.ofDays(4), null, null, null);
        Task    child2     = addTask(sprint, parent, "Child 2", LocalDateTime.now(), Duration.ofDays(3), null, null, null);
        Task    grandchild = addTask(sprint, child1, "Grandchild", LocalDateTime.now(), Duration.ofDays(2), null, null, null);

        UUID parentId     = parent.getId();
        UUID child1Id     = child1.getId();
        UUID child2Id     = child2.getId();
        UUID grandchildId = grandchild.getId();

        taskApi.deleteById(parentId);

        List<UUID> remainingIds = taskApi.getAll().stream().map(Task::getId).toList();
        Assertions.assertThat(remainingIds).doesNotContain(parentId, child1Id, child2Id, grandchildId);

        // Sync local state so @AfterEach validation passes
        expectedTasks.removeIf(t -> {
            UUID id = t.getId();
            return id.equals(parentId) || id.equals(child1Id) || id.equals(child2Id) || id.equals(grandchildId);
        });
        sprint.getTasks().removeIf(t -> {
            UUID id = t.getId();
            return id.equals(parentId) || id.equals(child1Id) || id.equals(child2Id) || id.equals(grandchildId);
        });
    }

    /**
     * Verifies that deleting a task also removes inbound predecessor relations from surviving tasks.
     * <p>
     * Setup: taskA is a predecessor of taskB (taskB → taskA). After deleting taskA:
     * <ul>
     *   <li>taskA must be gone from the server.</li>
     *   <li>taskB must still exist.</li>
     *   <li>taskB's predecessor list must no longer reference taskA.</li>
     * </ul>
     *
     * @throws Exception if the REST call fails unexpectedly
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteTaskCleansInboundRelations() throws Exception {
        Product product = addProduct("Product");
        Version version = addVersion(product, "1.0.0");
        Feature feature = addRandomFeature(version);
        Sprint  sprint  = addRandomSprint(feature);

        // taskB depends on taskA (taskA is a predecessor of taskB)
        Task taskA = addTask(sprint, null, "Task A", LocalDateTime.now(), Duration.ofDays(3), null, null, null);
        Task taskB = addTask(sprint, null, "Task B", LocalDateTime.now().plusDays(3), Duration.ofDays(3), null, null, taskA);

        UUID aId = taskA.getId();
        UUID bId = taskB.getId();

        // Verify the relation exists before deletion
        Assertions.assertThat(taskB.getPredecessors()).anyMatch(r -> r.getPredecessorId().equals(aId));

        taskApi.deleteById(aId);

        List<Task> remaining = taskApi.getAll();

        // taskA must be gone
        Assertions.assertThat(remaining.stream().map(Task::getId).toList()).doesNotContain(aId);

        // taskB must still exist and no longer reference taskA in its predecessors
        Task survivingB = remaining.stream()
                .filter(t -> t.getId().equals(bId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("taskB should still exist after deleting taskA"));
        Assertions.assertThat(survivingB.getPredecessors()).noneMatch(r -> r.getPredecessorId().equals(aId));

        // Sync local state so @AfterEach validation passes
        expectedTasks.remove(taskA);
        sprint.getTasks().remove(taskA);
        // Remove the now-stale predecessor reference from the local taskB so assertTaskEquals passes
        taskB.getPredecessors().removeIf(r -> r.getPredecessorId().equals(aId));
    }

    /**
     * Verifies that deleting a task that owns outbound predecessor relations (taskB → taskA) leaves the
     * referenced task (taskA) completely intact, and that the relation rows owned by taskB are
     * removed as part of the cascade.
     * <p>
     * Setup: taskA has no predecessors; taskB lists taskA as its predecessor.
     * After deleting taskB:
     * <ul>
     *   <li>taskB must be gone from the server.</li>
     *   <li>taskA must still exist, unchanged.</li>
     *   <li>No orphan relation rows should remain (verified implicitly via the {@code @AfterEach}
     *       deep-equality check that compares expected vs. actual task state).</li>
     * </ul>
     *
     * @throws Exception if the REST call fails unexpectedly
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void deleteTaskCleansOutboundRelations() throws Exception {
        Product product = addProduct("Product");
        Version version = addVersion(product, "1.0.0");
        Feature feature = addRandomFeature(version);
        Sprint  sprint  = addRandomSprint(feature);

        // taskB owns the relation: taskB → taskA
        Task taskA = addTask(sprint, null, "Task A", LocalDateTime.now(), Duration.ofDays(3), null, null, null);
        Task taskB = addTask(sprint, null, "Task B", LocalDateTime.now().plusDays(3), Duration.ofDays(3), null, null, taskA);

        UUID aId = taskA.getId();
        UUID bId = taskB.getId();

        // Verify the relation exists on taskB before deletion
        Assertions.assertThat(taskB.getPredecessors()).anyMatch(r -> r.getPredecessorId().equals(aId));

        // Delete the task that OWNS the relation (taskB), not the predecessor itself
        taskApi.deleteById(bId);

        List<Task> remaining = taskApi.getAll();

        // taskB must be gone
        Assertions.assertThat(remaining.stream().map(Task::getId).toList()).doesNotContain(bId);

        // taskA must still exist and be unmodified
        Task survivingA = remaining.stream()
                .filter(t -> t.getId().equals(aId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("taskA should still exist after deleting taskB"));
        Assertions.assertThat(survivingA.getPredecessors()).isEmpty();

        // Sync local state so @AfterEach validation passes
        expectedTasks.remove(taskB);
        sprint.getTasks().remove(taskB);
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void userSecurity(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        {
            setUser("admin-user", "ROLE_ADMIN");

            Product product = addProduct("Product");
            Version version = addVersion(product, "1.0.0");
            Feature feature = addRandomFeature(version);
            Sprint  sprint  = addRandomSprint(feature);
            Task    task    = addTask(sprint, null, "Project Phase 1", LocalDateTime.now(), Duration.ofDays(10), null, null, null);
        }
        setUser(user1.getEmail(), "ROLE_USER");

        // Regular users should be able to view tasks
        List<Task> allTasks = taskApi.getAll();
        assertThrows(AccessDeniedException.class, () -> {
            Task task = taskApi.getById(expectedTasks.getFirst().getId());
            log.trace("Task retrieved by regular user: {}", task);
        });

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