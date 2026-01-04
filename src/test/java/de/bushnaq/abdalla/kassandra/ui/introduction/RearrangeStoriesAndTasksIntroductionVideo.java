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
import de.bushnaq.abdalla.kassandra.ui.view.SprintListView;
import de.bushnaq.abdalla.kassandra.ui.view.TaskListView;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional
public class RearrangeStoriesAndTasksIntroductionVideo extends AbstractKeycloakUiTestUtil {
    public static final NarratorAttribute        EXCITED       = new NarratorAttribute().withExaggeration(.7f).withCfgWeight(.3f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    public static final String                   NEW_MILESTONE = "New Milestone-";
    public static final String                   NEW_STORY     = "New Story-";
    public static final String                   NEW_TASK      = "New Task-";
    public static final NarratorAttribute        NORMAL        = new NarratorAttribute().withExaggeration(.5f).withCfgWeight(.5f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    public static final String                   VIDEO_TITLE   = "Rearranging Stories and Tasks";
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
    private             Task                     task11;
    private             Task                     task12;
    private             Task                     task13;
    private             Task                     task21;
    private             Task                     task22;
    private             Task                     task23;
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
        seleniumHandler.waitForElementToBeClickable(TaskListView.TASK_LIST_PAGE_TITLE_ID);

        HumanizedSeleniumHandler.setHumanize(true);
        seleniumHandler.showOverlay(VIDEO_TITLE, InstructionVideosUtil.VIDEO_SUBTITLE);
        seleniumHandler.startRecording(InstructionVideosUtil.TARGET_FOLDER, VIDEO_TITLE + " " + InstructionVideosUtil.VIDEO_SUBTITLE);
        paul.pause(3000);
        paul.narrateAsync(NORMAL, "Hi everyone, Christopher Paul here from kassandra.org. Today we're going to learn how to rearrange and copy stories and tasks. By rearranging we mean changing the order in the list to change the priority of a story or task, or moving a task from a story to another story. We'll also see how to quickly duplicate stories with all their child tasks using copy and paste.");
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

        seleniumHandler.setMouseMoveStepsMultiplier(2.0);
        seleniumHandler.setMouseMoveDelayMultiplier(2);
        // Move Story-5 before Story-1
        paul.narrate(NORMAL, "Now, let's see how we can rearrange stories and tasks using drag and drop. First, we'll move the second story before first story to prioritize it higher in our sprint backlog. So, first the persistence, then the API");
        seleniumHandler.dragAndDropAbove(TaskGrid.TASK_GRID_NAME_PREFIX + story2.getName(), TaskGrid.TASK_GRID_NAME_PREFIX + story1.getName());

        paul.narrate(NORMAL, "Next, lets move the story back.");
        paul.pauseIfSilent(500);
        seleniumHandler.dragAndDropBelow(TaskGrid.TASK_GRID_NAME_PREFIX + story2.getName(), TaskGrid.TASK_GRID_NAME_PREFIX + story1.getName());
        paul.narrate(NORMAL, "Have you noticed that Kassandra only allowed me to drop the story above or below another story and that the child tasks always follow the story? This makes rearranging blocks of work really easy.");

        paul.narrate(NORMAL, "Great! Now, let's rearrange some tasks within the API story. I like open API first approach. We'll move API documentation before creating the controller.");
        paul.pauseIfSilent(500);
        seleniumHandler.dragAndDropAbove(TaskGrid.TASK_GRID_NAME_PREFIX + task12.getName(), TaskGrid.TASK_GRID_NAME_PREFIX + task11.getName());

        paul.narrate(NORMAL, "Finally, let's say we need to address error handling first to ensure robustness. We'll move API error handling to the top of our task list into the persistence story.");
        paul.pauseIfSilent(500);
        seleniumHandler.dragAndDropAbove(TaskGrid.TASK_GRID_NAME_PREFIX + task13.getName(), TaskGrid.TASK_GRID_NAME_PREFIX + task21.getName());

        paul.narrate(NORMAL, "Lets redo that. We do not want API related tasks in the story that is all about persistence.");
        paul.pauseIfSilent(500);
        seleniumHandler.dragAndDropBelow(TaskGrid.TASK_GRID_NAME_PREFIX + task13.getName(), TaskGrid.TASK_GRID_NAME_PREFIX + task11.getName());

        //---------------------------------------------------------------------------------------
        // Copy/Paste Introduction
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "Now, let's learn about another powerful feature: copying tasks and stories. Sometimes you need to duplicate a story with all its child tasks. Kassandra makes this incredibly easy with copy and paste.");

        paul.narrate(NORMAL, "First, I'll select the first story, Config API implementation, by clicking on it.");
        paul.pauseIfSilent(500);
//        seleniumHandler.waitUntilBrowserClosed(0);
        seleniumHandler.selectGridRow(TaskGrid.TASK_GRID_ID_PREFIX, TaskListView.class, story1.getName());

        paul.pause(500);

        paul.narrate(NORMAL, "Now, I'll press control plus C to copy the story.");
        seleniumHandler.copy();
        paul.pauseIfSilent(1000);
        paul.pause(1000);

        paul.narrate(NORMAL, "And now, control plus V to paste it.");
        seleniumHandler.past();
        paul.pauseIfSilent(1000);
        paul.pause(1500);

        paul.narrate(NORMAL, "Notice how Kassandra automatically copied the entire story along with all three of its child tasks to the end of the grid. The copied story and tasks are exact duplicates, ready for you to modify as needed.");
        paul.pause(1000);

        paul.narrate(NORMAL, "This copy and paste feature is perfect for managing project templates. You can quickly build your project duplicate your work and then adjust the details.");

        //---------------------------------------------------------------------------------------
        // Edit Copied Story and Tasks
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "Let's now customize the copied story and tasks to make them unique.");

        // Define new names for the copied story and tasks
        String copiedStory1Name = "Config api implementation (copy)";
        String copiedTask11Name = "create controller (copy)";
        String copiedTask12Name = "api documentation (copy)";
        String copiedTask13Name = "api error handling (copy)";

        paul.narrate(NORMAL, "First, we'll enter edit mode to modify the copied items.");
        seleniumHandler.click(TaskListView.EDIT_BUTTON_ID);
        paul.pauseIfSilent(500);
        seleniumHandler.waitForPageLoaded();

        paul.narrate(NORMAL, "Let's rename the copied story to Config security implementation.");
        seleniumHandler.setTextField(TaskGrid.TASK_GRID_NAME_PREFIX + copiedStory1Name, "Config security implementation");

        paul.narrate(NORMAL, "Now we'll update the tasks to match the security theme.");
        seleniumHandler.setTextField(TaskGrid.TASK_GRID_NAME_PREFIX + copiedTask11Name, "implement authentication");

        seleniumHandler.setTextField(TaskGrid.TASK_GRID_NAME_PREFIX + copiedTask12Name, "security documentation");

        seleniumHandler.setTextField(TaskGrid.TASK_GRID_NAME_PREFIX + copiedTask13Name, "security audit");

        seleniumHandler.click(TaskListView.SAVE_BUTTON_ID);

        // Update the names to the new values
        copiedStory1Name = "Config security implementation";
        copiedTask11Name = "implement authentication";
        copiedTask12Name = "security documentation";
        copiedTask13Name = "security audit";

        paul.narrate(NORMAL, "Now let's ask Grace to update the effort estimates for these new security tasks.");
        paul.narrate(NORMAL, "Grace, can you help us estimate the effort for the security tasks?");

        grace.narrate(NORMAL, "Sure, Christopher! Let me update the estimates.").pause();
        grace.narrate(NORMAL, "Let's go to edit mode.");

        seleniumHandler.click(TaskListView.EDIT_BUTTON_ID);
        seleniumHandler.waitForPageLoaded();
        grace.pause(500);

        grace.narrateAsync(NORMAL, "Authentication will need more time. Let's set minimum to 6 hours and maximum to 8 hours.");
        seleniumHandler.setTextField(TaskGrid.TASK_GRID_MIN_EST_PREFIX + copiedTask11Name, "6h");
        seleniumHandler.setTextField(TaskGrid.TASK_GRID_MAX_EST_PREFIX + copiedTask11Name, "8h");

        grace.narrateAsync(NORMAL, "Security documentation is quick. Minimum 1 hour and maximum 2 hours.");
        seleniumHandler.setTextField(TaskGrid.TASK_GRID_MIN_EST_PREFIX + copiedTask12Name, "1h");
        seleniumHandler.setTextField(TaskGrid.TASK_GRID_MAX_EST_PREFIX + copiedTask12Name, "2h");

        grace.narrateAsync(NORMAL, "Security audit requires thorough testing. Minimum 8 hours and maximum 10 hours.");
        seleniumHandler.setTextField(TaskGrid.TASK_GRID_MIN_EST_PREFIX + copiedTask13Name, "8h");
        seleniumHandler.setTextField(TaskGrid.TASK_GRID_MAX_EST_PREFIX + copiedTask13Name, "10h");

        seleniumHandler.click(TaskListView.SAVE_BUTTON_ID);

        //---------------------------------------------------------------------------------------
        // Closing
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "That's all there is to rearranging and copying stories and tasks in Kassandra. Thanks for watching!");

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


        return sprint;
    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(1, OffsetDateTime.parse("2025-08-11T08:00:00+01:00"), LocalDate.parse("2025-08-04"), Duration.ofDays(10), 0, 0, 0, 0, 0, 0, 0, 0, 6, 8, 12, 6, 13)//
        };
        return Arrays.stream(randomCases).toList();
    }

}
