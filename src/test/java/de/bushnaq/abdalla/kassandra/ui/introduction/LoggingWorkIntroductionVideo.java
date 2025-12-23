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
import de.bushnaq.abdalla.kassandra.ui.dialog.WorklogDialog;
import de.bushnaq.abdalla.kassandra.ui.introduction.util.InstructionVideosUtil;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.ActiveSprints;
import de.bushnaq.abdalla.kassandra.ui.view.util.ProductListViewTester;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
public class LoggingWorkIntroductionVideo extends AbstractKeycloakUiTestUtil {
    public static final NarratorAttribute        EXCITED     = new NarratorAttribute().withExaggeration(.7f).withCfgWeight(.3f).withTemperature(1f);
    public static final NarratorAttribute        NORMAL      = new NarratorAttribute().withExaggeration(.6f).withCfgWeight(.2f).withTemperature(1f);
    public static final String                   VIDEO_TITLE = "Logging Work in Kassandra";
    private             String                   featureName;
    @Autowired
    private             ProductListViewTester    productListViewTester;
    private             String                   productName;
    @Autowired
    private             HumanizedSeleniumHandler seleniumHandler;
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

        Narrator grace = Narrator.withChatterboxTTS("tts/" + testInfo.getTestClass().get().getSimpleName(), "grace");
        grace.setSilent(false);

        // Login as Grace Martin
//        seleniumHandler.getAndCheck("http://localhost:" + "8080" + "/ui/" + LoginView.ROUTE);
        productListViewTester.switchToProductListViewWithOidc("grace.martin@kassandra.org", "password", null, null, null);

        // Navigate directly to Active Sprints view
        seleniumHandler.getAndCheck("http://localhost:" + productListViewTester.getPort() + "/ui/" + ActiveSprints.ROUTE);
        seleniumHandler.waitForElementToBeClickable(ActiveSprints.ID_CLEAR_FILTERS_BUTTON);

        HumanizedSeleniumHandler.setHumanize(true);
        seleniumHandler.showOverlay(VIDEO_TITLE, InstructionVideosUtil.VIDEO_SUBTITLE);
        seleniumHandler.startRecording(InstructionVideosUtil.TARGET_FOLDER, VIDEO_TITLE + " " + InstructionVideosUtil.VIDEO_SUBTITLE);
        grace.pause(3000);
        grace.narrateAsync(NORMAL, "Hi everyone, Grace Martin here from kassandra.org. Today I'm going to show you how to log work on your tasks using the Active Sprints view and the Worklog dialog. This is essential for tracking your progress and keeping the team informed about task status.");
        seleniumHandler.hideOverlay();

        //---------------------------------------------------------------------------------------
        // Explain and Demonstrate Filter Controls
        //---------------------------------------------------------------------------------------

        grace.narrate(NORMAL, "First, let me show you the controls at the top of the Active Sprints view. These filters help you focus on the work that matters to you.");
        grace.pause(2000);

        // Demonstrate Search Field
        grace.narrate(NORMAL, "On the left, we have the Search field. You can type here to filter tasks by name, description, or any other text. Let me search for 'controller' to see only tasks related to the controller.");
        grace.pause(2000);

        seleniumHandler.setTextField(ActiveSprints.ID_SEARCH_FIELD, "controller");
        grace.pauseIfSilent(1000);
        grace.pause(2000);

        grace.narrate(NORMAL, "See how the board now shows only tasks matching 'controller'. Now I'll clear this search by clicking the X button.");
        grace.pause(2000);

        seleniumHandler.clickClearButton(ActiveSprints.ID_SEARCH_FIELD);
        grace.pauseIfSilent(1000);
        grace.pause(1500);

        // Demonstrate User Filter
        grace.narrate(NORMAL, "Next is the User filter. You can select one or more team members to see only their tasks. Let me filter to show only my tasks by selecting Grace Martin.");
        grace.pause(2500);

        seleniumHandler.setMultiSelectComboBoxValue(ActiveSprints.ID_USER_SELECTOR, "Grace Martin");
        seleniumHandler.closeMultiSelectComboBoxValue(ActiveSprints.ID_USER_SELECTOR);
        grace.pauseIfSilent(1000);
        grace.pause(2000);

        grace.narrate(NORMAL, "Perfect! Now the board shows only tasks assigned to me.");
        grace.pause(2000);

        // Explain Sprint Filter
        grace.narrate(NORMAL, "Then we have the Sprint filter. If you have multiple active sprints, you can choose which ones to display. By default, the first active sprint is shown. You can select additional sprints from the dropdown.");
        grace.pause(3000);

        // Demonstrate Grouping Mode Selector
        grace.narrate(NORMAL, "The Group by dropdown lets you organize the board in two ways. Right now, we're in Features mode, which groups all stories under their parent features. Let me switch to Stories mode.");
        grace.pause(2500);

        seleniumHandler.setComboBoxValue(ActiveSprints.ID_GROUPING_MODE_SELECTOR, "Stories");
        grace.pauseIfSilent(1000);
        grace.pause(2500);

        grace.narrate(NORMAL, "In Stories mode, each story is shown individually with its own lanes. This gives you a more detailed view. Let me switch back to Features mode for our demo.");
        grace.pause(2500);

        seleniumHandler.setComboBoxValue(ActiveSprints.ID_GROUPING_MODE_SELECTOR, "Features");
        grace.pauseIfSilent(1000);
        grace.pause(2000);

        // Demonstrate Clear Filter Button
        grace.narrate(NORMAL, "And finally, the Clear filter button resets all filters back to their defaults, including search, user selection, and sprint selection. Let me click it now to show you.");
        grace.pause(2500);

        seleniumHandler.highlight(ActiveSprints.ID_CLEAR_FILTERS_BUTTON);
        seleniumHandler.click(ActiveSprints.ID_CLEAR_FILTERS_BUTTON);
        grace.pauseIfSilent(1000);
        grace.pause(1500);

        grace.narrate(NORMAL, "Now let's get to work! I can see my tasks here in the Property request API feature, organized into the Config API Implementation story.");
        grace.pause(2000);

        //---------------------------------------------------------------------------------------
        // Introduction to Active Sprints View
        //---------------------------------------------------------------------------------------

        String inProgressLaneId = VaadinUtil.generateFeatureLaneId(story1.getSprint().getFeature(), TaskStatus.IN_PROGRESS);

        //---------------------------------------------------------------------------------------
        // Moving Task 11 to IN PROGRESS
        //---------------------------------------------------------------------------------------
        {
            grace.narrate(NORMAL, "To start working on a task, I simply drag it from the to-do lane to the in-progress lane. Let me move the 'create controller' task. This tells the team that I've started working on it.");

            // Use real drag and drop - drag task card to IN_PROGRESS lane
            // ActiveSprints defaults to Feature grouping mode, so use feature-based lane ID
            String taskCardId = "task-card-" + task11.getId();

            grace.pause(500);
            seleniumHandler.dragAndDrop(taskCardId, inProgressLaneId);
            grace.pauseIfSilent(1000);
            grace.pause(1500);

            grace.narrate(NORMAL, "Perfect! The task is now in the in-progress lane. Everyone can see that I'm actively working on it.");
            grace.pause(1500);

            //---------------------------------------------------------------------------------------
            // Opening Worklog Dialog
            //---------------------------------------------------------------------------------------

            grace.narrate(NORMAL, "Now, to log the time I've worked on this task, I click on the task card. This opens the Worklog dialog where I can enter details about my work.");

            seleniumHandler.click(taskCardId);
            grace.pauseIfSilent(500);

            // Wait for worklog dialog to appear
            seleniumHandler.waitForElementToBeClickable(WorklogDialog.TITLE_ID);

            grace.pause(2000);

            grace.narrate(NORMAL, "Here's the Worklog dialog. It shows the task I'm logging work for at the top, and provides fields to enter my time and comments.");
            grace.pause(1500);

            //---------------------------------------------------------------------------------------
            // Filling Worklog Fields
            //---------------------------------------------------------------------------------------

            grace.narrate(NORMAL, "I'll enter the time I spent today. Let's say 1 hours. Notice how the Time Remaining field automatically updates based on what I enter.");

            seleniumHandler.setTextField(WorklogDialog.TIME_SPENT_FIELD, "1h");
            grace.pauseIfSilent(1000);
            grace.pause(2000);

            grace.narrate(NORMAL, "See that? Originally this task had 4 hours remaining, and now it shows 3 hours after logging 1 hours of work. The system automatically calculates the burndown for us!");
            grace.pause(2000);

            grace.narrate(NORMAL, "In the comment field, I'll add a description of what I did. This helps the team understand what progress was made.");

            seleniumHandler.setTextArea(WorklogDialog.COMMENT_FIELD, "Implemented basic controller structure and routing");
            grace.pauseIfSilent(1000);
            grace.pause(2000);

            //---------------------------------------------------------------------------------------
            // Saving Worklog
            //---------------------------------------------------------------------------------------

            grace.narrate(NORMAL, "Now I'll click Save to record this worklog entry.");

            seleniumHandler.click(WorklogDialog.SAVE_BUTTON);
            grace.pauseIfSilent(500);

            // Wait for dialog to close
            seleniumHandler.waitForElementInvisibility(WorklogDialog.WORKLOG_DIALOG);
            grace.pause(1500);

            grace.narrate(NORMAL, "The dialog closes and we're back at the scrum board.");
            grace.pause(1000);

            //---------------------------------------------------------------------------------------
            // Show Results
            //---------------------------------------------------------------------------------------

            grace.narrate(NORMAL, "Look at the task card now. The remaining work has been updated to 3 hours. This is your burndown in action! The team can see that real progress is being made on this task.");
            grace.pause(2500);

            grace.narrate(NORMAL, "This information feeds into the sprint burndown chart, giving everyone visibility into how the sprint is progressing. It helps us track velocity and understand if we're on track to complete our commitments.");
            grace.pause(2000);
        }

        //---------------------------------------------------------------------------------------
        // Logging More Work on Another Task (Optional)
        //---------------------------------------------------------------------------------------
        {

            grace.narrate(NORMAL, "Let me show you one more example. I'll move another task to in-progress and log some work on it too.");

            // Use real drag and drop for second task
            String task12CardId = "task-card-" + task12.getId();
            seleniumHandler.dragAndDrop(task12CardId, inProgressLaneId);
            grace.pauseIfSilent(1000);
            grace.pause(1500);

            grace.narrate(NORMAL, "Now I'll click on the API documentation task to log work.");
            seleniumHandler.click(task12CardId);
            grace.pauseIfSilent(500);
            seleniumHandler.waitForElementToBeClickable(WorklogDialog.TITLE_ID);
            grace.pause(1500);

            grace.narrate(NORMAL, "I'll log 1 hour of work on this task.");
            seleniumHandler.setTextField(WorklogDialog.TIME_SPENT_FIELD, "1h");
            grace.pauseIfSilent(1000);
            grace.pause(1500);

            grace.narrate(NORMAL, "Notice that the system automatically reduced the remaining time. But wait - sometimes you spend time on a task without actually making progress. Maybe you hit unexpected complexity or had to research something.");
            grace.pause(3000);

            grace.narrate(NORMAL, "In this case, I spent an hour but realized the task will take longer than expected. So I need to adjust the remaining time back to 2 hours to reflect the actual work left.");
            grace.pause(2500);

            seleniumHandler.setTextField(WorklogDialog.TIME_REMAINING_FIELD, "2h");
            grace.pauseIfSilent(1000);
            grace.pause(1500);

            grace.narrate(NORMAL, "This is important for accurate tracking. We're being honest about both the time spent and the work remaining. This helps the team make better planning decisions.");
            grace.pause(2500);

            grace.narrate(NORMAL, "Now I'll add a comment explaining what happened.");
            seleniumHandler.setTextArea(WorklogDialog.COMMENT_FIELD, "Researched documentation approach - task more complex than expected");
            grace.pauseIfSilent(1000);
            grace.pause(2000);

            grace.narrate(NORMAL, "And I'll save this worklog entry.");
            seleniumHandler.click(WorklogDialog.SAVE_BUTTON);
            grace.pauseIfSilent(500);
            seleniumHandler.waitForElementInvisibility(WorklogDialog.WORKLOG_DIALOG);
            grace.pause(1500);

            grace.narrate(NORMAL, "Perfect! The task card still shows 2 hours remaining, accurately reflecting the reality. This is exactly how you track your daily progress in Kassandra - honestly and transparently.");
            grace.pause(2500);
        }

        //---------------------------------------------------------------------------------------
        // Complete a Task and Move to DONE
        //---------------------------------------------------------------------------------------
        {
            grace.narrate(NORMAL, "Now let me show you what happens when you complete a task. I'll work on the error handling task and finish it completely.");
            grace.pause(2000);

            // Drag the third task to IN_PROGRESS
            String task13CardId = "task-card-" + task13.getId();
            seleniumHandler.dragAndDrop(task13CardId, inProgressLaneId);
            grace.pauseIfSilent(1000);
            grace.pause(1500);

            grace.narrate(NORMAL, "I've moved the error handling task to in-progress. Notice something important: the story itself has now moved to the in-progress lane! This happened because all tasks in the story are now either in-progress or done - none are left in the to-do lane.");
            grace.pause(3500);

            grace.narrate(NORMAL, "The story status automatically changes from to-do to in-progress when all of its tasks have been started. This gives you a quick overview of which stories are actively being worked on.");
            grace.pause(3000);

            grace.narrate(NORMAL, "Now I'll log the work I did to complete this task.");
            grace.pause(2000);

            // Open worklog dialog
            seleniumHandler.click(task13CardId);
            grace.pauseIfSilent(500);
            seleniumHandler.waitForElementToBeClickable(WorklogDialog.TITLE_ID);
            grace.pause(1500);

            grace.narrate(NORMAL, "This task has 5 hours remaining. I'll log 5 hours of work to complete it entirely.");
            grace.pause(2000);

            seleniumHandler.setTextField(WorklogDialog.TIME_SPENT_FIELD, "5h");
            grace.pauseIfSilent(1000);
            grace.pause(2000);

            grace.narrate(NORMAL, "Notice how the remaining time automatically goes to zero. This means the task is now complete.");
            grace.pause(2000);

            seleniumHandler.setTextArea(WorklogDialog.COMMENT_FIELD, "Completed error handling implementation and testing");
            grace.pauseIfSilent(1000);
            grace.pause(1500);

            seleniumHandler.click(WorklogDialog.SAVE_BUTTON);
            grace.pauseIfSilent(500);
            seleniumHandler.waitForElementInvisibility(WorklogDialog.WORKLOG_DIALOG);
            grace.pause(1500);

            grace.narrate(NORMAL, "Now I'll move this completed task to the done lane by dragging it.");
            grace.pause(2000);

            // Drag task to DONE lane
            String doneLaneId = VaadinUtil.generateFeatureLaneId(story1.getSprint().getFeature(), TaskStatus.DONE);
            seleniumHandler.dragAndDrop(task13CardId, doneLaneId);
            grace.pauseIfSilent(1000);
            grace.pause(2000);

            grace.narrate(NORMAL, "Perfect! The task is now in the done lane, showing it's complete. The story remains in the in-progress lane because we still have other tasks in progress.");
            grace.pause(3000);
        }

        //---------------------------------------------------------------------------------------
        // Closing
        //---------------------------------------------------------------------------------------

        grace.narrate(NORMAL, "Logging work regularly like this is crucial for accurate sprint tracking and helps the team understand velocity and capacity. It also provides a history of what was done and when, which is invaluable for retrospectives and future planning.");
        grace.pause(2500);

        grace.narrate(NORMAL, "That's how easy it is to log work in Kassandra! Remember to log your time regularly to keep everyone informed about progress. Thanks for watching!");
        grace.pause(1000);

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

        // Set sprint status to STARTED to make it active
        sprint.setStatus(Status.STARTED);
        // Ensure sprint has feature reference for lane ID generation
        sprint.setFeature(feature);
        sprintApi.update(sprint);

        User graceMartin = userApi.getByEmail("grace.martin@kassandra.org");

        // Create a start milestone
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

