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
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.config;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Status;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.GanttBurndownChartDto;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.GanttChartDto;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.RelationDto;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.TaskDto;
import de.bushnaq.abdalla.kassandra.service.GanttBurndownChartService;
import de.bushnaq.abdalla.kassandra.service.GanttChartService;
import de.bushnaq.abdalla.kassandra.util.AbstractTestUtil;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies server configuration and interactive Gantt zoom rendering.
 */
@Tag("UnitTest")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "kassandra.gantt.render-task-progress-bar-at-day-width=10.5",
        "kassandra.gantt.render-task-progress-text-at-day-width=11.5",
        "kassandra.gantt.render-task-key-at-day-width=22",
        "kassandra.gantt.render-task-non-working-days-at-day-width=12.5",
        "kassandra.gantt.render-task-border-at-day-width=13.5",
        "kassandra.gantt.render-milestone-start-date-at-day-width=14.5",
        "kassandra.gantt.render-milestone-start-time-at-day-width=15.5",
        "kassandra.gantt.render-milestone-border-at-day-width=16.5",
        "kassandra.gantt.render-dependencies-at-day-width=17.5"
})
@WithMockUser(username = "admin-user", roles = "ADMIN")
public class GanttZoomTest extends AbstractTestUtil {
    @Autowired
    private GanttBurndownChartService burndownService;
    @Autowired
    private GanttChartService         ganttService;
    @Autowired
    private JsonMapper                jsonMapper;
    @Autowired
    private KassandraProperties       properties;

    /**
     * Checks binding, both server DTOs and the rendered chart at threshold boundaries.
     *
     * @throws Exception if chart generation or browser verification fails
     */
    @Test
    public void rendersConfiguredZoomDetails() throws Exception {
        KassandraProperties.Gantt zoom = properties.getGantt();
        assertEquals(10.5, zoom.getRenderTaskProgressBarAtDayWidth());
        assertEquals(11.5, zoom.getRenderTaskProgressTextAtDayWidth());
        assertEquals(22, zoom.getRenderTaskKeyAtDayWidth());
        assertEquals(12.5, zoom.getRenderTaskNonWorkingDaysAtDayWidth());
        assertEquals(13.5, zoom.getRenderTaskBorderAtDayWidth());
        assertEquals(14.5, zoom.getRenderMilestoneStartDateAtDayWidth());
        assertEquals(15.5, zoom.getRenderMilestoneStartTimeAtDayWidth());
        assertEquals(16.5, zoom.getRenderMilestoneBorderAtDayWidth());
        assertEquals(17.5, zoom.getRenderDependenciesAtDayWidth());

        LocalDateTime start  = LocalDateTime.parse("2025-06-01T08:00:00");
        LocalDateTime end    = LocalDateTime.parse("2025-06-15T16:30:00");
        LocalDateTime now    = LocalDateTime.parse("2025-06-06T08:00:00");
        Sprint        sprint = mock(Sprint.class);
        when(sprint.getName()).thenReturn("Zoom test");
        when(sprint.getStart()).thenReturn(start);
        when(sprint.getEnd()).thenReturn(end);
        when(sprint.getEarliestStartDate()).thenReturn(start);
        when(sprint.getLatestFinishDate()).thenReturn(end);
        when(sprint.getStatus()).thenReturn(Status.CREATED);

        GanttChartDto         gantt    = ganttService.build(sprint, now, false);
        GanttBurndownChartDto combined = burndownService.build(sprint, now, false);
        assertEquals(zoom, gantt.meta.ganttZoom);
        assertEquals(zoom, combined.meta.ganttZoom);
        assertEquals(Color.blue.getRGB(), gantt.meta.theme.ganttTheme.progressColor);
        assertEquals(Color.blue.getRGB(), combined.meta.theme.ganttTheme.progressColor);
        assertEquals(Color.blue.getRGB(), ganttService.build(sprint, now, true).meta.theme.ganttTheme.progressColor);
        assertEquals(Color.blue.getRGB(), burndownService.build(sprint, now, true).meta.theme.ganttTheme.progressColor);

        TaskDto task = task("00000000-0000-0000-0000-000000000001", "Alpha",
                "2025-06-06T08:00:00", "2025-06-09T16:30:00", 0);
        task.progress         = 0.5;
        task.progressColor    = "#99bbddff";
        task.key              = "K-1";
        task.assignedUserName = "Alice";
        TaskDto milestone = task("00000000-0000-0000-0000-000000000002", "Milestone",
                "2025-06-10T08:00:00", "2025-06-10T08:00:00", 1);
        milestone.milestone = true;
        milestone.predecessors.add(relation(task));
        TaskDto shortTask = task("00000000-0000-0000-0000-000000000003", "Short",
                "2025-06-10T08:00:00", "2025-06-10T14:00:00", 2);
        TaskDto story = task("00000000-0000-0000-0000-000000000004", "Story",
                "2025-06-10T08:00:00", "2025-06-11T16:30:00", 3);
        story.story = true;
        story.predecessors.add(relation(milestone));
        gantt.tasks.add(task);
        gantt.tasks.add(milestone);
        gantt.tasks.add(shortTask);
        gantt.tasks.add(story);
        combined.tasks.addAll(gantt.tasks);
        combined.meta.maxWorkedSeconds         = 27000;
        combined.meta.estimatedBestWorkSeconds = 27000;
        combined.ganttGuideWithBuffer          = null;
        combined.ganttGuideWithoutBuffer       = null;

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", GanttZoomTest::serveResource);
        server.start();
        try {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new", "--disable-gpu", "--window-size=2500,1200");
            ChromeDriver driver = new ChromeDriver(options);
            try {
                driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(120));
                driver.get("http://127.0.0.1:" + server.getAddress().getPort() + "/");
                Object result = driver.executeAsyncScript("""
                        const [gantt, combined, done] = arguments;
                        import('/gantt-zoom-check.js')
                            .then(module => module.verifyGanttZoom(JSON.parse(gantt), JSON.parse(combined)))
                            .then(() => done('ok'))
                            .catch(error => done(String(error.stack || error)));
                        """, jsonMapper.writeValueAsString(gantt), jsonMapper.writeValueAsString(combined));
                assertEquals("ok", result);
            } finally {
                driver.quit();
            }
        } finally {
            server.stop(0);
        }
    }

    /**
     * Rejects invalid thresholds before the application starts.
     */
    @Test
    public void rejectsInvalidThresholds() {
        KassandraProperties invalid = new KassandraProperties();
        invalid.getGantt().setRenderTaskProgressBarAtDayWidth(-1);
        assertThrows(IllegalArgumentException.class, invalid::init);
        invalid.getGantt().setRenderTaskProgressBarAtDayWidth(Double.NaN);
        assertThrows(IllegalArgumentException.class, invalid::init);
        invalid.getGantt().setRenderTaskProgressBarAtDayWidth(Double.POSITIVE_INFINITY);
        assertThrows(IllegalArgumentException.class, invalid::init);
    }

    private static TaskDto task(String id, String name, String start, String finish, int row) {
        TaskDto task = new TaskDto();
        task.id        = UUID.fromString(id);
        task.name      = name;
        task.start     = LocalDateTime.parse(start);
        task.finish    = LocalDateTime.parse(finish);
        task.rowIndex  = row;
        task.fillColor = "#336699ff";
        return task;
    }

    private static RelationDto relation(TaskDto predecessor) {
        RelationDto relation = new RelationDto();
        relation.predecessorId = predecessor.id;
        relation.visible       = true;
        return relation;
    }

    private static void serveResource(HttpExchange exchange) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            byte[] content;
            String contentType;
            if ("/".equals(path)) {
                content     = """
                        <!doctype html><html><body>
                        <div id="chart" style="width: 2400px; height: 900px"></div>
                        </body></html>
                        """.getBytes(StandardCharsets.UTF_8);
                contentType = "text/html; charset=utf-8";
            } else {
                String resource = "/gantt-zoom-check.js".equals(path) ? "gantt-zoom-check.js"
                        : path.startsWith("/js/generated/") && !path.contains("..") ? "META-INF/resources" + path : null;
                try (InputStream stream = resource == null ? null : GanttZoomTest.class.getClassLoader().getResourceAsStream(resource)) {
                    if (stream == null) {
                        exchange.sendResponseHeaders(404, -1);
                        return;
                    }
                    content = stream.readAllBytes();
                }
                contentType = "text/javascript; charset=utf-8";
            }
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, content.length);
            exchange.getResponseBody().write(content);
        }
    }
}
