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
import de.bushnaq.abdalla.kassandra.dto.OffDayType;
import de.bushnaq.abdalla.kassandra.ui.component.TaskGrid;
import de.bushnaq.abdalla.kassandra.ui.dialog.*;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
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
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=${test.server.port:0}",
                "spring.profiles.active=test",
                // Disable basic authentication for these tests
                "spring.security.basic.enabled=false"
        }
)
@AutoConfigureMockMvc
//@Transactional
@Testcontainers
@Slf4j
public class GenerateScreenshots extends AbstractKeycloakUiTestUtil {
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
    private final LocalDate                  firstDayRecord1 = LocalDate.of(2025, 8, 4);
    private final LocalDate                  lastDay         = LocalDate.of(2025, 6, 1);
    private final LocalDate                  lastDayRecord1  = LocalDate.of(2025, 8, 8);
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
    private final OffDayType                 typeRecord1     = OffDayType.VACATION;
    @Autowired
    private       UserListViewTester         userListViewTester;
    private       String                     userName;
    @Autowired
    private       VersionListViewTester      versionListViewTester;
    private       String                     versionName;

    // Method to get the public-facing URL, fixing potential redirect issues
    private static String getPublicFacingUrl(KeycloakContainer container) {
        return String.format("http://%s:%s",
                container.getHost(),
                container.getMappedPort(8080));
    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(
                        1,
                        OffsetDateTime.parse("2025-08-11T08:00:00+01:00"),
                        LocalDate.parse("2025-08-04"),
                        Duration.ofDays(10),
                        4, 4,
                        1, 3,
                        1, 3,
                        1, 4,
                        6, 8, 8, 6, 13)//
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

    /**
     * Takes screenshots of Availability create, edit and delete dialogs
     */
    private void takeAvailabilityDialogScreenshots() {
        // Create availability dialog
        {
            seleniumHandler.click(AvailabilityListView.CREATE_AVAILABILITY_BUTTON);
            seleniumHandler.waitForElementToBeClickable(AvailabilityDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(AvailabilityDialog.AVAILABILITY_DIALOG), AvailabilityDialog.AVAILABILITY_DIALOG, "../kassandra.wiki/screenshots/availability-create-dialog.png");
            availabilityListViewTester.closeDialog(AvailabilityDialog.CANCEL_BUTTON);
        }

        // Edit availability dialog
        // We'll use the current date as a reference to find a record to edit
        {
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            seleniumHandler.click(AvailabilityListView.AVAILABILITY_GRID_EDIT_BUTTON_PREFIX + dateStr);
            seleniumHandler.waitForElementToBeClickable(AvailabilityDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(AvailabilityDialog.AVAILABILITY_DIALOG), AvailabilityDialog.AVAILABILITY_DIALOG, "../kassandra.wiki/screenshots/availability-edit-dialog.png");
            availabilityListViewTester.closeDialog(AvailabilityDialog.CANCEL_BUTTON);
        }

        // Delete availability dialog
        {
            // create something we can at least try to delete
            availabilityListViewTester.createAvailabilityConfirm(firstDay, 50);
            String dateStr = firstDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            seleniumHandler.click(AvailabilityListView.AVAILABILITY_GRID_DELETE_BUTTON_PREFIX + dateStr);
            seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, "../kassandra.wiki/screenshots/availability-delete-dialog.png");
            availabilityListViewTester.closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);
        }
    }

    /**
     * Takes screenshots of Project create, edit and delete dialogs
     */
    private void takeFeatureDialogScreenshots() {
        // Create project dialog
        seleniumHandler.click(FeatureListView.CREATE_FEATURE_BUTTON_ID);
        seleniumHandler.waitForElementToBeClickable(FeatureDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(FeatureDialog.FEATURE_DIALOG), FeatureDialog.FEATURE_DIALOG, "../kassandra.wiki/screenshots/feature-create-dialog.png");
        featureListViewTester.closeDialog(FeatureDialog.CANCEL_BUTTON);

        // Edit project dialog - open action menu first, then edit
        seleniumHandler.click(FeatureListView.FEATURE_GRID_EDIT_BUTTON_PREFIX + featureName);
        seleniumHandler.waitForElementToBeClickable(FeatureDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(FeatureDialog.FEATURE_DIALOG), FeatureDialog.FEATURE_DIALOG, "../kassandra.wiki/screenshots/feature-edit-dialog.png");
        featureListViewTester.closeDialog(FeatureDialog.CANCEL_BUTTON);

        // Delete project dialog - open action menu first, then delete
        seleniumHandler.click(FeatureListView.FEATURE_GRID_DELETE_BUTTON_PREFIX + featureName);
        seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, "../kassandra.wiki/screenshots/feature-delete-dialog.png");
        featureListViewTester.closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);
    }

    /**
     * Takes screenshots of Location create, edit and delete dialogs
     */
    private void takeLocationDialogScreenshots() {
        // Create location dialog
        {
            seleniumHandler.click(LocationListView.CREATE_LOCATION_BUTTON);
            seleniumHandler.waitForElementToBeClickable(LocationDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(LocationDialog.LOCATION_DIALOG), LocationDialog.LOCATION_DIALOG, "../kassandra.wiki/screenshots/location-create-dialog.png");
            locationListViewTester.closeDialog(LocationDialog.CANCEL_BUTTON);
        }

        // Edit location dialog
        // We'll use the current date as a reference to find a record to edit
        {
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            seleniumHandler.click(LocationListView.LOCATION_GRID_EDIT_BUTTON_PREFIX + dateStr);
            seleniumHandler.waitForElementToBeClickable(LocationDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(LocationDialog.LOCATION_DIALOG), LocationDialog.LOCATION_DIALOG, "../kassandra.wiki/screenshots/location-edit-dialog.png");
            locationListViewTester.closeDialog(LocationDialog.CANCEL_BUTTON);
        }

        // Delete location dialog
        {
            locationListViewTester.createLocationConfirm(firstDay, "United States (US)", "California (ca)");
            String dateStr = firstDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            seleniumHandler.click(LocationListView.LOCATION_GRID_DELETE_BUTTON_PREFIX + dateStr);
            seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, "../kassandra.wiki/screenshots/location-delete-dialog.png");
            locationListViewTester.closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);
        }
    }

    /**
     * Takes screenshots of OffDay create, edit and delete dialogs
     */
    private void takeOffDayDialogScreenshots() {
        // Create availability dialog
        {
            seleniumHandler.click(OffDayListView.CREATE_OFFDAY_BUTTON);
            seleniumHandler.setDatePickerValue(OffDayDialog.OFFDAY_START_DATE_FIELD, firstDay);
            seleniumHandler.setDatePickerValue(OffDayDialog.OFFDAY_END_DATE_FIELD, lastDay);
            seleniumHandler.setComboBoxValue(OffDayDialog.OFFDAY_TYPE_FIELD, OffDayType.VACATION.name());
            seleniumHandler.waitForElementToBeClickable(OffDayDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(OffDayDialog.OFFDAY_DIALOG), OffDayDialog.OFFDAY_DIALOG, "../kassandra.wiki/screenshots/offday-create-dialog.png");
            offDayListViewTester.closeDialog(OffDayDialog.CANCEL_BUTTON);
        }

        // Edit availability dialog
        {
            // Create an initial record
            offDayListViewTester.createOffDayConfirm(firstDayRecord1, lastDayRecord1, typeRecord1);

            offDayListViewTester.clickEditButtonForRecord(firstDayRecord1);
            seleniumHandler.waitForElementToBeClickable(OffDayDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(OffDayDialog.OFFDAY_DIALOG), OffDayDialog.OFFDAY_DIALOG, "../kassandra.wiki/screenshots/offday-edit-dialog.png");
            offDayListViewTester.closeDialog(OffDayDialog.CANCEL_BUTTON);
        }

        // Delete availability dialog
        {
            offDayListViewTester.clickDeleteButtonForRecord(firstDayRecord1);
            seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, "../kassandra.wiki/screenshots/offday-delete-dialog.png");
            offDayListViewTester.closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);
        }
    }

    /**
     * Takes screenshots of Product create, edit and delete dialogs
     */
    private void takeProductDialogScreenshots() {
        // Create product dialog
        seleniumHandler.click(ProductListView.CREATE_PRODUCT_BUTTON);
        seleniumHandler.waitForElementToBeClickable(ProductDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ProductDialog.PRODUCT_DIALOG), ProductDialog.PRODUCT_DIALOG, "../kassandra.wiki/screenshots/product-create-dialog.png");
        productListViewTester.closeDialog(ProductDialog.CANCEL_BUTTON);


        // Edit product dialog - open action menu first, then edit
        seleniumHandler.click(ProductListView.PRODUCT_GRID_EDIT_BUTTON_PREFIX + productName);
        seleniumHandler.waitForElementToBeClickable(ProductDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ProductDialog.PRODUCT_DIALOG), ProductDialog.PRODUCT_DIALOG, "../kassandra.wiki/screenshots/product-edit-dialog.png");
        productListViewTester.closeDialog(ProductDialog.CANCEL_BUTTON);

        // Delete product dialog - open action menu first, then delete
        seleniumHandler.click(ProductListView.PRODUCT_GRID_DELETE_BUTTON_PREFIX + productName);
        seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, "../kassandra.wiki/screenshots/product-delete-dialog.png");
        productListViewTester.closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void takeScreenshots(RandomCase randomCase, TestInfo testInfo) throws Exception {
        // Set browser window to a fixed size for consistent screenshots
//        seleniumHandler.setWindowSize(1800, 1300);
        seleniumHandler.setWindowSize(1700, 1200);

//        printAuthentication();
        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateProductsIfNeeded(testInfo, randomCase);
        seleniumHandler.startRecording(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
        userName    = "Christopher Paul";
        productName = nameGenerator.generateProductName(0);
        versionName = nameGenerator.generateVersionName(0);
        featureName = nameGenerator.generateFeatureName(0);
        sprintName  = nameGenerator.generateSprintName(0);
//        taskName    = nameGenerator.generateSprintName(0);


        //ProductListView
        productListViewTester.switchToProductListViewWithOidc("christopher.paul@kassandra.org", "password", "../kassandra.wiki/screenshots/login-view.png", testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
        seleniumHandler.takeScreenShot("../kassandra.wiki/screenshots/product-list-view.png");
        takeProductDialogScreenshots();
        productListViewTester.selectProduct(productName);
        //VersionListView
        seleniumHandler.takeScreenShot("../kassandra.wiki/screenshots/version-list-view.png");
        takeVersionDialogScreenshots();
        versionListViewTester.selectVersion(versionName);
        //FeatureListView
        seleniumHandler.takeScreenShot("../kassandra.wiki/screenshots/feature-list-view.png");
        takeFeatureDialogScreenshots();
        featureListViewTester.selectFeature(featureName);
        //SprintListView
        seleniumHandler.takeScreenShot("../kassandra.wiki/screenshots/sprint-list-view.png");
        takeSprintDialogScreenshots();
        sprintListViewTester.selectSprint(sprintName);
        seleniumHandler.waitForElementToBeClickable(RenderUtil.GANTT_CHART);
        seleniumHandler.waitForElementToBeClickable(RenderUtil.BURNDOWN_CHART);
        seleniumHandler.takeScreenShot("../kassandra.wiki/screenshots/sprint-quality-board.png");


        // After visiting the SprintQualityBoard, go back to SprintListView and use the column config button
        seleniumHandler.click("Sprints (" + sprintName + ")"); // Go back to SprintListView using breadcrumb
        // Find and click the column configuration button
        seleniumHandler.click(SprintListView.SPRINT_GRID_CONFIG_BUTTON_PREFIX + sprintName);
        seleniumHandler.waitForElementToBeClickable(TaskListView.TASK_LIST_PAGE_TITLE_ID);
        seleniumHandler.takeScreenShot("../kassandra.wiki/screenshots/task-list-view.png");

        //Backlog
        backlogTester.switchToBacklog();
        seleniumHandler.takeScreenShot("../kassandra.wiki/screenshots/backlog.png");

        //ActiveSprints
        activeSprintsTester.switchToActiveSprints();
        seleniumHandler.takeScreenShot("../kassandra.wiki/screenshots/active-sprints.png");

        userListViewTester.switchToUserListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
        seleniumHandler.takeScreenShot("../kassandra.wiki/screenshots/user-list-view.png");
        takeUserDialogScreenshots();

        // Navigate to AvailabilityListView for the current user and take screenshots
        availabilityListViewTester.switchToAvailabilityListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo), null);
        seleniumHandler.takeScreenShot("../kassandra.wiki/screenshots/availability-list-view.png");
        takeAvailabilityDialogScreenshots();

        // Navigate to LocationListView for the current user and take screenshots
        locationListViewTester.switchToLocationListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo), null);
        seleniumHandler.takeScreenShot("../kassandra.wiki/screenshots/location-list-view.png");
        takeLocationDialogScreenshots();

        // Navigate to OffDayListView for the current user and take screenshots
        offDayListViewTester.switchToOffDayListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo), null);
        seleniumHandler.takeScreenShot("../kassandra.wiki/screenshots/offday-list-view.png");
        takeOffDayDialogScreenshots();

        seleniumHandler.waitUntilBrowserClosed(5000);
    }

    /**
     * Takes screenshots of Sprint create, edit and delete dialogs
     */
    private void takeSprintDialogScreenshots() {
        // Create sprint dialog
        seleniumHandler.click(SprintListView.CREATE_SPRINT_BUTTON);
        seleniumHandler.waitForElementToBeClickable(SprintDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(SprintDialog.SPRINT_DIALOG), SprintDialog.SPRINT_DIALOG, "../kassandra.wiki/screenshots/sprint-create-dialog.png");
        sprintListViewTester.closeDialog(SprintDialog.CANCEL_BUTTON);

        // Edit sprint dialog - open action menu first, then edit
        seleniumHandler.click(SprintListView.SPRINT_GRID_EDIT_BUTTON_PREFIX + sprintName);
        seleniumHandler.waitForElementToBeClickable(SprintDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(SprintDialog.SPRINT_DIALOG), SprintDialog.SPRINT_DIALOG, "../kassandra.wiki/screenshots/sprint-edit-dialog.png");
        sprintListViewTester.closeDialog(SprintDialog.CANCEL_BUTTON);

        // Delete sprint dialog - open action menu first, then delete
        seleniumHandler.click(SprintListView.SPRINT_GRID_DELETE_BUTTON_PREFIX + sprintName);
        seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, "../kassandra.wiki/screenshots/sprint-delete-dialog.png");
        sprintListViewTester.closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);
    }

    private void takeTaskDialogScreenshots() {
        seleniumHandler.click(TaskListView.CREATE_MILESTONE_BUTTON_ID);
        seleniumHandler.ensureIsInList(ProductListView.PRODUCT_GRID_NAME_PREFIX, "New Milestone-34");
        seleniumHandler.click(TaskListView.CREATE_STORY_BUTTON_ID);
        seleniumHandler.ensureIsInList(ProductListView.PRODUCT_GRID_NAME_PREFIX, "New Story-2");
        seleniumHandler.click(TaskListView.CREATE_TASK_BUTTON_ID);
        seleniumHandler.ensureIsInList(ProductListView.PRODUCT_GRID_NAME_PREFIX, "New Task-3");
        // select the milestone
        seleniumHandler.click(TaskGrid.TASK_GRID_NAME_PREFIX + "New Milestone-34");
        // select start cell
        seleniumHandler.click(TaskGrid.TASK_GRID_START_PREFIX + "New Milestone-34");
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, "../kassandra.wiki/screenshots/task-view-list.png");
    }

    /**
     * Takes screenshots of User create, edit and delete dialogs
     */
    private void takeUserDialogScreenshots() {
        // Create user dialog
        seleniumHandler.click(UserListView.CREATE_USER_BUTTON);
        seleniumHandler.waitForElementToBeClickable(UserDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(UserDialog.USER_DIALOG), UserDialog.USER_DIALOG, "../kassandra.wiki/screenshots/user-create-dialog.png");
        userListViewTester.closeDialog(UserDialog.CANCEL_BUTTON);


        // Edit user dialog - open action menu first, then edit
        // Get first user in the list
        seleniumHandler.wait(300);
        seleniumHandler.click(UserListView.USER_GRID_EDIT_BUTTON_PREFIX + userName);
        seleniumHandler.waitForElementToBeClickable(UserDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(UserDialog.USER_DIALOG), UserDialog.USER_DIALOG, "../kassandra.wiki/screenshots/user-edit-dialog.png");
        userListViewTester.closeDialog(UserDialog.CANCEL_BUTTON);

        // Delete user dialog - open action menu first, then delete
        seleniumHandler.wait(300);
        seleniumHandler.click(UserListView.USER_GRID_DELETE_BUTTON_PREFIX + userName);
        seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, "../kassandra.wiki/screenshots/user-delete-dialog.png");
        userListViewTester.closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);
    }

    /**
     * Takes screenshots of Version create, edit and delete dialogs
     */
    private void takeVersionDialogScreenshots() {
        // Create version dialog
        seleniumHandler.click(VersionListView.CREATE_VERSION_BUTTON);
        seleniumHandler.waitForElementToBeClickable(VersionDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(VersionDialog.VERSION_DIALOG), VersionDialog.VERSION_DIALOG, "../kassandra.wiki/screenshots/version-create-dialog.png");
        versionListViewTester.closeDialog(VersionDialog.CANCEL_BUTTON);

        // Edit version dialog - open action menu first, then edit

        seleniumHandler.click(VersionListView.VERSION_GRID_EDIT_BUTTON_PREFIX + versionName);
        seleniumHandler.waitForElementToBeClickable(VersionDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(VersionDialog.VERSION_DIALOG), VersionDialog.VERSION_DIALOG, "../kassandra.wiki/screenshots/version-edit-dialog.png");
        versionListViewTester.closeDialog(VersionDialog.CANCEL_BUTTON);

        // Delete version dialog - open action menu first, then delete
        seleniumHandler.click(VersionListView.VERSION_GRID_DELETE_BUTTON_PREFIX + versionName);
        seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, "../kassandra.wiki/screenshots/version-delete-dialog.png");
        versionListViewTester.closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);
    }
}
