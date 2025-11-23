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
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.rest.debug.DebugUtil;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.util.FeatureListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.ProductListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.SprintListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.VersionListViewTester;
import de.bushnaq.abdalla.kassandra.util.NameGenerator;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class SprintQualityBoardTest extends AbstractUiTestUtil {
    @Autowired
    private FeatureListViewTester    featureListViewTester;
    @Autowired
    private ProductListViewTester    productListViewTester;
    @Autowired
    private HumanizedSeleniumHandler seleniumHandler;
    @Autowired
    private SprintListViewTester     sprintListViewTester;
    @Autowired
    private VersionListViewTester    versionListViewTester;

    private static String generateFeatureName(int t) {
        return String.format("Feature-%d", t);
    }

    private void generateTasks(RandomCase randomCase) {
        random.setSeed(randomCase.getSeed());
        int numberOfUsers    = random.nextInt(randomCase.getMaxNumberOfUsers()) + 2;
        int numberOfFeatures = random.nextInt(randomCase.getMaxNumberOfStories()) + 1;
        int numberOfTasks    = random.nextInt(randomCase.getMaxNumberOfTasks()) + 1;
        {
            addRandomUsers(numberOfUsers);
            Product product = addProduct(nameGenerator.generateProductName(0));
            Version version = addVersion(product, nameGenerator.generateVersionName(0));
            Feature feature = addFeature(version, nameGenerator.generateFeatureName(0));
            addSprint(feature, nameGenerator.generateSprintName(0));
        }
        Sprint sprint = expectedSprints.getFirst();

        Task startMilestone = addTask(sprint, null, "Start", LocalDateTime.parse("2024-12-15T08:00:00"), Duration.ZERO, null, null, null, TaskMode.MANUALLY_SCHEDULED, true);
        for (int f = 0; f < numberOfFeatures; f++) {
            String featureName = generateFeatureName(f);
            Task   feature     = addParentTask(featureName, sprint, null, startMilestone);
            for (int t = 0; t < numberOfTasks; t++) {
                User   user     = expectedUsers.stream().toList().get(random.nextInt(numberOfUsers));
                String duration = String.format("%dd", random.nextInt(randomCase.getMaxDurationDays()) + 1);
                String workName = NameGenerator.generateWorkName(featureName, t);
                addTask(workName, duration, null, user, sprint, feature, null);
            }
        }
    }


    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(1, 10, 2, 1, 2, 1),//
//                new RandomCase(2, 10, 3, 2, 3, 1)//
        };
        return Arrays.stream(randomCases).toList();
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testShowProducts(RandomCase randomCase, TestInfo testInfo) throws Exception {
        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateTasks(randomCase);
        Sprint sprint = expectedSprints.getFirst();
        sprint.initialize();
        sprint.initUserMap(userApi.getAll(sprint.getId()));
        sprint.initTaskMap(taskApi.getAll(sprint.getId()), worklogApi.getAll(sprint.getId()));

        levelResources(testInfo, sprint, null);
        generateWorklogs(sprint, ParameterOptions.getLocalNow());
//        seleniumHandler.startRecording(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
        productListViewTester.switchToProductListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
        productListViewTester.selectProduct(nameGenerator.generateProductName(0));
        versionListViewTester.selectVersion(nameGenerator.generateVersionName(0));
        featureListViewTester.selectFeature(nameGenerator.generateFeatureName(0));
        sprintListViewTester.selectSprint(nameGenerator.generateSprintName(0));
        if (DebugUtil.DEBUG) {
            seleniumHandler.waitUntilBrowserClosed(0);
        } else {
            seleniumHandler.waitUntilBrowserClosed(5000);
        }
    }

}
