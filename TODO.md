# TODO

1. GENERATOR. ensure some projects show delay, are not started or are finished closed.
2. add better error handling in api.
3. add ability to edit or delete a worklog.
4. remove default test password from AbstractApi.
5. make sure getAll will filter via alc in db, not in memory.
6. need ability to delete a task/story.
7. in case a user wants to remove himself from the ACL, we automatically add him back. you cannot remove yourself from
   ACL.

## Feature

1- Create Sprint Dialog that allows creation of Product, Version, Feature.

# Bugs

1. add gantt buffer calculation.
2. editing estimation will not change gantt chart task duration.
3. new task at the end will not take over last user assignment.
4. sometimes adding a story and two tasks will add additionally one task.
5. changing assignment must also change hidden dependencies
6. some tests fail with java.awt.HeadlessException.
7. users are retired by their name instead of their email address.
8. LocationDialog not showing errors in dialog.
9. AvailabilityTest.userSecurity() generates several exceptions on server side that the test does not catch.
10. gantt resource leveling fails sometimes with circular dependency error.
11. fix resource leveling not handling dependency to later story.
12. gantt calendar too light.
13. gantt calendar should be using sprint calendar.
14. some ai filter test fail all the time, as the tests are vague.
15. fix none humanized version of setMultiSelectComboBoxValue.
16. TaskGrid user colors are fake.

# Failing Tests

1. takeScreenshots

1- BurndownTest
2- ProjectsVersionsFeaturesAndSprintsIntroductionVideo 404 NOT_FOUND "Backlog sprint not found"

