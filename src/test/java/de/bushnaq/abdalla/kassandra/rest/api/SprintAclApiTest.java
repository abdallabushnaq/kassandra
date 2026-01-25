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
 * Test ACL-based access control for Sprints.
 * Sprints inherit access control from their parent Feature's Version's Product.
 */
@Tag("UnitTest")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
public class SprintAclApiTest extends AbstractUiTestUtil {

    private User admin1;
    private User user1;
    private User user2;
    private User user3;

    @Autowired
    UserRepository userRepository;

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

    /**
     * Verifies that admin users have unrestricted access to all sprints regardless of ownership.
     * Tests admin capability to:
     * - View all sprints via getAll()
     * - Retrieve specific sprints by ID (across different products)
     * - Update any sprint
     * - Delete any sprint
     * This is the only test that validates full admin privileges on sprints.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void testAdminCanAccessAllSprints(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version, feature, and sprint
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");
        Feature feature1 = addFeature(version1, "Feature 1");
        Sprint  sprint1  = addSprint(feature1, "Sprint 1");

        // User2 creates a product with a version, feature, and sprint
        setUser(user2.getEmail(), "ROLE_USER");
        Product product2 = addProduct("User2 Product");
        Version version2 = addVersion(product2, "Version 2.0");
        Feature feature2 = addFeature(version2, "Feature 2");
        Sprint  sprint2  = addSprint(feature2, "Sprint 2");

        // Admin can access all sprints
        setUser(admin1.getEmail(), "ROLE_ADMIN");
        List<Sprint> allSprints = sprintApi.getAll();
        assertEquals(1 + 2, allSprints.size(), "Admin should see all sprints");// the "Backlog" Sprint is always there

        // Admin can get specific sprints
        Sprint retrieved1 = sprintApi.getById(sprint1.getId());
        assertNotNull(retrieved1);
        assertEquals(sprint1.getId(), retrieved1.getId());

        Sprint retrieved2 = sprintApi.getById(sprint2.getId());
        assertNotNull(retrieved2);
        assertEquals(sprint2.getId(), retrieved2.getId());

        // Admin can update any sprint
        sprint1.setName("Updated by Admin");
        sprintApi.update(sprint1);

        // Admin can delete any sprint
        sprintApi.deleteById(sprint2.getId());
    }

    /**
     * Validates that the getAll(featureId) endpoint enforces ACL permissions.
     * Specifically tests:
     * - Users can retrieve all sprints for features they own
     * - Users cannot retrieve sprints for features they don't have access to (AccessDeniedException)
     * - After being granted access, users can retrieve sprints for that feature
     * This is the only test that focuses on the feature-scoped sprint listing endpoint.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testGetAllByFeatureIdRespectsAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version, feature, and multiple sprints
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");
        Feature feature1 = addFeature(version1, "Feature 1");
        Sprint  sprint1a = addSprint(feature1, "Sprint 1A");
        Sprint  sprint1b = addSprint(feature1, "Sprint 1B");

        // User1 can get all sprints for their feature
        List<Sprint> sprints = sprintApi.getAll(feature1.getId());
        assertEquals(2, sprints.size(), "User1 should see all sprints of their feature");

        // User2 cannot get sprints for user1's feature
        setUser(user2.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            sprintApi.getAll(feature1.getId());
        });

        // After granting access, user2 can get sprints
        setUser(user1.getEmail(), "ROLE_USER");
        productAclApi.grantUserAccess(product1.getId(), user2.getId());

        setUser(user2.getEmail(), "ROLE_USER");
        List<Sprint> sprintsAfterGrant = sprintApi.getAll(feature1.getId());
        assertEquals(2, sprintsAfterGrant.size(), "User2 should see all sprints after being granted access");
    }

    /**
     * Validates that group-based ACL grants work correctly for sprint access.
     * Tests the inheritance chain: Product ACL → Version → Feature → Sprint access via group membership.
     * Specifically validates:
     * - Users initially cannot access sprints of products they don't own
     * - Granting group access to a product enables all group members to access its sprints
     * - Group members can see the sprints in both getById() and getAll() operations
     * This is the only test that validates group-based (as opposed to direct user-based) ACL grants for sprints.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testGroupAccessToProductGrantsSprintAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Admin creates a group with user1
        setUser(admin1.getEmail(), "ROLE_ADMIN");
        var group = userGroupApi.create("Dev Team", "Development Team", java.util.Set.of(user1.getId()));

        // User2 creates a product with a version, feature, and sprint
        setUser(user2.getEmail(), "ROLE_USER");
        Product product = addProduct("Team Product");
        Version version = addVersion(product, "Version 1.0");
        Feature feature = addFeature(version, "Feature 1");
        Sprint  sprint  = addSprint(feature, "Sprint 1");

        // User1 cannot access the sprint
        setUser(user1.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            sprintApi.getById(sprint.getId());
        });

        // User2 grants group access to the product
        setUser(user2.getEmail(), "ROLE_USER");
        productAclApi.grantGroupAccess(product.getId(), group.getId());

        // Now user1 can access the sprint (via group membership)
        setUser(user1.getEmail(), "ROLE_USER");
        Sprint retrieved = sprintApi.getById(sprint.getId());
        assertNotNull(retrieved);
        assertEquals(sprint.getId(), retrieved.getId());

        // User1 can also see it in getAll()
        List<Sprint> allSprints = sprintApi.getAll();
        assertTrue(allSprints.stream().anyMatch(s -> s.getId().equals(sprint.getId())), "User1 should see the sprint via group access");
    }

    /**
     * Validates that revoking product access properly cascades to sprint access denial.
     * Tests the ACL revocation workflow:
     * - User is granted access to a product and can access its sprints
     * - Product access is revoked
     * - User can no longer access any sprints of that product
     * This is the only test that validates the ACL revocation mechanism for sprints.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testRevokeProductAccessRevokesSprintAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version, feature, and sprint
        setUser(user1.getEmail(), "ROLE_USER");
        Product product = addProduct("User1 Product");
        Version version = addVersion(product, "Version 1.0");
        Feature feature = addFeature(version, "Feature 1");
        Sprint  sprint  = addSprint(feature, "Sprint 1");

        // User1 grants user2 access to the product
        productAclApi.grantUserAccess(product.getId(), user2.getId());

        // User2 can access the sprint
        setUser(user2.getEmail(), "ROLE_USER");
        Sprint retrieved = sprintApi.getById(sprint.getId());
        assertNotNull(retrieved);

        // User1 revokes user2's access
        setUser(user1.getEmail(), "ROLE_USER");
        productAclApi.revokeUserAccess(product.getId(), user2.getId());

        // User2 can no longer access the sprint
        setUser(user2.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            sprintApi.getById(sprint.getId());
        });
    }

    /**
     * Validates that granting product access enables sprint-level access via getById().
     * Tests the positive grant workflow:
     * - User initially cannot access another user's sprint by ID
     * - After being granted product access, user can retrieve the sprint by ID
     * This is the only test that validates the grant workflow for individual sprint retrieval.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testUserCanAccessSprintAfterProductAccessGranted(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version, feature, and sprint
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");
        Feature feature1 = addFeature(version1, "Feature 1");
        Sprint  sprint1  = addSprint(feature1, "Sprint 1");

        // User2 creates a product with a version, feature, and sprint
        setUser(user2.getEmail(), "ROLE_USER");
        Product product2 = addProduct("User2 Product");
        Version version2 = addVersion(product2, "Version 2.0");
        Feature feature2 = addFeature(version2, "Feature 2");
        Sprint  sprint2  = addSprint(feature2, "Sprint 2");

        // User1 cannot access sprint2
        setUser(user1.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            sprintApi.getById(sprint2.getId());
        });

        // User2 grants user1 access to product2
        setUser(user2.getEmail(), "ROLE_USER");
        productAclApi.grantUserAccess(product2.getId(), user1.getId());

        // Now user1 can access sprint2
        setUser(user1.getEmail(), "ROLE_USER");
        Sprint retrieved = sprintApi.getById(sprint2.getId());
        assertNotNull(retrieved);
        assertEquals(sprint2.getId(), retrieved.getId());
    }

    /**
     * Validates basic ownership-based access control for sprints.
     * Tests the fundamental ACL behavior:
     * - Users can access their own sprints via getById() and getAll()
     * - Users cannot access other users' sprints (AccessDeniedException)
     * - getAll() only returns sprints the user has access to
     * This is the only test that validates basic ownership isolation without any ACL grants for sprints.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testUserCanOnlyAccessSprintsOfOwnedProducts(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version, feature, and sprint
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");
        Feature feature1 = addFeature(version1, "Feature 1");
        Sprint  sprint1  = addSprint(feature1, "Sprint 1");

        // User2 creates a product with a version, feature, and sprint
        setUser(user2.getEmail(), "ROLE_USER");
        Product product2 = addProduct("User2 Product");
        Version version2 = addVersion(product2, "Version 2.0");
        Feature feature2 = addFeature(version2, "Feature 2");
        Sprint  sprint2  = addSprint(feature2, "Sprint 2");

        // User1 can access their own sprint
        setUser(user1.getEmail(), "ROLE_USER");
        Sprint retrieved1 = sprintApi.getById(sprint1.getId());
        assertNotNull(retrieved1);
        assertEquals(sprint1.getId(), retrieved1.getId());

        // User1 cannot access user2's sprint
        assertThrows(AccessDeniedException.class, () -> {
            sprintApi.getById(sprint2.getId());
        });

        // User1 can only see their own sprints in getAll()
        List<Sprint> user1Sprints = sprintApi.getAll();
        assertEquals(1 + 1, user1Sprints.size(), "User1 should only see their own sprints");// the "Backlog" Sprint is always there
        assertEquals(sprint1.getId(), user1Sprints.get(1).getId());

        // User2 can access their own sprint
        setUser(user2.getEmail(), "ROLE_USER");
        Sprint retrieved2 = sprintApi.getById(sprint2.getId());
        assertNotNull(retrieved2);
        assertEquals(sprint2.getId(), retrieved2.getId());

        // User2 cannot access user1's sprint
        assertThrows(AccessDeniedException.class, () -> {
            sprintApi.getById(sprint1.getId());
        });
    }

    /**
     * Validates that sprint creation is restricted by product ACL.
     * Tests that:
     * - Users cannot create sprints for features of products they don't have access to
     * - Create operation properly enforces access control at the product level
     * This is the only test that specifically validates CREATE operation access control for sprints.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testUserCannotCreateSprintForFeatureWithoutAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version and feature
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");
        Feature feature1 = addFeature(version1, "Feature 1");

        // User2 tries to create a sprint for user1's feature - should fail
        setUser(user2.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            addSprint(feature1, "Unauthorized Sprint");
        });
    }

    /**
     * Validates that sprint deletion is restricted by product ACL.
     * Tests that:
     * - Users cannot delete sprints of products they don't have access to
     * - Delete operation properly enforces access control
     * This is the only test that specifically validates DELETE operation access control for sprints.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testUserCannotDeleteSprintWithoutAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version, feature, and sprint
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");
        Feature feature1 = addFeature(version1, "Feature 1");
        Sprint  sprint1  = addSprint(feature1, "Sprint 1");

        // User2 tries to delete user1's sprint - should fail
        setUser(user2.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            sprintApi.deleteById(sprint1.getId());
        });
    }

    /**
     * Validates that sprint updates are restricted by product ACL.
     * Tests that:
     * - Users cannot update sprints of products they don't have access to
     * - Update operation properly enforces access control
     * This is the only test that specifically validates UPDATE operation access control for sprints.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testUserCannotUpdateSprintWithoutAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version, feature, and sprint
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");
        Feature feature1 = addFeature(version1, "Feature 1");
        Sprint  sprint1  = addSprint(feature1, "Sprint 1");

        // User2 tries to update user1's sprint - should fail
        setUser(user2.getEmail(), "ROLE_USER");
        sprint1.setName("Hacked Sprint");
        assertThrows(AccessDeniedException.class, () -> {
            sprintApi.update(sprint1);
        });
    }
}

