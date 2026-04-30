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

package de.bushnaq.abdalla.util.db.er;

import de.bushnaq.abdalla.kassandra.Application;
import de.bushnaq.abdalla.kassandra.util.AbstractEntityGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Generates an SVG ER diagram from the live in-memory H2 schema and writes it
 * to {@code test-results/er-diagram.svg}.
 *
 * <p>The test is tagged {@code UnitTest} so it participates in the standard
 * fast-test suite.  It extends {@link AbstractEntityGenerator} so the full
 * entity hierarchy is seeded before the diagram is produced, giving a
 * representative view of the schema.
 */
@Tag("UnitTest")
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
public class ErDiagramTest extends AbstractEntityGenerator {

    private static final String OUTPUT_PATH = "../kassandra.wiki/er-diagram.svg";

    /**
     * Extracts the H2 schema and renders it as an SVG ER diagram.
     *
     * @param testInfo JUnit test info injected by the framework
     * @throws Exception if the diagram cannot be generated or written
     */
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void generateErDiagram(TestInfo testInfo) throws Exception {
        log.info("Generating ER diagram → {}", OUTPUT_PATH);

        SchemaExtractor   extractor = new SchemaExtractor();
        ErSchema          schema    = extractor.extract(entityManager);
        ErDiagramRenderer renderer  = new ErDiagramRenderer();
        renderer.render(schema, OUTPUT_PATH);

        File outputFile = new File(OUTPUT_PATH);
        assertTrue(outputFile.exists(), "ER diagram file was not created: " + OUTPUT_PATH);
        assertTrue(outputFile.length() > 0, "ER diagram file is empty: " + OUTPUT_PATH);

        log.info("ER diagram generated successfully: {} bytes", outputFile.length());
    }
}

