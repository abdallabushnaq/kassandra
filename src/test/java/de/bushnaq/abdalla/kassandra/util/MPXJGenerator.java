/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.util;

import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.report.gantt.ColorGenerator;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttUtil;
import de.bushnaq.abdalla.util.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MPXJGenerator {
    private final   ColorGenerator  colorGenerator = new ColorGenerator();
    protected final List<Throwable> exceptions     = new ArrayList<>();
    protected       List<Feature>   features       = new ArrayList<>();
    protected       JsonMapper      jsonMapper     = new JsonMapper();
    private final   NameGenerator   nameGenerator  = new NameGenerator();
    protected       List<Product>   products       = new ArrayList<>();
    protected       List<Sprint>    sprints        = new ArrayList<>();
    protected       LocalDate       startOfTime    = LocalDate.of(2000, 1, 1);
    protected       List<Task>      tasks          = new ArrayList<>();
    protected       List<User>      users          = new ArrayList<>();
    protected       List<Version>   versions       = new ArrayList<>();
    protected       List<Worklog>   worklogs       = new ArrayList<>();

    protected void addAvailability(User user, float availability, LocalDate start) {
        Availability a = new Availability(availability, start);
        a.setUser(user);
        user.addAvailability(a);
    }

    protected Feature addFeature(Version version, String name) {
        Feature feature = new Feature();
        feature.setName(name);
        feature.setVersion(version);
        feature.setVersionId(version.getId());
        feature.setId((long) features.size());
        version.addFeature(feature);
        features.add(feature);
        return feature;
    }

    protected void addLocation(User user, String country, String state, LocalDate start) {
        Location location = new Location(country, state, start);
        location.setUser(user);
        location.setCreated(user.getCreated());
        location.setUpdated(user.getUpdated());
        user.addLocation(location);
    }

    public Task addParentTask(String name, Sprint sprint, Task parent, Task dependency) {
        return addTask(sprint, parent, name, null, Duration.ofDays(0), null, null, dependency);
    }

    protected Product addProduct(String name) {
        Product product = new Product();
        product.setName(name);
        product.setId((long) products.size());
        products.add(product);
        return product;
    }

    protected Sprint addSprint(Feature feature, String name) {
        Sprint sprint = new Sprint();
        sprint.setName(name);
        sprint.setStatus(Status.STARTED);
        sprint.setFeature(feature);
        sprint.setFeatureId(feature.getId());
        sprint.setId((long) sprints.size());
        feature.addSprint(sprint);
        sprints.add(sprint);
        return sprint;
    }

    public Sprint addSprint() {
        Product product = addProduct(nameGenerator.generateProductName(1));
        Version version = addVersion(product, nameGenerator.generateVersionName(1));
        Feature feature = addFeature(version, nameGenerator.generateFeatureName(1));
        Sprint  sprint  = addSprint(feature, nameGenerator.generateSprintName(1));
        return sprint;
    }

    public Task addTask(String name, String minWorkString, String maxWorkString, User user, Sprint sprint, Task parent, Task dependency) {
        return addTask(sprint, parent, name, null, DateUtil.parseWorkDayDurationString(minWorkString), DateUtil.parseWorkDayDurationString(maxWorkString), user, dependency, null, false);
    }

    public Task addTask(Sprint sprint, Task parent, String name, LocalDateTime start, Duration minWork, Duration maxWork, User user, Task dependency) {
        return addTask(sprint, parent, name, start, minWork, maxWork, user, dependency, null, false);
    }

    public Task addTask(Sprint sprint, Task parent, String name, LocalDateTime start, Duration minWork, Duration maxWork, User user, Task dependency, TaskMode taskMode, boolean milestone) {
        Task task = new Task();
        task.setName(name);
        task.setStart(start);
        if (minWork != null) {
            task.setMinEstimate(minWork);
            task.setRemainingEstimate(minWork);
        }
        if (maxWork != null && !maxWork.isZero()) {
            task.setMaxEstimate(maxWork);
        }
        if (minWork == null || minWork.equals(Duration.ZERO)) {
            task.setFinish(start);
        }
        if (taskMode != null) {
            task.setTaskMode(taskMode);
        }
        task.setMilestone(milestone);
        if (user != null) {
            task.setResourceId(user.getId());
        }
        if (dependency != null) {
            task.addPredecessor(dependency, true);
        }
        if (sprint != null) {
            task.setSprint(sprint);
            task.setSprintId(sprint.getId());
        }
        if (parent != null) {
            task.setParentTask(parent);
            task.setParentTaskId(parent.getId());
        }
        // Save the task
//        System.out.printf("trying to add %s%n", task);

        if (parent != null) {
            parent.addChildTask(task);
        }
        if (sprint != null) {
            task.setSprint(sprint);
            sprint.addTask(task);
        }
        task.setId((long) tasks.size());
        task.setOrderId(tasks.size());
        tasks.add(task);
//        System.out.printf("Adding %s%n", task);
        System.out.printf("Task ID: %s, Task Name: %s resource id: %d%n", task.getId(), task.getName(), task.getResourceId());
        return task;
    }

    public User addUser(String name, float availability) {
        User user = new User();
        user.setName(name);
        user.setEmail(name);
        user.setColor(colorGenerator.generateUserColor(users.size()));
        user.setId((long) users.size());
        addAvailability(user, availability, startOfTime);
        addLocation(user, "de", "nw", startOfTime);
        users.add(user);
        return user;
    }

    protected Version addVersion(Product product, String versionName) {
        Version version = new Version();
        version.setName(versionName);
        version.setProduct(product);
        version.setProductId(product.getId());
        version.setId((long) versions.size());
        product.addVersion(version);
        versions.add(version);
        return version;
    }

    protected Worklog addWorklog(Task task, User user, OffsetDateTime start, Duration timeSpent, String comment) {
        Worklog worklog = new Worklog();
        worklog.setSprintId(task.getSprintId());
        worklog.setTaskId(task.getId());
        worklog.setAuthorId(user.getId());
        worklog.setStart(start);
        worklog.setTimeSpent(timeSpent);
        worklog.setComment(comment);
        worklog.setId((long) worklogs.size());
        task.addWorklog(worklog);
//        Worklog saved = worklogApi.persist(worklog);
        worklogs.add(worklog);
        return worklog;
    }

    //    protected Task addParentTask(String name, User user, Sprint sprint, Task parent, Task dependency) {
//        return addTask(sprint, parent, name, null, Duration.ofDays(0), null, user, dependency);
//    }
    public Task createDeliveryBufferTask(Sprint sprint, Duration minWork) {
        //create the buffer task
        Task task = new Task();
        task.setName(GanttUtil.DELIVERY_BUFFER);
        task.setImpactOnCost(false);//delivery buffer has no impact on cost
        if (sprint != null) {
            task.setSprint(sprint);
            task.setSprintId(sprint.getId());
        }
        task.setMinEstimate(minWork);

        task.setId((long) tasks.size());
        if (sprint != null) {
            task.setSprint(sprint);
            sprint.addTask(task);
        }
//        for (Task story : sprint.getTasks()) {
//            if (story.isStory()) {
//                //create hidden dependency to every story of this sprint, so that this buffer is the last task in the sprint
//                if (!GanttUtil.hasDependency(task, story)) {
//                    task.addPredecessor(story, true);
//                }
//            }
//        }
        return task;
    }


}
