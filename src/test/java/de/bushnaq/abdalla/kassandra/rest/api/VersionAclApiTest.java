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

import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.Version;
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
 * Test ACL-based access control for Versions.
 * Versions inherit access control from their parent Product.
 */
@Tag("UnitTest")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
public class VersionAclApiTest extends AbstractUiTestUtil {

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
     * Verifies that admin users have unrestricted access to all versions regardless of ownership.
     * Tests admin capability to:
     * - View all versions via getAll()
     * - Retrieve specific versions by ID (across different products)
     * - Update any version
     * - Delete any version
     * This is the only test that validates full admin privileges on versions.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void testAdminCanAccessAllVersions(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");

        // User2 creates a product with a version
        setUser(user2.getEmail(), "ROLE_USER");
        Product product2 = addProduct("User2 Product");
        Version version2 = addVersion(product2, "Version 2.0");

        // Admin can access all versions
        setUser(admin1.getEmail(), "ROLE_ADMIN");
        List<Version> allVersions = versionApi.getAll();
        assertEquals(2, allVersions.size(), "Admin should see all versions");

        // Admin can get specific versions
        Version retrieved1 = versionApi.getById(version1.getId());
        assertNotNull(retrieved1);
        assertEquals(version1.getId(), retrieved1.getId());

        Version retrieved2 = versionApi.getById(version2.getId());
        assertNotNull(retrieved2);
        assertEquals(version2.getId(), retrieved2.getId());

        // Admin can update any version
        version1.setName("Updated by Admin");
        versionApi.update(version1);

        // Admin can delete any version
        versionApi.deleteById(version2.getId());
    }

    /**
     * Validates that the getAll(productId) endpoint enforces ACL permissions.
     * Specifically tests:
     * - Users can retrieve all versions for products they own
     * - Users cannot retrieve versions for products they don't have access to (AccessDeniedException)
     * - After being granted access, users can retrieve versions for that product
     * This is the only test that focuses on the product-scoped version listing endpoint.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testGetAllByProductIdRespectsAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with multiple versions
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1  = addProduct("User1 Product");
        Version version1a = addVersion(product1, "Version 1.0");
        Version version1b = addVersion(product1, "Version 1.1");

        // User1 can get all versions for their product
        List<Version> versions = versionApi.getAll(product1.getId());
        assertEquals(2, versions.size(), "User1 should see all versions of their product");

        // User2 cannot get versions for user1's product
        setUser(user2.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            versionApi.getAll(product1.getId());
        });

        // After granting access, user2 can get versions
        setUser(user1.getEmail(), "ROLE_USER");
        productAclApi.grantUserAccess(product1.getId(), user2.getId());

        setUser(user2.getEmail(), "ROLE_USER");
        List<Version> versionsAfterGrant = versionApi.getAll(product1.getId());
        assertEquals(2, versionsAfterGrant.size(), "User2 should see all versions after being granted access");
    }

    /**
     * Validates that group-based ACL grants work correctly for version access.
     * Tests the inheritance chain: Product ACL → Version access via group membership.
     * Specifically validates:
     * - Users initially cannot access versions of products they don't own
     * - Granting group access to a product enables all group members to access its versions
     * - Group members can see the versions in both getById() and getAll() operations
     * This is the only test that validates group-based (as opposed to direct user-based) ACL grants.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testGroupAccessToProductGrantsVersionAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Admin creates a group with user1
        setUser(admin1.getEmail(), "ROLE_ADMIN");
        var group = userGroupApi.create("Dev Team", "Development Team", java.util.Set.of(user1.getId()));

        // User2 creates a product with a version
        setUser(user2.getEmail(), "ROLE_USER");
        Product product = addProduct("Team Product");
        Version version = addVersion(product, "Version 1.0");

        // User1 cannot access the version
        setUser(user1.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            versionApi.getById(version.getId());
        });

        // User2 grants group access to the product
        setUser(user2.getEmail(), "ROLE_USER");
        productAclApi.grantGroupAccess(product.getId(), group.getId());

        // Now user1 can access the version (via group membership)
        setUser(user1.getEmail(), "ROLE_USER");
        Version retrieved = versionApi.getById(version.getId());
        assertNotNull(retrieved);
        assertEquals(version.getId(), retrieved.getId());

        // User1 can also see it in getAll()
        List<Version> allVersions = versionApi.getAll();
        assertTrue(allVersions.stream().anyMatch(v -> v.getId().equals(version.getId())), "User1 should see the version via group access");
    }

    /**
     * Validates that revoking product access properly cascades to version access denial.
     * Tests the ACL revocation workflow:
     * - User is granted access to a product and can access its versions
     * - Product access is revoked
     * - User can no longer access any versions of that product
     * This is the only test that validates the ACL revocation mechanism for versions.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testRevokeProductAccessRevokesVersionAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version
        setUser(user1.getEmail(), "ROLE_USER");
        Product product = addProduct("User1 Product");
        Version version = addVersion(product, "Version 1.0");

        // User1 grants user2 access to the product
        productAclApi.grantUserAccess(product.getId(), user2.getId());

        // User2 can access the version
        setUser(user2.getEmail(), "ROLE_USER");
        Version retrieved = versionApi.getById(version.getId());
        assertNotNull(retrieved);

        // User1 revokes user2's access
        setUser(user1.getEmail(), "ROLE_USER");
        productAclApi.revokeUserAccess(product.getId(), user2.getId());

        // User2 can no longer access the version
        setUser(user2.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            versionApi.getById(version.getId());
        });
    }

    /**
     * Validates that granting product access enables version-level access via getById().
     * Tests the positive grant workflow:
     * - User initially cannot access another user's version by ID
     * - After being granted product access, user can retrieve the version by ID
     * This is the only test that validates the grant workflow for individual version retrieval.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testUserCanAccessVersionAfterProductAccessGranted(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");

        // User2 creates a product with a version
        setUser(user2.getEmail(), "ROLE_USER");
        Product product2 = addProduct("User2 Product");
        Version version2 = addVersion(product2, "Version 2.0");

        // User1 cannot access version2
        setUser(user1.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            versionApi.getById(version2.getId());
        });

        // User2 grants user1 access to product2
        setUser(user2.getEmail(), "ROLE_USER");
        productAclApi.grantUserAccess(product2.getId(), user1.getId());

        // Now user1 can access version2
        setUser(user1.getEmail(), "ROLE_USER");
        Version retrieved = versionApi.getById(version2.getId());
        assertNotNull(retrieved);
        assertEquals(version2.getId(), retrieved.getId());
    }

    /**
     * Validates basic ownership-based access control for versions.
     * Tests the fundamental ACL behavior:
     * - Users can access their own versions via getById() and getAll()
     * - Users cannot access other users' versions (AccessDeniedException)
     * - getAll() only returns versions the user has access to
     * This is the only test that validates basic ownership isolation without any ACL grants.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testUserCanOnlyAccessVersionsOfOwnedProducts(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");

        // User2 creates a product with a version
        setUser(user2.getEmail(), "ROLE_USER");
        Product product2 = addProduct("User2 Product");
        Version version2 = addVersion(product2, "Version 2.0");

        // User1 can access their own version
        setUser(user1.getEmail(), "ROLE_USER");
        Version retrieved1 = versionApi.getById(version1.getId());
        assertNotNull(retrieved1);
        assertEquals(version1.getId(), retrieved1.getId());

        // User1 cannot access user2's version
        assertThrows(AccessDeniedException.class, () -> {
            versionApi.getById(version2.getId());
        });

        // User1 can only see their own versions in getAll()
        List<Version> user1Versions = versionApi.getAll();
        assertEquals(1, user1Versions.size(), "User1 should only see their own versions");
        assertEquals(version1.getId(), user1Versions.get(0).getId());

        // User2 can access their own version
        setUser(user2.getEmail(), "ROLE_USER");
        Version retrieved2 = versionApi.getById(version2.getId());
        assertNotNull(retrieved2);
        assertEquals(version2.getId(), retrieved2.getId());

        // User2 cannot access user1's version
        assertThrows(AccessDeniedException.class, () -> {
            versionApi.getById(version1.getId());
        });
    }

    /**
     * Validates that version creation is restricted by product ACL.
     * Tests that:
     * - Users cannot create versions for products they don't have access to
     * - Create operation properly enforces access control at the product level
     * This is the only test that specifically validates CREATE operation access control.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testUserCannotCreateVersionForProductWithoutAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");

        // User2 tries to create a version for user1's product - should fail
        setUser(user2.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            addVersion(product1, "Unauthorized Version");
        });
    }

    /**
     * Validates that version deletion is restricted by product ACL.
     * Tests that:
     * - Users cannot delete versions of products they don't have access to
     * - Delete operation properly enforces access control
     * This is the only test that specifically validates DELETE operation access control.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testUserCannotDeleteVersionWithoutAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");

        // User2 tries to delete user1's version - should fail
        setUser(user2.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            versionApi.deleteById(version1.getId());
        });
    }

    /**
     * Validates that version updates are restricted by product ACL.
     * Tests that:
     * - Users cannot update versions of products they don't have access to
     * - Update operation properly enforces access control
     * This is the only test that specifically validates UPDATE operation access control.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testUserCannotUpdateVersionWithoutAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");

        // User2 tries to update user1's version - should fail
        setUser(user2.getEmail(), "ROLE_USER");
        version1.setName("Hacked Version");
        assertThrows(AccessDeniedException.class, () -> {
            versionApi.update(version1);
        });
    }
}

