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
import de.bushnaq.abdalla.util.date.DateUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.UnaryOperator;

@Slf4j
public class EntityGenerator {
    @Getter
    private final TreeSet<Availability> availabilities = new TreeSet<>();
    private final ColorGenerator        colorGenerator = new ColorGenerator();
    @Getter
    private       int                   featureIndex   = 0;
    @Getter
    private final List<Feature>         features       = new ArrayList<>();
    @Getter
    private final TreeSet<Location>     locations      = new TreeSet<>();
    private final NameGenerator         nameGenerator  = new NameGenerator();
    @Getter
    private       int                   productIndex   = 0;
    @Getter
    private final List<Product>         products       = new ArrayList<>();
    @Getter
    private       int                   sprintIndex    = 0;
    @Getter
    private final List<Sprint>          sprints        = new ArrayList<>();
    protected     LocalDate             startOfTime    = LocalDate.of(2000, 1, 1);
    @Getter
    private final List<Task>            tasks          = new ArrayList<>();
    @Getter
    private final TreeSet<User>         users          = new TreeSet<>();
    @Getter
    private       int                   versionIndex   = 0;
    @Getter
    private final List<Version>         versions       = new ArrayList<>();
    @Getter
    private final List<Worklog>         worklogs       = new ArrayList<>();

    protected Availability addAvailability(User user, float availability, LocalDate start) {
        return addAvailability(user, availability, start, UnaryOperator.identity());
    }

    protected Availability addAvailability(User user, float availability, LocalDate start, UnaryOperator<Availability> persister) {
        Availability a = new Availability(availability, start);
        a.setId(UUID.randomUUID());
        a.setCreated(user.getCreated());
        a.setUpdated(user.getUpdated());

        Availability saved = persister.apply(a);
//        if (saved.getUser() == null)
        saved.setUser(user);
        user.addAvailability(saved);
        availabilities.add(saved);
        return saved;
    }

    protected Feature addFeature(Version version, String name) {
        return addFeature(version, name, UnaryOperator.identity());
    }

    protected Feature addFeature(Version version, String name, UnaryOperator<Feature> persister) {
        Feature feature = new Feature();
        feature.setId(UUID.randomUUID());
        feature.setName(name);
        feature.setVersionId(version.getId());

        Feature saved = persister.apply(feature);
        saved.setVersion(version);
        version.addFeature(saved);
        features.add(saved);
        featureIndex = featureIndex + 1;
        return saved;
    }

    protected Location addLocation(User user, String country, String state, LocalDate start) {
        return addLocation(user, country, state, start, UnaryOperator.identity());
    }

    protected Location addLocation(User user, String country, String state, LocalDate start, UnaryOperator<Location> persister) {
        Location location = new Location(country, state, start);
        location.setId(UUID.randomUUID());
        location.setCreated(user.getCreated());
        location.setUpdated(user.getUpdated());

        Location saved = persister.apply(location);
        saved.setUser(user);
        user.addLocation(saved);
        locations.add(saved);
        return saved;
    }

    public Task addParentTask(String name, Sprint sprint, Task parent, Task dependency) {
        return addTask(sprint, parent, name, null, Duration.ofDays(0), null, null, dependency);
    }

    protected Product addProduct(String name) {
        return addProduct(name, UnaryOperator.identity());
    }

    protected Product addProduct(String name, UnaryOperator<Product> persister) {
        Product product = new Product();
        product.setName(name);
        product.setId(UUID.randomUUID());

        Product saved = persister.apply(product);

        products.add(saved);
        productIndex = productIndex + 1;
        return saved;
    }

    protected Sprint addSprint(Feature feature, String name) {
        return addSprint(feature, name, UnaryOperator.identity());
    }

    protected Sprint addSprint(Feature feature, String name, UnaryOperator<Sprint> persister) {
        Sprint sprint = new Sprint();
        sprint.setName(name);
        sprint.setStatus(Status.STARTED);
        sprint.setFeatureId(feature.getId());

        Sprint saved = persister.apply(sprint);
        saved.setFeature(feature);

        feature.addSprint(saved);
        sprints.add(saved);
        sprintIndex = sprintIndex + 1;
        return saved;
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
        return addTask(sprint, parent, name, start, minWork, maxWork, user, dependency, taskMode, milestone, UnaryOperator.identity());
    }

    public Task addTask(Sprint sprint, Task parent, String name, LocalDateTime start, Duration minWork, Duration maxWork, User user, Task dependency, TaskMode taskMode, boolean milestone, UnaryOperator<Task> persister) {
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setOrderId(getTasks().size());
        task.setName(name);
        task.setStart(start);
        if (minWork != null) {
            task.setMinEstimate(minWork);
            task.setRemainingEstimate(minWork);
        }
        if (maxWork != null && !maxWork.isZero()) {
            task.setMaxEstimate(maxWork);
        }
        //TODO do we need this?
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
            task.setSprintId(sprint.getId());
        }
        if (parent != null) {
            task.setParentTaskId(parent.getId());
        }

        Task saved = persister.apply(task);

        if (sprint != null) {
            saved.setSprint(sprint);
            sprint.addTask(saved);
        }
        if (parent != null) {
            saved.setParentTask(parent);
            parent.addChildTask(saved);
        }
        getTasks().add(saved);
        System.out.printf("Adding Task ID: %s, Task Name: %s resource id: %s%n", saved.getId(), saved.getName(), saved.getResourceId());
        return saved;
    }

    public User addUser(String name, float availability) {
        User user = new User();
        user.setName(name);
        user.setEmail(name);
        user.setColor(colorGenerator.generateUserColor(getUsers().size()));
        user.setId(UUID.randomUUID());
        addAvailability(user, availability, startOfTime);
        addLocation(user, "de", "nw", startOfTime);
        getUsers().add(user);
        return user;
    }

    protected Version addVersion(Product product, String versionName) {
        return addVersion(product, versionName, UnaryOperator.identity());
    }

    protected Version addVersion(Product product, String versionName, UnaryOperator<Version> persister) {
        Version version = new Version();
        version.setName(versionName);
        version.setProductId(product.getId());
        version.setId(UUID.randomUUID());

        Version saved = persister.apply(version);
        saved.setProduct(product);

        product.addVersion(saved);
        versions.add(saved);
        versionIndex++;
        return saved;
    }

    protected Worklog addWorklog(Task task, User user, OffsetDateTime start, Duration timeSpent, String comment) {
        Worklog worklog = new Worklog();
        worklog.setSprintId(task.getSprintId());
        worklog.setTaskId(task.getId());
        worklog.setAuthorId(user.getId());
        worklog.setStart(start);
        worklog.setTimeSpent(timeSpent);
        worklog.setTimeRemainingEstimate(task.getRemainingEstimate().minus(timeSpent));
        worklog.setComment(comment);
        worklog.setId(UUID.randomUUID());
        task.addWorklog(worklog);
        getWorklogs().add(worklog);
        return worklog;
    }

    public Task createDeliveryBufferTask(Sprint sprint, Duration minWork) {
        Task task = addTask(Task.DELIVERY_BUFFER, null, null, null, sprint, null, null);
        task.setImpactOnCost(false);//delivery buffer has no impact on cost
        task.setMinEstimate(minWork);

        return task;
    }

    public void init() {
        getAvailabilities().clear();
        getUsers().clear();
        getAvailabilities().clear();
        versionIndex = 0;
        getVersions().clear();
        productIndex = 0;
        getProducts().clear();
        featureIndex = 0;
        getFeatures().clear();
        sprintIndex = 0;
        getSprints().clear();
        getTasks().clear();
        getWorklogs().clear();
    }

}
