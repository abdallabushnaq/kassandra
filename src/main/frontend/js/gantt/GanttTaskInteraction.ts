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
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

import {ChartHandle} from '../InteractiveTimelineChart.js';
import {Theme} from '../theme/Theme.js';
import {DateUtils} from '../DateUtils.js';
import {GanttRenderer, GanttTaskSelection} from './GanttRenderer.js';
import {TaskDto} from './dto/TaskDto.js';

/** Read-only task inspection shared by embedded and detached Gantt charts. */
export class GanttTaskInteraction {
    readonly viewport = document.createElement('div');
    private readonly layout = document.createElement('div');
    private readonly panel = document.createElement('aside');
    private readonly tasksById = new Map<string, TaskDto>();
    private readonly resizeObserver: ResizeObserver;
    private selection: GanttTaskSelection | null = null;
    private chart: ChartHandle | null = null;
    private focusedTaskId: string | null = null;

    constructor(container: HTMLElement, private readonly renderer: GanttRenderer, theme: Theme) {
        for (const task of renderer.tasks)
            this.tasksById.set(String(task.id), task);
        this.layout.className = 'gantt-inspection-layout';
        this.layout.dataset.theme = theme.themeVariance;
        this.viewport.className = 'gantt-inspection-viewport';
        this.viewport.tabIndex = 0;
        this.viewport.setAttribute('aria-label', 'Interactive Gantt chart');
        this.panel.className = 'gantt-task-details';
        this.panel.setAttribute('aria-label', 'Task details');
        this.panel.setAttribute('aria-live', 'polite');
        this.panel.hidden = true;
        const style = document.createElement('style');
        style.textContent = `
            .gantt-inspection-layout {
                display: grid; grid-template-columns: minmax(0, 1fr); align-items: start;
                gap: 12px; min-width: 0; width: 100%; color: #172033;
                --inspector-bg: #f8fafc; --inspector-border: #cbd5e1;
            }
            .gantt-inspection-layout[data-theme="dark"] {
                color: #f1f5f9; --inspector-bg: #1e293b; --inspector-border: #64748b;
            }
            .gantt-inspection-layout.has-selection:not(.is-narrow) {
                grid-template-columns: minmax(0, 1fr) 320px;
            }
            .gantt-inspection-viewport { min-width: 0; position: relative; }
            .gantt-task-details {
                box-sizing: border-box; padding: 14px; border: 1px solid var(--inspector-border);
                border-radius: 6px; background: var(--inspector-bg); color: inherit;
                font: 13px/1.5 sans-serif; overflow: auto; overflow-wrap: anywhere;
                max-height: min(70vh, 640px); position: sticky; top: 8px;
            }
            .gantt-task-details[hidden] { display: none; }
            .gantt-inspection-layout.is-narrow .gantt-task-details { position: static; }
            .gantt-task-details header { display: flex; align-items: start; gap: 8px; }
            .gantt-task-details h3 { margin: 0; flex: 1; font-size: 15px; }
            .gantt-task-details button {
                background: transparent; color: inherit; border: 1px solid var(--inspector-border);
                border-radius: 4px; cursor: pointer; padding: 3px 8px; font: inherit;
            }
            .gantt-task-details dt { font-weight: bold; margin-top: 8px; }
            .gantt-task-details dd { margin: 0; white-space: pre-wrap; }
            .gantt-task-details ul { padding-left: 20px; }
        `;
        this.layout.append(style, this.viewport, this.panel);
        container.replaceChildren(this.layout);
        this.layout.addEventListener('keydown', this.handleKeyDown);
        this.layout.addEventListener('focusin', this.handleFocusIn);
        this.layout.addEventListener('focusout', this.handleFocusOut);
        this.resizeObserver = new ResizeObserver(() => {
            this.layout.classList.toggle('is-narrow', container.clientWidth < 900);
        });
        this.resizeObserver.observe(container);
        this.layout.classList.toggle('is-narrow', container.clientWidth < 900);
    }

    attach(chart: ChartHandle): ChartHandle {
        this.chart = chart;
        let destroyed = false;
        return {
            render: () => chart.render(),
            schedule: () => chart.schedule(),
            destroy: () => {
                if (destroyed)
                    return;
                destroyed = true;
                this.resizeObserver.disconnect();
                this.layout.removeEventListener('keydown', this.handleKeyDown);
                this.layout.removeEventListener('focusin', this.handleFocusIn);
                this.layout.removeEventListener('focusout', this.handleFocusOut);
                chart.destroy();
                this.chart = null;
                this.layout.remove();
            },
        };
    }

    activate(target: Element | null): void {
        const task = target?.closest('[data-task-id]');
        if (task) {
            const id = task.getAttribute('data-task-id')!;
            this.focusedTaskId = id;
            this.selectTask(id);
        } else if (!target?.closest('.task-dependency')) {
            this.focusedTaskId = null;
            this.selectTask(null);
        }
    }

    afterRender(): void {
        if (this.focusedTaskId && (document.activeElement === document.body || document.activeElement === this.viewport)) {
            const task = Array.from(this.viewport.querySelectorAll<SVGGElement>('[data-task-id]'))
                .find(group => group.getAttribute('data-task-id') === this.focusedTaskId);
            task?.focus({preventScroll: true});
        }
    }

    private selectTask(id: string | null): void {
        if (id === (this.selection?.taskId ?? null))
            return;
        if (id === null) {
            if (this.panel.contains(document.activeElement))
                this.viewport.focus({preventScroll: true});
            this.selection = null;
            this.panel.hidden = true;
            this.panel.replaceChildren();
            delete this.layout.dataset.selectedTaskId;
        } else {
            const task = this.tasksById.get(id);
            if (!task)
                throw new Error(`Selected Gantt task is unavailable: ${id}`);
            this.selection = this.findPredecessors(task);
            this.layout.dataset.selectedTaskId = id;
            this.renderDetails(task, this.selection);
            this.panel.hidden = false;
        }
        this.layout.classList.toggle('has-selection', this.selection !== null);
        this.renderer.setTaskSelection(this.selection);
        this.chart?.schedule();
    }

    private findPredecessors(task: TaskDto): GanttTaskSelection {
        const predecessorIds = new Set<string>();
        for (const relation of task.predecessors ?? [])
            if (relation.visible)
                predecessorIds.add(String(relation.predecessorId));
        return {taskId: String(task.id), predecessorIds};
    }

    private renderDetails(task: TaskDto, selection: GanttTaskSelection): void {
        const header = document.createElement('header');
        const title = document.createElement('h3');
        title.textContent = [task.key, task.name].filter(Boolean).join(' ') || 'Task';
        const close = document.createElement('button');
        close.type = 'button';
        close.textContent = 'Close';
        close.setAttribute('aria-label', 'Close task details');
        close.addEventListener('click', () => {
            this.focusedTaskId = null;
            this.selectTask(null);
        });
        header.append(title, close);
        const fields = document.createElement('dl');
        const detail = (label: string, value: string | null | undefined): void => {
            const term = document.createElement('dt');
            term.textContent = label;
            const description = document.createElement('dd');
            description.textContent = value || 'Unavailable';
            fields.append(term, description);
        };
        detail('Type', task.story ? 'Story' : task.milestone ? 'Milestone' : 'Task');
        detail('Start', task.start ? DateUtils.toLocalYMDHMString(task.start, this.renderer.dateTimeFormat) : null);
        detail('Finish', task.finish ? DateUtils.toLocalYMDHMString(task.finish, this.renderer.dateTimeFormat) : null);
        detail('Assignee', task.assignedUserName);
        detail('Availability', task.assignedUserAvailability);
        detail('Location', [task.assignedUserCountry, task.assignedUserState].filter(Boolean).join(', '));
        detail('Progress', task.progress == null ? null : `${Math.round(task.progress * 100)}%`);
        detail('Critical', task.critical == null ? null : task.critical ? 'Yes' : 'No');
        detail('Manually scheduled', task.manuallyScheduled == null ? null : task.manuallyScheduled ? 'Yes' : 'No');
        detail('Notes', task.notes);
        const heading = document.createElement('h4');
        heading.textContent = `Direct visible predecessors (${selection.predecessorIds.size})`;
        const list = document.createElement('ul');
        for (const id of selection.predecessorIds) {
            const predecessor = this.tasksById.get(id);
            const item = document.createElement('li');
            item.dataset.predecessorId = id;
            const label = predecessor
                ? [predecessor.key, predecessor.name].filter(Boolean).join(' ') || id
                : `Unavailable task (${id})`;
            item.textContent = label;
            list.appendChild(item);
        }
        const note = document.createElement('p');
        note.textContent = selection.predecessorIds.size
            ? 'Includes direct predecessors outside the current view. Hidden resource dependencies are excluded.'
            : 'No direct visible predecessors. Hidden resource dependencies are excluded.';
        this.panel.replaceChildren(header, fields, heading, list, note);
    }

    private handleKeyDown = (event: KeyboardEvent): void => {
        if (event.key === 'Escape' && this.selection) {
            event.preventDefault();
            event.stopPropagation();
            this.selectTask(null);
        } else if ((event.key === 'Enter' || event.key === ' ') && !event.repeat
            && event.target instanceof Element && event.target.closest('[data-task-id]')) {
            event.preventDefault();
            this.activate(event.target);
        }
    };

    private handleFocusIn = (event: FocusEvent): void => {
        this.focusedTaskId = event.target instanceof Element
            ? event.target.closest('[data-task-id]')?.getAttribute('data-task-id') ?? null
            : null;
    };

    private handleFocusOut = (event: FocusEvent): void => {
        if (event.relatedTarget instanceof Node && !this.layout.contains(event.relatedTarget))
            this.focusedTaskId = null;
    };
}
