# TODO

1. fix tests failing in github action.
2. backlog. add a default sprint with the actual backlog.
3. GENERATOR. ensure some projects show delay, are not started or are finished closed.
4. add better error handling in api.
5. add ability to edit or delete a worklog.

## Feature

1. in my spring boot web app, all api calls are secured using oidc token roles. currently the roles are admin and user.
   it is not realistic for the customer to add these roles to the oidc ticket, which is why we want to handle user roles
   now locally in our own database. only admin users are allowed to add/delete/update users and navigate to the
   UserListView.
2. We want to restrict access to a Product and its Version/Features/Sprints using access-control-list. admin users can
   create/edit/delete user groups.
   Whoever creates a Product is automatically added to the access-control-list. Whoever has access to a product and its
   child entities can edit the access-control-list.
   admin users can edit the access-control-list.

# Bugs

