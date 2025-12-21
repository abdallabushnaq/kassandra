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

package de.bushnaq.abdalla.kassandra.ui.view;

import de.bushnaq.abdalla.kassandra.ui.util.AbstractUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.util.LocationListViewTester;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Integration test for the LocationListView UI component.
 * Tests CRUD (Create, Read, Update, Delete) operations for location records in the UI.
 * <p>
 * These tests verify that:
 * - Location records can be created with appropriate details
 * - Created records appear correctly in the list
 * - Records can be edited and changes are reflected in the UI
 * - Records can be deleted from the system
 * - Cancellation of operations works as expected
 * - Validation rules are enforced (unique start dates)
 * <p>
 * The tests account for the fact that each user already has an initial location record
 * for the current date when they're created.
 * <p>
 * The tests use {@link LocationListViewTester} to interact with the UI elements
 * and verify the expected behavior.
 */
@Tag("IntegrationUiTest")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@Slf4j
public class LocationListViewTest extends AbstractUiTestUtil {
    private final String                   country        = "United States (US)";  // United States
    private final String                   initialCountry = "Germany (DE)";  // Germany
    private final LocalDate                initialDate    = LocalDate.now();
    private final String                   initialState   = "North Rhine-Westphalia (nw)";  // North Rhine-Westphalia
    @Autowired
    private       LocationListViewTester   locationListViewTester;
    private final String                   newCountry     = "United Kingdom (GB)";  // United Kingdom
    private final LocalDate                newStartDate   = LocalDate.of(2025, 8, 1);
    private final String                   newState       = "England (eng)"; // England
    @Autowired
    private       HumanizedSeleniumHandler seleniumHandler;
    // Test data for the first new location record
    private final LocalDate                startDate      = LocalDate.of(2025, 6, 1);
    private final String                   state          = "California (ca)";  // California
    private final String                   testUsername   = "location-test-user";

    @BeforeEach
    public void setupTest(TestInfo testInfo) {
        locationListViewTester.switchToLocationListView(
                testInfo.getTestClass().get().getSimpleName(),
                generateTestCaseName(testInfo),
                testUsername);
    }

    /**
     * Tests that users cannot delete their only location record.
     * <p>
     * Verifies that the delete button for the initial location record
     * is disabled, preventing users from deleting their only record.
     */
    @Test
    public void testCannotDeleteOnlyLocation() {
        // Verify user cannot delete their only location record (the initial one)
        locationListViewTester.verifyCannotDeleteOnlyLocation(initialDate);
    }

    /**
     * Tests the behavior when creating a location record but canceling the operation.
     * <p>
     * Verifies that when a user clicks the create location button, enters data, and then
     * cancels the operation, no record is created in the list.
     */
    @Test
    public void testCreateCancel() {
        locationListViewTester.createLocationCancel(startDate, country, state);
    }

    /**
     * Tests the behavior when successfully creating a location record.
     * <p>
     * Verifies that when a user clicks the create location button, enters all required fields,
     * and confirms the creation, the record appears in the list with the correct values.
     */
    @Test
    public void testCreateConfirm() {
        locationListViewTester.createLocationConfirm(startDate, country, state);
    }

    /**
     * Tests the behavior when attempting to delete a location record but canceling the operation.
     * <p>
     * Creates a record, then attempts to delete it but cancels the confirmation dialog.
     * Verifies that the record remains in the list.
     * <p>
     * Note: This test assumes the user already has the initial default location record.
     */
    @Test
    public void testDeleteCancel() {
        // Create a second location record
        locationListViewTester.createLocationConfirm(startDate, country, state);
        // Try to delete but cancel
        locationListViewTester.deleteLocationCancel(startDate);
    }

    /**
     * Tests the behavior when successfully deleting a location record.
     * <p>
     * Creates a record, then deletes it by confirming the deletion in the confirmation dialog.
     * Verifies that the record is removed from the list.
     * <p>
     * Note: This test assumes the user already has the initial default location record.
     */
    @Test
    public void testDeleteConfirm() {
        // Create a second location record
        locationListViewTester.createLocationConfirm(startDate, country, state);
        // Delete the newly created record
        locationListViewTester.deleteLocationConfirm(startDate);
    }

    /**
     * Tests that validation prevents creation of duplicate location records with the same start date.
     * <p>
     * Attempts to create a record with the same start date as the initial record but different
     * country and state. Verifies that an error is shown and the duplicate is not created.
     */
    @Test
    public void testDuplicateStartDate() {
        // Try to create a duplicate with the same start date as the initial record
        locationListViewTester.createDuplicateDateLocation(initialDate, newCountry, newState);
    }

    /**
     * Tests the behavior when attempting to edit a location record but canceling the operation.
     * <p>
     * Creates a record, attempts to edit all its fields (start date, country, state),
     * but cancels the edit dialog.
     * Verifies that the original record details remain unchanged and the new values are not applied.
     * <p>
     * Note: This test uses the initial location record that exists when the user is created.
     */
    @Test
    public void testEditCancel() {
        // Edit initial record but cancel
        locationListViewTester.editLocationCancel(
                initialDate, newStartDate,
                initialCountry, newCountry,
                initialState, newState);
    }

    /**
     * Tests the behavior when successfully editing a location record.
     * <p>
     * Edits all fields of the initial location record and confirms the edit.
     * Verifies that the record with the new values appears in the list
     * and the old values are no longer present.
     */
    @Test
    public void testEditConfirm() {
        // Edit initial record and confirm
        locationListViewTester.editLocationConfirm(initialDate, newStartDate, newCountry, newState);
    }
}
