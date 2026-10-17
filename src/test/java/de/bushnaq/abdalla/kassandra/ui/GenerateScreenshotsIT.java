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

import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.config.DefaultEntitiesInitializer;
import de.bushnaq.abdalla.kassandra.dao.OidcProviderDAO;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.report.dao.ETheme;
import de.bushnaq.abdalla.kassandra.repository.SecurityConfigurationRepository;
import de.bushnaq.abdalla.kassandra.rest.api.AuditLogApi;
import de.bushnaq.abdalla.kassandra.rest.api.ProductAclApi;
import de.bushnaq.abdalla.kassandra.rest.api.ServerSettingsApi;
import de.bushnaq.abdalla.kassandra.rest.api.UndoRedoApi;
import de.bushnaq.abdalla.kassandra.service.SecurityConfigurationService;
import de.bushnaq.abdalla.kassandra.ui.component.*;
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
import org.junit.jupiter.api.*;
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
import org.springframework.test.annotation.DirtiesContext;
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

import static org.junit.jupiter.api.Assertions.*;

@Tag("IntegrationUiTest")
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=${test.server.port:0}",
                "spring.security.basic.enabled=false",
                // Serve newly captured assets before the next build copies them onto the classpath.
                "spring.web.resources.static-locations=file:./src/main/resources/META-INF/resources/,classpath:/META-INF/resources/,classpath:/resources/,classpath:/static/,classpath:/public/"
        }
)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
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
    private       boolean                         previousForceScreenshotRefresh;
    private       Integer                         previousScreenshotTolerance;
    @Autowired
    private       ProductAclApi                   productAclApi;
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

    @BeforeEach
    void configureDocumentationScreenshots() {
        previousScreenshotTolerance    = seleniumHandler.getDocumentationScreenshotTolerance();
        previousForceScreenshotRefresh = seleniumHandler.isForceDocumentationScreenshotRefresh();
        int tolerance = Integer.parseInt(System.getProperty("screenshot.channel-tolerance", "4"));
        if (tolerance < 0 || tolerance > 255) {
            throw new IllegalArgumentException("screenshot.channel-tolerance must be between 0 and 255");
        }
        String force = System.getProperty("screenshot.force-refresh", "false");
        if (!"true".equalsIgnoreCase(force) && !"false".equalsIgnoreCase(force)) {
            throw new IllegalArgumentException("screenshot.force-refresh must be true or false");
        }
        seleniumHandler.setDocumentationScreenshotTolerance(tolerance);
        seleniumHandler.setForceDocumentationScreenshotRefresh(Boolean.parseBoolean(force));
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

    private void prepareScreenshotFixture(RandomCase randomCase, TestInfo testInfo) throws Exception {
        seleniumHandler.setWindowSize(InstructionVideo.VIDEO_WIDTH, InstructionVideo.VIDEO_EXTENDED_HEIGHT);
        StableDiffusionService.setEnabled(true);
        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateProductsIfNeeded(testInfo, randomCase);
//        StableDiffusionService.setEnabled(false);
        read();
        userName    = "Christopher Paul";
        productName = peg.nameGenerator.generateProductName(0);
        versionName = peg.nameGenerator.generateVersionName(0);
        featureName = peg.nameGenerator.generateFeatureName(0);
        sprintName  = peg.nameGenerator.generateSprintName(1);
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

    @AfterEach
    void restoreDocumentationScreenshots() {
        seleniumHandler.setDocumentationScreenshotTolerance(previousScreenshotTolerance);
        seleniumHandler.setForceDocumentationScreenshotRefresh(previousForceScreenshotRefresh);
    }

    private void scrollIntoView(String id) {
        seleniumHandler.waitUntil(ExpectedConditions.refreshed(driver -> {
            WebElement element = ExpectedConditions.elementToBeClickable(By.id(id)).apply(driver);
            if (element == null)
                return false;
            seleniumHandler.executeJavaScript("arguments[0].scrollIntoView({block: 'start', inline: 'nearest'});", element);
            return true;
        }));
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
    private void takeActiveSprintsDialogScreenshots(String folder) throws IOException {
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
        takeWorklogHelpScreenshots();
        seleniumHandler.takeElementScreenShot(
                seleniumHandler.findDialogOverlayElement(WorklogDialog.WORKLOG_DIALOG),
                WorklogDialog.WORKLOG_DIALOG,
                folder + "/worklog-create-dialog.png");
        activeSprintsTester.closeDialog(WorklogDialog.CANCEL_BUTTON);

        // TaskDialog — opened by clicking the task card title link (stops card-click propagation)
        String taskTitleId = TaskCard.TASK_CARD_TITLE_ID_PREFIX + leafTask.getKey();
        seleniumHandler.click(taskTitleId);
        seleniumHandler.waitForElementToBeClickable(TaskDialog.CANCEL_BUTTON);
        takeDialogHelpElements(TaskDialog.class, TaskDialog.HELP_BUTTON_ID, TaskDialog.CALCULATIONS_ID,
                TaskDialog.NAME_FIELD_ID, TaskDialog.NOTES_FIELD_ID, TaskDialog.ESTIMATES_ID,
                TaskDialog.MANUAL_SCHEDULING_ID, TaskDialog.WORKLOGS_ID,
                TaskDialog.SAVE_TASK_BUTTON, TaskDialog.CANCEL_BUTTON, TaskDialog.LOG_WORK_BUTTON_ID);
        boolean manual = seleniumHandler.getCheckbox(TaskDialog.MANUAL_SCHEDULING_ID);
        try {
            seleniumHandler.setCheckCheckbox(TaskDialog.MANUAL_SCHEDULING_ID, true);
            seleniumHandler.takeHelpElementScreenshot(TaskDialog.class, TaskDialog.START_FIELD_ID);
        } finally {
            seleniumHandler.setCheckCheckbox(TaskDialog.MANUAL_SCHEDULING_ID, manual);
        }
        seleniumHandler.takeElementScreenShot(
                seleniumHandler.findDialogOverlayElement(TaskDialog.TASK_DIALOG),
                TaskDialog.TASK_DIALOG,
                folder + "/task-dialog.png");

        Worklog worklog = worklogs.stream().filter(w -> leafTask.getId().equals(w.getTaskId())).findFirst().orElseThrow();
        seleniumHandler.click(TaskDialog.WORKLOG_EDIT_BUTTON_PREFIX + worklog.getId());
        seleniumHandler.waitForElementToBeClickable(WorklogDialog.CANCEL_BUTTON);
        takeDialogHelpElements(WorklogDialog.class, WorklogDialog.HELP_BUTTON_ID);
        takeDialogScreenshot(WorklogDialog.WORKLOG_DIALOG, folder + "/worklog-edit-dialog.png");
        seleniumHandler.click(WorklogDialog.CANCEL_BUTTON);
        seleniumHandler.waitForElementToBeClickable(TaskDialog.WORKLOG_DELETE_BUTTON_PREFIX + worklog.getId());
        seleniumHandler.click(TaskDialog.WORKLOG_DELETE_BUTTON_PREFIX + worklog.getId());
        seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON);
        takeDialogScreenshot(ConfirmDialog.CONFIRM_DIALOG, folder + "/worklog-delete-dialog.png");
        seleniumHandler.click(ConfirmDialog.CANCEL_BUTTON);
        seleniumHandler.waitForElementToBeClickable(TaskDialog.CANCEL_BUTTON);
        scrollIntoView(TaskDialog.CANCEL_BUTTON);
        activeSprintsTester.closeDialog(TaskDialog.CANCEL_BUTTON);
    }

    private void takeActiveSprintsElementScreenshots(ETheme theme) throws IOException {
        takeHelpElements(ActiveSprints.class, "page-header", theme, ActiveSprints.ID_SEARCH_FIELD,
                ActiveSprints.ID_USER_SELECTOR, ActiveSprints.ID_SPRINT_SELECTOR, ActiveSprints.ID_GROUPING_MODE_SELECTOR,
                ActiveSprints.ID_CLEAR_FILTERS_BUTTON, ActiveSprints.OPEN_GANTT_BURNDOWN_CHART_BUTTON_ID,
                ActiveSprints.GANTT_BURNDOWN_CONTAINER_ID);
        seleniumHandler.takeHelpElementScreenshot(ActiveSprints.class, ActiveSprints.BOARD_ID);
        seleniumHandler.takeHelpRowActionScreenshot(ActiveSprints.class, TaskCard.TASK_CARD_TITLE_ID_PREFIX);
        seleniumHandler.prepareHelpScreenshots("page-header");
        takePageHelpScreenshot(ActiveSprints.class, ActiveSprints.HELP_BUTTON_ID);
    }

    private void takeAdministrationScreenshots(String folder, ETheme theme) throws IOException {
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_AUDIT);
        seleniumHandler.waitForElementToBeClickable(AuditLogView.PAGE_TITLE_ID);
        seleniumHandler.takeScreenShot(folder + "/audit-log-view.png");
        takeHelpElements(AuditLogView.class, AuditLogView.PAGE_TITLE_ID, theme,
                AuditLogView.SEARCH_FIELD_ID, AuditLogView.USER_FILTER_ID, AuditLogView.ACTION_FILTER_ID,
                AuditLogView.FROM_FILTER_ID, AuditLogView.TO_FILTER_ID, AuditLogView.APPLY_BUTTON_ID,
                AuditLogView.PREVIOUS_BUTTON_ID, AuditLogView.NEXT_BUTTON_ID, AuditLogView.PAGE_NUMBER_ID, AuditLogView.GO_BUTTON_ID);
        takePageHelpScreenshot(AuditLogView.class, AuditLogView.HELP_BUTTON_ID);
        seleniumHandler.takeHelpGridScreenshot(AuditLogView.class, AuditLogView.GRID_ID);
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
        seleniumHandler.takeHelpRowActionScreenshot(AuditLogView.class, AuditLogView.EVENT_ID_PREFIX);
        seleniumHandler.takeHelpRowActionScreenshot(AuditLogView.class, AuditLogView.DETAILS_ID_PREFIX);

        navigateTo(OidcProviderManagementView.ROUTE);
        seleniumHandler.waitForElementToBeClickable(OidcProviderManagementView.PAGE_TITLE_ID);
        seleniumHandler.takeScreenShot(folder + "/identity-providers-view.png");
        takeOidcProviderElementScreenshots(theme);

        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_MANAGE_SETTINGS);
        seleniumHandler.waitForElementToBeClickable(ServerSettingsView.PAGE_TITLE_ID);
        List<ServerSetting> settings = serverSettingsApi.getAll();
        takeHelpElements(ServerSettingsView.class, ServerSettingsView.PAGE_TITLE_ID, theme, ServerSettingsView.HELP_BUTTON_ID);
        takePageHelpScreenshot(ServerSettingsView.class, ServerSettingsView.HELP_BUTTON_ID);
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
                takeServerSettingsElementScreenshots(theme, category, settings);
                takeScrollableScreenshots(ServerSettingsView.DETAILS_ID, folder + "/server-settings-" + category);
            } finally {
                if (enableForCapture) {
                    serverSettingsApi.updateCategoryEnabled(category, false);
                }
            }
        }
    }

    private void takeAuthenticationScreenshots(String folder, ETheme theme) throws IOException {
        var                   configuration        = securityConfigurationService.getConfiguration();
        String                originalRecoveryHash = configuration.getRecoveryPasswordHash();
        List<OidcProviderDAO> enabledProviders     = oidcProviderService.getProviders().stream().filter(OidcProviderDAO::isEnabled).toList();
        OidcProviderDAO       primary              = enabledProviders.getFirst();
        OidcProviderDAO       additional           = null;
        try {
            configuration.setRecoveryPasswordHash(passwordEncoder.encode("screenshot-recovery-password"));
            securityConfigurationRepository.saveAndFlush(configuration);
            takeLoginScreenshot(folder + "/login-view.png", theme, LoginView.OIDC_LOGIN_BUTTON);
            seleniumHandler.prepareHelpScreenshots(LoginView.LOGIN_VIEW);
            seleniumHandler.takeHelpElementScreenshot(LoginView.class, LoginView.OIDC_LOGIN_BUTTON);

            // Providers require distinct issuers; the existing master realm also supports discovery.
            additional = oidcProviderService.createProvider("Keycloak master", URI.create(primary.getIssuerUri()).resolve("master").toString(),
                    "admin-cli", "screenshot-client-secret", List.of(primary.getScopes().split(",")));
            takeLoginScreenshot(folder + "/login-multiple-providers.png", theme, LoginView.OIDC_PROVIDER_SELECTOR);
            seleniumHandler.prepareHelpScreenshots(LoginView.LOGIN_VIEW);
            seleniumHandler.takeHelpElementScreenshot(LoginView.class, LoginView.OIDC_PROVIDER_SELECTOR);
            seleniumHandler.takeHelpElementScreenshot(LoginView.class, LoginView.OIDC_PROVIDER_SIGN_IN_BUTTON);
            oidcProviderService.disableProvider(additional.getId());
            for (OidcProviderDAO provider : enabledProviders) {
                oidcProviderService.disableProvider(provider.getId());
            }
            takeLoginScreenshot(folder + "/login-no-providers.png", theme, LoginView.SETUP_LINK);
            seleniumHandler.prepareHelpScreenshots(LoginView.LOGIN_VIEW);
            seleniumHandler.takeHelpElementScreenshot(LoginView.class, LoginView.SETUP_LINK);
            navigateTo(RecoveryView.ROUTE);
            seleniumHandler.waitForElementToBeClickable(RecoveryView.VIEW_ID);
            applyLoginTheme(theme);
            seleniumHandler.takeScreenShot(folder + "/recovery-view.png");
            seleniumHandler.prepareHelpScreenshots(RecoveryView.VIEW_ID);
            seleniumHandler.takeHelpElementScreenshot(RecoveryView.class, RecoveryView.FORM_ID);
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
    private void takeAvailabilityDialogScreenshots(String folder) throws IOException {
        // Create availability dialog
        {
            seleniumHandler.click(AvailabilityListView.CREATE_AVAILABILITY_BUTTON);
            seleniumHandler.waitForElementToBeClickable(AvailabilityDialog.CANCEL_BUTTON); // Wait for dialog
            takeDialogHelpElements(AvailabilityDialog.class, AvailabilityDialog.HELP_BUTTON_ID,
                    AvailabilityDialog.AVAILABILITY_START_DATE_FIELD, AvailabilityDialog.AVAILABILITY_PERCENTAGE_FIELD,
                    AvailabilityDialog.CONFIRM_BUTTON, AvailabilityDialog.CANCEL_BUTTON);
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

    private void takeAvailabilityElementScreenshots(ETheme theme) throws IOException {
        takeHelpListElements(AvailabilityListView.class, AvailabilityListView.AVAILABILITY_LIST_PAGE_TITLE, theme,
                AvailabilityListView.AVAILABILITY_GRID, AvailabilityListView.CREATE_AVAILABILITY_BUTTON,
                AvailabilityListView.AVAILABILITY_GLOBAL_FILTER, AvailabilityListView.AVAILABILITY_ROW_COUNTER,
                AvailabilityListView.AVAILABILITY_GRID_EDIT_BUTTON_PREFIX, AvailabilityListView.AVAILABILITY_GRID_DELETE_BUTTON_PREFIX,
                AvailabilityCalendarComponent.CALENDAR_PREV_YEAR_BTN, AvailabilityCalendarComponent.CALENDAR_NEXT_YEAR_BTN);
        seleniumHandler.takeHelpRowActionScreenshot(AvailabilityListView.class, AvailabilityCalendarComponent.LEGEND_ITEM_ID_PREFIX);
        seleniumHandler.prepareHelpScreenshots(AvailabilityListView.AVAILABILITY_LIST_PAGE_TITLE);
        takePageHelpScreenshot(AvailabilityListView.class, AvailabilityListView.HELP_BUTTON_ID);
    }

    private void takeBacklogElementScreenshots(ETheme theme) throws IOException {
        takeHelpElements(Backlog.class, Backlog.BACKLOG_PAGE_TITLE_ID, theme,
                Backlog.SEARCH_FIELD_ID, Backlog.USER_SELECTOR_ID, Backlog.SPRINT_SELECTOR_ID,
                Backlog.CLEAR_FILTER_BUTTON_ID, Backlog.EXPAND_TOGGLE_BUTTON_ID,
                Backlog.CREATE_MILESTONE_BUTTON_ID, Backlog.CREATE_STORY_BUTTON_ID, Backlog.CREATE_TASK_BUTTON_ID,
                Backlog.EDIT_BUTTON_ID, Backlog.SPRINT_STATUS_SELECTOR_ID, Backlog.OPEN_GANTT_CHART_BUTTON_ID);
        seleniumHandler.takeHelpGridScreenshot(Backlog.class, Backlog.TASK_GRID_ID);
        seleniumHandler.takeHelpRowActionScreenshot(Backlog.class, TaskGrid.TASK_GRID_DELETE_BUTTON_PREFIX);
        seleniumHandler.takeHelpElementScreenshot(Backlog.class, Backlog.GANTT_CHART_CONTAINER_ID);
        seleniumHandler.click(Backlog.EDIT_BUTTON_ID);
        try {
            seleniumHandler.waitForElementToBeClickable(Backlog.CANCEL_BUTTON_ID);
            takeHelpElements(Backlog.class, Backlog.BACKLOG_PAGE_TITLE_ID, theme,
                    Backlog.SAVE_BUTTON_ID, Backlog.CANCEL_BUTTON_ID);
            seleniumHandler.takeHelpRowActionScreenshot(Backlog.class, TaskGrid.TASK_GRID_DEPENDENCY_PREFIX);
        } finally {
            seleniumHandler.click(Backlog.CANCEL_BUTTON_ID);
            seleniumHandler.waitForChartRendered(RenderUtil.GANTT_CHART);
            seleniumHandler.prepareHelpScreenshots(Backlog.BACKLOG_PAGE_TITLE_ID);
        }
        takePageHelpScreenshot(Backlog.class, Backlog.HELP_BUTTON_ID);
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
    private void takeDependencyDialogScreenshots(String folder) throws IOException {
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
        takeDialogHelpElements(DependencyDialog.class, DependencyDialog.HELP_BUTTON_ID, DependencyDialog.CHOICES_ID,
                DependencyDialog.QUICK_EDIT_ID, DependencyDialog.SAVE_BUTTON, DependencyDialog.CANCEL_BUTTON);
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
    private void takeFeatureDialogScreenshots(String folder) throws IOException {
        // Create project dialog
        seleniumHandler.click(FeatureListView.CREATE_FEATURE_BUTTON_ID);
        seleniumHandler.waitForElementToBeClickable(FeatureDialog.CANCEL_BUTTON); // Wait for dialog
        takeDialogHelpElements(FeatureDialog.class, FeatureDialog.HELP_BUTTON_ID,
                FeatureDialog.CONFIRM_BUTTON, FeatureDialog.CANCEL_BUTTON);
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(FeatureDialog.FEATURE_DIALOG), FeatureDialog.FEATURE_DIALOG, folder + "/feature-create-dialog.png");
        featureListViewTester.closeDialog(FeatureDialog.CANCEL_BUTTON);

        // Edit project dialog - open action menu first, then edit
        seleniumHandler.click(FeatureListView.FEATURE_GRID_EDIT_BUTTON_PREFIX + featureName);
        seleniumHandler.waitForElementToBeClickable(FeatureDialog.CANCEL_BUTTON); // Wait for dialog
        takeDialogHelpElements(FeatureDialog.class, FeatureDialog.HELP_BUTTON_ID,
                FeatureDialog.FEATURE_NAME_FIELD, FeatureDialog.GENERATE_IMAGE_BUTTON);
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
        seleniumHandler.prepareHelpScreenshots(FeatureListView.FEATURE_LIST_PAGE_TITLE);
    }

    private void takeHelpElements(Class<?> view, String titleId, ETheme theme, String... elementIds) throws IOException {
        seleniumHandler.prepareHelpScreenshots(titleId);
        seleniumHandler.setHelpScreenshotTheme(theme);
        for (String elementId : elementIds) {
            seleniumHandler.takeHelpElementScreenshot(view, elementId);
        }
        assertEquals(ETheme.dark.equals(theme), Boolean.TRUE.equals(seleniumHandler.executeJavaScript(
                        "return document.body.matches('[theme~=dark]') || document.documentElement.matches('[theme~=dark]')")),
                "Captured controls must use the requested theme: " + view.getSimpleName());
        seleniumHandler.prepareHelpScreenshots(titleId);
    }

    private void takeDialogHelpElements(Class<?> dialog, String helpButtonId, String... elementIds) throws IOException {
        seleniumHandler.prepareHelpScreenshots(elementIds.length == 0 ? helpButtonId : elementIds[0]);
        seleniumHandler.takeHelpElementScreenshot(dialog, helpButtonId);
        for (String elementId : elementIds) {
            seleniumHandler.takeHelpElementScreenshot(dialog, elementId);
        }
    }

    private void takeWorklogHelpScreenshots() throws IOException {
        takeDialogHelpElements(WorklogDialog.class, WorklogDialog.HELP_BUTTON_ID,
                WorklogDialog.TIME_SPENT_FIELD, WorklogDialog.TIME_REMAINING_FIELD,
                WorklogDialog.DATETIME_PICKER, WorklogDialog.COMMENT_FIELD,
                WorklogDialog.SAVE_BUTTON, WorklogDialog.CANCEL_BUTTON);
    }

    private void takeImagePromptHelpScreenshots(String buttonId, boolean headers) throws IOException {
        seleniumHandler.click(buttonId);
        try {
            seleniumHandler.waitForElementToBeClickable(ImagePromptDialog.ID_CANCEL_BUTTON);
            if (headers) {
                takeDialogHelpElements(ImagePromptDialog.class, ImagePromptDialog.HELP_BUTTON_ID,
                        ImagePromptDialog.ID_AVATAR_PROMPT_FIELD, ImagePromptDialog.ID_NEGATIVE_PROMPT_FIELD,
                        ImagePromptDialog.ID_HEADER_PROMPT_FIELD, ImagePromptDialog.ID_STYLE_SELECT,
                        ImagePromptDialog.ID_RANDOM_STYLE_BUTTON, "light-column", "dark-column",
                        "light-header-column", "dark-header-column", ImagePromptDialog.ID_GENERATE_BUTTON,
                        ImagePromptDialog.ID_ACCEPT_BUTTON, ImagePromptDialog.ID_CANCEL_BUTTON);
            } else {
                // Avatar-only forms use the same controls but have no header editors.
                assertTrue(seleniumHandler.getDriver().findElements(By.id(ImagePromptDialog.ID_HEADER_PROMPT_FIELD)).isEmpty());
                assertTrue(seleniumHandler.findElement(By.id(ImagePromptDialog.HELP_BUTTON_ID)).isDisplayed());
            }
        } finally {
            seleniumHandler.click(ImagePromptDialog.ID_CANCEL_BUTTON);
        }
    }

    private void takeReplayHelpScreenshots(String direction) throws IOException {
        seleniumHandler.prepareHelpScreenshots(UndoHistoryConfirmationDialog.CANCEL_BUTTON);
        for (String id : List.of(UndoHistoryConfirmationDialog.HELP_BUTTON_ID,
                UndoHistoryConfirmationDialog.CONFIRM_BUTTON, UndoHistoryConfirmationDialog.CANCEL_BUTTON)) {
            seleniumHandler.takeHelpElementScreenshot(UndoHistoryConfirmationDialog.class, id, direction);
        }
        WebElement operations = seleniumHandler.findElement(By.id(UndoHistoryConfirmationDialog.OPERATIONS_ID));
        for (WebElement details : operations.findElements(By.cssSelector("vaadin-details"))) {
            if (details.getDomAttribute("opened") == null) {
                details.click();
            }
        }
        seleniumHandler.takeHelpElementScreenshot(UndoHistoryConfirmationDialog.class,
                UndoHistoryConfirmationDialog.OPERATIONS_ID, direction);
    }

    private void takeHelpListElements(Class<?> view, String titleId, ETheme theme, String gridId,
                                      String createId, String filterId, String counterId,
                                      String editPrefix, String deletePrefix, String... additionalIds) throws IOException {
        takeHelpElements(view, titleId, theme, createId, filterId, filterId + "-help-button", counterId);
        for (String elementId : additionalIds) {
            seleniumHandler.takeHelpElementScreenshot(view, elementId);
        }
        seleniumHandler.takeHelpGridScreenshot(view, gridId);
        seleniumHandler.takeHelpRowActionScreenshot(view, editPrefix);
        seleniumHandler.takeHelpRowActionScreenshot(view, deletePrefix);
        seleniumHandler.prepareHelpScreenshots(titleId);
    }

    private void takeKassandraElementScreenshots(ETheme theme) throws IOException {
        navigateTo("kassandra");
        takeHelpElements(Kassandra.class, Kassandra.AI_VIEW_TITLE, theme,
                Kassandra.AI_QUERY_INPUT, Kassandra.AI_SUBMIT_BUTTON, Kassandra.AI_CLEAR_BUTTON, Kassandra.AI_RESPONSE_AREA);
        takePageHelpScreenshot(Kassandra.class, Kassandra.HELP_BUTTON_ID);
    }

    /**
     * Takes screenshots of Location create, edit and delete dialogs
     */
    private void takeLocationDialogScreenshots(String folder) throws IOException {
        // Create location dialog
        {
            seleniumHandler.click(LocationListView.CREATE_LOCATION_BUTTON);
            seleniumHandler.waitForElementToBeClickable(LocationDialog.CANCEL_BUTTON); // Wait for dialog
            takeDialogHelpElements(LocationDialog.class, LocationDialog.HELP_BUTTON_ID,
                    LocationDialog.LOCATION_COUNTRY_FIELD, LocationDialog.LOCATION_STATE_FIELD,
                    LocationDialog.LOCATION_START_DATE_FIELD, LocationDialog.CONFIRM_BUTTON, LocationDialog.CANCEL_BUTTON);
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

    private void takeLocationElementScreenshots(ETheme theme) throws IOException {
        takeHelpListElements(LocationListView.class, LocationListView.LOCATION_LIST_PAGE_TITLE, theme,
                LocationListView.LOCATION_GRID, LocationListView.CREATE_LOCATION_BUTTON,
                LocationListView.LOCATION_GLOBAL_FILTER, LocationListView.LOCATION_ROW_COUNTER,
                LocationListView.LOCATION_GRID_EDIT_BUTTON_PREFIX, LocationListView.LOCATION_GRID_DELETE_BUTTON_PREFIX);
        takeOffDayCalendarElementScreenshots(LocationListView.class, LocationListView.LOCATION_LIST_PAGE_TITLE, theme);
        takePageHelpScreenshot(LocationListView.class, LocationListView.HELP_BUTTON_ID);
    }

    private void takeLoginScreenshot(String fileName, ETheme theme, String readyId) {
        navigateTo(LoginView.ROUTE);
        seleniumHandler.waitForElementToBeClickable(readyId);
        applyLoginTheme(theme);
        seleniumHandler.takeElementScreenShot(seleniumHandler.findElement(By.id(LoginView.LOGIN_VIEW)), LoginView.LOGIN_VIEW, fileName);
    }

    private void takeMainLayoutElementScreenshots(ETheme theme, String audience) throws IOException {
        boolean admin = "admin".equals(audience);
        seleniumHandler.setHelpScreenshotTheme(theme);
        seleniumHandler.prepareHelpScreenshots(FeatureListView.FEATURE_LIST_PAGE_TITLE);
        seleniumHandler.takeHelpContentScreenshot(MainLayout.class, MainLayout.ID_NAVIGATION, audience);
        seleniumHandler.takeHelpElementScreenshot(MainLayout.class, MainLayout.ID_USER_MENU, audience);
        if (admin) {
            seleniumHandler.takeHelpContentScreenshot(MainLayout.class, MainLayout.ID_BREADCRUMBS, null);
            for (String id : List.of(MainLayout.ID_LOGO,
                    MainLayout.ID_ACTION_HISTORY_BUTTON, MainLayout.ID_THEME_TOGGLE)) {
                seleniumHandler.takeHelpElementScreenshot(MainLayout.class, id);
            }
            for (String route : List.of("product-list", "version-list", "feature-list", "sprint-list",
                    "backlog", "active-sprints", "quality-board", "kassandra")) {
                seleniumHandler.takeHelpElementScreenshot(MainLayout.class, MainLayout.ID_TAB_BASE + route);
            }
        }
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.waitForElementToBeClickable(MainLayout.ID_USER_MENU_AVAILABILITY);
        seleniumHandler.takeHelpMenuScreenshot(MainLayout.class, MainLayout.ID_USER_MENU, audience);
        List<String> adminItems = List.of(MainLayout.ID_USER_MENU_IDENTITY_PROVIDERS, MainLayout.ID_USER_MENU_MANAGE_USER_GROUPS,
                MainLayout.ID_USER_MENU_MANAGE_USERS, MainLayout.ID_USER_MENU_MANAGE_WORK_WEEKS,
                MainLayout.ID_USER_MENU_MANAGE_SETTINGS, MainLayout.ID_USER_MENU_AUDIT);
        if (admin) {
            for (String id : List.of(MainLayout.ID_USER_MENU_AVAILABILITY, MainLayout.ID_USER_MENU_LOCATION,
                    MainLayout.ID_USER_MENU_OFF_DAYS, MainLayout.ID_USER_MENU_WORK_WEEK,
                    MainLayout.ID_USER_MENU_VIEW_PROFILE, MainLayout.ID_USER_MENU_ABOUT, MainLayout.ID_USER_MENU_LOGOUT)) {
                seleniumHandler.takeHelpElementScreenshot(MainLayout.class, id);
            }
            for (String id : adminItems) {
                seleniumHandler.takeHelpElementScreenshot(MainLayout.class, id);
            }
        } else {
            seleniumHandler.pushImplicitWait(Duration.ZERO);
            try {
                assertTrue(seleniumHandler.getDriver().findElements(By.id(MainLayout.ID_TAB_BASE + "kassandra")).isEmpty(),
                        "User navigation must not contain the administrator assistant");
                for (String id : adminItems) {
                    assertTrue(seleniumHandler.getDriver().findElements(By.id(id)).isEmpty(), "User menu must not contain " + id);
                }
            } finally {
                seleniumHandler.popImplicitWait();
            }
        }
        seleniumHandler.findElement(By.id(MainLayout.ID_USER_MENU_AVAILABILITY)).sendKeys(Keys.ESCAPE);
        seleniumHandler.waitUntil(ExpectedConditions.invisibilityOfElementLocated(By.id(MainLayout.ID_USER_MENU_AVAILABILITY)));
        seleniumHandler.prepareHelpScreenshots(FeatureListView.FEATURE_LIST_PAGE_TITLE);
    }

    private void takeMainLayoutHelpScreenshots(TestInfo testInfo, String captureProduct, String captureVersion) throws Exception {
        String userEmail = "jennifer.holleman@kassandra.org";
        User   helpUser  = peg.userApi.getAll().stream().filter(user -> userEmail.equals(user.getEmail())).findFirst().orElse(null);
        if (helpUser == null) {
            helpUser = peg.addUser("Jennifer Holleman", userEmail, "USER", "de", "nw",
                    firstDay, peg.generateUserColor(peg.getUserIndex()), 1f, null);
        }
        assertEquals("USER", helpUser.getRoles(), "The help account must be a regular user");
        var provider = oidcProviderService.getProviders().stream()
                .filter(candidate -> "Test Keycloak".equals(candidate.getDisplayName())).findFirst().orElseThrow();
        oidcIdentityService.linkIdentity(helpUser.getId(), provider.getRegistrationId(), getKeycloakUserSubject(userEmail));
        Product product = peg.productApi.getAll().stream().filter(candidate -> captureProduct.equals(candidate.getName())).findFirst().orElseThrow();
        productAclApi.grantUserAccess(product.getId(), helpUser.getId());
        for (String audience : List.of("admin", "user")) {
            boolean admin      = "admin".equals(audience);
            String  loginEmail = admin ? "christopher.paul@kassandra.org" : userEmail;
            // A local logout can leave Keycloak SSO cookies; isolate both origins between accounts.
            seleniumHandler.destroy();
            aboutViewTester.login(loginEmail, "password", null,
                    testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
            try {
                assertTrue(seleniumHandler.findElement(By.id(MainLayout.ID_USER_MENU)).getDomProperty("textContent").contains(loginEmail),
                        "The browser must be signed in as " + loginEmail);
                productListViewTester.switchToProductListView();
                productListViewTester.selectProduct(captureProduct);
                versionListViewTester.selectVersion(captureVersion);
                for (ETheme theme : List.of(ETheme.light, ETheme.dark)) {
                    takeMainLayoutElementScreenshots(theme, audience);
                }
            } finally {
                aboutViewTester.logout();
            }
        }
    }

    private void takeOffDayCalendarElementScreenshots(Class<?> view, String titleId, ETheme theme) throws IOException {
        takeHelpElements(view, titleId, theme, OffDaysCalendarComponent.CALENDAR_PREV_YEAR_BTN,
                OffDaysCalendarComponent.CALENDAR_NEXT_YEAR_BTN, OffDaysCalendarComponent.LEGEND_ITEM_ID_PREFIX_VACATION,
                OffDaysCalendarComponent.LEGEND_ITEM_ID_PREFIX_SICK_LEAVE, OffDaysCalendarComponent.LEGEND_ITEM_ID_PREFIX_HOLIDAY,
                OffDaysCalendarComponent.LEGEND_ITEM_ID_PREFIX_BUSINESS_TRIP);
    }

    /**
     * Takes screenshots of OffDay create, edit and delete dialogs
     */
    private void takeOffDayDialogScreenshots(String folder) throws IOException {
        // Create availability dialog
        {
            seleniumHandler.click(OffDayListView.CREATE_OFFDAY_BUTTON);
            seleniumHandler.setDatePickerValue(OffDayDialog.OFFDAY_START_DATE_FIELD, firstDay);
            seleniumHandler.setDatePickerValue(OffDayDialog.OFFDAY_END_DATE_FIELD, lastDay);
            seleniumHandler.setComboBoxValue(OffDayDialog.OFFDAY_TYPE_FIELD, OffDayType.VACATION.name());
            seleniumHandler.waitForElementToBeClickable(OffDayDialog.CANCEL_BUTTON); // Wait for dialog
            takeDialogHelpElements(OffDayDialog.class, OffDayDialog.HELP_BUTTON_ID,
                    OffDayDialog.OFFDAY_START_DATE_FIELD, OffDayDialog.OFFDAY_END_DATE_FIELD, OffDayDialog.OFFDAY_TYPE_FIELD,
                    OffDayDialog.CONFIRM_BUTTON, OffDayDialog.CANCEL_BUTTON);
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

    private void takeOffDayElementScreenshots(ETheme theme) throws IOException {
        takeHelpListElements(OffDayListView.class, OffDayListView.OFFDAY_LIST_PAGE_TITLE, theme,
                OffDayListView.OFFDAY_GRID, OffDayListView.CREATE_OFFDAY_BUTTON,
                OffDayListView.OFFDAY_GLOBAL_FILTER, OffDayListView.OFFDAY_ROW_COUNTER,
                OffDayListView.OFFDAY_GRID_EDIT_BUTTON_PREFIX, OffDayListView.OFFDAY_GRID_DELETE_BUTTON_PREFIX);
        takeOffDayCalendarElementScreenshots(OffDayListView.class, OffDayListView.OFFDAY_LIST_PAGE_TITLE, theme);
        takePageHelpScreenshot(OffDayListView.class, OffDayListView.HELP_BUTTON_ID);
    }

    private void takeOidcProviderElementScreenshots(ETheme theme) throws IOException {
        takeHelpElements(OidcProviderManagementView.class, OidcProviderManagementView.PAGE_TITLE_ID, theme,
                OidcProviderManagementView.NAME_FIELD_ID, OidcProviderManagementView.ISSUER_FIELD_ID,
                OidcProviderManagementView.CLIENT_ID_FIELD_ID, OidcProviderManagementView.CLIENT_SECRET_FIELD_ID,
                OidcProviderManagementView.SCOPES_FIELD_ID, OidcProviderManagementView.ADD_BUTTON_ID);
        seleniumHandler.takeHelpGridScreenshot(OidcProviderManagementView.class, OidcProviderManagementView.GRID_ID);
        seleniumHandler.takeHelpRowActionScreenshot(OidcProviderManagementView.class, OidcProviderManagementView.STATE_BUTTON_PREFIX);
        seleniumHandler.takeHelpRowActionScreenshot(OidcProviderManagementView.class, OidcProviderManagementView.DELETE_BUTTON_PREFIX);
        seleniumHandler.prepareHelpScreenshots(OidcProviderManagementView.PAGE_TITLE_ID);
        takePageHelpScreenshot(OidcProviderManagementView.class, OidcProviderManagementView.HELP_BUTTON_ID);
    }

    private void takePageHelpScreenshot(Class<?> view, String helpButtonId) throws IOException {
        seleniumHandler.takeHelpElementScreenshot(view, helpButtonId);
    }

    /**
     * Takes screenshots of Product create, edit and delete dialogs
     */
    private void takeProductDialogScreenshots(String folder) throws IOException {
        // Create product dialog
        seleniumHandler.click(ProductListView.CREATE_PRODUCT_BUTTON);
        seleniumHandler.waitForElementToBeClickable(ProductDialog.CANCEL_BUTTON); // Wait for dialog
        takeDialogHelpElements(ProductDialog.class, ProductDialog.PRODUCT_HELP_BUTTON,
                ProductDialog.PRODUCT_ACL_USERS_FIELD, ProductDialog.PRODUCT_ACL_GROUPS_FIELD,
                ProductDialog.CONFIRM_BUTTON, ProductDialog.CANCEL_BUTTON);
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ProductDialog.PRODUCT_DIALOG), ProductDialog.PRODUCT_DIALOG, folder + "/product-create-dialog.png");
        productListViewTester.closeDialog(ProductDialog.CANCEL_BUTTON);


        // Edit product dialog - open action menu first, then edit
        seleniumHandler.click(ProductListView.PRODUCT_GRID_EDIT_BUTTON_PREFIX + productName);
        seleniumHandler.waitForElementToBeClickable(ProductDialog.CANCEL_BUTTON); // Wait for dialog
        takeDialogHelpElements(ProductDialog.class, ProductDialog.PRODUCT_HELP_BUTTON,
                ProductDialog.PRODUCT_NAME_FIELD, ProductDialog.GENERATE_IMAGE_BUTTON);
        takeImagePromptHelpScreenshots(ProductDialog.GENERATE_IMAGE_BUTTON, true);
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ProductDialog.PRODUCT_DIALOG), ProductDialog.PRODUCT_DIALOG, folder + "/product-edit-dialog.png");
        productListViewTester.closeDialog(ProductDialog.CANCEL_BUTTON);

        // Delete product dialog - open action menu first, then delete
        seleniumHandler.click(ProductListView.PRODUCT_GRID_DELETE_BUTTON_PREFIX + productName);
        seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, folder + "/product-delete-dialog.png");
        productListViewTester.closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);
    }

    private void takeProductElementScreenshots(ETheme theme) throws IOException {
        takeHelpListElements(ProductListView.class, ProductListView.PRODUCT_LIST_PAGE_TITLE, theme,
                ProductListView.PRODUCT_GRID, ProductListView.CREATE_PRODUCT_BUTTON, ProductListView.PRODUCT_GLOBAL_FILTER,
                ProductListView.PRODUCT_ROW_COUNTER, ProductListView.PRODUCT_GRID_EDIT_BUTTON_PREFIX,
                ProductListView.PRODUCT_GRID_DELETE_BUTTON_PREFIX, ProductListView.PRODUCT_AI_PANEL_BUTTON,
                ProductListView.PRODUCT_HELP_BUTTON, MainLayout.ID_ACTION_HISTORY_BUTTON);
        seleniumHandler.takeHelpRowActionScreenshot(ProductListView.class, ProductListView.PRODUCT_GRID_ACCESS_PREFIX);
        seleniumHandler.prepareHelpScreenshots(ProductListView.PRODUCT_LIST_PAGE_TITLE);
    }

    private void takeProductHistoryScreenshots(String folder) throws IOException {
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
            takeReplayHelpScreenshots("undo");
            takeDialogScreenshot(UndoHistoryConfirmationDialog.DIALOG_ID, folder + "/undo-history-confirmation-dialog.png");
            seleniumHandler.click(UndoHistoryConfirmationDialog.CONFIRM_BUTTON);
            seleniumHandler.waitForElementToBeClickable(ProductListView.PRODUCT_GRID_NAME_PREFIX + productName);

            seleniumHandler.click(MainLayout.ID_ACTION_HISTORY_BUTTON);
            seleniumHandler.click(UndoHistoryOperationDetails.SUMMARY_ID_PREFIX + operation.getId());
            seleniumHandler.waitForElementToBeClickable(UndoHistoryOperationDetails.ACTION_ID_PREFIX + operation.getId());
            seleniumHandler.takeScreenShot(folder + "/product-history-undone.png");
            seleniumHandler.click(UndoHistoryOperationDetails.ACTION_ID_PREFIX + operation.getId());
            seleniumHandler.waitForElementToBeClickable(UndoHistoryConfirmationDialog.CANCEL_BUTTON);
            takeReplayHelpScreenshots("redo");
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
        aboutViewTester.login("christopher.paul@kassandra.org", "password", null,
                testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
        // Allow for checkpoint loading and generation, then wait for browser-side image decoding.
        new WebDriverWait(seleniumHandler.getDriver(), Duration.ofMinutes(10))
                .withMessage("About View banner did not finish loading before screenshot capture")
                .until(ExpectedConditions.refreshed(driver -> driver.findElements(By.cssSelector("#" + AboutView.ABOUT_VIEW + " img"))
                        .stream()
                        .anyMatch(image -> image.isDisplayed()
                                && "true".equals(image.getDomProperty("complete"))
                                && Integer.parseInt(image.getDomProperty("naturalWidth")) > 0)));
        seleniumHandler.setHelpScreenshotTheme(eTheme);
        takeHelpElements(AboutView.class, AboutView.ABOUT_PAGE_TITLE, eTheme, AboutView.ABOUT_PAGE_TITLE, AboutView.LICENSE_LINK_ID, AboutView.GITHUB_LINK_ID, MainLayout.ID_THEME_TOGGLE, MainLayout.ID_USER_MENU);
        takePageHelpScreenshot(AboutView.class, AboutView.HELP_BUTTON_ID);
        seleniumHandler.takeScreenShot(folder + "/about-view.png");
        //-----------------
        //ProductListView
        //-----------------
        productListViewTester.switchToProductListView();
        takeProductElementScreenshots(eTheme);
        seleniumHandler.takeScreenShot(folder + "/product-list-view.png");
        takeProductDialogScreenshots(folder);
        takeProductHistoryScreenshots(folder);
        productListViewTester.selectProduct(productName);

        //-----------------
        //VersionListView
        //-----------------
        takeVersionElementScreenshots(eTheme);
        seleniumHandler.takeScreenShot(folder + "/version-list-view.png");
        takeVersionDialogScreenshots(folder);
        versionListViewTester.selectVersion(versionName);

        //-----------------
        //FeatureListView
        //-----------------
        takeFeatureElementScreenshots(eTheme);
        seleniumHandler.takeScreenShot(folder + "/feature-list-view.png");
        takeFeatureDialogScreenshots(folder);
        featureListViewTester.selectFeature(featureName);

        //-----------------
        //SprintListView
        //-----------------
        seleniumHandler.waitForChartRendered(RenderUtil.SPRINTS_OVERVIEW_CHART);
        takeSprintElementScreenshots(eTheme);
        seleniumHandler.takeScreenShot(folder + "/sprint-list-view.png");
        takeSprintDialogScreenshots(folder);
        sprintListViewTester.selectSprint(sprintName);


        //-----------------
        //QualityBoard
        //-----------------
        seleniumHandler.waitForChartRendered(RenderUtil.GANTT_BURNDOWN_CHART);
        takeHelpElements(QualityBoard.class, "page-header", eTheme,
                QualityBoard.SPRINT_SELECTOR_ID, QualityBoard.EXPORT_JSON_BUTTON_ID, QualityBoard.EXPORT_XML_BUTTON_ID,
                QualityBoard.EXPORT_XLSX_BUTTON_ID, QualityBoard.OPEN_GANTT_BURNDOWN_CHART_BUTTON_ID,
                QualityBoard.GANTT_BURNDOWN_CONTAINER_ID);
        takePageHelpScreenshot(QualityBoard.class, QualityBoard.HELP_BUTTON_ID);
        seleniumHandler.takeScreenShot(folder + "/quality-board-delayed.png");
        seleniumHandler.setComboBoxValue(QualityBoard.SPRINT_SELECTOR_ID, "Lisbon");
        seleniumHandler.waitForChartRendered(RenderUtil.GANTT_BURNDOWN_CHART);
        seleniumHandler.takeScreenShot(folder + "/quality-board-in-time.png");
        takeDetachedChartScreenshot(QualityBoard.OPEN_GANTT_BURNDOWN_CHART_BUTTON_ID, RenderUtil.GANTT_BURNDOWN_CHART,
                folder + "/quality-board-detached-chart.png");

        //-----------------
        //UserProfileView
        //-----------------
        takeUserProfileScreenshots(folder, eTheme);

        //-----------------
        //UserGroupListView
        //-----------------
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_MANAGE_USER_GROUPS);
        seleniumHandler.waitForElementToBeClickable(UserGroupListView.GROUP_LIST_PAGE_TITLE);
        takeUserGroupElementScreenshots(eTheme);
        seleniumHandler.takeScreenShot(folder + "/user-group-list-view.png");
        takeUserGroupDialogScreenshots(folder);


        //-----------------
        //Backlog
        //-----------------
        backlogTester.switchToBacklog();
        seleniumHandler.setComboBoxValue(Backlog.SPRINT_SELECTOR_ID, sprintName);
        seleniumHandler.waitForChartRendered(RenderUtil.GANTT_CHART);
        takeBacklogElementScreenshots(eTheme);
        seleniumHandler.takeScreenShot(folder + "/backlog.png");
        takeDetachedChartScreenshot(Backlog.OPEN_GANTT_CHART_BUTTON_ID, RenderUtil.GANTT_CHART, folder + "/backlog-detached-chart.png");
        takeDependencyDialogScreenshots(folder);

        //-----------------
        //ActiveSprints
        //-----------------
        activeSprintsTester.switchToActiveSprints();
        seleniumHandler.waitForChartRendered(RenderUtil.GANTT_BURNDOWN_CHART);
        takeActiveSprintsElementScreenshots(eTheme);
        seleniumHandler.takeScreenShot(folder + "/active-sprints.png");
        takeDetachedChartScreenshot(ActiveSprints.OPEN_GANTT_BURNDOWN_CHART_BUTTON_ID, RenderUtil.GANTT_BURNDOWN_CHART,
                folder + "/active-sprints-detached-chart.png");
        takeActiveSprintsDialogScreenshots(folder);

        //-----------------
        //UserListView
        //-----------------
        userListViewTester.switchToUserListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
        takeUserElementScreenshots(eTheme);
        seleniumHandler.takeScreenShot(folder + "/user-list-view.png");
        takeUserDialogScreenshots(folder);

        //-----------------
        //AvailabilityListView
        //-----------------
        availabilityListViewTester.switchToAvailabilityListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo), null);
        takeAvailabilityElementScreenshots(eTheme);
        seleniumHandler.takeScreenShot(folder + "/availability-list-view.png");
        takeAvailabilityDialogScreenshots(folder);

        //-----------------
        //LocationListView
        //-----------------
        locationListViewTester.switchToLocationListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo), null);
        takeLocationElementScreenshots(eTheme);
        seleniumHandler.takeScreenShot(folder + "/location-list-view.png");
        takeLocationDialogScreenshots(folder);

        //-----------------
        //OffdayListView
        //-----------------
        offDayListViewTester.switchToOffDayListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo), null);
        takeOffDayElementScreenshots(eTheme);
        seleniumHandler.takeScreenShot(folder + "/offday-list-view.png");
        takeOffDayDialogScreenshots(folder);

        //-----------------
        //UserWorkWeekListView
        //-----------------
        userWorkWeekListViewTester.switchToUserWorkWeekListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo), null);
        takeUserWorkWeekElementScreenshots(eTheme);
        seleniumHandler.takeScreenShot(folder + "/user-work-week-list-view.png");
        takeUserWorkWeekDialogScreenshots(folder);

        //-----------------
        //WorkWeekListView
        //-----------------
        workWeekListViewTester.switchToWorkWeekListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
        takeWorkWeekElementScreenshots(eTheme);
        seleniumHandler.takeScreenShot(folder + "/work-week-list-view.png");
        takeWorkWeekDialogScreenshots(folder);
        takeAdministrationScreenshots(folder, eTheme);
        takeKassandraElementScreenshots(eTheme);
        aboutViewTester.logout();
        takeAuthenticationScreenshots(folder, eTheme);
    }

    /**
     * Restores or generates the showcase fixture once, then captures screens, dialogs
     * and help controls in light and dark themes, including shared administrator/user navigation.
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
        prepareScreenshotFixture(randomCase, testInfo);
        seleniumHandler.startRecording(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
        takeScreenshot(testInfo, "../kassandra.wiki/light-screenshots/", ETheme.light);
        takeScreenshot(testInfo, "../kassandra.wiki/dark-screenshots/", ETheme.dark);
        takeMainLayoutHelpScreenshots(testInfo, productName, versionName);

        seleniumHandler.waitUntilBrowserClosed(5000);
    }

    /**
     * Refreshes Task Details and worklog help captures without recapturing unrelated pages.
     *
     * @param randomCase deterministic screenshot fixture
     * @param testInfo current test metadata
     * @throws Exception when fixture restoration or screenshot capture fails
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void takeTaskDialogHelpScreenshots(RandomCase randomCase, TestInfo testInfo) throws Exception {
        prepareScreenshotFixture(randomCase, testInfo);
        for (ETheme theme : List.of(ETheme.light, ETheme.dark)) {
            aboutViewTester.login("christopher.paul@kassandra.org", "password", null, null, null);
            seleniumHandler.setHelpScreenshotTheme(theme);
            takeActiveSprintsDialogScreenshots("../kassandra.wiki/" + theme.name() + "-screenshots/");
            aboutViewTester.logout();
        }
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

    private void takeServerSettingsElementScreenshots(ETheme theme, String category, List<ServerSetting> settings) throws IOException {
        takeHelpElements(ServerSettingsView.class, ServerSettingsView.PAGE_TITLE_ID, theme,
                ServerSettingsView.CATEGORY_BUTTON_PREFIX + category, ServerSettingsView.CATEGORY_TITLE_PREFIX + category);
        ServerSetting definition = settings.stream().filter(setting -> category.equals(setting.getCategoryKey())).findFirst().orElseThrow();
        if (definition.isCategoryAllowDisable()) {
            seleniumHandler.takeHelpElementScreenshot(ServerSettingsView.class, ServerSettingsView.CATEGORY_ENABLED_PREFIX + category);
        }
        for (ServerSetting setting : settings.stream().filter(candidate -> category.equals(candidate.getCategoryKey())).toList()) {
            seleniumHandler.takeHelpElementScreenshot(ServerSettingsView.class, ServerSettingsView.SETTING_FIELD_PREFIX + setting.getKey());
            seleniumHandler.takeHelpElementScreenshot(ServerSettingsView.class, ServerSettingsView.SAVE_BUTTON_PREFIX + setting.getKey());
            if (setting.isTestable()) {
                seleniumHandler.takeHelpElementScreenshot(ServerSettingsView.class, ServerSettingsView.TEST_BUTTON_PREFIX + setting.getKey());
            }
            if (setting.isSecret() && setting.isConfigured()) {
                seleniumHandler.takeHelpElementScreenshot(ServerSettingsView.class, ServerSettingsView.CLEAR_BUTTON_PREFIX + setting.getKey());
            }
        }
    }

    /**
     * Takes screenshots of Sprint create, edit and delete dialogs
     */
    private void takeSprintDialogScreenshots(String folder) throws IOException {
        // Create sprint dialog
        seleniumHandler.click(SprintListView.CREATE_SPRINT_BUTTON);
        seleniumHandler.waitForElementToBeClickable(SprintDialog.CANCEL_BUTTON); // Wait for dialog
        takeDialogHelpElements(SprintDialog.class, SprintDialog.HELP_BUTTON_ID,
                SprintDialog.CONFIRM_BUTTON, SprintDialog.CANCEL_BUTTON);
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(SprintDialog.SPRINT_DIALOG), SprintDialog.SPRINT_DIALOG, folder + "/sprint-create-dialog.png");
        sprintListViewTester.closeDialog(SprintDialog.CANCEL_BUTTON);

        // Edit sprint dialog - open action menu first, then edit
        seleniumHandler.click(SprintListView.SPRINT_GRID_EDIT_BUTTON_PREFIX + sprintName);
        seleniumHandler.waitForElementToBeClickable(SprintDialog.CANCEL_BUTTON); // Wait for dialog
        takeDialogHelpElements(SprintDialog.class, SprintDialog.HELP_BUTTON_ID,
                SprintDialog.SPRINT_NAME_FIELD, SprintDialog.GENERATE_IMAGE_BUTTON);
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(SprintDialog.SPRINT_DIALOG), SprintDialog.SPRINT_DIALOG, folder + "/sprint-edit-dialog.png");
        sprintListViewTester.closeDialog(SprintDialog.CANCEL_BUTTON);

        // Delete sprint dialog - open action menu first, then delete
        seleniumHandler.click(SprintListView.SPRINT_GRID_DELETE_BUTTON_PREFIX + sprintName);
        seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, folder + "/sprint-delete-dialog.png");
        sprintListViewTester.closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);
    }

    private void takeSprintElementScreenshots(ETheme theme) throws IOException {
        takeHelpListElements(SprintListView.class, SprintListView.SPRINT_LIST_PAGE_TITLE, theme,
                SprintListView.SPRINT_GRID, SprintListView.CREATE_SPRINT_BUTTON, SprintListView.SPRINT_GLOBAL_FILTER,
                SprintListView.SPRINT_ROW_COUNTER, SprintListView.SPRINT_GRID_EDIT_BUTTON_PREFIX,
                SprintListView.SPRINT_GRID_DELETE_BUTTON_PREFIX, SprintListView.FEATURE_SELECTOR,
                SprintListView.SPRINT_AI_PANEL_BUTTON, MainLayout.ID_ACTION_HISTORY_BUTTON);
        seleniumHandler.takeHelpRowActionScreenshot(SprintListView.class, SprintListView.SPRINT_GRID_CONFIG_BUTTON_PREFIX);
        seleniumHandler.takeHelpElementScreenshot(SprintListView.class, SprintListView.OVERVIEW_CONTAINER_ID);
        seleniumHandler.prepareHelpScreenshots(SprintListView.SPRINT_LIST_PAGE_TITLE);
        takePageHelpScreenshot(SprintListView.class, SprintListView.HELP_BUTTON_ID);
    }

    /**
     * Takes screenshots of User create, edit and delete dialogs
     */
    private void takeUserDialogScreenshots(String folder) throws IOException {
        // Create user dialog
        seleniumHandler.click(UserListView.CREATE_USER_BUTTON);
        seleniumHandler.waitForElementToBeClickable(UserDialog.CANCEL_BUTTON); // Wait for dialog
        takeDialogHelpElements(UserDialog.class, UserDialog.HELP_BUTTON_ID,
                UserDialog.USER_EMAIL_FIELD, UserDialog.ROLES_FIELD_ID,
                UserDialog.USER_FIRST_WORKING_DAY_PICKER, UserDialog.USER_LAST_WORKING_DAY_PICKER,
                UserDialog.USER_LOCATION_COUNTRY_COMBO, UserDialog.USER_LOCATION_STATE_COMBO, UserDialog.USER_WORK_WEEK_COMBO,
                UserDialog.CONFIRM_BUTTON, UserDialog.CANCEL_BUTTON);
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(UserDialog.USER_DIALOG), UserDialog.USER_DIALOG, folder + "/user-create-dialog.png");
        userListViewTester.closeDialog(UserDialog.CANCEL_BUTTON);


        // Edit user dialog - open action menu first, then edit
        // Get first user in the list
        seleniumHandler.wait(300);
        seleniumHandler.click(UserListView.USER_GRID_EDIT_BUTTON_PREFIX + userName);
        seleniumHandler.waitForElementToBeClickable(UserDialog.CANCEL_BUTTON); // Wait for dialog
        takeDialogHelpElements(UserDialog.class, UserDialog.HELP_BUTTON_ID,
                UserDialog.USER_NAME_FIELD, UserDialog.GENERATE_IMAGE_BUTTON);
        takeImagePromptHelpScreenshots(UserDialog.GENERATE_IMAGE_BUTTON, false);
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(UserDialog.USER_DIALOG), UserDialog.USER_DIALOG, folder + "/user-edit-dialog.png");
        userListViewTester.closeDialog(UserDialog.CANCEL_BUTTON);

        // Delete user dialog - open action menu first, then delete
        seleniumHandler.wait(300);
        seleniumHandler.click(UserListView.USER_GRID_DELETE_BUTTON_PREFIX + userName);
        seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, folder + "/user-delete-dialog.png");
        userListViewTester.closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);
    }

    private void takeUserElementScreenshots(ETheme theme) throws IOException {
        takeHelpListElements(UserListView.class, UserListView.USER_LIST_PAGE_TITLE, theme,
                UserListView.USER_GRID, UserListView.CREATE_USER_BUTTON, UserListView.USER_GLOBAL_FILTER,
                UserListView.USER_ROW_COUNTER, UserListView.USER_GRID_EDIT_BUTTON_PREFIX,
                UserListView.USER_GRID_DELETE_BUTTON_PREFIX, UserListView.USER_AI_PANEL_BUTTON);
        takePageHelpScreenshot(UserListView.class, UserListView.HELP_BUTTON_ID);
    }

    /**
     * Takes screenshots of the {@link UserGroupListView} and the UserGroup create, edit, and delete dialogs.
     * <p>
     * The edit screenshot uses the always-present "Team" group. The delete screenshot opens the confirm
     * dialog on the always-present "All" group and then cancels, leaving the data unchanged.
     *
     * @param folder destination folder for the screenshot files
     */
    private void takeUserGroupDialogScreenshots(String folder) throws IOException {
        // Create dialog
        {
            seleniumHandler.click(UserGroupListView.CREATE_GROUP_BUTTON);
            seleniumHandler.waitForElementToBeClickable(UserGroupDialog.CANCEL_BUTTON);
            takeDialogHelpElements(UserGroupDialog.class, UserGroupDialog.HELP_BUTTON_ID,
                    UserGroupDialog.GROUP_NAME_FIELD, UserGroupDialog.GROUP_DESCRIPTION_FIELD, UserGroupDialog.GROUP_MEMBERS_FIELD,
                    UserGroupDialog.CONFIRM_BUTTON, UserGroupDialog.CANCEL_BUTTON);
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

    private void takeUserGroupElementScreenshots(ETheme theme) throws IOException {
        takeHelpListElements(UserGroupListView.class, UserGroupListView.GROUP_LIST_PAGE_TITLE, theme,
                UserGroupListView.GROUP_GRID, UserGroupListView.CREATE_GROUP_BUTTON, UserGroupListView.GROUP_GLOBAL_FILTER,
                UserGroupListView.GROUP_ROW_COUNTER, UserGroupListView.GROUP_GRID_EDIT_BUTTON_PREFIX,
                UserGroupListView.GROUP_GRID_DELETE_BUTTON_PREFIX);
        takePageHelpScreenshot(UserGroupListView.class, UserGroupListView.HELP_BUTTON_ID);
    }

    /**
     * Navigates to the user profile and captures it without invoking AI image generation.
     *
     * @param folder destination folder for the screenshot files
     */
    private void takeUserProfileScreenshots(String folder, ETheme theme) throws IOException {
        // Navigate to the profile page via the user menu
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_VIEW_PROFILE);
        seleniumHandler.waitForElementToBeClickable(UserProfileView.PROFILE_PAGE_TITLE);
        takeHelpElements(UserProfileView.class, UserProfileView.PROFILE_PAGE_TITLE, theme,
                UserProfileView.USER_NAME_FIELD, UserProfileView.USER_EMAIL_FIELD, UserProfileView.USER_COLOR_PICKER,
                UserProfileView.SAVE_PROFILE_BUTTON);
        if (seleniumHandler.getDriver().findElements(By.id(UserProfileView.GENERATE_AVATAR_BUTTON)).stream().anyMatch(WebElement::isDisplayed)) {
            seleniumHandler.takeHelpElementScreenshot(UserProfileView.class, UserProfileView.GENERATE_AVATAR_BUTTON);
        }
        takePageHelpScreenshot(UserProfileView.class, UserProfileView.HELP_BUTTON_ID);
        seleniumHandler.takeScreenShot(folder + "/user-profile-view.png");

    }

    /**
     * Takes screenshots of UserWorkWeek create, edit and delete dialogs.
     */
    private void takeUserWorkWeekDialogScreenshots(String folder) throws IOException {
        // Create dialog
        {
            seleniumHandler.click(UserWorkWeekListView.CREATE_BUTTON);
            seleniumHandler.waitForElementToBeClickable(UserWorkWeekDialog.CANCEL_BUTTON);
            takeDialogHelpElements(UserWorkWeekDialog.class, UserWorkWeekDialog.HELP_BUTTON_ID,
                    UserWorkWeekDialog.WORK_WEEK_COMBO_FIELD, UserWorkWeekDialog.START_DATE_FIELD,
                    UserWorkWeekDialog.CONFIRM_BUTTON, UserWorkWeekDialog.CANCEL_BUTTON);
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

    private void takeUserWorkWeekElementScreenshots(ETheme theme) throws IOException {
        takeHelpListElements(UserWorkWeekListView.class, UserWorkWeekListView.PAGE_TITLE, theme,
                UserWorkWeekListView.GRID, UserWorkWeekListView.CREATE_BUTTON, UserWorkWeekListView.GLOBAL_FILTER,
                UserWorkWeekListView.ROW_COUNTER, UserWorkWeekListView.GRID_EDIT_BUTTON_PREFIX,
                UserWorkWeekListView.GRID_DELETE_BUTTON_PREFIX);
        takeOffDayCalendarElementScreenshots(UserWorkWeekListView.class, UserWorkWeekListView.PAGE_TITLE, theme);
        takePageHelpScreenshot(UserWorkWeekListView.class, UserWorkWeekListView.HELP_BUTTON_ID);
    }

    /**
     * Takes screenshots of Version create, edit and delete dialogs.
     */
    private void takeVersionDialogScreenshots(String folder) throws IOException {
        // Create version dialog
        seleniumHandler.click(VersionListView.CREATE_VERSION_BUTTON);
        seleniumHandler.waitForElementToBeClickable(VersionDialog.CANCEL_BUTTON); // Wait for dialog
        takeDialogHelpElements(VersionDialog.class, VersionDialog.HELP_BUTTON_ID, VersionDialog.VERSION_NAME_FIELD,
                VersionDialog.CONFIRM_BUTTON, VersionDialog.CANCEL_BUTTON);
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

    private void takeVersionElementScreenshots(ETheme theme) throws IOException {
        takeHelpListElements(VersionListView.class, VersionListView.VERSION_LIST_PAGE_TITLE, theme,
                VersionListView.VERSION_GRID, VersionListView.CREATE_VERSION_BUTTON, VersionListView.VERSION_GLOBAL_FILTER,
                VersionListView.VERSION_ROW_COUNTER, VersionListView.VERSION_GRID_EDIT_BUTTON_PREFIX,
                VersionListView.VERSION_GRID_DELETE_BUTTON_PREFIX, VersionListView.PRODUCT_SELECTOR,
                VersionListView.VERSION_AI_PANEL_BUTTON, MainLayout.ID_ACTION_HISTORY_BUTTON);
        takePageHelpScreenshot(VersionListView.class, VersionListView.HELP_BUTTON_ID);
    }

    /**
     * Takes screenshots of WorkWeek create, edit and delete dialogs.
     */
    private void takeWorkWeekDialogScreenshots(String folder) throws IOException {
        // Create dialog
        {
            seleniumHandler.click(WorkWeekListView.CREATE_BUTTON);
            seleniumHandler.waitForElementToBeClickable(WorkWeekDialog.CANCEL_BUTTON);
            takeDialogHelpElements(WorkWeekDialog.class, WorkWeekDialog.HELP_BUTTON_ID,
                    WorkWeekDialog.NAME_FIELD, WorkWeekDialog.DESCRIPTION_FIELD,
                    WorkWeekDialog.DAY_ROW_ID_PREFIX + "monday", WorkWeekDialog.CONFIRM_BUTTON, WorkWeekDialog.CANCEL_BUTTON);
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

    private void takeWorkWeekElementScreenshots(ETheme theme) throws IOException {
        takeHelpListElements(WorkWeekListView.class, WorkWeekListView.PAGE_TITLE, theme,
                WorkWeekListView.GRID, WorkWeekListView.CREATE_BUTTON, WorkWeekListView.GLOBAL_FILTER,
                WorkWeekListView.ROW_COUNTER, WorkWeekListView.GRID_EDIT_BUTTON_PREFIX, WorkWeekListView.GRID_DELETE_BUTTON_PREFIX);
        takePageHelpScreenshot(WorkWeekListView.class, WorkWeekListView.HELP_BUTTON_ID);
    }

}
