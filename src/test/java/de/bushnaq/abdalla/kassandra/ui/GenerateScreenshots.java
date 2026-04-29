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
import de.bushnaq.abdalla.kassandra.config.DefaultEntitiesInitializer;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.report.dao.ETheme;
import de.bushnaq.abdalla.kassandra.ui.component.TaskCard;
import de.bushnaq.abdalla.kassandra.ui.component.TaskGrid;
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
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

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
public class GenerateScreenshots extends AbstractKeycloakUiTestUtil {
    @Autowired
    private       AboutViewTester            aboutViewTester;
    @Autowired
    private       ActiveSprintsTester        activeSprintsTester;
    @Autowired
    private       AvailabilityListViewTester availabilityListViewTester;
    @Autowired
    private       BacklogTester              backlogTester;
    @Autowired
    private       FeatureListViewTester      featureListViewTester;
    private       String                     featureName;
    private final LocalDate                  firstDay        = LocalDate.of(2025, 6, 1);
    private final LocalDate                  firstDayRecord1 = LocalDate.of(2026, 8, 4);
    Availability lastAvailability = null;
    private final LocalDate lastDay        = LocalDate.of(2025, 6, 1);
    private final LocalDate lastDayRecord1 = LocalDate.of(2026, 8, 7);
    Location     lastLocation     = null;
    UserWorkWeek lastUserWorkWeek = null;
    @Autowired
    private       LocationListViewTester     locationListViewTester;
    @Autowired
    private       OffDayListViewTester       offDayListViewTester;
    @Autowired
    private       ProductListViewTester      productListViewTester;
    private       String                     productName;
    @Autowired
    private       HumanizedSeleniumHandler   seleniumHandler;
    @Autowired
    private       SprintListViewTester       sprintListViewTester;
    private       String                     sprintName;
    @Autowired
    private       TaskListViewTester         taskListViewTester;
    private       String                     taskName;
    private final OffDayType                 typeRecord1 = OffDayType.VACATION;
    @Autowired
    private       UserGroupListViewTester    userGroupListViewTester;
    @Autowired
    private       UserListViewTester         userListViewTester;
    private       String                     userName;
    @Autowired
    private       UserProfileViewTester      userProfileViewTester;
    @Autowired
    private       UserWorkWeekListViewTester userWorkWeekListViewTester;
    @Autowired
    private       VersionListViewTester      versionListViewTester;
    private       String                     versionName;
    @Autowired
    private       WorkWeekListViewTester     workWeekListViewTester;

    // Method to get the public-facing URL, fixing potential redirect issues
    private static String getPublicFacingUrl(KeycloakContainer container) {
        return String.format("http://%s:%s",
                container.getHost(),
                container.getMappedPort(8080));
    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(3,
                        OffsetDateTime.parse("2026-02-02T08:00:00+01:00"),
                        LocalDate.parse("2025-08-04"),
                        Duration.ofDays(10),
                        2, 2,
                        2, 2,
                        2, 2,
                        1, 5,
                        5, 8, 8, 6, 7)//official demo data
//                new RandomCase(
//                        1,
//                        OffsetDateTime.parse("2026-02-02T08:00:00+01:00"),
//                        LocalDate.parse("2025-08-04"),
//                        Duration.ofDays(10),
//                        4, 4,
//                        1, 3,
//                        1, 3,
//                        1, 4,
//                        6, 8, 8, 6, 13)//
        };
        return Arrays.stream(randomCases).toList();
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
        User paul = userApi.getByEmail("christopher.paul@kassandra.org").get();
        lastAvailability = paul.getAvailabilities().getLast();
        lastLocation     = paul.getLocations().getLast();
        lastUserWorkWeek = paul.getUserWorkWeeks().getLast();
    }

    /**
     * Takes screenshots of {@link WorklogDialog} and {@link TaskDialog} from the Active Sprints view.
     * <p>
     * Relies on sprint "Zurich" being present in the generated test data (it is always created with
     * {@code Status.STARTED} thanks to {@code AbstractEntityGenerator.addSprint}).
     *
     * @param folder destination folder for the screenshot files
     */
    private void takeActiveSprintsDialogScreenshots(String folder) {
        List<Sprint> allSprints = sprintApi.getAll();
        Sprint sprint = allSprints.stream()
                .filter(s -> !s.getStatus().equals(Status.CLOSED))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find a sprint that is not closed"));
        List<Task> tasks = taskApi.getAll(sprint.getId());
        Task leafTask = tasks.stream()
                .filter(t -> !t.isMilestone() && t.getParentTaskId() != null && t.getTaskStatus().equals(TaskStatus.IN_PROGRESS))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No task found in sprint that has IN_PROGRESS status"));

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
        activeSprintsTester.closeDialog(TaskDialog.CANCEL_BUTTON);
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
        List<Sprint> allSprints = sprintApi.getAll();
        Sprint sprint = allSprints.stream()
                .filter(s -> s.getName().equals(sprintName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Sprint '" + sprintName + "' not found"));
        List<Task> tasks = taskApi.getAll(sprint.getId());
        Task leafTask = tasks.stream()
                .filter(t -> !t.isMilestone() && t.getParentTaskId() != null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No leaf task found in sprint '" + sprintName + "'"));

        // Switch to edit mode
        seleniumHandler.click(Backlog.EDIT_BUTTON_ID);
        seleniumHandler.waitForElementToBeClickable(Backlog.CANCEL_BUTTON_ID);

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
        seleniumHandler.waitForElementToBeClickable(RenderUtil.GANTT_CHART);
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

    private void takeScreenshot(TestInfo testInfo, String folder, ETheme eTheme) throws Exception {

        //-----------------
        //AboutView
        //-----------------
        aboutViewTester.login("christopher.paul@kassandra.org", "password", folder + "/login-view.png", testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
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
        takeFeatureDialogScreenshots(folder);
        featureListViewTester.selectFeature(featureName);

        //-----------------
        //SprintListView
        //-----------------
        seleniumHandler.waitForElementToBeClickable(RenderUtil.SPRINTS_OVERVIEW_CHART);
        seleniumHandler.takeScreenShot(folder + "/sprint-list-view.png");
        takeSprintDialogScreenshots(folder);
        sprintListViewTester.selectSprint(sprintName);
        seleniumHandler.waitForElementToBeClickable(RenderUtil.GANTT_BURNDOWN_CHART);
        seleniumHandler.takeScreenShot(folder + "/quality-board.png");

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
        seleniumHandler.waitForElementToBeClickable(RenderUtil.GANTT_CHART);
        seleniumHandler.takeScreenShot(folder + "/backlog.png");
        takeDependencyDialogScreenshots(folder);

        //-----------------
        //ActiveSprints
        //-----------------
        activeSprintsTester.switchToActiveSprints();
        seleniumHandler.wait(1000);
//        seleniumHandler.setMultiSelectComboBoxValue(ActiveSprints.ID_SPRINT_SELECTOR, new String[]{"Zurich"});//enable
//        seleniumHandler.setMultiSelectComboBoxValue(ActiveSprints.ID_SPRINT_SELECTOR, new String[]{"Oslo"});//enable
        seleniumHandler.wait(1000);
        seleniumHandler.takeScreenShot(folder + "/active-sprints.png");
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
        aboutViewTester.logout();
    }

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
        read();
        seleniumHandler.startRecording(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
        userName    = "Christopher Paul";
        productName = nameGenerator.generateProductName(0);
        versionName = nameGenerator.generateVersionName(0);
        featureName = nameGenerator.generateFeatureName(0);
        sprintName  = nameGenerator.generateSprintName(1);


        takeScreenshot(testInfo, "../kassandra.wiki/light-screenshots/", ETheme.light);
        takeScreenshot(testInfo, "../kassandra.wiki/dark-screenshots/", ETheme.dark);

        seleniumHandler.waitUntilBrowserClosed(5000);
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
     * Navigates to the {@link UserProfileView} via the user menu, takes a full-page screenshot,
     * and – when Stable Diffusion is available – also opens and screenshots the {@link ImagePromptDialog}.
     * <p>
     * The {@link ImagePromptDialog} requires the {@code GENERATE_AVATAR_BUTTON} to be present in the DOM,
     * which only happens when the Stable Diffusion API is reachable.  When SD is unavailable the dialog
     * screenshot is silently skipped.
     *
     * @param folder destination folder for the screenshot files
     */
    private void takeUserProfileScreenshots(String folder) {
        // Navigate to the profile page via the user menu
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_VIEW_PROFILE);
        seleniumHandler.waitForElementToBeClickable(UserProfileView.PROFILE_PAGE_TITLE);
        seleniumHandler.takeScreenShot(folder + "/user-profile-view.png");

        // ImagePromptDialog is only reachable when Stable Diffusion is running
        if (seleniumHandler.isElementPresent(UserProfileView.GENERATE_AVATAR_BUTTON)) {
            seleniumHandler.click(UserProfileView.GENERATE_AVATAR_BUTTON);
            seleniumHandler.waitForElementToBeClickable(ImagePromptDialog.ID_CANCEL_BUTTON);

//            seleniumHandler.click(ImagePromptDialog.ID_GENERATE_BUTTON);
//            seleniumHandler.pushWaitDuration(Duration.ofSeconds(120));
//            seleniumHandler.waitForElementToBeInteractable(ImagePromptDialog.ID_GENERATE_BUTTON);
//            seleniumHandler.popWaitDuration();

            seleniumHandler.takeElementScreenShot(
                    seleniumHandler.findDialogOverlayElement(ImagePromptDialog.IMAGE_PROMPT_DIALOG),
                    ImagePromptDialog.IMAGE_PROMPT_DIALOG,
                    folder + "/image-prompt-dialog.png");
            seleniumHandler.wait(200);
            seleniumHandler.click(ImagePromptDialog.ID_CANCEL_BUTTON);
            seleniumHandler.waitForElementToBeClickable(UserProfileView.PROFILE_PAGE_TITLE);
        } else {
            log.info("Skipping ImagePromptDialog screenshot: Stable Diffusion is not available");
        }
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
