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

import de.bushnaq.abdalla.kassandra.ai.mcp.KassandraToolCallResultConverter;
import de.bushnaq.abdalla.kassandra.ai.mcp.ToolActivityContextHolder;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.rest.api.SprintApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

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
    @Qualifier("aiSprintApi")
    private SprintApi sprintApi;

    @Tool(description = "Create a new sprint for a feature.", resultConverter = KassandraToolCallResultConverter.class)
    public SprintDto createSprint(
            @ToolParam(description = "Unique sprint name") String name,
            @ToolParam(description = "The featureId this sprint belongs to") Long featureId) {
        Sprint sprint = new Sprint();
        sprint.setName(name);
        sprint.setFeatureId(featureId);
        Sprint savedSprint = sprintApi.persist(sprint);
        ToolActivityContextHolder.reportActivity("created sprint '" + savedSprint.getName() + "' with ID: " + savedSprint.getId());
        return SprintDto.from(savedSprint);
    }

    @Tool(description = "Delete a sprint by its sprintId.", resultConverter = KassandraToolCallResultConverter.class)
    public void deleteSprint(
            @ToolParam(description = "The sprintId") Long sprintId) {
        Sprint sprint = sprintApi.getById(sprintId);
        if (sprint == null) {
            throw new IllegalArgumentException("Sprint not found with ID: " + sprintId);
        }
        ToolActivityContextHolder.reportActivity("Deleting sprint '" + sprint.getName() + "' (ID: " + sprintId + ")");
        sprintApi.deleteById(sprintId);
        ToolActivityContextHolder.reportActivity("Deleted sprint '" + sprint.getName() + "' (ID: " + sprintId + ")");
    }

    @Tool(description = "Get all sprints accessible to the current user.", resultConverter = KassandraToolCallResultConverter.class)
    public List<SprintDto> getAllSprints() {
        List<Sprint> sprints = sprintApi.getAll();
        ToolActivityContextHolder.reportActivity("read " + sprints.size() + " sprints.");
        return sprints.stream().map(SprintDto::from).collect(Collectors.toList());
    }

    @Tool(description = "Get all sprints for a feature.", resultConverter = KassandraToolCallResultConverter.class)
    public List<SprintDto> getAllSprintsByFeatureId(
            @ToolParam(description = "The featureId") Long featureId) {
        List<Sprint> sprints = sprintApi.getAll(featureId);
        ToolActivityContextHolder.reportActivity("Found " + sprints.size() + " sprints for feature " + featureId + ".");
        return sprints.stream().map(SprintDto::from).collect(Collectors.toList());
    }

    @Tool(description = "Get a sprint by its sprintId.", resultConverter = KassandraToolCallResultConverter.class)
    public SprintDto getSprintById(
            @ToolParam(description = "The sprintId") Long sprintId) {
        Sprint sprint = sprintApi.getById(sprintId);
        if (sprint == null) {
            throw new IllegalArgumentException("Sprint not found with ID: " + sprintId);
        }
        return SprintDto.from(sprint);
    }

    @Tool(description = "Get a sprint by name within a feature.", resultConverter = KassandraToolCallResultConverter.class)
    public SprintDto getSprintByName(
            @ToolParam(description = "The featureId the sprint belongs to") Long featureId,
            @ToolParam(description = "The sprint name") String name) {
        return sprintApi.getByName(featureId, name)
                .map(SprintDto::from)
                .orElseThrow(() -> new IllegalArgumentException("Sprint '" + name + "' not found in feature " + featureId));
    }

    @Tool(description = "Update a sprint name by its sprintId.", resultConverter = KassandraToolCallResultConverter.class)
    public SprintDto updateSprint(
            @ToolParam(description = "The sprintId") Long sprintId,
            @ToolParam(description = "The new sprint name") String name) {
        Sprint sprint = sprintApi.getById(sprintId);
        if (sprint == null) {
            throw new IllegalArgumentException("Sprint not found with ID: " + sprintId);
        }
        ToolActivityContextHolder.reportActivity("Updating sprint " + sprintId + " with name: " + name);
        sprint.setName(name);
        sprintApi.update(sprint);
        return SprintDto.from(sprint);
    }
}
