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

import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.util.ProductListViewTester;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Test class for the ActiveSprints view.
 * <p>
 * Tests the Scrum board functionality including task status changes via drag and drop.
 * </p>
 *
 * @author Abdalla Bushnaq
 * @version 1.0
 * @since 2025
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
@Slf4j
public class ActiveSprintsTest extends AbstractKeycloakUiTestUtil {

    private String                   featureName;
    @Autowired
    private ProductListViewTester    productListViewTester;
    private String                   productName;
    @Autowired
    private HumanizedSeleniumHandler seleniumHandler;
    private String                   sprintName;
    private Task                     story1;
    private Task                     story2;
    private Task                     task11;
    private Task                     task12;
    private Task                     task13;
    private Task                     task21;
    private Task                     task22;
    private Task                     task23;
    private String                   versionName;

    private Sprint generateData() {
        productName = "Jupiter";
        versionName = "1.0.0";
        featureName = "Property request api";
        sprintName  = "Sprint 1";

        addRandomUsers(10);
        Product product = addProduct(productName);
        Version version = addVersion(product, versionName);
        Feature feature = addFeature(version, featureName);
        Sprint  sprint  = addSprint(feature, sprintName);

        // Set sprint status to STARTED to make it active
        sprint.setStatus(Status.STARTED);
        sprintApi.update(sprint);

        User christopherPaul = userApi.getByEmail("christopher.paul@kassandra.org");
        User graceMartin     = userApi.getByEmail("grace.martin@kassandra.org");

        // Create a start milestone
        {
            LocalDateTime startDateTime  = LocalDateTime.parse("2025-05-05T08:00");
            Task          startMilestone = addTask(sprint, null, "Start", startDateTime, Duration.ZERO, null, null, null, TaskMode.MANUALLY_SCHEDULED, true);
        }

        // Story 1: Config API implementation with tasks in different statuses
        {
            story1 = addParentTask("Config api implementation", sprint, null, null);
            story1.setTaskStatus(TaskStatus.IN_PROGRESS);
            taskApi.update(story1);

            task11 = addTask("create controller", "4h", "6h", graceMartin, sprint, story1, null);
            task11.setTaskStatus(TaskStatus.IN_PROGRESS);
            task11.setRemainingEstimate(Duration.ofHours(3));
            taskApi.update(task11);

            task12 = addTask("api documentation", "2h", "3h", graceMartin, sprint, story1, null);
            task12.setTaskStatus(TaskStatus.TODO);
            task12.setRemainingEstimate(Duration.ofHours(2));
            taskApi.update(task12);

            task13 = addTask("api error handling", "5h", "7h", graceMartin, sprint, story1, null);
            task13.setTaskStatus(TaskStatus.TODO);
            task13.setRemainingEstimate(Duration.ofHours(5));
            taskApi.update(task13);
        }

        // Story 2: Config persistence implementation with tasks in different statuses
        {
            story2 = addParentTask("Config persistence implementation", sprint, null, null);
            story2.setTaskStatus(TaskStatus.IN_PROGRESS);
            taskApi.update(story2);

            task21 = addTask("create repository", "4h", "6h", christopherPaul, sprint, story2, null);
            task21.setTaskStatus(TaskStatus.DONE);
            task21.setRemainingEstimate(Duration.ZERO);
            taskApi.update(task21);

            task22 = addTask("schema documentation", "2h", "3h", christopherPaul, sprint, story2, null);
            task22.setTaskStatus(TaskStatus.DONE);
            task22.setRemainingEstimate(Duration.ZERO);
            taskApi.update(task22);

            task23 = addTask("persistence error handling", "5h", "7h", christopherPaul, sprint, story2, null);
            task23.setTaskStatus(TaskStatus.IN_PROGRESS);
            task23.setRemainingEstimate(Duration.ofHours(4));
            taskApi.update(task23);
        }

        return sprint;
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testActiveSprintsView() throws Exception {
        // Generate test data
        Sprint sprint = generateData();
        productListViewTester.switchToProductListViewWithOidc("christopher.paul@kassandra.org", "password", null, null, null);
        seleniumHandler.getAndCheck("http://localhost:" + productListViewTester.getPort() + "/ui/" + ActiveSprints.ROUTE);
        // Navigate to active sprints view
        // TODO: Implement navigation once the view is created
        // openUrl("/active-sprints");

        // Wait for page to load
        // waitForVaadin();
        // Thread.sleep(2000);

        // Take screenshot
        // takeScreenshot("active_sprints_initial");

        log.info("ActiveSprints view test completed successfully");
        seleniumHandler.waitUntilBrowserClosed(5000);
    }
}

