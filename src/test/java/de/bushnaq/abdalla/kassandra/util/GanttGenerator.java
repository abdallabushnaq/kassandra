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

import de.bushnaq.abdalla.kassandra.Context;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttChart;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttUtil;
import de.bushnaq.abdalla.util.GanttErrorHandler;
import de.bushnaq.abdalla.util.Util;
import net.sf.mpxj.ProjectFile;
import org.junit.jupiter.api.TestInfo;

public class GanttGenerator extends MPXJGenerator {

    public void generateGanttChart(TestInfo testInfo, long sprintId, String testFolder) throws Exception {
        Sprint sprint = sprints.stream().filter(s -> s.getId() == sprintId).findFirst().orElseThrow(() -> new IllegalArgumentException("Sprint with id " + sprintId + " not found"));
        sprint.initialize();
        sprint.initUserMap(users);
        sprint.initTaskMap(tasks, worklogs);
        sprint.recalculate(ParameterOptions.getLocalNow());
        Context context = new Context();
        String  sprintName;
        if (testInfo.getDisplayName().indexOf('=') != -1) {
            sprintName = testInfo.getDisplayName().substring(testInfo.getDisplayName().indexOf('"') + 1, testInfo.getDisplayName().lastIndexOf('"')) + "-gant-chart";
        } else {
            sprintName = testInfo.getDisplayName() + "-gant-chart";
        }
        GanttChart chart = new GanttChart(context, "", "/", "Gantt Chart", sprintName, exceptions, ParameterOptions.getLocalNow(), false, sprint/*, 1887, 1000*/, "scheduleWithMargin", context.parameters.graphicsTheme);
//        String     description = testCaseInfo.getDisplayName().replace("_", "-");
        String description = TestInfoUtil.getTestMethodName(testInfo);
        chart.render(Util.generateCopyrightString(ParameterOptions.getLocalNow()), description, testFolder);
    }

    public void initializeSprint(Sprint sprint) {
        sprint.initialize();
        sprint.initUserMap(users);
        sprint.initTaskMap(tasks, worklogs);
    }

    public void levelResources(TestInfo testInfo, Sprint sprint, ProjectFile projectFile) throws Exception {
//        initializeInstances();
        GanttUtil         ganttUtil = new GanttUtil();
        GanttErrorHandler eh        = new GanttErrorHandler();
        ganttUtil.levelResources(eh, sprint, "", ParameterOptions.getLocalNow());

//        if (projectFile == null) {
//            try (Profiler pc = new Profiler(SampleType.FILE)) {
//                storeExpectedResult(testInfo, sprint);
//                storeResult(testInfo, sprint);
//            }
//        }
    }


//    protected GanttContext initializeInstances() throws Exception {
//        GanttContext gc = new GanttContext();
//        gc.allUsers    = new ArrayList<>(users);
//        gc.allProducts = new ArrayList<>(products);
//        gc.allVersions = new ArrayList<>(versions);
//        gc.allFeatures = new ArrayList<>(features);
//        gc.allSprints  = new ArrayList<>(sprints);
//        gc.allTasks    = new ArrayList<>(tasks);
//        gc.allWorklogs = new ArrayList<>(worklogs);
//        gc.initialize();
//
//        return gc;
//    }

}
