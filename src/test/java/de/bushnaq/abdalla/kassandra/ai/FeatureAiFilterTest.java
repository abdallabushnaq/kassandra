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
import de.bushnaq.abdalla.kassandra.dto.Feature;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Feature AI filtering testing real LLM regex pattern generation
 * and filtering capabilities with various search scenarios.
 * <p>
 * This test requires Ollama to be running with a model available (e.g., llama3.2:1b).
 * The test will be skipped if Ollama is not available.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.DisplayName.class)
@Disabled
class FeatureAiFilterTest extends AbstractAiFilterTest<Feature> {

    public FeatureAiFilterTest(ObjectMapper mapper, AiFilterService aiFilterService) {
        super(mapper, aiFilterService, LocalDate.of(2025, 8, 10));
    }

    private Feature createFeature(Long id, String name, Long versionId, OffsetDateTime created, OffsetDateTime updated) {
        Feature feature = new Feature();
        feature.setId(id);
        feature.setName(name);
        feature.setVersionId(versionId);
        feature.setCreated(created);
        feature.setUpdated(updated);
        return feature;
    }

    @BeforeEach
    void setUp() {
        // Create test data
        setupTestFeatures();
    }

    private void setupTestFeatures() {
        testProducts = new ArrayList<>();

        // Features with different functional domains and naming patterns for comprehensive testing
        testProducts.add(createFeature(1L, "User Authentication",
                1L,
                OffsetDateTime.of(2023, 6, 15, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 3, 20, 14, 30, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createFeature(2L, "Payment Processing",
                1L,
                OffsetDateTime.of(2024, 1, 10, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 6, 5, 16, 45, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createFeature(3L, "User Profile Management",
                2L,
                OffsetDateTime.of(2024, 2, 28, 11, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 8, 12, 13, 20, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createFeature(4L, "Shopping Cart",
                2L,
                OffsetDateTime.of(2024, 4, 3, 8, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 9, 18, 12, 10, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createFeature(5L, "Email Notifications",
                3L,
                OffsetDateTime.of(2024, 7, 22, 15, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 12, 1, 10, 25, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createFeature(6L, "Data Analytics Dashboard",
                3L,
                OffsetDateTime.of(2024, 9, 5, 14, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 15, 11, 40, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createFeature(7L, "API Security Enhancement",
                4L,
                OffsetDateTime.of(2024, 11, 12, 9, 20, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 2, 8, 16, 15, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createFeature(8L, "Mobile App Integration",
                4L,
                OffsetDateTime.of(2025, 1, 5, 12, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 20, 15, 50, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createFeature(9L, "Search Functionality",
                5L,
                OffsetDateTime.of(2025, 2, 10, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 2, 25, 17, 30, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createFeature(10L, "Reporting System",
                5L,
                OffsetDateTime.of(2025, 3, 18, 13, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 4, 2, 9, 20, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createFeature(11L, "Social Media Integration",
                6L,
                OffsetDateTime.of(2025, 5, 10, 10, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 6, 15, 14, 45, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createFeature(12L, "Machine Learning Recommendations",
                6L,
                OffsetDateTime.of(2025, 7, 1, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 8, 1, 16, 30, 0, 0, ZoneOffset.UTC)));
    }

    // === SIMPLE TEST CASE (keeping only ONE) ===
    @Test
    @DisplayName("authentication")
    void testAuthentication() throws Exception {
        List<Feature> results  = performSearch("authentication", "Feature");
        List<Feature> expected = Collections.singletonList(testProducts.get(0)); // User Authentication

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("created in 2025")
    void testCreatedIn2025() throws Exception {
        List<Feature> results  = performSearch("created in 2025", "Feature");
        List<Feature> expected = Arrays.asList(testProducts.get(7), testProducts.get(8), testProducts.get(9), testProducts.get(10), testProducts.get(11)); // Features created in 2025

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("features created after January 2024")
    void testFeaturesCreatedAfterJanuary2024() throws Exception {
        List<Feature> results = performSearch("features created after January 2024", "Feature");
        List<Feature> expected = Arrays.asList(
//                testProducts.get(1), // Payment Processing (created 2024-01-10)
                testProducts.get(2), // User Profile Management (created 2024-02-28)
                testProducts.get(3), // Shopping Cart (created 2024-04-03)
                testProducts.get(4), // Email Notifications (created 2024-07-22)
                testProducts.get(5), // Data Analytics Dashboard (created 2024-09-05)
                testProducts.get(6), // API Security Enhancement (created 2024-11-12)
                testProducts.get(7), // Mobile App Integration (created 2025-01-05)
                testProducts.get(8), // Search Functionality (created 2025-02-10)
                testProducts.get(9), // Reporting System (created 2025-03-18)
                testProducts.get(10), // Social Media Integration (created 2025-05-10)
                testProducts.get(11)  // Machine Learning Recommendations (created 2025-07-01)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("features created before March 2024")
    void testFeaturesCreatedBeforeMarch2024() throws Exception {
        List<Feature> results = performSearch("features created before March 2024", "Feature");
        List<Feature> expected = Arrays.asList(
                testProducts.get(0), // User Authentication (created 2023-06-15)
                testProducts.get(1), // Payment Processing (created 2024-01-10)
                testProducts.get(2)  // User Profile Management (created 2024-02-28)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("features updated in 2025")
    void testFeaturesUpdatedIn2025() throws Exception {
        List<Feature> results = performSearch("features updated in 2025", "Feature");
        List<Feature> expected = Arrays.asList(
                testProducts.get(5), // Data Analytics Dashboard (updated 2025-01-15)
                testProducts.get(6), // API Security Enhancement (updated 2025-02-08)
                testProducts.get(7), // Mobile App Integration (updated 2025-01-20)
                testProducts.get(8), // Search Functionality (updated 2025-02-25)
                testProducts.get(9), // Reporting System (updated 2025-04-02)
                testProducts.get(10), // Social Media Integration (updated 2025-06-15)
                testProducts.get(11)  // Machine Learning Recommendations (updated 2025-08-01)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    // === COLUMN-SPECIFIC TESTS (one per column) ===
    @Test
    @DisplayName("name contains Payment")
    void testNameContainsPayment() throws Exception {
        List<Feature> results  = performSearch("name contains Payment", "Feature");
        List<Feature> expected = Collections.singletonList(testProducts.get(1)); // Payment Processing

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("purple elephant dancing")
    void testPurpleElephantDancing() throws Exception {
        List<Feature> results  = performSearch("purple elephant dancing", "Feature");
        List<Feature> expected = Collections.emptyList(); // Should return empty results for nonsensical queries

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("updated in 2025")
    void testUpdatedIn2025() throws Exception {
        List<Feature> results = performSearch("updated in 2025", "Feature");
        List<Feature> expected = Arrays.asList(
                testProducts.get(5), // Data Analytics Dashboard (updated 2025-01-15)
                testProducts.get(6), // API Security Enhancement (updated 2025-02-08)
                testProducts.get(7), // Mobile App Integration (updated 2025-01-20)
                testProducts.get(8), // Search Functionality (updated 2025-02-25)
                testProducts.get(9), // Reporting System (updated 2025-04-02)
                testProducts.get(10), // Social Media Integration (updated 2025-06-15)
                testProducts.get(11)  // Machine Learning Recommendations (updated 2025-08-01)
        );
        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

}
