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

import de.bushnaq.abdalla.kassandra.dto.OffDayType;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.util.OffDayListViewTester;
import lombok.extern.slf4j.Slf4j;
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
 * Integration test for the OffDayListView UI component.
 * Tests CRUD (Create, Read, Update, Delete) operations for off day records in the UI.
 * <p>
 * These tests verify that:
 * - Off day records can be created with appropriate details (first day, last day, type)
 * - Created records appear correctly in the list
 * - Records can be edited and changes are reflected in the UI
 * - Records can be deleted from the system
 * - Cancellation of operations works as expected
 * - Validation rules are enforced (valid date ranges, overlapping periods)
 * <p>
 * The tests use {@link OffDayListViewTester} to interact with the UI elements
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
//@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class OffDayListViewTest extends AbstractKeycloakUiTestUtil {
    // Test data for the first off day record
    private final LocalDate                firstDayRecord1  = LocalDate.of(2025, 8, 4);
    // Test data for the second off day record
    private final LocalDate                firstDayRecord2  = LocalDate.of(2025, 9, 15);
    // Test data for invalid date range
    private final LocalDate                invalidFirstDay  = LocalDate.of(2025, 11, 10);
    private final LocalDate                invalidLastDay   = LocalDate.of(2025, 11, 5);  // Before first day
    private final LocalDate                lastDayRecord1   = LocalDate.of(2025, 8, 8);
    private final LocalDate                lastDayRecord2   = LocalDate.of(2025, 9, 19);
    // Test data for editing
    private final LocalDate                newFirstDay      = LocalDate.of(2025, 10, 6);
    private final LocalDate                newLastDay       = LocalDate.of(2025, 10, 10);
    private final OffDayType               newType          = OffDayType.TRIP;
    @Autowired
    private       OffDayListViewTester     offDayListViewTester;
    // Test data for overlapping off days
    private final LocalDate                overlapFirstDay1 = LocalDate.of(2025, 12, 1);
    private final LocalDate                overlapFirstDay2 = LocalDate.of(2025, 12, 2);  // Overlaps with first record
    private final LocalDate                overlapLastDay1  = LocalDate.of(2025, 12, 3);
    private final LocalDate                overlapLastDay2  = LocalDate.of(2025, 12, 5);
    @Autowired
    private       HumanizedSeleniumHandler seleniumHandler;
    // Test username
    private final String                   testUsername     = "offday-test-user";
    private final OffDayType               typeRecord1      = OffDayType.VACATION;
    private final OffDayType               typeRecord2      = OffDayType.SICK;

    @BeforeEach
    public void setupTest(TestInfo testInfo) throws Exception {
        offDayListViewTester.switchToOffDayListView(
                testInfo.getTestClass().get().getSimpleName(),
                generateTestCaseName(testInfo),
                testUsername);
    }

    /**
     * Tests the behavior when creating an off day record but canceling the operation.
     * <p>
     * Verifies that when a user clicks the create off day button, enters data, and then
     * cancels the operation, no record is created in the list.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateCancel() {
        offDayListViewTester.createOffDayCancel(firstDayRecord1, lastDayRecord1, typeRecord1);
    }

    /**
     * Tests the behavior when successfully creating an off day record.
     * <p>
     * Verifies that when a user clicks the create off day button, enters all required fields,
     * and confirms the creation, the record appears in the list with the correct values.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateConfirm() {
        offDayListViewTester.createOffDayConfirm(firstDayRecord1, lastDayRecord1, typeRecord1);
    }

    /**
     * Tests the behavior when attempting to delete an off day record but canceling the operation.
     * <p>
     * Creates a record, then attempts to delete it but cancels the confirmation dialog.
     * Verifies that the record remains in the list.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDeleteCancel() {
        // Create a record
        offDayListViewTester.createOffDayConfirm(firstDayRecord1, lastDayRecord1, typeRecord1);

        // Try to delete but cancel
        offDayListViewTester.deleteOffDayCancel(firstDayRecord1, lastDayRecord1, typeRecord1);
    }

    /**
     * Tests the behavior when successfully deleting an off day record.
     * <p>
     * Creates a record, then deletes it by confirming the deletion in the confirmation dialog.
     * Verifies that the record is removed from the list.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDeleteConfirm() {
        // Create a record
        offDayListViewTester.createOffDayConfirm(firstDayRecord1, lastDayRecord1, typeRecord1);

        // Delete the record
        offDayListViewTester.deleteOffDayConfirm(firstDayRecord1, lastDayRecord1, typeRecord1);
    }

    /**
     * Tests creation of off days with different types.
     * <p>
     * Creates off day records with each of the possible off day types.
     * Verifies that each type can be correctly created and displayed.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDifferentOffDayTypes() {
        // Test each type with slightly different dates
        offDayListViewTester.createOffDayConfirm(
                LocalDate.of(2025, 7, 1),
                LocalDate.of(2025, 7, 4),
                OffDayType.VACATION);

        offDayListViewTester.createOffDayConfirm(
                LocalDate.of(2025, 7, 7),
                LocalDate.of(2025, 7, 9),
                OffDayType.SICK);

        offDayListViewTester.createOffDayConfirm(
                LocalDate.of(2025, 7, 21),
                LocalDate.of(2025, 7, 21),
                OffDayType.HOLIDAY);

        offDayListViewTester.createOffDayConfirm(
                LocalDate.of(2025, 7, 28),
                LocalDate.of(2025, 7, 31),
                OffDayType.TRIP);
    }

    /**
     * Tests the behavior when attempting to edit an off day record but canceling the operation.
     * <p>
     * Creates a record, attempts to edit all its fields (first day, last day, type),
     * but cancels the edit dialog.
     * Verifies that the original record details remain unchanged and the new values are not applied.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditCancel() {
        // Create an initial record
        offDayListViewTester.createOffDayConfirm(firstDayRecord1, lastDayRecord1, typeRecord1);

        // Edit the record but cancel
        offDayListViewTester.editOffDayCancel(
                firstDayRecord1, newFirstDay,
                lastDayRecord1, newLastDay,
                typeRecord1, newType);
    }

    /**
     * Tests the behavior when successfully editing an off day record.
     * <p>
     * Creates a record, then edits all its fields (first day, last day, type) and confirms the edit.
     * Verifies that the record with the new values appears in the list
     * and the old values are no longer present.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditConfirm() {
        // Create an initial record
        offDayListViewTester.createOffDayConfirm(firstDayRecord1, lastDayRecord1, typeRecord1);

        // Edit the record and confirm
        offDayListViewTester.editOffDayConfirm(
                firstDayRecord1, newFirstDay,
                lastDayRecord1, newLastDay,
                typeRecord1, newType);
    }

    /**
     * Tests that validation prevents creation of off day records with invalid date ranges.
     * <p>
     * Attempts to create a record with a first day that is after the last day.
     * Verifies that an error is shown and the invalid record is not created.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testInvalidDateRange() {
        offDayListViewTester.createInvalidDateRangeOffDay(invalidFirstDay, invalidLastDay, typeRecord1);
    }

    /**
     * Tests the creation of multiple off day records.
     * <p>
     * Creates two separate off day records with different date ranges and types.
     * Verifies that both records appear correctly in the list.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testMultipleOffDays() {
        // Create first record
        offDayListViewTester.createOffDayConfirm(firstDayRecord1, lastDayRecord1, typeRecord1);

        // Create second record
        offDayListViewTester.createOffDayConfirm(firstDayRecord2, lastDayRecord2, typeRecord2);
    }

    /**
     * Tests behavior with overlapping off day periods.
     * <p>
     * Creates an off day record, then attempts to create another with a date range
     * that overlaps with the first. Based on business rules, this may or may not be allowed.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testOverlappingOffDays() {
        // The last parameter indicates whether the second creation should succeed
        // This depends on your business rules - if overlaps are allowed, set to true
        offDayListViewTester.testOverlappingOffDays(
                overlapFirstDay1, overlapLastDay1,
                overlapFirstDay2, overlapLastDay2,
                typeRecord1, false);
    }
}
