/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
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

import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.ai.tts.narrator.Narrator;
import de.bushnaq.abdalla.kassandra.ai.tts.narrator.NarratorAttribute;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.OffDaysCalendarComponent;
import de.bushnaq.abdalla.kassandra.ui.dialog.UserWorkWeekDialog;
import de.bushnaq.abdalla.kassandra.ui.dialog.WorkWeekDialog;
import de.bushnaq.abdalla.kassandra.ui.introduction.util.InstructionVideo;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.UserWorkWeekListView;
import de.bushnaq.abdalla.kassandra.ui.view.WorkWeekListView;
import de.bushnaq.abdalla.kassandra.ui.view.util.AboutViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.ProductListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.UserWorkWeekListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.WorkWeekListViewTester;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Introduction video showcasing Work Week management in Kassandra.
 * <p>
 * The video tells the story of Christopher Paul deciding to switch to a 4-day work week
 * (Monday–Thursday) starting June 1st. It covers two pages in sequence:
 * <ol>
 *   <li><b>Work Weeks (admin)</b> — explains the global work week catalog and creates a
 *       new "Mon-Thu 4x8" schedule.</li>
 *   <li><b>User Work Week</b> — explains personal work week history, assigns the new
 *       schedule from June 1st, and shows how the year calendar immediately reflects the
 *       change (Fridays become non-working from that date onwards).</li>
 * </ol>
 */
@Tag("IntroductionVideo")
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=${test.server.port:0}",
                // Disable basic authentication for these tests
                "spring.security.basic.enabled=false"
        }
)
@AutoConfigureMockMvc
@AutoConfigureTestRestTemplate
public class Test_08_UserWorkWeekIntroductionVideo extends AbstractIntroductionVideo {

    /**
     * The date from which the narrator switches to the 4-day schedule.
     */
    private static final LocalDate         FOUR_DAY_START_DATE       = LocalDate.of(2025, 6, 1);
    /**
     * Narrator style used throughout the video.
     */
    public static final  NarratorAttribute NORMAL                    = new NarratorAttribute()
            .withExaggeration(.5f).withCfgWeight(.5f).withTemperature(1f);
    /**
     * Description of the custom 4-day work week.
     */
    private static final String            WORK_WEEK_4X8_DESCRIPTION = "4-day work week, Monday to Thursday, 8-hour days";
    /**
     * Name of the custom 4-day work week created during the video.
     */
    private static final String            WORK_WEEK_4X8_NAME        = "Mon-Thu 4x8";
    @Autowired
    AboutViewTester aboutViewTester;
    @Autowired
    private ProductListViewTester      productListViewTester;
    @Autowired
    private UserWorkWeekListViewTester userWorkWeekListViewTester;
    @Autowired
    private WorkWeekListViewTester     workWeekListViewTester;

    @BeforeAll
    static void beforeAll() {
        StableDiffusionService.setEnabled(true);
        video.setVersion(2);
        video.setTitle("08 Work Weeks in Kassandra");
        video.setDescription("Learn how to create a custom work week schedule and assign it to yourself. "
                + "We follow Christopher Paul as he switches to a 4-day Monday–Thursday work week starting June 1st, "
                + "and see how the calendar immediately reflects the change.");
    }

    /**
     * Records the introduction video for Work Week management.
     *
     * @param randomCase test-case parameters (product/sprint configuration)
     * @param testInfo   JUnit test metadata
     * @throws Exception if navigation, recording, or TTS fails
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void createVideo(RandomCase randomCase, TestInfo testInfo) throws Exception {
        seleniumHandler.setWindowSize(InstructionVideo.VIDEO_WIDTH, InstructionVideo.VIDEO_EXTENDED_HEIGHT);

        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateProductsIfNeeded(testInfo, randomCase);

        Narrator paul = Narrator.withChatterboxTTS("tts/" + testInfo.getTestClass().get().getSimpleName());
        paul.setEnabled(true);
        HumanizedSeleniumHandler.setHumanize(true);

        seleniumHandler.showOverlay(video.getTitle(), InstructionVideo.VIDEO_SUBTITLE);
        startRecording();
        seleniumHandler.wait(3000);
        paul.narrateAsync(NORMAL, "Hi everyone, Christopher Paul here from kassandra.org. Today I'll show you how to manage and use work weeks in Kassandra, and how I switched to a 4-day working week starting June 1st.");
        seleniumHandler.hideOverlay();

        aboutViewTester.login(
                "christopher.paul@kassandra.org", "password",
                "../kassandra.wiki/screenshots/login-view.png",
                testInfo.getTestClass().get().getSimpleName(),
                generateTestCaseName(testInfo));

        //---------------------------------------------------------------------------------------
        // Navigate to Work Weeks admin page
        //---------------------------------------------------------------------------------------

        seleniumHandler.setHighlightEnabled(true);
        paul.narrateAsync(NORMAL, "Let me start by opening the user menu and navigating to the Work Weeks admin page.");
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_MANAGE_WORK_WEEKS);

        //---------------------------------------------------------------------------------------
        // Explain the Work Week catalog
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(WorkWeekListView.PAGE_TITLE);
        paul.narrate(NORMAL, "This is the global Work Week catalog — an admin-only page where you define named work week schedules.");
        paul.narrate(NORMAL, "Kassandra ships with three built-in schedules: the standard Western Monday-to-Friday, and two Sunday-to-Thursday variants used in Islamic and Jewish work cultures.");
        paul.narrate(NORMAL, "These definitions are shared across the whole organisation. Once an admin creates a work week here, any user can be assigned to it.");

        seleniumHandler.highlight(WorkWeekListView.GRID);
        paul.narrate(NORMAL, "Each row in the grid shows the schedule name, a description, which days are marked as working, and how many users are currently on that schedule.");
        paul.narrate(NORMAL, "The user count badge helps admins understand the impact before modifying or deleting a work week.");

        //---------------------------------------------------------------------------------------
        // Create the Mon-Thu 4x8 work week
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "I've decided to switch to a 4-day Monday through Thursday work week starting June 1st. Before I can assign it to myself, I need to define it here in the catalog.");

        paul.narrateAsync(NORMAL, "Click the Create button.");
        seleniumHandler.click(WorkWeekListView.CREATE_BUTTON);

        paul.narrate(NORMAL, "I'll give the schedule a clear name and description.");
        paul.narrateAsync(NORMAL, "Monday to Thursday 4 (8 hour) days per week.");
        seleniumHandler.setTextField(WorkWeekDialog.NAME_FIELD, WORK_WEEK_4X8_NAME);

        paul.narrateAsync(NORMAL, "Four-day work week, Monday to Thursday.");
        seleniumHandler.setTextField(WorkWeekDialog.DESCRIPTION_FIELD, WORK_WEEK_4X8_DESCRIPTION);

        paul.narrate(NORMAL, "The schedule table in the middle of the dialog lets me define exactly which days are working days and what the hours are. I'll enable Monday through Thursday.");
        seleniumHandler.setCheckCheckbox(WorkWeekDialog.WORKING_DAY_CHECK_ID_PREFIX + "monday", true);
        seleniumHandler.setCheckCheckbox(WorkWeekDialog.WORKING_DAY_CHECK_ID_PREFIX + "tuesday", true);
        seleniumHandler.setCheckCheckbox(WorkWeekDialog.WORKING_DAY_CHECK_ID_PREFIX + "wednesday", true);
        seleniumHandler.setCheckCheckbox(WorkWeekDialog.WORKING_DAY_CHECK_ID_PREFIX + "thursday", true);
        paul.narrate(NORMAL, "Friday, Saturday, and Sunday stay unchecked — those will be my new three-day weekend.");

        paul.narrateAsync(NORMAL, "Save the new work week.");
        workWeekListViewTester.closeDialog(WorkWeekDialog.CONFIRM_BUTTON);

        seleniumHandler.wait(500);
        seleniumHandler.highlight(WorkWeekListView.GRID_NAME_PREFIX + WORK_WEEK_4X8_NAME);
        paul.narrate(NORMAL, "The Monday to Thursday 4 (8 hour) days per week schedule is now in the catalog. Notice the user count still shows zero — nobody is assigned to it yet. That's the next step.");

        //---------------------------------------------------------------------------------------
        // Navigate to User Work Week page
        //---------------------------------------------------------------------------------------

        paul.narrateAsync(NORMAL, "Now let me navigate to my personal work week page via the user menu.");
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_WORK_WEEK);

        //---------------------------------------------------------------------------------------
        // Explain the User Work Week page
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserWorkWeekListView.PAGE_TITLE);
        paul.narrate(NORMAL, "This is the User Work Week page. Unlike the admin catalog we just visited, this page is personal — it shows my own work week history.");
        paul.narrate(NORMAL, "The key concept here is history: if my schedule changes over time, Kassandra tracks every transition. That way it always knows which schedule was in effect on any given date, for any capacity or availability calculation.");

        seleniumHandler.highlight(UserWorkWeekListView.GRID);
        paul.narrate(NORMAL, "Right now I have a single entry: the Western 5x8, the standard Monday-to-Friday schedule, assigned from the very beginning of my employment.");
        paul.narrate(NORMAL, "The Start Date column tells Kassandra when each schedule begins. The previous schedule is automatically considered active until the next start date.");

        //---------------------------------------------------------------------------------------
        // Explain the calendar (before assignment)
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "On the right side you can see a full year calendar. This is the same calendar you'll find on other personal pages — it visualises your working and non-working days across the whole year.");
        paul.narrate(NORMAL, "With my current Western 5x8 schedule, Saturdays and Sundays are the only non-working days, shown in a darker colour. Monday through Friday are all standard working days.");
        paul.narrateAsync(NORMAL, "Notice how the weekend pattern is completely uniform — every single week looks the same right now.");

        //---------------------------------------------------------------------------------------
        // Assign the 4-day work week from June 1st
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "Now let me add the new assignment. Starting June 1st, I will switch to the Monday to Thursday 4 (8 hour) days per week schedule.");

        paul.narrateAsync(NORMAL, "Click the Create button to add a new work week assignment.");
        seleniumHandler.click(UserWorkWeekListView.CREATE_BUTTON);

        paul.narrate(NORMAL, "First, the start date, June 1st, twenty twenty five. This is the day my new schedule takes effect.");
        seleniumHandler.setDatePickerValue(UserWorkWeekDialog.START_DATE_FIELD, FOUR_DAY_START_DATE);

        paul.narrate(NORMAL, "Now I select the work week I just created from the dropdown.");
        seleniumHandler.setComboBoxValue(UserWorkWeekDialog.WORK_WEEK_COMBO_FIELD, WORK_WEEK_4X8_NAME);

        paul.narrateAsync(NORMAL, "Save the assignment.");
        userWorkWeekListViewTester.closeDialog(UserWorkWeekDialog.CONFIRM_BUTTON);

        seleniumHandler.wait(500);

        //---------------------------------------------------------------------------------------
        // Explain the updated grid
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserWorkWeekListView.GRID);
        paul.narrate(NORMAL, "I now have two records in my history. The original Western 5x8 entry is still there — it defines my schedule from the beginning up to May 31st.");
        paul.narrate(NORMAL, "The new 4 (8 hour) days per week entry starts June 1st and is my current active schedule going forward.");
        paul.narrate(NORMAL, "This is the power of the history approach: I never lose the record of what my schedule was before. Sprint capacity calculations for sprints before June 1st will still correctly use 5 working days per week.");

        //---------------------------------------------------------------------------------------
        // Show calendar change
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "Now look at the calendar. It has automatically updated to reflect the transition.");
        paul.narrate(NORMAL, "In January through May everything looks the same as before — only Saturdays and Sundays are non-working days.");
        paul.narrate(NORMAL, "But starting from June 1st, Fridays also turn into non-working days, joining the weekend. From June onwards I have a three-day weekend every single week.");
        paul.narrate(NORMAL, "This is exactly what I wanted — the calendar immediately reflects the schedule change at the precise date I specified.");

        paul.narrateAsync(NORMAL, "Let me scroll to next year to confirm the Monday to Thursday schedule continues throughout.");
        seleniumHandler.click(OffDaysCalendarComponent.CALENDAR_NEXT_YEAR_BTN);
        seleniumHandler.waitForElementToBeClickable(UserWorkWeekListView.PAGE_TITLE);
        paul.narrate(NORMAL, "In the following year the Monday to Thursday schedule is active for the entire year. Every Friday is a non-working day, every week has only four working days.");
        seleniumHandler.click(OffDaysCalendarComponent.CALENDAR_PREV_YEAR_BTN);
        seleniumHandler.waitForElementToBeClickable(UserWorkWeekListView.PAGE_TITLE);

        //---------------------------------------------------------------------------------------
        // Closing
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "That is all it takes to manage work weeks in Kassandra. Define the schedule in the global catalog, then assign it to yourself with a start date. The calendar and all capacity calculations update automatically.");
        paul.narrate(NORMAL, "This works just as well for company-wide changes — an admin can create a new schedule and reassign whole teams, and every sprint and availability report will immediately reflect the new reality. Thanks for watching!");

        seleniumHandler.showOverlay(video.getTitle(), InstructionVideo.COPYLEFT_SUBTITLE);
        seleniumHandler.waitUntilBrowserClosed(5000);
    }

    private static List<RandomCase> listRandomCases() {
        // "now" is set to May 5 2025 so that June 1st is a near-future date in the narrative.
        RandomCase[] randomCases = new RandomCase[]{
                new RandomCase(1, OffsetDateTime.parse("2025-05-05T08:00:00+01:00"), LocalDate.parse("2025-04-28"), Duration.ofDays(10), 0, 0, 0, 0, 0, 0, 0, 0, 6, 8, 12, 6, 13)
        };
        return Arrays.stream(randomCases).toList();
    }
}


