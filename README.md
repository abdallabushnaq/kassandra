[![License](https://img.shields.io/github/license/kunterbunt2/project-hub)](https://github.com/kunterbunt2/project-hub/blob/main/LICENSE)
[![Tests](https://img.shields.io/github/actions/workflow/status/kunterbunt2/project-hub/maven-build.yml?label=tests)](https://github.com/kunterbunt2/project-hub/actions/workflows/maven-build.yml)
[![codecov](https://codecov.io/github/kunterbunt2/project-hub/branch/main/graph/badge.svg)](https://codecov.io/github/kunterbunt2/project-hub)

# kassandra

tiny project management server.<br>
project effort estimation and progress tracking and release date interpolation open source server.

## Notice

- for the agent tests to run, you need to load ministral-3-8B with 20480 token context and a sed of 42.

## features

tbd

[Requirements](https://github.com/kunterbunt2/project-hub/wiki/Requirements)

[Limitations](https://github.com/kunterbunt2/project-hub/wiki/Limitations)

[Design](https://github.com/kunterbunt2/project-hub/wiki/Design)

[ER Diagram](https://github.com/kunterbunt2/project-hub/wiki/ER-Diagram)

# Roadmap

## Phase 1 (minimum viable specs)

1. ✅ Authentication via oidc
2. ✅ Product crud
3. ✅ Version crud
4. ✅ Feature crud
5. ✅ Sprint crud
6. ✅ User crud
7. ✅ User groups crud
8. ❌ Task crud
9. ❌ worklog crud
10. ✅ User availability time-frames
11. ✅ User location time-frames
12. ❌ User work week time-frames
13. ✅ National Holidays
14. ✅ vacations
15. ✅ sick leaves
16. ✅ Authorization, access control using user groups on project level.
17. ✅ Gantt chart
18. ❌ Automatic Gantt buffer calculation
19. ✅ Burn down chart
20. ❌ Close project Release Date.
21. ✅ Dialog should set curser to edit box
22. ✅ Dialog confirmation button should react to return
23. ✅ add dark avatar
24. ❌ show product, versions, features, in sprint view
25. ✅ show Versions, Features, Sprints pages in menu
26. ❌ can we run browser in full screen mode?
27. ❌ data scenario simulation generator
    1. ✅ Simulator Write the use case as a Story in the project or product
    2. ❌ include closed and delayed sprints.

## Phase 2 (installable version)

1. ❌ alpha release (0.1.0) of minimum viable product.
2. ❌ docker container image.
3. ❌ first initialization.
4. ❌ server settings

## Phase 3 (optimizations)

1. ❌ gantt chart generation with resource conflict visualization.
2. ❌ keep number of clicks to minimum for daily work of developer.
3. ❌ Audit logs
4. ❌ lock project.
5. ❌ give project managers ways to control schedule.
6. ❌ Admin hub
7. ❌ Performance
8. ❌ Live updates to your inputs
9. ❌ Live response to your Input.
10. ✅ product page.
11. ❌ GDPR
12. ❌ undo
13. ❌ history
14. ❌ add aura theme

# Kassandra Introduction Videos

https://www.youtube.com/playlist?list=PL1FdjPuGzg7LDRGZeP6uQAPet1_fZePGs

1. ✅ Managing Users in Kassandra Introduction Video
2. ✅ Managing User Groups in Kassandra Introduction Video
3. ✅ User Profiles in Kassandra Introduction Video
4. ✅ User Off Days Introduction Video
5. ✅ User Locations Introduction Video
6. ✅ User Availability Introduction Video
7. ✅ Kassandra Products, Versions, Features and Sprints Introduction Video
8. ✅ Stories and Tasks Introduction Video
9. Rearranging Stories and Tasks Introduction Video
10. Story and Task Relations Introduction Video
11. Logging Work Introduction Video
12. Kassandra Agent Introduction Video

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
