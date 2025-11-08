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
import de.bushnaq.abdalla.kassandra.ui.dialog.*;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.*;
import de.bushnaq.abdalla.kassandra.ui.view.util.*;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=8080",
                "spring.profiles.active=test",
                // Disable basic authentication for these tests
                "spring.security.basic.enabled=false"
        }
)
@AutoConfigureMockMvc
@Transactional
@Testcontainers
public class GenerateScreenshots extends AbstractUiTestUtil {
    // Start Keycloak container with realm configuration
    @Container
    private static final KeycloakContainer          keycloak        = new KeycloakContainer("quay.io/keycloak/keycloak:24.0.1")
            .withRealmImportFile("keycloak/project-hub-realm.json")
            .withAdminUsername("admin")
            .withAdminPassword("admin")
            // Expose on a fixed port for more reliable testing
            .withExposedPorts(8080, 8443)
            // Add debugging to see container output
            .withLogConsumer(outputFrame -> System.out.println("Keycloak: " + outputFrame.getUtf8String()))
            // Make Keycloak accessible from outside the container
            .withEnv("KC_HOSTNAME_STRICT", "false")
            .withEnv("KC_HOSTNAME_STRICT_HTTPS", "false");
    @Autowired
    private              AvailabilityListViewTester availabilityListViewTester;
    @Autowired
    private              FeatureListViewTester      featureListViewTester;
    private              String                     featureName;
    private final        LocalDate                  firstDay        = LocalDate.of(2025, 6, 1);
    private final        LocalDate                  firstDayRecord1 = LocalDate.of(2025, 8, 1);
    private final        LocalDate                  lastDay         = LocalDate.of(2025, 6, 1);
    private final        LocalDate                  lastDayRecord1  = LocalDate.of(2025, 8, 5);
    @Autowired
    private              LocationListViewTester     locationListViewTester;
    @Autowired
    private              OffDayListViewTester       offDayListViewTester;
    @Autowired
    private              ProductListViewTester      productListViewTester;
    private              String                     productName;
    @Autowired
    private              HumanizedSeleniumHandler   seleniumHandler;
    @Autowired
    private              SprintListViewTester       sprintListViewTester;
    private              String                     sprintName;
    @Autowired
    private              TaskListViewTester         taskListViewTester;
    private              String                     taskName;
    private final        OffDayType                 typeRecord1     = OffDayType.VACATION;
    @Autowired
    private              UserListViewTester         userListViewTester;
    private              String                     userName;
    @Autowired
    private              VersionListViewTester      versionListViewTester;
    private              String                     versionName;

    // Method to get the public-facing URL, fixing potential redirect issues
    private static String getPublicFacingUrl(KeycloakContainer container) {
        return String.format("http://%s:%s",
                container.getHost(),
                container.getMappedPort(8080));
    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
//                new RandomCase(1, 5, 8, 8, 8, 1),//
                new RandomCase(1, LocalDate.parse("2024-12-01"), Duration.ofDays(10), 1, 1, 1, 1, 1, 6, 8, 8, 6, 13)//
//                new RandomCase(2, 10, 3, 2, 3, 1)//
//                new RandomCase(2, 4, 4, 4, 4, 10, 7, 8, 5, 1)//
        };
        return Arrays.stream(randomCases).toList();
    }

    private void printAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            String password = "test-password";
            logger.info("Running demo with user: {} and password: {}", username, password);
        } else {
            logger.warn("No authenticated user found. Running demo without authentication.");
        }
    }

    // Configure Spring Security to use the Keycloak container
    @DynamicPropertySource
    static void registerKeycloakProperties(DynamicPropertyRegistry registry) {
        // Start the container
        keycloak.start();

        // Get the actual URL that's accessible from outside the container
        String externalUrl = getPublicFacingUrl(keycloak);
        System.out.println("Keycloak External URL: " + externalUrl);

        // Log all container environment information for debugging
        System.out.println("Keycloak Container:");
        System.out.println("  Auth Server URL: " + keycloak.getAuthServerUrl());
        System.out.println("  Container IP: " + keycloak.getHost());
        System.out.println("  HTTP Port Mapping: " + keycloak.getMappedPort(8080));
        System.out.println("  HTTPS Port Mapping: " + keycloak.getMappedPort(8443));

        // Override the authServerUrl with our public-facing URL
        String publicAuthServerUrl = externalUrl + "/";

        // Create properties with the public URL
        Map<String, String> props = new HashMap<>();
        props.put("spring.security.oauth2.client.provider.keycloak.issuer-uri", publicAuthServerUrl + "realms/project-hub-realm");
        props.put("spring.security.oauth2.client.provider.keycloak.authorization-uri", publicAuthServerUrl + "realms/project-hub-realm/protocol/openid-connect/auth");
        props.put("spring.security.oauth2.client.provider.keycloak.token-uri", publicAuthServerUrl + "realms/project-hub-realm/protocol/openid-connect/token");
        props.put("spring.security.oauth2.client.provider.keycloak.user-info-uri", publicAuthServerUrl + "realms/project-hub-realm/protocol/openid-connect/userinfo");
        props.put("spring.security.oauth2.client.provider.keycloak.jwk-set-uri", publicAuthServerUrl + "realms/project-hub-realm/protocol/openid-connect/certs");

        props.put("spring.security.oauth2.client.registration.keycloak.client-id", "project-hub-client");
        props.put("spring.security.oauth2.client.registration.keycloak.client-secret", "test-client-secret");
        props.put("spring.security.oauth2.client.registration.keycloak.scope", "openid,profile,email");
        props.put("spring.security.oauth2.client.registration.keycloak.authorization-grant-type", "authorization_code");
        props.put("spring.security.oauth2.client.registration.keycloak.redirect-uri", "{baseUrl}/login/oauth2/code/{registrationId}");

        props.put("spring.security.oauth2.resourceserver.jwt.issuer-uri", publicAuthServerUrl + "realms/project-hub-realm");

        // Register all properties
        props.forEach((key, value) -> registry.add(key, () -> value));
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
            seleniumHandler.click(AvailabilityDialog.CANCEL_BUTTON);
        }

        // Edit availability dialog
        // We'll use the current date as a reference to find a record to edit
        {
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            seleniumHandler.click(AvailabilityListView.AVAILABILITY_GRID_EDIT_BUTTON_PREFIX + dateStr);
            seleniumHandler.waitForElementToBeClickable(AvailabilityDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(AvailabilityDialog.AVAILABILITY_DIALOG), AvailabilityDialog.AVAILABILITY_DIALOG, "../kassandra.wiki/screenshots/availability-edit-dialog.png");
            seleniumHandler.click(AvailabilityDialog.CANCEL_BUTTON);
        }

        // Delete availability dialog
        {
            // create something we can at least try to delete
            availabilityListViewTester.createAvailabilityConfirm(firstDay, 50);
            String dateStr = firstDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            seleniumHandler.click(AvailabilityListView.AVAILABILITY_GRID_DELETE_BUTTON_PREFIX + dateStr);
            seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, "../kassandra.wiki/screenshots/availability-delete-dialog.png");
            seleniumHandler.click(ConfirmDialog.CANCEL_BUTTON);
        }
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
            seleniumHandler.click(LocationDialog.CANCEL_BUTTON);
        }

        // Edit location dialog
        // We'll use the current date as a reference to find a record to edit
        {
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            seleniumHandler.click(LocationListView.LOCATION_GRID_EDIT_BUTTON_PREFIX + dateStr);
            seleniumHandler.waitForElementToBeClickable(LocationDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(LocationDialog.LOCATION_DIALOG), LocationDialog.LOCATION_DIALOG, "../kassandra.wiki/screenshots/location-edit-dialog.png");
            seleniumHandler.click(LocationDialog.CANCEL_BUTTON);
        }

        // Delete location dialog
        {
            locationListViewTester.createLocationConfirm(firstDay, "United States (US)", "California (ca)");
            String dateStr = firstDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            seleniumHandler.click(LocationListView.LOCATION_GRID_DELETE_BUTTON_PREFIX + dateStr);
            seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, "../kassandra.wiki/screenshots/location-delete-dialog.png");
            seleniumHandler.click(ConfirmDialog.CANCEL_BUTTON);
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
            seleniumHandler.click(OffDayDialog.CANCEL_BUTTON);
        }

        // Edit availability dialog
        {
            // Create an initial record
            offDayListViewTester.createOffDayConfirm(firstDayRecord1, lastDayRecord1, typeRecord1);

            offDayListViewTester.clickEditButtonForRecord(firstDayRecord1);
            seleniumHandler.waitForElementToBeClickable(OffDayDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(OffDayDialog.OFFDAY_DIALOG), OffDayDialog.OFFDAY_DIALOG, "../kassandra.wiki/screenshots/offday-edit-dialog.png");
            seleniumHandler.click(OffDayDialog.CANCEL_BUTTON);
        }

        // Delete availability dialog
        {
            offDayListViewTester.clickDeleteButtonForRecord(firstDayRecord1);
            seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, "../kassandra.wiki/screenshots/offday-delete-dialog.png");
            seleniumHandler.click(ConfirmDialog.CANCEL_BUTTON);
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
        seleniumHandler.click(ProductDialog.CANCEL_BUTTON);

        // Edit product dialog - open action menu first, then edit
        seleniumHandler.click(ProductListView.PRODUCT_GRID_EDIT_BUTTON_PREFIX + productName);
        seleniumHandler.waitForElementToBeClickable(ProductDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ProductDialog.PRODUCT_DIALOG), ProductDialog.PRODUCT_DIALOG, "../kassandra.wiki/screenshots/product-edit-dialog.png");
        seleniumHandler.click(ProductDialog.CANCEL_BUTTON);

        // Delete product dialog - open action menu first, then delete
        seleniumHandler.click(ProductListView.PRODUCT_GRID_DELETE_BUTTON_PREFIX + productName);
        seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, "../kassandra.wiki/screenshots/product-delete-dialog.png");
        seleniumHandler.click(ConfirmDialog.CANCEL_BUTTON);
    }

    /**
     * Takes screenshots of Project create, edit and delete dialogs
     */
    private void takeProjectDialogScreenshots() {
        // Create project dialog
        seleniumHandler.click(FeatureListView.CREATE_FEATURE_BUTTON_ID);
        seleniumHandler.waitForElementToBeClickable(FeatureDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(FeatureDialog.FEATURE_DIALOG), FeatureDialog.FEATURE_DIALOG, "../kassandra.wiki/screenshots/feature-create-dialog.png");
        seleniumHandler.click(FeatureDialog.CANCEL_BUTTON);

        // Edit project dialog - open action menu first, then edit
        seleniumHandler.click(FeatureListView.FEATURE_GRID_EDIT_BUTTON_PREFIX + featureName);
        seleniumHandler.waitForElementToBeClickable(FeatureDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(FeatureDialog.FEATURE_DIALOG), FeatureDialog.FEATURE_DIALOG, "../kassandra.wiki/screenshots/feature-edit-dialog.png");
        seleniumHandler.click(FeatureDialog.CANCEL_BUTTON);

        // Delete project dialog - open action menu first, then delete
        seleniumHandler.click(FeatureListView.FEATURE_GRID_DELETE_BUTTON_PREFIX + featureName);
        seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, "../kassandra.wiki/screenshots/feature-delete-dialog.png");
        seleniumHandler.click(ConfirmDialog.CANCEL_BUTTON);
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void takeScreenshots(RandomCase randomCase, TestInfo testInfo) throws Exception {
        // Set browser window to a fixed size for consistent screenshots
//        seleniumHandler.setWindowSize(1024, 800);
        seleniumHandler.setWindowSize(1800, 1300);

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
        taskName    = nameGenerator.generateSprintName(0);

        productListViewTester.switchToProductListViewWithOidc("christopher.paul@kassandra.org", "password", "../kassandra.wiki/screenshots/login-view.png", testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
//        seleniumHandler.takeScreenShot("../kassandra.wiki/screenshots/product-list-view.png");
//        takeProductDialogScreenshots();
        productListViewTester.selectProduct(productName);
//        seleniumHandler.takeScreenShot("../kassandra.wiki/screenshots/version-list-view.png");
//        takeVersionDialogScreenshots();
        versionListViewTester.selectVersion(versionName);
//        seleniumHandler.takeScreenShot("../kassandra.wiki/screenshots/feature-list-view.png");
//        takeProjectDialogScreenshots();
        featureListViewTester.selectFeature(featureName);
//        seleniumHandler.takeScreenShot("../kassandra.wiki/screenshots/sprint-list-view.png");
//        takeSprintDialogScreenshots();

//        seleniumHandler.setWindowSize(1800, 1300);
        seleniumHandler.click(SprintListView.SPRINT_GRID_CONFIG_BUTTON_PREFIX + sprintName);
        seleniumHandler.waitUntil(ExpectedConditions.elementToBeClickable(By.id(TaskListView.TASK_LIST_PAGE_TITLE)));

//        sprintListViewTester.selectSprint(sprintName);
//        seleniumHandler.waitForElementToBeClickable(RenderUtil.GANTT_CHART);
//        seleniumHandler.waitForElementToBeClickable(RenderUtil.BURNDOWN_CHART);
//        seleniumHandler.takeScreenShot("../kassandra.wiki/screenshots/sprint-quality-board.png");
        takeTaskDialogScreenshots();

//        // After visiting the SprintQualityBoard, go back to SprintListView and use the column config button
//        seleniumHandler.click("Sprints (" + sprintName + ")"); // Go back to SprintListView using breadcrumb
//        // Find and click the column configuration button
//        seleniumHandler.click(SprintListView.SPRINT_GRID_CONFIG_BUTTON_PREFIX + sprintName);
//        seleniumHandler.waitForElementToBeClickable(RenderUtil.GANTT_CHART);
//        seleniumHandler.takeScreenShot("../kassandra.wiki/screenshots/task-list-view.png");
//
//
//        userListViewTester.switchToUserListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
//        seleniumHandler.takeScreenShot("../kassandra.wiki/screenshots/user-list-view.png");
//        takeUserDialogScreenshots();
//
////        seleniumHandler.setWindowSize(1024, 800);
//        // Navigate to AvailabilityListView for the current user and take screenshots
//        availabilityListViewTester.switchToAvailabilityListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo), null);
//        seleniumHandler.takeScreenShot("../kassandra.wiki/screenshots/availability-list-view.png");
//        takeAvailabilityDialogScreenshots();
//
//        // Navigate to LocationListView for the current user and take screenshots
//        locationListViewTester.switchToLocationListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo), null);
//        seleniumHandler.takeScreenShot("../kassandra.wiki/screenshots/location-list-view.png");
//        takeLocationDialogScreenshots();
//
//        // Navigate to OffDayListView for the current user and take screenshots
////        seleniumHandler.setWindowSize(1800, 1300);
//        offDayListViewTester.switchToOffDayListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo), null);
//        seleniumHandler.takeScreenShot("../kassandra.wiki/screenshots/offday-list-view.png");
//        takeOffDayDialogScreenshots();

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
        seleniumHandler.click(SprintDialog.CANCEL_BUTTON);

        // Edit sprint dialog - open action menu first, then edit
        seleniumHandler.click(SprintListView.SPRINT_GRID_EDIT_BUTTON_PREFIX + sprintName);
        seleniumHandler.waitForElementToBeClickable(SprintDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(SprintDialog.SPRINT_DIALOG), SprintDialog.SPRINT_DIALOG, "../kassandra.wiki/screenshots/sprint-edit-dialog.png");
        seleniumHandler.click(SprintDialog.CANCEL_BUTTON);

        // Delete sprint dialog - open action menu first, then delete
        seleniumHandler.click(SprintListView.SPRINT_GRID_DELETE_BUTTON_PREFIX + sprintName);
        seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, "../kassandra.wiki/screenshots/sprint-delete-dialog.png");
        seleniumHandler.click(ConfirmDialog.CANCEL_BUTTON);
    }

    private void takeTaskDialogScreenshots() {
        // Create product dialog
        seleniumHandler.click(TaskListView.CREATE_MILESTONE_BUTTON_ID);
        seleniumHandler.ensureIsInList(ProductListView.PRODUCT_GRID_NAME_PREFIX, "New Milestone-34");
        seleniumHandler.click(TaskListView.CREATE_STORY_BUTTON_ID);
        seleniumHandler.ensureIsInList(ProductListView.PRODUCT_GRID_NAME_PREFIX, "New Story-2");
        seleniumHandler.click(TaskListView.CREATE_TASK_BUTTON_ID);
        seleniumHandler.ensureIsInList(ProductListView.PRODUCT_GRID_NAME_PREFIX, "New Task-3");
        // select the milestone
        seleniumHandler.click(TaskListView.TASK_GRID_NAME_PREFIX + "New Milestone-34");
        // select start cell
        seleniumHandler.click(TaskListView.TASK_GRID_NAME_PREFIX + "New Milestone-34" + "-start-cell");
    }

    /**
     * Takes screenshots of Task view and dialogs
     */
//    private void takeTaskListViewScreenshots() {
//        // Navigate to the TaskListView page
//        seleniumHandler.click("task-list"); // Click on the Tasks link in the breadcrumb
//        seleniumHandler.waitForElementToBeClickable("task-grid-name-"); // Wait for the task grid to be loaded
//
//        // Take a screenshot of the task list view
//        seleniumHandler.takeScreenShot("../kassandra.wiki/screenshots/task-list-view.png");
//
//        // Create task dialog (if there's a create button similar to other views)
//        if (seleniumHandler.isElementPresent("create-task-button")) {
//            seleniumHandler.click("create-task-button");
//            seleniumHandler.waitForDialogToOpen();
//            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement("task-dialog"), "task-dialog", "../kassandra.wiki/screenshots/task-create-dialog.png");
//            seleniumHandler.click("cancel-button"); // Assume there's a cancel button in the dialog
//        }
//
//        // If tasks exist, try to edit one
//        if (seleniumHandler.isElementPresent("task-grid-edit-button-")) {
//            seleniumHandler.click("task-grid-edit-button-");
//            seleniumHandler.waitForDialogToOpen();
//            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement("task-dialog"), "task-dialog", "../kassandra.wiki/screenshots/task-edit-dialog.png");
//            seleniumHandler.click("cancel-button");
//        }
//
//        // If tasks exist, try to delete one
//        if (seleniumHandler.isElementPresent("task-grid-delete-button-")) {
//            seleniumHandler.click("task-grid-delete-button-");
//            seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON);
//            seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, "../kassandra.wiki/screenshots/task-delete-dialog.png");
//            seleniumHandler.click(ConfirmDialog.CANCEL_BUTTON);
//        }
//    }

    /**
     * Takes screenshots of User create, edit and delete dialogs
     */
    private void takeUserDialogScreenshots() {
        // Create user dialog
        seleniumHandler.click(UserListView.CREATE_USER_BUTTON);
        seleniumHandler.waitForElementToBeClickable(UserDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(UserDialog.USER_DIALOG), UserDialog.USER_DIALOG, "../kassandra.wiki/screenshots/user-create-dialog.png");
        seleniumHandler.click(UserDialog.CANCEL_BUTTON);

        // Edit user dialog - open action menu first, then edit
        // Get first user in the list
        seleniumHandler.click(UserListView.USER_GRID_EDIT_BUTTON_PREFIX + userName);
        seleniumHandler.waitForElementToBeClickable(UserDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(UserDialog.USER_DIALOG), UserDialog.USER_DIALOG, "../kassandra.wiki/screenshots/user-edit-dialog.png");
        seleniumHandler.click(UserDialog.CANCEL_BUTTON);

        // Delete user dialog - open action menu first, then delete
        seleniumHandler.click(UserListView.USER_GRID_DELETE_BUTTON_PREFIX + userName);
        seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, "../kassandra.wiki/screenshots/user-delete-dialog.png");
        seleniumHandler.click(ConfirmDialog.CANCEL_BUTTON);
    }

    /**
     * Takes screenshots of Version create, edit and delete dialogs
     */
    private void takeVersionDialogScreenshots() {
        // Create version dialog
        seleniumHandler.click(VersionListView.CREATE_VERSION_BUTTON);
        seleniumHandler.waitForElementToBeClickable(VersionDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(VersionDialog.VERSION_DIALOG), VersionDialog.VERSION_DIALOG, "../kassandra.wiki/screenshots/version-create-dialog.png");
        seleniumHandler.click(VersionDialog.CANCEL_BUTTON);

        // Edit version dialog - open action menu first, then edit
        seleniumHandler.click(VersionListView.VERSION_GRID_EDIT_BUTTON_PREFIX + versionName);
        seleniumHandler.waitForElementToBeClickable(VersionDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(VersionDialog.VERSION_DIALOG), VersionDialog.VERSION_DIALOG, "../kassandra.wiki/screenshots/version-edit-dialog.png");
        seleniumHandler.click(VersionDialog.CANCEL_BUTTON);

        // Delete version dialog - open action menu first, then delete
        seleniumHandler.click(VersionListView.VERSION_GRID_DELETE_BUTTON_PREFIX + versionName);
        seleniumHandler.waitForElementToBeClickable(ConfirmDialog.CANCEL_BUTTON); // Wait for dialog
        seleniumHandler.takeElementScreenShot(seleniumHandler.findDialogOverlayElement(ConfirmDialog.CONFIRM_DIALOG), ConfirmDialog.CONFIRM_DIALOG, "../kassandra.wiki/screenshots/version-delete-dialog.png");
        seleniumHandler.click(ConfirmDialog.CANCEL_BUTTON);
    }
}
