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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionConfig;
import de.bushnaq.abdalla.kassandra.dao.SprintDAO;
import de.bushnaq.abdalla.kassandra.dto.Relation;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.Worklog;
import de.bushnaq.abdalla.kassandra.report.dao.ETheme;
import de.bushnaq.abdalla.kassandra.report.dao.theme.DarkTheme;
import de.bushnaq.abdalla.kassandra.report.dao.theme.LightTheme;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttUtil;
import de.bushnaq.abdalla.kassandra.repository.SprintRepository;
import de.bushnaq.abdalla.kassandra.rest.JacksonConfig;
import de.bushnaq.abdalla.kassandra.service.GanttBurndownChartService;
import de.bushnaq.abdalla.kassandra.service.GanttChartService;
import de.bushnaq.abdalla.kassandra.service.SprintsOverviewService;
import de.bushnaq.abdalla.profiler.Profiler;
import de.bushnaq.abdalla.profiler.SampleType;
import de.bushnaq.abdalla.util.GanttErrorHandler;
import de.bushnaq.abdalla.util.date.DateUtil;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import net.sf.mpxj.ProjectFile;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@Service
public class GanttGenerator extends EntityGenerator {
    private static final String CHART_PAGE = """
            <!doctype html>
            <html><head><meta charset="utf-8"><style>
            html, body { margin: 0; }
            #chart { width: 2400px; height: 900px; }
            </style></head><body><div id="chart"></div></body></html>
            """;
    protected final      Random random     = new Random();
    /**
     * Theme used for Gantt chart generation in tests.
     * Defaults to {@link ETheme#dark} to preserve pre-existing test reference images.
     * Tests that need to switch theme (e.g. {@code CriticalTest}) set this field directly.
     */
    public               ETheme testTheme  = ETheme.light;

    public GanttGenerator(GanttBurndownChartService service) {
        super();
    }

    /**
     * Returns {@code true} when all predecessors of the given task—including predecessors
     * declared on any ancestor task in the parent hierarchy—have a zero
     * {@code remainingEstimate} (i.e. are done).
     * <p>
     * Predecessors that cannot be resolved within the sprint (e.g. cross-sprint relations)
     * are treated as done so they do not block execution.
     * </p>
     *
     * @param task   the task to check
     * @param sprint the sprint that owns the task
     * @return {@code true} if no unfinished predecessors exist; {@code false} otherwise
     */
    private boolean areAllPredecessorsDone(Task task, Sprint sprint) {
        Task cursor = task;
        while (cursor != null) {
            for (Relation relation : cursor.getPredecessors()) {
                Task predecessor = sprint.getTaskById(relation.getPredecessorId());
                if (predecessor != null && !predecessor.getRemainingEstimate().isZero()) {
                    return false;
                }
            }
            cursor = cursor.getParentTask();
        }
        return true;
    }

    private void captureGanttBurndownChart(UUID sprintId, String testFolder, String name) throws Exception {
        Sprint                    sprint       = prepareChartSprint(sprintId);
        StableDiffusionConfig     config       = new StableDiffusionConfig();
        LightTheme                lightTheme   = new LightTheme(config);
        DarkTheme                 darkTheme    = new DarkTheme(config);
        GanttChartService         ganttService = new GanttChartService(lightTheme, darkTheme);
        GanttBurndownChartService chartService = new GanttBurndownChartService(lightTheme, darkTheme, ganttService);
        String json = new JacksonConfig().jsonMapper().writeValueAsString(
                chartService.build(sprint, ParameterOptions.getLocalNow(), testTheme == ETheme.dark));
        captureJsChart(Path.of(testFolder), "/js/generated/burndown/gantt-burndown-bundle.js", "mountGanttBurndownChart",
                json, name, "gantt-burndown-chart");
    }

    private static void captureJsChart(Path outputDirectory, String module, String mount, String json, String name, String expected) throws IOException {
        Files.createDirectories(outputDirectory);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", GanttGenerator::serveChartResource);
        server.start();
        try {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new", "--disable-gpu", "--window-size=2500,1200");
            ChromeDriver driver = new ChromeDriver(options);
            try {
                driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
                driver.get("http://127.0.0.1:" + server.getAddress().getPort() + "/");
                Object result = driver.executeAsyncScript("""
                        const [url, mount, json, expected, done] = arguments;
                        import(url).then(async () => {
                            localStorage.clear();
                            window[mount]('chart', JSON.parse(json), 'Test chart');
                            await document.fonts.ready;
                            await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));
                            const container = document.getElementById('chart');
                            const svg = container.querySelector('svg');
                            if (!svg || !container.getAttribute('data-chart-rendered')?.split(' ').includes(expected))
                                throw new Error('Expected chart did not render an SVG: ' + expected);
                            done({svg: new XMLSerializer().serializeToString(svg)});
                        }).catch(error => done({error: String(error.stack || error)}));
                        """, module, mount, json, expected);
                if (!(result instanceof Map<?, ?> response) || !(response.get("svg") instanceof String svg)) {
                    throw new IllegalStateException("Failed to render " + name + " chart: " + result);
                }
                Files.writeString(outputDirectory.resolve(name + ".svg"), svg, StandardCharsets.UTF_8);
                WebElement    chart = driver.findElement(By.cssSelector("#chart svg"));
                byte[]        png   = chart.getScreenshotAs(OutputType.BYTES);
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
                if (image == null || image.getWidth() < chart.getSize().getWidth() || image.getHeight() < chart.getSize().getHeight()) {
                    throw new IllegalStateException("PNG capture is clipped or invalid for " + name + " chart");
                }
                Files.write(outputDirectory.resolve(name + ".png"), png);
                log.info("Saved {} chart to {}", name, outputDirectory.toAbsolutePath());
            } finally {
                driver.quit();
            }
        } finally {
            server.stop(0);
        }
    }

    private String chartName(TestInfo testInfo) {
        String displayName = testInfo.getDisplayName();
        if (displayName.indexOf('=') != -1) {
            displayName = displayName.substring(displayName.indexOf('"') + 1, displayName.lastIndexOf('"'));
        }
        return displayName + "-" + testTheme.name();
    }

    /**
     * Generates the production combined Gantt and burndown chart as SVG and PNG.
     * Requires the static frontend bundles and Chrome.
     *
     * @param testInfo   test metadata used to name the chart
     * @param sprintId   identifier of the sprint to render
     * @param testFolder output directory for the chart
     * @throws Exception if the sprint, browser rendering, or file output fails
     */
    public void generateBurndownChart(TestInfo testInfo, UUID sprintId, String testFolder) throws Exception {
        captureGanttBurndownChart(sprintId, testFolder, chartName(testInfo));
    }

    /**
     * Generates the production combined Gantt and burndown chart as SVG and PNG.
     * Requires the static frontend bundles and Chrome.
     *
     * @param testInfo   test metadata
     * @param sprintId   identifier of the sprint to render
     * @param testFolder output directory for the chart
     * @throws Exception if the sprint, browser rendering, or file output fails
     */
    public void generateGanttBurndownChart(TestInfo testInfo, UUID sprintId, String testFolder) throws Exception {
        captureGanttBurndownChart(sprintId, testFolder, "gannt-burn-down");
    }

    /**
     * Generates the production Gantt chart as SVG and PNG.
     * Requires the static frontend bundles and Chrome.
     *
     * @param testInfo   test metadata used to name the chart
     * @param sprintId   identifier of the sprint to render
     * @param testFolder output directory for the chart
     * @throws Exception if the sprint, browser rendering, or file output fails
     */
    public void generateGanttChart(TestInfo testInfo, UUID sprintId, String testFolder) throws Exception {
        Sprint                sprint       = prepareChartSprint(sprintId);
        StableDiffusionConfig config       = new StableDiffusionConfig();
        GanttChartService     chartService = new GanttChartService(new LightTheme(config), new DarkTheme(config));
        String json = new JacksonConfig().jsonMapper().writeValueAsString(
                chartService.build(sprint, ParameterOptions.getLocalNow(), testTheme == ETheme.dark));
        captureJsChart(Path.of(testFolder), "/js/generated/gantt/gantt-bundle.js", "mountGanttChart",
                json, chartName(testInfo) + "-gant-chart", "gantt-chart");
    }

    /**
     * Generates the production sprint overview chart as SVG and PNG.
     * Requires the static frontend bundles and Chrome.
     *
     * @param testInfo   test metadata used to name the chart
     * @param sprintId   identifier of the sprint to render
     * @param testFolder output directory for the chart
     * @throws Exception if the sprint, browser rendering, or file output fails
     */
    public void generateOverviewChart(TestInfo testInfo, UUID sprintId, String testFolder) throws Exception {
        Sprint    sprint    = prepareChartSprint(sprintId);
        SprintDAO sprintDao = new SprintDAO();
        sprintDao.setId(sprint.getId());
        sprintDao.setName(sprint.getName());
        sprintDao.setStart(sprint.getStart());
        sprintDao.setEnd(sprint.getEnd());
        sprintDao.setStatus(sprint.getStatus());
        SprintRepository repository = mock(SprintRepository.class);
        when(repository.findAll()).thenReturn(List.of(sprintDao));
        StableDiffusionConfig  config          = new StableDiffusionConfig();
        SprintsOverviewService overviewService = new SprintsOverviewService(repository, new LightTheme(config), new DarkTheme(config));
        String json = new JacksonConfig().jsonMapper().writeValueAsString(
                overviewService.getOverview(ParameterOptions.getLocalNow(), null, testTheme == ETheme.dark));
        captureJsChart(Path.of(testFolder), "/js/generated/sprints-overview/sprints-overview-bundle.js", "mountSprintsOverviewChart",
                json, chartName(testInfo) + "-overview-chart-projectOverviewChart", "sprints-overview-chart");
    }

    /**
     * Generates worklogs for the tasks in the sprint, simulating a team working day-by-day.
     * <p>
     * <b>Estimate inflation (delay &gt; 0):</b> before the day-loop starts, each leaf task's
     * {@code remainingEstimate} is inflated in-memory to a value sampled from
     * {@code [maxEstimate, maxEstimate * (1 + delay)]}, so that delayed sprints exceed the
     * worst-case estimate.  Tasks in sprints with {@code delay == 0} keep their original
     * {@code minEstimate}.
     * </p>
     * <p>
     * <b>Dependency gate:</b> work is only logged for a task when all of its predecessors—
     * including predecessors declared on any ancestor task in the hierarchy—have a zero
     * {@code remainingEstimate} (i.e. are effectively done).
     * </p>
     *
     * @param sprint the sprint to generate worklogs for
     * @param delay  over-run factor (0 = no over-run; 0.3 = up to 30 % above {@code maxEstimate})
     * @param now    the simulated current date/time; work is only logged for days before this date
     */
    @Transactional
    public void generateWorklogs(Sprint sprint, float delay, LocalDateTime now) {
        try (Profiler pc = new Profiler(SampleType.CPU)) {

            final long SECONDS_PER_WORKING_DAY = 75 * 6 * 60;

            // Step 1: pre-inflate remaining estimates when this sprint is delayed.
            if (delay > 0f) {
                for (Task task : sprint.getTasks()) {
                    if (task.isTask() && task.isImpactOnCost()) {
                        Duration maxEstimate = task.getMaxEstimate();
                        if (maxEstimate != null && !maxEstimate.isZero()) {
                            long     extraSeconds = (long) (delay * maxEstimate.getSeconds() * random.nextFloat());
                            Duration inflated     = maxEstimate.plusSeconds(extraSeconds);
                            log.debug("Inflating task '{}': minEstimate={}, inflated remainingEstimate={} (delay={})", task.getName(), task.getMinEstimate(), inflated, delay);
                            task.setRemainingEstimate(inflated);
                        }
                    }
                }
            }
            printTasks(sprint);
            Duration rest = Duration.ofSeconds(1);
            // Step 2: iterate over the days of the sprint
            for (LocalDate day = sprint.getStart().toLocalDate(); !rest.equals(Duration.ZERO) && now.toLocalDate().isAfter(day); day = day.plusDays(1)) {
                LocalDateTime startOfDay = day.atStartOfDay().plusHours(8);
                rest = Duration.ZERO;
                // iterate over all tasks
                for (Task task : sprint.getTasks()) {
                    if (task.isTask() && task.isImpactOnCost()) {
                        Number availability = task.getAvailability();
                        if (!day.isBefore(task.getStart().toLocalDate())) {
                            // Day is on or after task start
                            if (task.getEffectiveCalendar().isWorkingDate(day)) {
                                // is a working day for this user
                                if (task.getStart().isBefore(startOfDay) || task.getStart().isEqual(startOfDay)) {
                                    if (!task.getRemainingEstimate().isZero()) {
                                        if (areAllPredecessorsDone(task, sprint)) {
                                            // we have the whole day
                                            double performance = 1f;//daily performance is usually 100% of the resource availability
                                            if (random.nextFloat() < 0.2f) {
                                                //in rare cases, performance can be much worse or better than usual, e.g. due to unexpected problems or overtime
                                                double minPerformance = 0.5f;//minimum performance of a resource (underwork)
                                                double maxPerformance = 1.2f;//maximum performance of a resource (overwork)
                                                performance = minPerformance + random.nextFloat() * (maxPerformance - minPerformance);
                                            }
                                            Duration maxWork = Duration.ofSeconds((long) ((performance * availability.doubleValue() * SECONDS_PER_WORKING_DAY)));
                                            Duration w       = maxWork;
                                            Duration delta   = task.getRemainingEstimate().minus(w);
                                            if (delta.isZero() || delta.isPositive()) {
                                            } else {
                                                w = task.getRemainingEstimate();
                                            }
                                            if (day.atStartOfDay().toLocalDate().isEqual(task.getStart().toLocalDate())) {
                                                //first day
                                                Worklog worklog = addWorklog(task, task.getAssignedUser(), task.getStart(), w, task.getName());
                                            } else {
                                                Worklog worklog = addWorklog(task, task.getAssignedUser(), day.atStartOfDay().plusHours(8), w, task.getName());//TODO use user calendar
                                            }
                                            task.calculateStatus(true);
                                        } else {
                                            log.debug("Task '{}' blocked on {} – predecessor not yet done", task.getName(), day);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    rest = rest.plus(task.getRemainingEstimate()); // accumulate the rest
                }
            }
            printTasks(sprint);
            sprint.recalculate(ParameterOptions.getLocalNow());
        }
//        flushWorklogBuffer(sprint);
//        persistTasksAndSprint(sprint);
    }

    /**
     * Generates worklogs for the tasks in the sprint simulating a team of people working.
     *
     * @param sprintId
     * @param now
     */
    public void generateWorklogs(UUID sprintId, LocalDateTime now) {
        Sprint sprint = getSprints().stream().filter(s -> s.getId() == sprintId).findFirst().orElseThrow(() -> new IllegalArgumentException("Sprint with id " + sprintId + " not found"));
        try (Profiler pc = new Profiler(SampleType.CPU)) {

            final long         SECONDS_PER_WORKING_DAY = 75 * 6 * 60;
            final long         SECONDS_PER_HOUR        = 60 * 60;
            long               oneDay                  = 75 * SECONDS_PER_HOUR / 10;
            java.time.Duration rest                    = java.time.Duration.ofSeconds(1);
            //- iterate over the days of the sprint
            for (LocalDate day = sprint.getStart().toLocalDate(); !rest.equals(java.time.Duration.ZERO) && now.toLocalDate().isAfter(day); day = day.plusDays(1)) {
                LocalDateTime startOfDay     = day.atStartOfDay().plusHours(8);
                LocalDateTime endOfDay       = day.atStartOfDay().plusHours(16).plusMinutes(30);
                LocalDateTime lunchStartTime = DateUtil.calculateLunchStartTime(day.atStartOfDay());
                LocalDateTime lunchStopTime  = DateUtil.calculateLunchStopTime(day.atStartOfDay());
                rest = java.time.Duration.ZERO;
                //iterate over all tasks
                for (de.bushnaq.abdalla.kassandra.dto.Task task : sprint.getTasks()) {
                    if (task.isTask() && task.isImpactOnCost()) {
                        Number availability = task.getAvailability();
                        if (!day.isBefore(task.getStart().toLocalDate())) {
                            // Day is after task start
                            if (task.getEffectiveCalendar().isWorkingDate(day)) {
                                //is a working day for this user
                                if (task.getStart().isBefore(startOfDay) || task.getStart().isEqual(startOfDay)) {
                                    if (!task.getRemainingEstimate().isZero()) {
                                        // we have the whole day
                                        double             minPerformance = 0.6f;
                                        double             fraction       = minPerformance + random.nextFloat() * (1 - minPerformance) * 1.2;
                                        java.time.Duration maxWork        = java.time.Duration.ofSeconds((long) ((fraction * availability.doubleValue() * SECONDS_PER_WORKING_DAY)));
                                        java.time.Duration w              = maxWork;
                                        java.time.Duration delta          = task.getRemainingEstimate().minus(w);
                                        if (delta.isZero() || delta.isPositive()) {
                                        } else {
                                            w = task.getRemainingEstimate();
                                        }
                                        if (day.atStartOfDay().toLocalDate().isEqual(task.getStart().toLocalDate())) {
                                            //first day
                                            Worklog worklog = addWorklog(task, task.getAssignedUser(), task.getStart(), w, task.getName());
                                        } else {
                                            Worklog worklog = addWorklog(task, task.getAssignedUser(), day.atStartOfDay().plusHours(8), w, task.getName());//TODO use user calendar
                                        }

                                        task.calculateStatus(true);
                                    }
                                }
                            }
                        }
                    }
                    rest = rest.plus(task.getRemainingEstimate());//accumulate the rest
                }
            }
        }
    }

    public void initializeSprint(Sprint sprint) {
        sprint.initialize();
        sprint.initUserMap(getUsers().stream().toList());
        sprint.initTaskMap(getTasks(), getWorklogs());
    }

    public void levelResources(TestInfo testInfo, Sprint sprint, ProjectFile projectFile) throws Exception {
//        initializeInstances();
        GanttUtil         ganttUtil = new GanttUtil();
        GanttErrorHandler eh        = new GanttErrorHandler();
        ganttUtil.levelResources(eh, sprint, "", ParameterOptions.getLocalNow());
    }

    private Sprint prepareChartSprint(UUID sprintId) {
        Sprint sprint = getSprints().stream().filter(s -> s.getId().equals(sprintId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Sprint with id " + sprintId + " not found"));
        initializeSprint(sprint);
        sprint.recalculate(ParameterOptions.getLocalNow());
        return sprint;
    }

    private void printTasks(Sprint sprint) {
        log.info("---------------------------");
        for (Task task : sprint.getTasks()) {
            if (!task.isStory() && !task.isDeliveryBufferTask() && !task.isMilestone())
                log.info("Task '{}': minEstimate={}, maxEstimate={}, spent={}, remainingEstimate={}", task.getName(), task.getMinEstimate(), task.getMaxEstimate(), task.getTimeSpent(), task.getRemainingEstimate());
        }
        log.info("---------------------------");
    }

    private static void sendChartResource(HttpExchange exchange, String contentType, byte[] content) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, content.length);
        exchange.getResponseBody().write(content);
    }

    private static void serveChartResource(HttpExchange exchange) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            if ("/".equals(path)) {
                sendChartResource(exchange, "text/html; charset=utf-8", CHART_PAGE.getBytes(StandardCharsets.UTF_8));
            } else if (path.startsWith("/js/generated/") && !path.contains("..")) {
                try (InputStream resource = GanttGenerator.class.getClassLoader().getResourceAsStream("META-INF/resources" + path)) {
                    if (resource == null) {
                        exchange.sendResponseHeaders(404, -1);
                    } else {
                        sendChartResource(exchange, "text/javascript; charset=utf-8", resource.readAllBytes());
                    }
                }
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
        }
    }


}
