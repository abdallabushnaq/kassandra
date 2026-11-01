# Failing Tests

## Known Tests to Fail

1. 2 TestTest (dependency to task with higher orderId fails to level)

## New Tests to Fail

1. OldGanttTest.gantt_01 fails when run a second time with
   org.opentest4j.AssertionFailedError: Sprint names do not match ==>
   Expected :Backlog
   Actual   :gantt_01-1
   at org.junit.jupiter.api.AssertionFailureBuilder.build (AssertionFailureBuilder.java:158)
   at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow (AssertionFailureBuilder.java:139)
   at org.junit.jupiter.api.AssertEquals.failNotEqual (AssertEquals.java:201)
   at org.junit.jupiter.api.AssertEquals.assertEquals (AssertEquals.java:184)
   at org.junit.jupiter.api.Assertions.assertEquals (Assertions.java:1199)
   at de.bushnaq.abdalla.kassandra.util.DTOAsserts.assertSprintEquals (DTOAsserts.java:117)
   at de.bushnaq.abdalla.kassandra.util.DTOAsserts.assertSprintEquals (DTOAsserts.java:109)
   at de.bushnaq.abdalla.kassandra.util.AbstractGanttTestUtil.compareResults (AbstractGanttTestUtil.java:130)
   at de.bushnaq.abdalla.kassandra.util.AbstractGanttTestUtil.compareResults (AbstractGanttTestUtil.java:165)
   at de.bushnaq.abdalla.kassandra.util.AbstractGanttTestUtil.generateGanttChart (AbstractGanttTestUtil.java:239)
   at de.bushnaq.abdalla.kassandra.report.gantt.OldGanttTest.gantt_01 (OldGanttTest.java:87)

2. most TestTest tests do not match the reference.
   TestTest test9-private fails with:
   java.lang.NullPointerException: Cannot invoke "java.time.LocalDateTime.toLocalDate ()" because the return value of
   "de.bushnaq.abdalla.kassandra.dto.Task.getStart ()" is null
   at de.bushnaq.abdalla.kassandra.util.GanttGenerator.generateWorklogs (GanttGenerator.java:384)
   at de.bushnaq.abdalla.kassandra.report.gantt.AbstractGanttTester.evaluate (AbstractGanttTester.java:109)
   at de.bushnaq.abdalla.kassandra.report.gantt.AbstractGanttTester.executeTest (AbstractGanttTester.java:138)
   at de.bushnaq.abdalla.kassandra.report.gantt.TestTest.test (TestTest.java:70)
   TestTest (dependency to task with higher orderId fails to level)
