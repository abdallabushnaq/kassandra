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

import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import de.bushnaq.abdalla.kassandra.dto.Task;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Coordinates drag and drop operations between multiple TaskGrid instances.
 * Enables cross-grid task transfers (e.g., moving tasks from Sprint to Backlog or vice versa).
 * <p>
 * This coordinator manages:
 * - Registration of participating grids
 * - Tracking of current drag state across grids
 * - Notification of drag events to all registered grids
 * - Handling of cross-grid drop events
 */
@Log4j2
public class CrossGridDragDropCoordinator {

    /**
     * The current drag mode ("reorder" or "dependency").
     */
    @Getter
    private String currentDragMode;
    /**
     * The grid where the current drag operation started.
     */
    @Getter
    private TaskGrid currentDragSource;
    /**
     * The task currently being dragged.
     */
    @Getter
    private Task currentDraggedTask;
    /**
     * Callback invoked when a cross-grid transfer occurs.
     */
    private final Consumer<CrossGridTransferEvent> onCrossGridTransfer;
    /**
     * All grids registered with this coordinator.
     */
    private final List<TaskGrid> registeredGrids = new ArrayList<>();

    /**
     * Creates a new coordinator with the specified transfer handler.
     *
     * @param onCrossGridTransfer Callback to handle cross-grid transfers
     */
    public CrossGridDragDropCoordinator(Consumer<CrossGridTransferEvent> onCrossGridTransfer) {
        this.onCrossGridTransfer = onCrossGridTransfer;
    }

    /**
     * Get the number of registered grids.
     *
     * @return The count of registered grids
     */
    public int getRegisteredGridCount() {
        return registeredGrids.size();
    }

    /**
     * Handle a cross-grid drop event.
     * Validates the drop and invokes the transfer handler if valid.
     *
     * @param targetGrid   The grid where the drop occurred
     * @param dropTarget   The task that was dropped on/near
     * @param dropLocation Whether the drop was above or below the target
     */
    public void handleCrossGridDrop(TaskGrid targetGrid, Task dropTarget, GridDropLocation dropLocation) {
        if (currentDragSource == null || currentDraggedTask == null) {
            log.warn("Cross-grid drop attempted but no active drag state");
            return;
        }

        if (currentDragSource == targetGrid) {
            log.debug("Drop on same grid - not a cross-grid operation");
            return;
        }

        if (!isCrossGridDropAllowed()) {
            log.info("Cross-grid drop not allowed in {} mode", currentDragMode);
            return;
        }

        log.info("Handling cross-grid drop: task={} from grid to grid, dropTarget={}, location={}",
                currentDraggedTask.getKey(),
                dropTarget != null ? dropTarget.getKey() : "null",
                dropLocation);

        // Collect children if a story is being moved
        List<Task> childTasks = new ArrayList<>();
        if (currentDraggedTask.isStory() && currentDraggedTask.getChildTasks() != null) {
            childTasks.addAll(currentDraggedTask.getChildTasks());
        }

        // Invoke the transfer handler
        if (onCrossGridTransfer != null) {
            onCrossGridTransfer.accept(new CrossGridTransferEvent(
                    currentDraggedTask,
                    childTasks,
                    currentDragSource,
                    targetGrid,
                    dropTarget,
                    dropLocation
            ));
        }
    }

    /**
     * Check if the current drag operation is a cross-grid drag relative to the target grid.
     *
     * @param targetGrid The grid to check against
     * @return true if there is an active drag from a different grid
     */
    public boolean isCrossGridDrag(TaskGrid targetGrid) {
        return currentDragSource != null && currentDragSource != targetGrid;
    }

    /**
     * Check if cross-grid drops should be allowed for the current drag mode.
     * Only reorder mode supports cross-grid transfers.
     *
     * @return true if cross-grid drops are allowed
     */
    public boolean isCrossGridDropAllowed() {
        // Only allow cross-grid drops in reorder mode
        // Dependency mode doesn't make sense across sprints
        return "reorder".equals(currentDragMode);
    }

    /**
     * Notify the coordinator that a drag operation has ended.
     * This will clear the drag state and notify all other registered grids.
     *
     * @param sourceGrid The grid where the drag ended
     */
    public void notifyDragEnd(TaskGrid sourceGrid) {
        log.debug("Cross-grid drag ended: source={}", sourceGrid);

        // Notify other grids that the external drag has ended
        for (TaskGrid grid : registeredGrids) {
            if (grid != sourceGrid) {
                grid.onExternalDragEnd();
            }
        }

        // Clear state
        this.currentDragSource  = null;
        this.currentDraggedTask = null;
        this.currentDragMode    = null;
    }

    /**
     * Notify the coordinator that a drag operation has started.
     * This will propagate the notification to all other registered grids.
     *
     * @param sourceGrid The grid where the drag started
     * @param task       The task being dragged
     * @param mode       The drag mode ("reorder" or "dependency")
     */
    public void notifyDragStart(TaskGrid sourceGrid, Task task, String mode) {
        this.currentDragSource  = sourceGrid;
        this.currentDraggedTask = task;
        this.currentDragMode    = mode;

        log.debug("Cross-grid drag started: task={}, mode={}, source={}",
                task != null ? task.getKey() : "null", mode, sourceGrid);

        // Notify other grids about the external drag
        for (TaskGrid grid : registeredGrids) {
            if (grid != sourceGrid) {
                grid.onExternalDragStart(task, sourceGrid, mode);
            }
        }
    }

    /**
     * Register a TaskGrid with this coordinator.
     * The grid will receive notifications about drag events from other registered grids.
     *
     * @param grid The grid to register
     */
    public void register(TaskGrid grid) {
        if (!registeredGrids.contains(grid)) {
            registeredGrids.add(grid);
            log.debug("Registered grid for cross-grid drag & drop. Total registered: {}", registeredGrids.size());
        }
    }

    /**
     * Unregister a TaskGrid from this coordinator.
     *
     * @param grid The grid to unregister
     */
    public void unregister(TaskGrid grid) {
        registeredGrids.remove(grid);
        log.debug("Unregistered grid from cross-grid drag & drop. Total registered: {}", registeredGrids.size());
    }

    /**
     * Event data for cross-grid task transfers.
     *
     * @param task           The task being transferred
     * @param childTasks     Child tasks to move along (when moving a story)
     * @param sourceGrid     The grid where the drag originated
     * @param targetGrid     The grid where the drop occurred
     * @param dropTargetTask The task that was dropped on/near
     * @param dropLocation   Whether the drop was above or below the target
     */
    public record CrossGridTransferEvent(
            Task task,
            List<Task> childTasks,
            TaskGrid sourceGrid,
            TaskGrid targetGrid,
            Task dropTargetTask,
            GridDropLocation dropLocation
    ) {
    }
}
