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
import de.bushnaq.abdalla.kassandra.ui.view.Backlog;
import org.springframework.beans.factory.annotation.Value;
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
public class TaskListViewTester extends AbstractViewTester {

    /**
     * Constructs a new TaskListViewTester with the given Selenium handler.
     *
     * @param seleniumHandler the handler for Selenium operations
     */
    public TaskListViewTester(HumanizedSeleniumHandler seleniumHandler, @Value("${local.server.port:8080}") int port) {
        super(seleniumHandler, port);
    }

    public void createTask(String newTaskName) {
        seleniumHandler.click(Backlog.CREATE_TASK_BUTTON_ID);
    }

}
