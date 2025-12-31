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

package de.bushnaq.abdalla.kassandra.rest.api;

import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.rest.debug.DebugUtil;
import de.bushnaq.abdalla.kassandra.util.AbstractEntityGenerator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;


@Tag("UnitTest")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
//@Transactional
public class WorklogApiTest extends AbstractEntityGenerator {
    @Autowired
    DebugUtil debugUtil;

    //TODO add missing test cases
    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void create() throws Exception {
        User user1 = addRandomUser();

        for (int i = 0; i < 1; i++) {
            Product product = addProduct("Product " + i);
            Version version = addVersion(product, String.format("1.%d.0", i));
            Feature feature = addRandomFeature(version);
            Sprint  sprint  = addRandomSprint(feature);

            Task    task1    = addTask(sprint, null, "Project Phase 1", LocalDateTime.now(), Duration.ofDays(10), null, null, null);
            Task    task2    = addTask(sprint, task1, "Design", LocalDateTime.now(), Duration.ofDays(4), null, user1, null);
            Worklog worklog1 = addWorklog(task2, user1, OffsetDateTime.now(), Duration.ofHours(2), "Design work 1");
            debugUtil.logJson(worklog1);
            Worklog worklog2 = addWorklog(task2, user1, OffsetDateTime.now(), Duration.ofHours(2), "Design work 1");
            Task    task3    = addTask(sprint, task1, "Implementation", LocalDateTime.now().plusDays(4), Duration.ofDays(6), null, user1, task1);
            Worklog worklog3 = addWorklog(task3, user1, OffsetDateTime.now(), Duration.ofHours(1), "Implementation 1");
            Worklog worklog4 = addWorklog(task3, user1, OffsetDateTime.now(), Duration.ofHours(1), "Implementation 2");
        }

        printTables();
        testAllAndPrintTables();
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void update() throws Exception {
        User user1 = addRandomUser();

        for (int i = 0; i < 1; i++) {
            Product product  = addProduct("Product " + i);
            Version version  = addVersion(product, String.format("1.%d.0", i));
            Feature feature  = addRandomFeature(version);
            Sprint  sprint   = addRandomSprint(feature);
            Task    task1    = addTask(sprint, null, "Project Phase 1", LocalDateTime.now(), Duration.ofDays(10), null, null, null);
            Task    task2    = addTask(sprint, task1, "Design", LocalDateTime.now(), Duration.ofDays(4), null, user1, null);
            Worklog worklog1 = addWorklog(task2, user1, OffsetDateTime.now(), Duration.ofHours(2), "Design work 1");
            Worklog worklog2 = addWorklog(task2, user1, OffsetDateTime.now(), Duration.ofHours(2), "Design work 1");
            Task    task3    = addTask(sprint, task1, "Implementation", LocalDateTime.now().plusDays(4), Duration.ofDays(6), null, user1, task1);
            Worklog worklog3 = addWorklog(task3, user1, OffsetDateTime.now(), Duration.ofHours(1), "Implementation 1");
            Worklog worklog4 = addWorklog(task3, user1, OffsetDateTime.now(), Duration.ofHours(1), "Implementation 2");
        }

        testAllAndPrintTables();

        //update
        {
            Worklog worklog1 = expectedWorklogs.getFirst();
            worklog1.setComment("Design work 1 - updated");
            worklog1.setTimeSpent(Duration.ofHours(3));
            worklogApi.persist(worklog1);
        }

        printTables();
        testAllAndPrintTables();

    }

}