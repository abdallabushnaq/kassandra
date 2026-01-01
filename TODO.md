# TODO

1. backlog. add a default sprint with the actual backlog.
2. GENERATOR. ensure some projects show delay, are not started or are finished closed.
3. add better error handling in api.
4. add ability to edit or delete a worklog.
5. remove default test password from AbstractApi.
6. make sure getAll will filter via alc in db, not in memory.

## Feature

# Bugs

- sometimes adding a story and two tasks will add additionally one task.
- changing assignment must also change hidden dependencies
- some tests fail with java.awt.HeadlessException.
- users are retired by their name instead of their email address.
- LocationDialog not showing errors in dialog.
- AvailabilityTest.userSecurity() generates several exceptions on server side that the test does not catch.
- gantt resource leveling fails sometimes with circular dependency error.
- fix resource leveling not handling dependency to later story.
- gantt calendar too light.
- gantt calendar should be using sprint calendar.
- some ai filter test fail all the time, as the tests are vague.
