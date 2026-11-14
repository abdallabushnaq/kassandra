# Interactive Gantt task inspection

## Problem and approach

Add read-only task selection, recursive predecessor highlighting, and persistent
task details to both GanttChart and GanttBurndownChart. Keep Java scheduling and
DTO preparation unchanged. Share the browser implementation through GanttRenderer
and a small Gantt-specific interaction controller; add no dependencies.

The SVG is live browser DOM, not an image. GanttRenderer already wraps each task
in a group with data-task-id. AbstractGanttRenderer.drawTaskRelations already
wraps each complete dependency in a task-dependency group containing its
connector path and arrowhead polygon. Add endpoint metadata to that group;
individual lines, path segments, and arrowheads do not need duplicate IDs.
Whole-relation styling targets the group and its graphical children.

## Confirmed decisions

- Include selection, predecessor-chain highlighting, and a persistent details
  panel in both chart types.
- Traverse only relations whose visible flag is true. Hidden, automatically
  generated resource dependencies remain hidden and are not traversed.
- Dock details to the right on wide chart hosts; stack them below the chart on
  narrow hosts.
- Selection temporarily reveals its visible predecessor-chain arrows even when
  the ordinary dependency zoom threshold would hide them. Other arrows continue
  to follow the current threshold.

## Feature boundaries and interaction contract

- Click a task bar, milestone, story shape, task key, or task label to select it.
  Selecting another task replaces the selection; selecting the same task keeps
  its details open.
- Highlight the selected task separately from its direct and indirect
  predecessors. Emphasize every edge on those upstream paths, including its
  complete connector and arrowhead. Do not follow successors or unrelated
  branches.
- Fade unrelated Gantt tasks and relations modestly, preserving readability,
  assignee colours, progress, and critical-path colours. Do not fade the
  burndown plot, calendar, or chart controls.
- Add subtle hover feedback without a full SVG rebuild on pointer movement.
- Background activation, Escape within the chart/details area, or the details
  close button clears selection and restores ordinary rendering thresholds.
  Activating an arrow is not a new navigation feature and must not accidentally
  select a task.
- Task groups support keyboard focus, Enter/Space selection, accessible labels,
  and a visible focus indication. Restore task focus after a redraw where
  practical, without stealing focus from details controls.
- Keep existing task/resource tooltips and burndown HTML tooltips unchanged in
  this first implementation. Tooltip reduction is not part of this change.
- Selection survives panning, either zoom mode, responsive layout changes, and
  SVG redraws within one mounted chart instance.
- Selection is not stored in localStorage. A fresh data/theme mount or moving
  between embedded and detached instances starts unselected. Both embedded and
  detached instances provide the full feature.
- Only inspect task/predecessor data already delivered in the DTO. Do not infer
  parent-story dependencies from row order, fetch missing tasks, or change the
  scheduler.
- No burndown crosshair/point inspection, editing, task dragging, successor
  highlighting, multi-selection, or task navigation outside this chart.

## Affected components and proposed APIs

All frontend paths below are relative to src\main\frontend\js.

### 1. SVG metadata and selection-aware rendering

Files: gantt\AbstractGanttRenderer.ts and gantt\GanttRenderer.ts.

- Preserve the existing task-relations layer and arrow routing.
- Add data-predecessor-id and data-successor-id to each task-dependency group.
  The predecessor is the task whose finish is the arrow's start; the successor
  is the task whose start receives the arrow. Do not derive direction from the
  current sourceTask/targetTask parameter names.
- Give task groups a stable class plus their existing data-task-id and keyboard
  attributes.
- Introduce a small typed selection value in GanttRenderer.ts:

  ```ts
  export interface GanttTaskSelection {
      taskId: string;
      predecessorIds: ReadonlySet<string>;
      predecessorIdsBySuccessorId: ReadonlyMap<string, ReadonlySet<string>>;
  }

  GanttRenderer.setTaskSelection(selection: GanttTaskSelection | null): void;
  ```

- Pass the selected edge set for each successor into the existing relation
  drawing method through an optional parameter:

  ```ts
  drawTaskRelations(
      g: SVGElement,
      task: TaskDto,
      emphasizedPredecessorIds?: ReadonlySet<string>,
  ): void;
  ```

  Draw a visible relation when either the ordinary zoom threshold is met or
  that exact edge is emphasized. Never override visible=false. An absent
  optional parameter preserves existing behaviour for other renderers.
- Render selection classes/attributes and task outlines from the selection
  value, so newly built SVG nodes immediately reflect selection.
- Emphasize a relation's entire group and child connector/arrowhead together;
  preserve its existing normal or critical colour.

### 2. Shared browser interaction and details UI

New file: gantt\GanttTaskInteraction.ts.

- Own a wrapper containing an SVG viewport and a sibling HTML details panel.
  AbstractCanvas.render clears only the viewport, never the details panel.
- Keep the panel hidden without selection, leaving the full chart width.
  Initially use a roughly 320-pixel right pane; switch to a below-chart layout
  when the outer chart host is narrower than roughly 900 pixels. Base the
  breakpoint on the outer host, not the shrinking viewport, to prevent layout
  oscillation.
- Use chart-local styles and the supplied Theme, not page-only Vaadin styling.
  The panel must work in the standalone popup document. Wrap long notes and
  identifiers and bound/scroll long details content.
- Build one task-ID index from renderer.tasks. On selection, traverse visible
  predecessors iteratively, recording both tasks and exact directed edges.
  Normalize IDs with String (id), deduplicate shared ancestors, and use visited
  state to terminate safely on cycles.
- Display unresolved visible predecessor references as unavailable in the
  panel and stop traversal at those references. Do not silently hide them or
  issue extra REST calls.
- Show available fields: key/name, task kind, start/finish, assignee,
  availability/location, progress including zero, critical/manual flags, notes,
  and direct/indirect predecessors. Leave unavailable fields explicitly empty
  or labelled unavailable; do not manufacture estimates or other missing data.
- The predecessor list remains readable when its tasks are outside the visible
  timeline. It is an inspection list, not an auto-scroll/navigation feature.
- Insert task content using textContent/DOM construction, not raw innerHTML.
- Suggested controller surface:

  ```ts
  constructor(container: HTMLElement, renderer: GanttRenderer, theme: Theme);
  readonly viewport: HTMLDivElement;
  activate(target: Element | null): void;
  afterRender(): void;
  attach(chart: ChartHandle): ChartHandle;
  ```

  attach returns a lifecycle wrapper delegating render/schedule and cleaning up
  controller listeners/observers/layout along with the underlying chart on
  destroy. Keep one canonical selection in the controller and pass its typed
  rendering value to GanttRenderer.

### 3. Pointer activation without breaking panning

File: InteractiveTimelineChart.ts.

- Add an optional onActivate callback to InteractiveTimelineChartOptions:

  ```ts
  onActivate?: (target: Element | null) => void;
  ```

- For opted-in charts, retain the original pointer-down target or its logical
  task identity before container pointer capture retargets later events.
  Preserve that identity even if a redraw replaces the original SVG.
- Track maximum movement from pointer-down, not just the release displacement.
  Invoke activation only for a primary-button release below a small movement
  threshold, initially 5 CSS pixels. A drag away and back is still a drag.
- Pointer cancellation and interrupted capture do not activate selection.
  Release/reset pointer state safely.
- Retain current navigation semantics for charts without onActivate. Keep
  inspector controls outside this listener's viewport.

### 4. Wire both factories and chart-specific hints

Files: gantt\gantt-bundle.ts, burndown\gantt-burndown-bundle.ts,
gantt\GanttChart.ts, and burndown\GanttBurndownChart.ts.

- Create the shared controller, give its viewport to InteractiveTimelineChart,
  connect onActivate/afterRender, and return the controller's ChartHandle wrapper.
- Keep public factory signatures, mount globals, container IDs, localStorage
  view-state keys, chart-ready/theme attributes, and detachable-host contracts
  unchanged.
- In the combined bundle, scope existing HTML tooltip positioning and listeners
  to the SVG viewport. Compose tooltip setup and selection afterRender work.
- Measure available timeline width from the viewport, so docking details causes
  an ordinary resize/redraw without disturbing date alignment between plots.
- Keep the inspector outside SVG transforms so chart zoom does not scale its UI.
- Update only the Gantt-specific caption hints for selection and Escape.
  Do not advertise task inspection on the sprints-overview chart.
- No production Java or DTO changes are expected.

## Todos and execution dependencies

1. Adding SVG endpoint metadata and selection-aware rendering.
2. Adding drag-safe optional activation to the shared timeline interaction.
3. Implementing the shared task inspector and integrating both chart factories.
   Requires todos 1 and 2.
4. Extending existing chart browser checks and running focused validation.
   Requires todo 3.
5. Updating the three affected help pages.
   Requires todo 3.

## Verification and acceptance criteria

Reuse config\GanttZoomTest.java and src\test\resources\gantt-zoom-check.js rather
than introducing a test framework or a new Spring test class. The existing
test inherits AbstractTestUtil and its state cleanup.

- Update related relation assertions to locate dependency groups by endpoint
  attributes rather than assuming arrows are nested inside task groups.
- Exercise both factory paths with a visible multi-level chain, a branch,
  multiple predecessors/shared ancestors, hidden resource edges, missing
  references, a cycle, and an isolated task.
- Verify task, milestone, story, label, and keyboard activation; panel values
  and safe rendering of HTML-like notes; Escape/close/background clearing.
- Verify the complete path and arrowhead share selection styling and that
  critical/assignee colours remain intact.
- At low zoom, reveal only the selected visible upstream edges. Clearing
  selection restores the configured threshold. Never reveal or traverse hidden
  resource edges.
- Use actual WebDriver pointer actions for click versus drag/pointer-capture
  coverage, not only synthetic click events. Cover drag-away-and-back and
  cancellation without accidental selection.
- Verify selection after day-width zoom, visual zoom, pan/lazy redraw, and
  resize; verify unselected rendering remains visually unchanged.
- Verify right-docked and below-chart layouts using chart-host width, inspector
  control clicks/scrolling without timeline pan, unchanged combined tooltips,
  and aligned Gantt/burndown timelines.
- Verify detached-window inspection and teardown/remount without duplicate
  panels, leftover listeners, or stale selection.
- Run npm run typecheck:static and npm run build:static, the IDE build tool,
  then mvn test -Dtest="GanttZoomTest,CriticalTest". Keep validation focused;
  no Vaadin view/security changes or external OIDC/AI services are required.
- Check the production static build path without leaving generated artifacts
  inconsistent with the final verification. Report genuine blockers rather
  than replacing browser interaction checks with DOM-only proxies.

## Documentation

Update the chart chapters in:

- src\main\resources\help\backlog.md
- src\main\resources\help\active-sprints.md
- src\main\resources\help\quality-board.md

Describe selection, visible-predecessor scope, selected-chain visibility at low
zoom, details placement, clearing selection, and read-only behaviour. Retain
existing tooltip and pan/zoom guidance. No screenshot regeneration is required
for an initially hidden panel; include focused visual checks of the selected
state during validation.

## Repository considerations

- Preserve the unrelated untracked docs\gdpr-compliance-design-changes.md.
- Keep changes surgical; do not refactor scheduling, tooltip generation, the
  detachable host, or unrelated chart code.
- Add the current Apache 2.0 header to the new TypeScript file, and Javadoc to
  any new public Java test methods if needed.
- Implementation begins only after plan approval.

