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

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    @Qualifier("aiSprintApi")
    private SprintApi sprintApi;

    @Tool(description = "Create a new sprint for a feature.")
    public String createSprint(
            @ToolParam(description = "Unique sprint name") String name,
            @ToolParam(description = "The featureId this sprint belongs to") Long featureId) {
        try {
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

    @Tool(description = "Delete a sprint by its sprintId.")
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

    @Tool(description = "Get all sprints accessible to the current user.")
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

    @Tool(description = "Get all sprints for a feature.")
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

    @Tool(description = "Get a sprint by its sprintId.")
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

    @Tool(description = "Get a sprint by name within a feature.")
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

    @Tool(description = "Update a sprint name by its sprintId.")
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
