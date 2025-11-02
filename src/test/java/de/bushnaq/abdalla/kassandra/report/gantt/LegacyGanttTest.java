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

package de.bushnaq.abdalla.kassandra.report.gantt;

import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.util.AbstractLegacyGanttTestUtil;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import de.bushnaq.abdalla.util.MpxjUtil;
import de.bushnaq.abdalla.util.date.DateUtil;
import net.sf.mpxj.*;
import net.sf.mpxj.reader.UniversalProjectReader;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.bushnaq.abdalla.kassandra.util.NameGenerator.PROJECT_HUB_ORG;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@TestMethodOrder(MethodOrderer.MethodName.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class LegacyGanttTest extends AbstractLegacyGanttTestUtil {
    public static final String ANONYMOUS     = "anonymous";
    private             int    testCaseIndex = 1;

    /**
     * Test method to read MPP files, clone all tasks, users and relations and then compare both after leveling the resources.
     * ensures that our leveling aligns with mpp.
     *
     * @param mppFileName
     * @param testInfo
     * @throws Exception
     */
    @MethodSource("listFilesByExtension")
    @ParameterizedTest
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void legacyTest(Path mppFileName, TestInfo testInfo) throws Exception {
        TestInfoUtil.setTestCaseIndex(testInfo, testCaseIndex);
        TestInfoUtil.setTestMethod(testInfo, mppFileName.getFileName().toString());
//        TestInfoUtil.setTestMethod(testInfo, generateTestCaseName(testInfo));
        TestInfoUtil.setDaysAfterStart(testInfo, random.nextInt(20) + 2);
        setTestCaseName(this.getClass().getName(), mppFileName.getFileName().toString());
        generateOneProduct(testInfo);
        Sprint sprint = expectedSprints.getFirst();
        testCaseIndex++;

        File file = new File(String.valueOf(mppFileName.toAbsolutePath()));
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
            UniversalProjectReader reader      = new UniversalProjectReader();
            ProjectFile            projectFile = reader.read(inputStream);
            LocalDateTime          date        = projectFile.getProjectProperties().getStartDate();//ensure start date matches
            ParameterOptions.setNow(DateUtil.localDateTimeToOffsetDateTime(date));
            //populate mpxjTaskMap and resourceMap
            for (net.sf.mpxj.Task mpxjTask : projectFile.getTasks()) {
                if (isValidTask(mpxjTask)) {
                    System.out.printf("Task ID: %s, Task Name: %s%n", mpxjTask.getID(), mpxjTask.getName());
                    mpxjTaskMap.put(mpxjTask.getName(), mpxjTask);//store tasks
                    if (!mpxjTask.getResourceAssignments().isEmpty()) {
                        ResourceAssignment resourceAssignment = mpxjTask.getResourceAssignments().get(0);
                        Resource           resource           = resourceAssignment.getResource();
                        if (resource != null) {
                            String resourceName = resource.getName();
                            if (resourceMap.get(resourceName) != null) {
                                resourceMap.put(resourceName, resource);//store resources
                            }
                            if (userMap.get(resourceName) == null) {
                                Number   units = resourceAssignment.getUnits();
                                Duration work  = resourceAssignment.getWork();
//                                TimeUnit units1       = work.getUnits();
//                                double   duration1    = work.getDuration();
                                double availability = units.doubleValue() / 100;
                                String emailAddress = resource.getEmailAddress();
                                if (emailAddress == null) {
                                    emailAddress = resourceName.replaceAll(" ", "_") + "@example.com";
                                }
                                User user = addUser(resourceName, emailAddress, "DE", "nw", date.toLocalDate(), generateUserColor(userIndex), (float) availability);

                                userMap.put(resourceName, user);//store users
                            }
                        }
                    }
                }
            }
            //populate taskMap
            for (net.sf.mpxj.Task mpxjTask : projectFile.getTasks()) {
                if (isValidTask(mpxjTask)) {
                    String                                    name     = mpxjTask.getName();
                    LocalDateTime                             start    = null;
                    de.bushnaq.abdalla.kassandra.dto.TaskMode taskMode = de.bushnaq.abdalla.kassandra.dto.TaskMode.AUTO_SCHEDULED;
                    if (mpxjTask.getTaskMode().equals(TaskMode.MANUALLY_SCHEDULED)) {
                        start    = mpxjTask.getStart();
                        taskMode = de.bushnaq.abdalla.kassandra.dto.TaskMode.MANUALLY_SCHEDULED;
                    }
                    if (!mpxjTask.getResourceAssignments().isEmpty() && mpxjTask.getResourceAssignments().get(0).getResource() != null) {
                        //user assigned to this task
                        ResourceAssignment resourceAssignment = mpxjTask.getResourceAssignments().get(0);
                        Resource           resource           = resourceAssignment.getResource();
                        String             resourceName       = resource.getName();
                        Duration           work               = resourceAssignment.getWork();
                        net.sf.mpxj.Task   parent             = mpxjTaskMap.get(mpxjTask.getParentTask().getName());
                        User               user               = userMap.get(resourceName);
                        Task               task               = addTask(sprint, null, mpxjTask.getName(), start, MpxjUtil.toJavaDuration(work), null, user, null, taskMode, mpxjTask.getMilestone());
                        taskMap.put(task.getName(), task);
                    } else if (!mpxjTask.hasChildTasks()) {
                        //no user assigned to this task
                        Duration work         = mpxjTask.getDuration();
                        String   resourceName = ANONYMOUS;
                        String   emailAddress = ANONYMOUS + PROJECT_HUB_ORG;
                        User     user         = userMap.get(resourceName);
                        if (user == null) {
                            user = addUser(resourceName, emailAddress, "de", "nw", date.toLocalDate(), generateUserColor(userIndex), (float) 1);
                        }
                        Task task = addTask(sprint, null, mpxjTask.getName(), start, MpxjUtil.toJavaDuration(work), null, user, null, taskMode, mpxjTask.getMilestone());//parent task
                        taskMap.put(task.getName(), task);
                    } else {
                        //story
                        String resourceName = ANONYMOUS;
                        String emailAddress = ANONYMOUS + PROJECT_HUB_ORG;
                        User   user         = userMap.get(resourceName);
                        if (user == null) {
                            user = addUser(resourceName, emailAddress, "de", "nw", date.toLocalDate(), generateUserColor(userIndex), (float) 1);
                        }
                        Task task = addTask(sprint, null, mpxjTask.getName(), start, null, null, user, null, taskMode, mpxjTask.getMilestone());//parent task
                        taskMap.put(task.getName(), task);
                    }
                }
            }
            //add parents and relations
            for (net.sf.mpxj.Task mpxjTask : projectFile.getTasks()) {
                if (isValidTask(mpxjTask)) {
                    String name = mpxjTask.getName();
                    Task   task = taskMap.get(name);
                    //set parent
                    if (mpxjTask.getParentTask() != null) {
                        String           parentTaskName = mpxjTask.getParentTask().getName();
                        net.sf.mpxj.Task mpxjParent     = mpxjTaskMap.get(mpxjTask.getParentTask().getName());
                        if (mpxjParent != null) {
                            //probably not valid task
                            Task parent = taskMap.get(mpxjParent.getName());
                            task.setParentTaskId(parent.getId());
                            parent.addChildTask(task);
                        }
                    }
                    //set relations
                    for (Relation relation : mpxjTask.getPredecessors()) {
                        if (!relation.getLag().equals(Duration.getInstance(0, TimeUnit.MINUTES))) {
                            net.sf.mpxj.Task mpxjPredecessor = relation.getPredecessorTask();
                            Task             predecessor     = taskMap.get(mpxjPredecessor.getName());
                            task.addPredecessor(predecessor, true);
                        }
                    }
                }
            }
            for (Task value : taskMap.values()) {
                taskApi.persist(value);
            }

//            sprint.setUserId(userMap.values().stream().findFirst().get().getId());
//            Sprint savedSprint = sprintApi.persist(sprint);
            Sprint savedSprint = sprintApi.getById(expectedSprints.getFirst().getId());
            {
                Sprint readSprint = sprintApi.getById(savedSprint.getId());
                readSprint.initialize();
                readSprint.initUserMap(userApi.getAll(readSprint.getId()));
                readSprint.initTaskMap(taskApi.getAll(readSprint.getId()), worklogApi.getAll(readSprint.getId()));
                levelResources(testInfo, readSprint, projectFile);
                ParameterOptions.setNow(ParameterOptions.getNow().plusDays(TestInfoUtil.getDaysAfterStart(testInfo)));
                generateWorklogs(readSprint, ParameterOptions.getLocalNow());
                initializeInstances();//reload from db for our test comparison
                generateGanttChart(testInfo, readSprint.getId(), projectFile);
                generateBurndownChart(testInfo, readSprint.getId());
            }
        }
    }

    private static List<Path> listFilesByExtension() throws IOException {
        return listFilesByExtension("references", ".mpp");
    }

    /**
     * Lists files in the specified directory with an extension filter
     *
     * @param directoryPath The path to the directory
     * @param extension     The file extension to filter by (e.g., ".java")
     * @return List of matching file paths
     * @throws IOException If an I/O error occurs
     */
    private static List<Path> listFilesByExtension(String directoryPath, String extension) throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get(directoryPath))) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(extension))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }
}
