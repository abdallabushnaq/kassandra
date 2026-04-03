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

package de.bushnaq.abdalla.kassandra.report.gantt;

import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.report.dao.ETheme;
import de.bushnaq.abdalla.kassandra.util.DTOAsserts;
import de.bushnaq.abdalla.kassandra.util.MPXJReader;
import de.bushnaq.abdalla.util.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.reader.UniversalProjectReader;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.TestInfo;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static de.bushnaq.abdalla.util.AnsiColorConstants.*;

@Slf4j
public class AbstractGanttTester extends DTOAsserts {
    private final DateTimeFormatter dtfymdhmss = DateTimeFormatter.ofPattern("yyyy.MMM.dd HH:mm:ss.SSS");
    /**
     * Theme used when generating Gantt charts in {@link #evaluate}.
     * Defaults to {@link ETheme#dark} to preserve pre-existing test reference images.
     * Sub-classes (e.g. {@code CriticalTest}) may set this field before calling
     * {@link #executeTest} to switch the rendering theme.
     */
    protected     ETheme            testTheme  = ETheme.dark;

    /**
     * Compares two Lombok {@code toString()} outputs field by field and returns the actual
     * string with any field whose value differs from the reference highlighted in red.
     *
     * @param reference the expected toString output
     * @param actual    the actual toString output to annotate
     * @return the actual string with differing field values wrapped in ANSI red
     */
    static String diffTaskToString(String reference, String actual) {
        Map<String, String> refTokens    = parseToStringTokens(reference);
        Map<String, String> actualTokens = parseToStringTokens(actual);

        int           parenIdx  = actual.indexOf('(');
        String        className = parenIdx >= 0 ? actual.substring(0, parenIdx) : "";
        StringBuilder sb        = new StringBuilder().append(className).append('(');

        boolean first = true;
        for (Map.Entry<String, String> e : actualTokens.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            String key      = e.getKey();
            String value    = e.getValue();
            String refValue = refTokens.get(key);
            sb.append(key).append('=');
            if (!value.equals(refValue)) {
                sb.append(ANSI_RED).append(value).append(ANSI_RESET);
            } else {
                sb.append(value);
            }
        }
        return sb.append(')').toString();
    }

    private void evaluate(TestInfo testInfo, String testFolder, String referenceFileName, String fileName) throws Exception {

        MPXJReader referenceProject = new MPXJReader(testFolder, true);
        Sprint     referenceSprint  = referenceProject.load(Path.of(referenceFileName));

        MPXJReader project = new MPXJReader(testFolder, false);
        Sprint     sprint  = project.load(Path.of(fileName));
        project.levelResources(testInfo, sprint, null);
        project.testTheme = testTheme;

        ParameterOptions.setNow(DateUtil.localDateTimeToOffsetDateTime(DateUtil.addDay(sprint.getStart(), 10)));
        project.generateWorklogs(sprint.getId(), ParameterOptions.getLocalNow());
        project.generateGanttChart(testInfo, sprint.getId(), testFolder);
        project.generateBurndownChart(testInfo, sprint.getId(), testFolder);

        sanitizeTasks(sprint);
        for (int i = 0; i < referenceSprint.getTasks().size(); i++) {
            Task   referenceTask         = referenceSprint.getTasks().get(i);
            String referenceTaskAsString = taskToString(referenceTask);
            log.trace("ref : {}", referenceTaskAsString);
            Task   task         = sprint.getTasks().get(i);
            String taskAsString = taskToString(task);
            log.trace("task: {}", diffTaskToString(referenceTaskAsString, taskAsString));
            log.info("------");
        }


        logProjectTasks(fileName, sprint, referenceFileName, referenceSprint);
        assertSprintEquals(referenceSprint, sprint);
    }

    protected void executeTest(String name, TestInfo testInfo, String testFolder) throws Exception {
        String                 fileName             = testFolder + File.separator + name + ".mpp";
        String                 referenceFileName    = testFolder + File.separator + name + ".mpp";
        UniversalProjectReader referenceReader      = new UniversalProjectReader();
        File                   referenceFile        = new File(referenceFileName);
        InputStream            referenceInputStream = new BufferedInputStream(new FileInputStream(referenceFile));
        ProjectFile            referenceProjectFile = referenceReader.read(referenceInputStream);
        LocalDateTime          date                 = referenceProjectFile.getProjectProperties().getStartDate();//ensure start date matches
        ParameterOptions.setNow(DateUtil.localDateTimeToOffsetDateTime(date));
        evaluate(testInfo, testFolder, referenceFileName, fileName);
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

    protected static @NonNull Stream<String> getMppFilesStream(Path folder) throws IOException {
        if (!Files.exists(folder) || !Files.isDirectory(folder)) {
            throw new IllegalStateException("Test folder does not exist: " + folder.toAbsolutePath());
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files
                    .filter(p -> p.getFileName().toString().endsWith(".mpp"))
                    .map(p -> {
                        String fileName = p.getFileName().toString();
                        return fileName.substring(0, fileName.length() - 4); // remove .mpp
                    })
                    .toList()
                    .stream();
        }
    }

    void logProjectTasks(String fileName, Sprint sprint, String referenceFileName, Sprint referenceSprint) {
        log.trace("----------------------------------------------------------------------");
        log.trace("Reference File Name=" + referenceFileName);
        logTasks(referenceSprint.getTasks());
        log.trace("----------------------------------------------------------------------");
        log.trace("File Name=" + fileName);
        logTasks(sprint, referenceSprint);
        log.trace("----------------------------------------------------------------------");
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
        log.trace(buffer);
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


    /**
     * Parses a Lombok {@code toString()} string into an ordered map of field name to value.
     * Bracket depth is tracked so that commas inside nested structures
     * (e.g. {@code predecessors=[Relation(...)]}) are not treated as token separators.
     *
     * @param s the toString string to parse
     * @return ordered map of field names to their string values
     */
    private static Map<String, String> parseToStringTokens(String s) {
        int start = s.indexOf('(');
        int end   = s.lastIndexOf(')');
        if (start < 0 || end < 0) return Collections.emptyMap();

        String              content    = s.substring(start + 1, end);
        Map<String, String> result     = new LinkedHashMap<>();
        int                 depth      = 0;
        int                 tokenStart = 0;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '(' || c == '[' || c == '{') depth++;
            else if (c == ')' || c == ']' || c == '}') depth--;
            else if (c == ',' && depth == 0 && i + 1 < content.length() && content.charAt(i + 1) == ' ') {
                putToken(result, content.substring(tokenStart, i));
                tokenStart = i + 2; // skip ", "
                i++;                // skip the space
            }
        }
        if (tokenStart < content.length()) {
            putToken(result, content.substring(tokenStart));
        }
        return result;
    }

    /**
     * Inserts a single {@code field=value} token into the map.
     *
     * @param map   target map
     * @param token raw token string of the form {@code fieldName=value}
     */
    private static void putToken(Map<String, String> map, String token) {
        int eq = token.indexOf('=');
        if (eq >= 0) {
            map.put(token.substring(0, eq), token.substring(eq + 1));
        }
    }

    private void sanitizeTasks(Sprint sprint) {
        for (Task task : sprint.getTasks()) {
            // Remove all predecessors that are not visible
            task.getPredecessors().removeIf(relation -> !relation.isVisible());
        }
    }

    /**
     * Returns a toString representation of the given task that excludes the {@code childTasks}
     * field. The Lombok-generated toString of a story task embeds the full list of child
     * {@link Task} objects, making the output very verbose. This method produces the same
     * format without that field.
     *
     * @param task the task to format
     * @return toString-style string with all fields except {@code childTasks}
     */
    static String taskToString(Task task) {
        String              s      = task.toString();
        Map<String, String> tokens = parseToStringTokens(s);
        int                 paren  = s.indexOf('(');
        String              prefix = paren >= 0 ? s.substring(0, paren) : "Task";
        tokens.remove("childTasks");
        StringBuilder sb    = new StringBuilder(prefix).append('(');
        boolean       first = true;
        for (Map.Entry<String, String> e : tokens.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.append(')').toString();
    }
}
