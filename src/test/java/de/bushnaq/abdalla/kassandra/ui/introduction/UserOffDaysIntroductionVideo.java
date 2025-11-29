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
import de.bushnaq.abdalla.kassandra.dto.OffDayType;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.OffDaysCalendarComponent;
import de.bushnaq.abdalla.kassandra.ui.dialog.OffDayDialog;
import de.bushnaq.abdalla.kassandra.ui.introduction.util.InstructionVideosUtil;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.LoginView;
import de.bushnaq.abdalla.kassandra.ui.view.OffDayListView;
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

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
public class UserOffDaysIntroductionVideo extends AbstractKeycloakUiTestUtil {
    public static final NarratorAttribute          INTENSE     = new NarratorAttribute().withExaggeration(.7f).withCfgWeight(.3f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    public static final NarratorAttribute          NORMAL      = new NarratorAttribute().withExaggeration(.5f).withCfgWeight(.5f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    public static final String                     VIDEO_TITLE = "Kassandra User Off Days";
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
        seleniumHandler.setWindowSize(InstructionVideosUtil.VIDEO_WIDTH, InstructionVideosUtil.VIDEO_HEIGHT);

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
        paul.narrateAsync(NORMAL, "Hi everyone, Christopher Paul here from kassandra.org. Today I am happy to release our first instruction video for our Kassandra project management server.");
        seleniumHandler.hideOverlay();
        productListViewTester.switchToProductListViewWithOidc("christopher.paul@kassandra.org", "password", "../kassandra.wiki/screenshots/login-view.png", testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));

        //---------------------------------------------------------------------------------------..
        // Products Page
        //---------------------------------------------------------------------------------------..

        seleniumHandler.setHighlightEnabled(true);//highlight elements starting now
        paul.narrate(NORMAL, "We are going to learn about User Off-Days management in Kassandra. By User Off-Days we mean vacations, sick leaves, or business trips. Basically any day where you are not available to work on your project tasks.");
        paul.narrateAsync(NORMAL, "Lets open the user menu and switch to the User Off-Days page.");
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_OFF_DAYS);


        seleniumHandler.highlight(OffDayListView.OFFDAY_LIST_PAGE_TITLE);
        paul.narrate(NORMAL, "The User Off-Days shows a calendar with every day of the current year.");
        seleniumHandler.highlight(OffDaysCalendarComponent.LEGEND_ITEM_ID_PREFIX_BUSINESS_TRIP, OffDaysCalendarComponent.LEGEND_ITEM_ID_PREFIX_SICK_LEAVE, OffDaysCalendarComponent.LEGEND_ITEM_ID_PREFIX_VACATION, OffDaysCalendarComponent.LEGEND_ITEM_ID_PREFIX_HOLIDAY);
        paul.narrate(NORMAL, "The legend on the bottom explains the different types of User Off-Days and their colors.");
        paul.narrateAsync(NORMAL, "Lets take a look at previous year. There is a lot more going on");
        seleniumHandler.click(OffDaysCalendarComponent.CALENDAR_PREV_YEAR_BTN);
        paul.narrate(NORMAL, "You can see that there are holidays marked in the calendar. These are public holidays that Kassandra automatically populated based on the logged in user location.");
        paul.narrate(NORMAL, "The vacation, business trip and sick leave days are all created by me.");
        seleniumHandler.click(OffDaysCalendarComponent.CALENDAR_NEXT_YEAR_BTN);
        paul.narrate(NORMAL, "I am located in Germany North Rhine Westphalia and so you can see the holidays of that region in Germany.");
        paul.narrate(NORMAL, "Ok, i just noticed, that I have not taken any vacation days this year. So let me plan my summer vacation.");
        paul.narrateAsync(NORMAL, "Select the create button.");
        seleniumHandler.click(OffDayListView.CREATE_OFFDAY_BUTTON);

        paul.narrateAsync(NORMAL, "Choose vacation.");
        seleniumHandler.setComboBoxValue(OffDayDialog.OFFDAY_TYPE_FIELD, OffDayType.VACATION.name());

        paul.narrateAsync(NORMAL, "Select the start date.");
        final LocalDate firstDay = LocalDate.of(2025, 6, 2);
        seleniumHandler.setDatePickerValue(OffDayDialog.OFFDAY_START_DATE_FIELD, firstDay);

        paul.narrate(NORMAL, "and select the end date.");
        paul.narrateAsync(NORMAL, "I am really looking forward to a long summer vacation with my family.");
        final LocalDate lastDay = LocalDate.of(2025, 6, 27);
        seleniumHandler.setDatePickerValue(OffDayDialog.OFFDAY_END_DATE_FIELD, lastDay);
        paul.narrateAsync(NORMAL, "Select Save to close the dialog and persist our vacation.").pause();
        seleniumHandler.click(OffDayDialog.CONFIRM_BUTTON);
        paul.narrate(NORMAL, "the vacation immediately becomes visible in the calendar. Kassandra skips any weekend day or holiday that falls in your vacation.").pause();

        seleniumHandler.showOverlay(VIDEO_TITLE, InstructionVideosUtil.COPYLEFT_SUBTITLE);
        seleniumHandler.waitUntilBrowserClosed(5000);
    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(1, OffsetDateTime.parse("2025-08-11T08:00:00+01:00"), LocalDate.parse("2025-08-04"), Duration.ofDays(10), 0, 0, 0, 0, 0, 0, 0, 0, 6, 8, 12, 6, 13)//
        };
        return Arrays.stream(randomCases).toList();
    }


}
