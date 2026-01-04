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

package de.bushnaq.abdalla.kassandra.ui.view;

import de.bushnaq.abdalla.kassandra.dao.UserDAO;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.UserGroup;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.util.ProductListViewTester;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * Integration test for the ProductListView UI component.
 * Tests create, edit, and delete operations for products in the UI.
 * <p>
 * These tests use {@link ProductListViewTester} to interact with the UI elements
 * and verify the expected behavior.
 */
@Tag("IntegrationUiTest")
@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=${test.server.port:0}",
                "spring.profiles.active=test",
                "spring.security.basic.enabled=false"// Disable basic authentication for these tests
        }
)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ProductListViewTest extends AbstractKeycloakUiTestUtil {
    private       User                     admin1;
    private final String                   name    = "Product-2";
    private final String                   newName = "NewProduct-2";
    @Autowired
    private       ProductListViewTester    productListViewTester;
    @Autowired
    private       HumanizedSeleniumHandler seleniumHandler;
    private       User                     user1;
    private       User                     user2;
    private       User                     user3;

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

    @BeforeEach
    public void setupTest(TestInfo testInfo) throws Exception {
        Optional<UserDAO> byEmail = userRepository.findByEmail("christopher.paul@kassandra.org");//debugging code


        productListViewTester.switchToProductListViewWithOidc(
                "christopher.paul@kassandra.org",
                "password",
                null,
                testInfo.getTestClass().get().getSimpleName(),
                generateTestCaseName(testInfo)
        );
    }

    /**
     * Tests the behavior when creating a product but canceling the operation.
     * <p>
     * Verifies that when a user clicks the create product button, enters a name, and then
     * cancels the operation, no product is created in the list.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateCancel() throws Exception {
        productListViewTester.createProductCancel(name);
    }

    /**
     * Tests the behavior when successfully creating a product.
     * <p>
     * Verifies that when a user clicks the create product button, enters a name, and confirms
     * the creation, the product appears in the list.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateConfirm() throws Exception {
        productListViewTester.createProductConfirm(name);
    }

    /**
     * Tests the behavior when attempting to create a product with a name that already exists.
     * <p>
     * Creates a product, then tries to create another product with the same name.
     * Verifies that the system displays an error and prevents the creation of the duplicate product.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateDuplicateNameFails() throws Exception {
        // First, create a product
        productListViewTester.createProductConfirm(name);
        // Then try to create another product with the same name
        productListViewTester.createProductWithDuplicateName(name);
    }

    /**
     * Tests creating a product with ACL assignments for groups.
     * <p>
     * Creates a product and grants access to specific groups during creation.
     * Verifies that the product appears with the correct ACL display.
     *
     * @throws Exception if any error occurs during the test
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateProductWithGroupAcl(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        String    productName = "Product-WithGroupAcl";
        UserGroup ug1         = userGroupApi.create("Developers", "well, what do you think?", new HashSet<>(List.of(user1.getId(), user2.getId())));
        UserGroup ug2         = userGroupApi.create("QA Team", "well, what do you think?", new HashSet<>(List.of(user2.getId(), user3.getId())));
        String[]  groups      = {ug1.getName(), ug2.getName()};

        productListViewTester.createProductWithAcl(productName, null, groups);
        productListViewTester.verifyProductAclDisplay(productName, -1, 2);
    }

    /**
     * Tests creating a product with both user and group ACL assignments.
     * <p>
     * Creates a product and grants access to both users and groups during creation.
     * Verifies that the product displays both counts in the ACL column.
     *
     * @throws Exception if any error occurs during the test
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateProductWithMixedAcl(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        String    productName = "Product-WithMixedAcl";
        UserGroup ug1         = userGroupApi.create("Developers", "well, what do you think?", new HashSet<>(List.of(user1.getId(), user2.getId())));
        String[]  users       = {user3.getName()};
        String[]  groups      = {ug1.getName()};

        productListViewTester.createProductWithAcl(productName, users, groups);
        productListViewTester.verifyProductAclDisplay(productName, 2, 1);
    }

    /**
     * Tests creating a product with ACL assignments for users.
     * <p>
     * Creates a product and grants access to specific users during creation.
     * Verifies that the product appears with the correct ACL display.
     *
     * @throws Exception if any error occurs during the test
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateProductWithUserAcl(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        String   productName = "Product-WithUserAcl";
        String[] users       = {user1.getName(), user2.getName()};

        productListViewTester.createProductWithAcl(productName, users, null);
        productListViewTester.verifyProductAclDisplay(productName, 3, -1);
    }

    /**
     * Tests creating a product without ACL assignments.
     * <p>
     * Creates a product without granting access to any users or groups.
     * Verifies that the ACL column shows "Owner only".
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateProductWithoutAcl() throws Exception {
        String productName = "Product-OwnerOnly";

        productListViewTester.createProductWithAcl(productName, null, null);
        productListViewTester.verifyProductAclDisplay(productName, 1, -1);
    }

    /**
     * Tests the behavior when attempting to delete a product but canceling the operation.
     * <p>
     * Creates a product, then attempts to delete it but cancels the confirmation dialog.
     * Verifies that the product remains in the list.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDeleteCancel() throws Exception {
        productListViewTester.createProductConfirm(name);
        productListViewTester.deleteProductCancel(name);
    }

    /**
     * Tests the behavior when successfully deleting a product.
     * <p>
     * Creates a product, then deletes it by confirming the deletion in the confirmation dialog.
     * Verifies that the product is removed from the list.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDeleteConfirm() throws Exception {
        productListViewTester.createProductConfirm(name);
        productListViewTester.deleteProductConfirm(name);
    }

    /**
     * Tests the behavior when attempting to edit a product but canceling the operation.
     * <p>
     * Creates a product, attempts to edit its name, but cancels the edit dialog.
     * Verifies that the original name remains unchanged and the new name is not present.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditCancel() throws Exception {
        productListViewTester.createProductConfirm(name);
        productListViewTester.editProductCancel(name, newName);
    }

    /**
     * Tests the behavior when successfully editing a product.
     * <p>
     * Creates a product, edits its name, and confirms the edit.
     * Verifies that the product with the new name appears in the list and the old name is removed.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditConfirm() throws Exception {
        productListViewTester.createProductConfirm(name);
        productListViewTester.editProductConfirm(name, newName);
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditDuplicateNameFails() throws Exception {
        productListViewTester.createProductConfirm(name);
        productListViewTester.createProductConfirm(newName);
        productListViewTester.editProductWithDuplicateNameFails(name, newName);
    }

    /**
     * Tests adding ACL assignments to an existing product.
     * <p>
     * Creates a product, then edits it to add user and group access.
     * Verifies that the ACL display is updated correctly.
     *
     * @throws Exception if any error occurs during the test
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditProductAddAcl(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        String    productName = "Product-ToAddAcl";
        UserGroup ug1         = userGroupApi.create("Developers", "well, what do you think?", new HashSet<>(List.of(user1.getId(), user2.getId())));
        String[]  groups      = {ug1.getName()};
        String[]  users       = {user3.getName()};

        // Create product without ACL
        productListViewTester.createProductConfirm(productName);
        productListViewTester.verifyProductAclDisplay(productName, 1, -1);

        // Add ACL
        productListViewTester.editProductAddAcl(productName, users, groups);
        seleniumHandler.wait(200);
        productListViewTester.verifyProductAclDisplay(productName, 2, 1);
    }

    /**
     * Tests modifying ACL assignments on an existing product.
     * <p>
     * Creates a product with initial ACL, then edits to change the users/groups.
     * Verifies that the ACL display reflects the changes.
     *
     * @throws Exception if any error occurs during the test
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditProductModifyAcl(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        String   productName     = "Product-ToModifyAcl";
        String[] users           = {user3.getName()};
        String[] initialUsers    = {user1.getName()};
        String[] additionalUsers = {user2.getName(), user3.getName()};

        // Create with initial ACL
        productListViewTester.createProductWithAcl(productName, initialUsers, null);
        productListViewTester.verifyProductAclDisplay(productName, 2, -1);

        // Add more users
        productListViewTester.editProductAddAcl(productName, additionalUsers, null);
        seleniumHandler.wait(200);
        productListViewTester.verifyProductAclDisplay(productName, 4, -1);
    }

    /**
     * Tests removing ACL assignments from an existing product.
     * <p>
     * Creates a product with ACL, then edits it to remove access.
     * Verifies that the ACL display returns to "Owner only".
     *
     * @throws Exception if any error occurs during the test
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditProductRemoveAcl(RandomCase randomCase, TestInfo testInfo) throws Exception {
        init(randomCase, testInfo);
        String    productName = "Product-ToRemoveAcl";
        UserGroup ug1         = userGroupApi.create("Developers", "well, what do you think?", new HashSet<>(List.of(user1.getId(), user2.getId())));
        String[]  users       = {user3.getName()};
        String[]  groups      = {ug1.getName()};

        // Create product with ACL
        productListViewTester.createProductWithAcl(productName, users, groups);
        productListViewTester.verifyProductAclDisplay(productName, 2, 1);

        // Remove ACL
        productListViewTester.editProductRemoveAcl(productName, users, groups);
        seleniumHandler.wait(200);
        productListViewTester.verifyProductAclDisplay(productName, 1, -1);
    }
}
