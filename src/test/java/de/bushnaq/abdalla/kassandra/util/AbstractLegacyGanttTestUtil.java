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

import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttContext;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttUtil;
import de.bushnaq.abdalla.util.MpxjUtil;
import de.bushnaq.abdalla.util.date.DateUtil;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectFile;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@TestMethodOrder(MethodOrderer.MethodName.class)
public class AbstractLegacyGanttTestUtil extends AbstractGanttTestUtil {
    protected Map<String, net.sf.mpxj.Task>     mpxjTaskMap = new HashMap<>();
    protected Map<String, net.sf.mpxj.Resource> resourceMap = new HashMap<>();
    protected Map<String, Task>                 taskMap     = new HashMap<>();
    protected Map<String, User>                 userMap     = new HashMap<>();

    @Override
    protected void compareResults(ProjectFile projectFile, TestInfo testInfo) throws IOException {
        GanttContext gc              = new GanttContext();
        Sprint       referenceSprint = new Sprint();//fake sprint
        referenceSprint.setId(1L);//fake sprint id
        gc.allSprints.add(referenceSprint);
        long taskIdIndex = 1;//fake task id
        for (net.sf.mpxj.Task mpxjTask : projectFile.getTasks()) {
            if (isValidTask(mpxjTask)) {
                Task referenceTask = new Task();//fake task
                referenceTask.setId(taskIdIndex++);
                referenceTask.setName(mpxjTask.getName());
                referenceTask.setStart(mpxjTask.getStart());
                referenceTask.setFinish(mpxjTask.getFinish());
                referenceTask.setDuration(MpxjUtil.toJavaDuration(mpxjTask.getDuration()));
                referenceTask.setCritical(mpxjTask.getCritical());
                referenceTask.setSprintId(gc.allSprints.getFirst().getId());
                gc.allTasks.add(referenceTask);
            }
        }
        gc.initialize();
        Sprint readSprint = sprintApi.getById(expectedSprints.getFirst().getId());
        readSprint.initialize();
        readSprint.initUserMap(userApi.getAll(readSprint.getId()));
        readSprint.initTaskMap(taskApi.getAll(readSprint.getId()), worklogApi.getAll(readSprint.getId()));

//        Sprint sprint = expectedSprints.getFirst();
        logTasks(readSprint, gc.allSprints.getFirst());
        DateUtil dateUtil = new DateUtil();
        for (net.sf.mpxj.Task mpxjTask : projectFile.getTasks()) {
            if (isValidTask(mpxjTask)) {
                Task            task     = taskMap.get(mpxjTask.getName());
                ProjectCalendar calendar = task.getEffectiveCalendar();
                assertTrue(GanttUtil.equals(calendar, task.getStart(), mpxjTask.getStart()), String.format("unexpected task: %s start expected %s actual %s", task.getName(),
                        DateUtil.createDateString(mpxjTask.getStart(), dateUtil.dtfymd),
                        DateUtil.createDateString(task.getStart(), dateUtil.dtfymd)));
                assertTrue(GanttUtil.equals(calendar, task.getFinish(), mpxjTask.getFinish()), String.format("unexpected task: %s finish", task.getName()));
                assertEquals(MpxjUtil.toJavaDuration(mpxjTask.getDuration()), task.getDuration(), String.format("unexpected task: %s duration", task.getName()));
                boolean c = mpxjTask.getCritical();
                if (mpxjTask.getChildTasks().isEmpty()) {
                    //TODO fix discrepancy between mpxj and java
                    assertEquals(mpxjTask.getCritical(), task.getCritical(), String.format("unexpected task: %s critical", task.getName()));
                }
            }
        }
    }

    @Override
    protected GanttContext initializeInstances() throws Exception {
        GanttContext gc = super.initializeInstances();

        taskMap.clear();
        for (Task allTask : gc.allTasks) {
            taskMap.put(allTask.getName(), allTask);
        }
        userMap.clear();
        for (User allUser : gc.allUsers) {
            userMap.put(allUser.getName(), allUser);
        }
//        sprint = gc.allSprints.getFirst();
        return gc;
    }

}
