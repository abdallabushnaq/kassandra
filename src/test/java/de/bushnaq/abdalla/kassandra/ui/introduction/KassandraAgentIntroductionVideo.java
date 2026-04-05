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

import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.ai.tts.narrator.Narrator;
import de.bushnaq.abdalla.kassandra.ai.tts.narrator.NarratorAttribute;
import de.bushnaq.abdalla.kassandra.dto.Feature;
import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Version;
import de.bushnaq.abdalla.kassandra.ui.introduction.util.InstructionVideo;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.ProductListView;
import de.bushnaq.abdalla.kassandra.ui.view.util.FeatureListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.ProductListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.UserListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.VersionListViewTester;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


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
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class KassandraAgentIntroductionVideo extends AbstractIntroductionVideo {
    public static final NarratorAttribute     INTENSE = new NarratorAttribute().withExaggeration(.7f).withCfgWeight(.3f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    public static final NarratorAttribute     NORMAL  = new NarratorAttribute().withExaggeration(.5f).withCfgWeight(.5f).withTemperature(1f)/*.withVoice("chatterbox")*/;
    //    @Autowired
//    private             AvailabilityListViewTester availabilityListViewTester;
    private             Feature               feature;
    @Autowired
    private             FeatureListViewTester featureListViewTester;
    private             String                featureName;
    //    @Autowired
//    private             LocationListViewTester     locationListViewTester;
//    @Autowired
//    private             OffDayListViewTester       offDayListViewTester;
    private             Product               product;
    @Autowired
    private             ProductListViewTester productListViewTester;
    private             String                productName;
    private             Sprint                sprint;
    //    @Autowired
//    private             SprintListViewTester       sprintListViewTester;
    private             String                sprintName;
    //    @Autowired
//    private             TaskListViewTester         taskListViewTester;
//    private             String                     taskName;
//    private final       OffDayType                 typeRecord1 = OffDayType.VACATION;
    @Autowired
    private             UserListViewTester    userListViewTester;
    //    private             String                     userName;
    private             Version               version;
    @Autowired
    private             VersionListViewTester versionListViewTester;
    private             String                versionName;

    @BeforeAll
    static void beforeAll() {
        StableDiffusionService.setEnabled(true);
        video.setVersion(2);
        video.setTitle("14 Kassandra Agent");
        video.setDescription("Today I will be demonstrating the AI capability of the latest alpha version of the Kassandra project server to you today.");
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void createASprint(RandomCase randomCase, TestInfo testInfo) throws Exception {
        seleniumHandler.setWindowSize(InstructionVideo.VIDEO_WIDTH, InstructionVideo.VIDEO_HEIGHT);

        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateProductsIfNeeded(testInfo, randomCase);
        HumanizedSeleniumHandler.setHumanize(true);
        seleniumHandler.showOverlay(video.getTitle(), InstructionVideo.VIDEO_SUBTITLE);
        startRecording();
        Narrator paul = Narrator.withChatterboxTTS("tts/" + testInfo.getTestClass().get().getSimpleName());
        paul.setEnabled(true);
        product     = productApi.getAll().get(1);
        productName = product.getName();
        version     = versionApi.getAll(product.getId()).getFirst();
        versionName = version.getName();
        feature     = featureApi.getAll(version.getId()).getFirst();
        featureName = feature.getName();
        sprint      = sprintApi.getAll(feature.getId()).getFirst();
        sprintName  = sprint.getName();
//        taskName    = nameGenerator.generateSprintName(0);


        paul.narrateAsync(NORMAL, "Good morning, my name is Christopher Paul. I am the product manager of Kassandra and I will be demonstrating the AI capability of the latest alpha version of the Kassandra project server to you today.");
        productListViewTester.switchToProductListViewWithOidc("christopher.paul@kassandra.org", "password", "../kassandra.wiki/screenshots/login-view.png", testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
        seleniumHandler.setEnabled(true);


        //---------------------------------------------------------------------------------------
        logHeader("Product AI");
        //---------------------------------------------------------------------------------------
        paul.narrate(NORMAL, "Kassandra has a built in AI agent that can help you with your daily activities.").pause();
        seleniumHandler.click(ProductListView.PRODUCT_AI_PANEL_BUTTON);
//        if (false) {
//        }
        paul.narrate(NORMAL, "Lets take a look what we can do.").pause();

        processQueryAndWaitForAnswer("Hi, give me a brief idea what you can do.");

        paul.narrate(NORMAL, "Everything we can do via the user interface, we can also do with the help of the Kassandra AI.").pause();

        processQueryAndWaitForAnswer("Add following new products Andromeda, Maestro and Hannibal and give the Team group access to all of them.");
        paul.narrate(NORMAL, "As you can see, kassandra does not have only read access. It is acting in my behalf using my security context. It is allowed to do anything I am allowed to do.").pause();
        paul.narrate(NORMAL, "The user interface is updated automatically. It will always show what Kassandra changed").pause();

        if (seleniumHandler.isEnabled()) {
            assertTrue(productApi.getByName("Andromeda").isPresent());//test
            assertTrue(productApi.getByName("Maestro").isPresent());//test
            assertTrue(productApi.getByName("Hannibal").isPresent());//test
        }

        paul.narrate(NORMAL, "Good, lets see if Kassandra can remember what it did. Lets delete the last 3 products.").pause();
        processQueryAndWaitForAnswer("Please delete the last 3 products you just created.");
        if (productApi.getByName("Hannibal").isPresent()) {
            //nothing was done
            approveAiPlan();//assuming the ai has a question
        }
        if (seleniumHandler.isEnabled()) {
            assertTrue(productApi.getByName("Andromeda").isEmpty());//test
            assertTrue(productApi.getByName("Maestro").isEmpty());//test
            assertTrue(productApi.getByName("Hannibal").isEmpty());//test
        }

        paul.narrate(NORMAL, "Lets ask for something a little bit more complex involving reformatting.").pause();
        String        response    = processQueryAndWaitForAnswer("List all products with their versions and features in a table so that every row has only one feature.");
        List<Product> allProducts = productApi.getAll();
        log.info("Total products in system: {}", allProducts.size());
        List<Version> allVersions = versionApi.getAll();
        log.info("Total versions in system: {}", allVersions.size());
        List<Feature> allFeatures = featureApi.getAll();
        log.info("Total features in system: {}", allFeatures.size());

        // remove Andromeda and Maestro from allProducts, as they have no versions or features, so they would not be listed in the table
        allProducts = allProducts.stream()
                .filter(product -> !product.getName().equals("Andromeda") && !product.getName().equals("Maestro"))
                .toList();

        if (seleniumHandler.isEnabled()) {
            allProducts.forEach(product -> assertTrue(response.toLowerCase(Locale.ROOT).contains(product.getName().toLowerCase()), "Product name missing: " + product.getName()));
            allVersions.forEach(version -> assertTrue(response.toLowerCase(Locale.ROOT).contains(version.getName().toLowerCase()), "Version name missing: " + version.getName()));
            allFeatures.forEach(feature -> assertTrue(response.toLowerCase(Locale.ROOT).contains(feature.getName().toLowerCase()), "Feature name missing: " + feature.getName()));
        }
        paul.narrate(NORMAL, "As you can see, kassandra has access to all information within the server, not just what you see on this page.").pause();


        paul.narrate(NORMAL, "Lets look into the Version page...");
        productListViewTester.selectProduct(productName);
        //---------------------------------------------------------------------------------------
        logHeader("Version AI");
        //---------------------------------------------------------------------------------------

        paul.narrateAsync(NORMAL, "Kassandra AI knows the context of where you are and therefore knows that we are currently looking at the versions of Product " + productName + ". I do not need to add that information to my question.");
        processQueryAndWaitForAnswer("Add version 2.0.0.");
        if (seleniumHandler.isEnabled()) {
            assertTrue(versionApi.getByName(product.getId(), "2.0.0").isPresent()); //test
        }
        paul.narrate(NORMAL, "Lets go to the Features page.");
        versionListViewTester.selectVersion(versionName);
        //---------------------------------------------------------------------------------------

        //---------------------------------------------------------------------------------------
        logHeader("Feature AI");
        //---------------------------------------------------------------------------------------

        processQueryAndWaitForAnswer("please rename 'dashboard' to 'Game Dashboard'.");
        if (seleniumHandler.isEnabled()) {
            assertTrue(featureApi.getByName(version.getId(), "Game Dashboard").isPresent()); //test
        }
        paul.narrate(NORMAL, "Again, context aware agent.");
        paul.narrate(NORMAL, "Lets visit the Sprints page...");
        featureListViewTester.selectFeature(featureName);
        //---------------------------------------------------------------------------------------

        //---------------------------------------------------------------------------------------
        logHeader("Sprint AI");
        //---------------------------------------------------------------------------------------

        processQueryAndWaitForAnswer("add sprint 'Muenchen'.");
        if (seleniumHandler.isEnabled()) {
            assertTrue(sprintApi.getByName(feature.getId(), "Muenchen").isPresent()); //test
        }

        //---------------------------------------------------------------------------------------
        logHeader("User AI");
        //---------------------------------------------------------------------------------------
        paul.narrateAsync(NORMAL, "Lets open the user menu and switch to the Users page. This menu item is only visible to administrators.");
        if (seleniumHandler.isEnabled()) {
            userListViewTester.switchToUserListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo), "christopher.paul@kassandra.org", "password");
            processQueryAndWaitForAnswer("please add the users Ahmet Mustafa, ahmet.mustafa@kassandra.org and Elke Mueller, elke.mueller@kassandra.org and add them to the Team group.");
            assertTrue(userApi.getByEmail("ahmet.mustafa@kassandra.org").isPresent(), "User ahmet.mustafa@kassandra.org should exist");
            assertEquals("Ahmet Mustafa", userApi.getByEmail("ahmet.mustafa@kassandra.org").get().getName(), "User ahmet.mustafa@kassandra.org should have name = Ahmet Mustafa");
            assertTrue(userApi.getByEmail("elke.mueller@kassandra.org").isPresent(), "User elke.mueller@kassandra.org should exist");
            assertEquals("Elke Mueller", userApi.getByEmail("elke.mueller@kassandra.org").get().getName(), "User elke.mueller@kassandra.org should have name =Elke Mueller");
        }

        paul.narrateAsync(NORMAL, "Next we ask Kassandra to update a user.");
        if (seleniumHandler.isEnabled()) {
            processQueryAndWaitForAnswer("please rename Elke Mueller to Elke Mustafa, her email address has also changed to elke.mustafa@kassandra.org.");
            assertTrue(userApi.getByEmail("elke.mustafa@kassandra.org").isPresent(), "User elke.mustafa@kassandra.org should exist");
            assertEquals("Elke Mustafa", userApi.getByEmail("elke.mustafa@kassandra.org").get().getName(), "User elke.mueller@kassandra.org should have name =Elke Mustafa");

            processQueryAndWaitForAnswer("delete Elke Mustafa she is no longer working with us.");
            assertTrue(userApi.getByEmail("elke.mustafa@kassandra.org").isEmpty(), "User elke.mustafa@kassandra.org should not exist");
        }
        //---------------------------------------------------------------------------------------

        paul.narrate(NORMAL, "That's all there is to using the agent in kassandra. Thanks for watching!");

        paul.pauseIfDisabled(5000);
        seleniumHandler.showOverlay(video.getTitle(), InstructionVideo.COPYLEFT_SUBTITLE);
        seleniumHandler.waitUntilBrowserClosed(5000);

    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                new RandomCase(3, OffsetDateTime.parse("2025-08-11T08:00:00+01:00"), LocalDate.parse("2025-08-04"), Duration.ofDays(10), 2, 2, 2, 2, 2, 2, 1, 5, 5, 8, 8, 6, 7)//
        };
        return Arrays.stream(randomCases).toList();
    }


}
