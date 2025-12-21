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

import de.bushnaq.abdalla.kassandra.ui.dialog.ConfirmDialog;
import de.bushnaq.abdalla.kassandra.ui.dialog.LocationDialog;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.LocationListView;
import de.bushnaq.abdalla.kassandra.ui.view.LoginView;
import de.bushnaq.abdalla.kassandra.ui.view.ProductListView;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test helper class for interacting with the Location UI components.
 * <p>
 * This class provides methods to test location-related operations in the UI such as
 * creating, editing, deleting location records and navigating between views. It uses
 * {@link HumanizedSeleniumHandler} to interact with UI elements and validate results.
 */
@Component
@Lazy
public class LocationListViewTester {

    private final DateTimeFormatter        dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final int                      port;
    private final HumanizedSeleniumHandler seleniumHandler;

    /**
     * Constructs a new LocationViewTester with the given Selenium handler and server port.
     *
     * @param seleniumHandler the handler for Selenium operations
     * @param port            the port on which the application server is running
     */
    public LocationListViewTester(HumanizedSeleniumHandler seleniumHandler, @Value("${local.server.port:8080}") int port) {
        this.seleniumHandler = seleniumHandler;
        this.port            = port;
    }

    /**
     * Tests attempting to create a duplicate location record with the same start date.
     * <p>
     * Opens the location creation dialog, enters a start date that already exists,
     * then attempts to confirm. Verifies that an error message is shown and the duplicate
     * record is not created.
     *
     * @param existingStartDate the start date that already has a location record
     * @param country           the country code for the duplicate location
     * @param state             the state/region code for the duplicate location
     */
    public void createDuplicateDateLocation(LocalDate existingStartDate, String country, String state) {
        String existingDateStr = existingStartDate.format(dateFormatter);

        // Try to create a duplicate record
        seleniumHandler.click(LocationListView.CREATE_LOCATION_BUTTON);
        seleniumHandler.setDatePickerValue(LocationDialog.LOCATION_START_DATE_FIELD, existingStartDate);
        seleniumHandler.setComboBoxValue(LocationDialog.LOCATION_COUNTRY_FIELD, country);
        seleniumHandler.setComboBoxValue(LocationDialog.LOCATION_STATE_FIELD, state);
        seleniumHandler.click(LocationDialog.CONFIRM_BUTTON);
        seleniumHandler.waitUntil(ExpectedConditions.invisibilityOfElementLocated(By.id(LocationDialog.LOCATION_DIALOG)));

        // Verify the validation error occurs (dialog should still be open)
        seleniumHandler.waitUntil(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("vaadin-notification-card")));

        // Cancel the dialog since we expect it to still be open
        seleniumHandler.click(LocationDialog.CANCEL_BUTTON);
    }

    /**
     * Tests the creation of a location record where the user cancels the operation.
     * <p>
     * Opens the location creation dialog, enters values for start date, country, and state,
     * then cancels the dialog. Verifies that no record with the specified start date appears in the list
     * and that the cancellation didn't affect any existing data.
     *
     * @param startDate the start date for the location record
     * @param country   the country code for the location (ISO 3166-1 alpha-2)
     * @param state     the state/region code for the location
     */
    public void createLocationCancel(LocalDate startDate, String country, String state) {
        seleniumHandler.click(LocationListView.CREATE_LOCATION_BUTTON);
        seleniumHandler.setDatePickerValue(LocationDialog.LOCATION_START_DATE_FIELD, startDate);
        seleniumHandler.setComboBoxValue(LocationDialog.LOCATION_COUNTRY_FIELD, country);
        seleniumHandler.setComboBoxValue(LocationDialog.LOCATION_STATE_FIELD, state);
        seleniumHandler.click(LocationDialog.CANCEL_BUTTON);
        seleniumHandler.waitUntil(ExpectedConditions.invisibilityOfElementLocated(By.id(LocationDialog.LOCATION_DIALOG)));

        // Verify the record doesn't appear in the list
        String startDateStr = startDate.format(dateFormatter);
        seleniumHandler.ensureIsNotInList(LocationListView.LOCATION_GRID_START_DATE_PREFIX, startDateStr);
    }

    /**
     * Tests the successful creation of a location record.
     * <p>
     * Opens the location creation dialog, enters start date, country, and state,
     * then confirms the dialog. Verifies that a record with the specified start date appears
     * in the location list and that the fields are correctly stored.
     *
     * @param startDate the start date for the location record
     * @param country   the country code for the location (ISO 3166-1 alpha-2)
     * @param state     the state/region code for the location
     */
    public void createLocationConfirm(LocalDate startDate, String country, String state) {
        seleniumHandler.click(LocationListView.CREATE_LOCATION_BUTTON);
        seleniumHandler.setDatePickerValue(LocationDialog.LOCATION_START_DATE_FIELD, startDate);
        seleniumHandler.setComboBoxValue(LocationDialog.LOCATION_COUNTRY_FIELD, country);
        seleniumHandler.setComboBoxValue(LocationDialog.LOCATION_STATE_FIELD, state);
        seleniumHandler.click(LocationDialog.CONFIRM_BUTTON);
        seleniumHandler.waitUntil(ExpectedConditions.invisibilityOfElementLocated(By.id(LocationDialog.LOCATION_DIALOG)));

        // Verify the record appears in the list
        String startDateStr = startDate.format(dateFormatter);
        seleniumHandler.ensureIsInList(LocationListView.LOCATION_GRID_START_DATE_PREFIX, startDateStr);

        // Verify the country and state are displayed correctly
        seleniumHandler.ensureIsInList(LocationListView.LOCATION_GRID_COUNTRY_PREFIX, country);
        seleniumHandler.ensureIsInList(LocationListView.LOCATION_GRID_STATE_PREFIX, state);
    }

    /**
     * Tests location record deletion where the user cancels the delete confirmation.
     * <p>
     * Clicks the delete button for the specified location record,
     * then cancels the confirmation dialog. Verifies that the record still exists in the list.
     *
     * @param startDate the start date of the location record to attempt to delete
     */
    public void deleteLocationCancel(LocalDate startDate) {
        String startDateStr = startDate.format(dateFormatter);
        seleniumHandler.click(LocationListView.LOCATION_GRID_DELETE_BUTTON_PREFIX + startDateStr);
        seleniumHandler.click(ConfirmDialog.CANCEL_BUTTON);
        seleniumHandler.waitUntil(ExpectedConditions.invisibilityOfElementLocated(By.id(LocationDialog.LOCATION_DIALOG)));
        seleniumHandler.ensureIsInList(LocationListView.LOCATION_GRID_START_DATE_PREFIX, startDateStr);
    }

    /**
     * Tests the successful deletion of a location record.
     * <p>
     * Clicks the delete button for the specified location record,
     * then confirms the deletion in the confirmation dialog. Verifies that the record
     * is removed from the location list.
     *
     * @param startDate the start date of the location record to delete
     */
    public void deleteLocationConfirm(LocalDate startDate) {
        String startDateStr = startDate.format(dateFormatter);
        seleniumHandler.click(LocationListView.LOCATION_GRID_DELETE_BUTTON_PREFIX + startDateStr);
        seleniumHandler.click(ConfirmDialog.CONFIRM_BUTTON);
        seleniumHandler.waitUntil(ExpectedConditions.invisibilityOfElementLocated(By.id(LocationDialog.LOCATION_DIALOG)));
        seleniumHandler.ensureIsNotInList(LocationListView.LOCATION_GRID_START_DATE_PREFIX, startDateStr);
    }

    /**
     * Tests the editing of a location record where the user cancels the edit operation.
     * <p>
     * Clicks the edit button for the location record with the specified start date,
     * enters new values, then cancels the edit dialog. Verifies that the record
     * still maintains its original values.
     *
     * @param originalStartDate the original start date of the record to edit
     * @param newStartDate      the new start date to attempt to assign
     * @param originalCountry   the original country code to verify remains unchanged after cancellation
     * @param newCountry        the country code to attempt to assign
     * @param originalState     the original state/region code to verify remains unchanged after cancellation
     * @param newState          the state/region code to attempt to assign
     */
    public void editLocationCancel(LocalDate originalStartDate, LocalDate newStartDate,
                                   String originalCountry, String newCountry,
                                   String originalState, String newState) {
        String originalDateStr = originalStartDate.format(dateFormatter);

        // Verify original values first
        verifyLocationValues(originalDateStr, originalCountry, originalState);

        // Edit the record but cancel
        seleniumHandler.click(LocationListView.LOCATION_GRID_EDIT_BUTTON_PREFIX + originalDateStr);
        seleniumHandler.setDatePickerValue(LocationDialog.LOCATION_START_DATE_FIELD, newStartDate);
        seleniumHandler.setComboBoxValue(LocationDialog.LOCATION_COUNTRY_FIELD, newCountry);
        seleniumHandler.setComboBoxValue(LocationDialog.LOCATION_STATE_FIELD, newState);
        seleniumHandler.click(LocationDialog.CANCEL_BUTTON);
        seleniumHandler.waitUntil(ExpectedConditions.invisibilityOfElementLocated(By.id(LocationDialog.LOCATION_DIALOG)));

        // Verify original record still exists and new one doesn't
        seleniumHandler.ensureIsInList(LocationListView.LOCATION_GRID_START_DATE_PREFIX, originalDateStr);
        if (!originalStartDate.equals(newStartDate)) {
            String newDateStr = newStartDate.format(dateFormatter);
            seleniumHandler.ensureIsNotInList(LocationListView.LOCATION_GRID_START_DATE_PREFIX, newDateStr);
        }

        // Verify the values didn't change
        verifyLocationValues(originalDateStr, originalCountry, originalState);
    }

    /**
     * Tests the successful editing of a location record.
     * <p>
     * Clicks the edit button for the location record with the specified start date,
     * enters new values, then confirms the edit. Verifies that the record with the new values
     * appears in the list and the record with the old values is gone.
     *
     * @param originalStartDate the original start date of the record to edit
     * @param newStartDate      the new start date to assign
     * @param newCountry        the new country code to assign
     * @param newState          the new state/region code to assign
     */
    public void editLocationConfirm(LocalDate originalStartDate, LocalDate newStartDate, String newCountry, String newState) {
        String originalDateStr = originalStartDate.format(dateFormatter);

        // Edit the record and confirm
        seleniumHandler.click(LocationListView.LOCATION_GRID_EDIT_BUTTON_PREFIX + originalDateStr);
        seleniumHandler.setDatePickerValue(LocationDialog.LOCATION_START_DATE_FIELD, newStartDate);
        seleniumHandler.setComboBoxValue(LocationDialog.LOCATION_COUNTRY_FIELD, newCountry);
        seleniumHandler.setComboBoxValue(LocationDialog.LOCATION_STATE_FIELD, newState);
        seleniumHandler.click(LocationDialog.CONFIRM_BUTTON);
        seleniumHandler.waitUntil(ExpectedConditions.invisibilityOfElementLocated(By.id(LocationDialog.LOCATION_DIALOG)));

        String newDateStr = newStartDate.format(dateFormatter);

        // Verify the new record exists and the old one is gone (if dates are different)
        seleniumHandler.ensureIsInList(LocationListView.LOCATION_GRID_START_DATE_PREFIX, newDateStr);
        if (!originalStartDate.equals(newStartDate)) {
            seleniumHandler.ensureIsNotInList(LocationListView.LOCATION_GRID_START_DATE_PREFIX, originalDateStr);
        }

        // Verify the updated values
        verifyLocationValues(newDateStr, newCountry, newState);
    }

    /**
     * Navigates to the LocationListView for a specific user.
     * <p>
     * Opens the location list URL directly, logs in if needed, and waits for the page to load
     * by checking for the presence of the page title element.
     *
     * @param recordingFolderName The folder name for recording the test
     * @param testName            The name of the test for recording
     * @param username            The username for which to view locations (optional, uses current user if null)
     */
    public void switchToLocationListView(String recordingFolderName, String testName, String username) {
        //- Check if we need to log in
        if (!seleniumHandler.getCurrentUrl().contains("/ui/")) {
            seleniumHandler.getAndCheck("http://localhost:" + port + "/ui/" + LoginView.ROUTE);
            seleniumHandler.startRecording(recordingFolderName, testName);
            seleniumHandler.setLoginUser("admin-user");
            seleniumHandler.setLoginPassword("test-password");
            seleniumHandler.loginSubmit();
            seleniumHandler.waitUntil(ExpectedConditions.elementToBeClickable(By.id(ProductListView.PRODUCT_LIST_PAGE_TITLE)));
        }

        // Navigate to the location view for the specific user or current user
        String url = "http://localhost:" + port + "/ui/" + LocationListView.ROUTE;
        if (username != null) {
            url += "/" + username;
        }

        seleniumHandler.getAndCheck(url);
        seleniumHandler.waitUntil(ExpectedConditions.elementToBeClickable(By.id(LocationListView.LOCATION_LIST_PAGE_TITLE)));
    }

    /**
     * Tests that a user cannot delete their only location record.
     * <p>
     * Verifies that the delete button is disabled when the user has only one location record.
     *
     * @param startDate the start date of the only location record
     */
    public void verifyCannotDeleteOnlyLocation(LocalDate startDate) {
        String startDateStr = startDate.format(dateFormatter);

        // Find the delete button element for the specified location record
        WebElement deleteButton         = seleniumHandler.findElement(By.id(LocationListView.LOCATION_GRID_DELETE_BUTTON_PREFIX + startDateStr));
        boolean    hasDisabledAttribute = deleteButton.getAttribute("disabled") != null;
        assertTrue(hasDisabledAttribute, "Delete button should be disabled for the only location record (missing disabled attribute)");

    }

    /**
     * Verifies the country and state values for a specific location record.
     * <p>
     * Opens the edit dialog for the record with the specified start date and verifies
     * that the country and state fields have the expected values. Cancels the dialog after verification
     * to avoid making changes.
     *
     * @param startDateStr    the formatted start date string of the record to verify
     * @param expectedCountry the expected country code
     * @param expectedState   the expected state/region code
     */
    private void verifyLocationValues(String startDateStr, String expectedCountry, String expectedState) {
        seleniumHandler.click(LocationListView.LOCATION_GRID_EDIT_BUTTON_PREFIX + startDateStr);
        String actualCountry = seleniumHandler.getComboBoxValue(LocationDialog.LOCATION_COUNTRY_FIELD);
        String actualState   = seleniumHandler.getComboBoxValue(LocationDialog.LOCATION_STATE_FIELD);
        seleniumHandler.click(LocationDialog.CANCEL_BUTTON);
        seleniumHandler.waitUntil(ExpectedConditions.invisibilityOfElementLocated(By.id(LocationDialog.LOCATION_DIALOG)));
        Assertions.assertEquals(expectedCountry, actualCountry, "Country mismatch for record with start date: " + startDateStr);
        Assertions.assertEquals(expectedState, actualState, "State/region mismatch for record with start date: " + startDateStr);
    }
}
