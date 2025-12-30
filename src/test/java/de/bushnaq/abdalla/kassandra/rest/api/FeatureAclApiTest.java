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

import de.bushnaq.abdalla.kassandra.dto.Feature;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
 * Test ACL-based access control for Features.
 * Features inherit access control from their parent Version's Product.
 */
@Tag("UnitTest")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class FeatureAclApiTest extends AbstractUiTestUtil {

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
     * Verifies that admin users have unrestricted access to all features regardless of ownership.
     * Tests admin capability to:
     * - View all features via getAll()
     * - Retrieve specific features by ID (across different products)
     * - Update any feature
     * - Delete any feature
     * This is the only test that validates full admin privileges on features.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "christopher.paul@kassandra.org", roles = "ADMIN")
    public void testAdminCanAccessAllFeatures(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version and feature
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");
        Feature feature1 = addFeature(version1, "Feature 1");

        // User2 creates a product with a version and feature
        setUser(user2.getEmail(), "ROLE_USER");
        Product product2 = addProduct("User2 Product");
        Version version2 = addVersion(product2, "Version 2.0");
        Feature feature2 = addFeature(version2, "Feature 2");

        // Admin can access all features
        setUser(admin1.getEmail(), "ROLE_ADMIN");
        List<Feature> allFeatures = featureApi.getAll();
        assertEquals(2, allFeatures.size(), "Admin should see all features");

        // Admin can get specific features
        Feature retrieved1 = featureApi.getById(feature1.getId());
        assertNotNull(retrieved1);
        assertEquals(feature1.getId(), retrieved1.getId());

        Feature retrieved2 = featureApi.getById(feature2.getId());
        assertNotNull(retrieved2);
        assertEquals(feature2.getId(), retrieved2.getId());

        // Admin can update any feature
        feature1.setName("Updated by Admin");
        featureApi.update(feature1);

        // Admin can delete any feature
        featureApi.deleteById(feature2.getId());
    }

    /**
     * Validates that the getAll(versionId) endpoint enforces ACL permissions.
     * Specifically tests:
     * - Users can retrieve all features for versions they own
     * - Users cannot retrieve features for versions they don't have access to (AccessDeniedException)
     * - After being granted access, users can retrieve features for that version
     * This is the only test that focuses on the version-scoped feature listing endpoint.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testGetAllByVersionIdRespectsAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version and multiple features
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1  = addProduct("User1 Product");
        Version version1  = addVersion(product1, "Version 1.0");
        Feature feature1a = addFeature(version1, "Feature 1A");
        Feature feature1b = addFeature(version1, "Feature 1B");

        // User1 can get all features for their version
        List<Feature> features = featureApi.getAll(version1.getId());
        assertEquals(2, features.size(), "User1 should see all features of their version");

        // User2 cannot get features for user1's version
        setUser(user2.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            featureApi.getAll(version1.getId());
        });

        // After granting access, user2 can get features
        setUser(user1.getEmail(), "ROLE_USER");
        productAclApi.grantUserAccess(product1.getId(), user2.getId());

        setUser(user2.getEmail(), "ROLE_USER");
        List<Feature> featuresAfterGrant = featureApi.getAll(version1.getId());
        assertEquals(2, featuresAfterGrant.size(), "User2 should see all features after being granted access");
    }

    /**
     * Validates that group-based ACL grants work correctly for feature access.
     * Tests the inheritance chain: Product ACL → Version → Feature access via group membership.
     * Specifically validates:
     * - Users initially cannot access features of products they don't own
     * - Granting group access to a product enables all group members to access its features
     * - Group members can see the features in both getById() and getAll() operations
     * This is the only test that validates group-based (as opposed to direct user-based) ACL grants for features.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testGroupAccessToProductGrantsFeatureAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // Admin creates a group with user1
        setUser(admin1.getEmail(), "ROLE_ADMIN");
        var group = userGroupApi.create("Dev Team", "Development Team", java.util.Set.of(user1.getId()));

        // User2 creates a product with a version and feature
        setUser(user2.getEmail(), "ROLE_USER");
        Product product = addProduct("Team Product");
        Version version = addVersion(product, "Version 1.0");
        Feature feature = addFeature(version, "Feature 1");

        // User1 cannot access the feature
        setUser(user1.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            featureApi.getById(feature.getId());
        });

        // User2 grants group access to the product
        setUser(user2.getEmail(), "ROLE_USER");
        productAclApi.grantGroupAccess(product.getId(), group.getId());

        // Now user1 can access the feature (via group membership)
        setUser(user1.getEmail(), "ROLE_USER");
        Feature retrieved = featureApi.getById(feature.getId());
        assertNotNull(retrieved);
        assertEquals(feature.getId(), retrieved.getId());

        // User1 can also see it in getAll()
        List<Feature> allFeatures = featureApi.getAll();
        assertTrue(allFeatures.stream().anyMatch(f -> f.getId().equals(feature.getId())), "User1 should see the feature via group access");
    }

    /**
     * Validates that revoking product access properly cascades to feature access denial.
     * Tests the ACL revocation workflow:
     * - User is granted access to a product and can access its features
     * - Product access is revoked
     * - User can no longer access any features of that product
     * This is the only test that validates the ACL revocation mechanism for features.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testRevokeProductAccessRevokesFeatureAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version and feature
        setUser(user1.getEmail(), "ROLE_USER");
        Product product = addProduct("User1 Product");
        Version version = addVersion(product, "Version 1.0");
        Feature feature = addFeature(version, "Feature 1");

        // User1 grants user2 access to the product
        productAclApi.grantUserAccess(product.getId(), user2.getId());

        // User2 can access the feature
        setUser(user2.getEmail(), "ROLE_USER");
        Feature retrieved = featureApi.getById(feature.getId());
        assertNotNull(retrieved);

        // User1 revokes user2's access
        setUser(user1.getEmail(), "ROLE_USER");
        productAclApi.revokeUserAccess(product.getId(), user2.getId());

        // User2 can no longer access the feature
        setUser(user2.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            featureApi.getById(feature.getId());
        });
    }

    /**
     * Validates that granting product access enables feature-level access via getById().
     * Tests the positive grant workflow:
     * - User initially cannot access another user's feature by ID
     * - After being granted product access, user can retrieve the feature by ID
     * This is the only test that validates the grant workflow for individual feature retrieval.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testUserCanAccessFeatureAfterProductAccessGranted(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version and feature
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");
        Feature feature1 = addFeature(version1, "Feature 1");

        // User2 creates a product with a version and feature
        setUser(user2.getEmail(), "ROLE_USER");
        Product product2 = addProduct("User2 Product");
        Version version2 = addVersion(product2, "Version 2.0");
        Feature feature2 = addFeature(version2, "Feature 2");

        // User1 cannot access feature2
        setUser(user1.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            featureApi.getById(feature2.getId());
        });

        // User2 grants user1 access to product2
        setUser(user2.getEmail(), "ROLE_USER");
        productAclApi.grantUserAccess(product2.getId(), user1.getId());

        // Now user1 can access feature2
        setUser(user1.getEmail(), "ROLE_USER");
        Feature retrieved = featureApi.getById(feature2.getId());
        assertNotNull(retrieved);
        assertEquals(feature2.getId(), retrieved.getId());
    }

    /**
     * Validates basic ownership-based access control for features.
     * Tests the fundamental ACL behavior:
     * - Users can access their own features via getById() and getAll()
     * - Users cannot access other users' features (AccessDeniedException)
     * - getAll() only returns features the user has access to
     * This is the only test that validates basic ownership isolation without any ACL grants for features.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testUserCanOnlyAccessFeaturesOfOwnedProducts(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version and feature
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");
        Feature feature1 = addFeature(version1, "Feature 1");

        // User2 creates a product with a version and feature
        setUser(user2.getEmail(), "ROLE_USER");
        Product product2 = addProduct("User2 Product");
        Version version2 = addVersion(product2, "Version 2.0");
        Feature feature2 = addFeature(version2, "Feature 2");

        // User1 can access their own feature
        setUser(user1.getEmail(), "ROLE_USER");
        Feature retrieved1 = featureApi.getById(feature1.getId());
        assertNotNull(retrieved1);
        assertEquals(feature1.getId(), retrieved1.getId());

        // User1 cannot access user2's feature
        assertThrows(AccessDeniedException.class, () -> {
            featureApi.getById(feature2.getId());
        });

        // User1 can only see their own features in getAll()
        List<Feature> user1Features = featureApi.getAll();
        assertEquals(1, user1Features.size(), "User1 should only see their own features");
        assertEquals(feature1.getId(), user1Features.getFirst().getId());

        // User2 can access their own feature
        setUser(user2.getEmail(), "ROLE_USER");
        Feature retrieved2 = featureApi.getById(feature2.getId());
        assertNotNull(retrieved2);
        assertEquals(feature2.getId(), retrieved2.getId());

        // User2 cannot access user1's feature
        assertThrows(AccessDeniedException.class, () -> {
            featureApi.getById(feature1.getId());
        });
    }

    /**
     * Validates that feature creation is restricted by product ACL.
     * Tests that:
     * - Users cannot create features for versions of products they don't have access to
     * - Create operation properly enforces access control at the product level
     * This is the only test that specifically validates CREATE operation access control for features.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testUserCannotCreateFeatureForVersionWithoutAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");

        // User2 tries to create a feature for user1's version - should fail
        setUser(user2.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            addFeature(version1, "Unauthorized Feature");
        });
    }

    /**
     * Validates that feature deletion is restricted by product ACL.
     * Tests that:
     * - Users cannot delete features of products they don't have access to
     * - Delete operation properly enforces access control
     * This is the only test that specifically validates DELETE operation access control for features.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testUserCannotDeleteFeatureWithoutAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version and feature
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");
        Feature feature1 = addFeature(version1, "Feature 1");

        // User2 tries to delete user1's feature - should fail
        setUser(user2.getEmail(), "ROLE_USER");
        assertThrows(AccessDeniedException.class, () -> {
            featureApi.deleteById(feature1.getId());
        });
    }

    /**
     * Validates that feature updates are restricted by product ACL.
     * Tests that:
     * - Users cannot update features of products they don't have access to
     * - Update operation properly enforces access control
     * This is the only test that specifically validates UPDATE operation access control for features.
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    public void testUserCannotUpdateFeatureWithoutAccess(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);

        // User1 creates a product with a version and feature
        setUser(user1.getEmail(), "ROLE_USER");
        Product product1 = addProduct("User1 Product");
        Version version1 = addVersion(product1, "Version 1.0");
        Feature feature1 = addFeature(version1, "Feature 1");

        // User2 tries to update user1's feature - should fail
        setUser(user2.getEmail(), "ROLE_USER");
        feature1.setName("Hacked Feature");
        assertThrows(AccessDeniedException.class, () -> {
            featureApi.update(feature1);
        });
    }
}

