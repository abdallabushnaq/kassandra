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
import de.bushnaq.abdalla.kassandra.dto.Version;
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
 * Integration test for Version AI filtering testing real LLM regex pattern generation
 * and filtering capabilities with various search scenarios.
 * <p>
 * This test requires Ollama to be running with a model available (e.g., llama3.2:1b).
 * The test will be skipped if Ollama is not available.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class VersionAiFilterTest extends AbstractAiFilterTest<Version> {

    public VersionAiFilterTest(ObjectMapper mapper, AiFilterService aiFilterService) {
        super(mapper, aiFilterService, LocalDate.of(2025, 8, 10));
    }

    private Version createVersion(Long id, String name, Long productId, OffsetDateTime created, OffsetDateTime updated) {
        Version version = new Version();
        version.setId(id);
        version.setName(name);
        version.setProductId(productId);
        version.setCreated(created);
        version.setUpdated(updated);
        return version;
    }

    @BeforeEach
    void setUp() {
        // Create test data
        setupTestVersions();
    }

    private void setupTestVersions() {
        testProducts = new ArrayList<>();

        // Versions with different semantic version patterns and dates for comprehensive testing
        testProducts.add(createVersion(1L, "1.0.0",
                1L,
                OffsetDateTime.of(2023, 6, 15, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 3, 20, 14, 30, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createVersion(2L, "1.2.3",
                1L,
                OffsetDateTime.of(2024, 1, 10, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 6, 5, 16, 45, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createVersion(3L, "2.0.0",
                2L,
                OffsetDateTime.of(2024, 2, 28, 11, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 8, 12, 13, 20, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createVersion(4L, "2.1.5",
                2L,
                OffsetDateTime.of(2024, 4, 3, 8, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 9, 18, 12, 10, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createVersion(5L, "3.0.0-beta",
                3L,
                OffsetDateTime.of(2024, 7, 22, 15, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 12, 1, 10, 25, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createVersion(6L, "3.1.0-alpha",
                3L,
                OffsetDateTime.of(2024, 9, 5, 14, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 15, 11, 40, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createVersion(7L, "4.0.0-rc1",
                4L,
                OffsetDateTime.of(2024, 11, 12, 9, 20, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 2, 8, 16, 15, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createVersion(8L, "0.9.0",
                4L,
                OffsetDateTime.of(2025, 1, 5, 12, 30, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 1, 20, 15, 50, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createVersion(9L, "1.0.0-SNAPSHOT",
                5L,
                OffsetDateTime.of(2025, 2, 10, 8, 15, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 2, 25, 17, 30, 0, 0, ZoneOffset.UTC)));

        testProducts.add(createVersion(10L, "5.2.1",
                5L,
                OffsetDateTime.of(2025, 3, 18, 13, 45, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 4, 2, 9, 20, 0, 0, ZoneOffset.UTC)));
    }

    @Test
    @DisplayName("1.2.3")
    void test1_2_3() throws Exception {
        List<Version> results  = performSearch("1.2.3", "Version");
        List<Version> expected = Collections.singletonList(testProducts.get(1)); // 1.2.3

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("alpha")
    void testAlpha() throws Exception {
        List<Version> results  = performSearch("alpha", "Version");
        List<Version> expected = Collections.singletonList(testProducts.get(5)); // 3.1.0-alpha

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("beta")
    void testBeta() throws Exception {
        List<Version> results  = performSearch("beta", "Version");
        List<Version> expected = Collections.singletonList(testProducts.get(4)); // 3.0.0-beta

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("created in 2025")
    void testCreatedIn2025() throws Exception {
        List<Version> results  = performSearch("created in 2025", "Version");
        List<Version> expected = Arrays.asList(testProducts.get(7), testProducts.get(8), testProducts.get(9)); // 0.9.0, 1.0.0-SNAPSHOT, 5.2.1

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("empty search query")
    void testEmptySearchQuery() throws Exception {
        List<Version> results  = performSearch("", "Version");
        List<Version> expected = new ArrayList<>(testProducts); // All versions should match empty query

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    // === COLUMN-SPECIFIC TESTS (one per column) ===
    @Test
    @DisplayName("name contains beta")
    void testNameContainsBeta() throws Exception {
        List<Version> results  = performSearch("name contains beta", "Version");
        List<Version> expected = Collections.singletonList(testProducts.get(4)); // 3.0.0-beta

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    // === OTHER VALUABLE TEST CASES (preserved from original) ===
    @Test
    @DisplayName("pre-release versions")
    void testPreReleaseVersions() throws Exception {
        List<Version> results = performSearch("pre-release versions", "Version");
        List<Version> expected = Arrays.asList(
                testProducts.get(4), // 3.0.0-beta
                testProducts.get(5), // 3.1.0-alpha
                testProducts.get(6), // 4.0.0-rc1
                testProducts.get(8)  // 1.0.0-SNAPSHOT
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("purple elephant dancing")
    void testPurpleElephantDancing() throws Exception {
        List<Version> results  = performSearch("purple elephant dancing", "Version");
        List<Version> expected = Collections.emptyList(); // Should return empty results for nonsensical queries

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("rc")
    void testRc() throws Exception {
        List<Version> results  = performSearch("rc", "Version");
        List<Version> expected = Collections.singletonList(testProducts.get(6)); // 4.0.0-rc1

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("snapshot")
    void testSnapshot() throws Exception {
        List<Version> results  = performSearch("snapshot", "Version");
        List<Version> expected = Collections.singletonList(testProducts.get(8)); // 1.0.0-SNAPSHOT

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("stable versions")
    void testStableVersions() throws Exception {
        List<Version> results = performSearch("stable versions", "Version");
        List<Version> expected = Arrays.asList(
                testProducts.get(0), // 1.0.0
                testProducts.get(1), // 1.2.3
                testProducts.get(2), // 2.0.0
                testProducts.get(3), // 2.1.5
                testProducts.get(7), // 0.9.0
                testProducts.get(9)  // 5.2.1
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("updated in 2025")
    void testUpdatedIn2025() throws Exception {
        List<Version> results = performSearch("updated in 2025", "Version");
        List<Version> expected = Arrays.asList(
                testProducts.get(5), //3.1.0-alpha
                testProducts.get(6), // 4.0.0-rc1 (created 2024-11-12)
                testProducts.get(7), // 0.9.0 (created 2025-01-05)
                testProducts.get(8), // 1.0.0-SNAPSHOT (created 2025-02-10)
                testProducts.get(9)  // 5.2.1
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("version 1")
    void testVersion1() throws Exception {
        List<Version> results  = performSearch("version 1", "Version");
        List<Version> expected = Arrays.asList(testProducts.get(0), testProducts.get(1), testProducts.get(8)); // 1.0.0, 1.2.3, 1.0.0-SNAPSHOT

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("version 3")
    void testVersion3() throws Exception {
        List<Version> results  = performSearch("version 3", "Version");
        List<Version> expected = Arrays.asList(testProducts.get(4), testProducts.get(5)); // 3.0.0-beta, 3.1.0-alpha

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("version greater than 1.0.0")
    void testVersionGreaterThan1_0_0() throws Exception {
        List<Version> results = performSearch("version greater than 1.0.0", "Version");
//        List<Version> results = performSearch(new ExampleVersionFilter());
        List<Version> expected = Arrays.asList(
                testProducts.get(1), // 1.2.3
                testProducts.get(2), // 2.0.0
                testProducts.get(3), // 2.1.5
                testProducts.get(4), // 3.0.0-beta
                testProducts.get(5), // 3.1.0-alpha
                testProducts.get(6), // 4.0.0-rc1
                testProducts.get(9)  // 5.2.1
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("versions between 1.0.0 and 3.0.0")
    void testVersionsBetween1_0_0And3_0_0() throws Exception {
        List<Version> results = performSearch("versions between 1.0.0 and 3.0.0", "Version");
//        List<Version> results = performSearch(new ExampleVersionFilter());
        List<Version> expected = Arrays.asList(
                testProducts.get(0), // 1.0.0
                testProducts.get(1), // 1.2.3
                testProducts.get(2), // 2.0.0
                testProducts.get(3), // 2.1.5
                testProducts.get(4), // 3.0.0-beta
                testProducts.get(8) // 1.0.0-SNAPSHOT (created 2025-02-10)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("versions created after January 2024")
    void testVersionsCreatedAfterJanuary2024() throws Exception {
        List<Version> results = performSearch("versions created after January 2024", "Version");
        List<Version> expected = Arrays.asList(
                testProducts.get(2), // 2.0.0 (created 2024-02-28)
                testProducts.get(3), // 2.1.5 (created 2024-04-03)
                testProducts.get(4), // 3.0.0-beta (created 2024-07-22)
                testProducts.get(5), // 3.1.0-alpha (created 2024-09-05)
                testProducts.get(6), // 4.0.0-rc1 (created 2024-11-12)
                testProducts.get(7), // 0.9.0 (created 2025-01-05)
                testProducts.get(8), // 1.0.0-SNAPSHOT (created 2025-02-10)
                testProducts.get(9)  // 5.2.1 (created 2025-03-18)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("versions created before March 2024")
    void testVersionsCreatedBeforeMarch2024() throws Exception {
        List<Version> results = performSearch("versions created before March 2024", "Version");
        List<Version> expected = Arrays.asList(
                testProducts.get(0), // 1.0.0 (created 2023-06-15)
                testProducts.get(1), // 1.2.3 (created 2024-01-10)
                testProducts.get(2)  // 2.0.0 (created 2024-02-28)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("versions updated in 2025")
    void testVersionsUpdatedIn2025() throws Exception {
        List<Version> results = performSearch("versions updated in 2025", "Version");
        List<Version> expected = Arrays.asList(
                testProducts.get(5), // 3.1.0-alpha (updated 2025-01-15)
                testProducts.get(6), // 4.0.0-rc1 (updated 2025-02-08)
                testProducts.get(7), // 0.9.0 (updated 2025-01-20)
                testProducts.get(8), // 1.0.0-SNAPSHOT (updated 2025-02-25)
                testProducts.get(9)  // 5.2.1 (updated 2025-04-02)
        );

        assertThat(results).hasSize(expected.size());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    /**
     * add generated ai code to test against one of the tests.
     */
    class ExampleVersionFilter implements Predicate<Version> {

        @Override
        public boolean test(Version entity) {
            if (entity == null) {
                return false;
            }

            try {
                if (entity.getName() == null) return false;
                String[] parts = entity.getName().split("\\.");
                if (parts.length < 3) return false;
                try {
                    int    majorStart     = Integer.parseInt(parts[0]);
                    int    minorStart     = Integer.parseInt(parts[1]);
                    String patchPartStart = parts[2];
                    int    patchStart     = Integer.parseInt(patchPartStart.contains("-") ? patchPartStart.substring(0, patchPartStart.indexOf("-")) : patchPartStart);

                    int versionValueStart = majorStart * 10000 + minorStart * 100 + patchStart;

                    if (parts.length >= 4) {
                        String patchPartEnd = parts[3];
                        int    patchEnd     = Integer.parseInt(patchPartEnd.contains("-") ? patchPartEnd.substring(0, patchPartEnd.indexOf("-")) : patchPartEnd);

                        int versionValueEnd = majorStart * 10000 + minorStart * 100 + patchEnd;
                    } else {
                        String patchPartEnd = parts[2];
                        int    patchEnd     = Integer.parseInt(patchPartEnd.contains("-") ? patchPartEnd.substring(0, patchPartEnd.indexOf("-")) : patchPartEnd);

                        int versionValueEnd = majorStart * 10000 + minorStart * 100 + patchEnd;
                    }

                    return versionValueStart >= 10000 && versionValueStart <= 30000;
                } catch (NumberFormatException e) {
                    return false;
                }

            } catch (Exception e) {
                // Log the error but don't fail the entire filter
                System.err.println("Error in filter execution: " + e.getMessage());
                return false;
            }
        }

    }
}
