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
import de.bushnaq.abdalla.kassandra.dto.Feature;
import de.bushnaq.abdalla.kassandra.dto.OffDayType;
import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.dto.Version;
import de.bushnaq.abdalla.kassandra.ui.introduction.util.InstructionVideosUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.Kassandra;
import de.bushnaq.abdalla.kassandra.ui.view.ProductListView;
import de.bushnaq.abdalla.kassandra.ui.view.util.*;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertTrue;

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
@Slf4j
public class KassandraAgentIntroductionVideo extends AbstractAiIntroductionVideo {
    public static final NarratorAttribute          INTENSE     = new NarratorAttribute().withExaggeration(.7f).withCfgWeight(.3f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    public static final NarratorAttribute          NORMAL      = new NarratorAttribute().withExaggeration(.5f).withCfgWeight(.5f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    public static final String                     VIDEO_TITLE = "Kassandra Agent";
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
        if (isAgentAskingForConfirmation()) {
            seleniumHandler.setTextArea(Kassandra.AI_QUERY_INPUT, "yes");
            seleniumHandler.click(Kassandra.AI_SUBMIT_BUTTON);
            waitForAi();
        }
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
//        productName = "Jupiter";
//        versionName = "1.0.0";
//        featureName = "Config server";
//        sprintName  = "Minimum Viable Product";
//        taskName    = nameGenerator.generateSprintName(0);


        paul.narrateAsync(NORMAL, "Good morning, my name is Christopher Paul. I am the product manager of Kassandra and I will be demonstrating the AI capability of the latest alpha version of the Kassandra project server to you today.");
        productListViewTester.switchToProductListViewWithOidc("christopher.paul@kassandra.org", "password", "../kassandra.wiki/screenshots/login-view.png", testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
        seleniumHandler.setEnabled(true);


        //---------------------------------------------------------------------------------------
        logHeader("Product AI");
        //---------------------------------------------------------------------------------------
        paul.narrateAsync(NORMAL, "Kassandra has a built in ai that can help you with your daily activities.").pause();
        seleniumHandler.click(ProductListView.PRODUCT_AI_PANEL_BUTTON);
        paul.narrateAsync(NORMAL, "Lets take a look what we can do.").pause();

        seleniumHandler.setTextArea(Kassandra.AI_QUERY_INPUT, "Hi, do you know where you are and what can you help me with?");
        seleniumHandler.click(Kassandra.AI_SUBMIT_BUTTON);
        waitForAi();

        paul.narrateAsync(NORMAL, "Everything we can do via the user interface, we can also do with the help of the Kassandra AI.").pause();
        seleniumHandler.setTextArea(Kassandra.AI_QUERY_INPUT, "Add following new products Andromsda, Maestro and Hannibal and give the Team group access to all of them.");
        seleniumHandler.click(Kassandra.AI_SUBMIT_BUTTON);
        paul.narrate(NORMAL, "As you can see, kassandra does not have only read access.").pause();
        waitForAi();

        if (productApi.getByName("Andromsda").isEmpty() || productApi.getByName("Andromeda").isEmpty()) {
            //nothing was done
            approveAiPlan();//assuming the ai has a question
        }
        if (seleniumHandler.isEnabled()) {
            assertTrue(productApi.getByName("Andromsda").isPresent() || productApi.getByName("Andromeda").isPresent());//test
            assertTrue(productApi.getByName("Maestro").isPresent());//test
            assertTrue(productApi.getByName("Hannibal").isPresent());//test
        }
        if (productApi.getByName("Andromeda").isPresent()) {
            //kassandra created the product as stated
            paul.narrate(NORMAL, "Kassandra fixed the type for me, I was going to ask it to fix it, but I no longer need to do so. Sometimes this is a nice feature, sometimes iti can cause issues. You can ask it to not fix any typos.").pause();
            paul.narrateGap();
        } else {
            //kassandra automatically fixed the type
            paul.narrateGap();
            paul.narrate(NORMAL, "I however mistyped the name of the first product. Lets ask Kassandra to fix that.").pause();
            if (productApi.getByName("Andromsda").isEmpty()) {
                //nothing was done
                approveAiPlan();//assuming the ai has a question
            }
            seleniumHandler.setTextArea(Kassandra.AI_QUERY_INPUT, "Please fix the typo in the product.");
            seleniumHandler.click(Kassandra.AI_SUBMIT_BUTTON);
            waitForAi();
            if (productApi.getByName("Andromeda").isEmpty()) {
                //nothing was done
                approveAiPlan();//assuming the ai has a question
            }
        }
        if (seleniumHandler.isEnabled()) {
            assertTrue(productApi.getByName("Andromeda").isPresent());//test
        }

        paul.narrate(NORMAL, "Lets delete a product.").pause();
        seleniumHandler.setTextArea(Kassandra.AI_QUERY_INPUT, "Please delete the last product you created.");
        seleniumHandler.click(Kassandra.AI_SUBMIT_BUTTON);
        waitForAi();
        if (productApi.getByName("Hannibal").isPresent()) {
            //nothing was done
            approveAiPlan();//assuming the ai has a question
        }
        if (seleniumHandler.isEnabled()) {
            assertTrue(productApi.getByName("Hannibal").isEmpty());//test
        }

        paul.narrate(NORMAL, "Lets ask for something a little bit more complex involving reformatting.").pause();
        seleniumHandler.setTextArea(Kassandra.AI_QUERY_INPUT, "List all products with their versions and features in a table so that every row has only one feature.");
        String        response    = submitQuestionAndWaitForAiAndGetResponse();
        List<Product> allProducts = productApi.getAll();
        log.info("Total products in system: {}", allProducts.size());
        List<Version> allVersions = versionApi.getAll();
        log.info("Total versions in system: {}", allVersions.size());
        List<Feature> allFeatures = featureApi.getAll();
        log.info("Total features in system: {}", allFeatures.size());
        allProducts.forEach(product -> Assertions.assertTrue(response.toLowerCase(Locale.ROOT).contains(product.getName().toLowerCase()), "Product name missing: " + product.getName()));
        allVersions.forEach(version -> Assertions.assertTrue(response.toLowerCase(Locale.ROOT).contains(version.getName().toLowerCase()), "Version name missing: " + version.getName()));
        allFeatures.forEach(feature -> Assertions.assertTrue(response.toLowerCase(Locale.ROOT).contains(feature.getName().toLowerCase()), "Feature name missing: " + feature.getName()));
        paul.narrate(NORMAL, "As you can see, kassandra has access to all information within the server, not just what you see on this page.").pause();

//        //---------------------------------------------------------------------------------------
//        logHeader("Kassandra Page");
//        //---------------------------------------------------------------------------------------
//        seleniumHandler.click("/kassandra");
//
//        seleniumHandler.setTextArea(Kassandra.AI_QUERY_INPUT, "Add a new product with the name Andromsda.");
//        seleniumHandler.click(Kassandra.AI_SUBMIT_BUTTON);
//        paul.narrate(NORMAL, "As you can see, kassandra does nto have only read access. I however mistyped the name of the product. Lets ask Kassandra to fix that.").pause();
//        waitForAi();
//        if (productApi.getByName("Andromeda").isPresent()) {
//            paul.narrate(NORMAL, "Kassandra fixed the type for me, I was going to ak it to fix it, but i no longer need to do so.").pause();
//        } else {
//            if (productApi.getByName("Andromsda").isEmpty()) {
//                approveAiPlan();//assuming the ai has a question
//            }
//            paul.narrate(NORMAL, "Kassandra fixed the typo for me, I was going to ak it to fix it, but i no longer need to do so.").pause();
//            seleniumHandler.setTextArea(Kassandra.AI_QUERY_INPUT, "Please fix the typo in the product.");
//            seleniumHandler.click(Kassandra.AI_SUBMIT_BUTTON);
//            waitForAi();
//            if (productApi.getByName("Andromeda").isEmpty()) {
//                approveAiPlan();//assuming the ai has a question
//            }
//
//        }
//
//        paul.narrate(NORMAL, "Lets undo that.").pause();
//        seleniumHandler.setTextArea(Kassandra.AI_QUERY_INPUT, "Please delete the product you created.");
//        seleniumHandler.click(Kassandra.AI_SUBMIT_BUTTON);
//        waitForAi();
//        if (productApi.getByName("Andromeda").isPresent()) {
//            approveAiPlan();//assuming the ai has a question
//        }
//
//        paul.narrate(NORMAL, "Lets try something a little bit more complex.").pause();
//        seleniumHandler.setTextArea(Kassandra.AI_QUERY_INPUT, "Please rename all versions by removing the last digit.");
//        seleniumHandler.click(Kassandra.AI_SUBMIT_BUTTON);
//        waitForAi();
//        for (Version version : versionApi.getAll()) {
//            if (version.getName().split("\\.").length == 3) {
//                approveAiPlan();//assuming the ai has a question
//                break;
//            }
//        }
//
//        paul.narrate(NORMAL, "Lets see if Kassandra can remember what it did.").pause();
//        seleniumHandler.setTextArea(Kassandra.AI_QUERY_INPUT, "Can you rename all versions back how they where before?");
//        seleniumHandler.click(Kassandra.AI_SUBMIT_BUTTON);
//        waitForAi();
//        for (Version version : versionApi.getAll()) {
//            if (version.getName().split("\\.").length == 2) {
//                approveAiPlan();//assuming the ai has a question
//                break;
//            }
//        }
//
//        seleniumHandler.setTextArea(Kassandra.AI_QUERY_INPUT, "List all sprints in a table.");
//        seleniumHandler.click(Kassandra.AI_SUBMIT_BUTTON);
//        paul.narrate(NORMAL, "Kassandra knows about your sprints and can help you identify problematic sprints that need management attention.").pause();
//        waitForAi();
//
//        paul.pauseIfSilent(5000);
//        seleniumHandler.showOverlay(VIDEO_TITLE, InstructionVideosUtil.COPYLEFT_SUBTITLE);
//        seleniumHandler.waitUntilBrowserClosed(5000);

    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(3, OffsetDateTime.parse("2025-08-11T08:00:00+01:00"), LocalDate.parse("2025-08-04"), Duration.ofDays(10), 2, 2, 2, 2, 2, 2, 1, 5, 5, 8, 8, 6, 7)//
        };
        return Arrays.stream(randomCases).toList();
    }


}
