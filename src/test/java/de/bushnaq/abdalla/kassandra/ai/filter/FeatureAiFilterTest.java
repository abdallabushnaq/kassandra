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

import de.bushnaq.abdalla.kassandra.ai.filter.dto.feature.FeatureFilterDto;
import de.bushnaq.abdalla.kassandra.dto.Feature;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the JavaScript AI filter generator for Feature entities.
 *
 * <p>Each test verifies that the LLM-generated JS filter for a natural-language
 * query produces the same result as a hand-written reference JS predicate applied
 * to the same feature list. Both the LLM filter and the reference filter operate
 * on {@link FeatureFilterDto} objects, keeping the filter layer decoupled from
 * the full entity. The feature list is the only place that needs to change when
 * test data is updated; no individual test hard-codes expected indices or counts.</p>
 */
@Tag("AiUnitTest")
@SpringBootTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class FeatureAiFilterTest extends AbstractAiFilterTest<FeatureFilterDto> {

    public FeatureAiFilterTest(JsonMapper mapper, AiFilterService aiFilterService) {
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
        List<Feature> raw = new ArrayList<>();

        raw.add(createFeature(1L, "User Authentication", 1L,
                OffsetDateTime.of(2023, 6, 15, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 3, 20, 14, 30, 0, 0, ZoneOffset.UTC)));

        raw.add(createFeature(2L, "Payment Processing", 1L,
                OffsetDateTime.of(2024, 1, 10, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 6, 5, 16, 45, 0, 0, ZoneOffset.UTC)));

        raw.add(createFeature(3L, "User Profile Management", 2L,
                OffsetDateTime.of(2024, 2, 28, 11, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 8, 12, 13, 20, 0, 0, ZoneOffset.UTC)));

        raw.add(createFeature(4L, "Shopping Cart", 2L,
                OffsetDateTime.of(2024, 4, 3, 8, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 9, 18, 12, 10, 0, 0, ZoneOffset.UTC)));

        raw.add(createFeature(5L, "Email Notifications", 3L,
                OffsetDateTime.of(2024, 7, 22, 15, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 12, 1, 10, 25, 0, 0, ZoneOffset.UTC)));

        raw.add(createFeature(6L, "Data Analytics Dashboard", 3L,
                OffsetDateTime.of(2024, 9, 5, 14, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 15, 11, 40, 0, 0, ZoneOffset.UTC)));

        raw.add(createFeature(7L, "API Security Enhancement", 4L,
                OffsetDateTime.of(2024, 11, 12, 9, 20, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 2, 8, 16, 15, 0, 0, ZoneOffset.UTC)));

        raw.add(createFeature(8L, "Mobile App Integration", 4L,
                OffsetDateTime.of(2025, 1, 5, 12, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 20, 15, 50, 0, 0, ZoneOffset.UTC)));

        raw.add(createFeature(9L, "Search Functionality", 5L,
                OffsetDateTime.of(2025, 2, 10, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 2, 25, 17, 30, 0, 0, ZoneOffset.UTC)));

        raw.add(createFeature(10L, "Reporting System", 5L,
                OffsetDateTime.of(2025, 3, 18, 13, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 4, 2, 9, 20, 0, 0, ZoneOffset.UTC)));

        raw.add(createFeature(11L, "Social Media Integration", 6L,
                OffsetDateTime.of(2025, 5, 10, 10, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 6, 15, 14, 45, 0, 0, ZoneOffset.UTC)));

        raw.add(createFeature(12L, "Machine Learning Recommendations", 6L,
                OffsetDateTime.of(2025, 7, 1, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 8, 1, 16, 30, 0, 0, ZoneOffset.UTC)));

        testProducts = new ArrayList<>();
        for (Feature f : raw) {
            testProducts.add(FeatureFilterDto.from(f));
        }
    }

    // -------------------------------------------------------------------------
    // Tests — each has an unambiguous natural-language query and a hand-written
    // reference JS predicate that is the ground truth for that query.
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("authentication")
    void testAuthentication() throws Exception {
        assertSearchMatchesReference(
                "authentication",
                "Feature",
                "return entity.getName().toLowerCase().includes('authentication');"
        );
    }

    @Test
    @DisplayName("features belonging to version 1")
    void testFeaturesBelongingToVersion1() throws Exception {
        assertSearchMatchesReference(
                "features belonging to version 1",
                "Feature",
                "return entity.getVersionId() === 1;"
        );
    }

    @Test
    @DisplayName("features created after January 31 2024")
    void testFeaturesCreatedAfterJanuary31_2024() throws Exception {
        // Explicit day removes all ambiguity about whether "after January" means after Jan 1 or Jan 31
        assertSearchMatchesReference(
                "features created after January 31 2024",
                "Feature",
                """
                        const boundary = Java.type('java.time.OffsetDateTime').of(2024, 1, 31, 23, 59, 59, 0, entity.getCreated().getOffset());
                        return entity.getCreated().isAfter(boundary);
                        """
        );
    }

    @Test
    @DisplayName("features created before March 1 2024")
    void testFeaturesCreatedBeforeMarch1_2024() throws Exception {
        // Explicit day removes all ambiguity about whether "before March" means before Mar 1 or Mar 31
        assertSearchMatchesReference(
                "features created before March 1 2024",
                "Feature",
                """
                        const boundary = Java.type('java.time.OffsetDateTime').of(2024, 3, 1, 0, 0, 0, 0, entity.getCreated().getOffset());
                        return entity.getCreated().isBefore(boundary);
                        """
        );
    }

    @Test
    @DisplayName("features created in 2024")
    void testFeaturesCreatedIn2024() throws Exception {
        assertSearchMatchesReference(
                "features created in 2024",
                "Feature",
                "return entity.getCreated().getYear() === 2024;"
        );
    }

    @Test
    @DisplayName("features created in 2025")
    void testFeaturesCreatedIn2025() throws Exception {
        assertSearchMatchesReference(
                "features created in 2025",
                "Feature",
                "return entity.getCreated().getYear() === 2025;"
        );
    }

    @Test
    @DisplayName("features updated in 2025")
    void testFeaturesUpdatedIn2025() throws Exception {
        assertSearchMatchesReference(
                "features updated in 2025",
                "Feature",
                "return entity.getUpdated().getYear() === 2025;"
        );
    }

    @Test
    @DisplayName("name contains Payment")
    void testNameContainsPayment() throws Exception {
        assertSearchMatchesReference(
                "name contains Payment",
                "Feature",
                "return entity.getName().toLowerCase().includes('payment');"
        );
    }

    @Test
    @DisplayName("name contains user")
    void testNameContainsUser() throws Exception {
        assertSearchMatchesReference(
                "name contains user",
                "Feature",
                "return entity.getName().toLowerCase().includes('user');"
        );
    }

}
