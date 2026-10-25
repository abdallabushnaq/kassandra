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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    void testSmartHeadersAcrossRoutes() throws Exception {
        aboutViewTester.login("christopher.paul@kassandra.org", "password", null, null, null);
        Sprint sprint = prepareDialogPlanningContext();
        Feature feature = peg.featureApi.getById(sprint.getFeatureId());
        Version version = peg.versionApi.getById(feature.getVersionId());
        String context = "?product=" + version.getProductId() + "&version=" + version.getId()
                + "&feature=" + feature.getId() + "&sprint=" + sprint.getId();
        seleniumHandler.getDriver().manage().window().setSize(new Dimension(1600, 1000));
        for (String route : List.of("", "product-list", "version-list", "feature-list", "sprint-list",
                "legacy-sprint-list", "backlog", "legacy-backlog", "active-sprints", "quality-board",
                "legacy-quality-board", "user-list", "user-group-list", "availability", "location",
                "offday", "work-week-list", "user-work-week", "profile", "server-settings",
                "identity-providers", "audit", "kassandra", "insights")) {
            navigateTo(route + context);
            seleniumHandler.waitUntil(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".smart-header")));
            assertEquals(1, seleniumHandler.getDriver().findElements(By.cssSelector(".smart-header")).size(), route);
            assertSmartHeaderGeometry(route);
            WebElement breadcrumbs = seleniumHandler.findElement(By.id(MainLayout.ID_BREADCRUMBS));
            assertTrue(breadcrumbs.isDisplayed(), route);
            assertFalse(breadcrumbs.getText().isBlank(), route);
            assertNotEquals("a", breadcrumbs.findElements(By.xpath("./*")).getLast().getTagName(), route);
        }
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    void testBreadcrumbsForEveryUserMenuEntry() throws Exception {
        aboutViewTester.login("christopher.paul@kassandra.org", "password", null, null, null);
        prepareDialogPlanningContext();
        Map<String, String> titles = Map.ofEntries(
                Map.entry(MainLayout.ID_USER_MENU_AVAILABILITY, "User Availability"),
                Map.entry(MainLayout.ID_USER_MENU_LOCATION, "User Location"),
                Map.entry(MainLayout.ID_USER_MENU_OFF_DAYS, "User Off Days"),
                Map.entry(MainLayout.ID_USER_MENU_WORK_WEEK, "User Work Week"),
                Map.entry(MainLayout.ID_USER_MENU_VIEW_PROFILE, "User Profile"),
                Map.entry(MainLayout.ID_USER_MENU_IDENTITY_PROVIDERS, "Identity Providers"),
                Map.entry(MainLayout.ID_USER_MENU_MANAGE_USER_GROUPS, "User Groups"),
                Map.entry(MainLayout.ID_USER_MENU_MANAGE_USERS, "Users"),
                Map.entry(MainLayout.ID_USER_MENU_MANAGE_WORK_WEEKS, "Work Week List"),
                Map.entry(MainLayout.ID_USER_MENU_MANAGE_SETTINGS, "Server Settings"),
                Map.entry(MainLayout.ID_USER_MENU_AUDIT, "Audit"),
                Map.entry(MainLayout.ID_USER_MENU_ABOUT, "About Kassandra"));
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.waitForElementToBeClickable(MainLayout.ID_USER_MENU_AVAILABILITY);
        List<String> menuIds = seleniumHandler.getDriver()
                .findElements(By.cssSelector("[id^='main-layout-user-menu-']"))
                .stream().map(element -> element.getDomAttribute("id"))
                .filter(id -> !MainLayout.ID_USER_MENU_LOGOUT.equals(id)).toList();
        assertEquals(titles.keySet(), new HashSet<>(menuIds));
        seleniumHandler.findElement(By.id(MainLayout.ID_USER_MENU_AVAILABILITY)).sendKeys(Keys.ESCAPE);
        seleniumHandler.waitUntil(ExpectedConditions.invisibilityOfElementLocated(By.id(MainLayout.ID_USER_MENU_AVAILABILITY)));
        for (String menuId : menuIds) {
            seleniumHandler.click(MainLayout.ID_USER_MENU);
            seleniumHandler.click(menuId);
            seleniumHandler.waitUntil(driver -> titles.get(menuId).equals(
                    driver.findElement(By.id(MainLayout.ID_BREADCRUMBS)).getText()));
            WebElement breadcrumbs = seleniumHandler.findElement(By.id(MainLayout.ID_BREADCRUMBS));
            assertTrue(breadcrumbs.isDisplayed(), menuId);
            List<WebElement> items = breadcrumbs.findElements(By.xpath("./*"));
            assertEquals(1, items.size(), menuId);
            assertNotEquals("a", items.getFirst().getTagName(), menuId);
        }
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    void testBreadcrumbsForTopLevelViews() throws Exception {
        aboutViewTester.login("christopher.paul@kassandra.org", "password", null, null, null);
        Sprint sprint = prepareDialogPlanningContext();
        Feature feature = peg.featureApi.getById(sprint.getFeatureId());
        Version version = peg.versionApi.getById(feature.getVersionId());
        List<Class<? extends Component>> views = List.of(UserProfileView.class, ServerSettingsView.class,
                OidcProviderManagementView.class, AboutView.class, AvailabilityListView.class,
                LocationListView.class, OffDayListView.class, WorkWeekListView.class,
                UserWorkWeekListView.class, AuditLogView.class);
        for (Class<? extends Component> view : views) {
            String route = view.getAnnotation(Route.class).value().split("/:")[0];
            navigateTo(route);
            String title = view.getAnnotation(PageTitle.class).value();
            seleniumHandler.waitUntil(driver -> {
                WebElement breadcrumbs = driver.findElement(By.id(MainLayout.ID_BREADCRUMBS));
                return breadcrumbs.isDisplayed() && title.equals(breadcrumbs.getText());
            });
            WebElement breadcrumbs = seleniumHandler.findElement(By.id(MainLayout.ID_BREADCRUMBS));
            List<WebElement> items = breadcrumbs.findElements(By.xpath("./*"));
            assertEquals(1, items.size(), route);
            assertNotEquals("a", items.getFirst().getTagName(), route);
        }

        navigateTo("feature-list?product=" + version.getProductId() + "&version=" + version.getId());
        seleniumHandler.waitForElementToBeClickable(FeatureListView.FEATURE_LIST_PAGE_TITLE);
        WebElement breadcrumbs = seleniumHandler.findElement(By.id(MainLayout.ID_BREADCRUMBS));
        assertTrue(breadcrumbs.isDisplayed());
        assertEquals(2, breadcrumbs.findElements(By.tagName("a")).size());
        WebElement versions = breadcrumbs.findElements(By.tagName("a")).getLast();
        assertTrue(versions.getDomAttribute("href").contains("?product="));
        versions.click();
        seleniumHandler.waitUntil(driver -> driver.getCurrentUrl().contains("/version-list?product="));
        seleniumHandler.waitUntil(driver -> driver.findElement(By.id(MainLayout.ID_BREADCRUMBS))
                .getText().endsWith("Versions"));

        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_VIEW_PROFILE);
        seleniumHandler.waitForElementToBeClickable(UserProfileView.PROFILE_PAGE_TITLE);
        assertEquals("User Profile", seleniumHandler.findElement(By.id(MainLayout.ID_BREADCRUMBS)).getText());
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_ABOUT);
        seleniumHandler.waitForElementToBeClickable(AboutView.ABOUT_PAGE_TITLE);
        assertEquals("About Kassandra", seleniumHandler.findElement(By.id(MainLayout.ID_BREADCRUMBS)).getText());
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    void testSmartHeaderResponsiveLayout() throws Exception {
        aboutViewTester.login("christopher.paul@kassandra.org", "password", null, null, null);
        Sprint sprint = prepareDialogPlanningContext();
        Dimension originalSize = seleniumHandler.getDriver().manage().window().getSize();
        try {
            for (String route : List.of("product-list", "backlog?sprint=" + sprint.getId(),
                    "active-sprints?sprints=" + sprint.getId(), "quality-board?sprint=" + sprint.getId(),
                    "", "audit", "server-settings", "profile")) {
                navigateTo(route);
                seleniumHandler.waitUntil(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".smart-header")));
                for (ETheme theme : List.of(ETheme.light, ETheme.dark)) {
                    seleniumHandler.getDriver().manage().window().setSize(new Dimension(1600, 1000));
                    seleniumHandler.setHelpScreenshotTheme(theme);
                    for (int width : List.of(1600, 1000, 480)) {
                        seleniumHandler.getDriver().manage().window().setSize(new Dimension(width, 1000));
                        assertSmartHeaderGeometry(route + " " + theme + " " + width);
                    }
                }
            }
        } finally {
            seleniumHandler.getDriver().manage().window().setSize(originalSize);
        }
    }

    private void assertSmartHeaderGeometry(String context) {
        seleniumHandler.waitUntil(driver -> Boolean.TRUE.equals(seleniumHandler.executeJavaScript(
                "return getComputedStyle(document.querySelector('.smart-header')).display === 'grid';")));
        Object errors = seleniumHandler.executeJavaScript("""
                const header = document.querySelector('.smart-header');
                const rect = header.getBoundingClientRect();
                const identity = header.querySelector('.smart-header-identity').getBoundingClientRect();
                const title = header.querySelector('.smart-header-title').getBoundingClientRect();
                const icon = header.querySelector('.smart-header-icon').getBoundingClientRect();
                const controls = header.querySelector('.smart-header-controls');
                const right = header.querySelector('.smart-header-right');
                const errors = [];
                const visible = element => element.getClientRects().length > 0;
                if (icon.right > title.left + 1) errors.push('Icon must precede title');
                if (rect.right > innerWidth + 1 || header.scrollWidth > header.clientWidth + 1)
                    errors.push('Header overflows: ' + rect.right + '/' + innerWidth + ', '
                        + header.scrollWidth + '/' + header.clientWidth);
                for (const element of header.querySelectorAll('vaadin-button, vaadin-text-field, vaadin-combo-box, vaadin-multi-select-combo-box')) {
                    if (visible(element) && element.getBoundingClientRect().right > rect.right + 1)
                        errors.push('Control overflows: ' + element.id);
                }
                if (innerWidth <= 1200 && visible(controls)
                        && controls.getBoundingClientRect().top < identity.bottom - 1)
                    errors.push('Filters must wrap below title');
                if (visible(right)) {
                    const expectedRight = rect.right - parseFloat(getComputedStyle(header).paddingRight);
                    if (Math.abs(right.getBoundingClientRect().right - expectedRight) > 2)
                        errors.push('Actions must align right: actual=' + right.getBoundingClientRect().right
                            + ', expected=' + expectedRight);
                    if (right.lastElementChild !== header.querySelector('.smart-header-help'))
                        errors.push('Help must be last');
                }
                return errors;
                """);
        assertEquals(List.of(), errors, context);
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    void testHelpInEmptyAndRebuiltViews() throws Exception {
        aboutViewTester.login("christopher.paul@kassandra.org", "password", null, null, null);
        assertTrue(peg.productApi.getAll().stream()
                .allMatch(product -> DefaultEntitiesInitializer.DEFAULT_NAME.equals(product.getName())));
        navigateTo(VersionListView.ROUTE);
        seleniumHandler.waitForElementToBeClickable(VersionListView.VERSION_LIST_PAGE_TITLE);
        assertEquals(VersionListView.class.getAnnotation(PageTitle.class).value(),
                seleniumHandler.findElement(By.id(MainLayout.ID_BREADCRUMBS)).getText());
        assertTrue(seleniumHandler.getDriver().findElements(By.id(VersionListView.CREATE_VERSION_BUTTON)).isEmpty());
        verifyPageHelp(VersionListView.HELP_BUTTON_ID, true);

        navigateTo(ServerSettingsView.ROUTE);
        seleniumHandler.waitForElementToBeClickable(ServerSettingsView.PAGE_TITLE_ID);
        WebElement save = seleniumHandler.findElement(By.id(ServerSettingsView.SAVE_BUTTON_PREFIX + "kassandra.undo-redo.history-limit"));
        seleniumHandler.click(ServerSettingsView.SAVE_BUTTON_PREFIX + "kassandra.undo-redo.history-limit");
        seleniumHandler.waitUntil(ExpectedConditions.stalenessOf(save));
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
