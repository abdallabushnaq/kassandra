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
import de.bushnaq.abdalla.kassandra.ui.dialog.FeatureDialog;
import de.bushnaq.abdalla.kassandra.ui.dialog.ProductDialog;
import de.bushnaq.abdalla.kassandra.ui.dialog.SprintDialog;
import de.bushnaq.abdalla.kassandra.ui.dialog.VersionDialog;
import de.bushnaq.abdalla.kassandra.ui.introduction.util.InstructionVideosUtil;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.RenderUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.*;
import de.bushnaq.abdalla.kassandra.ui.view.util.*;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
//@Transactional
//@Testcontainers
@Slf4j
public class ProductsVersionsFeaturesAndSprintsIntroductionVideo extends AbstractKeycloakUiTestUtil {
    public static final NarratorAttribute          INTENSE     = new NarratorAttribute().withExaggeration(.7f).withCfgWeight(.3f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    public static final NarratorAttribute          NORMAL      = new NarratorAttribute().withExaggeration(.5f).withCfgWeight(.5f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    public static final String                     VIDEO_TITLE = "Products, Versions, Features and Sprints";
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
    private             String                     productName;
    @Autowired
    private             HumanizedSeleniumHandler   seleniumHandler;
    @Autowired
    private             SprintListViewTester       sprintListViewTester;
    private             String                     sprintName;
    @Autowired
    private             TaskListViewTester         taskListViewTester;
    private             String                     taskName;
    private final       OffDayType                 typeRecord1 = OffDayType.VACATION;
    @Autowired
    private             UserListViewTester         userListViewTester;
    private             String                     userName;
    @Autowired
    private             VersionListViewTester      versionListViewTester;
    private             String                     versionName;

    private void approveAiPlan() {
        //assuming the ai has a question
        seleniumHandler.setTextArea(Kassandra.AI_QUERY_INPUT, "yes");
        seleniumHandler.click(Kassandra.AI_SUBMIT_BUTTON);
        waitForAi();
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void createASprint(RandomCase randomCase, TestInfo testInfo) throws Exception {
        seleniumHandler.setWindowSize(InstructionVideosUtil.VIDEO_WIDTH, InstructionVideosUtil.VIDEO_HEIGHT);

        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateProductsIfNeeded(testInfo, randomCase);
        HumanizedSeleniumHandler.setHumanize(true);
        seleniumHandler.showOverlay(VIDEO_TITLE, InstructionVideosUtil.VIDEO_SUBTITLE);
        seleniumHandler.startRecording(InstructionVideosUtil.TARGET_FOLDER, VIDEO_TITLE + " " + InstructionVideosUtil.VIDEO_SUBTITLE);
        Narrator paul = Narrator.withChatterboxTTS("tts/" + testInfo.getTestClass().get().getSimpleName());
        paul.setSilent(true);
        productName = "Jupiter";
        versionName = "1.0.0";
        featureName = "Config server";
        sprintName  = "Minimum Viable Product";
        taskName    = nameGenerator.generateSprintName(0);


        paul.narrateAsync(NORMAL, "Good morning, my name is Christopher Paul. I am the product manager of Kassandra and I will be demonstrating the latest alpha version of the Kassandra project server to you today.");
        productListViewTester.switchToProductListViewWithOidc("christopher.paul@kassandra.org", "password", "../kassandra.wiki/screenshots/login-view.png", testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));

        seleniumHandler.setEnabled(true);
        //---------------------------------------------------------------------------------------
        logHeader("Products Page");
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "Kassandra is a project planning and progress tracking server targeting small to medium team sizes. It is a open source project and has an Apachee two dot zero license.").pause();
        paul.narrate(NORMAL, "Kassandra supports OIDC authentication and authorization. I just logged into the server using my kassandra dot org ID.").pause();
        paul.narrate(NORMAL, "The first page you see when you log into the server is the Products page where all Products are listed.").pause();
        //---------------------------------------------------------------------------------------
        logHeader("Create a Product");
        //---------------------------------------------------------------------------------------
        paul.narrate(NORMAL, "Lets start by adding a new product by selecting the Create button.");
        seleniumHandler.click(ProductListView.CREATE_PRODUCT_BUTTON);
        paul.narrate(INTENSE, "Lets call it Jupiter!").pause();
        seleniumHandler.setTextField(ProductDialog.PRODUCT_NAME_FIELD, productName);

        //---------------------------------------------------------------------------------------
        logHeader(" Grant access to Team group via ACL");
        //---------------------------------------------------------------------------------------
        paul.narrate(NORMAL, "Kassandra supports Access Control Lists, or A C L for short. This allows us to control who can access our product.").pause();
        paul.narrate(NORMAL, "Lets grant access to our Team group, so all team members can collaborate on this product.");
        seleniumHandler.setMultiSelectComboBoxValue(ProductDialog.PRODUCT_ACL_GROUPS_FIELD, new String[]{"Team"});
        paul.narrate(NORMAL, "Perfect! Now all members of the Team group will have access to the Jupiter product, all its versions, features and sprints.").pause();

        paul.narrate(NORMAL, "Select Save to close the dialog and persist our new product with its access control settings.").pause();
        productListViewTester.closeDialog(ProductDialog.CONFIRM_BUTTON);
        paul.narrate(INTENSE, "And we got ourself a new product! Notice the access column shows that one group has access to this product.").pause();


        paul.narrate(NORMAL, "With the little notepad and trashcan icons, on the right side, you can edit or delete your product.").pause();
        paul.narrate(NORMAL, "Lets select our product...");
        productListViewTester.selectProduct(productName);

        //---------------------------------------------------------------------------------------
        logHeader(" Versions Page");
        //---------------------------------------------------------------------------------------
        paul.narrate(NORMAL, "This takes us to the Versions Page.").pause();
        paul.narrate(NORMAL, "Every Product can have any number of versions.").pause();
        paul.narrate(NORMAL, "Jupiter is a totally new product, so lets create a first version for it.");
        paul.narrate(NORMAL, "Select the Create button...");
        //---------------------------------------------------------------------------------------
        logHeader(" Create a Version");
        //---------------------------------------------------------------------------------------
        seleniumHandler.click(VersionListView.CREATE_VERSION_BUTTON);
        paul.narrate(NORMAL, "Lets use the obvious. One, dot, zero, dot, zero.");
        seleniumHandler.setTextField(VersionDialog.VERSION_NAME_FIELD, versionName);
        paul.narrateAsync(NORMAL, "Select Save to close the dialog and persist our version.");
        seleniumHandler.click(VersionDialog.CONFIRM_BUTTON);
        paul.narrate(INTENSE, "And we got ourself a new version!").longPause();
        //---------------------------------------------------------------------------------------
        logHeader("Version AI");
        //---------------------------------------------------------------------------------------

        paul.narrateAsync(NORMAL, "Kassandra AI knows the context of where you are and therefore knows that we are currently looking at teh versiosn of Product " + productName + ".");
        seleniumHandler.setTextArea(Kassandra.AI_QUERY_INPUT, "Add version 2.0.0.");
        seleniumHandler.click(Kassandra.AI_SUBMIT_BUTTON);
        waitForAi();

        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "The little notepad and trashcan icons, on the right side, can be used to edit or delete your version.").pause();
        paul.narrate(NORMAL, "Lets select our version.");
        versionListViewTester.selectVersion(versionName);

        //---------------------------------------------------------------------------------------
        logHeader(" Features Page");
        //---------------------------------------------------------------------------------------
        paul.narrate(NORMAL, "This takes us to the Features Page. Features are what we actually want to plan and track, although they are split into one or more sprints.").pause();
        paul.narrate(NORMAL, "Every product version can have any number of features.").pause();
        paul.narrate(NORMAL, "Lets assume Jupiter is a cloud service.").pause();
        paul.narrate(NORMAL, "So the first feature would be a micro service that supports retrieving configurations.").pause();
        paul.narrate(NORMAL, "Select the Create button...");
        //---------------------------------------------------------------------------------------
        logHeader(" Create Feature");
        //---------------------------------------------------------------------------------------
        seleniumHandler.click(FeatureListView.CREATE_FEATURE_BUTTON_ID);
        paul.narrate(NORMAL, "Lets call the feature 'Config server'.");
        seleniumHandler.setTextField(FeatureDialog.FEATURE_NAME_FIELD, featureName);
        paul.narrateAsync(NORMAL, "Select Save to close the dialog and persist our feature.");
        seleniumHandler.click(FeatureDialog.CONFIRM_BUTTON);
        paul.narrate(INTENSE, "Jupiter has its first feature!").longPause();

        //---------------------------------------------------------------------------------------
        logHeader("Feature AI");
        //---------------------------------------------------------------------------------------

        seleniumHandler.setTextArea(Kassandra.AI_QUERY_INPUT, "please rename 'Config server' to 'core-config-server'.");
        seleniumHandler.click(Kassandra.AI_SUBMIT_BUTTON);
        waitForAi();

        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "Again, as in the other pages, the little notepad and trashcan icons, on the right side, can be used to edit or delete your feature.").pause();
        paul.narrate(NORMAL, "Lets select our feature...");
        featureListViewTester.selectFeature(featureName);

        //---------------------------------------------------------------------------------------
        logHeader(" Sprints Page");
        //---------------------------------------------------------------------------------------
        paul.narrate(NORMAL, "We are now on the Sprints page of our product. On this page we however only see sprints related to the Feature we just selected.").pause();
        paul.narrate(NORMAL, "Lets create a sprint for our feature and just call it: Minimum Viable Product.").pause();
        paul.narrate(NORMAL, "Select the Create button.");
        logHeader(" Create a Sprint");
        seleniumHandler.click(SprintListView.CREATE_SPRINT_BUTTON);
        seleniumHandler.setTextField(SprintDialog.SPRINT_NAME_FIELD, sprintName);
        paul.narrateAsync(NORMAL, "Select Save to close the dialog and persist our sprint.");
        seleniumHandler.click(SprintDialog.CONFIRM_BUTTON);
        paul.narrate(INTENSE, "That was easy!").longPause();

        //---------------------------------------------------------------------------------------
        logHeader("Sprint AI");
        //---------------------------------------------------------------------------------------

        seleniumHandler.setTextArea(Kassandra.AI_QUERY_INPUT, "add sprint 'Muenchen'.");
        seleniumHandler.click(Kassandra.AI_SUBMIT_BUTTON);
        waitForAi();

        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "Now we need to start planning our sprint. We do this in the Tasks page. Not by selecting the sprint, but configuring it with the small crog icon on the right side.");
        seleniumHandler.click(SprintListView.SPRINT_GRID_CONFIG_BUTTON_PREFIX + sprintName);

        //---------------------------------------------------------------------------------------
        logHeader(" Tasks Page");
        //---------------------------------------------------------------------------------------
        paul.narrate(NORMAL, "This is the page where you plan your sprint including the gantt chart.").pause();
        seleniumHandler.waitForElementToBeClickable(RenderUtil.GANTT_CHART);

        paul.pauseIfSilent(5000);
        seleniumHandler.showOverlay(VIDEO_TITLE, InstructionVideosUtil.COPYLEFT_SUBTITLE);
        seleniumHandler.waitUntilBrowserClosed(5000);

    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(1, OffsetDateTime.parse("2025-08-11T08:00:00+01:00"), LocalDate.parse("2025-08-04"), Duration.ofDays(10), 0, 0, 0, 0, 0, 0, 0, 0, 6, 8, 12, 6, 13)//
        };
        return Arrays.stream(randomCases).toList();
    }

    private void printAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            String password = "test-password";
            log.info("Running demo with user: {} and password: {}", username, password);
        } else {
            log.warn("No authenticated user found. Running demo without authentication.");
        }
    }

    // Method to get the public-facing URL, fixing potential redirect issues
//    private static String getPublicFacingUrl(KeycloakContainer container) {
//        return String.format("http://%s:%s",
//                container.getHost(),
//                container.getMappedPort(8080));
//    }

    private void waitForAi() {
        seleniumHandler.pushWaitDuration(Duration.ofSeconds(240));
        seleniumHandler.waitForElementToBeEnabled(Kassandra.AI_SUBMIT_BUTTON);
        seleniumHandler.popWaitDuration();
    }

}
