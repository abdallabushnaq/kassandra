# TODO

1. creating a user with no email will fail without any explanation
2. StableDiffusionService still has many methods returning byte[]. remove them.
3. number of tasks in lane is always 0
4. number of task per story is only including the once in that lane.
5. backlog. add a default sprint with the actual backlog.
6. GENERATOR. ensure some projects show delay, are not started or are finished closed.
7. add better error handling in api.
8. cleanup GenerateScreenshots logs.


## Feature
1. in my spring boot web app, all api calls are secured using oidc token roles. currently the roles are admin and user. it is not realistic for the customer to add these roles to the oidc ticket, which is why we want to handle user roles now locally in our own database. only admin users are allowed to add/delete/update users and navigate to the UserListView.
2. We want to restrict access to a Product and its Version/Features/Sprints using access-control-list. admin users can create/edit/delete user groups.
Whoever creasts a Product is authomatically added to the access-control-list. Whoever has access to a product and its child entities can edit the access-control-list.


ISSUES
- there is an exception everytime we start one of our tests.
org.springframework.web.client.UnknownContentTypeException: Could not extract response: no suitable HttpMessageConverter found for response type [class de.bushnaq.abdalla.kassandra.dto.User] and content type [text/html;charset=utf-8]
	at org.springframework.web.client.HttpMessageConverterExtractor.extractData(HttpMessageConverterExtractor.java:133) ~[spring-web-6.2.6.jar:6.2.6]
	at org.springframework.web.client.RestTemplate$ResponseEntityResponseExtractor.extractData(RestTemplate.java:1183) ~[spring-web-6.2.6.jar:6.2.6]
	at org.springframework.web.client.RestTemplate$ResponseEntityResponseExtractor.extractData(RestTemplate.java:1166) ~[spring-web-6.2.6.jar:6.2.6]
	at org.springframework.web.client.RestTemplate.doExecute(RestTemplate.java:903) ~[spring-web-6.2.6.jar:6.2.6]
	at org.springframework.web.client.RestTemplate.execute(RestTemplate.java:801) ~[spring-web-6.2.6.jar:6.2.6]
	at org.springframework.web.client.RestTemplate.exchange(RestTemplate.java:683) ~[spring-web-6.2.6.jar:6.2.6]
	at de.bushnaq.abdalla.kassandra.rest.api.UserApi.lambda$getByEmail$3(UserApi.java:85) ~[classes/:na]
	at de.bushnaq.abdalla.kassandra.rest.api.AbstractApi.executeWithErrorHandling(AbstractApi.java:188) ~[classes/:na]
