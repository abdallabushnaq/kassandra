package de.bushnaq.abdalla.util;


import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class Debug {
    public static boolean exportSql = false;

    protected     boolean filterGantt     = false;
    private final String  filterGanttName = "SLNX 2.7.3 ADM.mpp";
    //    public        boolean filterRacProject     = false;
//    public        String  filterRacProjectName = "";
//    public        boolean filterRequest        = false;
//    public        String  filterRequestKey;
    protected     boolean filterResource  = false;
    private       String  filterResourceName;
    //    public        boolean filterSfps           = false;
//    public        String  filterSfpsKey;
    public        boolean filterSprint    = false;
    public        long    filterSprintId;
    public        boolean filterTask      = false;
    private       long    filterTaskId;
    //    public        boolean filterSubtask        = false;
//    private       long    filterSubtaskId;
    public        boolean hidden          = true;
    public        boolean print           = false;

    public static void export(String folder, String file, String command) throws IOException, InterruptedException, ErrorException {
        if (exportSql) {
            File   dir          = new File(folder);
            String absolutePath = dir.getAbsolutePath();
            DirectoryUtil.createDirectory(absolutePath);
            PrintWriter writer = new PrintWriter(folder + "/" + file, StandardCharsets.UTF_8);
            writer.print(command);
            writer.close();
        }
    }

//    public boolean filter(TimeTrackerProject project) {
//        if (project != null) {
//            return project.name.equals(filterRacProjectName) || !filterRacProject;
//        }
//        return false;
//    }

//    public boolean filterGantt(GanttInformation ganttInformation) {
//        if (ganttInformation != null) {
//            return ganttInformation.projectFileName.equals(filterGanttName) || !filterGantt;
//        }
//        return false;
//    }

//    public boolean filterRequest(AbstractDevelopmentRequest issue) {
//        if (issue != null) {
//            return issue.getKey().equalsIgnoreCase(filterRequestKey) || !filterRequest;
//        }
//        return false;
//    }

    public boolean filterResource(User resource) {
        if (resource == null) {
            return true;
        }
        if (resource.getName() != null) {
            return resource.getName().equals(filterResourceName) || !filterResource;
        }
        return false;
    }

//    public boolean filterSfps(SfpsTicket issue) {
//        if (issue != null) {
//            return issue.getKey().equalsIgnoreCase(filterSfpsKey) || !filterSfps;
//        }
//        return false;
//    }

    public boolean filterSprint(Sprint sprint) {
        if (sprint.getId() != null) {
            return sprint.getId() == filterSprintId || !filterSprint;
        }
        return false;
    }

    public boolean filterSprint(Long sprintId) {
        if (sprintId != null) {
            return sprintId == filterSprintId || !filterSprint;
        }
        return false;
    }

    public boolean filterTask(Task issue) {
        if (issue != null) {
            return issue.getId() == filterTaskId || !filterTask;
        }
        return false;
    }

//    public boolean filterSubtask(JiraSubtask issue) {
//        if (issue != null) {
//            return issue.getId() == filterSubtaskId || !filterSubtask;
//        }
//        return false;
//    }

}
