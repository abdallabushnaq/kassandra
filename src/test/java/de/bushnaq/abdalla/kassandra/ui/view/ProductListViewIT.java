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
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.dialog.ProductDialog;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.util.AboutViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.ProductListViewTester;
import de.bushnaq.abdalla.kassandra.util.PersistingEntityGenerator;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for the ProductListView UI component.
 * Tests create, edit, and delete operations for products in the UI.
 * <p>
 * These tests use {@link ProductListViewTester} to interact with the UI elements
 * and verify the expected behavior.
 */
@Tag("IntegrationUiTest")
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=${test.server.port:0}",
                "spring.security.basic.enabled=false"// Disable basic authentication for these tests
        }
)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ProductListViewIT extends AbstractKeycloakUiTestUtil {
    @Autowired
    private ProductListViewTester ProductListViewTester;
    @Autowired
    AboutViewTester aboutViewTester;
    private       User                     admin1;
    private final String                   name    = "Product-2";
    private final String                   newName = "NewProduct-2";
    @Autowired
    private       HumanizedSeleniumHandler seleniumHandler;
    private       User                     user1;
    private       User                     user2;
    private       User                     user3;

    private void init(RandomCase randomCase, TestInfo testInfo) throws Exception {
        Authentication roleAdmin = PersistingEntityGenerator.setUser("admin-user", "ROLE_ADMIN");
        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateProductsIfNeeded(testInfo, randomCase);
        admin1 = peg.userApi.getByEmail("christopher.paul@kassandra.org").get();
        user1  = peg.userApi.getByEmail("kristen.hubbell@kassandra.org").get();
        user2  = peg.userApi.getByEmail("claudine.fick@kassandra.org").get();
        user3  = peg.userApi.getByEmail("randy.asmus@kassandra.org").get();

        PersistingEntityGenerator.setUser(roleAdmin);
    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(1, OffsetDateTime.parse("2025-08-11T08:00:00+01:00"), LocalDate.parse("2025-08-04"), Duration.ofDays(10), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 4, 0, 13)//
        };
        return Arrays.stream(randomCases).toList();
    }

    @BeforeEach
    public void setupTest(TestInfo testInfo) throws Exception {
        Optional<UserDAO> byEmail = userRepository.findByEmail("christopher.paul@kassandra.org");//debugging code


        aboutViewTester.login(
                "christopher.paul@kassandra.org",
                "password",
                null,
                testInfo.getTestClass().get().getSimpleName(),
                generateTestCaseName(testInfo)
        );
        ProductListViewTester.switchToProductListView();
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
        ProductListViewTester.createProductCancel(name);
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
        ProductListViewTester.createProductConfirm(name);
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
        ProductListViewTester.createProductConfirm(name);
        // Then try to create another product with the same name
        ProductListViewTester.createProductWithDuplicateName(name);
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
        UserGroup ug1         = peg.userGroupApi.create("Developers", "well, what do you think?", new HashSet<>(List.of(user1.getId(), user2.getId())));
        UserGroup ug2         = peg.userGroupApi.create("QA Team", "well, what do you think?", new HashSet<>(List.of(user2.getId(), user3.getId())));
        String[]  groups      = {ug1.getName(), ug2.getName()};

        ProductListViewTester.createProductWithAcl(productName, null, groups);
        ProductListViewTester.verifyProductAclDisplay(productName, -1, 2);
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
        UserGroup ug1         = peg.userGroupApi.create("Developers", "well, what do you think?", new HashSet<>(List.of(user1.getId(), user2.getId())));
        String[]  users       = {user3.getName()};
        String[]  groups      = {ug1.getName()};

        ProductListViewTester.createProductWithAcl(productName, users, groups);
        ProductListViewTester.verifyProductAclDisplay(productName, 2, 1);
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

        ProductListViewTester.createProductWithAcl(productName, users, null);
        ProductListViewTester.verifyProductAclDisplay(productName, 3, -1);
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

        ProductListViewTester.createProductWithAcl(productName, null, null);
        ProductListViewTester.verifyProductAclDisplay(productName, 1, -1);
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
        ProductListViewTester.createProductConfirm(name);
        ProductListViewTester.deleteProductCancel(name);
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
        ProductListViewTester.createProductConfirm(name);
        ProductListViewTester.deleteProductConfirm(name);
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
        ProductListViewTester.createProductConfirm(name);
        ProductListViewTester.editProductCancel(name, newName);
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
        ProductListViewTester.createProductConfirm(name);
        ProductListViewTester.editProductConfirm(name, newName);
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditDuplicateNameFails() throws Exception {
        ProductListViewTester.createProductConfirm(name);
        ProductListViewTester.createProductConfirm(newName);
        ProductListViewTester.editProductWithDuplicateNameFails(name, newName);
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
        UserGroup ug1         = peg.userGroupApi.create("Developers", "well, what do you think?", new HashSet<>(List.of(user1.getId(), user2.getId())));
        String[]  groups      = {ug1.getName()};
        String[]  users       = {user3.getName()};

        // Create product without ACL
        ProductListViewTester.createProductConfirm(productName);
        ProductListViewTester.verifyProductAclDisplay(productName, 1, -1);

        // Add ACL
        ProductListViewTester.editProductAddAcl(productName, users, groups);
        seleniumHandler.wait(200);
        ProductListViewTester.verifyProductAclDisplay(productName, 2, 1);
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
        ProductListViewTester.createProductWithAcl(productName, initialUsers, null);
        ProductListViewTester.verifyProductAclDisplay(productName, 2, -1);

        // Add more users
        ProductListViewTester.editProductAddAcl(productName, additionalUsers, null);
        seleniumHandler.wait(200);
        ProductListViewTester.verifyProductAclDisplay(productName, 4, -1);
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
        UserGroup ug1         = peg.userGroupApi.create("Developers", "well, what do you think?", new HashSet<>(List.of(user1.getId(), user2.getId())));
        String[]  users       = {user3.getName()};
        String[]  groups      = {ug1.getName()};

        // Create product with ACL
        ProductListViewTester.createProductWithAcl(productName, users, groups);
        ProductListViewTester.verifyProductAclDisplay(productName, 2, 1);

        // Remove ACL
        ProductListViewTester.editProductRemoveAcl(productName, users, groups);
        seleniumHandler.wait(200);
        ProductListViewTester.verifyProductAclDisplay(productName, 1, -1);
    }

    /**
     * Exercises Products help, live diagrams, themed images and keyboard closing.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testHelpDrawer() {
        WebDriver          driver     = seleniumHandler.getDriver();
        WebDriverWait      wait       = new WebDriverWait(driver, Duration.ofSeconds(30));
        JavascriptExecutor javascript = (JavascriptExecutor) driver;
        By                 toggle     = By.id(ProductListView.PRODUCT_HELP_BUTTON);
        By                 drawer     = By.id(ProductListView.PRODUCT_HELP_BUTTON + "-drawer");
        By                 diagram    = By.cssSelector(".help-drawer .help-diagram svg");
        By                 close      = By.cssSelector(".help-drawer vaadin-button[aria-label='Close help']");
        wait.until(ExpectedConditions.elementToBeClickable(toggle)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(drawer));
        By chapters = By.cssSelector("#" + ProductListView.PRODUCT_HELP_BUTTON + "-drawer vaadin-details.help-chapter");
        wait.until(webDriver -> !driver.findElements(chapters).isEmpty());
        assertTrue(driver.findElements(chapters).stream().noneMatch(chapter -> chapter.getDomAttribute("opened") != null));
        driver.findElement(By.xpath("//*[@id='" + ProductListView.PRODUCT_HELP_BUTTON
                + "-drawer']//vaadin-button[normalize-space(.)='Expand all']")).click();
        wait.until(webDriver -> driver.findElements(chapters).stream().allMatch(chapter -> chapter.getDomAttribute("opened") != null));
        wait.until(ExpectedConditions.presenceOfElementLocated(diagram));
        assertEquals("true", driver.findElement(toggle).getDomAttribute("aria-expanded"));
        String helpText = driver.findElement(drawer).getDomProperty("textContent");
        assertTrue(helpText.contains("creator receives a user grant"), helpText);
        assertTrue(helpText.contains("Actions"), helpText);
        assertTrue(helpText.contains("Use the main menu and header"), helpText);
        assertTrue(helpText.contains("Recover changes"), helpText);
        new WebDriverWait(driver, Duration.ofSeconds(10)).withMessage(() -> String.valueOf(javascript.executeScript(
                        "return JSON.stringify({base: document.baseURI, images: [...document.querySelectorAll('.help-drawer img')]"
                                + ".map(i => ({src: i.src, complete: i.complete, width: i.naturalWidth}))})")))
                .until(webDriver -> Boolean.TRUE.equals(javascript.executeScript(
                        "return [...document.querySelectorAll('.help-drawer img')].length > 1"
                                + " && [...document.querySelectorAll('.help-drawer img')].every(i => i.complete && i.naturalWidth > 0)")));

        String previousDiagram = driver.findElement(diagram).getDomAttribute("id");
        assertEquals(Boolean.TRUE, javascript.executeScript(
                "return [...document.querySelectorAll('.help-drawer img')]"
                        + ".every(image => image.getBoundingClientRect().width <= image.naturalWidth)"));
        javascript.executeScript("document.getElementById(arguments[0]).click()", MainLayout.ID_THEME_TOGGLE);
        wait.until(webDriver -> !previousDiagram.equals(driver.findElement(diagram).getDomAttribute("id")));
        wait.withMessage(() -> String.valueOf(javascript.executeScript(
                        "return JSON.stringify({body: document.body.getAttribute('theme'), html: document.documentElement.getAttribute('theme'),"
                                + " images: [...document.querySelectorAll('.help-drawer img')]"
                                + ".map(i => ({src: i.src, title: i.title, complete: i.complete, width: i.naturalWidth}))})")))
                .until(webDriver -> Boolean.TRUE.equals(javascript.executeScript(
                        "const dark = document.body.matches('[theme~=dark]') || document.documentElement.matches('[theme~=dark]');"
                                + " return [...document.querySelectorAll('.help-drawer img')].every(i =>"
                                + " i.src.includes(dark ? '/dark/' : '/light/') && i.complete && i.naturalWidth > 0)")));

        Dimension originalSize = driver.manage().window().getSize();
        try {
            driver.manage().window().setSize(new Dimension(390, 800));
            assertEquals(Boolean.TRUE, javascript.executeScript(
                    "const overlay = document.querySelector('.help-drawer').shadowRoot.querySelector('vaadin-dialog-overlay')"
                            + ".shadowRoot.querySelector('[part~=overlay]');"
                            + " return overlay.getBoundingClientRect().width <= window.innerWidth"));
        } finally {
            driver.manage().window().setSize(originalSize);
        }
        driver.findElement(By.xpath("//*[@id='" + ProductListView.PRODUCT_HELP_BUTTON
                + "-drawer']//vaadin-button[normalize-space(.)='Collapse all']")).click();
        wait.until(webDriver -> driver.findElements(chapters).stream().noneMatch(chapter -> chapter.getDomAttribute("opened") != null));
        driver.findElement(close).sendKeys(Keys.ESCAPE);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(drawer));
        assertEquals("false", driver.findElement(toggle).getDomAttribute("aria-expanded"));
        wait.until(webDriver -> Boolean.TRUE.equals(javascript.executeScript(
                "return document.activeElement.id === arguments[0]", ProductListView.PRODUCT_HELP_BUTTON)));
        driver.findElement(toggle).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(drawer));
        wait.until(ExpectedConditions.presenceOfElementLocated(diagram));
        driver.findElement(close).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(drawer));
    }

    /**
     * Exercises help above the product form without losing unsaved input.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testProductDialogHelp() {
        WebDriver          driver     = seleniumHandler.getDriver();
        WebDriverWait      wait       = new WebDriverWait(driver, Duration.ofSeconds(30));
        JavascriptExecutor javascript = (JavascriptExecutor) driver;
        By                 form       = By.id(ProductDialog.PRODUCT_DIALOG);
        By                 toggle     = By.id(ProductDialog.PRODUCT_HELP_BUTTON);
        By                 drawer     = By.id(ProductDialog.PRODUCT_HELP_BUTTON + "-drawer");
        By close = By.cssSelector("#" + ProductDialog.PRODUCT_HELP_BUTTON
                + "-drawer vaadin-button[aria-label='Close help']");
        wait.until(ExpectedConditions.elementToBeClickable(By.id(ProductListView.CREATE_PRODUCT_BUTTON))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(form));
        seleniumHandler.setTextField(ProductDialog.PRODUCT_NAME_FIELD, "Jupiter help example");
        driver.findElement(toggle).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(drawer));
        assertTrue(driver.findElement(drawer).getText().contains("Who has access by default?"));
        assertTrue(driver.findElement(drawer).getText().contains("Administrators can access all"));
        wait.until(webDriver -> Boolean.TRUE.equals(javascript.executeScript(
                "const images = [...document.querySelectorAll('#' + arguments[0] + ' img')];"
                        + " return images.length === 1 && images.every(i => i.complete && i.naturalWidth > 0)",
                ProductDialog.PRODUCT_HELP_BUTTON + "-drawer")));
        driver.findElement(close).sendKeys(Keys.ESCAPE);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(drawer));
        assertTrue(driver.findElement(form).isDisplayed());
        assertEquals("Jupiter help example",
                driver.findElement(By.id(ProductDialog.PRODUCT_NAME_FIELD)).getDomProperty("value"));
        driver.findElement(toggle).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(drawer));
        javascript.executeScript("document.getElementById(arguments[0]).click()", ProductDialog.CANCEL_BUTTON);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(form));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(drawer));
    }
}
