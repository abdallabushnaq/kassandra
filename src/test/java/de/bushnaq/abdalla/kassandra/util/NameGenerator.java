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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class NameGenerator {
    public static final  String       PROJECT_HUB_ORG = "@kassandra.org";
    private static final Logger       logger          = Logger.getLogger(NameGenerator.class.getName().toLowerCase());
    private final        List<String> productNames;
    private final        List<String> projectNames;
    private final        List<String> sprintNames;
    private final        List<Name>   userNames;
    private final        List<String> versionNames;

    NameGenerator() {
        NameGeneratorOptions options = new NameGeneratorOptions();
        options.setRandomSeed(123L);//Get deterministic results by setting a random seed.
        org.ajbrown.namemachine.NameGenerator generator = new org.ajbrown.namemachine.NameGenerator(options);
        userNames    = generator.generateNames(1000);
        productNames = new ArrayList<>();
        versionNames = new ArrayList<>();
        projectNames = new ArrayList<>();
        sprintNames  = new ArrayList<>();
        try {
            productNames.addAll(Files.readAllLines(Paths.get("src/test/resources/product-names.txt")));
        } catch (IOException e) {
            logger.severe("Error reading product-names.txt: " + e.getMessage());
        }
        try {
            versionNames.addAll(Files.readAllLines(Paths.get("src/test/resources/version-names.txt")));
        } catch (IOException e) {
            logger.severe("Error reading version-names.txt: " + e.getMessage());
        }
        try {
            projectNames.addAll(Files.readAllLines(Paths.get("src/test/resources/feature-names.txt")));
        } catch (IOException e) {
            logger.severe("Error reading feature-names.txt: " + e.getMessage());
        }
        try {
            sprintNames.addAll(Files.readAllLines(Paths.get("src/test/resources/sprint-names.txt")));
        } catch (IOException e) {
            logger.severe("Error reading sprint-names.txt: " + e.getMessage());
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
            return sprintNames.get(index);
        }
        return String.format("Sprint-%d", index);
    }

    public String generateStoryName(int t) {
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

    public static String generateWorkName(String featureName, int t) {
        String[] workNames = new String[]{"pre-planning", "planning", "analysis", "design", "implementation", "module test", "Functional Test", "System Test", "debugging", "deployment"};
        return String.format("%s-%s", featureName, workNames[t]);
    }
}
