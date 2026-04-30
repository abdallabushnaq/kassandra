[![License](https://img.shields.io/github/license/kunterbunt2/project-hub)](https://github.com/kunterbunt2/project-hub/blob/main/LICENSE)
[![Tests](https://img.shields.io/github/actions/workflow/status/kunterbunt2/project-hub/maven-build.yml?label=tests)](https://github.com/kunterbunt2/project-hub/actions/workflows/maven-build.yml)
[![codecov](https://codecov.io/github/kunterbunt2/project-hub/branch/main/graph/badge.svg)](https://codecov.io/github/kunterbunt2/project-hub)

# kassandra

tiny project management server.<br>
project effort estimation and progress tracking and release date interpolation open source server.

## Notice

- some entities support client side id generation, which means that the client can generate an id for the entity before
  sending it to the server. This is useful to reducing the number of round trips to the server.
- for the agent tests to run, you need to load ministral-3-8B with 20480 token context and a sed of 42.

## features

tbd

[Requirements](https://github.com/kunterbunt2/project-hub/wiki/Requirements)

[Limitations](https://github.com/kunterbunt2/project-hub/wiki/Limitations)

[Design](https://github.com/kunterbunt2/project-hub/wiki/Design)

[ER Diagram](https://github.com/kunterbunt2/project-hub/wiki/entity-relationship-diagram)

# Roadmap

## Phase 1 (minimum viable specs)

1. ✅ Authentication via oidc
2. ✅ Product crud
3. ✅ Version crud
4. ✅ Feature crud
5. ✅ Sprint crud
6. ✅ User crud
7. ✅ User groups crud
8. ✅ Task crud
9. ✅ worklog crud
10. ✅ User availability time-frames
11. ✅ User location time-frames
12. ✅ User work week time-frames
13. ❌ Project work week time-frames
14. ✅ National Holidays
15. ✅ vacations
16. ✅ sick leaves
17. ✅ Authorization, access control using user groups on project level.
18. ✅ Gantt chart
19. ✅ Automatic Gantt buffer calculation
20. ✅ Burn down chart
21. ❌ Close project Release Date.
22. ✅ Dialog should set curser to edit box
23. ✅ Dialog confirmation button should react to return
24. ✅ add dark avatar
25. ❌ show product, versions, features, in backlog
26. ❌ show product, versions, features, in quality-board
27. ✅ show Versions, Features, Sprints pages in menu
28. ✅ add about box.
29. ✅ optimaze AbstractEntityGenrator avatar generation code.
30. ❌ data scenario simulation generator
    1. ✅ Simulator Write the use case as a Story in the project or product
    2. ❌ include closed and delayed sprints.

## Phase 2 (installable version)

1. ❌ alpha release (0.1.0) of minimum viable product.
2. ❌ docker container image.
3. ❌ first initialization.
4. ❌ server settings

## Phase 3 (optimizations)

1. ❌ test should not create default user with avatar to speed up test execution.
2. ❌ unit tests should turn off stable diffusion service to speed up tests execution.
3. ❌ run ui tests in browser full screen mode.
4. ❌ gantt chart generation with resource conflict visualization.
5. ❌ keep number of clicks to minimum for daily work of developer.
6. ❌ Audit logs
7. ❌ lock project.
8. ❌ give project managers ways to control schedule.
9. ❌ give project managers ways to control resource leveling.
10. ❌ Admin hub
11. ❌ Performance
12. ❌ Live updates to your inputs
13. ❌ Live response to your Input.
14. ✅ product page.
15. ❌ GDPR
16. ❌ undo
17. ❌ history
18. ❌ add aura theme

# Kassandra Introduction Videos

https://www.youtube.com/playlist?list=PL1FdjPuGzg7LDRGZeP6uQAPet1_fZePGs

1. ✅ 01 Welcome to Kassandra Introduction Video
2. ✅ 02 Managing Users in Kassandra Introduction Video
3. ✅ 03 Managing User Groups in Kassandra Introduction Video
4. ✅ 04 User Profiles in Kassandra Introduction Video
5. ✅ 05 User Off Days Introduction Video
6. ✅ 06 User Locations Introduction Video
7. ✅ 07 User Availability Introduction Video
8. ✅ 08 Work Weeks in Kassandra Introduction Video
9. ✅ 09 Kassandra Products, Versions, Features and Sprints Introduction Video
10. ✅ 10 Stories and Tasks Introduction Video
11. ✅ 11 Rearranging Stories and Tasks Introduction Video
12. ✅ 12 Story and Task Relations Introduction Video
13. ✅ 13 Logging Work Introduction Video
14. ✅ 14 Kassandra Agent Introduction Video

# Screenshots

![QualityBoard](https://raw.githubusercontent.com/wiki/kunterbunt2/project-hub/light-screenshots/quality-board.png)

![User Off-Days](https://raw.githubusercontent.com/wiki/kunterbunt2/project-hub/light-screenshots/offday-list-view.png)

![Active Spints](https://raw.githubusercontent.com/wiki/kunterbunt2/project-hub/light-screenshots/active-sprints.png)

![Generate AI Image](https://raw.githubusercontent.com/wiki/kunterbunt2/project-hub/light-screenshots/image-prompt-dialog.png)

![Dependency Dialog](https://raw.githubusercontent.com/wiki/kunterbunt2/project-hub/light-screenshots/dependency-dialog.png)

![Task Dialog](https://raw.githubusercontent.com/wiki/kunterbunt2/project-hub/light-screenshots/task-dialog.png)

![Log Work Dialog](https://raw.githubusercontent.com/wiki/kunterbunt2/project-hub/light-screenshots/worklog-create-dialog.png)

![Edit Work Week Dialog](https://raw.githubusercontent.com/wiki/kunterbunt2/project-hub/light-screenshots/work-week-edit-dialog.png)

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
