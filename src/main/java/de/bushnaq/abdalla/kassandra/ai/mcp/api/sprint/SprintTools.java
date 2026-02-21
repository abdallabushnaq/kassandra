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

import de.bushnaq.abdalla.kassandra.ai.mcp.ToolActivityContextHolder;
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
    private static final String SPRINT_FIELDS             = """
            sprintId (number): Unique identifier of the sprint,
            name (string): The sprint name,
            featureId (number): The feature this sprint belongs to,
            userId (number): The user this sprint is assigned to,
            start (ISO 8601 datetime string): Sprint start,
            end (ISO 8601 datetime string): Sprint end,
            releaseDate (ISO 8601 datetime string): Calculated release date,
            originalEstimation (ISO 8601 duration string): Original estimation,
            remaining (ISO 8601 duration string): Remaining work,
            worked (ISO 8601 duration string): Worked time,
            status (string): Sprint status,
            avatarHash (string): Avatar hash
            """;
    private static final String RETURNS_SPRINT_ARRAY_JSON = "Returns: JSON array of Sprint objects. Each Sprint contains: " + SPRINT_FIELDS;
    private static final String RETURNS_SPRINT_JSON       = "Returns: JSON Sprint object with fields: " + SPRINT_FIELDS;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    @Qualifier("aiSprintApi")
    private SprintApi sprintApi;

    @Tool(description = "Create a new sprint (requires USER or ADMIN role). " +
            "IMPORTANT: The returned JSON includes an 'sprintId' field - you MUST extract and use this ID for subsequent operations (like deleting this sprint). " +
            RETURNS_SPRINT_JSON)
    public String createSprint(
            @ToolParam(description = "The sprint name (must be unique)") String name,
            @ToolParam(description = "The featureId this sprint belongs to") Long featureId) {
        try {
//            ToolActivityContextHolder.reportActivity("Creating sprint with name: " + name + " for feature " + featureId);
            Sprint sprint = new Sprint();
            sprint.setName(name);
            sprint.setFeatureId(featureId);
            Sprint savedSprint = sprintApi.persist(sprint);
            ToolActivityContextHolder.reportActivity("created sprint '" + savedSprint.getName() + "' with ID: " + savedSprint.getId());
            SprintDto sprintDto = SprintDto.from(savedSprint);
            return jsonMapper.writeValueAsString(sprintDto);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error creating sprint: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Delete a sprint by ID (requires access or admin role). " +
            "IMPORTANT: You must provide the exact sprint ID. If you just created a sprint, use the 'id' field from the createSprint response. " +
            "Do NOT guess or use a different sprint's ID. " +
            "Returns: Success message (string) confirming deletion")
    public String deleteSprint(
            @ToolParam(description = "The sprintId") Long sprintId) {
        try {
            // First, get the sprint details to log what we're about to delete
            Sprint sprintToDelete = sprintApi.getById(sprintId);
            if (sprintToDelete != null) {
                ToolActivityContextHolder.reportActivity("Deleting sprint '" + sprintToDelete.getName() + "' (ID: " + sprintId + ")");
            } else {
                ToolActivityContextHolder.reportActivity("Attempting to delete sprint with ID: " + sprintId + " (sprint not found)");
            }

            sprintApi.deleteById(sprintId);
            ToolActivityContextHolder.reportActivity("Successfully deleted sprint with ID: " + sprintId);
            return "Sprint deleted successfully with ID: " + sprintId;
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error deleting sprint " + sprintId + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a list of all sprints accessible to the current user. " + RETURNS_SPRINT_ARRAY_JSON)
    public String getAllSprints() {
        try {
            List<Sprint> sprints = sprintApi.getAll();
            ToolActivityContextHolder.reportActivity("read " + sprints.size() + " sprints.");
            List<SprintDto> sprintDtos = sprints.stream()
                    .map(SprintDto::from)
                    .collect(Collectors.toList());
            return jsonMapper.writeValueAsString(sprintDtos);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error getting all sprints: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a list of all sprints for a feature (requires access or admin role). " + RETURNS_SPRINT_ARRAY_JSON)
    public String getAllSprintsByFeatureId(
            @ToolParam(description = "The featureId") Long featureId) {
        try {
            ToolActivityContextHolder.reportActivity("Getting all sprints for feature " + featureId);
            List<Sprint> sprints = sprintApi.getAll(featureId);
            ToolActivityContextHolder.reportActivity("Found " + sprints.size() + " sprints.");
            List<SprintDto> sprintDtos = sprints.stream()
                    .map(SprintDto::from)
                    .collect(Collectors.toList());
            return jsonMapper.writeValueAsString(sprintDtos);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error getting all sprints for feature " + featureId + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a specific sprint by its ID (requires access or admin role). " + RETURNS_SPRINT_JSON)
    public String getSprintById(
            @ToolParam(description = "The sprintId") Long sprintId) {
        try {
            ToolActivityContextHolder.reportActivity("Getting sprint with ID: " + sprintId);
            Sprint sprint = sprintApi.getById(sprintId);
            if (sprint != null) {
                SprintDto sprintDto = SprintDto.from(sprint);
                return jsonMapper.writeValueAsString(sprintDto);
            }
            return "Sprint not found with ID: " + sprintId;
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error getting sprint " + sprintId + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a specific sprint by its name within a feature. " + RETURNS_SPRINT_JSON)
    public String getSprintByName(
            @ToolParam(description = "The featureId the sprint belongs to") Long featureId,
            @ToolParam(description = "The sprint name") String name) {
        try {
            ToolActivityContextHolder.reportActivity("Getting sprint with name: " + name + " in feature " + featureId);
            return sprintApi.getByName(featureId, name)
                    .map(sprint -> {
                        try {
                            return jsonMapper.writeValueAsString(SprintDto.from(sprint));
                        } catch (Exception e) {
                            return "Error: " + e.getMessage();
                        }
                    })
                    .orElse("Sprint not found with name: " + name + " in feature " + featureId);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error getting sprint by name " + name + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Update an existing sprint (requires access or admin role). " + RETURNS_SPRINT_JSON)
    public String updateSprint(
            @ToolParam(description = "The sprintId") Long sprintId,
            @ToolParam(description = "The new sprint name") String name) {
        try {
            ToolActivityContextHolder.reportActivity("Updating sprint " + sprintId + " with name: " + name);
            Sprint sprint = sprintApi.getById(sprintId);
            if (sprint == null) {
                return "Sprint not found with ID: " + sprintId;
            }
            sprint.setName(name);
            sprintApi.update(sprint);
            SprintDto sprintDto = SprintDto.from(sprint);
            return jsonMapper.writeValueAsString(sprintDto);
        } catch (Exception e) {
            ToolActivityContextHolder.reportActivity("Error updating sprint " + sprintId + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }
}
