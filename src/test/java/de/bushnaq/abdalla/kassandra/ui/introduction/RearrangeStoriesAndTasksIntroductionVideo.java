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

package de.bushnaq.abdalla.kassandra.ui.introduction;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.bushnaq.abdalla.kassandra.ai.narrator.Narrator;
import de.bushnaq.abdalla.kassandra.ai.narrator.NarratorAttribute;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.ui.introduction.util.InstructionVideosUtil;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.LoginView;
import de.bushnaq.abdalla.kassandra.ui.view.SprintListView;
import de.bushnaq.abdalla.kassandra.ui.view.TaskGrid;
import de.bushnaq.abdalla.kassandra.ui.view.TaskListView;
import de.bushnaq.abdalla.kassandra.ui.view.util.*;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=8080",
                "spring.profiles.active=test",
                "spring.security.basic.enabled=false"// Disable basic authentication for these tests
        }
)
@AutoConfigureMockMvc
@Transactional
public class RearrangeStoriesAndTasksIntroductionVideo extends AbstractKeycloakUiTestUtil {
    //    public static final  float                      EXAGGERATE_LOW    = 0.25f;
//    public static final  float                      EXAGGERATE_NORMAL = 0.3f;
    public static final  NarratorAttribute          EXCITED       = new NarratorAttribute().withExaggeration(.7f).withCfgWeight(.3f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    public static final  String                     NEW_MILESTONE = "New Milestone-";
    public static final  String                     NEW_STORY     = "New Story-";
    public static final  String                     NEW_TASK      = "New Task-";
    public static final  NarratorAttribute          NORMAL        = new NarratorAttribute().withExaggeration(.5f).withCfgWeight(.5f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    public static final  String                     VIDEO_TITLE   = "Rearranging Stories and Tasks Introduction Video";
    // Start Keycloak container with realm configuration
    @Container
    private static final KeycloakContainer          keycloak      = new KeycloakContainer("quay.io/keycloak/keycloak:24.0.1")
            .withRealmImportFile("keycloak/project-hub-realm.json")
            .withAdminUsername("admin")
            .withAdminPassword("admin")
            // Expose on a fixed port for more reliable testing
            .withExposedPorts(8080, 8443)
            // Add debugging to see container output
            .withLogConsumer(outputFrame -> System.out.println("Keycloak: " + outputFrame.getUtf8String()))
            // Make Keycloak accessible from outside the container
            .withEnv("KC_HOSTNAME_STRICT", "false")
            .withEnv("KC_HOSTNAME_STRICT_HTTPS", "false");
    @Autowired
    private              AvailabilityListViewTester availabilityListViewTester;
    @Autowired
    private              FeatureListViewTester      featureListViewTester;
    private              String                     featureName;
    @Autowired
    private              LocationListViewTester     locationListViewTester;
    @Autowired
    private              OffDayListViewTester       offDayListViewTester;
    @Autowired
    private              ProductListViewTester      productListViewTester;
    private              String                     productName;
    @Autowired
    private              HumanizedSeleniumHandler   seleniumHandler;
    @Autowired
    private              SprintListViewTester       sprintListViewTester;
    private              String                     sprintName;
    @Autowired
    private              TaskListViewTester         taskListViewTester;
    @Autowired
    private              UserListViewTester         userListViewTester;
    @Autowired
    private              VersionListViewTester      versionListViewTester;
    private              String                     versionName;

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void createVideo(RandomCase randomCase, TestInfo testInfo) throws Exception {
        //generate the users
        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateProductsIfNeeded(testInfo, randomCase);

        Sprint sprint = generateData();

        Narrator paul = Narrator.withChatterboxTTS("tts/" + testInfo.getTestClass().get().getSimpleName());
        paul.setSilent(true);
        Narrator grace = Narrator.withChatterboxTTS("tts/" + testInfo.getTestClass().get().getSimpleName(), "grace");
        grace.setSilent(true);
        seleniumHandler.getAndCheck("http://localhost:" + "8080" + "/ui/" + LoginView.ROUTE);
        productListViewTester.switchToProductListViewWithOidc("christopher.paul@kassandra.org", "password", null, null, null);


        // Demo the natural language search capabilities
        productListViewTester.selectProduct(productName);
        versionListViewTester.selectVersion(versionName);
        featureListViewTester.selectFeature(featureName);
        seleniumHandler.click(SprintListView.SPRINT_GRID_CONFIG_BUTTON_PREFIX + sprintName);
        seleniumHandler.waitUntil(ExpectedConditions.elementToBeClickable(By.id(TaskListView.TASK_LIST_PAGE_TITLE_ID)));

        HumanizedSeleniumHandler.setHumanize(false);
        seleniumHandler.showOverlay("Kassandra Stories and Tasks", InstructionVideosUtil.VIDEO_SUBTITLE);
//        seleniumHandler.startRecording(InstructionVideosUtil.TARGET_FOLDER, VIDEO_TITLE);
        paul.pause(3000);
        paul.narrateAsync(NORMAL, "Hi everyone, Christopher Paul here from kassandra.org. Today we're going to learn about Stories and Tasks in Kassandra. A story is basically a container for a list of Tasks. Tasks represent the work we plan including the estimation for the effort. This is essential for accurate sprint planning and capacity calculations.");
        seleniumHandler.hideOverlay();

        int    orderId        = 0;
        String milestone1Name = NEW_MILESTONE + orderId++;
        String story1Name     = NEW_STORY + orderId++;
        String task11Name     = NEW_TASK + orderId++;
        String task12Name     = NEW_TASK + orderId++;
        String task13Name     = NEW_TASK + orderId++;
        String story2Name     = NEW_STORY + orderId++;
        String task21Name     = NEW_TASK + orderId++;
        String task22Name     = NEW_TASK + orderId++;
        String task23Name     = NEW_TASK + orderId++;

        //---------------------------------------------------------------------------------------..
        // Tasks Page
        //---------------------------------------------------------------------------------------..


        HumanizedSeleniumHandler.setHumanize(true);
        // Move Story-5 before Story-1
        seleniumHandler.dragAndDrop(TaskGrid.TASK_GRID_PREFIX + story2Name, TaskGrid.TASK_GRID_PREFIX + story1Name);

        seleniumHandler.dragAndDrop(TaskGrid.TASK_GRID_PREFIX + task12Name, TaskGrid.TASK_GRID_PREFIX + task11Name);

        seleniumHandler.dragAndDrop(TaskGrid.TASK_GRID_PREFIX + task22Name, TaskGrid.TASK_GRID_PREFIX + task11Name);

        seleniumHandler.waitUntilBrowserClosed(0);
    }

    private Sprint generateData() {
        productName = "Jupiter";
        versionName = "1.0.0";
        featureName = "Property request api";
        sprintName  = "Minimum Viable Product";
        Product product = addProduct(productName);
        Version version = addVersion(product, versionName);
        Feature feature = addFeature(version, featureName);
        Sprint  sprint  = addSprint(feature, sprintName);

        User christopherPaul = userApi.getByEmail("christopher.paul@kassandra.org");
        User graceMartin     = userApi.getByEmail("grace.martin@kassandra.org");
        {
            LocalDateTime startDateTime  = LocalDateTime.parse("2025-05-05T08:00");
            Task          startMilestone = addTask(sprint, null, "New Milestone-0", startDateTime, Duration.ZERO, null, null, null, TaskMode.MANUALLY_SCHEDULED, true);
        }
        {
            Task story1 = addParentTask("New Story-1", sprint, null, null);
            Task task11 = addTask("New Task-2", "1d", null, christopherPaul, sprint, story1, null);
            Task task12 = addTask("New Task-3", "1d", null, christopherPaul, sprint, story1, null);
            Task task13 = addTask("New Task-4", "1d", null, christopherPaul, sprint, story1, null);
        }
        {
            Task story2 = addParentTask("New Story-5", sprint, null, null);
            Task task21 = addTask("New Task-6", "1d", null, graceMartin, sprint, story2, null);
            Task task22 = addTask("New Task-7", "1d", null, graceMartin, sprint, story2, null);
            Task task32 = addTask("New Task-8", "1d", null, graceMartin, sprint, story2, null);
        }


        return sprint;
    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(1, LocalDate.parse("2025-05-01"), Duration.ofDays(10), 0, 0, 0, 0, 0, 6, 8, 12, 6, 13)//
        };
        return Arrays.stream(randomCases).toList();
    }

}
