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

package de.bushnaq.abdalla.kassandra.ai.mcp.api.sprint;

import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.rest.api.SprintApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring AI native tool implementations for Sprint operations.
 * Uses @Tool annotation for automatic tool registration with ChatClient.
 */
@Component
@Slf4j
public class SprintTools {
    private static final String SPRINT_FIELDS             =
            "id (number): Unique identifier of the sprint, " +
                    "name (string): The sprint name, " +
                    "featureId (number): The feature this sprint belongs to, " +
                    "start (ISO 8601 datetime string): Sprint start, " +
                    "end (ISO 8601 datetime string): Sprint end, " +
                    "releaseDate (ISO 8601 datetime string): Calculated release date, " +
                    "originalEstimation (ISO 8601 duration string): Original estimation, " +
                    "remaining (ISO 8601 duration string): Remaining work, " +
                    "worked (ISO 8601 duration string): Worked time, " +
                    "avatarHash (string): Avatar hash, " +
                    "status (string): Sprint status, " +
                    "userId (number): The user this sprint is assigned to";
    private static final String RETURNS_SPRINT_ARRAY_JSON = "Returns: JSON array of Sprint objects. Each Sprint contains: " + SPRINT_FIELDS;
    private static final String RETURNS_SPRINT_JSON       = "Returns: JSON Sprint object with fields: " + SPRINT_FIELDS;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    @Qualifier("aiSprintApi")
    private SprintApi sprintApi;

    @Tool(description = "Create a new sprint (requires USER or ADMIN role). " + RETURNS_SPRINT_JSON)
    public String createSprint(
            @ToolParam(description = "The sprint name (must be unique)") String name,
            @ToolParam(description = "The feature ID this sprint belongs to") Long featureId) {
        try {
            log.info("Creating sprint with name: {} for feature {}", name, featureId);
            Sprint sprint = new Sprint();
            sprint.setName(name);
            sprint.setFeatureId(featureId);
            Sprint    savedSprint = sprintApi.persist(sprint);
            SprintDto sprintDto   = SprintDto.from(savedSprint);
            return jsonMapper.writeValueAsString(sprintDto);
        } catch (Exception e) {
            log.error("Error creating sprint: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Delete a sprint by ID (requires access or admin role). Returns: Success message (string) confirming deletion")
    public String deleteSprint(
            @ToolParam(description = "The sprint ID") Long id) {
        try {
            log.info("Deleting sprint with ID: {}", id);
            sprintApi.deleteById(id);
            return "Sprint deleted successfully with ID: " + id;
        } catch (Exception e) {
            log.error("Error deleting sprint {}: {}", id, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a list of all sprints accessible to the current user (Admin sees all). " + RETURNS_SPRINT_ARRAY_JSON)
    public String getAllSprints() {
        try {
            log.info("Getting all sprints");
            List<Sprint> sprints = sprintApi.getAll();
            List<SprintDto> sprintDtos = sprints.stream()
                    .map(SprintDto::from)
                    .collect(Collectors.toList());
            return jsonMapper.writeValueAsString(sprintDtos);
        } catch (Exception e) {
            log.error("Error getting all sprints: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a list of all sprints for a feature (requires access or admin role). " + RETURNS_SPRINT_ARRAY_JSON)
    public String getAllSprintsByFeatureId(
            @ToolParam(description = "The feature ID") Long featureId) {
        try {
            log.info("Getting all sprints for feature {}", featureId);
            List<Sprint> sprints = sprintApi.getAll(featureId);
            List<SprintDto> sprintDtos = sprints.stream()
                    .map(SprintDto::from)
                    .collect(Collectors.toList());
            return jsonMapper.writeValueAsString(sprintDtos);
        } catch (Exception e) {
            log.error("Error getting all sprints for feature {}: {}", featureId, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a specific sprint by its ID (requires access or admin role). " + RETURNS_SPRINT_JSON)
    public String getSprintById(
            @ToolParam(description = "The sprint ID") Long id) {
        try {
            log.info("Getting sprint with ID: {}", id);
            Sprint sprint = sprintApi.getById(id);
            if (sprint != null) {
                SprintDto sprintDto = SprintDto.from(sprint);
                return jsonMapper.writeValueAsString(sprintDto);
            }
            return "Sprint not found with ID: " + id;
        } catch (Exception e) {
            log.error("Error getting sprint {}: {}", id, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Update an existing sprint (requires access or admin role). " + RETURNS_SPRINT_JSON)
    public String updateSprint(
            @ToolParam(description = "The sprint ID") Long id,
            @ToolParam(description = "The new sprint name") String name) {
        try {
            log.info("Updating sprint {} with name: {}", id, name);
            Sprint sprint = sprintApi.getById(id);
            if (sprint == null) {
                return "Sprint not found with ID: " + id;
            }
            sprint.setName(name);
            sprintApi.update(sprint);
            SprintDto sprintDto = SprintDto.from(sprint);
            return jsonMapper.writeValueAsString(sprintDto);
        } catch (Exception e) {
            log.error("Error updating sprint {}: {}", id, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }
}
