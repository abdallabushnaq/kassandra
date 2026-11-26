# Injecting Synthetic Problems into Sprints

Demo is a test that generates synthetic data for our app and stores it in a db dump.
We now want to alter the simulation to inject specific project management issues into specific sprints.

# Injecting Sick-Leaves

Currently, `AbstractGanttTestUtilgenerateProductsInternal` generates random sick days for all users independent of the
projects.
We want to change that.

A sprint has X percent chance of getting this injected.

For that we need to

1. iterate over the sprints and if a sprint is affected, we call method injectSickDays ()
2. we need to iterate over the planned tasks and their resources and pick the resource with the most planned work
3. chose 50% of the users planned task days in the middle of the project and add those days as sick OffDays in the users
   calendar.

Later, in BurnDownUtil.generateWorkLogs, this will cause the project to overrun.

# Injecting Cascading-Resource-Delay (domino effect)

Currently in `AbstractGanttTestUtilgenerateProductsInternal`, `BurnDownUtil.generateWorkLogs(peg)` generates work for
each sprint independently.
It uses `userWorkToday` to remember how much a user has already worked on a specific day, so that when we move from one
task to the next, we can only use up as much time as the user has available on that day.

We now want to allow sharing that information between sprints, so that if sprint **S-A** has delay and a resource
**R-A** is still busy working on it, sprint **S-B** will automatically get delayed if **R-A** is on the critical path of
that sprint, because the resource cannot start working for that sprint before finishing all tasks in sprint **S-A**

# Injecting Under-Estimation

Tasks that are estimated to take less time than they actually do, causing the sprint to overrun.

A sprint has X percent chance of getting this injected.

# Injecting Over-Estimation

Tasks that are estimated to take more time than they actually do, causing the sprint to finish early.

A sprint has X percent chance of getting this injected.

# Injecting Cascading-Task-Delay (domino effect)

Tasks that had to wait for their (delayed) dependencies and therefore were delayed (domino effect) visible in the chart.

# Injecting Over-Optimistic Extrapolated Sprint Release Date

The linear Extrapolated Sprint Release Date is overly optimistic in burn-down charts that have more resources
burning down in the beginning than in the end.
This causes the linear interpolation at the beginning of the project to have a steeper slope than the average total
project slope at the end of the project.

A sprint has X percent chance of getting this injected.

# Injecting Over-Pessimistic Extrapolated Sprint Release Date

The linear Extrapolated Sprint Release Date is overly pessimistic in burn-down charts that have less resources
burning down in the beginning than in the end.
This causes the linear interpolation at the beginning of the project to have a flatter slope than the average total
project slope at the end of the project.

A sprint has X percent chance of getting this injected.
