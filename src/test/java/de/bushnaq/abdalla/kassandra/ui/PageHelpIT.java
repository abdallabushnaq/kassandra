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
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.report.dao.ETheme;
import de.bushnaq.abdalla.kassandra.rest.api.ProductAclApi;
import de.bushnaq.abdalla.kassandra.rest.api.UndoRedoApi;
import de.bushnaq.abdalla.kassandra.ui.component.TaskCard;
import de.bushnaq.abdalla.kassandra.ui.component.TaskGrid;
import de.bushnaq.abdalla.kassandra.ui.component.UndoHistoryOperationDetails;
import de.bushnaq.abdalla.kassandra.ui.dialog.*;
import de.bushnaq.abdalla.kassandra.ui.util.RenderUtil;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.*;
import de.bushnaq.abdalla.kassandra.ui.view.util.AboutViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.FeatureListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.ProductListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.VersionListViewTester;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

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
    @Autowired
    private UndoRedoApi undoRedoApi;
    @MockitoSpyBean
    private StableDiffusionService stableDiffusionService;

    private static Stream<Arguments> dialogHelpCases() {
        return Stream.of(
                Arguments.of(ProductListView.ROUTE, ProductListView.CREATE_PRODUCT_BUTTON, ProductDialog.PRODUCT_HELP_BUTTON,
                        ProductDialog.PRODUCT_DIALOG, ProductDialog.CANCEL_BUTTON),
                Arguments.of(VersionListView.ROUTE, VersionListView.CREATE_VERSION_BUTTON, VersionDialog.HELP_BUTTON_ID,
                        VersionDialog.VERSION_DIALOG, VersionDialog.CANCEL_BUTTON),
                Arguments.of("feature-list", FeatureListView.CREATE_FEATURE_BUTTON_ID, FeatureDialog.HELP_BUTTON_ID,
                        FeatureDialog.FEATURE_DIALOG, FeatureDialog.CANCEL_BUTTON),
                Arguments.of("sprint-list", SprintListView.CREATE_SPRINT_BUTTON, SprintDialog.HELP_BUTTON_ID,
                        SprintDialog.SPRINT_DIALOG, SprintDialog.CANCEL_BUTTON),
                Arguments.of(UserListView.ROUTE, UserListView.CREATE_USER_BUTTON, UserDialog.HELP_BUTTON_ID,
                        UserDialog.USER_DIALOG, UserDialog.CANCEL_BUTTON),
                Arguments.of(UserGroupListView.ROUTE, UserGroupListView.CREATE_GROUP_BUTTON, UserGroupDialog.HELP_BUTTON_ID,
                        UserGroupDialog.GROUP_DIALOG, UserGroupDialog.CANCEL_BUTTON),
                Arguments.of(AvailabilityListView.ROUTE, AvailabilityListView.CREATE_AVAILABILITY_BUTTON, AvailabilityDialog.HELP_BUTTON_ID,
                        AvailabilityDialog.AVAILABILITY_DIALOG, AvailabilityDialog.CANCEL_BUTTON),
                Arguments.of(LocationListView.ROUTE, LocationListView.CREATE_LOCATION_BUTTON, LocationDialog.HELP_BUTTON_ID,
                        LocationDialog.LOCATION_DIALOG, LocationDialog.CANCEL_BUTTON),
                Arguments.of(OffDayListView.ROUTE, OffDayListView.CREATE_OFFDAY_BUTTON, OffDayDialog.HELP_BUTTON_ID,
                        OffDayDialog.OFFDAY_DIALOG, OffDayDialog.CANCEL_BUTTON),
                Arguments.of(UserWorkWeekListView.ROUTE, UserWorkWeekListView.CREATE_BUTTON, UserWorkWeekDialog.HELP_BUTTON_ID,
                        UserWorkWeekDialog.USER_WORK_WEEK_DIALOG, UserWorkWeekDialog.CANCEL_BUTTON),
                Arguments.of(WorkWeekListView.ROUTE, WorkWeekListView.CREATE_BUTTON, WorkWeekDialog.HELP_BUTTON_ID,
                        WorkWeekDialog.WORK_WEEK_DIALOG, WorkWeekDialog.CANCEL_BUTTON));
    }

    private Sprint prepareDialogPlanningContext() throws Exception {
        Product product = new Product();
        product.setName("Dialog help product");
        product = peg.productApi.persist(product);
        Version version = new Version();
        version.setName("Dialog help version");
        version.setProductId(product.getId());
        version = peg.versionApi.persist(version);
        Feature feature = new Feature();
        feature.setName("Dialog help feature");
        feature.setVersionId(version.getId());
        feature = peg.featureApi.persist(feature);
        Sprint sprint = new Sprint();
        sprint.setName("Dialog help sprint");
        sprint.setFeatureId(feature.getId());
        sprint.setStart(LocalDateTime.now());
        sprint = peg.sprintApi.persist(sprint);
        productListViewTester.switchToProductListView();
        productListViewTester.selectProduct(product.getName());
        versionListViewTester.selectVersion(version.getName());
        featureListViewTester.selectFeature(feature.getName());
        return sprint;
    }

    @ParameterizedTest
    @MethodSource("dialogHelpCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    void testDialogHelp(String route, String createId, String helpId, String dialogId, String cancelId) throws Exception {
        aboutViewTester.login("christopher.paul@kassandra.org", "password", null, null, null);
        prepareDialogPlanningContext();
        navigateTo(route);
        for (ETheme theme : List.of(ETheme.light, ETheme.dark)) {
            seleniumHandler.setHelpScreenshotTheme(theme);
            seleniumHandler.click(createId);
            verifyDialogHelp(helpId, dialogId);
            closeOwnerWithHelp(helpId, dialogId, cancelId);
        }
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    void testNestedImageDialogHelp() throws Exception {
        aboutViewTester.login("christopher.paul@kassandra.org", "password", null, null, null);
        doReturn(true).when(stableDiffusionService).isAvailable();
        for (ETheme theme : List.of(ETheme.light, ETheme.dark)) {
            productListViewTester.switchToProductListView();
            seleniumHandler.setHelpScreenshotTheme(theme);
            seleniumHandler.click(ProductListView.CREATE_PRODUCT_BUTTON);
            seleniumHandler.setTextField(ProductDialog.PRODUCT_NAME_FIELD, "Unsaved image help product");
            seleniumHandler.click(ProductDialog.GENERATE_IMAGE_BUTTON);
            verifyDialogHelp(ImagePromptDialog.HELP_BUTTON_ID, ImagePromptDialog.IMAGE_PROMPT_DIALOG);
            closeOwnerWithHelp(ImagePromptDialog.HELP_BUTTON_ID, ImagePromptDialog.IMAGE_PROMPT_DIALOG,
                    ImagePromptDialog.ID_CANCEL_BUTTON);
            assertEquals("Unsaved image help product",
                    seleniumHandler.findElement(By.id(ProductDialog.PRODUCT_NAME_FIELD)).getDomProperty("value"));
            seleniumHandler.click(ProductDialog.CANCEL_BUTTON);
        }
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    void testTaskDependencyAndNestedWorklogHelp() throws Exception {
        aboutViewTester.login("christopher.paul@kassandra.org", "password", null, null, null);
        Sprint sprint = prepareDialogPlanningContext();
        User user = peg.userApi.getByEmail("christopher.paul@kassandra.org").orElseThrow();
        Task story = peg.addParentTask("Help story", sprint, null, null);
        Task task = peg.addTask("Help task", "1h", "2h", user, sprint, story, null);
        task.setTaskStatus(TaskStatus.IN_PROGRESS);
        peg.taskApi.update(task);
        sprint.setStatus(Status.STARTED);
        peg.sprintApi.update(sprint);
        for (ETheme theme : List.of(ETheme.light, ETheme.dark)) {
            navigateTo(ActiveSprints.ROUTE + "?sprints=" + sprint.getId());
            seleniumHandler.waitForChartRendered(RenderUtil.GANTT_BURNDOWN_CHART);
            seleniumHandler.setHelpScreenshotTheme(theme);
            seleniumHandler.click(TaskCard.TASK_CARD_TITLE_ID_PREFIX + task.getKey());
            seleniumHandler.setTextField(TaskDialog.NAME_FIELD_ID, "Unsaved task help input");
            verifyDialogHelp(TaskDialog.HELP_BUTTON_ID, TaskDialog.TASK_DIALOG);
            seleniumHandler.click(TaskDialog.LOG_WORK_BUTTON_ID);
            seleniumHandler.setTextField(WorklogDialog.TIME_SPENT_FIELD, "30m");
            verifyDialogHelp(WorklogDialog.HELP_BUTTON_ID, WorklogDialog.WORKLOG_DIALOG);
            assertEquals("30m", seleniumHandler.findElement(By.id(WorklogDialog.TIME_SPENT_FIELD)).getDomProperty("value"));
            closeOwnerWithHelp(WorklogDialog.HELP_BUTTON_ID, WorklogDialog.WORKLOG_DIALOG, WorklogDialog.CANCEL_BUTTON);
            assertEquals("Unsaved task help input", seleniumHandler.findElement(By.id(TaskDialog.NAME_FIELD_ID)).getDomProperty("value"));
            closeOwnerWithHelp(TaskDialog.HELP_BUTTON_ID, TaskDialog.TASK_DIALOG, TaskDialog.CANCEL_BUTTON);

            navigateTo(Backlog.ROUTE + "?sprints=" + sprint.getId());
            seleniumHandler.waitForChartRendered(RenderUtil.GANTT_CHART);
            seleniumHandler.click(Backlog.EDIT_BUTTON_ID);
            seleniumHandler.waitForChartRendered(RenderUtil.GANTT_CHART);
            seleniumHandler.click(TaskGrid.TASK_GRID_DEPENDENCY_PREFIX + task.getName());
            verifyDialogHelp(DependencyDialog.HELP_BUTTON_ID, DependencyDialog.DEPENDENCY_DIALOG);
            closeOwnerWithHelp(DependencyDialog.HELP_BUTTON_ID, DependencyDialog.DEPENDENCY_DIALOG, DependencyDialog.CANCEL_BUTTON);
            seleniumHandler.click(Backlog.CANCEL_BUTTON_ID);
        }
        assertEquals("Help task", peg.taskApi.getById(task.getId()).getName());
        assertTrue(peg.worklogApi.getAll(sprint.getId()).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    void testReplayConfirmationHelp(boolean dark) throws Exception {
        aboutViewTester.login("christopher.paul@kassandra.org", "password", null, null, null);
        prepareDialogPlanningContext();
        Product product = peg.productApi.getAll().stream().filter(candidate -> "Dialog help product".equals(candidate.getName())).findFirst().orElseThrow();
        product.setName("Dialog help renamed");
        peg.productApi.update(product);
        var operation = undoRedoApi.history(product.getId()).getOperations().getFirst();
        navigateTo(ProductListView.ROUTE);
        seleniumHandler.waitForElementToBeClickable(ProductListView.PRODUCT_GRID_NAME_PREFIX + "Dialog help renamed");
        seleniumHandler.setHelpScreenshotTheme(dark ? ETheme.dark : ETheme.light);
        seleniumHandler.click(MainLayout.ID_ACTION_HISTORY_BUTTON);
        seleniumHandler.click(UndoHistoryOperationDetails.SUMMARY_ID_PREFIX + operation.getId());
        seleniumHandler.click(UndoHistoryOperationDetails.ACTION_ID_PREFIX + operation.getId());
        verifyDialogHelp(UndoHistoryConfirmationDialog.HELP_BUTTON_ID, UndoHistoryConfirmationDialog.DIALOG_ID);
        closeOwnerWithHelp(UndoHistoryConfirmationDialog.HELP_BUTTON_ID, UndoHistoryConfirmationDialog.DIALOG_ID,
                UndoHistoryConfirmationDialog.CANCEL_BUTTON);
        assertEquals("Dialog help renamed", peg.productApi.getById(product.getId()).getName());
        seleniumHandler.click(UndoHistoryOperationDetails.ACTION_ID_PREFIX + operation.getId());
        seleniumHandler.click(UndoHistoryConfirmationDialog.CONFIRM_BUTTON);
        seleniumHandler.waitUntil(driver -> "Dialog help product".equals(peg.productApi.getById(product.getId()).getName()));
        seleniumHandler.waitForElementToBeClickable(ProductListView.PRODUCT_GRID_NAME_PREFIX + "Dialog help product");
        seleniumHandler.click(MainLayout.ID_ACTION_HISTORY_BUTTON);
        seleniumHandler.click(UndoHistoryOperationDetails.SUMMARY_ID_PREFIX + operation.getId());
        seleniumHandler.click(UndoHistoryOperationDetails.ACTION_ID_PREFIX + operation.getId());
        verifyDialogHelp(UndoHistoryConfirmationDialog.HELP_BUTTON_ID, UndoHistoryConfirmationDialog.DIALOG_ID);
        closeOwnerWithHelp(UndoHistoryConfirmationDialog.HELP_BUTTON_ID, UndoHistoryConfirmationDialog.DIALOG_ID,
                UndoHistoryConfirmationDialog.CANCEL_BUTTON);
        assertEquals("Dialog help product", peg.productApi.getById(product.getId()).getName());
    }

    private void verifyDialogHelp(String helpId, String dialogId) {
        seleniumHandler.click(helpId);
        String drawerId = helpId + "-drawer";
        seleniumHandler.waitUntil(ExpectedConditions.visibilityOfElementLocated(By.id(drawerId)));
        By chapters = By.cssSelector("#" + drawerId + " vaadin-details.help-chapter");
        seleniumHandler.waitUntil(driver -> !driver.findElements(chapters).isEmpty());
        assertTrue(seleniumHandler.getDriver().findElements(chapters).stream()
                .noneMatch(chapter -> chapter.getDomAttribute("opened") != null), helpId);
        WebElement drawer = seleniumHandler.findElement(By.id(drawerId));
        assertEquals("true", seleniumHandler.findElement(By.id(helpId)).getDomAttribute("aria-expanded"));
        drawer.findElement(By.xpath(".//vaadin-button[normalize-space(.)='Expand all']")).click();
        seleniumHandler.waitUntil(driver -> driver.findElements(chapters).stream()
                .allMatch(chapter -> chapter.getDomAttribute("opened") != null));
        seleniumHandler.waitUntil(driver -> Boolean.TRUE.equals(seleniumHandler.executeJavaScript(
                "const images = [...document.getElementById(arguments[0]).querySelectorAll('img')];"
                        + " const dark = document.body.matches('[theme~=dark]') || document.documentElement.matches('[theme~=dark]');"
                        + " return images.length > 0 && images.every(image => image.complete && image.naturalWidth > 0"
                        + " && image.src.includes('/images/help/' + (dark ? 'dark' : 'light') + '/'));", drawerId)));
        drawer.findElement(By.xpath(".//vaadin-button[normalize-space(.)='Collapse all']")).click();
        seleniumHandler.waitUntil(driver -> driver.findElements(chapters).stream()
                .noneMatch(chapter -> chapter.getDomAttribute("opened") != null));
        drawer.findElement(By.cssSelector("vaadin-button[aria-label='Close help']")).sendKeys(Keys.ESCAPE);
        seleniumHandler.waitUntil(ExpectedConditions.invisibilityOfElementLocated(By.id(drawerId)));
        assertTrue(seleniumHandler.findElement(By.id(dialogId)).isDisplayed());
        assertEquals("false", seleniumHandler.findElement(By.id(helpId)).getDomAttribute("aria-expanded"));
    }

    private void closeOwnerWithHelp(String helpId, String dialogId, String cancelId) {
        seleniumHandler.click(helpId);
        seleniumHandler.waitUntil(ExpectedConditions.visibilityOfElementLocated(By.id(helpId + "-drawer")));
        seleniumHandler.executeJavaScript("document.getElementById(arguments[0]).click()", cancelId);
        seleniumHandler.waitUntil(ExpectedConditions.invisibilityOfElementLocated(By.id(dialogId)));
        seleniumHandler.waitUntil(ExpectedConditions.invisibilityOfElementLocated(By.id(helpId + "-drawer")));
    }

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
