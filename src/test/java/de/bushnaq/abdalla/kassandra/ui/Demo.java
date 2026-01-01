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

package de.bushnaq.abdalla.kassandra.ui;

import de.bushnaq.abdalla.kassandra.dto.Feature;
import de.bushnaq.abdalla.kassandra.dto.Product;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Version;
import de.bushnaq.abdalla.kassandra.ui.util.AbstractKeycloakUiTestUtil;
import de.bushnaq.abdalla.kassandra.ui.util.selenium.HumanizedSeleniumHandler;
import de.bushnaq.abdalla.kassandra.ui.view.util.FeatureListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.ProductListViewTester;
import de.bushnaq.abdalla.kassandra.ui.view.util.VersionListViewTester;
import de.bushnaq.abdalla.kassandra.util.RandomCase;
import de.bushnaq.abdalla.kassandra.util.TestInfoUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=${test.server.port:0}",
                "spring.profiles.active=test",
                "spring.security.basic.enabled=false"// Disable basic authentication for these tests
        }
)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class Demo extends AbstractKeycloakUiTestUtil {
    private static final Logger                   logger = LoggerFactory.getLogger(Demo.class);
    @Autowired
    private              FeatureListViewTester    featureListViewTester;
    @Autowired
    private              ProductListViewTester    productListViewTester;
    @Autowired
    private              HumanizedSeleniumHandler seleniumHandler;
    @Autowired
    private              VersionListViewTester    versionListViewTester;


    @AfterAll
    static void cleanupOllama() {
        logger.info("=== Demo completed - Ollama container will remain running ===");
        logger.info("To stop Ollama manually, run: ollama-helper.bat stop");
    }

    /**
     * Demonstrates the natural language search functionality
     */
    private void demonstrateNaturalLanguageSearch() throws InterruptedException {
//        logger.info("=== Demonstrating Natural Language Search with LLM ===");
//
//        // Wait for page to fully load
//        Thread.sleep(2000);
//
//        // Find the smart search field
//        WebElement searchField = seleniumHandler.getDriver().findElement(By.id("product-global-filter"));
//
//        // Demo queries to showcase LLM capabilities
//        String[] demoQueries = {
//                "products created after January 2024",
//                "name contains test",
//                "key:PROJ-123",
//                "show me items created before December",
//                "find project items",
//                "products updated after 2024-06-01"
//        };
//
//        for (String query : demoQueries) {
//            logger.info("Testing natural language query: '{}'", query);
//
//            // Clear and enter the query
//            seleniumHandler.setTextField("product-global-filter", query);
//
////            searchField.clear();
////            Thread.sleep(500);
////            searchField.sendKeys(query);
//
//            // Wait for search to process and show results
//            Thread.sleep(3000);
//
//            // Check if feedback is shown
//            try {
//                WebElement statusSpan = seleniumHandler.getDriver().findElement(By.className("smart-global-filter")).findElement(By.tagName("span"));
//                if (statusSpan.isDisplayed()) {
//                    logger.info("Search feedback: {}", statusSpan.getText());
//                }
//            } catch (Exception e) {
//                // Status span might not be visible for all queries
//            }
//
//            // Pause between queries for demonstration
//            Thread.sleep(2000);
//        }
//
//        // Clear the search to show all results again
//        searchField.clear();
//        Thread.sleep(1000);
//
//        logger.info("=== Natural Language Search Demo Completed ===");
    }

    private static List<RandomCase> listRandomCases() {
        RandomCase[] randomCases = new RandomCase[]{//
//                new RandomCase(1, LocalDate.parse("2024-12-01"), Duration.ofDays(10), 10, 2, 1, 2, 1),//
//                new RandomCase(2, LocalDate.parse("2024-12-01"), Duration.ofDays(10), 1, 1, 1, 6, 6, 8, 8, 6, 7)//
                new RandomCase(3, OffsetDateTime.parse("2025-08-11T08:00:00+01:00"), LocalDate.parse("2025-08-04"), Duration.ofDays(10), 2, 2, 2, 2, 2, 2, 1, 5, 5, 8, 8, 6, 7)//
//                new RandomCase(3, LocalDate.parse("2024-12-01"), Duration.ofDays(10), 4, 3, 3, 3, 10, 5, 8, 5, 1)//
        };
        return Arrays.stream(randomCases).toList();
    }

    @BeforeAll
    static void setupOllama() {
        logger.info("=== Setting up Ollama for Natural Language Search Demo ===");
//        OllamaTestHelper.ensureOllamaForTests();
    }

    @ParameterizedTest
    @MethodSource("listRandomCases")
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    public void testShowProducts(RandomCase randomCase, TestInfo testInfo) throws Exception {
        TestInfoUtil.setTestMethod(testInfo, testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        TestInfoUtil.setTestCaseIndex(testInfo, randomCase.getTestCaseIndex());
        setTestCaseName(this.getClass().getName(), testInfo.getTestMethod().get().getName() + "-" + randomCase.getTestCaseIndex());
        generateProductsIfNeeded(testInfo, randomCase);

        List<Product> products     = productApi.getAll();
        Product       firstProduct = products.getFirst();
        List<Version> versions     = versionApi.getAll(firstProduct.getId());
        Version       firstVersion = versions.getFirst();
        List<Feature> features     = featureApi.getAll(firstVersion.getId());
        Feature       firstFeature = features.getFirst();
        List<Sprint>  sprints      = sprintApi.getAll(firstFeature.getId());
        Sprint        firstSprint  = sprints.getFirst();

//        productListViewTester.switchToProductListViewWithOidc("christopher.paul@kassandra.org", "password", null, testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
        productListViewTester.switchToProductListViewWithOidc("jennifer.holleman@kassandra.org", "password", null, testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
//        productListViewTester.switchToProductListView(testInfo.getTestClass().get().getSimpleName(), generateTestCaseName(testInfo));
//        seleniumHandler.getAndCheck("http://localhost:" + productListViewTester.getPort() + "/ui/" + "grid-row-reordering");

//        // Demo the natural language search capabilities
//        demonstrateNaturalLanguageSearch();
//        productListViewTester.selectProduct(firstProduct.getName());
//        versionListViewTester.selectVersion(firstVersion.getName());
//        featureListViewTester.selectFeature(firstFeature.getName());

//        seleniumHandler.click(SprintListView.SPRINT_GRID_CONFIG_BUTTON_PREFIX + firstSprint.getName());

        seleniumHandler.waitUntilBrowserClosed(0);
    }

}
