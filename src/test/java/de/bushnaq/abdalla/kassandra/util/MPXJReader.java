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

package de.bushnaq.abdalla.kassandra.util;

import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.util.MpxjUtil;
import de.bushnaq.abdalla.util.date.DateUtil;
import net.sf.mpxj.*;
import net.sf.mpxj.reader.UniversalProjectReader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static de.bushnaq.abdalla.kassandra.util.NameGenerator.PROJECT_HUB_ORG;

public class MPXJReader extends GanttGenerator {
    public static final String                                             ANONYMOUS   = "anonymous";
    private final       boolean                                            includeLevelingInfo;//leveling info like task start/finish and duration time
    protected           Map<String, Task>                                  mpxjTaskMap = new HashMap<>();
    protected           Map<String, net.sf.mpxj.Resource>                  resourceMap = new HashMap<>();
    protected           Map<String, de.bushnaq.abdalla.kassandra.dto.Task> taskMap     = new HashMap<>();
    protected           String                                             testFolder;
    protected           Map<String, User>                                  userMap     = new HashMap<>();

    public MPXJReader(String testFolder, boolean includeLevelingInfo) {
        super();
        this.testFolder          = testFolder;
        this.includeLevelingInfo = includeLevelingInfo;
    }


    public static boolean isValidTask(net.sf.mpxj.Task task) {
        //ignore task with ID 0
        //ignore tasks that have no name
        //ignore tasks that do not have a start date or finish date
        return task.getID() != 0 && task.getUniqueID() != null && task.getName() != null && task.getStart() != null && task.getFinish() != null && (task.getID() != 1);
    }

    public Sprint load(Path mppFileName) throws Exception {
        Sprint sprint = addSprint();
        File   file   = new File(String.valueOf(mppFileName.toAbsolutePath()));
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
            UniversalProjectReader reader      = new UniversalProjectReader();
            ProjectFile            projectFile = reader.read(inputStream);
            LocalDateTime          date        = projectFile.getProjectProperties().getStartDate();//ensure start date matches
            ParameterOptions.setNow(DateUtil.localDateTimeToOffsetDateTime(date));
            //populate mpxjTaskMap and resourceMap
            for (net.sf.mpxj.Task mpxjTask : projectFile.getTasks()) {
                if (isValidTask(mpxjTask)) {
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
                                User user = addUser(resourceName, (float) availability);
                                System.out.printf("Resource ID: %s, resource Name: %s%n", user.getId(), user.getName());

                                userMap.put(resourceName, user);//store users
                            }
                        }
                    }
                }
            }
            //populate taskMap
            int anonymousUserIndex = 1;
            for (net.sf.mpxj.Task mpxjTask : projectFile.getTasks()) {
                if (isValidTask(mpxjTask)) {
                    String             name     = mpxjTask.getName();
                    LocalDateTime      start    = null;
                    LocalDateTime      finish   = null;
                    java.time.Duration duration = null;
                    boolean            critical = false;
                    if (includeLevelingInfo) {
                        start    = mpxjTask.getStart();
                        finish   = mpxjTask.getFinish();
                        duration = MpxjUtil.toJavaDuration(mpxjTask.getDuration());
                        critical = mpxjTask.getCritical();
                    }
                    de.bushnaq.abdalla.kassandra.dto.TaskMode taskMode = de.bushnaq.abdalla.kassandra.dto.TaskMode.AUTO_SCHEDULED;
                    if (mpxjTask.getTaskMode().equals(TaskMode.MANUALLY_SCHEDULED)) {
                        taskMode = de.bushnaq.abdalla.kassandra.dto.TaskMode.MANUALLY_SCHEDULED;
                        start    = mpxjTask.getStart();
                    }
                    if (!mpxjTask.getResourceAssignments().isEmpty() && mpxjTask.getResourceAssignments().get(0).getResource() != null) {
                        //user assigned to this task
                        ResourceAssignment resourceAssignment = mpxjTask.getResourceAssignments().get(0);
                        Resource           resource           = resourceAssignment.getResource();
                        String             resourceName       = resource.getName();
                        Duration           work               = resourceAssignment.getWork();
                        if (work.getDuration() == 0) {
                            java.time.Duration d = java.time.Duration.between(resourceAssignment.getStart(), resourceAssignment.getFinish());
                            java.time.Duration w = java.time.Duration.ofSeconds((long) (d.getSeconds() * resourceAssignment.getResource().getCurrentAvailabilityTableEntry().getUnits().doubleValue() / 100));
                            work = MpxjUtil.toMpjxDuration(w);
                        }

                        net.sf.mpxj.Task                      parent = mpxjTaskMap.get(mpxjTask.getParentTask().getName());
                        User                                  user   = userMap.get(resourceName);
                        de.bushnaq.abdalla.kassandra.dto.Task task   = addTask(sprint, null, mpxjTask.getName(), start, MpxjUtil.toJavaDuration(work), null, user, null, taskMode, mpxjTask.getMilestone());
                        task.setFinish(finish);
                        task.setDuration(duration);
                        task.setCritical(critical);
                        taskMap.put(task.getName(), task);
                    } else if (!mpxjTask.hasChildTasks()) {
                        //no user assigned to this task
                        Duration work         = mpxjTask.getDuration();
                        String   resourceName = ANONYMOUS + "-" + anonymousUserIndex;
                        String   emailAddress = ANONYMOUS + "-" + anonymousUserIndex + PROJECT_HUB_ORG;
                        anonymousUserIndex++;
                        User user = userMap.get(resourceName);
                        if (user == null) {
                            user = addUser(resourceName, (float) 1);
                            userMap.put(resourceName, user);//store anonymous user for reuse
                        }
                        de.bushnaq.abdalla.kassandra.dto.Task task = addTask(sprint, null, mpxjTask.getName(), start, MpxjUtil.toJavaDuration(work), null, user, null, taskMode, mpxjTask.getMilestone());//parent task
                        task.setFinish(finish);
                        task.setDuration(duration);
                        task.setCritical(critical);
                        taskMap.put(task.getName(), task);
                    } else {
                        //story
                        String resourceName = ANONYMOUS + "-" + anonymousUserIndex;
                        String emailAddress = ANONYMOUS + "-" + anonymousUserIndex + PROJECT_HUB_ORG;
                        anonymousUserIndex++;
                        User user = userMap.get(resourceName);
                        if (user == null) {
                            user = addUser(resourceName, (float) 1);
                            userMap.put(resourceName, user);//store anonymous user for reuse
                        }
                        de.bushnaq.abdalla.kassandra.dto.Task task = addTask(sprint, null, mpxjTask.getName(), start, null, null, user, null, taskMode, mpxjTask.getMilestone());//parent task
                        task.setFinish(finish);
                        task.setDuration(duration);
                        task.setCritical(critical);
                        taskMap.put(task.getName(), task);
                    }
                }
            }
            //add parents and relations
            for (net.sf.mpxj.Task mpxjTask : projectFile.getTasks()) {
                if (isValidTask(mpxjTask)) {
                    String                                name = mpxjTask.getName();
                    de.bushnaq.abdalla.kassandra.dto.Task task = taskMap.get(name);
                    //set parent
                    if (mpxjTask.getParentTask() != null) {
                        String           parentTaskName = mpxjTask.getParentTask().getName();
                        net.sf.mpxj.Task mpxjParent     = mpxjTaskMap.get(mpxjTask.getParentTask().getName());
                        if (mpxjParent != null) {
                            //probably not valid task
                            de.bushnaq.abdalla.kassandra.dto.Task parent = taskMap.get(mpxjParent.getName());
                            task.setParentTaskId(parent.getId());
                            parent.addChildTask(task);
                        }
                    }
                    //set relations
                    for (Relation relation : mpxjTask.getPredecessors()) {
                        if (!relation.getLag().equals(Duration.getInstance(0, TimeUnit.MINUTES))) {
                            net.sf.mpxj.Task                      mpxjPredecessor = relation.getPredecessorTask();
                            de.bushnaq.abdalla.kassandra.dto.Task predecessor     = taskMap.get(mpxjPredecessor.getName());
                            task.addPredecessor(predecessor, true);
                        }
                    }
                }
            }
        }
        sprint.initialize();
        sprint.initUserMap(users);
        sprint.initTaskMap(tasks, worklogs);
        if (includeLevelingInfo) {
            sprint.setEnd(sprint.getLatestFinishDate());
        }
        return sprint;
    }


//    private void store(String directory, TestInfo testInfo, Sprint sprint, boolean overwrite) throws IOException {
//        Path filePath = Paths.get(directory, TestInfoUtil.getTestMethodName(testInfo) + ".json");
//        if (overwrite || !Files.exists(filePath)) {
//            Map<String, Object> container = new LinkedHashMap<>();
//            container.put("users", sprint.getUserMap());
//            container.put("sprint", sprint);
//            container.put("tasks", sprint.getTasks());
//
//            String json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(container);
//            Files.writeString(filePath, json, StandardCharsets.UTF_8);
//        }
//    }
//
//    private void storeExpectedResult(TestInfo testCaseInfo, Sprint sprint) throws IOException {
//        store(testFolder, testCaseInfo, sprint, true);
//    }
//
//    private void storeResult(TestInfo testCaseInfo, Sprint sprint) throws IOException {
//        store(testFolder, testCaseInfo, sprint, false);
//    }

}
