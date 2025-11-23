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
import de.bushnaq.abdalla.kassandra.dto.Feature;
import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Version;
import de.bushnaq.abdalla.kassandra.ui.component.TaskGrid;
import de.bushnaq.abdalla.kassandra.ui.introduction.util.InstructionVideosUtil;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.LoginView;
import de.bushnaq.abdalla.kassandra.ui.view.SprintListView;
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
import java.time.OffsetDateTime;
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
public class StoriesAndTasksIntroductionVideo extends AbstractKeycloakUiTestUtil {
    //    public static final  float                      EXAGGERATE_LOW    = 0.25f;
//    public static final  float                      EXAGGERATE_NORMAL = 0.3f;
    public static final  NarratorAttribute          EXCITED       = new NarratorAttribute().withExaggeration(.7f).withCfgWeight(.3f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    public static final  String                     NEW_MILESTONE = "New Milestone-";
    public static final  String                     NEW_STORY     = "New Story-";
    public static final  String                     NEW_TASK      = "New Task-";
    public static final  NarratorAttribute          NORMAL        = new NarratorAttribute().withExaggeration(.5f).withCfgWeight(.5f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    public static final  String                     VIDEO_TITLE   = "Kassandra Stories and Tasks";
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
        seleniumHandler.setWindowSize(InstructionVideosUtil.VIDEO_WIDTH, InstructionVideosUtil.VIDEO_HEIGHT);
        //generate the users
        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateProductsIfNeeded(testInfo, randomCase);

        productName = "Jupiter";
        versionName = "1.0.0";
        featureName = "Property request api";
        sprintName  = "Minimum Viable Product";
        Product product = addProduct(productName);
        Version version = addVersion(product, versionName);
        Feature feature = addFeature(version, featureName);
        Sprint  sprint  = addSprint(feature, sprintName);

        Narrator paul = Narrator.withChatterboxTTS("tts/" + testInfo.getTestClass().get().getSimpleName());
        paul.setSilent(false);
        Narrator grace = Narrator.withChatterboxTTS("tts/" + testInfo.getTestClass().get().getSimpleName(), "grace");
        seleniumHandler.getAndCheck("http://localhost:" + "8080" + "/ui/" + LoginView.ROUTE);
        productListViewTester.switchToProductListViewWithOidc("christopher.paul@kassandra.org", "password", null, null, null);


        // Demo the natural language search capabilities
        productListViewTester.selectProduct(productName);
        versionListViewTester.selectVersion(versionName);
        featureListViewTester.selectFeature(featureName);
        seleniumHandler.click(SprintListView.SPRINT_GRID_CONFIG_BUTTON_PREFIX + sprintName);
        seleniumHandler.waitUntil(ExpectedConditions.elementToBeClickable(By.id(TaskListView.TASK_LIST_PAGE_TITLE_ID)));

        HumanizedSeleniumHandler.setHumanize(true);
        seleniumHandler.showOverlay(VIDEO_TITLE, InstructionVideosUtil.VIDEO_SUBTITLE);
        seleniumHandler.startRecording(InstructionVideosUtil.TARGET_FOLDER, VIDEO_TITLE + " " + InstructionVideosUtil.VIDEO_SUBTITLE);
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
        paul.narrate(NORMAL, "Lets start by adding a milestone that will fix the starting point of our sprint.").pause();

        paul.narrate(NORMAL, "Select the Create Milestone button...");
        seleniumHandler.click(TaskListView.CREATE_MILESTONE_BUTTON_ID);
        seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_PREFIX, milestone1Name);

        String graceMartin = "Grace Martin";
        {
            //first story
            paul.narrate(NORMAL, "Lets also create a story. We use stories as containers for the actual work items called tasks.");
            seleniumHandler.click(TaskListView.CREATE_STORY_BUTTON_ID);
            seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_PREFIX, story1Name);

            paul.narrate(NORMAL, "You can see that all the new created items are always added to the end of our table.").pause();

            paul.narrateAsync(NORMAL, "Lets create 3 additional tasks as work units for our first sprint.");
            seleniumHandler.click(TaskListView.CREATE_TASK_BUTTON_ID);
            seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_PREFIX, task11Name);

            seleniumHandler.click(TaskListView.CREATE_TASK_BUTTON_ID);
            seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_PREFIX, task12Name);

            seleniumHandler.click(TaskListView.CREATE_TASK_BUTTON_ID);
            seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_PREFIX, task13Name);

            paul.narrate(NORMAL, "If you look carefully, you will notice that all three tasks have been assigned to the story.").pause();
            paul.narrate(NORMAL, "The story is the parent of these tasks.").pause();
            paul.narrate(NORMAL, "Kassandra does that automatically. All three tasks also are automatically assigned to myself.").pause();

            //edit
            paul.narrate(EXCITED, "Good!").longPause();
            paul.narrate(NORMAL, "Select the edit button to change to whole table into edit mode...").pause();
            seleniumHandler.click(TaskListView.EDIT_BUTTON_ID);

            paul.narrate(NORMAL, "We can now edit all valid milestone, story or task cells.").pause();
            paul.narrate(NORMAL, "Lets give the milestone a name and fixed start date and time. We want our developers to start working first thing Monday morning.");
            paul.narrate(NORMAL, "Start is a short and descriptive name.");
            paul.pauseIfSilent(500);// for debugging purposes only, has an effect if narrator is set to silent
            seleniumHandler.waitForPageLoaded();
            seleniumHandler.setTextField(TaskGrid.TASK_GRID_PREFIX + milestone1Name + TaskGrid.NAME_FIELD, "Start");
            paul.narrate(NORMAL, "Monday morning would be 8 AM.");
            final LocalDateTime startDateTime = LocalDateTime.of(2025, 5, 5, 8, 0);
            seleniumHandler.setDateTimePickerValue(TaskGrid.TASK_GRID_PREFIX + milestone1Name + TaskGrid.START_FIELD, startDateTime);

            paul.narrate(NORMAL, "Our first Story is all about the API.");
            seleniumHandler.setTextField(TaskGrid.TASK_GRID_PREFIX + story1Name + TaskGrid.NAME_FIELD, "Config api implementation");

            paul.narrate(NORMAL, "We need a API controller.");
            seleniumHandler.setTextField(TaskGrid.TASK_GRID_PREFIX + task11Name + TaskGrid.NAME_FIELD, "create controller");
            paul.narrate(NORMAL, "But, as i am not a developer, we will assign these tasks to a developer.");
            paul.narrate(NORMAL, "Lets assign it to Grace.");
            seleniumHandler.setComboBoxValue(TaskGrid.TASK_GRID_PREFIX + task11Name + TaskGrid.ASSIGNED_FIELD, graceMartin);

            paul.narrate(NORMAL, "Next we need to document our API.");
            seleniumHandler.setTextField(TaskGrid.TASK_GRID_PREFIX + task12Name + TaskGrid.NAME_FIELD, "api documentation");
            seleniumHandler.setComboBoxValue(TaskGrid.TASK_GRID_PREFIX + task12Name + TaskGrid.ASSIGNED_FIELD, graceMartin);

            paul.narrate(NORMAL, "Never forget error handling.");
            seleniumHandler.setTextField(TaskGrid.TASK_GRID_PREFIX + task13Name + TaskGrid.NAME_FIELD, "api error handling");
            seleniumHandler.setComboBoxValue(TaskGrid.TASK_GRID_PREFIX + task13Name + TaskGrid.ASSIGNED_FIELD, graceMartin);

            seleniumHandler.click(TaskListView.SAVE_BUTTON_ID);
            story1Name = "Config api implementation";
            task11Name = "create controller";
            task12Name = "api documentation";
            task13Name = "api error handling";


            paul.narrate(NORMAL, "For every task we need to estimate minimum and maximum effort in person hours. This usually something a developer would do. So lets ask Grace to help us with that.");
            paul.narrate(NORMAL, "Grace, can you please take over and estimate the effort for your tasks?");
            grace.narrate(NORMAL, "Sure, Christopher! Let me take over.").pause();
            grace.narrate(NORMAL, "Lets got to edit mode.");

            seleniumHandler.click(TaskListView.EDIT_BUTTON_ID);
            grace.pauseIfSilent(500);// for debugging purposes only, has an effect if narrator is set to silent
            seleniumHandler.waitForPageLoaded();

            grace.narrateAsync(NORMAL, "Lets set minimum to 4 hours and maximum to 6 hours.");
            seleniumHandler.setTextField(TaskGrid.TASK_GRID_PREFIX + task11Name + TaskGrid.MIN_ESTIMATE_FIELD, "4h");
            grace.narrate(NORMAL, "If we do not define the maximum, Kassandra will use the minimum instead.");
            seleniumHandler.setTextField(TaskGrid.TASK_GRID_PREFIX + task11Name + TaskGrid.MAX_ESTIMATE_FIELD, "6h");

            grace.narrateAsync(NORMAL, "Lets set minimum to 2 hours and maximum to 3 hours.");
            seleniumHandler.setTextField(TaskGrid.TASK_GRID_PREFIX + task12Name + TaskGrid.MIN_ESTIMATE_FIELD, "2h");
            seleniumHandler.setTextField(TaskGrid.TASK_GRID_PREFIX + task12Name + TaskGrid.MAX_ESTIMATE_FIELD, "3h");

            grace.narrateAsync(NORMAL, "Lets set minimum to 5 hours and maximum to 7 hours.");
            seleniumHandler.setTextField(TaskGrid.TASK_GRID_PREFIX + task13Name + TaskGrid.MIN_ESTIMATE_FIELD, "5h");
            seleniumHandler.setTextField(TaskGrid.TASK_GRID_PREFIX + task13Name + TaskGrid.MAX_ESTIMATE_FIELD, "7h");

            seleniumHandler.click(TaskListView.SAVE_BUTTON_ID);
        }


        {
            // second story
            paul.narrate(NORMAL, "Lets create another story.");
            seleniumHandler.click(TaskListView.CREATE_STORY_BUTTON_ID);
            seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_PREFIX, story2Name);

            seleniumHandler.click(TaskListView.CREATE_TASK_BUTTON_ID);
            seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_PREFIX, task21Name);

            seleniumHandler.click(TaskListView.CREATE_TASK_BUTTON_ID);
            seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_PREFIX, task22Name);

            seleniumHandler.click(TaskListView.CREATE_TASK_BUTTON_ID);
            seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_PREFIX, task23Name);
            paul.narrate(NORMAL, "Have you noticed that the new tasks have been assigned to Grace automatically?").pause();
            paul.narrate(NORMAL, "Kassandra uses the assigned user of a previous task. In case there is no previous task, it will use the logged in user.").pause();

            //edit
            seleniumHandler.click(TaskListView.EDIT_BUTTON_ID);
            paul.pauseIfSilent(500);// for debugging purposes only, has an effect if narrator is set to silent
            seleniumHandler.waitForPageLoaded();

            seleniumHandler.setTextField(TaskGrid.TASK_GRID_PREFIX + story2Name + TaskGrid.NAME_FIELD, "Config persistence implementation");

            seleniumHandler.setTextField(TaskGrid.TASK_GRID_PREFIX + task21Name + TaskGrid.NAME_FIELD, "create repository");

            seleniumHandler.setTextField(TaskGrid.TASK_GRID_PREFIX + task22Name + TaskGrid.NAME_FIELD, "schema documentation");

            seleniumHandler.setTextField(TaskGrid.TASK_GRID_PREFIX + task23Name + TaskGrid.NAME_FIELD, "persistence error handling");

            seleniumHandler.click(TaskListView.SAVE_BUTTON_ID);
            story2Name = "Config persistence implementation";
            task21Name = "create repository";
            task22Name = "schema documentation";
            task23Name = "persistence error handling";


            paul.narrate(NORMAL, "Grace, can you take over again?");
            grace.narrate(NORMAL, "Let me take over.").pause();
            grace.narrate(NORMAL, "Edit mode...");

            seleniumHandler.click(TaskListView.EDIT_BUTTON_ID);
            grace.pauseIfSilent(500);// for debugging purposes only, has an effect if narrator is set to silent
            seleniumHandler.waitForPageLoaded();

            seleniumHandler.setMoveMouse(false);
            grace.narrate(NORMAL, "I will need minimum 4 hours.").pause();
            seleniumHandler.setTextField(TaskGrid.TASK_GRID_PREFIX + task21Name + TaskGrid.MIN_ESTIMATE_FIELD, "4h");
            grace.narrate(NORMAL, "We can also navigate to the next field by pressing the tab key.").pause();
            seleniumHandler.showTransientTitle("Tab");
            seleniumHandler.sendKeys(TaskGrid.TASK_GRID_PREFIX + task21Name + TaskGrid.MIN_ESTIMATE_FIELD, "\t");
            grace.pause();
            grace.narrate(NORMAL, "Maximum is about  6 hours.").pause();
            seleniumHandler.setTextField(TaskGrid.TASK_GRID_PREFIX + task21Name + TaskGrid.MAX_ESTIMATE_FIELD, "6h");
            seleniumHandler.setMoveMouse(true);

            grace.narrateAsync(NORMAL, "Lets set minimum to 2 hours and maximum to 3 hours.");
            seleniumHandler.setTextField(TaskGrid.TASK_GRID_PREFIX + task22Name + TaskGrid.MIN_ESTIMATE_FIELD, "2h");
            seleniumHandler.setTextField(TaskGrid.TASK_GRID_PREFIX + task22Name + TaskGrid.MAX_ESTIMATE_FIELD, "3h");

            grace.narrateAsync(NORMAL, "Lets set minimum to 5 hours and maximum to 7 hours.");
            seleniumHandler.setTextField(TaskGrid.TASK_GRID_PREFIX + task23Name + TaskGrid.MIN_ESTIMATE_FIELD, "5h");
            seleniumHandler.setTextField(TaskGrid.TASK_GRID_PREFIX + task23Name + TaskGrid.MAX_ESTIMATE_FIELD, "7h");

            seleniumHandler.click(TaskListView.SAVE_BUTTON_ID);
        }


        paul.narrate(NORMAL, "We want our story to depend on our milestone. The story can only start after the milestone.").pause();
        paul.narrate(NORMAL, "Defining such a dependency between a task or story to other tasks or stories can be done in 3 different ways...");


        seleniumHandler.waitUntilBrowserClosed(5000);
    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(1, OffsetDateTime.parse("2025-08-11T08:00:00+01:00"), LocalDate.parse("2025-08-04"), Duration.ofDays(10), 0, 0, 0, 0, 0, 0, 0, 0, 6, 8, 12, 6, 13)//
        };
        return Arrays.stream(randomCases).toList();
    }

}
