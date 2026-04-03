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
import de.bushnaq.abdalla.kassandra.report.dao.ETheme;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

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
public class TestTest extends AbstractGanttTester {

    private static final String testFolder = "references/test";

    @BeforeEach
    public void beforeEach() {
        Logger logger = (Logger) LoggerFactory.getLogger("de.bushnaq");
        logger.setLevel(Level.TRACE);
    }

    /**
     * Provides the list of .mpp file names (without extension) in the testFolder for parameterized testing.
     *
     * @return Stream of file names without the .mpp extension
     */
    public static Stream<String> mppFileNamesProvider() throws Exception {
        Path folder = Paths.get(testFolder);
        return getMppFilesStream(folder);
    }

    @ParameterizedTest
    @MethodSource("mppFileNamesProvider")
    public void test(String name, TestInfo testInfo) throws Exception {
        testTheme = ETheme.dark;
        executeTest(name, testInfo, testFolder);
        testTheme = ETheme.light;
        executeTest(name, testInfo, testFolder);
    }

}
