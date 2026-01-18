[![License](https://img.shields.io/github/license/kunterbunt2/project-hub)](https://github.com/kunterbunt2/project-hub/blob/main/LICENSE)
[![Tests](https://img.shields.io/github/actions/workflow/status/kunterbunt2/project-hub/maven-build.yml?label=tests)](https://github.com/kunterbunt2/project-hub/actions/workflows/maven-build.yml)
[![codecov](https://codecov.io/github/kunterbunt2/project-hub/branch/main/graph/badge.svg)](https://codecov.io/github/kunterbunt2/project-hub)

# kassandra

tiny project management server.<br>
project effort estimation and progress tracking and release date interpolation open source server.

## features

tbd

[Requirements](https://github.com/kunterbunt2/project-hub/wiki/Requirements)

[Limitations](https://github.com/kunterbunt2/project-hub/wiki/Limitations)

[Design](https://github.com/kunterbunt2/project-hub/wiki/Design)

[ER Diagram](https://github.com/kunterbunt2/project-hub/wiki/ER-Diagram)

# Roadmap

## Phase 1

1. ✅ Basic functionality
2. ✅ Product, Version, project
3. ✅ project list.
4. ✅ every project has list of sprints.
5. ✅ every sprint contains stories and tasks.
6. ✅ every task has start, end, effort estimation, effort worked, dependency to other tasks or stories.
7. ✅ Authentication via oidc
8. ✅ User availability time-frames
9. ✅ User location time-frames
10. ✅ User work week time-frames
11. ✅ User Work hours time-frames
12. ✅ Authorization, access control using user groups on project level.
13. ❌ gantt chart generation with resource conflict visualization.
    1. ✅ Gantt task only on working days.
    2. ✅ X-axis calendar make none working day gray.
14. ✅ burn down chart for every sprint.
15. ❌ keep number of clicks to minimum for daily work of developer.
16. ❌ Close project Release Date.
17. ✅ National Holidays
18. ❌ data scenario simulation generator
    1. ✅ Simulator Write the use case as a Story in the project or product
    2. ❌ include closed and delayed sprints.

## Phase 2

1. ❌ Audit logs
2. ❌ lock project.
3. ❌ give project managers ways to control schedule.

## Phase 3

1. ❌ Admin hub

## Phase 4

1. ❌ Performance
2. ❌ Live updates to your inputs
3. ❌ Live response to your Input.
4. ✅ product page.
5. ❌ GDPR

# Kassandra Introduction Videos

1. Managing Users Introduction Video
2. Managing User Groups Introduction Video
3. User Profile Introduction Video
4. User Off Days Introduction Video
5. User Locations Introduction Video
6. User Availability Introduction Video
7. Projects, Versions, Features and Sprints Introduction Video
8. Stories and Tasks Introduction Video
9. Rearranging Stories and Tasks Introduction Video
10. Story and Task Relations Introduction Video
11. Logging Work Introduction Video

# Screenshots

![gantt-03](https://raw.githubusercontent.com/wiki/kunterbunt2/project-hub/gantt/gantt_03-3-gant-chart.svg)

![burn-down-03](https://raw.githubusercontent.com/wiki/kunterbunt2/project-hub/burn-down/gantt_03-3-burn-down.svg)

![Christopher Paul.de.nw](https://raw.githubusercontent.com/wiki/kunterbunt2/project-hub/calendar/Christopher%20Paul.de.nw.svg)

# Design Philosophy

- As simple as possible, as complex as necessary.
- backup the development with unit tests.
- create data generators that can be used in unit tests.
- written in Java + spring boot + Vaadin.
- minimalistic project status tracking within one single server.
- simple local database, but keep option to switch to other databases.

# Ideas

- introduce ai summary for all projects.
- Take a look how jira is sending fields to the client and replicate if it is good.
- Projects can be locked for change, which will lock start/end dates and all milestones
- project priority can be changed by moving them within the list
- sprint priority can be changed by moving them within the list

# License

[Apache License, version 2.0](https://github.com/kunterbunt2/project-hub/blob/main/LICENSE)
