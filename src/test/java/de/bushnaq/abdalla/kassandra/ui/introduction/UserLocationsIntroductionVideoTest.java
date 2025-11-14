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
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.OffDaysCalendarComponent;
import de.bushnaq.abdalla.kassandra.ui.dialog.LocationDialog;
import de.bushnaq.abdalla.kassandra.ui.introduction.util.InstructionVideosUtil;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.LocationListView;
import de.bushnaq.abdalla.kassandra.ui.view.LoginView;
import de.bushnaq.abdalla.kassandra.ui.view.util.*;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=8080",
                "spring.profiles.active=test",
                // Disable basic authentication for these tests
                "spring.security.basic.enabled=false"
        }
)
@AutoConfigureMockMvc
@Transactional
public class UserLocationsIntroductionVideoTest extends AbstractKeycloakUiTestUtil {
    public static final NarratorAttribute          INTENSE     = new NarratorAttribute().withExaggeration(.7f).withCfgWeight(.3f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    public static final NarratorAttribute          NORMAL      = new NarratorAttribute().withExaggeration(.5f).withCfgWeight(.5f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    public static final String                     VIDEO_TITLE = "Kassandra User Locations";
    @Autowired
    private             AvailabilityListViewTester availabilityListViewTester;
    @Autowired
    private             FeatureListViewTester      featureListViewTester;
    private             String                     featureName;
    @Autowired
    private             LocationListViewTester     locationListViewTester;
    @Autowired
    private             OffDayListViewTester       offDayListViewTester;
    @Autowired
    private             ProductListViewTester      productListViewTester;
    @Autowired
    private             HumanizedSeleniumHandler   seleniumHandler;
    @Autowired
    private             SprintListViewTester       sprintListViewTester;
    @Autowired
    private             TaskListViewTester         taskListViewTester;
    @Autowired
    private             UserListViewTester         userListViewTester;
    @Autowired
    private             VersionListViewTester      versionListViewTester;

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void createVideo(RandomCase randomCase, TestInfo testInfo) throws Exception {

        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateProductsIfNeeded(testInfo, randomCase);
        Narrator paul = Narrator.withChatterboxTTS("tts/" + testInfo.getTestClass().get().getSimpleName());
        HumanizedSeleniumHandler.setHumanize(true);
        seleniumHandler.getAndCheck("http://localhost:" + "8080" + "/ui/" + LoginView.ROUTE);
        seleniumHandler.showOverlay(VIDEO_TITLE, InstructionVideosUtil.VIDEO_SUBTITLE);
        seleniumHandler.startRecording(InstructionVideosUtil.TARGET_FOLDER, VIDEO_TITLE + " " + InstructionVideosUtil.VIDEO_SUBTITLE);
        seleniumHandler.wait(3000);
        paul.narrateAsync(NORMAL, "Hi everyone, Christopher Paul here from kassandra.org. Today we're going to learn about User Location management in Kassandra. User locations are essential for accurate project planning because they determine which public holidays apply to each team member.");
        seleniumHandler.hideOverlay();
        productListViewTester.switchToProductListViewWithOidc("christopher.paul@kassandra.org", "password", "../kassandra.wiki/screenshots/login-view.png", testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));

        //---------------------------------------------------------------------------------------
        // Navigate to Location Page
        //---------------------------------------------------------------------------------------

        seleniumHandler.setHighlightEnabled(true);//highlight elements starting now
        paul.narrateAsync(NORMAL, "Let's open the user menu and navigate to the User Location page.");
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_LOCATION);

        //---------------------------------------------------------------------------------------
        // Explain Location Page Purpose
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(LocationListView.LOCATION_LIST_PAGE_TITLE);
        paul.narrate(NORMAL, "This page shows your location history. Each location record defines where you were working during a specific time period.");
        paul.narrate(NORMAL, "This is important because Kassandra uses this information to automatically populate the correct public holidays for your region when planning sprints and calculating availability.");

        //---------------------------------------------------------------------------------------
        // Explain Calendar
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "On the right side, you can see a calendar with every day of the current year.");
        seleniumHandler.highlight(OffDaysCalendarComponent.LEGEND_ITEM_ID_PREFIX_BUSINESS_TRIP, OffDaysCalendarComponent.LEGEND_ITEM_ID_PREFIX_SICK_LEAVE, OffDaysCalendarComponent.LEGEND_ITEM_ID_PREFIX_VACATION, OffDaysCalendarComponent.LEGEND_ITEM_ID_PREFIX_HOLIDAY);
        paul.narrate(NORMAL, "The legend on the bottom explains the different types of days and their colors.");
        paul.narrateAsync(NORMAL, "Let's take a look at last year. There is a lot more going on.");
        seleniumHandler.click(OffDaysCalendarComponent.CALENDAR_PREV_YEAR_BTN);
        paul.narrate(NORMAL, "You can see the holidays marked in the calendar, based on my Germany North Rhine Westphalia location.");
        paul.narrate(NORMAL, "When you change your location, the holidays will automatically update to match your new region.");
        seleniumHandler.click(OffDaysCalendarComponent.CALENDAR_NEXT_YEAR_BTN);

        //---------------------------------------------------------------------------------------
        // Explain Existing Records
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(LocationListView.LOCATION_GRID);
        paul.narrate(NORMAL, "On the left, the location grid shows my location history.");
        paul.narrate(NORMAL, "I have one location record starting from my first working day, which is currently active.");

        //---------------------------------------------------------------------------------------
        // Why Multiple Locations
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "Why do we need a location history? Well, if you relocate to a different country or region, the holidays will change.");
        paul.narrate(NORMAL, "For example, if I move from Germany to the United States, Thanksgiving would now count as a day off, but German Unity Day would not.");

        //---------------------------------------------------------------------------------------
        // Create New Location - Intro
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "Let me show you how to add a new location. Imagine I'm planning to relocate to California in September. Let's create that location record now.");

        //---------------------------------------------------------------------------------------
        // Create New Location - Dialog
        //---------------------------------------------------------------------------------------

        paul.narrateAsync(NORMAL, "Click the create button.");
        seleniumHandler.click(LocationListView.CREATE_LOCATION_BUTTON);

        paul.narrate(NORMAL, "First, we select the start date - this is when the new location becomes effective.");
        paul.narrateAsync(NORMAL, "Let's set it to September first, twenty twenty-five.");
        final LocalDate californiaStartDate = LocalDate.of(2025, 9, 1);
        seleniumHandler.setDatePickerValue(LocationDialog.LOCATION_START_DATE_FIELD, californiaStartDate);

        paul.narrateAsync(NORMAL, "Next, we choose the country - United States.");
        seleniumHandler.setComboBoxValue(LocationDialog.LOCATION_COUNTRY_FIELD, "United States (US)");

        paul.narrateAsync(NORMAL, "And finally, the state or region - California.");
        seleniumHandler.setComboBoxValue(LocationDialog.LOCATION_STATE_FIELD, "California (ca)");

        paul.narrateAsync(NORMAL, "Now click Save to create the location record.");
        seleniumHandler.click(LocationDialog.CONFIRM_BUTTON);

        //---------------------------------------------------------------------------------------
        // Verify Creation & Explain Impact
        //---------------------------------------------------------------------------------------

        seleniumHandler.wait(1000);
        seleniumHandler.highlight(LocationListView.LOCATION_GRID_START_DATE_PREFIX + "2025-09-01");
        paul.narrate(NORMAL, "Perfect! The new location record is now visible in the grid.");
        paul.narrate(NORMAL, "Starting September first, Kassandra will use California holidays instead of North Rhine-Westphalia holidays when calculating my availability and planning sprints.");
        paul.narrate(NORMAL, "My old location record automatically ends the day before the new one begins, ensuring there are no gaps or overlaps.");

        //---------------------------------------------------------------------------------------
        // Mention Edit/Delete Capabilities
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "With the little notepad and trashcan icons, on the right side, you can edit or delete any existing location.");

        //---------------------------------------------------------------------------------------
        // Mention Minimum Location Requirement
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(LocationListView.LOCATION_GRID);
        paul.narrate(NORMAL, "However, you cannot delete your only location record - Kassandra requires at least one location to calculate holidays properly.");

        //---------------------------------------------------------------------------------------
        // Closing
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "That's all there is to managing your location in Kassandra. Remember, keeping your location up to date ensures accurate holiday calculations and better project planning. Thanks for watching!");

        seleniumHandler.showOverlay(VIDEO_TITLE, InstructionVideosUtil.COPYLEFT_SUBTITLE);
        seleniumHandler.waitUntilBrowserClosed(5000);
    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(1, LocalDate.parse("2025-05-01"), Duration.ofDays(10), 0, 0, 0, 0, 0, 6, 8, 12, 6, 13)//
        };
        return Arrays.stream(randomCases).toList();
    }

}
