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

import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.repository.UserRepository;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractUiTestUtil;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test ACL-based access control for Tasks.
 * Tasks inherit access control from their parent Sprint's Feature's Version's Product.
 */
@Tag("UnitTest")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
public class TaskAclApiTest extends AbstractUiTestUtil {

    private User admin1;
    private User user1;
    private User user2;
    private User user3;

    @Autowired
    UserRepository userRepository;

    /**
     * Simple wrapper to add a task with just a sprint and name.
     * Uses default values for other parameters.
     *
     * @param sprint the sprint to add the task to
     * @param name   the name of the task
     * @return the created Task object
     */
    protected Task addTask(Sprint sprint, String name) {
        return addTask(sprint, null, name, ParameterOptions.getLocalNow(), Duration.ofHours(1), Duration.ofHours(2), null, null, TaskMode.AUTO_SCHEDULED, false);
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

    /**
     * Verifies that admin users have unrestricted access to all tasks regardless of ownership.
     * Tests admin capability to:
     * - View all tasks via getAll()
     * - Retrieve specific tasks by ID (across different products)
     * - Update any task
     * - Delete any task
     * This is the only test that validates full admin privileges on tasks.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void testAdminCanAccessAllTasks(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version, feature, sprint, and task
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");
        Feature feature1 = addFeature(version1, "Feature 1");
        Sprint  sprint1  = addSprint(feature1, "Sprint 1");
        Task    task1    = addTask(sprint1, "Task 1");

        // User2 creates a product with a version, feature, sprint, and task
        setUser(user2.getEmail(), "ROLE_USER");
        Product product2 = addProduct("User2 Product");
        Version version2 = addVersion(product2, "Version 2.0");
        Feature feature2 = addFeature(version2, "Feature 2");
        Sprint  sprint2  = addSprint(feature2, "Sprint 2");
        Task    task2    = addTask(sprint2, "Task 2");

        // Admin can access all tasks
        setUser(admin1.getEmail(), "ROLE_ADMIN");
        List<Task> allTasks = taskApi.getAll();
        assertEquals(2, allTasks.size(), "Admin should see all tasks");

        // Admin can get specific tasks
        Task retrieved1 = taskApi.getById(task1.getId());
        assertNotNull(retrieved1);
        assertEquals(task1.getId(), retrieved1.getId());

        Task retrieved2 = taskApi.getById(task2.getId());
        assertNotNull(retrieved2);
        assertEquals(task2.getId(), retrieved2.getId());

        // Admin can update any task
        task1.setName("Updated by Admin");
        taskApi.update(task1);

        // Admin can delete any task
        taskApi.deleteById(task2.getId());
    }

    /**
     * Validates that the getAll(sprintId) endpoint enforces ACL permissions.
     * Specifically tests:
     * - Users can retrieve all tasks for sprints they own
     * - Users cannot retrieve tasks for sprints they don't have access to (AccessDeniedException)
     * - After being granted access, users can retrieve tasks for that sprint
     * This is the only test that focuses on the sprint-scoped task listing endpoint.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testGetAllBySprintIdRespectsAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version, feature, sprint, and multiple tasks
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");
        Feature feature1 = addFeature(version1, "Feature 1");
        Sprint  sprint1  = addSprint(feature1, "Sprint 1");
        Task    task1a   = addTask(sprint1, "Task 1A");
        Task    task1b   = addTask(sprint1, "Task 1B");

        // User1 can get all tasks for their sprint
        List<Task> tasks = taskApi.getAll(sprint1.getId());
        assertEquals(2, tasks.size(), "User1 should see all tasks of their sprint");

        // User2 cannot get tasks for user1's sprint
        setUser(user2.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            taskApi.getAll(sprint1.getId());
        });

        // After granting access, user2 can get tasks
        setUser(user1.getEmail(), "ROLE_USER");
        productAclApi.grantUserAccess(product1.getId(), user2.getId());

        setUser(user2.getEmail(), "ROLE_USER");
        List<Task> tasksAfterGrant = taskApi.getAll(sprint1.getId());
        assertEquals(2, tasksAfterGrant.size(), "User2 should see all tasks after being granted access");
    }

    /**
     * Validates that group-based ACL grants work correctly for task access.
     * Tests the inheritance chain: Product ACL → Version → Feature → Sprint → Task access via group membership.
     * Specifically validates:
     * - Users initially cannot access tasks of products they don't own
     * - Granting group access to a product enables all group members to access its tasks
     * - Group members can see the tasks in both getById() and getAll() operations
     * This is the only test that validates group-based (as opposed to direct user-based) ACL grants for tasks.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testGroupAccessToProductGrantsTaskAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Admin creates a group with user1
        setUser(admin1.getEmail(), "ROLE_ADMIN");
        var group = userGroupApi.create("Dev Team", "Development Team", java.util.Set.of(user1.getId()));

        // User2 creates a product with a version, feature, sprint, and task
        setUser(user2.getEmail(), "ROLE_USER");
        Product product = addProduct("Team Product");
        Version version = addVersion(product, "Version 1.0");
        Feature feature = addFeature(version, "Feature 1");
        Sprint  sprint  = addSprint(feature, "Sprint 1");
        Task    task    = addTask(sprint, "Task 1");

        // User1 cannot access the task
        setUser(user1.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            taskApi.getById(task.getId());
        });

        // User2 grants group access to the product
        setUser(user2.getEmail(), "ROLE_USER");
        productAclApi.grantGroupAccess(product.getId(), group.getId());

        // Now user1 can access the task (via group membership)
        setUser(user1.getEmail(), "ROLE_USER");
        Task retrieved = taskApi.getById(task.getId());
        assertNotNull(retrieved);
        assertEquals(task.getId(), retrieved.getId());

        // User1 can also see it in getAll()
        List<Task> allTasks = taskApi.getAll();
        assertTrue(allTasks.stream().anyMatch(t -> t.getId().equals(task.getId())), "User1 should see the task via group access");
    }

    /**
     * Validates that revoking product access properly cascades to task access denial.
     * Tests the ACL revocation workflow:
     * - User is granted access to a product and can access its tasks
     * - Product access is revoked
     * - User can no longer access any tasks of that product
     * This is the only test that validates the ACL revocation mechanism for tasks.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testRevokeProductAccessRevokesTaskAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version, feature, sprint, and task
        setUser(user1.getEmail(), "ROLE_USER");
        Product product = addProduct("User1 Product");
        Version version = addVersion(product, "Version 1.0");
        Feature feature = addFeature(version, "Feature 1");
        Sprint  sprint  = addSprint(feature, "Sprint 1");
        Task    task    = addTask(sprint, "Task 1");

        // User1 grants user2 access to the product
        productAclApi.grantUserAccess(product.getId(), user2.getId());

        // User2 can access the task
        setUser(user2.getEmail(), "ROLE_USER");
        Task retrieved = taskApi.getById(task.getId());
        assertNotNull(retrieved);

        // User1 revokes user2's access
        setUser(user1.getEmail(), "ROLE_USER");
        productAclApi.revokeUserAccess(product.getId(), user2.getId());

        // User2 can no longer access the task
        setUser(user2.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            taskApi.getById(task.getId());
        });
    }

    /**
     * Validates that granting product access enables task-level access via getById().
     * Tests the positive grant workflow:
     * - User initially cannot access another user's task by ID
     * - After being granted product access, user can retrieve the task by ID
     * This is the only test that validates the grant workflow for individual task retrieval.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testUserCanAccessTaskAfterProductAccessGranted(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version, feature, sprint, and task
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");
        Feature feature1 = addFeature(version1, "Feature 1");
        Sprint  sprint1  = addSprint(feature1, "Sprint 1");
        Task    task1    = addTask(sprint1, "Task 1");

        // User2 creates a product with a version, feature, sprint, and task
        setUser(user2.getEmail(), "ROLE_USER");
        Product product2 = addProduct("User2 Product");
        Version version2 = addVersion(product2, "Version 2.0");
        Feature feature2 = addFeature(version2, "Feature 2");
        Sprint  sprint2  = addSprint(feature2, "Sprint 2");
        Task    task2    = addTask(sprint2, "Task 2");

        // User1 cannot access task2
        setUser(user1.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            taskApi.getById(task2.getId());
        });

        // User2 grants user1 access to product2
        setUser(user2.getEmail(), "ROLE_USER");
        productAclApi.grantUserAccess(product2.getId(), user1.getId());

        // Now user1 can access task2
        setUser(user1.getEmail(), "ROLE_USER");
        Task retrieved = taskApi.getById(task2.getId());
        assertNotNull(retrieved);
        assertEquals(task2.getId(), retrieved.getId());
    }

    /**
     * Validates basic ownership-based access control for tasks.
     * Tests the fundamental ACL behavior:
     * - Users can access their own tasks via getById() and getAll()
     * - Users cannot access other users' tasks (AccessDeniedException)
     * - getAll() only returns tasks the user has access to
     * This is the only test that validates basic ownership isolation without any ACL grants for tasks.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testUserCanOnlyAccessTasksOfOwnedProducts(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version, feature, sprint, and task
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");
        Feature feature1 = addFeature(version1, "Feature 1");
        Sprint  sprint1  = addSprint(feature1, "Sprint 1");
        Task    task1    = addTask(sprint1, "Task 1");

        // User2 creates a product with a version, feature, sprint, and task
        setUser(user2.getEmail(), "ROLE_USER");
        Product product2 = addProduct("User2 Product");
        Version version2 = addVersion(product2, "Version 2.0");
        Feature feature2 = addFeature(version2, "Feature 2");
        Sprint  sprint2  = addSprint(feature2, "Sprint 2");
        Task    task2    = addTask(sprint2, "Task 2");

        // User1 can access their own task
        setUser(user1.getEmail(), "ROLE_USER");
        Task retrieved1 = taskApi.getById(task1.getId());
        assertNotNull(retrieved1);
        assertEquals(task1.getId(), retrieved1.getId());

        // User1 cannot access user2's task
        assertThrows(AccessDeniedException.class, () -> {
            taskApi.getById(task2.getId());
        });

        // User1 can only see their own tasks in getAll()
        List<Task> user1Tasks = taskApi.getAll();
        assertEquals(1, user1Tasks.size(), "User1 should only see their own tasks");
        assertEquals(task1.getId(), user1Tasks.getFirst().getId());

        // User2 can access their own task
        setUser(user2.getEmail(), "ROLE_USER");
        Task retrieved2 = taskApi.getById(task2.getId());
        assertNotNull(retrieved2);
        assertEquals(task2.getId(), retrieved2.getId());

        // User2 cannot access user1's task
        assertThrows(AccessDeniedException.class, () -> {
            taskApi.getById(task1.getId());
        });
    }

    /**
     * Validates that task creation is restricted by product ACL.
     * Tests that:
     * - Users cannot create tasks for sprints of products they don't have access to
     * - Create operation properly enforces access control at the product level
     * This is the only test that specifically validates CREATE operation access control for tasks.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testUserCannotCreateTaskForSprintWithoutAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version, feature, and sprint
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");
        Feature feature1 = addFeature(version1, "Feature 1");
        Sprint  sprint1  = addSprint(feature1, "Sprint 1");

        // User2 tries to create a task for user1's sprint - should fail
        setUser(user2.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            addTask(sprint1, "Unauthorized Task");
        });
    }

    /**
     * Validates that task deletion is restricted by product ACL.
     * Tests that:
     * - Users cannot delete tasks of products they don't have access to
     * - Delete operation properly enforces access control
     * This is the only test that specifically validates DELETE operation access control for tasks.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testUserCannotDeleteTaskWithoutAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version, feature, sprint, and task
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");
        Feature feature1 = addFeature(version1, "Feature 1");
        Sprint  sprint1  = addSprint(feature1, "Sprint 1");
        Task    task1    = addTask(sprint1, "Task 1");

        // User2 tries to delete user1's task - should fail
        setUser(user2.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            taskApi.deleteById(task1.getId());
        });
    }

    /**
     * Validates that task updates are restricted by product ACL.
     * Tests that:
     * - Users cannot update tasks of products they don't have access to
     * - Update operation properly enforces access control
     * This is the only test that specifically validates UPDATE operation access control for tasks.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testUserCannotUpdateTaskWithoutAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version, feature, sprint, and task
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");
        Feature feature1 = addFeature(version1, "Feature 1");
        Sprint  sprint1  = addSprint(feature1, "Sprint 1");
        Task    task1    = addTask(sprint1, "Task 1");

        // User2 tries to update user1's task - should fail
        setUser(user2.getEmail(), "ROLE_USER");
        task1.setName("Hacked Task");
        assertThrows(AccessDeniedException.class, () -> {
            taskApi.update(task1);
        });
    }
}

