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

package de.bushnaq.abdalla.kassandra.ai.filter;

import de.bushnaq.abdalla.kassandra.dto.Product;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

/**
 * Tests the JavaScript AI filter generator for Product entities.
 *
 * <p>Each test verifies that the LLM-generated JS filter for a natural-language
 * query produces the same result as a hand-written reference JS predicate applied
 * to the same product list.  The reference predicate is the ground truth — it must
 * be simple, obvious, and unambiguous.  The product list is the only place that
 * needs to change when test data is updated; no individual test hard-codes expected
 * indices or counts.</p>
 */
@Tag("AiUnitTest")
@SpringBootTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class ProductAiFilterTest extends AbstractAiFilterTest<Product> {

    public ProductAiFilterTest(JsonMapper mapper, AiFilterService aiFilterService) {
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
        setupTestProducts();
    }

    private void setupTestProducts() {
        testProducts = new ArrayList<>();

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

    // -------------------------------------------------------------------------
    // Tests — each has an unambiguous natural-language query and a hand-written
    // reference JS predicate that is the ground truth for that query.
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("MARS")
    void testMARS() throws Exception {
        assertSearchMatchesReference(
                "MARS",
                "Product",
                "return entity.getName().toLowerCase().includes('mars');"
        );
    }

    @Test
    @DisplayName("name contains project")
    void testNameContainsProject() throws Exception {
        assertSearchMatchesReference(
                "name contains project",
                "Product",
                "return entity.getName().toLowerCase().includes('project');"
        );
    }

    @Test
    @DisplayName("Orion")
    void testOrion() throws Exception {
        assertSearchMatchesReference(
                "Orion",
                "Product",
                "return entity.getName().toLowerCase().includes('orion');"
        );
    }

    @Test
    @DisplayName("products created after July 2024")
    void testProductsCreatedAfterJuly2024() throws Exception {
        // "after July 2024" means after the last moment of July 31, 2024
        assertSearchMatchesReference(
                "products created after July 2024",
                "Product",
                """
                        const boundary = Java.type('java.time.OffsetDateTime').of(2024, 7, 31, 23, 59, 59, 0, entity.getCreated().getOffset());
                        return entity.getCreated().isAfter(boundary);
                        """
        );
    }

    @Test
    @DisplayName("products created in 2024")
    void testProductsCreatedIn2024() throws Exception {
        assertSearchMatchesReference(
                "products created in 2024",
                "Product",
                "return entity.getCreated().getYear() === 2024;"
        );
    }

    @Test
    @DisplayName("products updated in 2025")
    void testProductsUpdatedIn2025() throws Exception {
        assertSearchMatchesReference(
                "products updated in 2025",
                "Product",
                "return entity.getUpdated().getYear() === 2025;"
        );
    }

    @Test
    @DisplayName("space products created in 2024")
    void testSpaceProductsCreatedIn2024() throws Exception {
        assertSearchMatchesReference(
                "space products created in 2024",
                "Product",
                "return entity.getName().toLowerCase().includes('space') && entity.getCreated().getYear() === 2024;"
        );
    }
}
