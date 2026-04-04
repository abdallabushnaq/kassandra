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

package de.bushnaq.abdalla.kassandra.ui.view;

import de.bushnaq.abdalla.kassandra.config.DefaultEntitiesInitializer;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.UserWorkWeek;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.view.util.UserWorkWeekListViewTester;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;

/**
 * Integration test for the UserWorkWeekListView UI component.
 * Tests CRUD (Create, Read, Update, Delete) operations for work week assignments in the UI.
 * <p>
 * These tests verify that:
 * - Work week assignments can be created with appropriate details
 * - Created records appear correctly in the list
 * - Records can be edited and changes are reflected in the UI
 * - Records can be deleted from the system
 * - Cancellation of operations works as expected
 * - Validation rules are enforced (unique start dates)
 * <p>
 * The tests account for the fact that each user already has an initial work week assignment
 * (Western 5x8) for the date two years before the current date when they are created.
 * <p>
 * The tests use {@link UserWorkWeekListViewTester} to interact with the UI elements
 * and verify the expected behavior.
 */
@Tag("IntegrationUiTest")
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=${test.server.port:0}",
                "spring.security.basic.enabled=false" // Disable basic authentication for these tests
        }
)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class UserWorkWeekListViewTest extends AbstractKeycloakUiTestUtil {

    private       UserWorkWeek lastUserWorkWeek;
    private       String       lastUserWorkWeekName;
    private final LocalDate    newStartDate    = LocalDate.of(2025, 8, 1);
    /**
     * A second work week used as the target when testing edits and duplicates.
     */
    private final String       newWorkWeekName = DefaultEntitiesInitializer.WORK_WEEK_JEWISH_5X8;
    private final LocalDate    startDate       = LocalDate.of(2025, 6, 1);
    private final String       testUsername    = "christopher.paul@kassandra.org";
    @Autowired
    private UserWorkWeekListViewTester userWorkWeekListViewTester;
    /**
     * Work week used when creating a second assignment in the create/delete tests.
     */
    private final String       workWeekName    = DefaultEntitiesInitializer.WORK_WEEK_ISLAMIC_5X8;

    /**
     * Reads the initial work week assignment of the test user from the API
     * and stores it for use in the test methods.
     */
    private void read() {
        User paul = userApi.getByEmail("christopher.paul@kassandra.org").get();
        lastUserWorkWeek     = paul.getUserWorkWeeks().getFirst();
        lastUserWorkWeekName = lastUserWorkWeek.getWorkWeek().getName();
    }

    /**
     * Navigates to the UserWorkWeekListView for the test user before each test,
     * then reads the current state of that user's work week assignments.
     *
     * @param testInfo JUnit test info used to name the test recording
     * @throws Exception if navigation or login fails
     */
    @BeforeEach
    public void setupTest(TestInfo testInfo) throws Exception {
        userWorkWeekListViewTester.switchToUserWorkWeekListView(
                testInfo.getTestClass().get().getSimpleName(),
                generateTestCaseName(testInfo),
                testUsername);
        read();
    }

    /**
     * Tests that users cannot delete their only work week assignment.
     * <p>
     * Verifies that the delete button for the initial assignment is disabled,
     * preventing users from deleting their only record.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCannotDeleteOnlyAssignment() {
        userWorkWeekListViewTester.verifyCannotDeleteOnlyAssignment(lastUserWorkWeek.getStart());
    }

    /**
     * Tests the behavior when creating a work week assignment but canceling the operation.
     * <p>
     * Verifies that when a user clicks the create button, enters data, and then cancels,
     * no record is created in the list.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateCancel() {
        userWorkWeekListViewTester.createWorkWeekAssignmentCancel(startDate, workWeekName);
    }

    /**
     * Tests the behavior when successfully creating a work week assignment.
     * <p>
     * Verifies that when a user enters all required fields and confirms,
     * the record appears in the list with the correct start date and work week name.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreateConfirm() {
        userWorkWeekListViewTester.createWorkWeekAssignmentConfirm(startDate, workWeekName);
    }

    /**
     * Tests the behavior when attempting to delete a work week assignment but canceling.
     * <p>
     * Creates a second assignment, then attempts to delete it but cancels the confirmation dialog.
     * Verifies that the record remains in the list.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDeleteCancel() {
        // Create a second assignment
        userWorkWeekListViewTester.createWorkWeekAssignmentConfirm(startDate, workWeekName);
        // Try to delete but cancel
        userWorkWeekListViewTester.deleteWorkWeekAssignmentCancel(startDate);
    }

    /**
     * Tests the behavior when successfully deleting a work week assignment.
     * <p>
     * Creates a second assignment, then deletes it by confirming the deletion dialog.
     * Verifies that the record is removed from the list.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDeleteConfirm() {
        // Create a second assignment
        userWorkWeekListViewTester.createWorkWeekAssignmentConfirm(startDate, workWeekName);
        // Delete the newly created record
        userWorkWeekListViewTester.deleteWorkWeekAssignmentConfirm(startDate);
    }

    /**
     * Tests that validation prevents creation of duplicate assignments with the same start date.
     * <p>
     * Attempts to create an assignment with the same start date as the initial record.
     * Verifies that the confirm button is disabled and the duplicate is not created.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testDuplicateStartDate() {
        // Try to create a duplicate with the same start date as the initial record
        userWorkWeekListViewTester.createDuplicateDateAssignment(lastUserWorkWeek.getStart(), newWorkWeekName);
    }

    /**
     * Tests the behavior when attempting to edit a work week assignment but canceling.
     * <p>
     * Attempts to edit both the start date and work week of the initial assignment,
     * then cancels. Verifies that the original record details remain unchanged.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditCancel() {
        // Edit initial record but cancel
        userWorkWeekListViewTester.editWorkWeekAssignmentCancel(
                lastUserWorkWeek.getStart(), newStartDate,
                lastUserWorkWeekName, newWorkWeekName);
    }

    /**
     * Tests the behavior when successfully editing a work week assignment.
     * <p>
     * Edits the start date and work week of the initial assignment and confirms.
     * Verifies that the record with the new values appears in the list
     * and the old start date is no longer present.
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testEditConfirm() {
        // Edit initial record and confirm
        userWorkWeekListViewTester.editWorkWeekAssignmentConfirm(lastUserWorkWeek.getStart(), newStartDate, newWorkWeekName);
    }
}

