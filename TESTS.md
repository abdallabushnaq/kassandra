# Failing Tests

## Known Tests to Fail

1. several GanttTets fail because they do not expect sprint to have any other status than STARTED.
2. 2 TestTest (dependency to task with higher orderId fails to level)

## New Tests to Fail

1. all TestTest.
   java.lang.NullPointerException: Cannot invoke
   "de.bushnaq.abdalla.kassandra.service.GanttBurndownChartService.calculateAuthorContribution
   (de.bushnaq.abdalla.kassandra.dto.Sprint, java.util.List, java.util.List,
   de.bushnaq.abdalla.kassandra.report.dao.AuthorsContribution)" because "this.service" is null

   at de.bushnaq.abdalla.kassandra.report.burndown.LegacyBurnDownRenderer.init (BurnDownRenderer.java:647)
   at de.bushnaq.abdalla.kassandra.report.burndown.LegacyBurnDownRenderer.processingInit (BurnDownRenderer.java:782)
   at de.bushnaq.abdalla.kassandra.report.burndown.LegacyBurnDownRenderer.<init>(BurnDownRenderer.java:88)
   at de.bushnaq.abdalla.kassandra.report.burndown.LegacyBurnDownChart.<init>(BurnDownChart.java:29)
   at de.bushnaq.abdalla.kassandra.util.GanttGenerator.generateBurndownChart (GanttGenerator.java:117)
   at de.bushnaq.abdalla.kassandra.report.gantt.AbstractGanttTester.evaluate (AbstractGanttTester.java:111)
   at de.bushnaq.abdalla.kassandra.report.gantt.AbstractGanttTester.executeTest (AbstractGanttTester.java:138)
   at de.bushnaq.abdalla.kassandra.report.gantt.TestTest.test (TestTest.java:70)
2. first 3 BurndownUtilTest
   org.opentest4j.AssertionFailedError: User 'Christopher Paul' created date does not match ==>
   Expected :2023-01-01T08:00+01:00
   Actual   :2023-01-01T08:00:01+01:00
3. all BurndownTest
   org.opentest4j.AssertionFailedError: User 'Christopher Paul' created date does not match ==>
   Expected :2022-12-15T08:00+01:00
   Actual   :2023-05-05T08:00:02+02:00

4. first DynamicGanttBurndownTest
   java.lang.NullPointerException: Cannot invoke
   "de.bushnaq.abdalla.kassandra.service.GanttBurndownChartService.calculateAuthorContribution
   (de.bushnaq.abdalla.kassandra.dto.Sprint, java.util.List, java.util.List,
   de.bushnaq.abdalla.kassandra.report.dao.AuthorsContribution)" because "this.service" is null

   at de.bushnaq.abdalla.kassandra.report.burndown.LegacyBurnDownRenderer.init (BurnDownRenderer.java:647)
   at de.bushnaq.abdalla.kassandra.report.burndown.LegacyBurnDownRenderer.processingInit (BurnDownRenderer.java:782)
   at de.bushnaq.abdalla.kassandra.report.burndown.LegacyBurnDownRenderer.<init>(BurnDownRenderer.java:88)
   at de.bushnaq.abdalla.kassandra.report.GanttBurndown.LegacyGanttBurndownChart.<init>(GanttBurndownChart.java:36)
   at de.bushnaq.abdalla.kassandra.util.GanttGenerator.generateGanttBurndownChart (GanttGenerator.java:138)
   at de.bushnaq.abdalla.kassandra.report.ganttBurndown.DynamicGanttBurndownTest.testDeliveryBuffer1
   (DynamicGanttBurndownTest.java:120)

5. all OldGanttTest
   org.springframework.security.access.AccessDeniedException: Access denied when accessing API

   at de.bushnaq.abdalla.kassandra.rest.api.AbstractApi.handleExceptions (AbstractApi.java:379)
   at de.bushnaq.abdalla.kassandra.rest.api.AbstractApi.executeWithErrorHandling (AbstractApi.java:307)
   at de.bushnaq.abdalla.kassandra.rest.api.UserApi.persist (UserApi.java:186)
   at de.bushnaq.abdalla.kassandra.util.PersistingEntityGenerator.lambda$addUser$0 (PersistingEntityGenerator.java:469)
   at de.bushnaq.abdalla.kassandra.util.EntityGenerator.addUser (EntityGenerator.java:266)
   at de.bushnaq.abdalla.kassandra.util.PersistingEntityGenerator.addUser (PersistingEntityGenerator.java:466)
   at de.bushnaq.abdalla.kassandra.util.PersistingEntityGenerator.addRandomUser (PersistingEntityGenerator.java:303)
   at de.bushnaq.abdalla.kassandra.report.gantt.OldGanttTest.gantt_03 (OldGanttTest.java:158)

6. all CriticalTest
   java.lang.NullPointerException: Cannot invoke
   "de.bushnaq.abdalla.kassandra.service.GanttBurndownChartService.calculateAuthorContribution
   (de.bushnaq.abdalla.kassandra.dto.Sprint, java.util.List, java.util.List,
   de.bushnaq.abdalla.kassandra.report.dao.AuthorsContribution)" because "this.service" is null

   at de.bushnaq.abdalla.kassandra.report.burndown.LegacyBurnDownRenderer.init (BurnDownRenderer.java:647)
   at de.bushnaq.abdalla.kassandra.report.burndown.LegacyBurnDownRenderer.processingInit (BurnDownRenderer.java:782)
   at de.bushnaq.abdalla.kassandra.report.burndown.LegacyBurnDownRenderer.<init>(BurnDownRenderer.java:88)
   at de.bushnaq.abdalla.kassandra.report.burndown.LegacyBurnDownChart.<init>(BurnDownChart.java:29)
   at de.bushnaq.abdalla.kassandra.util.GanttGenerator.generateBurndownChart (GanttGenerator.java:117)
   at de.bushnaq.abdalla.kassandra.report.gantt.AbstractGanttTester.evaluate (AbstractGanttTester.java:111)
   at de.bushnaq.abdalla.kassandra.report.gantt.AbstractGanttTester.executeTest (AbstractGanttTester.java:138)
   at de.bushnaq.abdalla.kassandra.report.gantt.CriticalTest.test (CriticalTest.java:70)


7. all GanttTest
   java.lang.NullPointerException: Cannot invoke
   "de.bushnaq.abdalla.kassandra.service.GanttBurndownChartService.calculateAuthorContribution
   (de.bushnaq.abdalla.kassandra.dto.Sprint, java.util.List, java.util.List,
   de.bushnaq.abdalla.kassandra.report.dao.AuthorsContribution)" because "this.service" is null

   at de.bushnaq.abdalla.kassandra.report.burndown.LegacyBurnDownRenderer.init (BurnDownRenderer.java:647)
   at de.bushnaq.abdalla.kassandra.report.burndown.LegacyBurnDownRenderer.processingInit (BurnDownRenderer.java:782)
   at de.bushnaq.abdalla.kassandra.report.burndown.LegacyBurnDownRenderer.<init>(BurnDownRenderer.java:88)
   at de.bushnaq.abdalla.kassandra.report.burndown.LegacyBurnDownChart.<init>(BurnDownChart.java:29)
   at de.bushnaq.abdalla.kassandra.util.GanttGenerator.generateBurndownChart (GanttGenerator.java:117)
   at de.bushnaq.abdalla.kassandra.report.gantt.AbstractGanttTester.evaluate (AbstractGanttTester.java:111)
   at de.bushnaq.abdalla.kassandra.report.gantt.AbstractGanttTester.executeTest (AbstractGanttTester.java:138)
   at de.bushnaq.abdalla.kassandra.report.gantt.GanttTest.test (GanttTest.java:76)

8. TaskListViewIT
9. DynamicGanttTest
   java.lang.NullPointerException: Cannot invoke
   "de.bushnaq.abdalla.kassandra.service.GanttBurndownChartService.calculateAuthorContribution
   (de.bushnaq.abdalla.kassandra.dto.Sprint, java.util.List, java.util.List,
   de.bushnaq.abdalla.kassandra.report.dao.AuthorsContribution)" because "this.service" is null

   at de.bushnaq.abdalla.kassandra.report.burndown.LegacyBurnDownRenderer.init (BurnDownRenderer.java:647)
   at de.bushnaq.abdalla.kassandra.report.burndown.LegacyBurnDownRenderer.processingInit (BurnDownRenderer.java:782)
   at de.bushnaq.abdalla.kassandra.report.burndown.LegacyBurnDownRenderer.<init>(BurnDownRenderer.java:88)
   at de.bushnaq.abdalla.kassandra.report.burndown.LegacyBurnDownChart.<init>(BurnDownChart.java:29)
   at de.bushnaq.abdalla.kassandra.util.GanttGenerator.generateBurndownChart (GanttGenerator.java:117)
   at de.bushnaq.abdalla.kassandra.report.gantt.DynamicGanttTest.testDeliveryBuffer1 (DynamicGanttTest.java:147)

10. many WorkWeekApiTest
    org.springframework.security.authentication.AuthenticationCredentialsNotFoundException: Authentication credentials
    not found when accessing API

    at de.bushnaq.abdalla.kassandra.rest.api.AbstractApi.handleExceptions (AbstractApi.java:376)
    at de.bushnaq.abdalla.kassandra.rest.api.AbstractApi.executeWithErrorHandling (AbstractApi.java:307)
    at de.bushnaq.abdalla.kassandra.rest.api.WorkWeekApi.persist (WorkWeekApi.java:113)
    at de.bushnaq.abdalla.kassandra.rest.api.WorkWeekApiTest.create (WorkWeekApiTest.java:138)

