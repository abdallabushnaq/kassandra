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
import de.bushnaq.abdalla.kassandra.ui.dialog.UserDialog;
import de.bushnaq.abdalla.kassandra.ui.introduction.util.InstructionVideosUtil;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.UserListView;
import de.bushnaq.abdalla.kassandra.ui.view.util.ProductListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.UserListViewTester;
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
                // Disable basic authentication for these tests
                "spring.security.basic.enabled=false"
        }
)
@AutoConfigureMockMvc
@Transactional
public class ManagingUsersIntroductionVideo extends AbstractKeycloakUiTestUtil {
    public static final NarratorAttribute        INTENSE     = new NarratorAttribute().withExaggeration(.7f).withCfgWeight(.3f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    public static final NarratorAttribute        NORMAL      = new NarratorAttribute().withExaggeration(.5f).withCfgWeight(.5f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    public static final String                   VIDEO_TITLE = "Managing Users";
    @Autowired
    private             ProductListViewTester    productListViewTester;
    @Autowired
    private             HumanizedSeleniumHandler seleniumHandler;
    @Autowired
    private             UserListViewTester       userListViewTester;

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
        //seleniumHandler.getAndCheck("http://localhost:" + "8080" + "/ui/" + LoginView.ROUTE);
        seleniumHandler.showOverlay(VIDEO_TITLE, InstructionVideosUtil.VIDEO_SUBTITLE);
        seleniumHandler.startRecording(InstructionVideosUtil.TARGET_FOLDER, VIDEO_TITLE + " " + InstructionVideosUtil.VIDEO_SUBTITLE);
        seleniumHandler.wait(3000);
        paul.narrateAsync(NORMAL, "Hi everyone, Christopher Paul here from kassandra.org. Today we're going to learn about User Management in Kassandra. As an administrator, the Users page is where you add team members to the system so they can access Kassandra and be assigned to projects.");
        seleniumHandler.hideOverlay();
        productListViewTester.switchToProductListViewWithOidc("christopher.paul@kassandra.org", "password", "../kassandra.wiki/screenshots/login-view.png", testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));

        //---------------------------------------------------------------------------------------
        // Navigate to Users Page
        //---------------------------------------------------------------------------------------

        seleniumHandler.setHighlightEnabled(true);//highlight elements starting now
        paul.narrateAsync(NORMAL, "Lets open the user menu and switch to the Users page. This menu item is only visible to administrators.");
        userListViewTester.switchToUserListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo), "christopher.paul@kassandra.org", "password");

        //---------------------------------------------------------------------------------------
        // Explain User List Page Purpose
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserListView.USER_LIST_PAGE_TITLE);
        paul.narrate(NORMAL, "This is the Users page. Here you can see all team members who have access to Kassandra.");

        //---------------------------------------------------------------------------------------
        // Explain Grid Columns
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserListView.USER_GRID);
        paul.narrate(NORMAL, "The grid shows use information.");

        //---------------------------------------------------------------------------------------
        // Explain Employment Dates
        //---------------------------------------------------------------------------------------

        //---------------------------------------------------------------------------------------
        // Create New User - Scenario Introduction
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "Let me show you how to add a new team member. We just hired a new developer named Sarah Johnson who needs access to Kassandra.");

        //---------------------------------------------------------------------------------------
        // Open Create User Dialog
        //---------------------------------------------------------------------------------------

        paul.narrateAsync(NORMAL, "Let's click the Create button to open the user dialog.");
        seleniumHandler.click(UserListView.CREATE_USER_BUTTON);

        //---------------------------------------------------------------------------------------
        // Fill User Name
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "First, we enter the user's full name. This is how they'll appear in task assignments and reports throughout Kassandra.");
        final String userName = "Sarah Johnson";
        seleniumHandler.setTextField(UserDialog.USER_NAME_FIELD, userName);

        //---------------------------------------------------------------------------------------
        // Fill User Email
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "Next, their email address. This is the most important field - it's the unique identifier Sarah will use to log into Kassandra. Make sure it matches their authentication email exactly.");
        final String userEmail = "sarah.johnson@kassandra.org";
        seleniumHandler.setTextField(UserDialog.USER_EMAIL_FIELD, userEmail);

        //---------------------------------------------------------------------------------------
        // Set First Working Day
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "The First Working Day is the date when Sarah will start working and can be assigned tasks. Let's set it to July first, twenty twenty-five.");
        final LocalDate firstWorkingDay = LocalDate.of(2025, 7, 1);
        seleniumHandler.setDatePickerValue(UserDialog.USER_FIRST_WORKING_DAY_PICKER, firstWorkingDay);

        //---------------------------------------------------------------------------------------
        // Explain Last Working Day
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserDialog.USER_LAST_WORKING_DAY_PICKER);
        paul.narrate(NORMAL, "The Last Working Day is optional and only used when someone leaves the company. We'll leave it empty for Sarah.");

        //---------------------------------------------------------------------------------------
        // Save User
        //---------------------------------------------------------------------------------------

        paul.narrateAsync(NORMAL, "Now let's save to create Sarah's user account.");
        seleniumHandler.click(UserDialog.CONFIRM_BUTTON);

        //---------------------------------------------------------------------------------------
        // Verify Creation
        //---------------------------------------------------------------------------------------

        seleniumHandler.wait(1000);
        seleniumHandler.highlight(UserListView.USER_GRID_NAME_PREFIX + userName);
        paul.narrate(NORMAL, "Perfect! Sarah Johnson now appears in our user list. She can now log into Kassandra using her email address and be assigned to project tasks.");

        //---------------------------------------------------------------------------------------
        // Explain Edit/Delete Capabilities
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(
                UserListView.USER_GRID_EDIT_BUTTON_PREFIX + userName,
                UserListView.USER_GRID_DELETE_BUTTON_PREFIX + userName
        );
        paul.narrate(NORMAL, "You can edit any user's information using the notepad icon on the right, or remove them from the system using the trashcan icon. However, be careful when deleting users who have been assigned tasks, as this may affect your project history and reporting.");

        //---------------------------------------------------------------------------------------
        // Explain Row Counter
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserListView.USER_ROW_COUNTER);
        paul.narrate(NORMAL, "The counter at the top shows you how many users currently have access to the system.");

        //---------------------------------------------------------------------------------------
        // Explain Global Filter
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserListView.USER_GLOBAL_FILTER);
        paul.narrate(NORMAL, "As your team grows, you can use the search filter to quickly find specific users by typing their name or email address.");

        //---------------------------------------------------------------------------------------
        // Closing
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "That's all there is to managing users in Kassandra. Remember, add new team members before they need to log in, and make sure to use their correct authentication email address. Thanks for watching!");

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
