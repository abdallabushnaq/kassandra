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

package de.bushnaq.abdalla.kassandra.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bushnaq.abdalla.kassandra.dto.Product;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Integration test for NaturalLanguageSearchService testing real LLM regex pattern generation
 * and filtering capabilities with various search scenarios.
 * <p>
 * This test requires Ollama to be running with a model available (e.g., llama3.2:1b).
 * The test will be skipped if Ollama is not available.
 */
@Tag("AiUnitTest")
@SpringBootTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class ProductAiFilterTest extends AbstractAiFilterTest<Product> {

    public ProductAiFilterTest(ObjectMapper mapper, AiFilterService aiFilterService) {
        super(mapper, aiFilterService, LocalDate.of(2025, 8, 10));
    }

    private Product createProduct(Long id, String name, OffsetDateTime created, OffsetDateTime updated) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setCreated(created);
        product.setUpdated(updated);
        return product;
    }

    @BeforeEach
    void setUp() {
        // Create test data
        setupTestProducts();
    }

    private void setupTestProducts() {
        testProducts = new ArrayList<>();

        // Products with different names and dates for comprehensive testing
        testProducts.add(createProduct(1L, "Orion Space System",
                OffsetDateTime.of(2023, 6, 15, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 3, 20, 14, 30, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createProduct(2L, "Project Apollo",
                OffsetDateTime.of(2024, 1, 10, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 6, 5, 16, 45, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createProduct(3L, "Mars Explorer",
                OffsetDateTime.of(2024, 2, 28, 11, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 8, 12, 13, 20, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createProduct(4L, "Satellite Network",
                OffsetDateTime.of(2024, 4, 3, 8, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 9, 18, 12, 10, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createProduct(5L, "Lunar Base",
                OffsetDateTime.of(2024, 7, 22, 15, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 12, 1, 10, 25, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createProduct(6L, "Deep Space Probe",
                OffsetDateTime.of(2024, 9, 5, 14, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 15, 11, 40, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createProduct(7L, "Space Station Alpha",
                OffsetDateTime.of(2024, 11, 12, 9, 20, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 2, 8, 16, 15, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createProduct(8L, "Rocket Engine X",
                OffsetDateTime.of(2025, 1, 5, 12, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 20, 15, 50, 0, 0, ZoneOffset.UTC)));
    }

    @Test
    @DisplayName("MARS")
    void testMARS() throws Exception {
        List<Product> results  = performSearch("MARS", "Product");
        List<Product> expected = Collections.singletonList(testProducts.get(2)); // Mars Explorer

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("name contains project")
    void testNameContainsProject() throws Exception {
        List<Product> results  = performSearch("name contains project", "Product");
        List<Product> expected = Collections.singletonList(testProducts.get(1)); // Project Apollo

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("Orion")
    void testOrion() throws Exception {
        List<Product> results  = performSearch("Orion", "Product");
        List<Product> expected = Collections.singletonList(testProducts.get(0)); // Orion Space System

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("products created after July 2024")
    void testProductsCreatedAfterJuly2024() throws Exception {
        List<Product> results = performSearch("products created after July 2024", "Product");
//        List<Product> results = performSearch(new ExampleProductFilter());
        List<Product> expected = Arrays.asList(
                testProducts.get(5), // Deep Space Probe (created 2024-09-05)
                testProducts.get(6), // Space Station Alpha (created 2024-11-12)
                testProducts.get(7)  // Rocket Engine X (created 2025-01-05)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("products created in 2024")
    void testProductsCreatedIn2024() throws Exception {
        List<Product> results = performSearch("products created in 2024", "Product");
        List<Product> expected = Arrays.asList(
                testProducts.get(1),
                testProducts.get(2),
                testProducts.get(3),
                testProducts.get(4),
                testProducts.get(5),
                testProducts.get(6)
        );
        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("products updated in 2025")
    void testProductsUpdatedIn2025() throws Exception {
        List<Product> results = performSearch("products updated in 2025", "Product");
        List<Product> expected = Arrays.asList(
                testProducts.get(5), // Deep Space Probe (updated 2025-01-15)
                testProducts.get(6), // Space Station Alpha (updated 2025-02-08)
                testProducts.get(7)  // Rocket Engine X (updated 2025-01-20)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("space products created in 2024")
    void testSpaceProductsCreatedIn2024() throws Exception {
        List<Product> results = performSearch("space products created in 2024", "Product");
        List<Product> expected = Arrays.asList(
                testProducts.get(5),//
                testProducts.get(6)//
        ); // Space Station Alpha

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    /**
     * add generated ai code to test against one of the tests.
     */
    class ExampleProductFilter implements Predicate<Product> {

        @Override
        public boolean test(Product entity) {
            if (entity == null) {
                return false;
            }

            try {
                // Execute the generated filter code
                return entity.getCreated() != null && entity.getCreated().isAfter(OffsetDateTime.of(2024, 7, 31, 23, 59, 59, 0, OffsetDateTime.now().getOffset()));
            } catch (Exception e) {
                // Log the error but don't fail the entire filter
                System.err.println("Error in filter execution: " + e.getMessage());
                return false;
            }
        }

    }

}
