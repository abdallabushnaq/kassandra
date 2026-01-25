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

import de.bushnaq.abdalla.kassandra.ai.narrator.Narrator;
import de.bushnaq.abdalla.kassandra.ai.narrator.NarratorAttribute;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.ui.dialog.UserGroupDialog;
import de.bushnaq.abdalla.kassandra.ui.introduction.util.InstructionVideosUtil;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.UserGroupListView;
import de.bushnaq.abdalla.kassandra.ui.view.util.ProductListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.UserGroupListViewTester;
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
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Introduction video demonstrating how to manage user groups in Kassandra.
 * <p>
 * This automated test creates an instructional video showing:
 * - Navigation to User Groups page
 * - Purpose of user groups for access control
 * - Creating a new user group
 * - Adding members to a group
 * - Understanding group-based product access
 * <p>
 * The video includes narrator (Christopher Paul) explaining each action
 * and highlights UI elements to guide viewers.
 */
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
@AutoConfigureTestRestTemplate
//@Transactional
public class ManagingUserGroupsIntroductionVideo extends AbstractKeycloakUiTestUtil {
    public static final NarratorAttribute        INTENSE     = new NarratorAttribute().withExaggeration(.7f).withCfgWeight(.3f).withTemperature(1f);
    public static final NarratorAttribute        NORMAL      = new NarratorAttribute().withExaggeration(.5f).withCfgWeight(.5f).withTemperature(1f);
    public static final String                   VIDEO_TITLE = "Managing User Groups";
    @Autowired
    private             ProductListViewTester    productListViewTester;
    @Autowired
    private             HumanizedSeleniumHandler seleniumHandler;
    @Autowired
    private             UserGroupListViewTester  userGroupListViewTester;

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

        User user1 = userApi.getByEmail("kristen.hubbell@kassandra.org");
        User user2 = userApi.getByEmail("claudine.fick@kassandra.org");
        User user3 = userApi.getByEmail("randy.asmus@kassandra.org");


        seleniumHandler.showOverlay(VIDEO_TITLE, InstructionVideosUtil.VIDEO_SUBTITLE);
        seleniumHandler.startRecording(InstructionVideosUtil.TARGET_FOLDER, VIDEO_TITLE + " " + InstructionVideosUtil.VIDEO_SUBTITLE);
        seleniumHandler.wait(3000);
        paul.narrateAsync(NORMAL, "Hi everyone, Christopher Paul here from kassandra.org. Today we're going to learn about User Groups in Kassandra. User Groups are a powerful feature that lets you control access to products by organizing team members into groups.");
        seleniumHandler.hideOverlay();
        productListViewTester.switchToProductListViewWithOidc("christopher.paul@kassandra.org", "password", "../kassandra.wiki/screenshots/user-groups-login-view.png", testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));

        //---------------------------------------------------------------------------------------
        // Navigate to User Groups Page
        //---------------------------------------------------------------------------------------

        seleniumHandler.setHighlightEnabled(true);//highlight elements starting now
        paul.narrateAsync(NORMAL, "Let's open the user menu and switch to the User Groups page. This menu item is only visible to administrators.");
        userGroupListViewTester.switchToUserGroupListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo), "christopher.paul@kassandra.org", "password");

        //---------------------------------------------------------------------------------------
        // Explain User Groups Page Purpose
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserGroupListView.GROUP_LIST_PAGE_TITLE);
        paul.narrate(NORMAL, "This is the User Groups page. Here you can create groups like Development Team, QA Team, or Product Owners. Instead of granting product access to individual users one by one, you can grant access to a group, and all members automatically inherit that access.");

        //---------------------------------------------------------------------------------------
        // Explain Grid Columns
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserGroupListView.GROUP_GRID);
        paul.narrate(NORMAL, "The grid shows the group name, description, how many members are in each group, and when it was created and last updated.");

        //---------------------------------------------------------------------------------------
        // Create New Group - Scenario Introduction
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "Let me show you how to create a new user group. We want to create a Backend Developers group for our engineering team.");

        //---------------------------------------------------------------------------------------
        // Open Create Group Dialog
        //---------------------------------------------------------------------------------------

        paul.narrateAsync(NORMAL, "Let's click the Create button to open the group dialog.");
        seleniumHandler.click(UserGroupListView.CREATE_GROUP_BUTTON);

        //---------------------------------------------------------------------------------------
        // Fill Group Name
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "First, we enter the group name. This is how the group will appear throughout Kassandra when setting up product access. Let's call it Backend Developers.");
        final String groupName = "Backend Developers";
        seleniumHandler.setTextField(UserGroupDialog.GROUP_NAME_FIELD, groupName);

        //---------------------------------------------------------------------------------------
        // Fill Group Description
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "Next, we add a description to explain the group's purpose. This helps other administrators understand who should be in this group.");
        final String groupDescription = "Backend development team members";
        seleniumHandler.setTextArea(UserGroupDialog.GROUP_DESCRIPTION_FIELD, groupDescription);

        //---------------------------------------------------------------------------------------
        // Add Group Members
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserGroupDialog.GROUP_MEMBERS_FIELD);
        paul.narrate(NORMAL, "Now the important part - selecting the members. Let's add " + user1.getName() + ", " + user2.getName() + ", and " + user3.getName() + " to our Backend Developers group. These team members will automatically get access to any product that grants access to this group.");

        // Add members using multi-select combo box
        String[] members = {user1.getName(), user2.getName(), user3.getName()};
        seleniumHandler.setMultiSelectComboBoxValue(UserGroupDialog.GROUP_MEMBERS_FIELD, members);

        paul.narrate(NORMAL, "Perfect! We've selected three team members for our Backend Developers group.");

        //---------------------------------------------------------------------------------------
        // Save Group
        //---------------------------------------------------------------------------------------

        paul.narrateAsync(NORMAL, "Now let's save to create our new user group.");
        userGroupListViewTester.closeDialog(UserGroupDialog.CONFIRM_BUTTON);

        //---------------------------------------------------------------------------------------
        // Verify Creation
        //---------------------------------------------------------------------------------------

        seleniumHandler.wait(1000);
        seleniumHandler.highlight(UserGroupListView.GROUP_GRID_NAME_PREFIX + groupName);
        paul.narrate(INTENSE, "Excellent! Backend Developers now appears in our list with three members. Now we can use this group when setting up product access control.");

        //---------------------------------------------------------------------------------------
        // Explain Benefits of Groups
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "User Groups make access control much easier. Instead of adding individual users to every product, you add the group once. When new team members join, just add them to the appropriate groups, and they automatically inherit all the group's product access.");

        //---------------------------------------------------------------------------------------
        // Explain Edit/Delete Capabilities
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(
                UserGroupListView.GROUP_GRID_EDIT_BUTTON_PREFIX + groupName,
                UserGroupListView.GROUP_GRID_DELETE_BUTTON_PREFIX + groupName
        );
        paul.narrate(NORMAL, "You can edit group membership or details using the notepad icon, or delete groups using the trashcan icon. Just be careful - if you delete a group, all products that granted access to that group will no longer be accessible to its members.");

        //---------------------------------------------------------------------------------------
        // Explain Row Counter
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserGroupListView.GROUP_ROW_COUNTER);
        paul.narrate(NORMAL, "The counter at the top shows you how many user groups currently exist in the system.");

        //---------------------------------------------------------------------------------------
        // Explain Global Filter
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserGroupListView.GROUP_GLOBAL_FILTER);
        paul.narrate(NORMAL, "As you create more groups, you can use the search filter to quickly find specific groups by typing their name or description.");

        //---------------------------------------------------------------------------------------
        // Closing
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "That's all there is to managing user groups in Kassandra. Create groups for your teams, add members, and use them to control product access efficiently. Thanks for watching!");

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

