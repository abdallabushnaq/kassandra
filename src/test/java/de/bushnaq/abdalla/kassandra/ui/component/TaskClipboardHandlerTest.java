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

package de.bushnaq.abdalla.kassandra.ui.component;

import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.TaskMode;
import de.bushnaq.abdalla.kassandra.dto.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Method;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for TaskClipboardHandler serialization and deserialization functionality.
 * <p>
 * Tests the ability to serialize a story task with children to JSON and deserialize it back,
 * ensuring that the structure and data are preserved correctly.
 *
 * @author Abdalla Bushnaq
 * @version 1.0
 * @since 2025
 */
@Tag("UnitTest")
public class TaskClipboardHandlerTest {

    private TaskClipboardHandler clipboardHandler;
    private JsonMapper           jsonMapper;
    private TaskGrid             taskGrid;

    /**
     * Helper method to create a child task with time estimates.
     * A child task is a regular task with min and max estimates.
     *
     * @param name        The name of the task
     * @param minEstimate The minimum time estimate
     * @return A new child task
     */
    private Task createChildTask(String name, Duration minEstimate) {
        Task task = new Task();
        task.setName(name);
        task.setMilestone(false);
        task.setMinEstimate(minEstimate);
        task.setMaxEstimate(minEstimate.multipliedBy(2)); // Max is 2x min for testing
        task.setRemainingEstimate(minEstimate);
        task.setTaskMode(TaskMode.AUTO_SCHEDULED);
        task.setTaskStatus(TaskStatus.TODO);
        task.setProgress(0);
        return task;
    }

    /**
     * Helper method to create a story task.
     * A story is a task without time estimates.
     *
     * @return A new story task
     */
    private Task createStoryTask() {
        Task story = new Task();
        story.setName("Test Story");
        story.setMilestone(false);
        story.setMinEstimate(Duration.ZERO); // Stories have no estimates
        story.setMaxEstimate(Duration.ZERO);
        story.setTaskMode(TaskMode.AUTO_SCHEDULED);
        story.setTaskStatus(TaskStatus.TODO);
        return story;
    }

    /**
     * Helper method to invoke private methods using reflection for testing purposes.
     *
     * @param obj        The object to invoke the method on
     * @param methodName The name of the private method
     * @param paramType  The parameter type of the method
     * @param param      The parameter value
     * @param <T>        The return type
     * @return The result of the method invocation
     * @throws Exception if the method invocation fails
     */
    @SuppressWarnings("unchecked")
    private <T> T invokePrivateMethod(Object obj, String methodName, Class<?> paramType, Object param) throws Exception {
        Method method = obj.getClass().getDeclaredMethod(methodName, paramType);
        method.setAccessible(true);
        return (T) method.invoke(obj, param);
    }

    @BeforeEach
    public void setUp() {
        jsonMapper = JsonMapper.builder().build();
        // We need to create a mock TaskGrid but we can use null for this unit test
        // since we're only testing serialization/deserialization which doesn't use the grid
        taskGrid         = null;
        clipboardHandler = new TaskClipboardHandler(taskGrid, jsonMapper);
    }

    /**
     * Test that a simple task (not a story) can be serialized and deserialized correctly.
     *
     * @throws Exception if serialization or deserialization fails
     */
    @Test
    public void testSerializeAndDeserializeSimpleTask() throws Exception {
        // Create a simple task
        Task task = createChildTask("Simple Task", Duration.ofHours(40));

        // Serialize the task
        String json = invokePrivateMethod(clipboardHandler, "serializeTask", Task.class, task);
        assertNotNull(json, "Serialized JSON should not be null");
        assertFalse(json.isEmpty(), "Serialized JSON should not be empty");

        // Deserialize the task
        Task deserializedTask = invokePrivateMethod(clipboardHandler, "deserializeTask", String.class, json);
        assertNotNull(deserializedTask, "Deserialized task should not be null");

        // Verify properties
        assertEquals(task.getName(), deserializedTask.getName(), "Task name should match");
        assertEquals(task.getMinEstimate(), deserializedTask.getMinEstimate(), "Task min estimate should match");
        assertEquals(task.getMaxEstimate(), deserializedTask.getMaxEstimate(), "Task max estimate should match");
        assertEquals(task.getTaskStatus(), deserializedTask.getTaskStatus(), "Task status should match");
        assertTrue(deserializedTask.isTask(), "Deserialized task should be a task");
    }

    /**
     * Test that a story task with 3 children can be serialized to JSON and deserialized back
     * with all data preserved correctly.
     *
     * @throws Exception if serialization or deserialization fails
     */
    @Test
    public void testSerializeAndDeserializeStoryWithChildren() throws Exception {
        // 1. Create a story task with 3 children
        Task story  = createStoryTask();
        Task child1 = createChildTask("Child Task 1", Duration.ofHours(8));
        Task child2 = createChildTask("Child Task 2", Duration.ofHours(16));
        Task child3 = createChildTask("Child Task 3", Duration.ofHours(24));

        // Add children to story
        story.addChildTask(child1);
        story.addChildTask(child2);
        story.addChildTask(child3);

        // 2. Serialize the story
        String json = invokePrivateMethod(clipboardHandler, "serializeTask", Task.class, story);
        assertNotNull(json, "Serialized JSON should not be null");
        assertFalse(json.isEmpty(), "Serialized JSON should not be empty");

        // Debug: Print the JSON to understand its structure
        System.out.println("Serialized JSON:");
        System.out.println(json);

        // Verify JSON structure contains story and children
        assertTrue(json.contains("\"story\""), "JSON should contain 'story' key");
        assertTrue(json.contains("\"children\""), "JSON should contain 'children' key");
        assertTrue(json.contains("Child Task 1"), "JSON should contain child task 1 name");
        assertTrue(json.contains("Child Task 2"), "JSON should contain child task 2 name");
        assertTrue(json.contains("Child Task 3"), "JSON should contain child task 3 name");

        // 3. Deserialize the story
        Task deserializedStory = invokePrivateMethod(clipboardHandler, "deserializeTask", String.class, json);
        assertNotNull(deserializedStory, "Deserialized story should not be null");

        // Debug: Print deserialized story details
        System.out.println("Deserialized story name: " + deserializedStory.getName());
        System.out.println("Deserialized story is story: " + deserializedStory.isStory());
        System.out.println("Deserialized story child count: " + deserializedStory.getChildTasks().size());
        if (!deserializedStory.getChildTasks().isEmpty()) {
            System.out.println("First child name: " + deserializedStory.getChildTasks().get(0).getName());
        }

        // 4. Verify the result matches the original story with child tasks
        // Verify story properties
        assertEquals(story.getName(), deserializedStory.getName(), "Story name should match");
        assertEquals(story.isMilestone(), deserializedStory.isMilestone(), "Story milestone flag should match");
        assertEquals(story.getTaskStatus(), deserializedStory.getTaskStatus(), "Story task status should match");
        assertTrue(deserializedStory.isStory(), "Deserialized task should be a story");

        // Verify child tasks
        assertEquals(3, deserializedStory.getChildTasks().size(), "Story should have 3 child tasks");

        // Verify first child
        Task deserializedChild1 = deserializedStory.getChildTasks().get(0);
        assertEquals(child1.getName(), deserializedChild1.getName(), "Child 1 name should match");
        assertEquals(child1.getMinEstimate(), deserializedChild1.getMinEstimate(), "Child 1 min estimate should match");
        assertEquals(child1.getMaxEstimate(), deserializedChild1.getMaxEstimate(), "Child 1 max estimate should match");
        assertEquals(child1.getTaskStatus(), deserializedChild1.getTaskStatus(), "Child 1 task status should match");
        assertTrue(deserializedChild1.isTask(), "Child 1 should be a task");

        // Verify second child
        Task deserializedChild2 = deserializedStory.getChildTasks().get(1);
        assertEquals(child2.getName(), deserializedChild2.getName(), "Child 2 name should match");
        assertEquals(child2.getMinEstimate(), deserializedChild2.getMinEstimate(), "Child 2 min estimate should match");
        assertEquals(child2.getMaxEstimate(), deserializedChild2.getMaxEstimate(), "Child 2 max estimate should match");
        assertEquals(child2.getTaskStatus(), deserializedChild2.getTaskStatus(), "Child 2 task status should match");
        assertTrue(deserializedChild2.isTask(), "Child 2 should be a task");

        // Verify third child
        Task deserializedChild3 = deserializedStory.getChildTasks().get(2);
        assertEquals(child3.getName(), deserializedChild3.getName(), "Child 3 name should match");
        assertEquals(child3.getMinEstimate(), deserializedChild3.getMinEstimate(), "Child 3 min estimate should match");
        assertEquals(child3.getMaxEstimate(), deserializedChild3.getMaxEstimate(), "Child 3 max estimate should match");
        assertEquals(child3.getTaskStatus(), deserializedChild3.getTaskStatus(), "Child 3 task status should match");
        assertTrue(deserializedChild3.isTask(), "Child 3 should be a task");
    }
}
