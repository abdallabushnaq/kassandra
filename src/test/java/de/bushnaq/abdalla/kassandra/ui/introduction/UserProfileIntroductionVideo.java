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
import de.bushnaq.abdalla.kassandra.ui.dialog.ImagePromptDialog;
import de.bushnaq.abdalla.kassandra.ui.introduction.util.InstructionVideosUtil;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.LoginView;
import de.bushnaq.abdalla.kassandra.ui.view.UserProfileView;
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
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Tag("IntroductionVideo")
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
public class UserProfileIntroductionVideo extends AbstractKeycloakUiTestUtil {
    public static final NarratorAttribute        NORMAL      = new NarratorAttribute().withExaggeration(.5f).withCfgWeight(.5f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    public static final String                   VIDEO_TITLE = "Kassandra User Profile & Avatar Management";
    @Autowired
    private             ProductListViewTester    productListViewTester;
    @Autowired
    private             HumanizedSeleniumHandler seleniumHandler;

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
//        paul.setSilent(true);
        HumanizedSeleniumHandler.setHumanize(true);
        seleniumHandler.getAndCheck("http://localhost:" + "8080" + "/ui/" + LoginView.ROUTE);
        seleniumHandler.showOverlay(VIDEO_TITLE, InstructionVideosUtil.VIDEO_SUBTITLE);
        seleniumHandler.startRecording(InstructionVideosUtil.TARGET_FOLDER, VIDEO_TITLE + " " + InstructionVideosUtil.VIDEO_SUBTITLE);
        seleniumHandler.wait(3000);
        paul.narrateAsync(NORMAL, "Hi everyone, Christopher Paul here from kassandra.org. Today we're going to learn about managing your user profile and creating custom avatar icons using AI in Kassandra. Your profile contains important information about how you're identified in the system and displayed in reports.");
        seleniumHandler.hideOverlay();
        productListViewTester.switchToProductListViewWithOidc("christopher.paul@kassandra.org", "password", "../kassandra.wiki/screenshots/login-view.png", testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));

        //---------------------------------------------------------------------------------------
        // Navigate to Profile Page
        //---------------------------------------------------------------------------------------

        seleniumHandler.setHighlightEnabled(true);//highlight elements starting now
        paul.narrateAsync(NORMAL, "Let's open the user menu and navigate to the User Profile page.");
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_VIEW_PROFILE);

        //---------------------------------------------------------------------------------------
        // Explain Profile Page Purpose
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserProfileView.PROFILE_PAGE_TITLE);
        paul.narrate(NORMAL, "This is your personal profile page where you can edit your display name, personal color, and avatar icon.");

        //---------------------------------------------------------------------------------------
        // Use Case - Name Change
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserProfileView.USER_NAME_FIELD);
        paul.narrate(NORMAL, "Let's say I recently married and decided to change my last name to my wife's. I'll update my display name to reflect this change. This is the name that will appear in task assignments, reports, and calendars.");
        seleniumHandler.setTextField(UserProfileView.USER_NAME_FIELD, "Christopher Wilson");

        //---------------------------------------------------------------------------------------
        // Generate Avatar - Open Dialog
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserProfileView.GENERATE_AVATAR_BUTTON);
        paul.narrate(NORMAL, "Now let's create a custom avatar icon using Kassandra's AI image generation feature. I'll click the magic wand button next to my name field.");
        seleniumHandler.click(UserProfileView.GENERATE_AVATAR_BUTTON);
        seleniumHandler.wait(1000);

        //---------------------------------------------------------------------------------------
        // Explain Image Dialog
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "This opens the AI Image Generation Dialog. Here I can describe the avatar I want, generate it, upload an existing image, or download the result.");

        //---------------------------------------------------------------------------------------
        // Enter Prompt
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(ImagePromptDialog.ID_IMAGE_PROMPT_FIELD);
        paul.narrate(NORMAL, "Let me enter a description for my avatar. I'll describe a professional portrait in a minimalist style.");
        seleniumHandler.setTextArea(ImagePromptDialog.ID_IMAGE_PROMPT_FIELD, "Professional avatar portrait of Christopher Wilson, software developer, minimalist flat design, simple background, icon style, blue tones");
        seleniumHandler.wait(500);

        //---------------------------------------------------------------------------------------
        // Generate Image
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(ImagePromptDialog.ID_GENERATE_BUTTON);
        paul.narrateAsync(NORMAL, "Now I'll click the Generate button to create the avatar using AI. This will take a few moments.");
        seleniumHandler.click(ImagePromptDialog.ID_GENERATE_BUTTON);

        // Wait for generation to complete (showing progress)
        waitForStableDiffusion();

        paul.narrate(NORMAL, "Great! The AI has generated an avatar based on my description. I can see the preview here in the dialog.");

        //---------------------------------------------------------------------------------------
        // Demonstrate Update Button
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(ImagePromptDialog.ID_UPDATE_BUTTON);
        paul.narrate(NORMAL, "If I want to refine the image, I can use the Update button. This uses the current image as a starting point and applies my prompt to modify it. Let me adjust my description slightly.");
        seleniumHandler.setTextArea(ImagePromptDialog.ID_IMAGE_PROMPT_FIELD, "Professional avatar portrait of Christopher Wilson, software developer, minimalist flat design, simple background, icon style, green tones");
        seleniumHandler.wait(500);

        paul.narrateAsync(NORMAL, "I'll click Update to refine the avatar with green tones instead.");
        seleniumHandler.click(ImagePromptDialog.ID_UPDATE_BUTTON);
        waitForStableDiffusion();

        paul.narrate(NORMAL, "Perfect! The avatar has been updated with the new color scheme while maintaining the overall style.");

        //---------------------------------------------------------------------------------------
        // Demonstrate Upload Button
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(ImagePromptDialog.ID_UPLOAD_BUTTON);
        paul.narrate(NORMAL, "You can also upload an existing image using this upload area. The system automatically resizes it to the correct dimensions. Just drag and drop a PNG file here, or click to browse.");

        //---------------------------------------------------------------------------------------
        // Demonstrate Download Button
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(ImagePromptDialog.ID_DOWNLOAD_BUTTON);
        paul.narrate(NORMAL, "If you want to save the generated image for use elsewhere, click the download button to save it as a PNG file to your computer.");

        //---------------------------------------------------------------------------------------
        // Create another Image
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "I am still not happy with my image. Lets generate something different.");
        seleniumHandler.setTextArea(ImagePromptDialog.ID_IMAGE_PROMPT_FIELD, "portrait of a friendly robot character, cartoon style, simple background");
        seleniumHandler.wait(500);

        paul.narrateAsync(NORMAL, "Lets see what we get this time.");
        seleniumHandler.click(ImagePromptDialog.ID_GENERATE_BUTTON);
        waitForStableDiffusion();

        //---------------------------------------------------------------------------------------
        // Accept Image
        //---------------------------------------------------------------------------------------
        seleniumHandler.highlight(ImagePromptDialog.ID_ACCEPT_BUTTON);
        paul.narrateAsync(NORMAL, "I'm happy with this avatar, so I'll click Accept to use it for my profile.");
        seleniumHandler.click(ImagePromptDialog.ID_ACCEPT_BUTTON);
        seleniumHandler.wait(1000);

        //---------------------------------------------------------------------------------------
        // Use Case - Color Change
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(UserProfileView.USER_COLOR_PICKER);
        paul.narrate(NORMAL, "I also want to change my color. I'm working with another developer who is already using red, so let me pick a different color to better distinguish my tasks from hers.");
        seleniumHandler.setColorPickerValue(UserProfileView.USER_COLOR_PICKER, "#00FF00");
        seleniumHandler.wait(1000);

        //---------------------------------------------------------------------------------------
        // Explain Color Usage
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "This color is used throughout Kassandra in Gantt charts and resource utilization graphs, making it easy to identify who's working on what.");

        //---------------------------------------------------------------------------------------
        // Save Changes
        //---------------------------------------------------------------------------------------

        paul.narrateAsync(NORMAL, "Now let's save all these changes - my new name, avatar, and color.");
        seleniumHandler.click(UserProfileView.SAVE_PROFILE_BUTTON);

        //---------------------------------------------------------------------------------------
        // Verify Changes
        //---------------------------------------------------------------------------------------

        seleniumHandler.wait(1000);
        paul.narrate(NORMAL, "Perfect! The profile has been updated successfully. My new avatar, name, and color are now reflected throughout the system.");

        //---------------------------------------------------------------------------------------
        // Closing
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "That's all there is to managing your profile and creating custom avatars in Kassandra. You can generate AI-powered avatars, upload your own images, or update existing ones. Keep your profile up to date so your team members can easily identify you. Thanks for watching!");

        seleniumHandler.showOverlay(VIDEO_TITLE, InstructionVideosUtil.COPYLEFT_SUBTITLE);
        seleniumHandler.waitUntilBrowserClosed(5000);
    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(
                        1,
                        OffsetDateTime.parse("2025-08-11T08:00:00+01:00"),
                        LocalDate.parse("2025-08-04"),
                        Duration.ofDays(10),
                        0, 0,
                        0, 0,
                        0, 0,
                        0, 0,
                        6, 8, 12, 6, 13)//
        };
        return Arrays.stream(randomCases).toList();
    }

    private void waitForStableDiffusion() {
        seleniumHandler.pushWaitDuration(Duration.ofSeconds(120));
        seleniumHandler.waitForElementToBeInteractable(ImagePromptDialog.ID_GENERATE_BUTTON);
        seleniumHandler.popWaitDuration();
    }


}
