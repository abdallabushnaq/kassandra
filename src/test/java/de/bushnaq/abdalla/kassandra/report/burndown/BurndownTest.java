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

package de.bushnaq.abdalla.kassandra.report.burndown;

import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.rest.debug.DebugUtil;
import de.bushnaq.abdalla.kassandra.util.AbstractGanttTestUtil;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;


@Tag("UnitTest")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class BurndownTest extends AbstractGanttTestUtil {
    @Autowired
    DebugUtil debugUtil;

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void burnDown(RandomCase randomCase, TestInfo testInfo) throws Exception {
        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateProductsIfNeeded(testInfo, randomCase);
        Sprint savedSprint = peg.sprintApi.getAll().getFirst();
        savedSprint.initialize();
        savedSprint.initUserMap(peg.userApi.getAll(savedSprint.getId()));
        savedSprint.initTaskMap(peg.taskApi.getAll(savedSprint.getId()), peg.worklogApi.getAll(savedSprint.getId()));
        {
            levelResourcesAndPersist(testInfo, savedSprint, null);
            generateBurndownChart(testInfo, savedSprint.getId());
            generateGanttChart(testInfo, savedSprint.getId(), null);
        }
    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
                RandomCase.builder()
                        .testCaseIndex(1)
                        .now(OffsetDateTime.parse("2025-05-05T08:00:00+01:00"))
                        .startDate(LocalDate.parse("2024-12-15"))
                        .minNumberOfProducts(1).maxNumberOfProducts(1)
                        .minNumberOfVersions(1).maxNumberOfVersions(1)
                        .minNumberOfFeatures(1).maxNumberOfFeatures(1)
                        .minNumberOfSprints(1).maxNumberOfSprints(1)
                        .maxTaskDurationDays(10).maxNumberOfStories(2)
                        .maxNumberOfUsers(1).maxNumberOfUsersPerSprint(2)
                        .maxNumberOfTasks(1).seed(1)
                        .build(),//
                RandomCase.builder()
                        .testCaseIndex(2)
                        .now(OffsetDateTime.parse("2025-05-05T08:00:00+01:00"))
                        .startDate(LocalDate.parse("2024-12-15"))
                        .minNumberOfProducts(1).maxNumberOfProducts(1)
                        .minNumberOfVersions(1).maxNumberOfVersions(1)
                        .minNumberOfFeatures(1).maxNumberOfFeatures(1)
                        .minNumberOfSprints(1).maxNumberOfSprints(1)
                        .maxTaskDurationDays(10).maxNumberOfStories(3)
                        .maxNumberOfUsers(2).maxNumberOfUsersPerSprint(3)
                        .maxNumberOfTasks(1).seed(1)
                        .build()//
        };
        return Arrays.stream(randomCases).toList();
    }


}
