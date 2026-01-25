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

import de.bushnaq.abdalla.kassandra.ai.narrator.Narrator;
import de.bushnaq.abdalla.kassandra.ai.narrator.NarratorAttribute;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.ui.component.TaskGrid;
import de.bushnaq.abdalla.kassandra.ui.introduction.util.InstructionVideosUtil;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.Backlog;
import de.bushnaq.abdalla.kassandra.ui.view.SprintListView;
import de.bushnaq.abdalla.kassandra.ui.view.util.FeatureListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.ProductListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.SprintListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.VersionListViewTester;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Tag("IntroductionVideo")
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
@AutoConfigureTestRestTemplate
//@Transactional
public class StoryAndTaskRelationsIntroductionVideo extends AbstractKeycloakUiTestUtil {
    public static final NarratorAttribute        EXCITED       = new NarratorAttribute().withExaggeration(.7f).withCfgWeight(.3f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    public static final String                   NEW_MILESTONE = "New Milestone-";
    public static final String                   NEW_STORY     = "New Story-";
    public static final String                   NEW_TASK      = "New Task-";
    public static final NarratorAttribute        NORMAL        = new NarratorAttribute().withExaggeration(.5f).withCfgWeight(.5f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    public static final String                   VIDEO_TITLE   = "Story and Task Relations";
    @Autowired
    private             FeatureListViewTester    featureListViewTester;
    private             String                   featureName;
    @Autowired
    private             ProductListViewTester    productListViewTester;
    private             String                   productName;
    @Autowired
    private             HumanizedSeleniumHandler seleniumHandler;
    @Autowired
    private             SprintListViewTester     sprintListViewTester;
    private             String                   sprintName;
    private             Task                     story1;
    private             Task                     story2;
    private             Task                     story3;
    private             Task                     task11;
    private             Task                     task12;
    private             Task                     task13;
    private             Task                     task21;
    private             Task                     task22;
    private             Task                     task23;
    private             Task                     task31;
    private             Task                     task32;
    private             Task                     task33;
    @Autowired
    private             VersionListViewTester    versionListViewTester;
    private             String                   versionName;

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void createVideo(RandomCase randomCase, TestInfo testInfo) throws Exception {
        //generate the users
        seleniumHandler.setWindowSize(InstructionVideosUtil.VIDEO_WIDTH, InstructionVideosUtil.VIDEO_HEIGHT);
        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateProductsIfNeeded(testInfo, randomCase);

        Sprint sprint = generateData();

        Narrator paul = Narrator.withChatterboxTTS("tts/" + testInfo.getTestClass().get().getSimpleName());
        paul.setSilent(false);
        Narrator grace = Narrator.withChatterboxTTS("tts/" + testInfo.getTestClass().get().getSimpleName(), "grace");
        grace.setSilent(false);
        //seleniumHandler.getAndCheck("http://localhost:" + "8080" + "/ui/" + LoginView.ROUTE);
        productListViewTester.switchToProductListViewWithOidc("christopher.paul@kassandra.org", "password", null, null, null);

        // navigate to the sprint's task list view
        productListViewTester.selectProduct(productName);
        versionListViewTester.selectVersion(versionName);
        featureListViewTester.selectFeature(featureName);
        seleniumHandler.click(SprintListView.SPRINT_GRID_CONFIG_BUTTON_PREFIX + sprintName);
        seleniumHandler.waitForElementToBeClickable(Backlog.CLEAR_FILTER_BUTTON_ID);

        HumanizedSeleniumHandler.setHumanize(true);
        seleniumHandler.showOverlay(VIDEO_TITLE, InstructionVideosUtil.VIDEO_SUBTITLE);
        seleniumHandler.startRecording(InstructionVideosUtil.TARGET_FOLDER, VIDEO_TITLE + " " + InstructionVideosUtil.VIDEO_SUBTITLE);
        paul.pause(3000);
        paul.narrateAsync(NORMAL, "Hi everyone, Christopher Paul here from kassandra.org. Today we're going to learn about task and story dependencies. Dependencies let you define relationships between tasks and stories, ensuring that one task can only start after another task is finished. This is crucial for managing complex workflows and project schedules.");
        seleniumHandler.hideOverlay();

        //---------------------------------------------------------------------------------------
        // Introduction to Dependencies
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "Let's look at our current sprint. We have three stories: Config API implementation, Config persistence implementation, and Config security implementation. Each story has its own tasks.");
        paul.pause(1000);

        paul.narrate(NORMAL, "Now, imagine we can't start working on the API until the persistence layer is complete. In Kassandra, we can create a dependency to represent this relationship.");

        //---------------------------------------------------------------------------------------
        // Creating Story Dependencies
        //---------------------------------------------------------------------------------------

        seleniumHandler.setMouseMoveStepsMultiplier(2.0);
        seleniumHandler.setMouseMoveDelayMultiplier(2);

        paul.narrate(NORMAL, "To create a dependency, we hold down the Control key and drag one task or story onto another. Let's make the API story depend on the persistence story.");

        paul.narrate(NORMAL, "First, I'll hold down Control and drag the API story onto the persistence story.");
        seleniumHandler.dragAndDropWithControl(TaskGrid.TASK_GRID_NAME_PREFIX + story1.getName(), TaskGrid.TASK_GRID_NAME_PREFIX + story2.getName());
        paul.pauseIfSilent(1000);
        paul.pause(1500);

        paul.narrate(NORMAL, "Notice in the Dependency column, the API story now shows the ID of the persistence story. This means the API story can only start after the persistence story is finished.");
        paul.pause(1500);

        //---------------------------------------------------------------------------------------
        // Creating Task Dependencies
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "We can also create dependencies between individual tasks. Let's say we need to create the controller before we can write the API documentation.");

        paul.narrate(NORMAL, "I'll hold Control and drag the API documentation task onto the create controller task.");
        seleniumHandler.dragAndDropWithControl(TaskGrid.TASK_GRID_NAME_PREFIX + task12.getName(), TaskGrid.TASK_GRID_NAME_PREFIX + task11.getName());
        paul.pauseIfSilent(1000);
        paul.pause(1500);

        paul.narrate(NORMAL, "Perfect! Now the API documentation task depends on the create controller task.");
        paul.pause(1000);

        //---------------------------------------------------------------------------------------
        // Cross-Story Dependencies
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "Dependencies can even cross story boundaries. Let's say the security audit task depends on the API error handling task being complete.");

        paul.narrate(NORMAL, "I'll hold Control and drag the security audit task from the security story onto the API error handling task in the API story.");
        seleniumHandler.dragAndDropWithControl(TaskGrid.TASK_GRID_NAME_PREFIX + task33.getName(), TaskGrid.TASK_GRID_NAME_PREFIX + task13.getName());
        paul.pauseIfSilent(1000);
        paul.pause(1500);

        paul.narrate(NORMAL, "Excellent! Now the security audit won't start until the API error handling is complete. This gives us a clear path through our work.");
        paul.pause(1000);

        //---------------------------------------------------------------------------------------
        // Removing Dependencies
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "What if we need to remove a dependency? It's just as easy. We simply hold Control and drag the task onto the same predecessor again. It acts like a toggle.");

        paul.narrate(NORMAL, "Let's remove the dependency between the security audit and API error handling.");
        seleniumHandler.dragAndDropWithControl(TaskGrid.TASK_GRID_NAME_PREFIX + task33.getName(), TaskGrid.TASK_GRID_NAME_PREFIX + task13.getName());
        paul.pauseIfSilent(1000);
        paul.pause(1500);

        paul.narrate(NORMAL, "See? The dependency is gone. The Dependency column for the security audit task is now empty again.");
        paul.pause(1000);

        //---------------------------------------------------------------------------------------
        // Multiple Dependencies
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "Tasks can have multiple dependencies. Let's make the security implementation story depend on both the API and persistence stories.");

        paul.narrate(NORMAL, "I'll add a dependency from the security story to the API story.");
        seleniumHandler.dragAndDropWithControl(TaskGrid.TASK_GRID_NAME_PREFIX + story3.getName(), TaskGrid.TASK_GRID_NAME_PREFIX + story1.getName());
        paul.pauseIfSilent(1000);
        paul.pause(1500);

        paul.narrate(NORMAL, "And now I'll add another dependency from the security story to the persistence story.");
        seleniumHandler.dragAndDropWithControl(TaskGrid.TASK_GRID_NAME_PREFIX + story3.getName(), TaskGrid.TASK_GRID_NAME_PREFIX + story2.getName());
        paul.pauseIfSilent(1000);
        paul.pause(1500);

        paul.narrate(NORMAL, "Perfect! Now the security story has two dependencies. It can only start once both the API and persistence stories are complete. This is really powerful for managing complex project dependencies.");
        paul.pause(1500);

        paul.narrate(NORMAL, "And that's all there is to managing task and story dependencies in Kassandra! Remember, hold Control and drag to create or remove dependencies. This feature helps you build realistic project schedules and ensures work happens in the right order. Thanks for watching!");
        paul.pause(1000);

        seleniumHandler.showOverlay(VIDEO_TITLE, InstructionVideosUtil.COPYLEFT_SUBTITLE);
        seleniumHandler.waitUntilBrowserClosed(5000);
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
            Task          startMilestone = addTask(sprint, null, "Start", startDateTime, Duration.ZERO, null, null, null, TaskMode.MANUALLY_SCHEDULED, true);
        }
        {
            story1 = addParentTask("Config api implementation", sprint, null, null);
            task11 = addTask("create controller", "4h", "6h", graceMartin, sprint, story1, null);
            task12 = addTask("api documentation", "2h", "3h", graceMartin, sprint, story1, null);
            task13 = addTask("api error handling", "5h", "7h", graceMartin, sprint, story1, null);
        }
        {
            story2 = addParentTask("Config persistence implementation", sprint, null, null);
            task21 = addTask("create repository", "4h", "6h", graceMartin, sprint, story2, null);
            task22 = addTask("schema documentation", "2h", "3h", graceMartin, sprint, story2, null);
            task23 = addTask("persistence error handling", "5h", "7h", graceMartin, sprint, story2, null);
        }
        {
            story3 = addParentTask("Config security implementation", sprint, null, null);
            task31 = addTask("implement authentication", "6h", "8h", graceMartin, sprint, story3, null);
            task32 = addTask("security documentation", "1h", "2h", graceMartin, sprint, story3, null);
            task33 = addTask("security audit", "8h", "10h", graceMartin, sprint, story3, null);
        }


        return sprint;
    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(1, OffsetDateTime.parse("2025-08-11T08:00:00+01:00"), LocalDate.parse("2025-08-04"), Duration.ofDays(10), 0, 0, 0, 0, 0, 0, 0, 0, 6, 8, 12, 6, 13)//
        };
        return Arrays.stream(randomCases).toList();
    }

}
