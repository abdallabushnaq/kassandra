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
import de.bushnaq.abdalla.kassandra.dto.ProductAclEntry;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.UserGroup;
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

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ProductAclApi - verifies product access control list functionality
 */
@Tag("UnitTest")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ProductAclApiTest extends AbstractUiTestUtil {

    private User admin1;
    private User user1;
    private User user2;
    private User user3;

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
     * Verifies that admin users have unrestricted access to view ACLs and products regardless of ownership.
     * Tests admin privileges:
     * - Admin can view ACL of any product (even those created by other users)
     * - Admin can see all products via getAll() regardless of ACL
     * This is the only test that validates full admin override capabilities for ACL viewing.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void testAdminCanViewAnyAcl(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Create product as different user
        setUser(user2.getEmail(), "ROLE_USER");
        Product product = addProduct("User Product");

        // Admin should be able to view ACL
        setUser("admin-user", "ROLE_ADMIN");
        List<ProductAclEntry> acl = productAclApi.getAcl(product.getId());
        assertNotNull(acl);
        List<Product> all = productApi.getAll();
        assertEquals(1, all.size(), "Admin should see all");
    }

    /**
     * Validates that unauthenticated (anonymous) users cannot access ACL endpoints.
     * Tests security requirement:
     * - Anonymous users (no authentication credentials) must be rejected with AuthenticationCredentialsNotFoundException
     * This is the only test that validates authentication requirement for ACL operations.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testAnonymousCannotAccessAcl(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        setUser("admin-user", "ROLE_ADMIN");
        Product product = addProduct("Product");

        // Clear security context (anonymous)
        setUser(null, null);

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            productAclApi.getAcl(product.getId());
        });
    }

    /**
     * Validates that duplicate ACL grants are prevented.
     * Tests idempotency constraint:
     * - Attempting to grant access to the same user twice should fail
     * - System should return an error indicating the entry already exists
     * This is the only test that validates ACL uniqueness constraints.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "kristen.hubbell@kassandra.org", roles = "USER")
    public void testCannotGrantAccessTwiceToSameUser(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        Product product = addProduct("My Product");

        // Grant access once
        productAclApi.grantUserAccess(product.getId(), user2.getId());

        // Try to grant again - should fail
        try {
            productAclApi.grantUserAccess(product.getId(), user2.getId());
            fail("Should not be able to grant access twice");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("already") || e.getMessage().contains("exists"));
        }
    }

    /**
     * Validates that product creators can grant user-level access.
     * Tests the basic ACL grant workflow:
     * - Creator successfully grants access to another user
     * - ACL entry is properly created with correct productId and userId
     * - Granted user can subsequently access the product
     * This is the only test that validates the complete creator-grants-user workflow with verification.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "kristen.hubbell@kassandra.org", roles = "USER")
    public void testCreatorCanGrantUserAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        // Creator creates a product
        Product product = addProduct("Shared Product");

        // Creator grants access to another user
        ProductAclEntry entry = productAclApi.grantUserAccess(product.getId(), user2.getId());
        assertNotNull(entry);
        assertNotNull(entry.getId());
        assertEquals(product.getId(), entry.getProductId());
        assertEquals(user2.getId(), entry.getUserId());

        // Verify the user now has access
        setUser(user2.getEmail(), "ROLE_USER");
        Product accessed = productApi.getById(product.getId());
        assertNotNull(accessed);
    }

    /**
     * Validates that product creators can revoke user access.
     * Tests the ACL revocation workflow:
     * - User is granted access and can access the product
     * - Creator revokes the access
     * - User can no longer access the product (AccessDeniedException)
     * This is the only test that validates the complete creator-revokes-user workflow.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "kristen.hubbell@kassandra.org", roles = "USER")
    public void testCreatorCanRevokeUserAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        // Creator creates product and grants access
        Product product = addProduct("My Product");
        productAclApi.grantUserAccess(product.getId(), user3.getId());

        // Verify user has access
        setUser(user3.getEmail(), "ROLE_USER");
        Product accessed = productApi.getById(product.getId());
        assertNotNull(accessed);

        // Creator revokes access
        setUser(user1.getEmail(), "ROLE_USER");
        productAclApi.revokeUserAccess(product.getId(), user3.getId());

        // User should no longer have access
        setUser(user3.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            productApi.getById(product.getId());
        });
    }

    /**
     * Validates that product creators can view their own product's ACL.
     * Tests basic ownership access to ACL viewing:
     * - Creator creates a product
     * - Creator can successfully retrieve the ACL entries
     * - ACL contains at least the creator's entry
     * This is the only test that validates the creator's ability to view ACL (as opposed to admin viewing any ACL).
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "kristen.hubbell@kassandra.org", roles = "USER")
    public void testCreatorCanViewOwnAcl(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        // User creates a product
        Product product = addProduct("My Product");

        // Creator should be able to view ACL
        List<ProductAclEntry> acl = productAclApi.getAcl(product.getId());
        assertNotNull(acl);
        // Should have at least one entry (the creator)
        assertTrue(acl.size() >= 1);
    }

    /**
     * Validates that ACL entries are automatically cleaned up when a product is deleted.
     * Tests cascade deletion behavior:
     * - Product is created with multiple ACL entries (multiple users granted access)
     * - Product is deleted
     * - All associated ACL entries are automatically removed (cascade delete)
     * This is the only test that validates ACL cleanup on product deletion.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void testDeleteProductCleansUpAcl(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        // Create product with multiple ACL entries
        Product product = addProduct("Product To Delete");

        productAclApi.grantUserAccess(product.getId(), user1.getId());
        productAclApi.grantUserAccess(product.getId(), user2.getId());

        // Verify ACL exists
        List<ProductAclEntry> acl = productAclApi.getAcl(product.getId());
        assertTrue(acl.size() >= 2);

        // Delete product
        productApi.deleteById(product.getId());

        // ACL should be cleaned up automatically (cascade delete)
        // Product no longer exists, so ACL access should fail
        List<ProductAclEntry> acl1 = productAclApi.getAcl(product.getId());
        assertEquals(0, acl1.size(), "ACL entries should be cleaned up after product deletion");
    }

    /**
     * Validates that the getAll() endpoint respects ACL permissions and only returns accessible products.
     * Tests comprehensive access filtering:
     * - Users initially see only their own created products
     * - Different users see different sets of products based on ownership
     * - After granting access, users see both owned and shared products
     * - Product list dynamically reflects current ACL state
     * This is the only test that validates the getAll() filtering mechanism comprehensively.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testGetAllOnlyReturnsProductsWithAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");

        // User2 creates another product
        setUser(user2.getEmail(), "ROLE_USER");
        Product product2 = addProduct("User2 Product");

        // User1 should only see their own product
        setUser(user1.getEmail(), "ROLE_USER");
        List<Product> user1Products = productApi.getAll();
        assertEquals(1, user1Products.size(), "User1 should only see their own product");
        assertEquals(product1.getId(), user1Products.get(0).getId());
        assertEquals("User1 Product", user1Products.get(0).getName());

        // User2 should only see their own product
        setUser(user2.getEmail(), "ROLE_USER");
        List<Product> user2Products = productApi.getAll();
        assertEquals(1, user2Products.size(), "User2 should only see their own product");
        assertEquals(product2.getId(), user2Products.get(0).getId());
        assertEquals("User2 Product", user2Products.get(0).getName());

        // Now grant user1 access to product2
        productAclApi.grantUserAccess(product2.getId(), user1.getId());

        // User1 should now see both products
        setUser(user1.getEmail(), "ROLE_USER");
        List<Product> user1ProductsAfterGrant = productApi.getAll();
        assertEquals(2, user1ProductsAfterGrant.size(), "User1 should see both products after being granted access");

        // Verify both products are in the list
        List<Long> productIds = user1ProductsAfterGrant.stream()
                .map(Product::getId)
                .toList();
        assertTrue(productIds.contains(product1.getId()), "Should contain product1");
        assertTrue(productIds.contains(product2.getId()), "Should contain product2");
    }

    /**
     * Validates that group-based ACL grants work correctly.
     * Tests group access mechanism:
     * - Admin can grant access to a user group
     * - ACL entry is properly created with groupId (not userId)
     * - All members of the group gain access to the product
     * - Access is inherited through group membership
     * This is the only test that validates the basic group-based (as opposed to user-based) ACL grant.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void testGrantGroupAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        UserGroup group = userGroupApi.getAll().getFirst();

        // Admin creates a product
        Product product = addProduct("Team Product");

        // Grant access to the group
        ProductAclEntry entry = productAclApi.grantGroupAccess(product.getId(), group.getId());
        assertNotNull(entry);
        assertEquals(group.getId(), entry.getGroupId());
        assertNull(entry.getUserId());

        // Both group members should now have access
        setUser(user1.getEmail(), "ROLE_USER");
        Product accessed1 = productApi.getById(product.getId());
        assertNotNull(accessed1);

        setUser(user2.getEmail(), "ROLE_USER");
        Product accessed2 = productApi.getById(product.getId());
        assertNotNull(accessed2);
    }

    /**
     * Validates that ACLs can contain multiple mixed entries (users and groups).
     * Tests complex ACL scenarios:
     * - Multiple groups can be granted access to the same product
     * - Individual users and groups can coexist in the same ACL
     * - All grant types (user + multiple groups) work simultaneously
     * - All granted users (direct or via group) can access the product
     * This is the only test that validates complex multi-entry ACL configurations.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void testMultipleUsersAndGroupsInAcl(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        // Create users and groups
        UserGroup group1 = userGroupApi.create("Team A", "Team A", Set.of(user1.getId()));
        UserGroup group2 = userGroupApi.create("Team B", "Team B", Set.of(user2.getId()));

        // Create product
        Product product = addProduct("Collaborative Product");

        // Grant access to individual user and multiple groups
        productAclApi.grantUserAccess(product.getId(), user3.getId());
        productAclApi.grantGroupAccess(product.getId(), group1.getId());
        productAclApi.grantGroupAccess(product.getId(), group2.getId());

        // Check ACL entries
        List<ProductAclEntry> acl = productAclApi.getAcl(product.getId());
        // Should have: creator + user3 + group1 + group2 = at least 4 entries
        assertTrue(acl.size() >= 4);

        // All users should have access
        setUser(user1.getEmail(), "ROLE_USER");
        assertNotNull(productApi.getById(product.getId()));

        setUser(user2.getEmail(), "ROLE_USER");
        assertNotNull(productApi.getById(product.getId()));

        setUser(user3.getEmail(), "ROLE_USER");
        assertNotNull(productApi.getById(product.getId()));
    }

    /**
     * Validates that group-based access can be revoked.
     * Tests group revocation workflow:
     * - Group is granted access to a product
     * - Group members can access the product
     * - Admin revokes group access
     * - Group members lose access (AccessDeniedException)
     * This is the only test that validates group-based ACL revocation.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void testRevokeGroupAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        UserGroup group = userGroupApi.getAll().getFirst();

        // Create product and grant group access
        Product product = addProduct("Team Product");
        productAclApi.grantGroupAccess(product.getId(), group.getId());

        // Verify user has access through group
        setUser(user1.getEmail(), "ROLE_USER");
        Product accessed = productApi.getById(product.getId());
        assertNotNull(accessed);

        // Revoke group access
        setUser("admin-user", "ROLE_ADMIN");
        productAclApi.revokeGroupAccess(product.getId(), group.getId());

        // User should no longer have access
        setUser(user1.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            productApi.getById(product.getId());
        });
    }

    /**
     * Validates that users granted access can themselves manage the ACL (not just the creator).
     * Tests transitive ACL management:
     * - Creator grants access to user2
     * - User2 (non-creator but has access) can grant access to user3
     * - User3 gains access through user2's grant
     * This is the only test that validates non-creator ACL management capabilities.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "kristen.hubbell@kassandra.org", roles = "USER")
    public void testUserWithAccessCanManageAcl(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        // Creator creates product
        Product product = addProduct("My Product");

        // Grant access to user2
        productAclApi.grantUserAccess(product.getId(), user2.getId());

        // User2 (who now has access) should be able to grant access to user3
        setUser(user2.getEmail(), "ROLE_USER");
        ProductAclEntry entry = productAclApi.grantUserAccess(product.getId(), user3.getId());
        assertNotNull(entry);

        // User3 should now have access
        setUser(user3.getEmail(), "ROLE_USER");
        Product accessed = productApi.getById(product.getId());
        assertNotNull(accessed);
    }

    /**
     * Validates that users without product access cannot grant access to others.
     * Tests access control enforcement on grant operations:
     * - User without access to a product attempts to grant access
     * - System rejects the operation with AccessDeniedException
     * This is the only test that validates unauthorized grant attempt prevention.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testUserWithoutAccessCannotGrantAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        // Admin creates a product
        setUser(admin1.getEmail(), "ROLE_ADMIN");
        Product product = addProduct("Admin Product");

        // Different user tries to grant access - should fail
        setUser(user1.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            productAclApi.grantUserAccess(product.getId(), user2.getId());
        });
    }

    /**
     * Validates that users without product access cannot revoke access from others.
     * Tests access control enforcement on revoke operations:
     * - Admin grants access to user1
     * - User2 (who has no access) attempts to revoke user1's access
     * - System rejects the operation with AccessDeniedException
     * This is the only test that validates unauthorized revoke attempt prevention.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testUserWithoutAccessCannotRevokeAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        // Admin creates product and grants access to user1
        setUser(admin1.getEmail(), "ROLE_ADMIN");
        Product product = addProduct("Admin Product");
        productAclApi.grantUserAccess(product.getId(), user1.getId());
        // Different user (user2) who has NO access tries to revoke user1's access - should fail
        setUser(user2.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            productAclApi.revokeUserAccess(product.getId(), user1.getId());
        });
    }

    /**
     * Validates that users without product access cannot view the product's ACL.
     * Tests access control enforcement on ACL viewing:
     * - User without access to a product attempts to view its ACL
     * - System rejects the operation with AccessDeniedException
     * This is the only test that validates unauthorized ACL viewing prevention.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testUserWithoutAccessCannotViewAcl(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        // Admin creates a product
        setUser(admin1.getEmail(), "ROLE_ADMIN");
        Product product = addProduct("Admin Product");

        // Different user tries to view ACL - should fail
        setUser(user2.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            productAclApi.getAcl(product.getId());
        });
    }
}

