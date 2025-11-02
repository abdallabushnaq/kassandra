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

package de.bushnaq.abdalla.kassandra;

import de.bushnaq.abdalla.util.Debug;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class Context {
    //    public Requests active = new Requests();
//    public List<AbstractDevelopmentRequest> activeList = null;
//    public Authentication authentication = new Authentication();
//    public Map<LocalDate, String> bankHolidays;
//    public List<AbstractDevelopmentRequest> closedList = null;
//    public List<AbstractDevelopmentRequest> closingList = null;
    public Debug            debug      = new Debug();
    //    public GanttInformationList ganttInformationList = new GanttInformationList();
//    public Index index = new Index();
//    private JiraClientFactory jiraClientFactory;
//    public Logs logs = new Logs();
//    public ArrayList<JiraSprint> orphanSprintList = new ArrayList<>();
    public ParameterOptions parameters = new ProjectHubParameterOptions();

    //    public ResourceUtilization resourceUtilization = new ResourceUtilization();
//    public List<SfpsTicket> spfsList;
//    private Map<Integer, Integer> taskIdToRowIndexMap = new HashMap<>();
//    @Autowired
//    public TimeTracker timeTracker;
//
//    public void addTaskIdToRowIndexRelation(Task task, Integer rowId) {
//        taskIdToRowIndexMap.put(task.getID(), rowId);
//    }
//
//    public JiraClientFactory getJiraClientFactory() {
//        return jiraClientFactory;
//    }
//
//    public Integer getRowIndexByTaskId(Task task) {
//        Integer rowIndex = taskIdToRowIndexMap.get(task.getID());
//        if (rowIndex != null) {
//            return ExcelUtil.rowIndextoExcelRowName(rowIndex);
//        } else {
//            return task.getID();
//        }
//    }
//
//    public Integer getRowIndexByTaskIdIfExists(Task task) {
//        Integer rowIndex = taskIdToRowIndexMap.get(task.getID());
//        return rowIndex;
//    }
//
//    public void init(ParameterOptions parameters) {
//        this.parameters = parameters;
//    }
//
//    public void setJiraClientFactory(JiraClientFactory jiraClientFactory) {
//        this.jiraClientFactory = jiraClientFactory;
//    }
//
//    public void start() {
//        jiraClientFactory = new JiraClientProductionFactory(parameters.reportFolder);
//    }
//
//    public void start(String[] args) throws Exception {
//        parameters.start(args);
//        jiraClientFactory = new JiraClientProductionFactory(parameters.reportFolder);
//    }
}
