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
import de.bushnaq.abdalla.kassandra.ui.dialog.ImagePromptDialog;
import de.bushnaq.abdalla.kassandra.ui.introduction.util.InstructionVideo;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.UserProfileView;
import de.bushnaq.abdalla.kassandra.ui.view.util.ProductListViewTester;
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
//@Transactional
public class Test_04_UserProfileIntroductionVideo extends AbstractIntroductionVideo {
    public static final NarratorAttribute        NORMAL = new NarratorAttribute().withExaggeration(.5f).withCfgWeight(.5f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    @Autowired
    private             ProductListViewTester    productListViewTester;
    @Autowired
    private             HumanizedSeleniumHandler seleniumHandler;

    @BeforeAll
    static void beforeAll() {
        StableDiffusionService.setEnabled(true);
        video.setVersion(2);
        video.setTitle("04 User Profiles in Kassandra");
        video.setDescription("Today we're going to learn about managing your user profile and creating custom avatar icons using AI in Kassandra. Your profile contains important information about how you're identified in the system and displayed in reports.");
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void createVideo(RandomCase randomCase, TestInfo testInfo) throws Exception {
        seleniumHandler.setWindowSize(InstructionVideo.VIDEO_WIDTH, InstructionVideo.VIDEO_HEIGHT);

        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateProductsIfNeeded(testInfo, randomCase);
        Narrator paul = Narrator.withChatterboxTTS("tts/" + testInfo.getTestClass().get().getSimpleName());
        paul.setEnabled(true);
        HumanizedSeleniumHandler.setHumanize(true);
        //seleniumHandler.getAndCheck("http://localhost:" + "8080" + "/ui/" + LoginView.ROUTE);
        seleniumHandler.showOverlay(video.getTitle(), InstructionVideo.VIDEO_SUBTITLE);
        startRecording();
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
        // Explain Image Dialog - Light and Dark Side by Side
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "This opens the AI Image Generation Dialog. It shows two side-by-side previews: a light avatar on the left for the regular theme, and a dark avatar on the right for the dark theme. Both are generated automatically from your prompt.");

        //---------------------------------------------------------------------------------------
        // Enter Light Prompt
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(ImagePromptDialog.ID_IMAGE_PROMPT_FIELD);
        paul.narrate(NORMAL, "Let me enter a description for my avatar. I'll describe a professional portrait in a minimalist style.");
        seleniumHandler.setTextArea(ImagePromptDialog.ID_IMAGE_PROMPT_FIELD, "Professional avatar portrait of Christopher Wilson, software developer, simple white background");
        seleniumHandler.wait(500);

        //---------------------------------------------------------------------------------------
        // Explain Negative Prompt
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(ImagePromptDialog.ID_NEGATIVE_PROMPT_FIELD);
        paul.narrate(NORMAL, "Below the main prompt is the negative prompt. Here you list things you want the AI to avoid — such as blurry backgrounds, extra limbs, or text. You can leave it empty most of the time.");

        //---------------------------------------------------------------------------------------
        // Explain Dark Prompt
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(ImagePromptDialog.ID_DARK_PROMPT_FIELD);
        paul.narrate(NORMAL, "Lets add a dark avatar prompt.");
        seleniumHandler.setTextArea(ImagePromptDialog.ID_DARK_PROMPT_FIELD, "Professional avatar portrait of Christopher Wilson, software developer, simple dark background");

        //---------------------------------------------------------------------------------------
        // Explain Dark Negative Prompt
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(ImagePromptDialog.ID_DARK_NEGATIVE_PROMPT_FIELD);
        paul.narrate(NORMAL, "There is also a separate negative prompt for the dark avatar, so you can independently fine-tune what the dark variant should avoid.");

        //---------------------------------------------------------------------------------------
        // Generate Image (light + dark)
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(ImagePromptDialog.ID_GENERATE_BUTTON);
        paul.narrateAsync(NORMAL, "Now I'll click Generate. Kassandra will produce both the light and dark avatars one after the other.");
        seleniumHandler.click(ImagePromptDialog.ID_GENERATE_BUTTON);

        // Wait for light generation to complete
        waitForStableDiffusion();
        paul.narrate(NORMAL, "The light avatar is ready. The dark avatar is now being generated automatically in the background.");

        // Wait for dark generation to complete
        waitForDarkStableDiffusion();
        paul.narrate(NORMAL, "Both avatars are now ready. Notice how the dark variant uses the same style but with a darker background, making it look great in dark mode.");

        //---------------------------------------------------------------------------------------
        // Demonstrate Light Update Button
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(ImagePromptDialog.ID_UPDATE_BUTTON);
        paul.narrate(NORMAL, "If I want to refine the light avatar, I can update the prompt and click the refresh button on the light panel. This uses the current image as a starting point and applies the new prompt to modify it.");
        seleniumHandler.setTextArea(ImagePromptDialog.ID_IMAGE_PROMPT_FIELD, "Professional avatar portrait of Christopher Wilson wearing glasses, software developer, simple white background");
        seleniumHandler.wait(500);

        paul.narrateAsync(NORMAL, "I'll click the light update button to refine it with green tones.");
        seleniumHandler.click(ImagePromptDialog.ID_UPDATE_BUTTON);
        waitForStableDiffusion();
        paul.narrate(NORMAL, "The light avatar has been updated. Now let me keep the dark variant in sync by updating its prompt as well.");

        //---------------------------------------------------------------------------------------
        // Demonstrate Dark Prompt Update and Dark Update Button
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(ImagePromptDialog.ID_DARK_PROMPT_FIELD);
        seleniumHandler.setTextArea(ImagePromptDialog.ID_DARK_PROMPT_FIELD, "Professional avatar portrait of Christopher Wilson wearing glasses, software developer, simple dark background");
        seleniumHandler.wait(500);

        seleniumHandler.highlight(ImagePromptDialog.ID_DARK_UPDATE_BUTTON);
        paul.narrateAsync(NORMAL, "Now I'll click the dark update button to regenerate just the dark variant independently.");
        seleniumHandler.click(ImagePromptDialog.ID_DARK_UPDATE_BUTTON);
        waitForDarkStableDiffusion();
        paul.narrate(NORMAL, "The dark avatar uses a dark background while the light avatar uses a bright one — consistent and theme-aware.");

        //---------------------------------------------------------------------------------------
        // Demonstrate Upload Button
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(ImagePromptDialog.ID_UPLOAD_BUTTON);
        paul.narrate(NORMAL, "You can also upload an existing PNG image using this upload area. The system automatically resizes it to the correct dimensions.");

        //---------------------------------------------------------------------------------------
        // Demonstrate Download Button
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(ImagePromptDialog.ID_DOWNLOAD_BUTTON);
        paul.narrate(NORMAL, "And you can download the generated image to save it locally for use elsewhere.");

        //---------------------------------------------------------------------------------------
        // Create another Image
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "I am still not happy with my image. Let me try a completely different style.");
        seleniumHandler.setTextArea(ImagePromptDialog.ID_IMAGE_PROMPT_FIELD, "portrait of a friendly robot character, simple white background");
        seleniumHandler.setTextArea(ImagePromptDialog.ID_DARK_PROMPT_FIELD, "portrait of a friendly robot character, simple dark background");
        seleniumHandler.wait(500);

        paul.narrateAsync(NORMAL, "Let's see what we get this time with a robot character.");
        seleniumHandler.click(ImagePromptDialog.ID_GENERATE_BUTTON);
        waitForStableDiffusion();
        paul.narrate(NORMAL, "The light robot avatar is ready. Waiting for the dark variant.");
        waitForDarkStableDiffusion();

        //---------------------------------------------------------------------------------------
        // Accept Image
        //---------------------------------------------------------------------------------------

        seleniumHandler.highlight(ImagePromptDialog.ID_ACCEPT_BUTTON);
        paul.narrateAsync(NORMAL, "I'm happy with both avatars this time! I'll click Accept to save them to my profile.");
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

        paul.narrate(NORMAL, "That's all there is to managing your profile and creating custom avatars in Kassandra. You can generate AI-powered avatars for both the light and dark themes, fine-tune them with negative prompts, update each variant independently, or upload your own images. Keep your profile up to date so your team members can easily identify you. Thanks for watching!");

        seleniumHandler.showOverlay(video.getTitle(), InstructionVideo.COPYLEFT_SUBTITLE);
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


}
