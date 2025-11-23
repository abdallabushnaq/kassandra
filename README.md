[![License](https://img.shields.io/github/license/kunterbunt2/project-hub)](https://github.com/kunterbunt2/project-hub/blob/main/LICENSE)
[![Tests](https://img.shields.io/github/actions/workflow/status/kunterbunt2/project-hub/maven-build.yml?label=tests)](https://github.com/kunterbunt2/project-hub/actions/workflows/maven-build.yml)
[![codecov](https://codecov.io/github/kunterbunt2/project-hub/branch/main/graph/badge.svg)](https://codecov.io/github/kunterbunt2/project-hub)

# kassandra

tiny project management server.<br>
project effort estimation and progress tracking and release date interpolation open source server.

## features

tbd

[Requirements](https://github.com/kunterbunt2/project-hub/wiki/Requirements)

[Limitations](https://github.com/kunterbunt2/project-hub/wiki/Limitations)

[Design](https://github.com/kunterbunt2/project-hub/wiki/Design)

[ER Diagram](https://github.com/kunterbunt2/project-hub/wiki/ER-Diagram)

Roadmap

1. add logout button
2. add authorization
3. fix resource leveling not handling dependency to later story.
4. sprint view
   1. change status
   2. burn down work

# Screenshots

![gantt-03](https://raw.githubusercontent.com/wiki/kunterbunt2/project-hub/gantt/gantt_03-3-gant-chart.svg)

![burn-down-03](https://raw.githubusercontent.com/wiki/kunterbunt2/project-hub/burn-down/gantt_03-3-burn-down.svg)

![Christopher Paul.de.nw](https://raw.githubusercontent.com/wiki/kunterbunt2/project-hub/calendar/Christopher%20Paul.de.nw.svg)

# Design Philosophy

- As simple as possible, as complex as necessary.
- backup the development with unit tests.
- create data generators that can be used in unit tests.
- written in Java + spring boot + Vaadin.
- minimalistic project status tracking within one single server.
- simple local database, but keep option to switch to other databases.

# Ideas

- introduce ai summary for all projects.
- Take a look how jira is sending fields to the client and replicate if it is good.
- Projects can be locked for change, which will lock start/end dates and all milestones
- project priority can be changed by moving them within the list
- sprint priority can be changed by moving them within the list

# Issues

- sometimes adding a story and two tasks will add additionally one task.
- changing assignment must also change hidden dependencies
- some tests fail with java.awt.HeadlessException.
- some ai filter test fail all the time, as the tests are vague.
- users are retired by their name instead of their email address.
- user email addresses must be unique.
- LocationDialog not showing errors in dialog.
- AvailabilityTest.userSecurity() generates several exceptions on server side that the test does not catch.
- gantt calendar too light.
- gantt calendar should be using sprint calendar.

# License

[Apache License, version 2.0](https://github.com/kunterbunt2/project-hub/blob/main/LICENSE)
