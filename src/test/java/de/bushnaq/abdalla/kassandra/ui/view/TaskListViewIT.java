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
import de.bushnaq.abdalla.kassandra.dto.Feature;
import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.Version;
import de.bushnaq.abdalla.kassandra.ui.component.TaskGrid;
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
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Integration test for the TaskListView UI component.
 * Tests task creation in the UI.
 * <p>
 * The test requires a product, version, feature,
 * and sprint to be created first, as tasks exist within the context of a sprint.
 */
@Tag("IntegrationUiTest")
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=${test.server.port:0}",
                "spring.security.basic.enabled=false"// Disable basic authentication for these tests
        }
)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class TaskListViewIT extends AbstractKeycloakUiTestUtil {
    @Autowired
    AboutViewTester aboutViewTester;
    @Autowired
    private       FeatureListViewTester    featureListViewTester;
    //    private final String                newTaskName = "NewTask-3";
    @Autowired
    private       ProductListViewTester    productListViewTester;
    @Autowired
    private       HumanizedSeleniumHandler seleniumHandler;
    @Autowired
    private       SprintListViewTester     sprintListViewTester;
    @Autowired
    private       VersionListViewTester    versionListViewTester;

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                RandomCase.builder()
                        .testCaseIndex(1)
                        .now(OffsetDateTime.parse("2025-08-11T08:00:00+01:00"))
                        .startDate(LocalDate.parse("2025-08-04"))
                        .numberOfProducts(1, 1)
                        .numberOfVersions(1, 1)
                        .numberOfFeatures(1, 1)
                        .numberOfSprints(1, 1)
                        .taskDurationPerDay(1, 6).numberOfStories(0, 0)
                        .numberOfUsers(1, 10).numberOfUsersPerSprint(1, 10)
                        .numberOfTasks(0, 0).seed(1)
                        .build()//
        };
        return Arrays.stream(randomCases).toList();
    }

    /**
     * Verifies that creating a task adds it to the sprint and displays it in the backlog.
     *
     * @param randomCase the fixture configuration
     * @param testInfo the current test metadata
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
        peg.addRandomUsers(1);
        Product product = peg.addProduct(peg.nameGenerator.generateProductName(0));
        Version version = peg.addVersion(product, peg.nameGenerator.generateVersionName(0));
        Feature feature = peg.addFeature(version, peg.nameGenerator.generateFeatureName(0));
        var sprint = peg.addSprint(feature, peg.nameGenerator.generateSprintName(0));

        aboutViewTester.login(
                "christopher.paul@kassandra.org",
                "password",
                null,
                testInfo.getTestClass().get().getSimpleName(),
                generateTestCaseName(testInfo)
        );
        productListViewTester.switchToProductListView();
        productListViewTester.selectProduct(peg.nameGenerator.generateProductName(0));
        versionListViewTester.selectVersion(peg.nameGenerator.generateVersionName(0));
        featureListViewTester.selectFeature(peg.nameGenerator.generateFeatureName(0));
        sprintListViewTester.configSprint(peg.nameGenerator.generateSprintName(0));

        var existingIds = peg.taskApi.getAll(sprint.getId()).stream().map(Task::getId).toList();
        seleniumHandler.click(Backlog.CREATE_TASK_BUTTON_ID);
        seleniumHandler.waitUntil(driver -> peg.taskApi.getAll(sprint.getId()).stream()
                .anyMatch(task -> !existingIds.contains(task.getId())));
        Task createdTask = peg.taskApi.getAll(sprint.getId()).stream()
                .filter(task -> !existingIds.contains(task.getId())).findFirst().orElseThrow();
        seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_NAME_PREFIX, createdTask.getName());
    }
}
