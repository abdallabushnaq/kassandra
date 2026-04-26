# TODO

1. rename all mcp tpo IDs
2. GENERATOR. ensure some projects show delay, are not started or are finished closed.
3. add better error handling in api.
4. remove default test password from AbstractApi.
5. make sure getAll will filter via alc in db, not in memory.
6. make all entities client side id generated
    1. dto and dao need default constructors that set the id
    2. remove the generator annotation
    3. use entitymanager to persist

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

# Failing Tests

1. all TestTest (dependency to task with higher orderId fails to level)
