<!--
Copyright (C) 2025-2026 Abdalla Bushnaq
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

**Quality Board** summarizes a sprint's planning and progress. Use it to inspect
the schedule and export information; use Backlog to change the underlying work.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Select and inspect a sprint

![Sprint selector](images/help/light/QualityBoard/sprint-selector.png)

Select the intended sprint and check the heading. Opening a row in Sprints
also brings you here. A selection refreshes the detail area and charts;
it does not modify the selected sprint.

Inspect the displayed sprint information alongside the chart. A missing start
date produces **No Sprint Data to Show**; an empty selection cannot produce a
schedule. Set up the sprint and its work before interpreting a missing forecast.

## Read the schedule and progress chart

![Combined chart](images/help/light/QualityBoard/quality-board-gantt-burndown-container.png)

Compare Gantt scheduling and burndown progress. Forecasts depend on the recorded
tasks, estimates, assignments, working calendars and worklogs; they are not a
guarantee of delivery dates.

![Separate chart window](images/help/light/QualityBoard/open-gantt-burndown-chart-button.png)

Open the chart in another window for a larger view. If no window appears,
check browser popup permissions. Switch sprint in the main page to inspect
different work.

## Export sprint information

![JSON export](images/help/light/QualityBoard/quality-board-export-json.png)

Use **JSON** for structured sprint data.

![XML export](images/help/light/QualityBoard/quality-board-export-xml.png)

Use **XML** for the supported XML sprint export.

![Spreadsheet export](images/help/light/QualityBoard/quality-board-export-xlsx.png)

Use **XLSX** for the Gantt/burndown spreadsheet export. Check the selected sprint
before downloading. Exports download information; they do not change server data.
Treat exported planning data according to your team's sharing rules.

## Use the main menu and header

{{include:main-menu}}

{{include-admin:main-menu-admin}}

![Quality Board help](images/help/light/QualityBoard/quality-board-help-button.png)

The question mark next to the sprint selector opens this help and remains
available when the sprint has no chart data. Use **Backlog** to correct the plan,
or **Active Sprints** to record day-to-day progress.
