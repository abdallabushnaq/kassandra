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

package de.bushnaq.abdalla.kassandra.test.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for managing Ollama container in tests
 */
public class OllamaTestHelper {

    private static final int    MAX_STARTUP_WAIT_SECONDS = 120;
    private static final String OLLAMA_URL               = "http://localhost:11434";
    private static final Logger logger                   = LoggerFactory.getLogger(OllamaTestHelper.class);

    /**
     * Starts Ollama for tests if not already running
     */
    public static void ensureOllamaForTests() {
        if (!isOllamaRunning()) {
            logger.info("Ollama not running, starting container...");
            startOllamaContainer();
        } else {
            logger.info("Ollama is already running");
        }
    }

    /**
     * Checks if Ollama is running and accessible
     */
    public static boolean isOllamaRunning() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(OLLAMA_URL + "/api/tags").openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);

            int responseCode = connection.getResponseCode();
            return responseCode == 200;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Pulls the required model
     */
    private static void pullModel() {
        try {
            logger.info("Ensuring llama3.2:1b model is available...");

            // Find the container name dynamically
            ProcessBuilder findContainer = new ProcessBuilder(
                    "docker", "ps", "--format", "{{.Names}}",
                    "--filter", "ancestor=ollama/ollama:latest"
            );
            Process findProcess   = findContainer.start();
            String  containerName = new String(findProcess.getInputStream().readAllBytes()).trim();

            if (containerName.isEmpty()) {
                logger.error("Could not find Ollama container");
                return;
            }

            // Pull the model
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "exec", containerName,
                    "ollama", "pull", "llama3.2:1b"
            );
            pb.inheritIO();
            Process process = pb.start();

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.info("Model llama3.2:1b is ready!");
            } else {
                logger.warn("Model pull returned exit code: {}", exitCode);
            }

        } catch (Exception e) {
            logger.error("Failed to pull model", e);
        }
    }

    /**
     * Starts Ollama container using Docker Compose
     */
    public static void startOllamaContainer() {
        try {
            logger.info("Starting Ollama container...");

            ProcessBuilder pb = new ProcessBuilder(
                    "docker-compose",
                    "-f", "docker-compose-ollama.yml",
                    "up", "-d"
            );
            pb.inheritIO();
            Process process = pb.start();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.warn("Docker compose start returned exit code: {}", exitCode);
            }

            // Wait for Ollama to be ready
            waitForOllamaReady();

            // Pull the model if needed
            pullModel();

        } catch (Exception e) {
            logger.error("Failed to start Ollama container", e);
        }
    }

    /**
     * Stops Ollama container
     */
    public static void stopOllamaContainer() {
        try {
            logger.info("Stopping Ollama container...");

            ProcessBuilder pb = new ProcessBuilder(
                    "docker-compose",
                    "-f", "docker-compose-ollama.yml",
                    "down"
            );
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();

        } catch (Exception e) {
            logger.error("Failed to stop Ollama container", e);
        }
    }

    /**
     * Waits for Ollama to be ready
     */
    private static void waitForOllamaReady() throws InterruptedException {
        logger.info("Waiting for Ollama to be ready...");

        for (int i = 0; i < MAX_STARTUP_WAIT_SECONDS; i++) {
            if (isOllamaRunning()) {
                logger.info("Ollama is ready!");
                return;
            }

            if (i % 10 == 0) {
                logger.info("Still waiting for Ollama... ({}/{} seconds)", i, MAX_STARTUP_WAIT_SECONDS);
            }

            TimeUnit.SECONDS.sleep(1);
        }

        logger.warn("Ollama did not become ready within {} seconds", MAX_STARTUP_WAIT_SECONDS);
    }
}
