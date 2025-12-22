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

package de.bushnaq.abdalla.kassandra.util;

import org.ajbrown.namemachine.Name;
import org.ajbrown.namemachine.NameGeneratorOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

public class NameGenerator {
    public static final  String       PROJECT_HUB_ORG = "@kassandra.org";
    private static final Logger       logger          = Logger.getLogger(NameGenerator.class.getName().toLowerCase());
    private final        List<String> productNames;
    private final        List<String> projectNames;
    private final        List<String> sprintNames;
    private final        List<String> storyNames;
    private final        Set<String>  usedStoryNames;
    private final        List<Name>   userNames;
    private final        List<String> versionNames;

    NameGenerator() {
        NameGeneratorOptions options = new NameGeneratorOptions();
        options.setRandomSeed(123L);//Get deterministic results by setting a random seed.
        org.ajbrown.namemachine.NameGenerator generator = new org.ajbrown.namemachine.NameGenerator(options);
        userNames      = generator.generateNames(1000);
        productNames   = new ArrayList<>();
        versionNames   = new ArrayList<>();
        projectNames   = new ArrayList<>();
        sprintNames    = new ArrayList<>();
        storyNames     = new ArrayList<>();
        usedStoryNames = new HashSet<>();
        try {
            productNames.addAll(Files.readAllLines(Paths.get("src/test/resources/product-names.txt"))
                    .stream()
                    .filter(line -> line != null && !line.trim().isEmpty())
                    .toList());
        } catch (IOException e) {
            logger.severe("Error reading product-names.txt: " + e.getMessage());
        }
        try {
            versionNames.addAll(Files.readAllLines(Paths.get("src/test/resources/version-names.txt"))
                    .stream()
                    .filter(line -> line != null && !line.trim().isEmpty())
                    .toList());
        } catch (IOException e) {
            logger.severe("Error reading version-names.txt: " + e.getMessage());
        }
        try {
            projectNames.addAll(Files.readAllLines(Paths.get("src/test/resources/feature-names.txt"))
                    .stream()
                    .filter(line -> line != null && !line.trim().isEmpty())
                    .toList());
        } catch (IOException e) {
            logger.severe("Error reading feature-names.txt: " + e.getMessage());
        }
        try {
            sprintNames.addAll(Files.readAllLines(Paths.get("src/test/resources/sprint-names.txt"))
                    .stream()
                    .filter(line -> line != null && !line.trim().isEmpty())
                    .toList());
        } catch (IOException e) {
            logger.severe("Error reading sprint-names.txt: " + e.getMessage());
        }
        try {
            storyNames.addAll(Files.readAllLines(Paths.get("src/test/resources/story-names.txt"))
                    .stream()
                    .filter(line -> line != null && !line.trim().isEmpty())
                    .toList());
        } catch (IOException e) {
            logger.severe("Error reading story-names.txt: " + e.getMessage());
        }
    }

    public String generateFeatureName(int index) {
        if (index >= 0 && index < projectNames.size()) {
            return projectNames.get(index);
        }
        return String.format("Feature-%d", index);
    }

    public String generateProductName(int index) {
        if (index >= 0 && index < productNames.size()) {
            return productNames.get(index);
        }
        return String.format("Product-%d", index);
    }

    public String generateSprintName(int index) {
        if (index >= 0 && index < sprintNames.size()) {
            // Capitalize first letter of city name
            String cityName = sprintNames.get(index);
            cityName = cityName.substring(0, 1).toUpperCase() + cityName.substring(1);
            return cityName;
        }
        return String.format("Sprint-%d", index);
    }

    public String generateStoryName(int t) {
        if (t >= 0 && t < storyNames.size()) {
            return storyNames.get(t);
        }
        return String.format("Story-%d", t);
    }

    public String generateUserEmail(int userIndex) {
        return userNames.get(userIndex).getFirstName().toLowerCase() + "." + userNames.get(userIndex).getLastName().toLowerCase() + PROJECT_HUB_ORG;
    }

    public String generateUserName(int userIndex) {
        return userNames.get(userIndex).getFirstName() + " " + userNames.get(userIndex).getLastName();
    }

    public String generateVersionName(int index) {
        if (index >= 0 && index < versionNames.size()) {
            return versionNames.get(index);
        }
        return String.format("1.%d.0", index);
    }

    public static String generateWorkName(String storyName, int t) {
        // Define realistic task types that would be part of developing a story
        String[] workTypes = new String[]{
                "Requirements Analysis",
                "Technical Design",
                "Database Schema",
                "Backend Implementation",
                "Frontend Development",
                "API Integration",
                "Unit Testing",
                "Integration Testing",
                "Code Review",
                "Bug Fixing",
                "Documentation",
                "Deployment Setup",
                "Performance Testing",
                "Security Review",
                "User Acceptance Testing"
        };

        if (t >= 0 && t < workTypes.length) {
            return String.format("%s - %s", storyName, workTypes[t]);
        }
        return String.format("%s - Task %d", storyName, t);
    }

    /**
     * Get total number of available story names
     */
    public int getAvailableStoryCount() {
        return storyNames.size() - usedStoryNames.size();
    }

    /**
     * Get a randomized list of story names for a sprint.
     * Uses a deterministic shuffle based on sprint index to ensure reproducibility.
     * Tracks used stories to avoid duplicates across sprints.
     *
     * @param sprintIndex The sprint index (used as seed for shuffling)
     * @param count       Number of stories needed
     * @return List of unique story names (always returns exactly 'count' stories)
     */
    public List<String> getShuffledStoryNames(int sprintIndex, int count) {
        List<String> result = new ArrayList<>();

        // Check if story names were loaded
        if (storyNames.isEmpty()) {
            logger.warning("Story names list is empty! Generating fallback story names.");
            for (int i = 0; i < count; i++) {
                result.add(String.format("Story-%d", sprintIndex * 100 + i));
            }
            return result;
        }

        List<String> availableStories = new ArrayList<>();

        // Get stories that haven't been used yet
        for (String story : storyNames) {
            if (!usedStoryNames.contains(story)) {
                availableStories.add(story);
            }
        }

        // If we don't have enough unused stories, reset the pool
        if (availableStories.size() < count) {
            logger.warning(String.format("Not enough unused stories (%d available, %d needed). Resetting story pool.",
                    availableStories.size(), count));
            usedStoryNames.clear();
            availableStories = new ArrayList<>(storyNames);
        }

        // Shuffle with a deterministic seed based on sprint index
        Collections.shuffle(availableStories, new Random(123L + sprintIndex));

        // Take the first 'count' stories, or all available if we still don't have enough
        int storiesToTake = Math.min(count, availableStories.size());
        for (int i = 0; i < storiesToTake; i++) {
            result.add(availableStories.get(i));
        }

        // If we still need more stories (shouldn't happen after reset, but safety check)
        while (result.size() < count) {
            String fallbackName = String.format("Story-%d", sprintIndex * 100 + result.size());
            logger.warning(String.format("Generating fallback story name: %s", fallbackName));
            result.add(fallbackName);
        }

        // Mark them as used
        usedStoryNames.addAll(result);

        return result;
    }

    /**
     * Reset the story pool (useful when starting a new test or project)
     */
    public void resetStoryPool() {
        usedStoryNames.clear();
    }
}
