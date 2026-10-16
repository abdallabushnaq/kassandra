/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
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

package de.bushnaq.abdalla.kassandra.ui;

import de.bushnaq.abdalla.kassandra.config.DefaultEntitiesInitializer;
import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.report.dao.ETheme;
import de.bushnaq.abdalla.kassandra.rest.api.ProductAclApi;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.FeatureListView;
import de.bushnaq.abdalla.kassandra.ui.view.ProductListView;
import de.bushnaq.abdalla.kassandra.ui.view.ServerSettingsView;
import de.bushnaq.abdalla.kassandra.ui.view.VersionListView;
import de.bushnaq.abdalla.kassandra.ui.view.util.AboutViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.FeatureListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.ProductListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.VersionListViewTester;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Help behavior regression tests, separate from documentation screenshot generation.
 */
@Tag("IntegrationUiTest")
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {"server.port=${test.server.port:0}", "spring.security.basic.enabled=false"}
)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PageHelpIT extends AbstractKeycloakUiTestUtil {
    @Autowired
    private AboutViewTester aboutViewTester;
    @Autowired
    private FeatureListViewTester featureListViewTester;
    @Autowired
    private ProductAclApi productAclApi;
    @Autowired
    private ProductListViewTester productListViewTester;
    @Autowired
    private HumanizedSeleniumHandler seleniumHandler;
    @Autowired
    private VersionListViewTester versionListViewTester;

    private void navigateTo(String route) {
        seleniumHandler.getAndCheck("http://localhost:" + allocatedPort + "/ui/" + route);
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    void testHelpInEmptyAndRebuiltViews() throws Exception {
        aboutViewTester.login("christopher.paul@kassandra.org", "password", null, null, null);
        assertTrue(peg.productApi.getAll().stream()
                .allMatch(product -> DefaultEntitiesInitializer.DEFAULT_NAME.equals(product.getName())));
        navigateTo(VersionListView.ROUTE);
        seleniumHandler.waitForElementToBeClickable(VersionListView.VERSION_LIST_PAGE_TITLE);
        assertTrue(seleniumHandler.getDriver().findElements(By.id(VersionListView.CREATE_VERSION_BUTTON)).isEmpty());
        verifyPageHelp(VersionListView.HELP_BUTTON_ID, true);

        navigateTo(ServerSettingsView.ROUTE);
        seleniumHandler.waitForElementToBeClickable(ServerSettingsView.PAGE_TITLE_ID);
        WebElement title = seleniumHandler.findElement(By.id(ServerSettingsView.PAGE_TITLE_ID));
        seleniumHandler.click(ServerSettingsView.SAVE_BUTTON_PREFIX + "kassandra.undo-redo.history-limit");
        seleniumHandler.waitUntil(ExpectedConditions.stalenessOf(title));
        seleniumHandler.waitForElementToBeClickable(ServerSettingsView.PAGE_TITLE_ID);
        verifyPageHelp(ServerSettingsView.HELP_BUTTON_ID, true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    void testHelpForAdminAndUser(boolean admin) throws Exception {
        String productName = "Help regression product";
        String versionName = "1.0.0";
        String userEmail = "jennifer.holleman@kassandra.org";
        User helpUser = peg.addUser("Jennifer Holleman", userEmail, "USER", "de", "nw",
                LocalDate.of(2025, 6, 1), peg.generateUserColor(peg.getUserIndex()), 1f, null);
        var provider = oidcProviderService.getProviders().stream()
                .filter(candidate -> "Test Keycloak".equals(candidate.getDisplayName())).findFirst().orElseThrow();
        oidcIdentityService.linkIdentity(helpUser.getId(), provider.getRegistrationId(), getKeycloakUserSubject(userEmail));

        aboutViewTester.login("christopher.paul@kassandra.org", "password", null, null, null);
        productListViewTester.switchToProductListView();
        productListViewTester.createProductConfirm(productName);
        productListViewTester.selectProduct(productName);
        versionListViewTester.createVersionConfirm(versionName);
        versionListViewTester.selectVersion(versionName);
        featureListViewTester.createFeatureConfirm("Help regression feature");
        Product product = peg.productApi.getAll().stream()
                .filter(candidate -> productName.equals(candidate.getName())).findFirst().orElseThrow();
        productAclApi.grantUserAccess(product.getId(), helpUser.getId());
        aboutViewTester.logout();
        seleniumHandler.destroy();

        String loginEmail = admin ? "christopher.paul@kassandra.org" : userEmail;
        aboutViewTester.login(loginEmail, "password", null, null, null);
        assertTrue(seleniumHandler.findElement(By.id(MainLayout.ID_USER_MENU)).getDomProperty("textContent").contains(loginEmail));
        for (ETheme theme : List.of(ETheme.light, ETheme.dark)) {
            productListViewTester.switchToProductListView();
            productListViewTester.selectProduct(productName);
            versionListViewTester.selectVersion(versionName);
            seleniumHandler.setHelpScreenshotTheme(theme);
            verifyMainLayoutHelp(admin);
            verifyPageHelp(FeatureListView.FEATURE_HELP_BUTTON, admin);
            productListViewTester.switchToProductListView();
            verifyPageHelp(ProductListView.PRODUCT_HELP_BUTTON, admin);
        }
    }

    private void verifyMainLayoutHelp(boolean admin) {
        seleniumHandler.click(FeatureListView.FEATURE_HELP_BUTTON);
        String drawerId = FeatureListView.FEATURE_HELP_BUTTON + "-drawer";
        seleniumHandler.waitUntil(ExpectedConditions.visibilityOfElementLocated(By.id(drawerId)));
        seleniumHandler.waitUntil(driver -> !driver.findElements(By.cssSelector("#" + drawerId + " vaadin-details")).isEmpty());
        WebElement drawer = seleniumHandler.findElement(By.id(drawerId));
        seleniumHandler.waitUntil(driver -> drawer.getDomProperty("textContent").contains("Manage Availability"));
        String text = drawer.getDomProperty("textContent");
        assertTrue(text.contains("Manage Availability"));
        assertEquals(admin, text.contains("Administer the server"));
        assertEquals(admin, text.contains("Saved client secrets are not displayed"));
        assertFalse(drawer.getDomProperty("innerHTML").contains(admin ? "/MainLayout/user/" : "/MainLayout/admin/"));
        drawer.findElement(By.cssSelector("vaadin-button[aria-label='Close help']")).click();
        seleniumHandler.waitUntil(ExpectedConditions.invisibilityOfElementLocated(By.id(drawerId)));
    }

    private void verifyPageHelp(String helpButtonId, boolean admin) {
        WebDriver driver = seleniumHandler.getDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        String drawerId = helpButtonId + "-drawer";
        seleniumHandler.click(helpButtonId);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(drawerId)));
        By chapters = By.cssSelector("#" + drawerId + " vaadin-details.help-chapter");
        wait.until(webDriver -> !driver.findElements(chapters).isEmpty());
        assertTrue(driver.findElements(chapters).stream().noneMatch(chapter -> chapter.getDomAttribute("opened") != null),
                "Help chapters must start collapsed: " + helpButtonId);
        WebElement drawer = driver.findElement(By.id(drawerId));
        drawer.findElement(By.xpath(".//vaadin-button[normalize-space(.)='Expand all']")).click();
        wait.until(webDriver -> driver.findElements(chapters).stream().allMatch(chapter -> chapter.getDomAttribute("opened") != null));
        wait.until(webDriver -> drawer.getDomProperty("textContent").contains("Manage Availability"));
        String text = drawer.getDomProperty("textContent");
        assertTrue(text.contains("Use the main menu and header"), helpButtonId);
        assertEquals(admin, text.contains("Administer the server"), helpButtonId);
        assertFalse(text.contains("{{include") || text.contains("{{audience}}"), helpButtonId);
        assertFalse(drawer.getDomProperty("innerHTML").contains(admin ? "/MainLayout/user/" : "/MainLayout/admin/"), helpButtonId);
        wait.withMessage(() -> "Help images must load in the current theme: " + helpButtonId + " "
                        + ((JavascriptExecutor) driver).executeScript(
                        "return JSON.stringify([...document.querySelectorAll('#' + arguments[0] + ' img')]"
                                + ".filter(image => !image.complete || !image.naturalWidth)"
                                + ".map(image => image.getAttribute('src')))", drawerId))
                .until(webDriver -> Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript(
                        "const root = document.getElementById(arguments[0]);"
                                + " const images = [...root.querySelectorAll('img')];"
                                + " const dark = document.body.matches('[theme~=dark]') || document.documentElement.matches('[theme~=dark]');"
                                + " return images.length > 0 && images.every(image => image.complete && image.naturalWidth > 0"
                                + " && image.src.includes('/images/help/' + (dark ? 'dark' : 'light') + '/'));",
                        drawerId)));
        drawer.findElement(By.xpath(".//vaadin-button[normalize-space(.)='Collapse all']")).click();
        wait.until(webDriver -> driver.findElements(chapters).stream().noneMatch(chapter -> chapter.getDomAttribute("opened") != null));
        drawer.findElement(By.cssSelector("vaadin-button[aria-label='Close help']")).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id(drawerId)));
        assertEquals("false", driver.findElement(By.id(helpButtonId)).getDomAttribute("aria-expanded"));
    }
}
