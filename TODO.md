# TODO

1. fix GenerateScreenshotsIT.
2. PersistingEntityGenerator should generate entities with correct user logged in.
3. test should not create default user with avatar to speed up test execution.
4. rename all mcp tpo IDs
5. GENERATOR. ensure some projects show delay, are not started or are finished closed.
6. add better error handling in api.
7. remove default test password from AbstractApi.
8. make sure getAll will filter via alc in db, not in memory.
9. make all entities client side id generated
    1. dto and dao need default constructors that set the id
    2. remove the generator annotation
    3. use entitymanager to persist
10. sort db printout
11. remove fallback parameter in ColorUtils.intToHex
12. TiledDiffusionOptions should be server settings.

## Feature

# Bugs

1. users are retired by their name instead of their email address.
2. AvailabilityTest.userSecurity () generates several exceptions on server side that the test does not catch.
3. fix resource leveling not handling dependency to later story (TestTest).
4. gantt calendar should be using sprint calendar and user calendar.
5. many places only reference lightAvatar, but not dark.
6. editing a worklog messes up the remaining work.
7. AI agent is very slow in introduction video.
8. selecting newly created sprint crashes with: "de.bushnaq.abdalla.kassandra.dto.Sprint.getEnd () is null.
9. gantt is not using user weekend from calendar.
10. Product show id instead of key
11. Feature shows id instead of key
12. Version shows id instead of key
13. Sprint shows id instead of key
14. User shows id instead of key
15. user colors are not predictable in the generation.
16. gantt task tooltip not accessible on the pprogress number.
17. a lot of Gantt charts are not actually resource leveled. Example Demo, testCaseIndex=1, sprint=Paris,
18. all js charts are drawn 2 times.
19. gantt chart task tooltip is missing many items from legacy code.
20. Gantt chart is drawn until now although they are closed.
21. there is currently no way to move the backlog back to independence if it was moved by mistake into a story.

# Latest Bugs

1. buffer not resource aligned.
2. tasks overarching off-days draw dotted borders strange (dotted lines will not scroll correctly).
3. chartWidth is contained in diagram.width and dayWidth is contained in calendarXAxis.dayOfWeek.width. We should
   consolidate.
4. off-day letter barely visible on weekends.
5. brun-down chart starting over minimum (Oslo).
6. burndown author legend color is too dark.
7. legend is drawn below grid lines.
8. FIXED: scrollYOffset and visualScale are not persisted in any of teh charts.
9. avatar default prompt is Default Avatar
10. fixed: header default prompt is missing simple white background/simple dark background
11. Create Feature dialog should allow selecting the project/version if the select box is allowing more than one.
12. Create Version dialog should allow selecting the project/version if the select box is allowing more than one.
13. Create Sprint dialog should allow selecting the project/version if the select box is allowing more than one.
14. Fixed: About banner is not themed.
15. Also corrected misleading filtering guidance: even name searches use AI, and the Features text filter and version
    selector currently replace rather than combine their filters.

# setup issues

- about image not stored in demo db.

# Missing Screenshots

| Area              | Screens or dialogs not covered                                                                                           |
|-------------------|--------------------------------------------------------------------------------------------------------------------------|
| Administration    | Audit page and expanded event details; Identity Providers; Server Settings and its categories                            |
| Authentication    | Setup wizard steps, recovery login, multi-provider login, no-provider login                                              |
| AI                | Kassandra assistant and Insights views                                                                                   |
| History           | Undo-history drawer, expanded operation details, undo/redo confirmation dialog                                           |
| Charts and boards | Detached chart windows; Backlog edit-mode screenshot; Active Sprints task-board and grouping variants                    |
| Worklogs          | Edit-worklog dialog and delete-worklog confirmation                                                                      |
| Image generation  | Product, feature, sprint and user image-prompt variants; profile variant is conditional on Stable Diffusion availability |