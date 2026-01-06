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

package de.bushnaq.abdalla.kassandra.ui.view.util;

import de.bushnaq.abdalla.kassandra.ui.dialog.AvailabilityDialog;
import de.bushnaq.abdalla.kassandra.ui.dialog.ConfirmDialog;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.AvailabilityListView;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test helper class for interacting with the Availability UI components.
 * <p>
 * This class provides methods to test availability-related operations in the UI such as
 * creating, editing, deleting availability records and navigating between views. It uses
 * {@link HumanizedSeleniumHandler} to interact with UI elements and validate results.
 */
@Component
@Lazy
public class AvailabilityListViewTester extends AbstractViewTester {


    @Autowired
    private ProductListViewTester productListViewTester;

    /**
     * Constructs a new AvailabilityViewTester with the given Selenium handler and server port.
     *
     * @param seleniumHandler the handler for Selenium operations
     * @param port            the port on which the application server is running
     */
    public AvailabilityListViewTester(HumanizedSeleniumHandler seleniumHandler, @Value("${local.server.port:8080}") int port) {
        super(seleniumHandler, port);
    }

    // Add missing static imports at the top of the class
    private static void assertTrue(boolean condition, String message) {
        org.junit.jupiter.api.Assertions.assertTrue(condition, message);
    }

    public void closeConfirmDialog(String button) {
        closeConfirmDialog(button, AvailabilityListView.AVAILABILITY_LIST_PAGE_TITLE);
    }

    public void closeDialog(String button) {
        seleniumHandler.wait(200);
        seleniumHandler.click(button);
        seleniumHandler.waitForElementToBeClickable(AvailabilityListView.AVAILABILITY_LIST_PAGE_TITLE);
    }

    /**
     * Tests the creation of an availability record where the user cancels the operation.
     * <p>
     * Opens the availability creation dialog, enters values for start date and availability percentage,
     * then cancels the dialog. Verifies that no record with the specified start date appears in the list
     * and that the cancellation didn't affect any existing data.
     *
     * @param startDate              the start date for the availability record
     * @param availabilityPercentage the availability percentage (as a value between 0 and 150)
     */
    public void createAvailabilityCancel(LocalDate startDate, int availabilityPercentage) {
        seleniumHandler.click(AvailabilityListView.CREATE_AVAILABILITY_BUTTON);
        seleniumHandler.setDatePickerValue(AvailabilityDialog.AVAILABILITY_START_DATE_FIELD, startDate);
        seleniumHandler.setTextField(AvailabilityDialog.AVAILABILITY_PERCENTAGE_FIELD, String.valueOf(availabilityPercentage));
        closeDialog(AvailabilityDialog.CANCEL_BUTTON);

        // Verify the record doesn't appear in the list
        String startDateStr = startDate.format(dateFormatter);
        seleniumHandler.ensureIsNotInList(AvailabilityListView.AVAILABILITY_GRID_START_DATE_PREFIX, startDateStr);
    }

    /**
     * Tests the successful creation of an availability record.
     * <p>
     * Opens the availability creation dialog, enters start date and availability percentage,
     * then confirms the dialog. Verifies that a record with the specified start date appears
     * in the availability list and that the fields are correctly stored.
     *
     * @param startDate              the start date for the availability record
     * @param availabilityPercentage the availability percentage (as a value between 0 and 150)
     */
    public void createAvailabilityConfirm(LocalDate startDate, int availabilityPercentage) {
        seleniumHandler.click(AvailabilityListView.CREATE_AVAILABILITY_BUTTON);
        seleniumHandler.setDatePickerValue(AvailabilityDialog.AVAILABILITY_START_DATE_FIELD, startDate);
        seleniumHandler.setTextField(AvailabilityDialog.AVAILABILITY_PERCENTAGE_FIELD, String.valueOf(availabilityPercentage));
        closeDialog(AvailabilityDialog.CONFIRM_BUTTON);

        seleniumHandler.wait(300);
        // Verify the record appears in the list
        String startDateStr = startDate.format(dateFormatter);
        seleniumHandler.ensureIsInList(AvailabilityListView.AVAILABILITY_GRID_START_DATE_PREFIX, startDateStr);
    }

    /**
     * Tests attempting to create a duplicate availability record with the same start date.
     * <p>
     * Opens the availability creation dialog, enters a start date that already exists,
     * then attempts to confirm. Verifies that an error message is shown and the duplicate
     * record is not created.
     *
     * @param existingStartDate the start date that already has an availability record
     * @param newPercentage     the new availability percentage for the duplicate record
     */
    public void createDuplicateDateAvailability(LocalDate existingStartDate, int newPercentage) {
        String existingDateStr = existingStartDate.format(dateFormatter);

        // Try to create a duplicate record
        seleniumHandler.click(AvailabilityListView.CREATE_AVAILABILITY_BUTTON);
        seleniumHandler.setDatePickerValue(AvailabilityDialog.AVAILABILITY_START_DATE_FIELD, existingStartDate);
        seleniumHandler.setTextField(AvailabilityDialog.AVAILABILITY_PERCENTAGE_FIELD, String.valueOf(newPercentage));
        seleniumHandler.wait(200);
        seleniumHandler.waitForElementToBeDisabled(AvailabilityDialog.CONFIRM_BUTTON);

        // Cancel the dialog since we expect it to still be open
        closeDialog(AvailabilityDialog.CANCEL_BUTTON);
    }

    /**
     * Tests attempting to create an invalid availability record with out-of-range percentage.
     * <p>
     * Opens the availability creation dialog, enters an out-of-range percentage value,
     * then attempts to confirm. Verifies that an error message is shown and the invalid
     * record is not created.
     *
     * @param startDate         the start date for the new availability record
     * @param invalidPercentage the invalid availability percentage (outside 0-150 range)
     */
    public void createInvalidPercentageAvailability(LocalDate startDate, int invalidPercentage) {
        String startDateStr = startDate.format(dateFormatter);

        // Try to create a record with invalid percentage
        seleniumHandler.click(AvailabilityListView.CREATE_AVAILABILITY_BUTTON);
        seleniumHandler.setDatePickerValue(AvailabilityDialog.AVAILABILITY_START_DATE_FIELD, startDate);
        seleniumHandler.setTextField(AvailabilityDialog.AVAILABILITY_PERCENTAGE_FIELD, String.valueOf(invalidPercentage));
        seleniumHandler.wait(200);
        seleniumHandler.waitForElementToBeDisabled(AvailabilityDialog.CONFIRM_BUTTON);

        // Cancel the dialog since we expect it to still be open
        closeDialog(AvailabilityDialog.CANCEL_BUTTON);
    }

    /**
     * Tests availability record deletion where the user cancels the delete confirmation.
     * <p>
     * Clicks the delete button for the specified availability record,
     * then cancels the confirmation dialog. Verifies that the record still exists in the list.
     *
     * @param startDate the start date of the availability record to attempt to delete
     */
    public void deleteAvailabilityCancel(LocalDate startDate) {
        String startDateStr = startDate.format(dateFormatter);
        seleniumHandler.click(AvailabilityListView.AVAILABILITY_GRID_DELETE_BUTTON_PREFIX + startDateStr);
        closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);
        seleniumHandler.ensureIsInList(AvailabilityListView.AVAILABILITY_GRID_START_DATE_PREFIX, startDateStr);
    }

    /**
     * Tests the successful deletion of an availability record.
     * <p>
     * Clicks the delete button for the specified availability record,
     * then confirms the deletion in the confirmation dialog. Verifies that the record
     * is removed from the availability list.
     *
     * @param startDate the start date of the availability record to delete
     */
    public void deleteAvailabilityConfirm(LocalDate startDate) {
        String startDateStr = startDate.format(dateFormatter);
        seleniumHandler.click(AvailabilityListView.AVAILABILITY_GRID_DELETE_BUTTON_PREFIX + startDateStr);
        closeConfirmDialog(ConfirmDialog.CONFIRM_BUTTON);
        seleniumHandler.ensureIsNotInList(AvailabilityListView.AVAILABILITY_GRID_START_DATE_PREFIX, startDateStr);
    }

    /**
     * Tests the editing of an availability record where the user cancels the edit operation.
     * <p>
     * Clicks the edit button for the availability record with the specified start date,
     * enters new values, then cancels the edit dialog. Verifies that the record
     * still maintains its original values.
     *
     * @param originalStartDate  the original start date of the record to edit
     * @param newStartDate       the new start date to attempt to assign
     * @param originalPercentage the original availability percentage to verify remains unchanged after cancellation
     * @param newPercentage      the availability percentage to attempt to assign
     */
    public void editAvailabilityCancel(LocalDate originalStartDate, LocalDate newStartDate, int originalPercentage, int newPercentage) {
        String originalDateStr = originalStartDate.format(dateFormatter);

        // Verify original values first
        verifyAvailabilityValue(originalDateStr, originalPercentage);

        // Edit the record but cancel
        seleniumHandler.click(AvailabilityListView.AVAILABILITY_GRID_EDIT_BUTTON_PREFIX + originalDateStr);
        seleniumHandler.setDatePickerValue(AvailabilityDialog.AVAILABILITY_START_DATE_FIELD, newStartDate);
        seleniumHandler.setTextField(AvailabilityDialog.AVAILABILITY_PERCENTAGE_FIELD, String.valueOf(newPercentage));
        closeDialog(AvailabilityDialog.CANCEL_BUTTON);

        // Verify original record still exists and new one doesn't
        seleniumHandler.ensureIsInList(AvailabilityListView.AVAILABILITY_GRID_START_DATE_PREFIX, originalDateStr);
        if (!originalStartDate.equals(newStartDate)) {
            String newDateStr = newStartDate.format(dateFormatter);
            seleniumHandler.ensureIsNotInList(AvailabilityListView.AVAILABILITY_GRID_START_DATE_PREFIX, newDateStr);
        }

        // Verify the value didn't change
        verifyAvailabilityValue(originalDateStr, originalPercentage);
    }

    /**
     * Tests the successful editing of an availability record.
     * <p>
     * Clicks the edit button for the availability record with the specified start date,
     * enters new values, then confirms the edit. Verifies that the record with the new values
     * appears in the list and the record with the old values is gone.
     *
     * @param originalStartDate the original start date of the record to edit
     * @param newStartDate      the new start date to assign
     * @param newPercentage     the new availability percentage to assign
     */
    public void editAvailabilityConfirm(LocalDate originalStartDate, LocalDate newStartDate, int newPercentage) {
        String originalDateStr = originalStartDate.format(dateFormatter);

        // Edit the record and confirm
        seleniumHandler.click(AvailabilityListView.AVAILABILITY_GRID_EDIT_BUTTON_PREFIX + originalDateStr);
        seleniumHandler.setDatePickerValue(AvailabilityDialog.AVAILABILITY_START_DATE_FIELD, newStartDate);
        seleniumHandler.setTextField(AvailabilityDialog.AVAILABILITY_PERCENTAGE_FIELD, String.valueOf(newPercentage));
        closeDialog(AvailabilityDialog.CONFIRM_BUTTON);

        seleniumHandler.wait(300);
        String newDateStr = newStartDate.format(dateFormatter);

        // Verify the new record exists and the old one is gone (if dates are different)
        seleniumHandler.ensureIsInList(AvailabilityListView.AVAILABILITY_GRID_START_DATE_PREFIX, newDateStr);
        if (!originalStartDate.equals(newStartDate)) {
            seleniumHandler.ensureIsNotInList(AvailabilityListView.AVAILABILITY_GRID_START_DATE_PREFIX, originalDateStr);
        }

        // Verify the updated value
        verifyAvailabilityValue(newDateStr, newPercentage);
    }

    /**
     * Logs locale information from both backend (JVM) and frontend (browser) for debugging.
     * This helps diagnose date formatting issues related to locale mismatches.
     */
    private void logLocaleInformation() {
        System.out.println("=== LOCALE INFORMATION ===");

        // Backend (JVM) locale information
        System.out.println("Backend JVM Locale: " + java.util.Locale.getDefault());
        System.out.println("Backend JVM Language: " + java.util.Locale.getDefault().getLanguage());
        System.out.println("Backend JVM Country: " + java.util.Locale.getDefault().getCountry());
        System.out.println("Backend Time Zone: " + java.time.ZoneId.systemDefault());
        System.out.println("Backend Date Format (sample): " + java.time.LocalDate.now().format(dateFormatter));

        // Frontend (Browser) locale information via JavaScript
        try {
            Object browserLanguage = seleniumHandler.executeJavaScript("return navigator.language;");
            System.out.println("Browser Language: " + browserLanguage);

            Object browserLanguages = seleniumHandler.executeJavaScript("return navigator.languages;");
            System.out.println("Browser Languages: " + browserLanguages);

            Object timeZone = seleniumHandler.executeJavaScript("return Intl.DateTimeFormat().resolvedOptions().timeZone;");
            System.out.println("Browser Time Zone: " + timeZone);

            Object locale = seleniumHandler.executeJavaScript("return Intl.DateTimeFormat().resolvedOptions().locale;");
            System.out.println("Browser Intl Locale: " + locale);

            // Test date formatting in browser
            Object dateFormat = seleniumHandler.executeJavaScript(
                    "const date = new Date(2025, 0, 15); " + // January 15, 2025
                            "return new Intl.DateTimeFormat(undefined, {year: 'numeric', month: '2-digit', day: '2-digit'}).format(date);"
            );
            System.out.println("Browser Date Format (sample): " + dateFormat);

        } catch (Exception e) {
            System.out.println("Error getting browser locale information: " + e.getMessage());
        }

        System.out.println("=========================");
    }

    /**
     * Navigates to the AvailabilityListView for a specific user.
     * <p>
     * Opens the availability list URL directly, logs in if needed, and waits for the page to load
     * by checking for the presence of the page title element.
     *
     * @param recordingFolderName The folder name for recording the test
     * @param testName            The name of the test for recording
     * @param username            The username for which to view availability (optional, uses current user if null)
     */
    public void switchToAvailabilityListView(String recordingFolderName, String testName, String username) throws Exception {
        //- Check if we need to log in
        if (!seleniumHandler.getCurrentUrl().contains("/ui/")) {
            productListViewTester.switchToProductListViewWithOidc(
                    "christopher.paul@kassandra.org",
                    "password",
                    null,
                    recordingFolderName,
                    testName
            );
        }
        // Navigate to the availability view for the specific user or current user
        String url = "http://localhost:" + port + "/ui/" + AvailabilityListView.ROUTE;
        seleniumHandler.getAndCheck(url);
        seleniumHandler.waitForElementToBeClickable(AvailabilityListView.AVAILABILITY_LIST_PAGE_TITLE);
    }

    /**
     * Verifies the availability percentage for a specific record.
     * <p>
     * Opens the edit dialog for the record with the specified start date and verifies
     * that the percentage field has the expected value. Cancels the dialog after verification
     * to avoid making changes.
     *
     * @param startDateStr       the formatted start date string of the record to verify
     * @param expectedPercentage the expected availability percentage
     */
    private void verifyAvailabilityValue(String startDateStr, int expectedPercentage) {
        // Open the edit dialog for the specified record
        seleniumHandler.click(AvailabilityListView.AVAILABILITY_GRID_EDIT_BUTTON_PREFIX + startDateStr);

        // Read the actual percentage value
        String actualPercentageText = seleniumHandler.getTextField(AvailabilityDialog.AVAILABILITY_PERCENTAGE_FIELD);
        int    actualPercentage     = Integer.parseInt(actualPercentageText);

        // Cancel the dialog to avoid making changes
        closeDialog(AvailabilityDialog.CANCEL_BUTTON);

        // Allow for a small floating-point difference
        assertEquals(expectedPercentage, actualPercentage, "Availability percentage mismatch for record with start date: " + startDateStr);
    }

    /**
     * Tests that a user cannot delete their only availability record.
     * <p>
     * Verifies that the delete button is disabled when the user has only one availability record.
     *
     * @param startDate the start date of the only availability record
     */
    public void verifyCannotDeleteOnlyAvailability(LocalDate startDate) {
        String startDateStr = startDate.format(dateFormatter);
        // Find the delete button element for the specified availability record
        WebElement deleteButton = seleniumHandler.findElement(By.id(AvailabilityListView.AVAILABILITY_GRID_DELETE_BUTTON_PREFIX + startDateStr));
        // Check if the button has the disabled attribute
        boolean hasDisabledAttribute = deleteButton.getAttribute("disabled") != null;
        // Assert that the button is properly disabled
        assertTrue(hasDisabledAttribute, "Delete button should be disabled for the only availability record (missing disabled attribute)");
    }
}
