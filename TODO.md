# TODO

1. backlog. add a default sprint with the actual backlog.
2. GENERATOR. ensure some projects show delay, are not started or are finished closed.
3. add better error handling in api.
4. add ability to edit or delete a worklog.
5. remove default test password from AbstractApi.
6. make sure getAll will filter via alc in db, not in memory.

## Feature

1. We want to restrict access to a Product and its Version/Features/Sprints using access-control-list. admin users can
   create/edit/delete user groups.
   Whoever creates a Product is automatically added to the access-control-list. Whoever has access to a product and its
   child entities can edit the access-control-list.
   admin users can edit the access-control-list.

# Bugs

2. adapt SprintApiTest to ACL changes.
 
