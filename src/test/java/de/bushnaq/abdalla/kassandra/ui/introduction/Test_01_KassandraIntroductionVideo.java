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
import de.bushnaq.abdalla.kassandra.dto.Feature;
import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Version;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.introduction.util.InstructionVideo;
import de.bushnaq.abdalla.kassandra.ui.util.RenderUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.*;
import de.bushnaq.abdalla.kassandra.ui.view.util.*;
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

/**
 * Overview introduction video for Kassandra.
 * <p>
 * Provides a dense, guided tour of every major area of the application, suitable for
 * viewers with no prior Kassandra or project management experience. Topics covered:
 * <ul>
 *   <li>OAuth 2 / OIDC authentication</li>
 *   <li>What project management is and why it matters</li>
 *   <li>Product → Version → Feature → Sprint data hierarchy</li>
 *   <li>User management and User Groups / access control</li>
 *   <li>User capacity data: Availability, Location, Work Week, Off Days</li>
 *   <li>Sprint planning in the Backlog page (tasks, estimates, stories, dependencies, Gantt chart)</li>
 *   <li>Progress reporting on the Active Sprints page</li>
 *   <li>Sprint monitoring on the Sprint Quality Board (Gantt + Burn Down chart)</li>
 *   <li>Kassandra AI agent</li>
 * </ul>
 */
@Tag("IntroductionVideo")
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=${test.server.port:0}",
                "spring.security.basic.enabled=false"
        }
)
@AutoConfigureMockMvc
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class Test_01_KassandraIntroductionVideo extends AbstractIntroductionVideo {

    /**
     * Narrator attribute for emphatic, high-energy passages.
     */
    public static final NarratorAttribute       INTENSE = new NarratorAttribute().withExaggeration(.7f).withCfgWeight(.3f).withTemperature(1f);
    /**
     * Narrator attribute for calm, conversational delivery.
     */
    public static final NarratorAttribute       NORMAL  = new NarratorAttribute().withExaggeration(.5f).withCfgWeight(.5f).withTemperature(1f);
    private             Feature                 feature;
    @Autowired
    private             FeatureListViewTester   featureListViewTester;
    private             String                  featureName;
    private             Product                 product;
    @Autowired
    private             ProductListViewTester   productListViewTester;
    private             String                  productName;
    private             Sprint                  sprint;
    @Autowired
    private             SprintListViewTester    sprintListViewTester;
    private             String                  sprintName;
    @Autowired
    private             UserGroupListViewTester userGroupListViewTester;
    @Autowired
    private             UserListViewTester      userListViewTester;
    private             Version                 version;
    @Autowired
    private             VersionListViewTester   versionListViewTester;
    private             String                  versionName;

    /**
     * Configures the shared {@link InstructionVideo} metadata for this video before any test runs.
     */
    @BeforeAll
    static void beforeAll() {
        StableDiffusionService.setEnabled(true);
        video.setVersion(2);
        video.setTitle("10 Welcome to Kassandra");
        video.setDescription(
                "A complete overview of Kassandra — the open-source self-hosted project management server. "
                        + "Covering products, users, sprint planning, progress tracking, monitoring, and the AI agent.");
    }

    /**
     * Produces the complete Kassandra overview introduction video.
     *
     * @param randomCase supplies the deterministic random seed and data-shape parameters used to generate test data
     * @param testInfo   JUnit test metadata used to name recordings and screenshots
     * @throws Exception if any Selenium interaction or REST API call fails
     */
    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void createVideo(RandomCase randomCase, TestInfo testInfo) throws Exception {
        seleniumHandler.setWindowSize(InstructionVideo.VIDEO_WIDTH, InstructionVideo.VIDEO_HEIGHT + 150);
        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());

        generateProductsIfNeeded(testInfo, randomCase);

        product     = productApi.getAll().get(1);
        productName = product.getName();
        version     = versionApi.getAll(product.getId()).getFirst();
        versionName = version.getName();
        feature     = featureApi.getAll(version.getId()).getFirst();
        featureName = feature.getName();
        sprint      = sprintApi.getAll(feature.getId()).stream()
                .filter(s -> !s.getName().equals("Backlog"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No non-Backlog sprint found for feature: " + featureName));
        sprintName  = sprint.getName();

        Narrator paul = Narrator.withChatterboxTTS("tts/" + testInfo.getTestClass().get().getSimpleName());
        paul.setEnabled(true);
        HumanizedSeleniumHandler.setHumanize(true);

        //---------------------------------------------------------------------------------------
        logHeader("Opening + OIDC login");
        //---------------------------------------------------------------------------------------
        seleniumHandler.showOverlay(video.getTitle(), InstructionVideo.VIDEO_SUBTITLE);
        startRecording();
        seleniumHandler.wait(3000);

        paul.narrateAsync(NORMAL, "Hi everyone, Christopher Paul here from kassandra.org. Welcome to Kassandra — the open-source, self-hosted project management server. In the next few minutes I will take you on a tour of everything Kassandra has to offer.");
        seleniumHandler.hideOverlay();
        productListViewTester.switchToProductListViewWithOidc(
                "christopher.paul@kassandra.org", "password",
                "../kassandra.wiki/screenshots/login-view.png",
                testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
        seleniumHandler.setHighlightEnabled(true);
        //---------------------------------------------------------------------------------------
        logHeader("What is Project Management");
        //---------------------------------------------------------------------------------------
        paul.narrate(NORMAL, "Kassandra is secured using OAuth 2 and OpenID Connect. You just saw the standard login page of your organisation's identity provider. No separate passwords, no extra user database to maintain.").pause();
        paul.narrate(NORMAL, "So — what is project management? At its core it is about planning a project and then monitoring its execution from three perspectives: cost, schedule, and quality.").pause();
        paul.narrate(NORMAL, "You need to know: what needs to be done, who will do it, how long it will take, and whether the team is on track to deliver on time. Kassandra helps you answer all of those questions.").pause();

        //---------------------------------------------------------------------------------------
        logHeader("Products, Versions, Features, Sprints hierarchy");
        //---------------------------------------------------------------------------------------
        paul.narrate(NORMAL, "The first page you see after logging in is the Products list. A Product is whatever your team is building — a software service, a firmware release, a mobile application.").pause();
        paul.narrate(NORMAL, "Let us navigate into one of our products.");
        productListViewTester.selectProduct(productName);

        paul.narrate(NORMAL, "Each product can have one or more Versions — representing specific release milestones such as one-dot-zero or two-dot-zero.").pause();
        versionListViewTester.selectVersion(versionName);

        paul.narrate(NORMAL, "Each version is divided into Features — the major areas of work you want to plan and deliver. Think of a feature as a significant piece of functionality: a configuration API, a login system, or a reporting dashboard.").pause();
        featureListViewTester.selectFeature(featureName);

        paul.narrate(NORMAL, "And each feature is broken into one or more Sprints — focused, time-boxed chunks of work. This four-level hierarchy — Product, Version, Feature, Sprint — keeps even large projects organised and easy to navigate.").pause();

        //---------------------------------------------------------------------------------------
        logHeader("Users");
        //---------------------------------------------------------------------------------------
        paul.narrateAsync(NORMAL, "To execute work you need people. Let us look at how Kassandra manages team members. I will open the user menu in the top right corner and navigate to the Users page. This section is only visible to administrators.");
        userListViewTester.switchToUserListView(
                testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo),
                "christopher.paul@kassandra.org", "password");

        paul.narrate(NORMAL, "This is the Users page. Every team member who needs to log in to Kassandra or be assigned to tasks must have a user record here.").pause();
        paul.narrate(NORMAL, "The most important field is the email address — it must exactly match the user's identity in your OIDC provider. Kassandra uses it to link the login session to the right person. You also record each user's first and last working day so the system knows their active employment period.").pause();

        //---------------------------------------------------------------------------------------
        logHeader("User Groups");
        //---------------------------------------------------------------------------------------
        paul.narrateAsync(NORMAL, "Now let us look at User Groups — the feature that controls who can access which products.");
        userGroupListViewTester.switchToUserGroupListView(
                testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo),
                "christopher.paul@kassandra.org", "password");

        paul.narrate(NORMAL, "User Groups let you organise team members and control which products they can access. Instead of granting access to each product for every individual user, you assign a group to a product once — through the access control field in the product dialog.").pause();
        paul.narrate(NORMAL, "All members of that group automatically inherit access to the product and all its versions, features, and sprints. When a new developer joins, just add them to the group and they instantly get access to everything.").pause();

        //---------------------------------------------------------------------------------------
        logHeader("User Availability");
        //---------------------------------------------------------------------------------------
        paul.narrateAsync(NORMAL, "Kassandra needs to understand each user's working capacity to generate realistic schedules. It tracks four data points. First: Availability.");
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_AVAILABILITY);

        paul.narrate(NORMAL, "Availability defines what percentage of a user's working time is dedicated to project tasks during a given period. One hundred percent means fully committed to project work. Fifty percent means the person is splitting their time — perhaps doing support or administrative work alongside project tasks. This feeds directly into the scheduling engine.").pause();

        //---------------------------------------------------------------------------------------
        logHeader("User Location");
        //---------------------------------------------------------------------------------------
        paul.narrateAsync(NORMAL, "Second: Location.");
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_LOCATION);

        paul.narrate(NORMAL, "Location records where a user is based — country and region. Kassandra uses this to automatically apply the correct public holidays for that region. If you relocate, just add a new location record and the holidays update automatically from that date forward. No manual holiday maintenance needed.").pause();

        //---------------------------------------------------------------------------------------
        logHeader("Work Week");
        //---------------------------------------------------------------------------------------
        paul.narrateAsync(NORMAL, "Third: Work Week.");
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_WORK_WEEK);
        seleniumHandler.waitForElementToBeClickable(UserWorkWeekListView.PAGE_TITLE);

        paul.narrate(NORMAL, "Work Week defines how many hours per day a user works, and on which days of the week. Not everyone works a standard Monday to Friday eight-hour day. Part-time employees, people in different time zones, and those on compressed schedules are all handled here.").pause();

        //---------------------------------------------------------------------------------------
        logHeader("Off Days");
        //---------------------------------------------------------------------------------------
        paul.narrateAsync(NORMAL, "And fourth: Off Days.");
        seleniumHandler.click(MainLayout.ID_USER_MENU);
        seleniumHandler.click(MainLayout.ID_USER_MENU_OFF_DAYS);

        paul.narrate(NORMAL, "Off Days record personal absences — vacations, sick leave, and business trips. Together, availability, location, work week, and off days give Kassandra a complete picture of each person's true working capacity, so the schedule it generates is realistic — not just theoretical.").pause();
        //---------------------------------------------------------------------------------------
        logHeader("Backlog — Sprint Planning");
        //---------------------------------------------------------------------------------------
        paul.narrateAsync(NORMAL, "Now for the heart of project planning: the Backlog page.");
        seleniumHandler.click(Backlog.MENU_ITEM_ID);
        seleniumHandler.waitForElementToBeClickable(RenderUtil.GANTT_CHART);

        paul.narrate(NORMAL, "Sprint planning is done here in the Backlog page. This is where you define and estimate all the work for a sprint.").pause();
        paul.narrate(NORMAL, "Every unit of work is a Task. For each task you provide two effort estimates: a minimum — the optimistic case where everything goes smoothly — and a maximum, which accounts for every risk, uncertainty, and unexpected complication that might arise.").pause();
        paul.narrate(NORMAL, "This range is fundamental to Kassandra's approach. Rather than pretending you can predict exactly how long something takes, you acknowledge the uncertainty up front. This is how you produce honest, defensible project plans.").pause();
        paul.narrate(NORMAL, "Tasks can be grouped into Stories — logical collections of related work. You can also define dependencies between tasks: for example, you can only build the roof after the walls are finished. Kassandra uses these dependencies to schedule work in the correct order.").pause();
        paul.narrate(NORMAL, "Look at the Gantt chart here. Kassandra generates this automatically. It shows each task's planned start time, duration, and dependencies as a timeline. The scheduling engine also ensures no user is overloaded — it distributes work intelligently based on each person's real capacity.").pause();

        //---------------------------------------------------------------------------------------
        logHeader("Active Sprints — Progress Reporting");
        //---------------------------------------------------------------------------------------
        paul.narrateAsync(NORMAL, "Once a sprint is started, team members report their progress here in the Active Sprints view.");
        seleniumHandler.click(ActiveSprints.MENU_ITEM_ID);
        seleniumHandler.waitForElementToBeClickable(ActiveSprints.ID_CLEAR_FILTERS_BUTTON);

        paul.narrate(NORMAL, "Each task appears as a card in one of three lanes: To-Do, In-Progress, and Done. Developers move tasks between lanes by dragging them as they work.").pause();
        paul.narrate(NORMAL, "By clicking a task card, you open the Worklog dialog. There you record the time spent and update how much work remains. This keeps the entire team informed about real progress — not just what was planned.").pause();
        paul.narrate(NORMAL, "The board can be filtered by user and by sprint, and organised by feature or story — so each team member sees exactly the tasks relevant to them.").pause();

        //---------------------------------------------------------------------------------------
        logHeader("Sprint Quality Board — Monitoring");
        //---------------------------------------------------------------------------------------
        paul.narrateAsync(NORMAL, "The Sprint Quality Board is where managers and product owners monitor the health of a sprint in real time.");
        seleniumHandler.click(SprintQualityBoard.MENU_ITEM_ID);
        seleniumHandler.waitForElementToBeClickable(RenderUtil.GANTT_CHART);
        seleniumHandler.setComboBoxValue(SprintQualityBoard.SPRINT_SELECTOR_ID, "London");

        paul.narrate(NORMAL, "At the top, you see key sprint statistics — total effort, elapsed time, and completion percentage.").pause();
        paul.narrate(NORMAL, "The Gantt chart shows the planned schedule with current progress overlaid. You can see at a glance whether tasks are running on time or slipping.").pause();
        paul.narrate(NORMAL, "And then the Burn Down Chart. This is one of the most powerful monitoring tools in Kassandra. The shaded band represents the range between the total minimum and maximum effort estimates for the sprint.").pause();
        paul.narrate(NORMAL, "As the team logs work each day, the actual line drops through this band. If it stays inside — the sprint is on track. If it rises above the band — the team is slower than the worst case and the sprint is in serious trouble. If it drops below — they are burning through work faster than the best case. This gives you an early warning signal while there is still time to act.").pause();

        //---------------------------------------------------------------------------------------
        logHeader("Kassandra Agent");
        //---------------------------------------------------------------------------------------
        paul.narrateAsync(NORMAL, "One more thing before we close. Kassandra includes a built-in AI agent.");
        seleniumHandler.click(ProductListView.MENU_ITEM_ID);
        seleniumHandler.waitForElementToBeClickable(ProductListView.PRODUCT_LIST_PAGE_TITLE);
        seleniumHandler.click(ProductListView.PRODUCT_AI_PANEL_BUTTON);

        paul.narrate(NORMAL, "The agent understands natural language and can perform most management tasks on your behalf — directly in the context of the page you are currently on.").pause();
        processQueryAndWaitForAnswer("Briefly summarise what you can do for me in two sentences.");
        paul.narrate(NORMAL, "The agent acts using your security context — it can only see and modify what you are permitted to access. Create products, manage users, add sprints, query reports — all through a conversation. There is a dedicated video for the Kassandra Agent if you want to explore all its capabilities in depth.").pause();

        //---------------------------------------------------------------------------------------
        logHeader("Closing");
        //---------------------------------------------------------------------------------------
        paul.narrate(NORMAL, "That is Kassandra — plan with confidence, deliver with clarity. An open-source, self-hosted project management server that guides you from the first task estimate all the way to delivery, with full transparency on schedule, cost, and quality.").pause();
        paul.narrate(NORMAL, "Check the other videos in this series for deep dives into each topic. Thanks for watching!");
        paul.pauseIfDisabled(3000);
        seleniumHandler.showOverlay(video.getTitle(), InstructionVideo.COPYLEFT_SUBTITLE);
        seleniumHandler.waitUntilBrowserClosed(5000);
    }

    /**
     * Provides the single deterministic {@link RandomCase} used to drive the overview video.
     *
     * @return list containing exactly one {@link RandomCase}
     */
    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{
                new RandomCase(3, OffsetDateTime.parse("2025-08-11T08:00:00+01:00"), LocalDate.parse("2025-08-04"),
                        Duration.ofDays(10), 2, 2, 2, 2, 2, 2, 1, 5, 5, 8, 8, 6, 7)
        };
        return Arrays.stream(randomCases).toList();
    }
}


