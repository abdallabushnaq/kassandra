# Failing Tests

## Known Tests to Fail

1. 2 TestTest (dependency to task with higher orderId fails to level)

## New Tests to Fail

1. BurndownUtilTest:burndownTask01_startOnSunday
   org.opentest4j.AssertionFailedError: Sprint should not start on a Sunday ==>
   Expected :2025-01-06T08:00
   Actual   :2025-01-05T08:00
   <Click to see difference>

   at org.junit.jupiter.api.AssertionFailureBuilder.build (AssertionFailureBuilder.java:158)
   at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow (AssertionFailureBuilder.java:139)
   at org.junit.jupiter.api.AssertEquals.failNotEqual (AssertEquals.java:201)
   at org.junit.jupiter.api.AssertEquals.assertEquals (AssertEquals.java:184)
   at org.junit.jupiter.api.Assertions.assertEquals (Assertions.java:1199)
   at de.bushnaq.abdalla.kassandra.report.burndown.BurnDownUtilTest.burndownTask01_startOnSunday (BurnDownUtilTest.java:
    123)

2. most tests do not match the reference.
   TestTest test9-private fails with:
   java.lang.NullPointerException: Cannot invoke "java.time.LocalDateTime.toLocalDate ()" because the return value of
   "de.bushnaq.abdalla.kassandra.dto.Task.getStart ()" is null
   at de.bushnaq.abdalla.kassandra.util.GanttGenerator.generateWorklogs (GanttGenerator.java:384)
   at de.bushnaq.abdalla.kassandra.report.gantt.AbstractGanttTester.evaluate (AbstractGanttTester.java:109)
   at de.bushnaq.abdalla.kassandra.report.gantt.AbstractGanttTester.executeTest (AbstractGanttTester.java:138)
   at de.bushnaq.abdalla.kassandra.report.gantt.TestTest.test (TestTest.java:70)

4. several GanttTets fail because they do not expect sprint to have any other status than STARTED.
   can we change the status in teh reference mmp files?
