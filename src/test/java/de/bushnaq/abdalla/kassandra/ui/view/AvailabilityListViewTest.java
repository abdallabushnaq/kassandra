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

import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.util.AvailabilityListViewTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;

/**
 * Integration test for the AvailabilityListView UI component.
 * Tests CRUD (Create, Read, Update, Delete) operations for availability records in the UI.
 * <p>
 * These tests verify that:
 * - Availability records can be created with appropriate details
 * - Created records appear correctly in the list
 * - Records can be edited and changes are reflected in the UI
 * - Records can be deleted from the system
 * - Cancellation of operations works as expected
 * - Validation rules are enforced (unique start dates, valid percentage range)
 * <p>
 * The tests account for the fact that each user already has an initial availability record
 * for the current date when they're created.
 * <p>
 * The tests use {@link AvailabilityListViewTester} to interact with the UI elements
 * and verify the expected behavior.
 */
@Tag("IntegrationUiTest")
@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=${test.server.port:0}",
                "spring.profiles.active=test",
                "spring.security.basic.enabled=false"// Disable basic authentication for these tests
        }
)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class AvailabilityListViewTest extends AbstractKeycloakUiTestUtil {
    @Autowired
    private       AvailabilityListViewTester availabilityListViewTester;
    private final int                        availabilityPercent    = 80;
    private final LocalDate                  initialDate            = LocalDate.now();
    private final int                        initialPercent         = 100;
    private final int                        invalidHighPercent     = 160; // Above 150% limit
    private final int                        invalidLowPercent      = -10; // Below 0% limit
    private final int                        newAvailabilityPercent = 50;
    private final LocalDate                  newStartDate           = LocalDate.of(2025, 8, 1);
    private final int                        secondAvailPercent     = 75;
    private final LocalDate                  secondStartDate        = LocalDate.of(2025, 7, 1);
    @Autowired
    private       HumanizedSeleniumHandler   seleniumHandler;
    private final LocalDate                  startDate              = LocalDate.of(2025, 6, 1);
    private final String                     testUsername           = "availability-test-user";

    @BeforeEach
    public void setupTest(TestInfo testInfo) throws Exception {
        availabilityListViewTester.switchToAvailabilityListView(
                testInfo.getTestClass().get().getSimpleName(),
                generateTestCaseName(testInfo),
                testUsername);
    }

    /**
     * Tests that users cannot delete their only availability record.
     * <p>
     * Verifies that the delete button for the initial availability record
     * is disabled, preventing users from deleting their only record.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCannotDeleteOnlyAvailability() {
        // Verify user cannot delete their only availability record (the initial one)
        availabilityListViewTester.verifyCannotDeleteOnlyAvailability(initialDate);
    }

    /**
     * Tests the behavior when creating an availability record but canceling the operation.
     * <p>
     * Verifies that when a user clicks the create availability button, enters data, and then
     * cancels the operation, no record is created in the list.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateCancel() {
        availabilityListViewTester.createAvailabilityCancel(startDate, availabilityPercent);
    }

    /**
     * Tests the behavior when successfully creating an availability record.
     * <p>
     * Verifies that when a user clicks the create availability button, enters all required fields,
     * and confirms the creation, the record appears in the list with the correct values.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateConfirm() {
        availabilityListViewTester.createAvailabilityConfirm(startDate, availabilityPercent);
    }

    /**
     * Tests the behavior when attempting to delete an availability record but canceling the operation.
     * <p>
     * Creates a record, then attempts to delete it but cancels the confirmation dialog.
     * Verifies that the record remains in the list.
     * <p>
     * Note: This test assumes the user already has the initial default availability record.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDeleteCancel() {
        // Create a second availability record
        availabilityListViewTester.createAvailabilityConfirm(startDate, availabilityPercent);
        // Try to delete but cancel
        availabilityListViewTester.deleteAvailabilityCancel(startDate);
    }

    /**
     * Tests the behavior when successfully deleting an availability record.
     * <p>
     * Creates a record, then deletes it by confirming the deletion in the confirmation dialog.
     * Verifies that the record is removed from the list.
     * <p>
     * Note: This test assumes the user already has the initial default availability record.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDeleteConfirm() {
        // Create a second availability record
        availabilityListViewTester.createAvailabilityConfirm(startDate, availabilityPercent);
        // Delete the newly created record
        availabilityListViewTester.deleteAvailabilityConfirm(startDate);
    }

    /**
     * Tests that validation prevents creation of duplicate availability records with the same start date.
     * <p>
     * Attempts to create a record with the same start date as the initial record but different
     * availability percentage. Verifies that an error is shown and the duplicate is not created.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDuplicateStartDate() {
        // Try to create a duplicate with the same start date as the initial record
        availabilityListViewTester.createDuplicateDateAvailability(initialDate, newAvailabilityPercent);
    }

    /**
     * Tests the behavior when attempting to edit an availability record but canceling the operation.
     * <p>
     * Creates a record, attempts to edit all its fields (start date, availability percentage),
     * but cancels the edit dialog.
     * Verifies that the original record details remain unchanged and the new values are not applied.
     * <p>
     * Note: This test uses the initial availability record that exists when the user is created.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditCancel() {
        // Edit initial record but cancel
        availabilityListViewTester.editAvailabilityCancel(initialDate, newStartDate, initialPercent, newAvailabilityPercent);
    }

    /**
     * Tests the behavior when successfully editing an availability record.
     * <p>
     * Edits all fields of the initial availability record and confirms the edit.
     * Verifies that the record with the new values appears in the list
     * and the old values are no longer present.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditConfirm() {
        // Edit initial record and confirm
        availabilityListViewTester.editAvailabilityConfirm(initialDate, newStartDate, newAvailabilityPercent);
    }

    /**
     * Tests that validation prevents creation of availability records with invalid percentage values.
     * <p>
     * Attempts to create records with percentage values outside the allowed range (0% to 150%).
     * Verifies that errors are shown and the invalid records are not created.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testInvalidPercentage() {
        // Try to create record with too high percentage
        availabilityListViewTester.createInvalidPercentageAvailability(startDate, invalidHighPercent);

        // Try to create record with negative percentage
        availabilityListViewTester.createInvalidPercentageAvailability(newStartDate, invalidLowPercent);
    }
}
