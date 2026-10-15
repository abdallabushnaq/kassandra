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

import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.util.AboutViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.FeatureListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.ProductListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.VersionListViewTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for the ProjectListView UI component.
 * Tests create, edit, and delete operations for projects in the UI.
 * <p>
 * These tests use {@link FeatureListViewTester} to interact with the UI elements
 * and verify the expected behavior. Each test requires a product and version
 * to be created first, as projects exist within the context of a version.
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
public class FeatureListViewIT extends AbstractKeycloakUiTestUtil {
    @Autowired
    AboutViewTester aboutViewTester;
    @Autowired
    private       FeatureListViewTester    FeatureListViewTester;
    private final String                   featureName    = "Project-2";
    private final String                   newProjectName = "NewProject-2";
    @Autowired
    private       ProductListViewTester    productListViewTester;
    private final String                   productName    = "Product-2";
    @Autowired
    private       HumanizedSeleniumHandler seleniumHandler;
    @Autowired
    private       VersionListViewTester    versionListViewTester;
    private final String                   versionName    = "Version-2";

    /**
     * Setup method that runs before each test.
     * <p>
     * Creates a product, selects it, then creates a version and selects it.
     * This establishes the required hierarchy for testing project operations,
     * as projects exist within versions, which exist within products.
     *
     * @throws Exception if any error occurs during setup
     */
    @BeforeEach
    public void setupTest(TestInfo testInfo) throws Exception {
        // Navigate to product list and create a product
        aboutViewTester.login(
                "christopher.paul@kassandra.org",
                "password",
                null,
                testInfo.getTestClass().get().getSimpleName(),
                generateTestCaseName(testInfo)
        );
//        seleniumHandler.startRecording(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
        productListViewTester.switchToProductListView();
        productListViewTester.createProductConfirm(productName);
        productListViewTester.selectProduct(productName);

        // Create a version
        versionListViewTester.createVersionConfirm(versionName);
        versionListViewTester.selectVersion(versionName);
    }

    /**
     * Exercises collapsible Features help, element images, themes and narrow screens.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testElementHelp() {
        WebDriver          driver     = seleniumHandler.getDriver();
        WebDriverWait      wait       = new WebDriverWait(driver, Duration.ofSeconds(30));
        JavascriptExecutor javascript = (JavascriptExecutor) driver;
        By                 toggle     = By.id(FeatureListView.FEATURE_HELP_BUTTON);
        By                 drawer     = By.id(FeatureListView.FEATURE_HELP_BUTTON + "-drawer");
        By close = By.cssSelector("#" + FeatureListView.FEATURE_HELP_BUTTON
                + "-drawer vaadin-button[aria-label='Close help']");
        driver.findElement(toggle).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(drawer));
        By chapters = By.cssSelector("#" + FeatureListView.FEATURE_HELP_BUTTON + "-drawer vaadin-details.help-chapter");
        wait.until(webDriver -> driver.findElements(chapters).size() == 6);
        assertTrue(driver.findElements(chapters).stream().noneMatch(chapter -> chapter.getDomAttribute("opened") != null));
        assertTrue(driver.findElement(drawer).getText().contains("A feature groups related work"));
        assertTrue(driver.findElement(drawer).getText().contains("Use the main menu and header"));
        wait.until(webDriver -> Boolean.TRUE.equals(javascript.executeScript(
                "return [...document.querySelectorAll('#' + arguments[0] + ' vaadin-details vaadin-markdown')]"
                        + ".every(markdown => markdown.getClientRects().length === 0)",
                FeatureListView.FEATURE_HELP_BUTTON + "-drawer")));

        WebElement firstChapter = driver.findElements(chapters).getFirst();
        firstChapter.findElement(By.tagName("h2")).click();
        wait.until(webDriver -> firstChapter.getDomAttribute("opened") != null);
        assertTrue(driver.findElements(chapters).stream().skip(1).noneMatch(chapter -> chapter.getDomAttribute("opened") != null));
        WebElement summary = firstChapter.findElement(By.tagName("vaadin-details-summary"));
        summary.sendKeys(Keys.SPACE);
        wait.until(webDriver -> firstChapter.getDomAttribute("opened") == null);
        summary.sendKeys(Keys.ENTER);
        wait.until(webDriver -> firstChapter.getDomAttribute("opened") != null);

        By expand   = By.xpath("//*[@id='" + FeatureListView.FEATURE_HELP_BUTTON + "-drawer']//vaadin-button[normalize-space(.)='Expand all']");
        By collapse = By.xpath("//*[@id='" + FeatureListView.FEATURE_HELP_BUTTON + "-drawer']//vaadin-button[normalize-space(.)='Collapse all']");
        driver.findElement(expand).click();
        wait.until(webDriver -> driver.findElements(chapters).stream().allMatch(chapter -> chapter.getDomAttribute("opened") != null));
        wait.until(webDriver -> Boolean.TRUE.equals(javascript.executeScript(
                "return [...document.querySelectorAll('#' + arguments[0] + ' vaadin-details vaadin-markdown')]"
                        + ".every(markdown => markdown.getBoundingClientRect().height > 0)",
                FeatureListView.FEATURE_HELP_BUTTON + "-drawer")));
        String text = driver.findElement(drawer).getDomProperty("textContent").replaceAll("\\s+", " ");
        assertTrue(text.contains("Current filtering behavior:"), text);
        assertTrue(text.contains("Manage Availability"), text);
        assertTrue(text.contains("Manage Work Week"), text);
        assertTrue(text.contains("Undo history"), text);
        wait.withMessage("Sorting guidance must follow the feature filter image").until(webDriver -> Boolean.TRUE.equals(javascript.executeScript(
                "const root = document.getElementById(arguments[0]);"
                        + " const image = root.querySelector('img[src$=\"feature-global-filter.png\"]');"
                        + " const sorting = [...root.querySelectorAll('h3')].find(h => h.textContent.trim() === 'Compare the results');"
                        + " return !!image && !!sorting && !!(image.compareDocumentPosition(sorting) & Node.DOCUMENT_POSITION_FOLLOWING);",
                FeatureListView.FEATURE_HELP_BUTTON + "-drawer")));
        for (int theme = 0; theme < 2; theme++) {
            wait.withMessage("Features help element images failed to load in the active theme")
                    .until(webDriver -> Boolean.TRUE.equals(javascript.executeScript(
                            "const images = [...document.querySelectorAll('#' + arguments[0] + ' img')];"
                                    + " const dark = document.body.matches('[theme~=dark]') || document.documentElement.matches('[theme~=dark]');"
                                    + " return images.length === 9 && images.every(i => i.complete && i.naturalWidth > 0"
                                    + " && /^[a-z0-9-]+\\.png$/.test(i.src.split('/').pop())"
                                    + " && i.src.includes('/' + (dark ? 'dark' : 'light') + '/FeatureListView/'))",
                            FeatureListView.FEATURE_HELP_BUTTON + "-drawer")));
            assertTrue(Boolean.TRUE.equals(javascript.executeScript(
                    "return [...document.querySelectorAll('#' + arguments[0] + ' img')]"
                            + ".every(i => i.getBoundingClientRect().width <= i.naturalWidth)",
                    FeatureListView.FEATURE_HELP_BUTTON + "-drawer")));
            if (theme == 0) {
                boolean dark = Boolean.TRUE.equals(javascript.executeScript(
                        "return document.body.matches('[theme~=dark]') || document.documentElement.matches('[theme~=dark]')"));
                javascript.executeScript("document.getElementById(arguments[0]).click()", MainLayout.ID_THEME_TOGGLE);
                wait.until(webDriver -> Boolean.TRUE.equals(javascript.executeScript(
                        "return (document.body.matches('[theme~=dark]') || document.documentElement.matches('[theme~=dark]')) === arguments[0]",
                        !dark)));
            }
        }
        Dimension original = driver.manage().window().getSize();
        try {
            driver.manage().window().setSize(new Dimension(390, 800));
            assertTrue(Boolean.TRUE.equals(javascript.executeScript(
                    "return [...document.querySelectorAll('#' + arguments[0] + ' img')]"
                            + ".every(i => i.getBoundingClientRect().width <= window.innerWidth)",
                    FeatureListView.FEATURE_HELP_BUTTON + "-drawer")));
        } finally {
            driver.manage().window().setSize(original);
        }
        driver.findElement(collapse).click();
        wait.until(webDriver -> driver.findElements(chapters).stream().noneMatch(chapter -> chapter.getDomAttribute("opened") != null));
        wait.until(webDriver -> Boolean.TRUE.equals(javascript.executeScript(
                "return [...document.querySelectorAll('#' + arguments[0] + ' vaadin-details vaadin-markdown')]"
                        + ".every(markdown => markdown.getClientRects().length === 0)",
                FeatureListView.FEATURE_HELP_BUTTON + "-drawer")));
        driver.findElement(close).sendKeys(Keys.ESCAPE);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(drawer));
        wait.until(webDriver -> Boolean.TRUE.equals(javascript.executeScript(
                "return document.activeElement.id === arguments[0]", FeatureListView.FEATURE_HELP_BUTTON)));
    }

    /**
     * Tests the behavior when creating a project but canceling the operation.
     * <p>
     * Verifies that when a user clicks the create project button, enters a name, and then
     * cancels the operation, no project is created in the list.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateCancel() throws Exception {
        FeatureListViewTester.createFeatureCancel(featureName);
    }

    /**
     * Tests the behavior when successfully creating a project.
     * <p>
     * Verifies that when a user clicks the create project button, enters a name, and confirms
     * the creation, the project appears in the list.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateConfirm() throws Exception {
        FeatureListViewTester.createFeatureConfirm(featureName);
    }

    /**
     * Tests the behavior when attempting to create a feature with a name that already exists.
     * <p>
     * Creates a feature, then tries to create another feature with the same name.
     * Verifies that the system displays an error and prevents the creation of the duplicate feature.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateDuplicateNameFails() throws Exception {
        // First, create a feature
        FeatureListViewTester.createFeatureConfirm(featureName);
        // Then try to create another feature with the same name
        FeatureListViewTester.createFeatureWithDuplicateName(featureName);
    }

    /**
     * Tests the behavior when attempting to delete a project but canceling the operation.
     * <p>
     * Creates a project, then attempts to delete it but cancels the confirmation dialog.
     * Verifies that the project remains in the list.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDeleteCancel() throws Exception {
        FeatureListViewTester.createFeatureConfirm(featureName);
        FeatureListViewTester.deleteFeatureCancel(featureName);
    }

    /**
     * Tests the behavior when successfully deleting a project.
     * <p>
     * Creates a project, then deletes it by confirming the deletion in the confirmation dialog.
     * Verifies that the project is removed from the list.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDeleteConfirm() throws Exception {
        FeatureListViewTester.createFeatureConfirm(featureName);
        FeatureListViewTester.deleteFeatureConfirm(featureName);
    }

    /**
     * Tests the behavior when attempting to edit a project but canceling the operation.
     * <p>
     * Creates a project, attempts to edit its name, but cancels the edit dialog.
     * Verifies that the original name remains unchanged and the new name is not present.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditCancel() throws Exception {
        FeatureListViewTester.createFeatureConfirm(featureName);
        FeatureListViewTester.editFeatureCancel(featureName, newProjectName);
    }

    /**
     * Tests the behavior when successfully editing a project.
     * <p>
     * Creates a project, edits its name, and confirms the edit.
     * Verifies that the project with the new name appears in the list and the old name is removed.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditConfirm() throws Exception {
        FeatureListViewTester.createFeatureConfirm(featureName);
        FeatureListViewTester.editFeatureConfirm(featureName, newProjectName);
    }

    /**
     * Tests the behavior when attempting to edit a feature to have a name that already exists.
     * <p>
     * Creates two features, then tries to update the second feature to have the same name as the first.
     * Verifies that the system displays an error and prevents the update.
     *
     * @throws Exception if any error occurs during the test
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditDuplicateNameFails() throws Exception {
        // First, create two features with different names
        FeatureListViewTester.createFeatureConfirm(featureName);
        FeatureListViewTester.createFeatureConfirm(newProjectName);

        // Then try to edit the second feature to have the same name as the first
        FeatureListViewTester.editFeatureWithDuplicateName(newProjectName, featureName);
    }
}
