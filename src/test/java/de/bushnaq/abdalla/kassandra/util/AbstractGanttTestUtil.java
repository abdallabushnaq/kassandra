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

package de.bushnaq.abdalla.kassandra.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import de.bushnaq.abdalla.kassandra.Context;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.report.burndown.BurnDownChart;
import de.bushnaq.abdalla.kassandra.report.burndown.RenderDao;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttChart;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttContext;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttUtil;
import de.bushnaq.abdalla.profiler.Profiler;
import de.bushnaq.abdalla.profiler.SampleType;
import de.bushnaq.abdalla.util.GanttErrorHandler;
import de.bushnaq.abdalla.util.Util;
import de.bushnaq.abdalla.util.date.DateUtil;
import jakarta.annotation.PostConstruct;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectFile;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@TestMethodOrder(MethodOrderer.MethodName.class)
public class AbstractGanttTestUtil extends AbstractEntityGenerator {
    private static final String                 ANSI_BLUE                 = "\u001B[36m";
    private static final String                 ANSI_GREEN                = "\u001B[32m";
    private static final String                 ANSI_RED                  = "\u001B[31m";
    private static final String                 ANSI_RESET                = "\u001B[0m";    // Declaring ANSI_RESET so that we can reset the color
    private static final String                 ANSI_YELLOW               = "\u001B[33m";
    @Autowired
    protected            Context                context;
    @Autowired
    private              H2DatabaseStateManager databaseStateManager;
    public final         DateTimeFormatter      dtfymdhmss                = DateTimeFormatter.ofPattern("yyyy.MMM.dd HH:mm:ss.SSS");
    protected final      List<Throwable>        exceptions                = new ArrayList<>();
    protected            String                 testReferenceResultFolder = "test-reference-results";
    protected            String                 testResultFolder          = "test-results";

    protected void addOneProduct(String sprintName) {
        int count = 1;

        for (int i = 0; i < count; i++) {
            Product product = addProduct(nameGenerator.generateProductName(i));
            Version version = addVersion(product, nameGenerator.generateVersionName(i));
            Feature feature = addFeature(version, nameGenerator.generateFeatureName(i));
            addSprint(feature, sprintName);
        }
        testProducts();
    }

    private void compareResults(TestInfo testInfo) throws IOException {
        String expectedJson = Files.readString(Paths.get(testReferenceResultFolder, TestInfoUtil.getTestMethodName(testInfo) + ".json"));
        String actualJson   = Files.readString(Paths.get(testResultFolder, TestInfoUtil.getTestMethodName(testInfo) + ".json"));

        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {
            };
            Map<String, Object> referenceMap = objectMapper.readValue(expectedJson, typeRef);
            Map<String, Object> map          = objectMapper.readValue(actualJson, typeRef);

            // Compare users
            Map<String, User> referenceUsers = objectMapper.convertValue(referenceMap.get("users"), new TypeReference<Map<String, User>>() {
            });
            Map<String, User> users = objectMapper.convertValue(map.get("users"), new TypeReference<Map<String, User>>() {
            });
            assertEquals(referenceUsers.size(), users.size(), "Number of users differs");
            for (String key : referenceUsers.keySet()) {
                assertTrue(users.containsKey(key), "Missing user: " + referenceUsers.get(key).getName());
                assertUserEquals(referenceUsers.get(key), users.get(key));
            }

            // Compare sprints
            Sprint referenceSprint = objectMapper.convertValue(referenceMap.get("sprint"), Sprint.class);
            Sprint sprint          = objectMapper.convertValue(map.get("sprint"), Sprint.class);
            assertSprintEquals(referenceSprint, sprint);

            // Compare tasks
            List<Task> referenceTasks = objectMapper.convertValue(referenceMap.get("tasks"), new TypeReference<List<Task>>() {
            });
            List<Task> tasks = objectMapper.convertValue(map.get("tasks"), new TypeReference<List<Task>>() {
            });
            for (Task task : tasks)
                sprint.addTask(task);
            for (Task task : referenceTasks)
                referenceSprint.addTask(task);
            {
                GanttContext gc = new GanttContext();
                gc.allUsers   = new ArrayList<>(referenceUsers.values());
                gc.allSprints = List.of(referenceSprint);
                gc.allTasks   = referenceTasks;
                gc.initialize();
            }
            {
                GanttContext gc = new GanttContext();
                gc.allUsers   = new ArrayList<>(users.values());
                gc.allSprints = List.of(sprint);
                gc.allTasks   = tasks;
                gc.initialize();
            }

            logProjectTasks(testResultFolder + "/" + TestInfoUtil.getTestMethodName(testInfo) + ".json", sprint, testReferenceResultFolder + "/" + TestInfoUtil.getTestMethodName(testInfo) + ".json", referenceSprint);
            compareTasks(tasks, referenceTasks);
        } catch (JsonProcessingException e) {
            fail("Failed to parse JSON: " + e.getMessage());
        }

    }

    protected void compareResults(ProjectFile projectFile, TestInfo testInfo) throws IOException {
        compareResults(testInfo);
    }

    private static void compareTasks(List<Task> tasks, List<Task> referenceTasks) {
        assertEquals(referenceTasks.size(), tasks.size(), "Number of tasks differs");
        for (int i = 0; i < referenceTasks.size(); i++) {
            assertTaskEquals(referenceTasks.get(i), tasks.get(i));
        }
    }

    private RenderDao createRenderDao(Context context, Sprint sprint, String column, LocalDateTime now, int chartWidth, int chartHeight, String link) {
        RenderDao dao = new RenderDao();
        dao.context            = context;
        dao.column             = column;
        dao.sprintName         = column + "-burn-down";
        dao.link               = link;
        dao.start              = sprint.getStart();
        dao.now                = now;
        dao.end                = sprint.getEnd();
        dao.release            = sprint.getReleaseDate();
        dao.chartWidth         = chartWidth;
        dao.chartHeight        = chartHeight;
        dao.sprint             = sprint;
        dao.estimatedBestWork  = DateUtil.add(sprint.getWorked(), sprint.getRemaining());
        dao.estimatedWorstWork = null;
        dao.maxWorked          = DateUtil.add(sprint.getWorked(), sprint.getRemaining());
        dao.remaining          = sprint.getRemaining();
        dao.worklog            = sprint.getWorklogs();
        dao.worklogRemaining   = sprint.getWorklogRemaining();
        dao.cssClass           = "scheduleWithMargin";
        dao.graphicsTheme      = context.parameters.graphicsTheme;
        return dao;
    }

    protected void generateBurndownChart(TestInfo testInfo, long sprintId) throws Exception {
        generateBurndownChart(testInfo, sprintId, 0, 36 * 20);
    }

    protected void generateBurndownChart(TestInfo testInfo, long sprintId, int width, int height) throws Exception {
//        initializeInstances();
//        sprint.initialize();
//        sprint.initUserMap(userApi.getAll(sprint.getId()));
//        sprint.initTaskMap(taskApi.getAll(sprint.getId()), worklogApi.getAll(sprint.getId()));
        Sprint sprint = sprintApi.getById(sprintId);
        sprint.initialize();
        sprint.initUserMap(userApi.getAll(sprintId));
        sprint.initTaskMap(taskApi.getAll(sprintId), worklogApi.getAll(sprintId));
        sprint.recalculate(ParameterOptions.getLocalNow());
//        sprint.recalculate(ParameterOptions.getLocalNow());
        RenderDao     dao         = createRenderDao(context, sprint, TestInfoUtil.getTestMethodName(testInfo), ParameterOptions.getLocalNow(), width, height,  /*urlPrefix +*/ "sprint-" + sprint.getId() + "/sprint.html");
        BurnDownChart chart       = new BurnDownChart("/", dao);
        String        description = testInfo.getDisplayName().replace("_", "-");
        chart.render(Util.generateCopyrightString(ParameterOptions.getLocalNow()), description, testResultFolder);
    }

    protected void generateGanttChart(TestInfo testInfo, long sprintId, ProjectFile projectFile) throws Exception {
        Sprint sprint = sprintApi.getById(sprintId);
        sprint.initialize();
        sprint.initUserMap(userApi.getAll(sprintId));
        sprint.initTaskMap(taskApi.getAll(sprintId), worklogApi.getAll(sprintId));
        sprint.recalculate(ParameterOptions.getLocalNow());
        GanttChart chart = new GanttChart(context, "", "/", "Gantt Chart", TestInfoUtil.getTestMethodName(testInfo) + "-gant-chart", exceptions, ParameterOptions.getLocalNow(), false, sprint/*, 1887, 1000*/, "scheduleWithMargin", context.parameters.graphicsTheme);
//        String     description = testCaseInfo.getDisplayName().replace("_", "-");
        String description = TestInfoUtil.getTestMethodName(testInfo);
        chart.render(Util.generateCopyrightString(ParameterOptions.getLocalNow()), description, testResultFolder);
        compareResults(projectFile, testInfo);
    }

    protected void generateOneProduct(TestInfo testInfo) throws Exception {
        ParameterOptions.setNow(OffsetDateTime.parse("2025-01-01T08:00:00+01:00"));
        addOneProduct(generateTestCaseName(testInfo));
    }

    protected void generateProductsIfNeeded(TestInfo testInfo, RandomCase randomCase) throws Exception {
        ParameterOptions.setNow(randomCase.getNow());
        String testCaseName = this.getClass().getName() + "-" + testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex();
        // Create a snapshot name based on the test case
        String snapshotName = testInfo.getTestClass().get().getSimpleName() + "-" + randomCase.getTestCaseIndex();
        // Try to find and load an existing database snapshot
        String  latestSnapshot = databaseStateManager.findLatestSnapshot(snapshotName);
        boolean dataLoaded     = false;
        if (latestSnapshot != null) {
            logger.info("Found existing database snapshot: {}. Attempting to load...", latestSnapshot);
            dataLoaded = databaseStateManager.importDatabaseSnapshot(latestSnapshot);
        }
        // If no snapshot was found or loading failed, generate data the regular way
        if (!dataLoaded) {
            logger.info("Generating fresh test data (this might take a few minutes)...");
            generateProductsInternal(testInfo, randomCase);
            // After successful data generation, export a snapshot for future test runs
            databaseStateManager.exportDatabaseSnapshot(snapshotName);
        } else {
            logger.info("Successfully loaded test data from snapshot. Skipping data generation.");
        }
    }

    private void generateProductsInternal(TestInfo testInfo, RandomCase randomCase) throws Exception {
        random.setSeed(randomCase.getSeed());
        expectedUsers.clear();
        try (Profiler pc = new Profiler(SampleType.JPA)) {
            addRandomUsers(randomCase.getMaxNumberOfUsers());
        }
        Profiler.log("generating users for test case " + randomCase.getTestCaseIndex());
        {
            int numberOfProducts = generateRandomValue(randomCase.getMinNumberOfProducts(), randomCase.getMaxNumberOfProducts());
            try (Profiler pc = new Profiler(SampleType.JPA)) {
                for (int p = 0; p < numberOfProducts; p++) {
                    Product product          = addProduct(nameGenerator.generateProductName(productIndex));
                    int     numberOfVersions = generateRandomValue(randomCase.getMinNumberOfVersions(), randomCase.getMaxNumberOfVersions());
                    for (int v = 0; v < numberOfVersions; v++) {
                        Version version          = addVersion(product, nameGenerator.generateVersionName(v));
                        int     numberOfFeatures = generateRandomValue(randomCase.getMinNumberOfFeatures(), randomCase.getMaxNumberOfFeatures());
                        for (int f = 0; f < numberOfFeatures; f++) {
                            Feature feature         = addFeature(version, nameGenerator.generateFeatureName(featureIndex));
                            int     numberOfSprints = generateRandomValue(randomCase.getMinNumberOfSprints(), randomCase.getMaxNumberOfSprints());
                            for (int s = 0; s < numberOfSprints; s++) {
                                generateSprint(testInfo, randomCase, feature);
                            }
                        }
                    }
                }
            }
            Profiler.log("generate Products for test case -" + randomCase.getTestCaseIndex());
        }
        generateRandomSickDays();
    }

    private int generateRandomValue(int minNumber, int maxNumberExclusive) {
        if (maxNumberExclusive - minNumber > 0)
            return random.nextInt(maxNumberExclusive - minNumber) + minNumber;
        else
            return minNumber;
    }

    private void generateSprint(TestInfo testInfo, RandomCase randomCase, Feature project) throws Exception {
        int numberOfUsers = randomCase.getMaxNumberOfUsers();
//        System.out.println("Number of users=" + numberOfUsers);
        try (Profiler pc1 = new Profiler(SampleType.JPA)) {
            // Capture current sprint index before creating the sprint
            int    currentSprintIndex = getCurrentSprintIndex();
            Sprint generatedSprint    = addRandomSprint(project);
            Sprint sprint             = sprintApi.getById(generatedSprint.getId());
            if (randomCase.getMaxNumberOfStories() > 0) {
                int           numberOfStories = random.nextInt(randomCase.getMaxNumberOfStories()) + 1;
                LocalDateTime startDateTime   = randomCase.getMinStartDate().plusDays(random.nextInt((int) randomCase.getMaxStartDateShift().toDays())).atStartOfDay().plusHours(8);
                Task          startMilestone  = addTask(sprint, null, "Start", startDateTime, Duration.ZERO, null, null, null, TaskMode.MANUALLY_SCHEDULED, true);

                // Get shuffled story names for this sprint to ensure variety
                List<String> sprintStoryNames = nameGenerator.getShuffledStoryNames(currentSprintIndex, numberOfStories);

                for (int f = 0; f < numberOfStories; f++) {
                    String storyName     = sprintStoryNames.get(f);
                    Task   story         = addParentTask(storyName, sprint, null, startMilestone);
                    int    numberOfTasks = random.nextInt(randomCase.getMaxNumberOfTasks()) + 1;
                    for (int t = 0; t < numberOfTasks; t++) {
                        int userIndex = random.nextInt(numberOfUsers);
//                    System.out.println("User index=" + userIndex);
                        User   user             = expectedUsers.stream().toList().get(userIndex);
                        String duration         = String.format("%dh", (int) (random.nextFloat(randomCase.getMaxDurationDays() * 7.5f) + 1));
                        String workName         = NameGenerator.generateWorkName(storyName, t);
                        Task   depenedenycyTask = null;
                        if (random.nextFloat(1) > 0.5f) {
                            int tries = 8;
                            do {
                                depenedenycyTask = sprint.getTasks().get(random.nextInt(sprint.getTasks().size()));
                                //make sure this task is not a parent of our parent and not a milestone
                                if (depenedenycyTask.isMilestone() || depenedenycyTask.isAncestorOf(story)) {
                                    depenedenycyTask = null;
                                    tries--;
                                }
                            }
                            while (depenedenycyTask == null && tries > 0);
                        }
                        addTask(workName, duration, null, user, sprint, story, depenedenycyTask);
                    }
                }
            }
            try (Profiler pc2 = new Profiler(SampleType.CPU)) {
                sprint.initialize();
            }
            sprint.initUserMap(userApi.getAll(sprint.getId()));
            sprint.initTaskMap(taskApi.getAll(sprint.getId()), worklogApi.getAll(sprint.getId()));
            try (Profiler pc3 = new Profiler(SampleType.CPU)) {
                levelResources(testInfo, sprint, null);
            }
            generateWorklogs(sprint, ParameterOptions.getLocalNow());
        }
        Profiler.log("generateProductsIfNeeded-" + randomCase.getTestCaseIndex());
    }

    protected String generateTestCaseName(TestInfo testInfo) {
        String displayName = testInfo.getDisplayName();
        String methodName  = TestInfoUtil.getTestMethodName(testInfo);
        if (displayName.startsWith("[")) {
            //parametrized test case
            //[1] mppFileName=references\CREQ11793 Siemens OMS 2.0 Ph2 SMIME-rcp.mpp
            String bullet = displayName.substring(0, displayName.indexOf(' '));
            if (displayName.contains("mppFileName=")) {
                String mppFileName = displayName.substring(displayName.indexOf('\\') + 1, displayName.lastIndexOf("-rcp."));
                if (mppFileName.length() > 10) {
                    mppFileName = mppFileName.substring(0, 7) + "...";
                }
                return bullet + " " + methodName + " (" + mppFileName + ")";
            } else {
                return bullet + methodName;
            }
        } else {
            return methodName;
        }
    }

    /**
     * Generates worklogs for the tasks in the sprint simulating a team of people working.
     *
     * @param sprint
     * @param now
     */
    protected void generateWorklogs(Sprint sprint, LocalDateTime now) {
        try (Profiler pc = new Profiler(SampleType.CPU)) {

            final long SECONDS_PER_WORKING_DAY = 75 * 6 * 60;
            final long SECONDS_PER_HOUR        = 60 * 60;
            long       oneDay                  = 75 * SECONDS_PER_HOUR / 10;
            Duration   rest                    = Duration.ofSeconds(1);
            //- iterate over the days of the sprint
            for (LocalDate day = sprint.getStart().toLocalDate(); !rest.equals(Duration.ZERO) && now.toLocalDate().isAfter(day); day = day.plusDays(1)) {
                LocalDateTime startOfDay     = day.atStartOfDay().plusHours(8);
                LocalDateTime endOfDay       = day.atStartOfDay().plusHours(16).plusMinutes(30);
                LocalDateTime lunchStartTime = DateUtil.calculateLunchStartTime(day.atStartOfDay());
                LocalDateTime lunchStopTime  = DateUtil.calculateLunchStopTime(day.atStartOfDay());
                rest = Duration.ZERO;
                //iterate over all tasks
                for (Task task : sprint.getTasks()) {
                    if (task.isTask()) {
                        Number availability = task.getAssignedUser().getAvailabilities().getLast().getAvailability();
                        if (!day.isBefore(task.getStart().toLocalDate())) {
                            // Day is after task start
                            if (task.getEffectiveCalendar().isWorkingDate(day)) {
                                //is a working day for this user
                                if (task.getStart().isBefore(startOfDay) || task.getStart().isEqual(startOfDay)) {
                                    if (!task.getRemainingEstimate().isZero()) {
                                        // we have the whole day
                                        double   minPerformance = 0.6f;
                                        double   fraction       = minPerformance + random.nextFloat() * (1 - minPerformance) * 1.2;
                                        Duration maxWork        = Duration.ofSeconds((long) ((fraction * availability.doubleValue() * SECONDS_PER_WORKING_DAY)));
                                        Duration w              = maxWork;
                                        Duration delta          = task.getRemainingEstimate().minus(w);
                                        if (delta.isZero() || delta.isPositive()) {
                                        } else {
                                            w = task.getRemainingEstimate();
                                        }
                                        Worklog worklog = addWorklog(task, task.getAssignedUser(), DateUtil.localDateTimeToOffsetDateTime(day.atStartOfDay()), w, task.getName());
                                        task.addTimeSpent(w);
                                        task.removeRemainingEstimate(w);
                                        task.recalculate();
                                        task.setTaskStatus(TaskStatus.IN_PROGRESS);
                                    } else {
                                        task.setTaskStatus(TaskStatus.DONE);
                                    }
                                }
                            }
                        }
                    }
                    rest = rest.plus(task.getRemainingEstimate());//accumulate the rest
                }
            }
        }
        try (Profiler pc = new Profiler(SampleType.JPA)) {
            sprint.getTasks().forEach(task -> {
                taskApi.update(task);
            });
            sprintApi.update(sprint);
        }
    }

    private int getMaxTaskNameLength(List<Task> taskList) {
        int maxNameLength = 0;
        for (Task task : taskList) {
            if (GanttUtil.isValidTask(task)) {
                maxNameLength = Math.max(maxNameLength, task.getName().length());
            }
        }
        return maxNameLength;
    }

    protected GanttContext initializeInstances() throws Exception {
        GanttContext gc = new GanttContext();
        gc.allUsers    = userApi.getAll();
        gc.allProducts = productApi.getAll();
        gc.allVersions = versionApi.getAll();
        gc.allFeatures = featureApi.getAll();
        gc.allSprints  = sprintApi.getAll();
        gc.allTasks    = taskApi.getAll();
        gc.allWorklogs = worklogApi.getAll();
        gc.initialize();

        return gc;
    }

    public static boolean isValidTask(net.sf.mpxj.Task task) {
        //ignore task with ID 0
        //ignore tasks that have no name
        //ignore tasks that do not have a start date or finish date
        return task.getID() != 0 && task.getUniqueID() != null && task.getName() != null && task.getStart() != null && task.getFinish() != null && (task.getID() != 1);
    }

    protected void levelResources(TestInfo testInfo, Sprint sprint, ProjectFile projectFile) throws Exception {
        initializeInstances();
        GanttUtil         ganttUtil = new GanttUtil(context);
        GanttErrorHandler eh        = new GanttErrorHandler();
        ganttUtil.levelResources(eh, sprint, "", ParameterOptions.getLocalNow());

        //save back to the database
        try (Profiler pc = new Profiler(SampleType.JPA)) {
            sprint.getTasks().forEach(task -> {
                taskApi.update(task);
            });
            sprintApi.update(sprint);
//        printTables();
//            initializeInstances();
        }
        if (projectFile == null) {
            try (Profiler pc = new Profiler(SampleType.FILE)) {
                storeExpectedResult(testInfo, sprint);
                storeResult(testInfo, sprint);
            }
        }
    }

    private void logProjectTasks(String fileName, Sprint sprint, String referenceFileName, Sprint referenceSprint) {
        logger.trace("----------------------------------------------------------------------");
        logger.trace("Reference File Name=" + referenceFileName);
        logTasks(referenceSprint.getTasks());
        logger.trace("----------------------------------------------------------------------");
        logger.trace("File Name=" + fileName);
        logTasks(sprint, referenceSprint);
        logger.trace("----------------------------------------------------------------------");
    }

    private void logTask(Task task, Task referenceTask, int maxNameLength) {
        String   buffer         = "";
        String   criticalString = task.getCritical() ? "Y" : "N";
        String   startString    = DateUtil.createDateString(task.getStart(), dtfymdhmss);
        String   finishString   = DateUtil.createDateString(task.getFinish(), dtfymdhmss);
        String   durationString = null;
        Duration duration       = task.getDuration();
        if (duration != null) {
            //            int minutes = (int) ((duration.getDuration() * 7.5 * 60 * 60) / 60);
            //            double seconds = (duration.getDuration() * 7.5 * 60 * 60 - minutes * 60);
            durationString = DateUtil.createDurationString(duration, true, true, true);
        }
        Duration referenceDuration = null;
        if (referenceTask != null) {
            referenceDuration = referenceTask.getDuration();
            //            int minutes = (int) ((duration.getDuration() * 7.5 * 60 * 60) / 60);
            //            double seconds = (duration.getDuration() * 7.5 * 60 * 60 - minutes * 60);
//            String referenceDurationString = DateUtil.createDurationString(referenceDuration, true, true, true);
        }
        String          criticalFlag = ANSI_GREEN;
        String          startFlag    = ANSI_GREEN;
        String          finishFlag   = ANSI_GREEN;
        String          durationFlag = ANSI_GREEN;
        ProjectCalendar calendar     = GanttUtil.getCalendar(task);
        if (referenceTask != null) {
            if (task.getChildTasks().isEmpty() && task.getCritical() != referenceTask.getCritical()) {
                criticalFlag = ANSI_RED;
            }
            if (task.getStart() == null) {
                startFlag = ANSI_RED;
            } else if (!GanttUtil.equals(calendar, task.getStart(), referenceTask.getStart())) {
                startFlag = ANSI_RED;
            } else if (!task.getStart().equals(referenceTask.getStart())) {
                startFlag = ANSI_YELLOW;
            }
            if (task.getFinish() == null) {
                finishFlag = ANSI_RED;
            } else if (!GanttUtil.equals(calendar, task.getFinish(), referenceTask.getFinish())) {
                finishFlag = ANSI_RED;

            } else if (!task.getFinish().equals(referenceTask.getFinish())) {
                finishFlag = ANSI_YELLOW;
            }
            if (task.getDuration() == null) {
                durationFlag = ANSI_RED;
            } else if (!GanttUtil.equals(task.getDuration(), referenceTask.getDuration())) {
                durationFlag = ANSI_RED;
            }

        }
        buffer += String.format("[%2d] N='%-" + maxNameLength + "s' C=%s%s%s S='%s%20s%s' D='%s%-19s%s' F='%s%20s%s'", task.getId(),//
                task.getName(),//
                criticalFlag, criticalString, ANSI_RESET,//
                startFlag, startString, ANSI_RESET,//
                durationFlag, durationString, ANSI_RESET,//
                finishFlag, finishString, ANSI_RESET);
        logger.trace(buffer);
    }

    protected void logTasks(List<Task> taskList) {
        int maxNameLength = getMaxTaskNameLength(taskList);
        for (Task task : taskList) {
            if (GanttUtil.isValidTask(task)) {
                logTask(task, null, maxNameLength);
            }
        }
    }

    protected void logTasks(Sprint sprint, Sprint referenceSprint) {
        int maxNameLength = getMaxTaskNameLength(sprint.getTasks());
        for (Task task : sprint.getTasks()) {
            if (GanttUtil.isValidTask(task)) {
                logTask(task, referenceSprint.getTaskById(task.getId()), maxNameLength);
            }
        }
    }

    @PostConstruct
    protected void postConstruct() {
        super.postConstruct();
//        new File(testResultFolder).mkdirs();
//        new File(testReferenceResultFolder).mkdirs();
    }

    private List<String> readStoredData(String directory, String sprintName) throws IOException {
        Path filePath = Paths.get(directory, sprintName + ".json");
        if (Files.exists(filePath)) {
            return Files.readAllLines(filePath, StandardCharsets.UTF_8);
        }
        return new ArrayList<>();
    }

    protected void setTestCaseName(String testClassName, String testMethodName) {
        testResultFolder = testResultFolder + "/" + testClassName;
        new File(testResultFolder).mkdirs();
        testReferenceResultFolder = testReferenceResultFolder + "/" + testClassName;
        new File(testReferenceResultFolder).mkdirs();
    }

    private void store(String directory, TestInfo testInfo, Sprint sprint, boolean overwrite) throws IOException {
        Path filePath = Paths.get(directory, TestInfoUtil.getTestMethodName(testInfo) + ".json");
        if (overwrite || !Files.exists(filePath)) {
            Map<String, Object> container = new LinkedHashMap<>();
            container.put("users", sprint.getUserMap());
            container.put("sprint", sprint);
            container.put("tasks", sprint.getTasks());

            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(container);
            Files.writeString(filePath, json, StandardCharsets.UTF_8);
        }
    }

    private void storeExpectedResult(TestInfo testCaseInfo, Sprint sprint) throws IOException {
        store(testResultFolder, testCaseInfo, sprint, true);
    }

    private void storeResult(TestInfo testCaseInfo, Sprint sprint) throws IOException {
        store(testReferenceResultFolder, testCaseInfo, sprint, false);
    }

    @Override
    protected void testAllAndPrintTables() {
        //we do not want to test gantt charts same way as the other tests
    }

}
