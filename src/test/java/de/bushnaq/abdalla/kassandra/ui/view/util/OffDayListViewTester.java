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

import de.bushnaq.abdalla.kassandra.dto.OffDayType;
import de.bushnaq.abdalla.kassandra.ui.dialog.ConfirmDialog;
import de.bushnaq.abdalla.kassandra.ui.dialog.OffDayDialog;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.LoginView;
import de.bushnaq.abdalla.kassandra.ui.view.OffDayListView;
import de.bushnaq.abdalla.kassandra.ui.view.ProductListView;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test helper class for interacting with the Off Day UI components.
 * <p>
 * This class provides methods to test off day-related operations in the UI such as
 * creating, editing, deleting off day records and navigating between views. It uses
 * {@link HumanizedSeleniumHandler} to interact with UI elements and validate results.
 */
@Component
@Lazy
public class OffDayListViewTester extends AbstractViewTester {

    /**
     * Constructs a new OffDayListViewTester with the given Selenium handler and server port.
     *
     * @param seleniumHandler the handler for Selenium operations
     * @param port            the port on which the application server is running
     */
    public OffDayListViewTester(HumanizedSeleniumHandler seleniumHandler, @Value("${local.server.port:8080}") int port) {
        super(seleniumHandler, port);
    }

    public void clickDeleteButtonForRecord(LocalDate firstDay) {
        String id = findOffDayRecordId(firstDay);
        seleniumHandler.click(OffDayListView.OFFDAY_GRID_DELETE_BUTTON_PREFIX + id);
    }

    /**
     * Clicks the edit button for the off day record with the specified first day.
     *
     * @param firstDay the first day of the record to edit
     */
    public void clickEditButtonForRecord(LocalDate firstDay) {
        String id = findOffDayRecordId(firstDay);
        seleniumHandler.click(OffDayListView.OFFDAY_GRID_EDIT_BUTTON_PREFIX + id);
    }

    private void closeDialog(String confirmButton) {
        seleniumHandler.click(confirmButton);
        seleniumHandler.waitUntil(ExpectedConditions.invisibilityOfElementLocated(By.id(OffDayDialog.OFFDAY_DIALOG)));
    }

    /**
     * Tests validation for invalid date range (first day after last day).
     * <p>
     * Attempts to create an off day record with first day after last day.
     * Verifies that validation prevents the creation and an error message is displayed.
     *
     * @param firstDay first day of the off day period (set after last day)
     * @param lastDay  last day of the off day period (set before first day)
     * @param type     type of off day
     */
    public void createInvalidDateRangeOffDay(LocalDate firstDay, LocalDate lastDay, OffDayType type) {
        // Try to create a record with invalid date range
        seleniumHandler.click(OffDayListView.CREATE_OFFDAY_BUTTON);
        seleniumHandler.setDatePickerValue(OffDayDialog.OFFDAY_START_DATE_FIELD, firstDay);
        seleniumHandler.setDatePickerValue(OffDayDialog.OFFDAY_END_DATE_FIELD, lastDay);
        seleniumHandler.setComboBoxValue(OffDayDialog.OFFDAY_TYPE_FIELD, type.name());
        closeDialog(OffDayDialog.CONFIRM_BUTTON);

        // Verify validation error occurs
        seleniumHandler.waitUntil(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("vaadin-notification-card")));

        // Cancel the dialog
        closeDialog(OffDayDialog.CANCEL_BUTTON);

        // Verify the record wasn't created
        verifyOffDayRecordNotExists(firstDay, lastDay, type);
    }

    /**
     * Tests the creation of an off day record where the user cancels the operation.
     * <p>
     * Opens the off day creation dialog, enters values for the fields,
     * then cancels the dialog. Verifies that no record was created.
     *
     * @param firstDay first day of the off day period
     * @param lastDay  last day of the off day period
     * @param type     type of off day (e.g. VACATION, SICK_LEAVE)
     */
    public void createOffDayCancel(LocalDate firstDay, LocalDate lastDay, OffDayType type) {
        seleniumHandler.click(OffDayListView.CREATE_OFFDAY_BUTTON);
        seleniumHandler.setDatePickerValue(OffDayDialog.OFFDAY_START_DATE_FIELD, firstDay);
        seleniumHandler.setDatePickerValue(OffDayDialog.OFFDAY_END_DATE_FIELD, lastDay);
        seleniumHandler.setComboBoxValue(OffDayDialog.OFFDAY_TYPE_FIELD, type.name());
        closeDialog(OffDayDialog.CANCEL_BUTTON);

        // Verify the record doesn't appear in the list
        verifyOffDayRecordNotExists(firstDay, lastDay, type);
    }

    /**
     * Tests the successful creation of an off day record.
     * <p>
     * Opens the off day creation dialog, enters values for all fields,
     * then confirms the dialog. Verifies that a record with the specified details appears
     * in the off day list.
     *
     * @param firstDay first day of the off day period
     * @param lastDay  last day of the off day period
     * @param type     type of off day (e.g. VACATION, SICK_LEAVE)
     */
    public void createOffDayConfirm(LocalDate firstDay, LocalDate lastDay, OffDayType type) {
        seleniumHandler.click(OffDayListView.CREATE_OFFDAY_BUTTON);
        seleniumHandler.setDatePickerValue(OffDayDialog.OFFDAY_START_DATE_FIELD, firstDay);
        seleniumHandler.setDatePickerValue(OffDayDialog.OFFDAY_END_DATE_FIELD, lastDay);
        seleniumHandler.setComboBoxValue(OffDayDialog.OFFDAY_TYPE_FIELD, type.name());
        closeDialog(OffDayDialog.CONFIRM_BUTTON);
        seleniumHandler.wait(300);
        // Verify the record appears in the list
        verifyOffDayRecordExists(firstDay, lastDay, type);
    }

    /**
     * Tests off day record deletion where the user cancels the delete confirmation.
     * <p>
     * Clicks the delete button for the specified off day record,
     * then cancels the confirmation dialog. Verifies that the record still exists in the list.
     *
     * @param firstDay first day of the off day period to attempt to delete
     * @param lastDay  last day of the off day period
     * @param type     type of the off day
     */
    public void deleteOffDayCancel(LocalDate firstDay, LocalDate lastDay, OffDayType type) {
        // Find the record
        String id = findOffDayRecordId(firstDay);

        // Click delete but cancel confirmation
        seleniumHandler.click(OffDayListView.OFFDAY_GRID_DELETE_BUTTON_PREFIX + id);
        closeConfirmDialog(ConfirmDialog.CANCEL_BUTTON);

        // Verify the record still exists
        verifyOffDayRecordExists(firstDay, lastDay, type);
    }

    /**
     * Tests the successful deletion of an off day record.
     * <p>
     * Clicks the delete button for the specified off day record,
     * then confirms the deletion in the confirmation dialog. Verifies that the record
     * is removed from the off day list.
     *
     * @param firstDay first day of the off day period to delete
     * @param lastDay  last day of the off day period
     * @param type     type of the off day
     */
    public void deleteOffDayConfirm(LocalDate firstDay, LocalDate lastDay, OffDayType type) {
        // Find the record
        String id = findOffDayRecordId(firstDay);

        // Click delete and confirm
        seleniumHandler.click(OffDayListView.OFFDAY_GRID_DELETE_BUTTON_PREFIX + id);
        closeConfirmDialog(ConfirmDialog.CONFIRM_BUTTON);

        // Verify the record is deleted
        verifyOffDayRecordNotExists(firstDay, lastDay, type);
    }

    /**
     * Tests the editing of an off day record where the user cancels the edit operation.
     * <p>
     * Clicks the edit button for the off day record with the specified start date,
     * enters new values, then cancels the edit dialog. Verifies that the record
     * still maintains its original values.
     *
     * @param originalFirstDay the original first day of the record to edit
     * @param newFirstDay      the new first day to attempt to assign
     * @param originalLastDay  the original last day of the record to edit
     * @param newLastDay       the new last day to attempt to assign
     * @param originalType     the original type of the record to edit
     * @param newType          the new type to attempt to assign
     */
    public void editOffDayCancel(LocalDate originalFirstDay, LocalDate newFirstDay,
                                 LocalDate originalLastDay, LocalDate newLastDay,
                                 OffDayType originalType, OffDayType newType) {
        String originalFirstDayStr = originalFirstDay.format(dateFormatter);

        // Find the record and click edit
        clickEditButtonForRecord(originalFirstDay);

        // Change all fields
        seleniumHandler.setDatePickerValue(OffDayDialog.OFFDAY_START_DATE_FIELD, newFirstDay);
        seleniumHandler.setDatePickerValue(OffDayDialog.OFFDAY_END_DATE_FIELD, newLastDay);
        seleniumHandler.setComboBoxValue(OffDayDialog.OFFDAY_TYPE_FIELD, newType.name());

        // Cancel the edit
        closeDialog(OffDayDialog.CANCEL_BUTTON);

        // Verify original record still exists with original values
        verifyOffDayRecordExists(originalFirstDay, originalLastDay, originalType);

        // If the dates are different, verify new record wasn't created
        if (!originalFirstDay.equals(newFirstDay)) {
            verifyOffDayRecordNotExists(newFirstDay, newLastDay, newType);
        }
    }

    /**
     * Tests the successful editing of an off day record.
     * <p>
     * Clicks the edit button for the off day record with the specified first day,
     * enters new values, then confirms the edit. Verifies that the record with the new values
     * appears in the list and the record with the old values is gone.
     *
     * @param originalFirstDay the original first day of the record to edit
     * @param newFirstDay      the new first day to assign
     * @param originalLastDay  the original last day of the record to edit
     * @param newLastDay       the new last day to assign
     * @param originalType     the original type of the record to edit
     * @param newType          the new type to assign
     */
    public void editOffDayConfirm(LocalDate originalFirstDay, LocalDate newFirstDay,
                                  LocalDate originalLastDay, LocalDate newLastDay,
                                  OffDayType originalType, OffDayType newType) {
        // Find the record and click edit
        clickEditButtonForRecord(originalFirstDay);

        // Change all fields
        seleniumHandler.setDatePickerValue(OffDayDialog.OFFDAY_START_DATE_FIELD, newFirstDay);
        seleniumHandler.setDatePickerValue(OffDayDialog.OFFDAY_END_DATE_FIELD, newLastDay);
        seleniumHandler.setComboBoxValue(OffDayDialog.OFFDAY_TYPE_FIELD, newType.name());

        // Confirm the edit
        closeDialog(OffDayDialog.CONFIRM_BUTTON);
        seleniumHandler.wait(300);

        // Verify updated record exists with new values
        verifyOffDayRecordExists(newFirstDay, newLastDay, newType);

        // If first dates are different, verify old record is gone
        if (!originalFirstDay.equals(newFirstDay)) {
            verifyOffDayRecordNotExists(originalFirstDay, originalLastDay, originalType);
        }
    }

    /**
     * Finds the ID of an off day record based on its first day.
     *
     * @param firstDay the first day of the record to find
     * @return the ID of the found record
     */
    private String findOffDayRecordId(LocalDate firstDay) {
        String firstDayStr = firstDay.format(dateFormatter);

        // Find the span with the start date text
        WebElement startDateSpan = seleniumHandler.findElement(
                By.xpath("//span[contains(@id,'" + OffDayListView.OFFDAY_GRID_START_DATE_PREFIX + "') and text()='" + firstDayStr + "']"));

        // Extract the ID from the span's id attribute
        String fullId = startDateSpan.getAttribute("id");
        return fullId.substring(OffDayListView.OFFDAY_GRID_START_DATE_PREFIX.length());
    }

    // Helper methods

    /**
     * Navigates to the OffDayListView for a specific user.
     * <p>
     * Opens the off day list URL directly, logs in if needed, and waits for the page to load
     * by checking for the presence of the page title element.
     *
     * @param recordingFolderName The folder name for recording the test
     * @param testName            The name of the test for recording
     * @param username            The username for which to view off days (optional, uses current user if null)
     */
    public void switchToOffDayListView(String recordingFolderName, String testName, String username) {
        //- Check if we need to log in
        if (!seleniumHandler.getCurrentUrl().contains("/ui/")) {
            seleniumHandler.getAndCheck("http://localhost:" + port + "/ui/" + LoginView.ROUTE);
            seleniumHandler.startRecording(recordingFolderName, testName);
            seleniumHandler.setLoginUser("admin-user");
            seleniumHandler.setLoginPassword("test-password");
            seleniumHandler.loginSubmit();
            seleniumHandler.waitUntil(ExpectedConditions.elementToBeClickable(By.id(ProductListView.PRODUCT_LIST_PAGE_TITLE)));
        }

        // Navigate to the off day view for the specific user
        String url = "http://localhost:" + port + "/ui/" + OffDayListView.ROUTE + (username != null ? "/" + username : "");
        seleniumHandler.getAndCheck(url);
        seleniumHandler.waitUntil(ExpectedConditions.elementToBeClickable(By.id(OffDayListView.OFFDAY_LIST_PAGE_TITLE)));
    }

    /**
     * Tests creating overlapping off day records.
     * <p>
     * Creates a first off day record, then attempts to create a second off day
     * with a date range that overlaps with the first. Verifies behavior according
     * to business rules (either success or validation error).
     *
     * @param firstRecord1Day first day of the first record
     * @param lastRecord1Day  last day of the first record
     * @param firstRecord2Day first day of the second record (overlapping)
     * @param lastRecord2Day  last day of the second record (overlapping)
     * @param type            type of off day
     * @param shouldSucceed   whether the creation of the second record should succeed
     */
    public void testOverlappingOffDays(LocalDate firstRecord1Day, LocalDate lastRecord1Day,
                                       LocalDate firstRecord2Day, LocalDate lastRecord2Day,
                                       OffDayType type, boolean shouldSucceed) {
        // Create the first record
        createOffDayConfirm(firstRecord1Day, lastRecord1Day, type);

        // Try to create a second overlapping record
        seleniumHandler.click(OffDayListView.CREATE_OFFDAY_BUTTON);
        seleniumHandler.setDatePickerValue(OffDayDialog.OFFDAY_START_DATE_FIELD, firstRecord2Day);
        seleniumHandler.setDatePickerValue(OffDayDialog.OFFDAY_END_DATE_FIELD, lastRecord2Day);
        seleniumHandler.setComboBoxValue(OffDayDialog.OFFDAY_TYPE_FIELD, type.name());
        closeDialog(OffDayDialog.CONFIRM_BUTTON);

        if (shouldSucceed) {
            // Verify the second record was created
            verifyOffDayRecordExists(firstRecord2Day, lastRecord2Day, type);
        } else {
            // Verify validation error occurs
            seleniumHandler.waitUntil(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("vaadin-notification-card")));

            // Cancel the dialog
            closeDialog(OffDayDialog.CANCEL_BUTTON);

            // Verify the second record wasn't created
            verifyOffDayRecordNotExists(firstRecord2Day, lastRecord2Day, type);
        }
    }

    /**
     * Verifies that an off day record exists with the specified details.
     *
     * @param firstDay the first day of the record
     * @param lastDay  the last day of the record
     * @param type     the type of the off day
     */
    private void verifyOffDayRecordExists(LocalDate firstDay, LocalDate lastDay, OffDayType type) {
        String firstDayStr = firstDay.format(dateFormatter);
        String lastDayStr  = lastDay.format(dateFormatter);

        // Find the record ID based on first day
        String id = findOffDayRecordId(firstDay);

        // Verify the last day and type match
        WebElement lastDayElement = seleniumHandler.findElement(By.id(OffDayListView.OFFDAY_GRID_END_DATE_PREFIX + id));
        assertEquals(lastDayStr, lastDayElement.getText(), "Last day doesn't match expected value");

        WebElement typeElement = seleniumHandler.findElement(By.id(OffDayListView.OFFDAY_GRID_TYPE_PREFIX + id));
        assertEquals(type.name(), typeElement.getText(), "Type doesn't match expected value");
    }

    /**
     * Verifies that an off day record does not exist with the specified details.
     *
     * @param firstDay the first day of the record
     * @param lastDay  the last day of the record
     * @param type     the type of the off day
     */
    private void verifyOffDayRecordNotExists(LocalDate firstDay, LocalDate lastDay, OffDayType type) {
        String firstDayStr = firstDay.format(dateFormatter);

        // Check if any element with the start date exists
        seleniumHandler.ensureIsNotInList(OffDayListView.OFFDAY_GRID_START_DATE_PREFIX, firstDayStr);

//        boolean exists = seleniumHandler.elementExists(
//                By.xpath("//span[contains(@id,'" + OffDayListView.OFFDAY_GRID_START_DATE_PREFIX + "') and text()='" + firstDayStr + "']"));
//
//        assertFalse(exists, "Off day record with first day " + firstDayStr + " should not exist");
    }
}
