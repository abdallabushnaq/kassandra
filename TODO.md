# TODO

1. still need to run npm run build:static to generate debugging version of js f
2. rename all mcp tpo IDs
3. GENERATOR. ensure some projects show delay, are not started or are finished closed.
4. add better error handling in api.
5. remove default test password from AbstractApi.
6. make sure getAll will filter via alc in db, not in memory.
7. make all entities client side id generated
    1. dto and dao need default constructors that set the id
    2. remove the generator annotation
    3. use entitymanager to persist
8. sort db printout
9. remove fallback parameter in ColorUtils.intToHex

## Feature

# Bugs

1. users are retired by their name instead of their email address.
2. AvailabilityTest.userSecurity() generates several exceptions on server side that the test does not catch.
3. fix resource leveling not handling dependency to later story (TestTest).
4. gantt calendar should be using sprint calendar and user calendar.
5. many places only reference lightAvatar, but not dark.
6. editing a worklog messes up the remaining work.
7. AI agent is very slow in introduction video.
8. selecting newly created sprint crashes with: "de.bushnaq.abdalla.kassandra.dto.Sprint.getEnd() is null.
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

# Latest Bugs

1. FIXED: Gantt and burndown charts do not scroll.
2. FIXED: Gantt grid only spans from S to E.
3. FIXED: Gantt footer missing generated message.
4. FIXED: Burndown Watermark not transparent.
5. FIXED: Burndown Authors legend location too much to the right.
6. FIXED: post-run should be 3 weeks not 2.
7. buffer not resource aligned.
8. FIXED: release extrapolation chart guide missing.
9. FIXED: burndown chart calendar day width is not aligned with gantt chart day width when zooming, fixed when
   scrolling.
10. FIXED: several sprints will fail to render (end of graph disappears when zooming in).
11. FIXED: several sprints will fail to render (only partly visible).
12. several sprints will fail to render (grows with time).
13. FIXED: key not visible in footer.
14. FIXED: y-axis of burndown chart is not scrolling.
15. FIXED: ID cells of Gantt chart are not scrolling.
16. FIXED: Gantt chart dependencies rendered half a day to the right.
17. FIXED: Burndown chart missing tooltip.
18. Chart mouse shape should be an arrow, not hand.
19. tasks overarching offdays draw dotted borders strange (dotted lines will not scroll correctly).
20. FIXED: Burndown legend not scrolling and not affected by zoom.
21. chartWidth is contained in diagram.width adn dayWidth is contained in calendarXAxis.dayOfWeek.width. We should
    consolidate.
22. Milestone flag connector going wrong direction.

# Failing Tests

1. 2 TestTest (dependency to task with higher orderId fails to level)
2. first OldGanttTest
3. all BurndownTest
4. several GanttTets fail because they do not expect sprint to have any other status than STARTED.
