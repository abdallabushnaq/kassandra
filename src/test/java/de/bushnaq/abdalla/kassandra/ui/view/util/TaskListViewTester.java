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

import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.TaskListView;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Test helper class for interacting with the Task UI components.
 * <p>
 * This class provides methods to test task-related operations in the UI such as
 * creating, editing, deleting tasks and navigating between views. It uses
 * {@link HumanizedSeleniumHandler} to interact with UI elements and validate results.
 * <p>
 * Tasks represent work items within a sprint. In the hierarchy: Products contain Versions,
 * Versions contain Features, Features contain Sprints, and Sprints contain Tasks.
 */
@Component
@Lazy
public class TaskListViewTester {
    private final HumanizedSeleniumHandler seleniumHandler;

    /**
     * Constructs a new TaskListViewTester with the given Selenium handler.
     *
     * @param seleniumHandler the handler for Selenium operations
     */
    public TaskListViewTester(HumanizedSeleniumHandler seleniumHandler) {
        this.seleniumHandler = seleniumHandler;
    }

    public void createTask(String newTaskName) {
        seleniumHandler.click(TaskListView.CREATE_TASK_BUTTON_ID);
    }

    /**
     * Selects a task from the task grid by name.
     *
     * @param name the name of the task to select
     */
//    public void selectTask(String name) {
//        seleniumHandler.selectGridRow(TASK_GRID_NAME_PREFIX, TaskView.class, name);
//    }

    /**
     * Tests the creation of a task where the user cancels the operation.
     * <p>
     * Opens the task creation dialog, enters the given task name, then cancels
     * the dialog. Verifies that no task with the specified name appears in the task list.
     *
     * @param name the name of the task to attempt to create
     */
//    public void createTaskCancel(String name) {
//        seleniumHandler.click(TaskListView.CREATE_TASK_BUTTON);
//        seleniumHandler.setTextField(TaskDialog.TASK_NAME_FIELD, name);
//        seleniumHandler.click(TaskDialog.CANCEL_BUTTON);
//        seleniumHandler.ensureIsNotInList(TaskListView.TASK_GRID_NAME_PREFIX, name);
//    }

    /**
     * Tests the successful creation of a task.
     * <p>
     * Opens the task creation dialog, enters the given task name, then confirms
     * the dialog. Verifies that a task with the specified name appears in the task list.
     *
     * @param name the name of the task to create
     */
//    public void createTaskConfirm(String name) {
//        seleniumHandler.click(TaskListView.CREATE_TASK_BUTTON);
//        seleniumHandler.setTextField(TaskDialog.TASK_NAME_FIELD, name);
//        seleniumHandler.click(TaskDialog.CONFIRM_BUTTON);
//        seleniumHandler.ensureIsInList(TaskListView.TASK_GRID_NAME_PREFIX, name);
//    }

    /**
     * Tests the attempt to create a task with a name that already exists.
     * <p>
     * Opens the task creation dialog, enters a name that already exists,
     * clicks the save button, and verifies that an error message is displayed
     * indicating the name is already in use.
     *
     * @param name the duplicate name to attempt to use
     */
//    public void createTaskWithDuplicateName(String name) {
//        seleniumHandler.click(TaskListView.CREATE_TASK_BUTTON);
//        seleniumHandler.setTextField(TaskDialog.TASK_NAME_FIELD, name);
//        seleniumHandler.click(TaskDialog.CONFIRM_BUTTON);
//
//        // Check for field error message
//        String errorMessage = seleniumHandler.getFieldErrorMessage(TaskDialog.TASK_NAME_FIELD);
//        assertNotNull(errorMessage, "Error message should be present on the name field");
//    }
}
