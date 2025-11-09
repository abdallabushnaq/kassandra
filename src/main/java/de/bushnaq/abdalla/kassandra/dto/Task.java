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

package de.bushnaq.abdalla.kassandra.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.bushnaq.abdalla.util.DurationDeserializer;
import de.bushnaq.abdalla.util.DurationSerializer;
import lombok.*;
import net.sf.mpxj.ProjectCalendar;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a task in a Gantt chart with scheduling, resource allocation, and progress tracking capabilities.
 * <p>
 * A Task can be:
 * <ul>
 *     <li>A milestone - a zero-duration marker in the project timeline</li>
 *     <li>A story - a task without time estimates that contains other tasks</li>
 *     <li>A task - a work item with min/max time estimates and tracking</li>
 * </ul>
 * <p>
 * Tasks support hierarchical structures (parent-child relationships), dependencies (predecessor relationships),
 * and resource assignments. They can be scheduled automatically or manually based on the task mode.
 * <p>
 * This class uses Lombok annotations for boilerplate code generation and Jackson annotations for JSON serialization.
 *
 * @author Abdalla Bushnaq
 * @version 1.0
 * @since 2025
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@ToString(callSuper = false)
public class Task implements Comparable<Task> {

    /**
     * List of child tasks in the task hierarchy. ONly used for Stories
     */
    @JsonIgnore
    private List<Task> childTasks = new ArrayList<>();

    /**
     * Indicates whether this task is on the critical path of the project.
     */
    private boolean critical = false;

    /**
     * The total duration of the task. Depends on the min estimate and the availability of the assigned resource.
     */
    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    private Duration duration;

    /**
     * The scheduled finish date and time of the task.
     */
    private LocalDateTime finish;

    /**
     * The unique identifier of the task. Unique within all tasks of the server.
     */
    private Long id;

    /**
     * Indicates whether this task should be included in cost calculations.
     */
    private boolean impactOnCost = true;

    /**
     * The maximum estimated person days for the task.
     */
    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    private Duration maxEstimate = Duration.ZERO;

    /**
     * Indicates whether this task is a milestone.
     */
    private boolean milestone;

    /**
     * The minimum estimated person days for the task.
     */
    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    private Duration minEstimate = Duration.ZERO;

    /**
     * The name of the task.
     */
    private String name;

    /**
     * Additional notes or comments about the task.
     */
    @JsonIgnore
    private String notes;

    /**
     * Zeero based order identifier for sorting tasks. Only unique within one sprint.
     */
    private Integer orderId = -1;

    /**
     * The original estimated person days when the task was created.
     */
    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    private Duration originalEstimate = Duration.ZERO;

    /**
     * Reference to the parent task in the hierarchy. Parent tasks are always Stories.
     */
    @JsonIgnore
    @ToString.Exclude//help intellij debugger not to go into a loop
    private Task parentTask;

    /**
     * The ID of the parent task. Parent tasks are always Stories.
     */
    private Long parentTaskId;

    /**
     * List of predecessor tasks that this task depends on. This task can only start when all predecessors are completed.
     */
    private List<Relation> predecessors = new ArrayList<>();

    /**
     * The completion progress of the task (0-1).
     */
    private Number progress = 0;

    /**
     * The estimated person days remaining to complete the task.
     */
    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    private Duration remainingEstimate = Duration.ZERO;

    /**
     * The ID of the resource (user) assigned to this task. Only one resource can be assigned to a task.
     */
    private Long resourceId;

    /**
     * Reference to the sprint this task belongs to.
     */
    @JsonIgnore
    @ToString.Exclude//help intellij debugger not to go into a loop
    private Sprint sprint;

    /**
     * The ID of the sprint this task belongs to.
     */
    private Long sprintId;

    /**
     * The scheduled start date and time of the task.
     */
    private LocalDateTime start;

    /**
     * The scheduling mode of the task (auto-scheduled or manually scheduled). Only milestones can be manually scheduled.
     */
    private TaskMode taskMode = TaskMode.AUTO_SCHEDULED;

    /**
     * The total person days already spent on this task.
     */
    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    private Duration timeSpent = Duration.ZERO;

    /**
     * List of work log entries recording time spent on this task. Worklogs represent time a resource spent on this task at one time.
     */
    @JsonIgnore
    private List<Worklog> worklogs = new ArrayList<>();

    /**
     * Adds a child task to this task's hierarchy.
     * If the child task already has a parent, it will be removed from that parent first.
     * This method also sets the bidirectional relationship between parent and child.
     *
     * @param childTask the task to add as a child
     */
    public void addChildTask(Task childTask) {
        if (childTask.getParentTask() != null) {
            childTask.getParentTask().removeChildTask(childTask);
        }
        childTask.setParentTask(this);
        childTask.setParentTaskId(this.getId());
        childTasks.add(childTask);
    }

    /**
     * Adds a predecessor task dependency to this task.
     * The predecessor must be completed before this task can start.
     *
     * @param dependency the task that this task depends on
     * @param isVisible  whether the dependency relationship should be visible in the UI
     */
    public void addPredecessor(Task dependency, boolean isVisible) {
        predecessors.add(new Relation(dependency, isVisible));
    }

    /**
     * Adds time spent to the total time spent on this task.
     * If the duration is null, no action is taken.
     *
     * @param w the duration to add to the time spent
     */
    public void addTimeSpent(Duration w) {
        if (w != null) {
            timeSpent = timeSpent.plus(w);
        }
    }

    /**
     * Adds a work log entry to this task.
     * The work log's task ID will be set to this task's ID.
     *
     * @param worklog the work log entry to add
     */
    public void addWorklog(Worklog worklog) {
        worklog.setTaskId(id);
        worklogs.add(worklog);
    }

    /**
     * Compares this task to another task based on their IDs.
     *
     * @param other the task to compare to
     * @return a negative integer, zero, or a positive integer as this task's ID
     * is less than, equal to, or greater than the other task's ID
     */
    @Override
    public int compareTo(Task other) {
        return this.id.compareTo(other.id);
    }

    /**
     * Gets the user assigned to this task based on the resource ID.
     *
     * @return the assigned User, or null if no resource is assigned
     */
    @JsonIgnore
    public User getAssignedUser() {
        return sprint.getuser(resourceId);
    }

    /**
     * Gets the effective calendar for this task.
     * If a user is assigned, returns the user's calendar; otherwise returns the sprint's calendar.
     *
     * @return the ProjectCalendar to use for scheduling this task
     */
    @JsonIgnore
    public ProjectCalendar getEffectiveCalendar() {
        User user = getAssignedUser();
        if (user != null) {
            return user.getCalendar();
        } else {
            return sprint.getCalendar();
        }
    }

    /**
     * Calculate the hierarchy depth of a task (how many parent levels it has)
     */
    @JsonIgnore
    public int getHierarchyDepth() {
        int  depth   = 0;
        Task current = this;
        while (current.getParentTask() != null) {
            depth++;
            current = current.getParentTask();
        }
        return depth;
    }

    /**
     * Gets the task's key in the format "T-{id}".
     *
     * @return the task key string
     */
    @JsonIgnore
    public String getKey() {
        return "T-" + id;
    }

    /**
     * Initializes the task. This method is currently empty but can be overridden or
     * extended for initialization logic.
     */
    public void initialize() {
    }

    /**
     * Checks if the specified story is an ancestor of this task in the task hierarchy.
     * A task considered its own descendant.
     *
     * @param story the story to check
     * @return true if the specified story is an ancestor of this task, false otherwise
     */
    public boolean isAncestorOf(Task story) {
        if (Objects.equals(story.getId(), getId()))
            return true;
        for (Task childTask : childTasks) {
            if (childTask.isAncestorOf(story))
                return true;
        }
        return false;
    }

    /**
     * Checks if the specified task is a descendant of this story in the task hierarchy.
     * A task is considered its own descendant.
     *
     * @param task the task to check
     * @return true if the specified task is a descendant of this story, false otherwise
     */
    public boolean isDescendantOf(Task task) {
        if (Objects.equals(task.getId(), getId()))
            return true;
        for (Task childTask : task.childTasks) {
            if (childTask.isDescendantOf(childTask))
                return true;
        }
        return false;
    }

    /**
     * Determines if this task is a story.
     * A story is a non-milestone task without time estimates (minEstimate is null or zero).
     *
     * @return true if this is a story, false otherwise
     */
    @JsonIgnore
    public boolean isStory() {
        return !isMilestone() && (getMinEstimate() == null || getMinEstimate().isZero());
    }

    /**
     * Determines if this task is a regular task.
     * A regular task is a non-milestone task with a non-zero minEstimate.
     *
     * @return true if this is a regular task, false otherwise
     */
    @JsonIgnore
    public boolean isTask() {
        return !isMilestone() && getMinEstimate() != null && !getMinEstimate().isZero();
    }

    /**
     * Recalculates the progress percentage based on time spent and remaining estimate.
     * Progress is calculated as: timeSpent / (timeSpent + remainingEstimate).
     */
    public void recalculate() {
        double fraction = ((double) getTimeSpent().toSeconds()) / getTimeSpent().plus(getRemainingEstimate()).toSeconds();
        setProgress(fraction);
    }

    /**
     * Removes a child task from this task's hierarchy.
     * This method also clears the child's parent reference.
     *
     * @param childTask the child task to remove
     */
    public void removeChildTask(Task childTask) {
        childTasks.remove(childTask);
        childTask.setParentTask(null);
        childTask.setParentTaskId(null);
    }

    /**
     * Subtracts time from the remaining estimate.
     * If the duration is null, no action is taken.
     *
     * @param w the duration to subtract from the remaining estimate
     */
    public void removeRemainingEstimate(Duration w) {
        if (w != null) {
            remainingEstimate = remainingEstimate.minus(w);
        }
    }
}
