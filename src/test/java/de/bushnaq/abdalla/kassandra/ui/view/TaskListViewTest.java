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

import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.util.*;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Integration test for the TaskListView UI component.
 * Tests create, edit, and delete operations for tasks in the UI.
 * <p>
 * These tests use {@link TaskListViewTester} to interact with the UI elements
 * and verify the expected behavior. Each test requires a product, version, project,
 * and sprint to be created first, as tasks exist within the context of a sprint.
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
public class TaskListViewTest extends AbstractKeycloakUiTestUtil {
    @Autowired
    private       FeatureListViewTester    featureListViewTester;
    private final String                   featureName = nameGenerator.generateFeatureName(0);
    //    private final String                newTaskName = "NewTask-3";
    @Autowired
    private       ProductListViewTester    productListViewTester;
    private final String                   productName = nameGenerator.generateProductName(0);
    @Autowired
    private       HumanizedSeleniumHandler seleniumHandler;
    @Autowired
    private       SprintListViewTester     sprintListViewTester;
    private final String                   sprintName  = nameGenerator.generateSprintName(0);
    @Autowired
    private       TaskListViewTester       taskListViewTester;
    private final String                   taskName0   = nameGenerator.generateSprintName(0);
    @Autowired
    private       VersionListViewTester    versionListViewTester;
    private final String                   versionName = nameGenerator.generateVersionName(0);

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(1, OffsetDateTime.parse("2025-08-11T08:00:00+01:00"), LocalDate.parse("2025-08-04"), Duration.ofDays(10), 1, 1, 1, 1, 1, 1, 1, 1, 6, 0, 10, 0, 1)//
        };
        return Arrays.stream(randomCases).toList();
    }

    /**
     * Tests the behavior when creating a task but canceling the operation.
     * <p>
     * Verifies that when a user clicks the create task button, enters a name, and then
     * cancels the operation, no task is created in the list.
     *
     * @throws Exception if any error occurs during the test
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testCreate(RandomCase randomCase, TestInfo testInfo) throws Exception {
        ParameterOptions.setNow(OffsetDateTime.now().withHour(8).withMinute(0).withSecond(0).withNano(0));
        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateProductsIfNeeded(testInfo, randomCase);

        productListViewTester.switchToProductListViewWithOidc(
                "christopher.paul@kassandra.org",
                "password",
                null,
                testInfo.getTestClass().get().getSimpleName(),
                generateTestCaseName(testInfo)
        );
        productListViewTester.selectProduct(productName);
        versionListViewTester.selectVersion(versionName);
        featureListViewTester.selectFeature(featureName);
        sprintListViewTester.configSprint(sprintName);

        taskListViewTester.createTask(taskName0);
        seleniumHandler.waitUntilBrowserClosed(5000);
    }
}
