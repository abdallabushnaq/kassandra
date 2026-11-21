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

package de.bushnaq.abdalla.kassandra.ui.view;

import de.bushnaq.abdalla.kassandra.dto.Feature;
import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.dto.Version;
import de.bushnaq.abdalla.kassandra.rest.api.ProductAclApi;
import de.bushnaq.abdalla.kassandra.ui.MainLayout;
import de.bushnaq.abdalla.kassandra.ui.component.TaskGrid;
import de.bushnaq.abdalla.kassandra.ui.dialog.ConfirmDialog;
import de.bushnaq.abdalla.kassandra.ui.dialog.DependencyDialog;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.ChartIds;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.util.AboutViewTester;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Browser regression tests for backlog planning.
 */
@Tag("IntegrationUiTest")
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {"server.port=${test.server.port:0}", "spring.security.basic.enabled=false"}
)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BacklogIT extends AbstractKeycloakUiTestUtil {
    @Autowired
    private AboutViewTester          aboutViewTester;
    @Autowired
    private ProductAclApi            productAclApi;
    @Autowired
    private HumanizedSeleniumHandler seleniumHandler;

    private Sprint createSprint(String name) {
        Product product = new Product();
        product.setName(name + " product");
        product = peg.productApi.persist(product);
        Version version = new Version();
        version.setName("Backlog test version");
        version.setProductId(product.getId());
        version = peg.versionApi.persist(version);
        Feature feature = new Feature();
        feature.setName("Backlog test feature");
        feature.setVersionId(version.getId());
        feature = peg.featureApi.persist(feature);
        return createSprint(feature, name);
    }

    private Sprint createSprint(Feature feature, String name) {
        Sprint sprint = new Sprint();
        sprint.setName(name);
        sprint.setFeatureId(feature.getId());
        sprint.setStart(LocalDateTime.of(2025, 5, 5, 9, 0));
        return peg.sprintApi.persist(sprint);
    }

    private void openSprint(Sprint sprint) throws Exception {
        aboutViewTester.login("christopher.paul@kassandra.org", "password", null, null, null);
        seleniumHandler.getAndCheck("http://localhost:" + allocatedPort + "/ui/" + Backlog.ROUTE
                + "?sprint=" + sprint.getId() + "&users=");
        seleniumHandler.waitForChartRendered(ChartIds.GANTT_CHART);
    }

    private Task taskNamed(Sprint sprint, String name) {
        return peg.taskApi.getAll(sprint.getId()).stream()
                .filter(task -> name.equals(task.getName()))
                .findFirst().orElseThrow();
    }

    private void enterEditMode(String taskName) {
        seleniumHandler.click(Backlog.EDIT_BUTTON_ID);
        seleniumHandler.waitUntil(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("vaadin-text-field[id='" + TaskGrid.TASK_GRID_NAME_PREFIX + taskName + "']")));
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    void filtersAndSprintSelectionSurviveNavigation() throws Exception {
        Sprint  first   = createSprint("First sprint");
        Feature feature = peg.featureApi.getById(first.getFeatureId());
        Sprint  second  = createSprint(feature, "Second sprint");
        User    user    = peg.userApi.getByEmail("christopher.paul@kassandra.org").orElseThrow();
        User other = peg.addUser("Backlog colleague", "backlog.colleague@example.org", "USER", "de", "nw",
                LocalDate.of(2025, 1, 1), peg.eg.generateUserColor(peg.getUserIndex()), 1f, null);
        Task story = peg.addParentTask("Planning story", first, null, null);
        peg.addTask("Find this task", "1h", "2h", user, first, story, null);
        peg.addTask("Other task", "1h", "2h", other, first, story, null);
        Sprint backlog      = peg.sprintApi.getBacklogSprint();
        Task   backlogStory = peg.addParentTask("Backlog story", backlog, null, null);
        peg.addTask("Other backlog task", "1h", "2h", other, backlog, backlogStory, null);
        peg.addParentTask("Second sprint story", second, null, null);

        openSprint(first);
        seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_NAME_PREFIX, "Find this task");
        seleniumHandler.setTextField(Backlog.SEARCH_FIELD_ID, "find THIS");
        seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_NAME_PREFIX, "Find this task");
        seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_NAME_PREFIX, "Planning story");
        seleniumHandler.ensureIsNotInList(TaskGrid.TASK_GRID_NAME_PREFIX, "Other task");
        seleniumHandler.click(Backlog.CLEAR_FILTER_BUTTON_ID);
        seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_NAME_PREFIX, "Other task");

        seleniumHandler.setMultiSelectComboBoxValue(Backlog.USER_SELECTOR_ID, new String[]{user.getName()});
        seleniumHandler.waitUntil(driver -> driver.getCurrentUrl().contains("users=" + user.getId()));
        seleniumHandler.ensureIsNotInList(TaskGrid.TASK_GRID_NAME_PREFIX, "Other task");
        seleniumHandler.ensureIsNotInList(TaskGrid.TASK_GRID_NAME_PREFIX, "Other backlog task");
        seleniumHandler.getDriver().navigate().refresh();
        seleniumHandler.waitForChartRendered(ChartIds.GANTT_CHART);
        if (Boolean.getBoolean("frontend.coverage")) {
            assertNotNull(seleniumHandler.executeJavaScript("return localStorage.getItem('__kassandra_frontend_coverage__');"),
                    "Coverage from the previous page must survive refresh");
        }
        seleniumHandler.waitUntil(driver -> driver.getCurrentUrl().contains("users=" + user.getId()));
        seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_NAME_PREFIX, "Find this task");
        seleniumHandler.ensureIsNotInList(TaskGrid.TASK_GRID_NAME_PREFIX, "Other task");
        seleniumHandler.ensureIsNotInList(TaskGrid.TASK_GRID_NAME_PREFIX, "Other backlog task");
        seleniumHandler.click(Backlog.CLEAR_FILTER_BUTTON_ID);
        seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_NAME_PREFIX, "Other task");
        seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_NAME_PREFIX, "Other backlog task");
        seleniumHandler.waitUntil(driver -> Boolean.TRUE.equals(seleniumHandler.executeJavaScript(
                "const params = new URL(location.href).searchParams; return params.has('users') && params.get('users') === '';")));
        seleniumHandler.getDriver().navigate().refresh();
        seleniumHandler.waitForChartRendered(ChartIds.GANTT_CHART);
        seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_NAME_PREFIX, "Other task");
        seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_NAME_PREFIX, "Other backlog task");
        seleniumHandler.setComboBoxValue(Backlog.SPRINT_SELECTOR_ID, "Second sprint");
        seleniumHandler.waitUntil(driver -> driver.getCurrentUrl().contains("sprint=" + second.getId()));
        seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_NAME_PREFIX, "Second sprint story");
        seleniumHandler.getDriver().navigate().refresh();
        seleniumHandler.waitForChartRendered(ChartIds.GANTT_CHART);
        seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_NAME_PREFIX, "Second sprint story");

        String mainWindow = seleniumHandler.getDriver().getWindowHandle();
        long savedBeforePopup = Boolean.getBoolean("frontend.coverage") ? savedCoverageStatements() : 0;
        seleniumHandler.click(Backlog.OPEN_GANTT_CHART_BUTTON_ID);
        seleniumHandler.waitUntil(ExpectedConditions.numberOfWindowsToBe(2));
        String chartWindow = seleniumHandler.getDriver().getWindowHandles().stream()
                .filter(handle -> !handle.equals(mainWindow)).findFirst().orElseThrow();
        seleniumHandler.getDriver().switchTo().window(chartWindow);
        seleniumHandler.waitUntil(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#chart svg")));
        long popupStatements = Boolean.getBoolean("frontend.coverage")
                ? ((Number) seleniumHandler.executeJavaScript("""
                return Object.values(window.__coverage__ || {})
                    .flatMap(file => Object.values(file.s))
                    .reduce((sum, count) => sum + count, 0);
                """)).longValue() : 0;
        seleniumHandler.getDriver().close();
        seleniumHandler.getDriver().switchTo().window(mainWindow);
        if (Boolean.getBoolean("frontend.coverage")) {
            assertTrue(popupStatements > 0, "The detached chart must execute instrumented code");
            assertTrue(savedCoverageStatements() >= savedBeforePopup + popupStatements,
                    "Coverage from the detached chart must survive closing its window");
        }
        seleniumHandler.waitForChartRendered(ChartIds.GANTT_CHART);
    }

    private long savedCoverageStatements() {
        return ((Number) seleniumHandler.executeJavaScript("""
                return Object.values(JSON.parse(localStorage.getItem('__kassandra_frontend_coverage__') || '{}'))
                    .flatMap(file => Object.values(file.s))
                    .reduce((sum, count) => sum + count, 0);
                """)).longValue();
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    void createsEditsAndDeletesPlanningItems() throws Exception {
        Sprint sprint = createSprint("Editing sprint");
        openSprint(sprint);

        seleniumHandler.click(Backlog.CREATE_MILESTONE_BUTTON_ID);
        seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_NAME_PREFIX, "New Milestone-0");
        seleniumHandler.click(Backlog.CREATE_STORY_BUTTON_ID);
        seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_NAME_PREFIX, "New Story-1");
        seleniumHandler.click(Backlog.CREATE_TASK_BUTTON_ID);
        seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_NAME_PREFIX, "New Task-2");
        Task milestone = taskNamed(sprint, "New Milestone-0");
        Task story     = taskNamed(sprint, "New Story-1");
        Task task      = taskNamed(sprint, "New Task-2");
        assertTrue(milestone.isMilestone());
        assertEquals(story.getId(), task.getParentTaskId());

        enterEditMode(task.getName());
        seleniumHandler.setTextField(TaskGrid.TASK_GRID_NAME_PREFIX + task.getName(), "Discarded name");
        seleniumHandler.click(Backlog.CANCEL_BUTTON_ID);
        seleniumHandler.waitUntil(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div[id='" + TaskGrid.TASK_GRID_NAME_PREFIX + task.getName() + "']")));
        assertEquals(task.getName(), peg.taskApi.getById(task.getId()).getName());

        enterEditMode(task.getName());
        seleniumHandler.setTextField(TaskGrid.TASK_GRID_NAME_PREFIX + task.getName(), "Saved task");
        seleniumHandler.setTextField(TaskGrid.TASK_GRID_MIN_EST_PREFIX + task.getName(), "3h");
        seleniumHandler.click(Backlog.SAVE_BUTTON_ID);
        seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_NAME_PREFIX, "Saved task");
        assertEquals("Saved task", peg.taskApi.getById(task.getId()).getName());
        assertEquals(Duration.ofHours(3), peg.taskApi.getById(task.getId()).getMinEstimate());

        seleniumHandler.click(TaskGrid.TASK_GRID_DELETE_BUTTON_PREFIX + "Saved task");
        seleniumHandler.closeDialog(ConfirmDialog.CANCEL_BUTTON);
        assertEquals("Saved task", peg.taskApi.getById(task.getId()).getName());
        seleniumHandler.click(TaskGrid.TASK_GRID_DELETE_BUTTON_PREFIX + "Saved task");
        seleniumHandler.closeDialog(ConfirmDialog.CONFIRM_BUTTON);
        seleniumHandler.ensureIsNotInList(TaskGrid.TASK_GRID_NAME_PREFIX, "Saved task");
        assertTrue(peg.taskApi.getAll(sprint.getId()).stream().noneMatch(item -> item.getId().equals(task.getId())));
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    void editsDependenciesAndShowsValidation() throws Exception {
        Sprint sprint      = createSprint("Dependency sprint");
        User   user        = peg.userApi.getByEmail("christopher.paul@kassandra.org").orElseThrow();
        Task   story       = peg.addParentTask("Dependency story", sprint, null, null);
        Task   predecessor = peg.addTask("Predecessor", "1h", "2h", user, sprint, story, null);
        Task   dependent   = peg.addTask("Dependent", "1h", "2h", user, sprint, story, null);
        openSprint(sprint);

        seleniumHandler.setCheckCheckbox(Backlog.EXPAND_TOGGLE_BUTTON_ID, false);
        seleniumHandler.ensureIsNotInList(TaskGrid.TASK_GRID_NAME_PREFIX, dependent.getName());
        seleniumHandler.setCheckCheckbox(Backlog.EXPAND_TOGGLE_BUTTON_ID, true);
        seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_NAME_PREFIX, dependent.getName());

        enterEditMode(dependent.getName());
        seleniumHandler.setTextField(TaskGrid.TASK_GRID_MIN_EST_PREFIX + dependent.getName(), "invalid");
        seleniumHandler.sendKeys(TaskGrid.TASK_GRID_MIN_EST_PREFIX + dependent.getName(), "\t");
        seleniumHandler.waitUntil(ExpectedConditions.attributeToBe(
                By.id(TaskGrid.TASK_GRID_MIN_EST_PREFIX + dependent.getName()), "invalid", "true"));
        seleniumHandler.setTextField(TaskGrid.TASK_GRID_MIN_EST_PREFIX + dependent.getName(), "1h");

        seleniumHandler.click(TaskGrid.TASK_GRID_DEPENDENCY_PREFIX + dependent.getName());
        seleniumHandler.setTextField(DependencyDialog.QUICK_EDIT_ID, predecessor.getOrderId().toString());
        seleniumHandler.closeDialog(DependencyDialog.CANCEL_BUTTON);
        assertTrue(peg.taskApi.getById(dependent.getId()).getPredecessors().stream().noneMatch(
                relation -> relation.isVisible() && predecessor.getId().equals(relation.getPredecessorId())));
        seleniumHandler.click(TaskGrid.TASK_GRID_DEPENDENCY_PREFIX + dependent.getName());
        seleniumHandler.setTextField(DependencyDialog.QUICK_EDIT_ID, predecessor.getOrderId().toString());
        seleniumHandler.closeDialog(DependencyDialog.SAVE_BUTTON);
        seleniumHandler.click(Backlog.SAVE_BUTTON_ID);
        seleniumHandler.waitUntil(driver -> peg.taskApi.getById(dependent.getId()).getPredecessors().stream()
                .anyMatch(relation -> relation.isVisible() && predecessor.getId().equals(relation.getPredecessorId())));
        List<?> predecessors = peg.taskApi.getById(dependent.getId()).getPredecessors();
        assertFalse(predecessors.isEmpty());
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    void anonymousUsersAreRedirectedToLogin() {
        Sprint sprint = createSprint("Protected sprint");
        seleniumHandler.get("http://localhost:" + allocatedPort + "/ui/" + Backlog.ROUTE
                + "?sprint=" + sprint.getId());
        seleniumHandler.waitForElementToBeClickable(LoginView.OIDC_LOGIN_BUTTON);
        assertTrue(seleniumHandler.getDriver().findElements(By.id(Backlog.EDIT_BUTTON_ID)).isEmpty());
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    void movesStoryAndItsTasksBetweenSprintAndBacklog() throws Exception {
        Sprint backlog = peg.sprintApi.getBacklogSprint();
        Sprint sprint  = createSprint(peg.featureApi.getById(backlog.getFeatureId()), "Transfer sprint");
        User   user    = peg.userApi.getByEmail("christopher.paul@kassandra.org").orElseThrow();
        Task   story   = peg.addParentTask("Transfer story", sprint, null, null);
        Task   child   = peg.addTask("Transfer child", "1h", "2h", user, sprint, story, null);
        peg.addParentTask("Backlog destination", backlog, null, null);
        openSprint(sprint);
        seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_NAME_PREFIX, "Backlog destination");
        seleniumHandler.dragAndDrop(TaskGrid.TASK_GRID_NAME_PREFIX + story.getName(),
                TaskGrid.TASK_GRID_NAME_PREFIX + "Backlog destination");
        seleniumHandler.waitUntil(driver -> backlog.getId().equals(peg.taskApi.getById(story.getId()).getSprintId())
                && backlog.getId().equals(peg.taskApi.getById(child.getId()).getSprintId()));
        Task movedChild = peg.taskApi.getById(child.getId());
        assertEquals(backlog.getId(), movedChild.getSprintId());
        assertEquals(story.getId(), movedChild.getParentTaskId());
        assertTrue(peg.taskApi.getAll(sprint.getId()).stream().noneMatch(item -> item.getId().equals(story.getId())));
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    void authenticatedUserCanOpenAnAuthorizedSprint() throws Exception {
        Sprint sprint = createSprint("Authorized sprint");
        User user = peg.addUser("Jennifer Holleman", "jennifer.holleman@kassandra.org", "USER", "de", "nw",
                LocalDate.of(2025, 1, 1), peg.eg.generateUserColor(peg.getUserIndex()), 1f, null);
        var provider = oidcProviderService.getProviders().stream()
                .filter(candidate -> "Test Keycloak".equals(candidate.getDisplayName())).findFirst().orElseThrow();
        oidcIdentityService.linkIdentity(user.getId(), provider.getRegistrationId(), getKeycloakUserSubject(user.getEmail()));
        Feature feature = peg.featureApi.getById(sprint.getFeatureId());
        Version version = peg.versionApi.getById(feature.getVersionId());
        productAclApi.grantUserAccess(version.getProductId(), user.getId());
        peg.addParentTask("Authorized story", sprint, null, null);
        Sprint restricted = createSprint("Restricted sprint");
        peg.addParentTask("Restricted story", restricted, null, null);

        aboutViewTester.login(user.getEmail(), "password", null, null, null);
        seleniumHandler.getAndCheck("http://localhost:" + allocatedPort + "/ui/" + Backlog.ROUTE
                + "?product=" + version.getProductId() + "&version=" + version.getId()
                + "&feature=" + feature.getId() + "&sprint=" + sprint.getId() + "&users=");
        seleniumHandler.waitForChartRendered(ChartIds.GANTT_CHART);
        seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_NAME_PREFIX, "Authorized story");
        assertTrue(seleniumHandler.findElement(By.id(Backlog.EDIT_BUTTON_ID)).isDisplayed());
        assertTrue(seleniumHandler.findElement(By.id(MainLayout.ID_BREADCRUMBS)).getText().contains("Backlog"));

        seleniumHandler.get("http://localhost:" + allocatedPort + "/ui/" + Backlog.ROUTE
                + "?sprint=" + restricted.getId() + "&users=");
        seleniumHandler.waitForChartRendered(ChartIds.GANTT_CHART);
        seleniumHandler.ensureIsInList(TaskGrid.TASK_GRID_NAME_PREFIX, "Authorized story");
        seleniumHandler.ensureIsNotInList(TaskGrid.TASK_GRID_NAME_PREFIX, "Restricted story");
        assertFalse(seleniumHandler.getDriver().getCurrentUrl().contains(restricted.getId().toString()));
    }
}
