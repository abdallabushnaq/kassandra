# TODO

1. fix tests failing in github action.
3. number of tasks in lane is always 0
4. number of task per story is only including the once in that lane.
5. backlog. add a default sprint with the actual backlog.
6. GENERATOR. ensure some projects show delay, are not started or are finished closed.
7. add better error handling in api.

## Feature

1. in my spring boot web app, all api calls are secured using oidc token roles. currently the roles are admin and user.
   it is not realistic for the customer to add these roles to the oidc ticket, which is why we want to handle user roles
   now locally in our own database. only admin users are allowed to add/delete/update users and navigate to the
   UserListView.
2. We want to restrict access to a Product and its Version/Features/Sprints using access-control-list. admin users can
   create/edit/delete user groups.
   Whoever creasts a Product is authomatically added to the access-control-list. Whoever has access to a product and its
   child entities can edit the access-control-list.

# Bugs

1. ActiveSprintsTest
   2025-12-21T14:28:32.820+01:00 ERROR 39236 --- [           main] d.b.a.k.u.v.util.ProductListViewTester   : OIDC
   Login: Error during Keycloak login: Expected condition failed: waiting for presence of element located by: By.id:
   username (tried for 30 second(s) with 500 milliseconds interval)
   2025-12-21T14:28:32.821+01:00 ERROR 39236 --- [           main] d.b.a.k.u.v.util.ProductListViewTester   : OIDC
   Login: Fatal error in login process: Expected condition failed: waiting for presence of element located by: By.id:
   username (tried for 30 second(s) with 500 milliseconds interval)
   org.openqa.selenium.TimeoutException: Expected condition failed: waiting for presence of element located by: By.id:
   username (tried for 30 second(s) with 500 milliseconds interval)
   at org.openqa.selenium.support.ui.WebDriverWait.timeoutException(WebDriverWait.java:
    84) ~[selenium-support-4.38.0.jar:na]
        at org.openqa.selenium.support.ui.FluentWait.until(FluentWait.java:228) ~[selenium-support-4.38.0.jar:na]
        at de.bushnaq.abdalla.kassandra.ui.util.selenium.SeleniumHandler.waitUntil(SeleniumHandler.java:
    1253) ~[test-classes/:na]
3. ProductListViewTest.testCreateDuplicateNameFails
   org.opentest4j.AssertionFailedError: Error message should indicate a conflict ==>
   Expected :true
   Actual   :false
   <Click to see difference>
   at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
   at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
   at org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
   at org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
   at org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:214)
   at de.bushnaq.abdalla.kassandra.ui.view.util.ProductListViewTester.createProductWithDuplicateName(
   ProductListViewTester.java:112)
   at de.bushnaq.abdalla.kassandra.ui.view.ProductListViewTest.testCreateDuplicateNameFails(ProductListViewTest.java:98)
4. ProductListViewTest.testEditDuplicateNameFails
   org.opentest4j.AssertionFailedError: Error message should indicate a conflict ==>
   Expected :true
   Actual   :false
   <Click to see difference>
   at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
   at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
   at org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
   at org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
   at org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:214)
   at de.bushnaq.abdalla.kassandra.ui.view.util.ProductListViewTester.editProductWithDuplicateNameFails(
   ProductListViewTester.java:202)
   at de.bushnaq.abdalla.kassandra.ui.view.ProductListViewTest.testEditDuplicateNameFails(ProductListViewTest.java:161)

