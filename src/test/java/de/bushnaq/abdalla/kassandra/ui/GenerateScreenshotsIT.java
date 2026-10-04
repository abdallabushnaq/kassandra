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

package de.bushnaq.abdalla.kassandra.ui;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.config.DefaultEntitiesInitializer;
import de.bushnaq.abdalla.kassandra.dao.OidcProviderDAO;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.report.dao.ETheme;
import de.bushnaq.abdalla.kassandra.repository.SecurityConfigurationRepository;
import de.bushnaq.abdalla.kassandra.rest.api.AuditLogApi;
import de.bushnaq.abdalla.kassandra.rest.api.ServerSettingsApi;
import de.bushnaq.abdalla.kassandra.rest.api.UndoRedoApi;
import de.bushnaq.abdalla.kassandra.service.SecurityConfigurationService;
import de.bushnaq.abdalla.kassandra.ui.component.TaskCard;
import de.bushnaq.abdalla.kassandra.ui.component.TaskGrid;
import de.bushnaq.abdalla.kassandra.ui.component.UndoHistoryOperationDetails;
import de.bushnaq.abdalla.kassandra.ui.dialog.*;
import de.bushnaq.abdalla.kassandra.ui.introduction.util.InstructionVideo;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.RenderUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.*;
import de.bushnaq.abdalla.kassandra.ui.view.util.*;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Tag("IntegrationUiTest")
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=${test.server.port:0}",
                "spring.security.basic.enabled=false"
        }
)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
@Testcontainers
@Slf4j
public class GenerateScreenshotsIT extends AbstractKeycloakUiTestUtil {
    @Autowired
    private       AboutViewTester                 aboutViewTester;
    @Autowired
    private       ActiveSprintsTester             activeSprintsTester;
    @Autowired
    private       AuditLogApi                     auditLogApi;
    @Autowired
    private       AvailabilityListViewTester      availabilityListViewTester;
    @Autowired
    private       BacklogTester                   backlogTester;
    @Autowired
    private       FeatureListViewTester           featureListViewTester;
    private       String                          featureName;
    private final LocalDate                       firstDay         = LocalDate.of(2025, 6, 1);
    private final LocalDate                       firstDayRecord1  = LocalDate.of(2026, 8, 4);
    private       Availability                    lastAvailability = null;
    private final LocalDate                       lastDay          = LocalDate.of(2025, 6, 1);
    private final LocalDate                       lastDayRecord1   = LocalDate.of(2026, 8, 7);
    private       Location                        lastLocation     = null;
    private       UserWorkWeek                    lastUserWorkWeek = null;
    @Autowired
    private       LocationListViewTester          locationListViewTester;
    @Autowired
    private       OffDayListViewTester            offDayListViewTester;
    @Autowired
    private       PasswordEncoder                 passwordEncoder;
    @Autowired
    private       ProductListViewTester           productListViewTester;
    private       String                          productName;
    @Autowired
    private       SecurityConfigurationRepository securityConfigurationRepository;
    @Autowired
    private       SecurityConfigurationService    securityConfigurationService;
    @Autowired
    private       HumanizedSeleniumHandler        seleniumHandler;
    @Autowired
    private       ServerSettingsApi               serverSettingsApi;
    @Autowired
    private       SprintListViewTester            sprintListViewTester;
    private       String                          sprintName;
    @Autowired
    private       TaskListViewTester              taskListViewTester;
    private       String                          taskName;
    private final OffDayType                      typeRecord1      = OffDayType.VACATION;
    @Autowired
    private       UndoRedoApi                     undoRedoApi;
    @Autowired
    private       UserGroupListViewTester         userGroupListViewTester;
    @Autowired
    private       UserListViewTester              userListViewTester;
    private       String                          userName;
    @Autowired
    private       UserProfileViewTester           userProfileViewTester;
    @Autowired
    private       UserWorkWeekListViewTester      userWorkWeekListViewTester;
    @Autowired
    private       VersionListViewTester           versionListViewTester;
    private       String                          versionName;
    @Autowired
    private       WorkWeekListViewTester          workWeekListViewTester;

    private void applyLoginTheme(ETheme theme) {
        // Login and recovery have no theme toggle; use the same persisted preference as MainLayout.
        seleniumHandler.executeJavaScript(
                "localStorage.setItem('theme', arguments[0]); document.documentElement.setAttribute('theme', arguments[0]); document.body.setAttribute('theme', arguments[0]);",
                theme.name());
    }

    // Method to get the public-facing URL, fixing potential redirect issues
    private static String getPublicFacingUrl(KeycloakContainer container) {
        return String.format("http://%s:%s",
                container.getHost(),
                container.getMappedPort(8080));
    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
//                new RandomCase(3,
//                        OffsetDateTime.parse("2026-02-02T08:00:00+01:00"),
//                        LocalDate.parse("2025-08-04"),
//                        Duration.ofDays(10),
//                        2, 2,
//                        2, 2,
//                        2, 2,
//                        1, 5,
//                        5, 8, 8, 8, 6, 7)//official demo data
                new RandomCase(3,
                        OffsetDateTime.parse("2026-08-22T08:00:00+01:00"),
                        LocalDate.parse("2026-02-01"),
                        Duration.ofDays(10),
                        6, 6,
                        1, 3,
                        1, 3,
                        1, 3,
                        5, 8, 8, 4, 6, 7)//official screenshot data
        };
        return Arrays.stream(randomCases).toList();
    }

    private void navigateTo(String route) {
        seleniumHandler.getAndCheck("http://localhost:" + allocatedPort + "/ui/" + route);
    }

    private void printAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            String password = "test-password";
            log.info("Running demo with user: {} and password: {}", username, password);
        } else {
            log.warn("No authenticated user found. Running demo without authentication.");
        }
    }

    private void read() {
        User paul = peg.userApi.getByEmail("christopher.paul@kassandra.org").get();
        lastAvailability = paul.getAvailabilities().getLast();
        lastLocation     = paul.getLocations().getLast();
        lastUserWorkWeek = paul.getUserWorkWeeks().getLast();
    }

    private void scrollIntoView(String id) {
        seleniumHandler.waitForElementToBeClickable(id);
        seleniumHandler.executeJavaScript("arguments[0].scrollIntoView({block: 'start', inline: 'nearest'});",
                seleniumHandler.findElement(By.id(id)));
    }

    private void takeActiveSprintsBoardScreenshot(String folder, GroupingMode mode) {
        seleniumHandler.setComboBoxValue(ActiveSprints.ID_GROUPING_MODE_SELECTOR, mode.getDisplayName());
        seleniumHandler.waitForChartRendered(RenderUtil.GANTT_BURNDOWN_CHART);
        scrollIntoView(ActiveSprints.BOARD_ID);
        seleniumHandler.takeScreenShot(folder + "/active-sprints-board-" + mode.name().toLowerCase(Locale.ROOT) + ".png");
    }

    /**
     * Takes screenshots of {@link WorklogDialog} and {@link TaskDialog} from the Active Sprints view.
     * <p>
     * Selects an active sprint and an in-progress task assigned to the signed-in user.
     * Existing worklogs are edited and deleted only as previews; both dialogs are cancelled.
     *
     * @param folder destination folder for the screenshot files
     */
    private void takeActiveSprintsDialogScreenshots(String folder) {
        List<Sprint> allSprints = peg.sprintApi.getAll();
        Sprint sprint = allSprints.stream()
                .filter(s -> s.getStatus() == Status.STARTED)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No started sprint found"));
        List<Task>    tasks       = peg.taskApi.getAll(sprint.getId());
        List<Worklog> worklogs    = peg.worklogApi.getAll(sprint.getId());
        User          currentUser = peg.userApi.getByEmail("christopher.paul@kassandra.org").orElseThrow();
        Task leafTask = tasks.stream()
                .filter(t -> !t.isMilestone() && t.getParentTaskId() != null && t.getTaskStatus().equals(TaskStatus.IN_PROGRESS))
                .filter(t -> currentUser.getId().equals(t.getResourceId()))
                .filter(t -> worklogs.stream().anyMatch(w -> t.getId().equals(w.getTaskId())))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No in-progress task with worklogs assigned to the signed-in user"));
        navigateTo(ActiveSprints.ROUTE + "?sprints=" + sprint.getId() + "&groupBy=" + GroupingMode.FEATURES.name());
        seleniumHandler.waitForChartRendered(RenderUtil.GANTT_BURNDOWN_CHART);
        takeActiveSprintsBoardScreenshot(folder, GroupingMode.FEATURES);
        takeActiveSprintsBoardScreenshot(folder, GroupingMode.STORIES);

        // WorklogDialog — opened by clicking the task card body
        String taskCardId = "task-card-" + leafTask.getId();
        seleniumHandler.click(taskCardId);
        seleniumHandler.waitForElementToBeClickable(WorklogDialog.TITLE_ID);
        seleniumHandler.setTextField(WorklogDialog.TIME_SPENT_FIELD, "1h");
        seleniumHandler.takeElementScreenShot(
                seleniumHandler.findDialogOverlayElement(WorklogDialog.WORKLOG_DIALOG),
                WorklogDialog.WORKLOG_DIALOG,
                folder + "/worklog-create-dialog.png");
        activeSprintsTester.closeDialog(WorklogDialog.CANCEL_BUTTON);

        // TaskDialog — opened by clicking the task card title link (stops card-click propagation)
        String taskTitleId = TaskCard.TASK_CARD_TITLE_ID_PREFIX + leafTask.getKey();
        seleniumHandler.click(taskTitleId);
        seleniumHandler.waitForElementToBeClickable(TaskDialog.CANCEL_BUTTON);
        seleniumHandler.takeElementScreenShot(
                seleniumHandler.findDialogOverlayElement(TaskDialog.TASK_DIALOG),
                TaskDialog.TASK_DIALOG,
                folder + "/task-dialog.png");

        Worklog worklog = worklogs.stream().filter(w -> leafTask.getId().equals(w.getTaskId())).findFirst().orElseThrow();
        seleniumHandler.click(TaskDialog.WORKLOG_EDIT_BUTTON_PREFIX + worklog.getId());
        seleniumHandler.waitForElementToBeClickable(WorklogDialog.CANCEL_BUTTON);
        takeDialogScreenshot(WorklogDialog.WORKLOG_DIALOG, folder + "/worklog-edit-dialog.png");
        seleniumHandler.click(WorklogDialog.CANCEL_BUTTON);
        seleniumHandler.waitForElementToBeClickable(TaskDialog.WORKLOG_DELETE_BUTTON_PREFIX + worklog.getId());
        seleniumHandler.click(TaskDialog.WORKLOG_DELETE_BUTTON_PREFIX + worklog.getId());
        seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON);
        takeDialogScreenshot(ConfirmDialog.CONFIRM_DIALOG, folder + "/worklog-delete-dialog.png");
        seleniumHandler.click(ConfirmDialog.CANCEL_BUTTON);
        seleniumHandler.waitForElementToBeClickable(TaskDialog.CANCEL_BUTTON);
        activeSprintsTester.closeDialog(TaskDialog.CANCEL_BUTTON);
    }

    private void takeAdministrationScreenshots(String folder) {
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_AUDIT);
        seleniumHandler.waitForElementToBeClickable(AuditLogView.PAGE_TITLE_ID);
        seleniumHandler.takeScreenShot(folder + "/audit-log-view.png");
        seleniumHandler.setTextField(AuditLogView.SEARCH_FIELD_ID, productName);
        seleniumHandler.click(AuditLogView.ACTION_FILTER_ID);
        By updateOption = By.xpath("//vaadin-select-item[normalize-space(.)='UPDATE']");
        seleniumHandler.waitUntil(ExpectedConditions.elementToBeClickable(updateOption));
        seleniumHandler.clickElement(seleniumHandler.findElement(updateOption));
        seleniumHandler.click(AuditLogView.APPLY_BUTTON_ID);
        AuditEvent event    = auditLogApi.getPage(null, "UPDATE", productName, null, null, 0, 50).items().getFirst();
        String     eventKey = event.revision() + "-" + event.entityId();
        seleniumHandler.click(AuditLogView.EVENT_ID_PREFIX + eventKey);
        seleniumHandler.waitForElementToBeClickable(AuditLogView.DETAILS_ID_PREFIX + eventKey);
        seleniumHandler.takeScreenShot(folder + "/audit-log-details.png");

        navigateTo(OidcProviderManagementView.ROUTE);
        seleniumHandler.waitForElementToBeClickable(OidcProviderManagementView.PAGE_TITLE_ID);
        seleniumHandler.takeScreenShot(folder + "/identity-providers-view.png");

        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_MANAGE_SETTINGS);
        seleniumHandler.waitForElementToBeClickable(ServerSettingsView.PAGE_TITLE_ID);
        List<ServerSetting> settings = serverSettingsApi.getAll();
        for (String category : settings.stream().map(ServerSetting::getCategoryKey).distinct().toList()) {
            seleniumHandler.click(ServerSettingsView.CATEGORY_BUTTON_PREFIX + category);
            seleniumHandler.waitForElementToBeClickable(ServerSettingsView.CATEGORY_TITLE_PREFIX + category);
            boolean enableForCapture = ("lm-studio".equals(category) || "stable-diffusion".equals(category))
                    && settings.stream().filter(setting -> category.equals(setting.getCategoryKey()))
                    .noneMatch(ServerSetting::isCategoryEnabled);
            try {
                if (enableForCapture) {
                    String     checkboxId = ServerSettingsView.CATEGORY_ENABLED_PREFIX + category;
                    WebElement details    = seleniumHandler.findElement(By.id(ServerSettingsView.DETAILS_ID));
                    seleniumHandler.waitForElementToBeClickable(checkboxId);
                    seleniumHandler.setCheckCheckbox(checkboxId, true);
                    seleniumHandler.waitUntil(ExpectedConditions.stalenessOf(details));
                    seleniumHandler.waitUntil(ExpectedConditions.refreshed(ExpectedConditions.elementToBeSelected(
                            By.cssSelector("#" + checkboxId + " input[type='checkbox']"))));
                }
                takeScrollableScreenshots(ServerSettingsView.DETAILS_ID, folder + "/server-settings-" + category);
            } finally {
                if (enableForCapture) {
                    serverSettingsApi.updateCategoryEnabled(category, false);
                }
            }
        }
    }

    private void takeAuthenticationScreenshots(String folder, ETheme theme) {
        var                   configuration        = securityConfigurationService.getConfiguration();
        String                originalRecoveryHash = configuration.getRecoveryPasswordHash();
        List<OidcProviderDAO> enabledProviders     = oidcProviderService.getProviders().stream().filter(OidcProviderDAO::isEnabled).toList();
        OidcProviderDAO       primary              = enabledProviders.getFirst();
        OidcProviderDAO       additional           = null;
        try {
            configuration.setRecoveryPasswordHash(passwordEncoder.encode("screenshot-recovery-password"));
            securityConfigurationRepository.saveAndFlush(configuration);
            takeLoginScreenshot(folder + "/login-view.png", theme, LoginView.OIDC_LOGIN_BUTTON);

            // Providers require distinct issuers; the existing master realm also supports discovery.
            additional = oidcProviderService.createProvider("Keycloak master", URI.create(primary.getIssuerUri()).resolve("master").toString(),
                    "admin-cli", "screenshot-client-secret", List.of(primary.getScopes().split(",")));
            takeLoginScreenshot(folder + "/login-multiple-providers.png", theme, LoginView.OIDC_PROVIDER_SELECTOR);
            oidcProviderService.disableProvider(additional.getId());
            for (OidcProviderDAO provider : enabledProviders) {
                oidcProviderService.disableProvider(provider.getId());
            }
            takeLoginScreenshot(folder + "/login-no-providers.png", theme, LoginView.SETUP_LINK);
            navigateTo(RecoveryView.ROUTE);
            seleniumHandler.waitForElementToBeClickable(RecoveryView.VIEW_ID);
            applyLoginTheme(theme);
            seleniumHandler.takeScreenShot(folder + "/recovery-view.png");
        } finally {
            try {
                for (OidcProviderDAO provider : enabledProviders) {
                    oidcProviderService.enableProvider(provider.getId());
                }
            } finally {
                try {
                    if (additional != null) {
                        oidcProviderService.deleteProvider(additional.getId());
                    }
                } finally {
                    var current = securityConfigurationService.getConfiguration();
                    current.setRecoveryPasswordHash(originalRecoveryHash);
                    securityConfigurationRepository.saveAndFlush(current);
                }
            }
        }
    }

    /**
     * Takes screenshots of Availability create, edit and delete dialogs
     */
    private void takeAvailabilityDialogScreenshots(String folder) {
        // Create availability dialog
        {
            seleniumHandler.click(AvailabilityListView.CREATE_AVAILABILITY_BUTTON);
            seleniumHandler.waitForElementToBeClickable(AvailabilityDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(AvailabilityDialog.AVAILABILITY_DIALOG), AvailabilityDialog.AVAILABILITY_DIALOG, folder + "/availability-create-dialog.png");
            availabilityListViewTester.closeDialog(AvailabilityDialog.CANCEL_BUTTON);
        }

        // Edit availability dialog
        // We'll use the current date as a reference to find a record to edit
        {
            String dateStr = lastAvailability.getStart().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            seleniumHandler.click(AvailabilityListView.AVAILABILITY_GRID_EDIT_BUTTON_PREFIX + dateStr);
            seleniumHandler.waitForElementToBeClickable(AvailabilityDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(AvailabilityDialog.AVAILABILITY_DIALOG), AvailabilityDialog.AVAILABILITY_DIALOG, folder + "/availability-edit-dialog.png");
            availabilityListViewTester.closeDialog(AvailabilityDialog.CANCEL_BUTTON);
        }

        // Delete availability dialog
        {
            // create something we can at least try to delete
            availabilityListViewTester.createAvailabilityConfirm(firstDay, 50);
            String dateStr = firstDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            seleniumHandler.click(AvailabilityListView.AVAILABILITY_GRID_DELETE_BUTTON_PREFIX + dateStr);
            seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, folder + "/availability-delete-dialog.png");
            availabilityListViewTester.closeConfirmDialog(ConfirmDialog.CONFIRM_BUTTON);
        }
    }

    /**
     * Enters Backlog edit mode and takes a screenshot of the {@link DependencyDialog}.
     * <p>
     * Requires the Backlog to be already visible with {@code sprintName} selected and the Gantt
     * chart rendered.  After the screenshot the dialog and edit mode are both cancelled, leaving
     * the Backlog in read-only state.
     *
     * @param folder destination folder for the screenshot file
     */
    private void takeDependencyDialogScreenshots(String folder) {
        // Resolve a leaf task in the currently-selected sprint
        List<Sprint> allSprints = peg.sprintApi.getAll();
        Sprint sprint = allSprints.stream()
                .filter(s -> s.getName().equals(sprintName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Sprint '" + sprintName + "' not found"));
        List<Task> tasks = peg.taskApi.getAll(sprint.getId());
        Task leafTask = tasks.stream()
                .filter(t -> !t.isMilestone() && t.getParentTaskId() != null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No leaf task found in sprint '" + sprintName + "'"));

        // Switch to edit mode
        seleniumHandler.click(Backlog.EDIT_BUTTON_ID);
        seleniumHandler.waitForElementToBeClickable(Backlog.CANCEL_BUTTON_ID);
        seleniumHandler.waitForChartRendered(RenderUtil.GANTT_CHART);
        seleniumHandler.takeScreenShot(folder + "/backlog-edit-mode.png");

        // Click the dependency cell for the leaf task to open DependencyDialog
        seleniumHandler.click(TaskGrid.TASK_GRID_DEPENDENCY_PREFIX + leafTask.getName());
        seleniumHandler.waitForElementToBeClickable(DependencyDialog.CANCEL_BUTTON);
        seleniumHandler.takeElementScreenShot(
                seleniumHandler.findDialogOverlayElement(DependencyDialog.DEPENDENCY_DIALOG),
                DependencyDialog.DEPENDENCY_DIALOG,
                folder + "/dependency-dialog.png");

        // Close dialog and exit edit mode
        seleniumHandler.wait(200);
        seleniumHandler.click(DependencyDialog.CANCEL_BUTTON);
        seleniumHandler.waitForElementToBeClickable(Backlog.CANCEL_BUTTON_ID);
        seleniumHandler.click(Backlog.CANCEL_BUTTON_ID);
        seleniumHandler.waitForChartRendered(RenderUtil.GANTT_CHART);
    }

    private void takeDetachedChartScreenshot(String buttonId, String chartId, String fileName) {
        WebDriver   driver          = seleniumHandler.getDriver();
        String      originalWindow  = driver.getWindowHandle();
        Set<String> originalWindows = driver.getWindowHandles();
        String      chartWindow     = null;
        try {
            scrollIntoView(buttonId);
            seleniumHandler.click(buttonId);
            seleniumHandler.waitUntil(ExpectedConditions.numberOfWindowsToBe(originalWindows.size() + 1));
            chartWindow = driver.getWindowHandles().stream().filter(handle -> !originalWindows.contains(handle))
                    .findFirst().orElseThrow();
            driver.switchTo().window(chartWindow);
            seleniumHandler.waitUntil(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#chart svg")));
            seleniumHandler.takeScreenShot(fileName);
        } finally {
            if (chartWindow != null && driver.getWindowHandles().contains(chartWindow)) {
                driver.switchTo().window(chartWindow);
                driver.close();
            }
            driver.switchTo().window(originalWindow);
        }
        seleniumHandler.waitForChartRendered(chartId);
    }

    private void takeDialogScreenshot(String dialogId, String fileName) {
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(dialogId), dialogId, fileName);
    }

    /**
     * Takes screenshots of Project create, edit and delete dialogs
     */
    private void takeFeatureDialogScreenshots(String folder) {
        // Create project dialog
        seleniumHandler.click(FeatureListView.CREATE_FEATURE_BUTTON_ID);
        seleniumHandler.waitForElementToBeClickable(FeatureDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(FeatureDialog.FEATURE_DIALOG), FeatureDialog.FEATURE_DIALOG, folder + "/feature-create-dialog.png");
        featureListViewTester.closeDialog(FeatureDialog.CANCEL_BUTTON);

        // Edit project dialog - open action menu first, then edit
        seleniumHandler.click(FeatureListView.FEATURE_GRID_EDIT_BUTTON_PREFIX + featureName);
        seleniumHandler.waitForElementToBeClickable(FeatureDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(FeatureDialog.FEATURE_DIALOG), FeatureDialog.FEATURE_DIALOG, folder + "/feature-edit-dialog.png");
        featureListViewTester.closeDialog(FeatureDialog.CANCEL_BUTTON);

        // Delete project dialog - open action menu first, then delete
        seleniumHandler.click(FeatureListView.FEATURE_GRID_DELETE_BUTTON_PREFIX + featureName);
        seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, folder + "/feature-delete-dialog.png");
        featureListViewTester.closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);
    }

    private void takeFeatureElementScreenshots(ETheme theme) throws IOException {
        seleniumHandler.setHelpScreenshotTheme(theme);
        seleniumHandler.prepareHelpScreenshots(FeatureListView.FEATURE_LIST_PAGE_TITLE);
        seleniumHandler.takeHelpElementScreenshot(FeatureListView.class, FeatureListView.CREATE_FEATURE_BUTTON_ID);
        seleniumHandler.takeHelpElementScreenshot(FeatureListView.class, FeatureListView.FEATURE_VERSION_SELECTOR);
        seleniumHandler.takeHelpElementScreenshot(FeatureListView.class, FeatureListView.FEATURE_GLOBAL_FILTER);
        seleniumHandler.takeHelpElementScreenshot(FeatureListView.class, FeatureListView.FEATURE_GLOBAL_FILTER + "-help-button");
        seleniumHandler.takeHelpElementScreenshot(FeatureListView.class, FeatureListView.FEATURE_ROW_COUNTER);
        seleniumHandler.takeHelpElementScreenshot(FeatureListView.class, FeatureListView.FEATURE_AI_PANEL_BUTTON);
        seleniumHandler.takeHelpElementScreenshot(FeatureListView.class, FeatureListView.FEATURE_HELP_BUTTON);
        seleniumHandler.takeHelpRowActionScreenshot(FeatureListView.class, FeatureListView.FEATURE_GRID_EDIT_BUTTON_PREFIX);
        seleniumHandler.takeHelpRowActionScreenshot(FeatureListView.class, FeatureListView.FEATURE_GRID_DELETE_BUTTON_PREFIX);
        seleniumHandler.takeHelpElementScreenshot(FeatureListView.class, MainLayout.ID_ACTION_HISTORY_BUTTON);
        seleniumHandler.takeHelpGridScreenshot(FeatureListView.class, FeatureListView.FEATURE_GRID);
    }

    /**
     * Regenerates only the Features help element images, without GPU services or manual waiting.
     *
     * @param testInfo current test metadata
     * @throws Exception if fixture creation or screenshot capture fails
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void takeFeatureHelpScreenshots(TestInfo testInfo) throws Exception {
        seleniumHandler.setWindowSize(InstructionVideo.VIDEO_WIDTH, InstructionVideo.VIDEO_EXTENDED_HEIGHT);
        StableDiffusionService.setEnabled(false);
        aboutViewTester.login("christopher.paul@kassandra.org", "password", null,
                testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
        productListViewTester.switchToProductListView();
        productListViewTester.createProductConfirm("Jupiter");
        productListViewTester.selectProduct("Jupiter");
        versionListViewTester.createVersionConfirm("1.0.0");
        versionListViewTester.selectVersion("1.0.0");
        featureListViewTester.createFeatureConfirm("Config server");
        for (ETheme theme : List.of(ETheme.light, ETheme.dark)) {
            takeFeatureElementScreenshots(theme);
        }
    }

    /**
     * Takes screenshots of Location create, edit and delete dialogs
     */
    private void takeLocationDialogScreenshots(String folder) {
        // Create location dialog
        {
            seleniumHandler.click(LocationListView.CREATE_LOCATION_BUTTON);
            seleniumHandler.waitForElementToBeClickable(LocationDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(LocationDialog.LOCATION_DIALOG), LocationDialog.LOCATION_DIALOG, folder + "/location-create-dialog.png");
            locationListViewTester.closeDialog(LocationDialog.CANCEL_BUTTON);
        }

        // Edit location dialog
        // We'll use the current date as a reference to find a record to edit
        {
            String dateStr = lastLocation.getStart().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            seleniumHandler.click(LocationListView.LOCATION_GRID_EDIT_BUTTON_PREFIX + dateStr);
            seleniumHandler.waitForElementToBeClickable(LocationDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(LocationDialog.LOCATION_DIALOG), LocationDialog.LOCATION_DIALOG, folder + "/location-edit-dialog.png");
            locationListViewTester.closeDialog(LocationDialog.CANCEL_BUTTON);
        }

        // Delete location dialog
        {
            locationListViewTester.createLocationConfirm(firstDay, "United States (US)", "California (ca)");
            String dateStr = firstDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            seleniumHandler.click(LocationListView.LOCATION_GRID_DELETE_BUTTON_PREFIX + dateStr);
            seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, folder + "/location-delete-dialog.png");
            locationListViewTester.closeConfirmDialog(ConfirmDialog.CONFIRM_BUTTON);
        }
    }

    private void takeLoginScreenshot(String fileName, ETheme theme, String readyId) {
        navigateTo(LoginView.ROUTE);
        seleniumHandler.waitForElementToBeClickable(readyId);
        applyLoginTheme(theme);
        seleniumHandler.takeElementScreenShot(seleniumHandler.findElement(By.id(LoginView.LOGIN_VIEW)), LoginView.LOGIN_VIEW, fileName);
    }

    /**
     * Takes screenshots of OffDay create, edit and delete dialogs
     */
    private void takeOffDayDialogScreenshots(String folder) {
        // Create availability dialog
        {
            seleniumHandler.click(OffDayListView.CREATE_OFFDAY_BUTTON);
            seleniumHandler.setDatePickerValue(OffDayDialog.OFFDAY_START_DATE_FIELD, firstDay);
            seleniumHandler.setDatePickerValue(OffDayDialog.OFFDAY_END_DATE_FIELD, lastDay);
            seleniumHandler.setComboBoxValue(OffDayDialog.OFFDAY_TYPE_FIELD, OffDayType.VACATION.name());
            seleniumHandler.waitForElementToBeClickable(OffDayDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(OffDayDialog.OFFDAY_DIALOG), OffDayDialog.OFFDAY_DIALOG, folder + "/offday-create-dialog.png");
            offDayListViewTester.closeDialog(OffDayDialog.CANCEL_BUTTON);
        }

        // Edit availability dialog
        {
            // Create an initial record
            offDayListViewTester.createOffDayConfirm(firstDayRecord1, lastDayRecord1, typeRecord1);

            offDayListViewTester.clickEditButtonForRecord(firstDayRecord1);
            seleniumHandler.waitForElementToBeClickable(OffDayDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(OffDayDialog.OFFDAY_DIALOG), OffDayDialog.OFFDAY_DIALOG, folder + "/offday-edit-dialog.png");
            offDayListViewTester.closeDialog(OffDayDialog.CANCEL_BUTTON);
        }

        // Delete availability dialog
        {
            offDayListViewTester.clickDeleteButtonForRecord(firstDayRecord1);
            seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, folder + "/offday-delete-dialog.png");
            offDayListViewTester.closeConfirmDialog(ConfirmDialog.CONFIRM_BUTTON);
        }
    }

    /**
     * Takes screenshots of Product create, edit and delete dialogs
     */
    private void takeProductDialogScreenshots(String folder) {
        // Create product dialog
        seleniumHandler.click(ProductListView.CREATE_PRODUCT_BUTTON);
        seleniumHandler.waitForElementToBeClickable(ProductDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ProductDialog.PRODUCT_DIALOG), ProductDialog.PRODUCT_DIALOG, folder + "/product-create-dialog.png");
        productListViewTester.closeDialog(ProductDialog.CANCEL_BUTTON);


        // Edit product dialog - open action menu first, then edit
        seleniumHandler.click(ProductListView.PRODUCT_GRID_EDIT_BUTTON_PREFIX + productName);
        seleniumHandler.waitForElementToBeClickable(ProductDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ProductDialog.PRODUCT_DIALOG), ProductDialog.PRODUCT_DIALOG, folder + "/product-edit-dialog.png");
        productListViewTester.closeDialog(ProductDialog.CANCEL_BUTTON);

        // Delete product dialog - open action menu first, then delete
        seleniumHandler.click(ProductListView.PRODUCT_GRID_DELETE_BUTTON_PREFIX + productName);
        seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, folder + "/product-delete-dialog.png");
        productListViewTester.closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);
    }

    private void takeProductHistoryScreenshots(String folder) {
        Product product = peg.productApi.getAll().stream().filter(p -> productName.equals(p.getName()))
                .findFirst().orElseThrow();
        try {
            seleniumHandler.click(ProductListView.PRODUCT_GRID_EDIT_BUTTON_PREFIX + productName);
            seleniumHandler.setTextField(ProductDialog.PRODUCT_NAME_FIELD, productName + " - planning update");
            productListViewTester.closeDialog(ProductDialog.CONFIRM_BUTTON);
            UndoRedoHistory.Operation operation = undoRedoApi.history(product.getId()).getOperations().getFirst();

            seleniumHandler.click(MainLayout.ID_ACTION_HISTORY_BUTTON);
            seleniumHandler.waitForElementToBeClickable(UndoHistoryOperationDetails.SUMMARY_ID_PREFIX + operation.getId());
            seleniumHandler.takeScreenShot(folder + "/product-history.png");
            seleniumHandler.click(UndoHistoryOperationDetails.SUMMARY_ID_PREFIX + operation.getId());
            seleniumHandler.waitForElementToBeClickable(UndoHistoryOperationDetails.ACTION_ID_PREFIX + operation.getId());
            seleniumHandler.takeScreenShot(folder + "/product-history-expanded.png");

            seleniumHandler.click(UndoHistoryOperationDetails.ACTION_ID_PREFIX + operation.getId());
            seleniumHandler.waitForElementToBeClickable(UndoHistoryConfirmationDialog.CONFIRM_BUTTON);
            takeDialogScreenshot(UndoHistoryConfirmationDialog.DIALOG_ID, folder + "/undo-history-confirmation-dialog.png");
            seleniumHandler.click(UndoHistoryConfirmationDialog.CONFIRM_BUTTON);
            seleniumHandler.waitForElementToBeClickable(ProductListView.PRODUCT_GRID_NAME_PREFIX + productName);

            seleniumHandler.click(MainLayout.ID_ACTION_HISTORY_BUTTON);
            seleniumHandler.click(UndoHistoryOperationDetails.SUMMARY_ID_PREFIX + operation.getId());
            seleniumHandler.waitForElementToBeClickable(UndoHistoryOperationDetails.ACTION_ID_PREFIX + operation.getId());
            seleniumHandler.takeScreenShot(folder + "/product-history-undone.png");
            seleniumHandler.click(UndoHistoryOperationDetails.ACTION_ID_PREFIX + operation.getId());
            seleniumHandler.waitForElementToBeClickable(UndoHistoryConfirmationDialog.CANCEL_BUTTON);
            takeDialogScreenshot(UndoHistoryConfirmationDialog.DIALOG_ID, folder + "/redo-history-confirmation-dialog.png");
            // Complete the replay before restoring a regular revision as the next theme's baseline.
            seleniumHandler.click(UndoHistoryConfirmationDialog.CONFIRM_BUTTON);
            seleniumHandler.waitForElementToBeClickable(ProductListView.PRODUCT_GRID_NAME_PREFIX + productName + " - planning update");
        } finally {
            Product current = peg.productApi.getById(product.getId());
            if (!productName.equals(current.getName())) {
                current.setName(productName);
                peg.productApi.update(current);
                navigateTo(ProductListView.ROUTE);
                seleniumHandler.waitForElementToBeClickable(ProductListView.PRODUCT_GRID_NAME_PREFIX + productName);
            }
        }
    }

    private void takeScreenshot(TestInfo testInfo, String folder, ETheme eTheme) throws Exception {

        //-----------------
        //AboutView
        //-----------------
        StableDiffusionService.setEnabled(true);
        try {
            aboutViewTester.login("christopher.paul@kassandra.org", "password", null, testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
            // Allow for checkpoint loading and generation, then wait for browser-side image decoding.
            new WebDriverWait(seleniumHandler.getDriver(), Duration.ofMinutes(10))
                    .withMessage("About View banner did not finish loading before screenshot capture")
                    .until(ExpectedConditions.refreshed(driver -> driver.findElements(By.cssSelector("#" + AboutView.ABOUT_VIEW + " img"))
                            .stream()
                            .anyMatch(image -> image.isDisplayed()
                                    && "true".equals(image.getDomProperty("complete"))
                                    && Integer.parseInt(image.getDomProperty("naturalWidth")) > 0)));
        } finally {
            StableDiffusionService.setEnabled(false);
        }
        if (ETheme.dark.equals(eTheme)) {
            seleniumHandler.click(MainLayout.ID_THEME_TOGGLE);
            seleniumHandler.wait(1000);
        }
        seleniumHandler.takeScreenShot(folder + "/about-view.png");

        if (false) {
        }
        //-----------------
        //ProductListView
        //-----------------
        productListViewTester.switchToProductListView();
        seleniumHandler.takeScreenShot(folder + "/product-list-view.png");
        takeProductDialogScreenshots(folder);
        takeProductHistoryScreenshots(folder);
        productListViewTester.selectProduct(productName);

        //-----------------
        //VersionListView
        //-----------------
        seleniumHandler.takeScreenShot(folder + "/version-list-view.png");
        takeVersionDialogScreenshots(folder);
        versionListViewTester.selectVersion(versionName);

        //-----------------
        //FeatureListView
        //-----------------
        seleniumHandler.takeScreenShot(folder + "/feature-list-view.png");
        takeFeatureElementScreenshots(eTheme);
        takeFeatureDialogScreenshots(folder);
        featureListViewTester.selectFeature(featureName);

        //-----------------
        //SprintListView
        //-----------------
        seleniumHandler.waitForChartRendered(RenderUtil.SPRINTS_OVERVIEW_CHART);
        seleniumHandler.takeScreenShot(folder + "/sprint-list-view.png");
        takeSprintDialogScreenshots(folder);
        sprintListViewTester.selectSprint(sprintName);


        //-----------------
        //QualityBoard
        //-----------------
        seleniumHandler.waitForChartRendered(RenderUtil.GANTT_BURNDOWN_CHART);
        seleniumHandler.takeScreenShot(folder + "/quality-board-delayed.png");
        seleniumHandler.setComboBoxValue(QualityBoard.SPRINT_SELECTOR_ID, "Lisbon");
        seleniumHandler.waitForChartRendered(RenderUtil.GANTT_BURNDOWN_CHART);
        seleniumHandler.takeScreenShot(folder + "/quality-board-in-time.png");
        takeDetachedChartScreenshot(QualityBoard.OPEN_GANTT_BURNDOWN_CHART_BUTTON_ID, RenderUtil.GANTT_BURNDOWN_CHART,
                folder + "/quality-board-detached-chart.png");

        //-----------------
        //UserProfileView
        //-----------------
        takeUserProfileScreenshots(folder);

        //-----------------
        //UserGroupListView
        //-----------------
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_MANAGE_USER_GROUPS);
        seleniumHandler.waitForElementToBeClickable(UserGroupListView.GROUP_LIST_PAGE_TITLE);
        seleniumHandler.takeScreenShot(folder + "/user-group-list-view.png");
        takeUserGroupDialogScreenshots(folder);


        //-----------------
        //Backlog
        //-----------------
        backlogTester.switchToBacklog();
        seleniumHandler.setComboBoxValue(Backlog.SPRINT_SELECTOR_ID, sprintName);
        seleniumHandler.waitForChartRendered(RenderUtil.GANTT_CHART);
        seleniumHandler.takeScreenShot(folder + "/backlog.png");
        takeDetachedChartScreenshot(Backlog.OPEN_GANTT_CHART_BUTTON_ID, RenderUtil.GANTT_CHART, folder + "/backlog-detached-chart.png");
        takeDependencyDialogScreenshots(folder);

        //-----------------
        //ActiveSprints
        //-----------------
        activeSprintsTester.switchToActiveSprints();
        seleniumHandler.waitForChartRendered(RenderUtil.GANTT_BURNDOWN_CHART);
        seleniumHandler.takeScreenShot(folder + "/active-sprints.png");
        takeDetachedChartScreenshot(ActiveSprints.OPEN_GANTT_BURNDOWN_CHART_BUTTON_ID, RenderUtil.GANTT_BURNDOWN_CHART,
                folder + "/active-sprints-detached-chart.png");
        takeActiveSprintsDialogScreenshots(folder);

        //-----------------
        //UserListView
        //-----------------
        userListViewTester.switchToUserListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
        seleniumHandler.takeScreenShot(folder + "/user-list-view.png");
        takeUserDialogScreenshots(folder);

        //-----------------
        //AvailabilityListView
        //-----------------
        availabilityListViewTester.switchToAvailabilityListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo), null);
        seleniumHandler.takeScreenShot(folder + "/availability-list-view.png");
        takeAvailabilityDialogScreenshots(folder);

        //-----------------
        //LocationListView
        //-----------------
        locationListViewTester.switchToLocationListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo), null);
        seleniumHandler.takeScreenShot(folder + "/location-list-view.png");
        takeLocationDialogScreenshots(folder);

        //-----------------
        //OffdayListView
        //-----------------
        offDayListViewTester.switchToOffDayListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo), null);
        seleniumHandler.takeScreenShot(folder + "/offday-list-view.png");
        takeOffDayDialogScreenshots(folder);

        //-----------------
        //UserWorkWeekListView
        //-----------------
        userWorkWeekListViewTester.switchToUserWorkWeekListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo), null);
        seleniumHandler.takeScreenShot(folder + "/user-work-week-list-view.png");
        takeUserWorkWeekDialogScreenshots(folder);

        //-----------------
        //WorkWeekListView
        //-----------------
        workWeekListViewTester.switchToWorkWeekListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
        seleniumHandler.takeScreenShot(folder + "/work-week-list-view.png");
        takeWorkWeekDialogScreenshots(folder);
        if (false) {
        }
        takeAdministrationScreenshots(folder);
        aboutViewTester.logout();
        takeAuthenticationScreenshots(folder, eTheme);
    }

    /**
     * Captures the supported non-AI screens and dialogs in light and dark themes.
     * Setup is excluded because it requires an unconfigured installation; AI interactions
     * are excluded because they need separate GPU-backed services.
     *
     * @param randomCase deterministic screenshot fixture
     * @param testInfo   current test metadata
     * @throws Exception when fixture generation or screenshot capture fails
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void takeScreenshots(RandomCase randomCase, TestInfo testInfo) throws Exception {
        seleniumHandler.setWindowSize(InstructionVideo.VIDEO_WIDTH, InstructionVideo.VIDEO_EXTENDED_HEIGHT);
//        seleniumHandler.setWindowSize(1700, 1200);

        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateProductsIfNeeded(testInfo, randomCase);
        StableDiffusionService.setEnabled(false);
        read();
        seleniumHandler.startRecording(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
        userName    = "Christopher Paul";
        productName = peg.nameGenerator.generateProductName(0);
        versionName = peg.nameGenerator.generateVersionName(0);
        featureName = peg.nameGenerator.generateFeatureName(0);
        sprintName  = peg.nameGenerator.generateSprintName(1);


        takeScreenshot(testInfo, "../kassandra.wiki/light-screenshots/", ETheme.light);
        takeScreenshot(testInfo, "../kassandra.wiki/dark-screenshots/", ETheme.dark);

        seleniumHandler.waitUntilBrowserClosed(5000);
    }

    private void takeScrollableScreenshots(String id, String filePrefix) {
        WebElement scroller = seleniumHandler.findElement(By.id(id));
        seleniumHandler.executeJavaScript("arguments[0].scrollTop = 0;", scroller);
        int part = 1;
        while (true) {
            seleniumHandler.takeScreenShot(filePrefix + (part == 1 ? "" : "-" + part) + ".png");
            boolean advanced = Boolean.TRUE.equals(seleniumHandler.executeJavaScript(
                    "const e = arguments[0]; const before = e.scrollTop; e.scrollTop += e.clientHeight; return e.scrollTop > before;",
                    scroller));
            if (!advanced)
                break;
            part++;
        }
    }

    /**
     * Takes screenshots of Sprint create, edit and delete dialogs
     */
    private void takeSprintDialogScreenshots(String folder) {
        // Create sprint dialog
        seleniumHandler.click(SprintListView.CREATE_SPRINT_BUTTON);
        seleniumHandler.waitForElementToBeClickable(SprintDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(SprintDialog.SPRINT_DIALOG), SprintDialog.SPRINT_DIALOG, folder + "/sprint-create-dialog.png");
        sprintListViewTester.closeDialog(SprintDialog.CANCEL_BUTTON);

        // Edit sprint dialog - open action menu first, then edit
        seleniumHandler.click(SprintListView.SPRINT_GRID_EDIT_BUTTON_PREFIX + sprintName);
        seleniumHandler.waitForElementToBeClickable(SprintDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(SprintDialog.SPRINT_DIALOG), SprintDialog.SPRINT_DIALOG, folder + "/sprint-edit-dialog.png");
        sprintListViewTester.closeDialog(SprintDialog.CANCEL_BUTTON);

        // Delete sprint dialog - open action menu first, then delete
        seleniumHandler.click(SprintListView.SPRINT_GRID_DELETE_BUTTON_PREFIX + sprintName);
        seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, folder + "/sprint-delete-dialog.png");
        sprintListViewTester.closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);
    }

    /**
     * Takes screenshots of User create, edit and delete dialogs
     */
    private void takeUserDialogScreenshots(String folder) {
        // Create user dialog
        seleniumHandler.click(UserListView.CREATE_USER_BUTTON);
        seleniumHandler.waitForElementToBeClickable(UserDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(UserDialog.USER_DIALOG), UserDialog.USER_DIALOG, folder + "/user-create-dialog.png");
        userListViewTester.closeDialog(UserDialog.CANCEL_BUTTON);


        // Edit user dialog - open action menu first, then edit
        // Get first user in the list
        seleniumHandler.wait(300);
        seleniumHandler.click(UserListView.USER_GRID_EDIT_BUTTON_PREFIX + userName);
        seleniumHandler.waitForElementToBeClickable(UserDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(UserDialog.USER_DIALOG), UserDialog.USER_DIALOG, folder + "/user-edit-dialog.png");
        userListViewTester.closeDialog(UserDialog.CANCEL_BUTTON);

        // Delete user dialog - open action menu first, then delete
        seleniumHandler.wait(300);
        seleniumHandler.click(UserListView.USER_GRID_DELETE_BUTTON_PREFIX + userName);
        seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, folder + "/user-delete-dialog.png");
        userListViewTester.closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);
    }

//    private void takeTaskDialogScreenshots(String folder) {
//        seleniumHandler.click(Backlog.CREATE_MILESTONE_BUTTON_ID);
//        seleniumHandler.ensureIsInList(ProductListView.PRODUCT_GRID_NAME_PREFIX, "New Milestone-34");
//        seleniumHandler.click(Backlog.CREATE_STORY_BUTTON_ID);
//        seleniumHandler.ensureIsInList(ProductListView.PRODUCT_GRID_NAME_PREFIX, "New Story-2");
//        seleniumHandler.click(Backlog.CREATE_TASK_BUTTON_ID);
//        seleniumHandler.ensureIsInList(ProductListView.PRODUCT_GRID_NAME_PREFIX, "New Task-3");
//        // select the milestone
//        seleniumHandler.click(TaskGrid.TASK_GRID_NAME_PREFIX + "New Milestone-34");
//        // select start cell
//        seleniumHandler.click(TaskGrid.TASK_GRID_START_PREFIX + "New Milestone-34");
//        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, folder + "/task-view-list.png");
//    }

    /**
     * Takes screenshots of the {@link UserGroupListView} and the UserGroup create, edit, and delete dialogs.
     * <p>
     * The edit screenshot uses the always-present "Team" group. The delete screenshot opens the confirm
     * dialog on the always-present "All" group and then cancels, leaving the data unchanged.
     *
     * @param folder destination folder for the screenshot files
     */
    private void takeUserGroupDialogScreenshots(String folder) {
        // Create dialog
        {
            seleniumHandler.click(UserGroupListView.CREATE_GROUP_BUTTON);
            seleniumHandler.waitForElementToBeClickable(UserGroupDialog.CANCEL_BUTTON);
            seleniumHandler.takeElementScreenShot(
                    seleniumHandler.findDialogOverlayElement(UserGroupDialog.GROUP_DIALOG),
                    UserGroupDialog.GROUP_DIALOG,
                    folder + "/user-group-create-dialog.png");
            userGroupListViewTester.closeDialog(UserGroupDialog.CANCEL_BUTTON);
        }

        // Edit dialog – use the always-present "Team" group
        {
            seleniumHandler.click(UserGroupListView.GROUP_GRID_EDIT_BUTTON_PREFIX + "Team");
            seleniumHandler.waitForElementToBeClickable(UserGroupDialog.CANCEL_BUTTON);
            seleniumHandler.takeElementScreenShot(
                    seleniumHandler.findDialogOverlayElement(UserGroupDialog.GROUP_DIALOG),
                    UserGroupDialog.GROUP_DIALOG,
                    folder + "/user-group-edit-dialog.png");
            userGroupListViewTester.closeDialog(UserGroupDialog.CANCEL_BUTTON);
        }

        // Delete dialog – use the always-present "All" group; cancel to leave it intact
        {
            seleniumHandler.click(UserGroupListView.GROUP_GRID_DELETE_BUTTON_PREFIX + "All");
            seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON);
            seleniumHandler.takeElementScreenShot(
                    seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG),
                    ConfirmDialog.CONFIRM_DIALOG,
                    folder + "/user-group-delete-dialog.png");
            userGroupListViewTester.closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);
        }
    }

    /**
     * Navigates to the user profile and captures it without invoking AI image generation.
     *
     * @param folder destination folder for the screenshot files
     */
    private void takeUserProfileScreenshots(String folder) {
        // Navigate to the profile page via the user menu
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_VIEW_PROFILE);
        seleniumHandler.waitForElementToBeClickable(UserProfileView.PROFILE_PAGE_TITLE);
        seleniumHandler.takeScreenShot(folder + "/user-profile-view.png");

    }

    /**
     * Takes screenshots of UserWorkWeek create, edit and delete dialogs.
     */
    private void takeUserWorkWeekDialogScreenshots(String folder) {
        // Create dialog
        {
            seleniumHandler.click(UserWorkWeekListView.CREATE_BUTTON);
            seleniumHandler.waitForElementToBeClickable(UserWorkWeekDialog.CANCEL_BUTTON);
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(UserWorkWeekDialog.USER_WORK_WEEK_DIALOG), UserWorkWeekDialog.USER_WORK_WEEK_DIALOG, folder + "/user-work-week-create-dialog.png");
            userWorkWeekListViewTester.closeDialog(UserWorkWeekDialog.CANCEL_BUTTON);
        }

        // Edit dialog
        {
            String dateStr = lastUserWorkWeek.getStart().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            seleniumHandler.click(UserWorkWeekListView.GRID_EDIT_BUTTON_PREFIX + dateStr);
            seleniumHandler.waitForElementToBeClickable(UserWorkWeekDialog.CANCEL_BUTTON);
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(UserWorkWeekDialog.USER_WORK_WEEK_DIALOG), UserWorkWeekDialog.USER_WORK_WEEK_DIALOG, folder + "/user-work-week-edit-dialog.png");
            userWorkWeekListViewTester.closeDialog(UserWorkWeekDialog.CANCEL_BUTTON);
        }

        // Delete dialog – create a temporary extra assignment so the delete button is enabled
        {
            userWorkWeekListViewTester.createWorkWeekAssignmentConfirm(firstDay, DefaultEntitiesInitializer.WORK_WEEK_5X8);
            String dateStr = firstDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            seleniumHandler.click(UserWorkWeekListView.GRID_DELETE_BUTTON_PREFIX + dateStr);
            seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON);
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, folder + "/user-work-week-delete-dialog.png");
            userWorkWeekListViewTester.closeConfirmDialog(ConfirmDialog.CONFIRM_BUTTON);
        }
    }

    /**
     * Takes screenshots of Version create, edit and delete dialogs.
     */
    private void takeVersionDialogScreenshots(String folder) {
        // Create version dialog
        seleniumHandler.click(VersionListView.CREATE_VERSION_BUTTON);
        seleniumHandler.waitForElementToBeClickable(VersionDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(VersionDialog.VERSION_DIALOG), VersionDialog.VERSION_DIALOG, folder + "/version-create-dialog.png");
        versionListViewTester.closeDialog(VersionDialog.CANCEL_BUTTON);

        // Edit version dialog - open action menu first, then edit

        seleniumHandler.click(VersionListView.VERSION_GRID_EDIT_BUTTON_PREFIX + versionName);
        seleniumHandler.waitForElementToBeClickable(VersionDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(VersionDialog.VERSION_DIALOG), VersionDialog.VERSION_DIALOG, folder + "/version-edit-dialog.png");
        versionListViewTester.closeDialog(VersionDialog.CANCEL_BUTTON);

        // Delete version dialog - open action menu first, then delete
        seleniumHandler.click(VersionListView.VERSION_GRID_DELETE_BUTTON_PREFIX + versionName);
        seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, folder + "/version-delete-dialog.png");
        versionListViewTester.closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);
    }

    /**
     * Takes screenshots of WorkWeek create, edit and delete dialogs.
     */
    private void takeWorkWeekDialogScreenshots(String folder) {
        // Create dialog
        {
            seleniumHandler.click(WorkWeekListView.CREATE_BUTTON);
            seleniumHandler.waitForElementToBeClickable(WorkWeekDialog.CANCEL_BUTTON);
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(WorkWeekDialog.WORK_WEEK_DIALOG), WorkWeekDialog.WORK_WEEK_DIALOG, folder + "/work-week-create-dialog.png");
            workWeekListViewTester.closeDialog(WorkWeekDialog.CANCEL_BUTTON);
        }

        // Edit dialog – use the always-present default work week
        {
            seleniumHandler.click(WorkWeekListView.GRID_EDIT_BUTTON_PREFIX + DefaultEntitiesInitializer.WORK_WEEK_5X8);
            seleniumHandler.waitForElementToBeClickable(WorkWeekDialog.CANCEL_BUTTON);
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(WorkWeekDialog.WORK_WEEK_DIALOG), WorkWeekDialog.WORK_WEEK_DIALOG, folder + "/work-week-edit-dialog.png");
            workWeekListViewTester.closeDialog(WorkWeekDialog.CANCEL_BUTTON);
        }

        // Delete dialog – create a temporary work week first (WORK_WEEK_5X8 cannot be deleted)
        {
            workWeekListViewTester.createWorkWeekConfirm("Islamic Sun-Wed 4x8", "Sunday–Wednesday 8-hour 4 day work week with a 1-hour lunch break");
            seleniumHandler.click(WorkWeekListView.GRID_DELETE_BUTTON_PREFIX + "Islamic Sun-Wed 4x8");
            seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON);
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, folder + "/work-week-delete-dialog.png");
            workWeekListViewTester.closeConfirmDialog(ConfirmDialog.CONFIRM_BUTTON);
        }
    }

}
