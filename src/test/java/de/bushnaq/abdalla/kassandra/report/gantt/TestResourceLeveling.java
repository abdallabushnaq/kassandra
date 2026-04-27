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


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.report.dao.ETheme;
import de.bushnaq.abdalla.kassandra.util.MPXJReader;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.OffsetDateTime;

/**
 * loading mpp files once with all start/finish/duration information into a reference sprint, then
 * loading mpp files once without start/finish/duration information into a sprint.
 * then leveling the sprint resources
 * then comparing both sprints.
 * <p>
 * This ensures that leveling  resources matches what ms project does, and that the generated gantt chart matches what ms project generates.
 */
@Tag("UnitTest")
@Slf4j
public class TestResourceLeveling extends AbstractGanttTester {
    private final String testFolder = "references/resource-leveling";

    @BeforeEach
    public void beforeEach() {
        Logger logger = (Logger) LoggerFactory.getLogger("de.bushnaq");
        logger.setLevel(Level.TRACE);
        ParameterOptions.setNow(OffsetDateTime.parse("2025-06-05T08:00:00+01:00"));
    }

    @Test
    public void gantt_01(TestInfo testInfo) throws Exception {
        MPXJReader g = new MPXJReader("references/resource-leveling", true);
        g.testTheme = ETheme.light;
        Sprint sprint = g.load(Path.of("references/resource-leveling/Tokyo.xml"), false);

        g.levelResources(testInfo, sprint, null);
        g.generateGanttChart(testInfo, sprint.getId(), testFolder);
    }

}
