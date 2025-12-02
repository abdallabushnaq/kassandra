/*
 *
 * Copyright (C) 2025-2025 Abdalla Bushnaq
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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.GeneratedImageResult;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionException;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.dto.util.AvatarUtil;
import de.bushnaq.abdalla.kassandra.report.dao.GraphicsLightTheme;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttContext;
import de.bushnaq.abdalla.kassandra.rest.api.*;
import de.bushnaq.abdalla.profiler.Profiler;
import de.bushnaq.abdalla.profiler.SampleType;
import de.bushnaq.abdalla.util.date.DateUtil;
import jakarta.annotation.PostConstruct;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectCalendarException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ServerErrorException;

import java.awt.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AbstractEntityGenerator extends AbstractTestUtil {
    public static final String                 FIRST_OFF_DAY_FINISH_DATE = "2024-04-10";
    public static final String                 FIRST_OFF_DAY_START_DATE  = "2024-04-01";
    protected           AvailabilityApi        availabilityApi;
    protected final     TreeSet<Availability>  expectedAvailabilities    = new TreeSet<>();
    protected           List<Feature>          expectedFeatures          = new ArrayList<>();
    protected final     TreeSet<Location>      expectedLocations         = new TreeSet<>();
    protected           TreeSet<OffDay>        expectedOffDays           = new TreeSet<>();
    protected           List<Product>          expectedProducts          = new ArrayList<>();
    protected           List<Sprint>           expectedSprints           = new ArrayList<>();
    protected           List<Task>             expectedTasks             = new ArrayList<>();
    protected           TreeSet<User>          expectedUsers             = new TreeSet<>();
    protected           List<Version>          expectedVersions          = new ArrayList<>();
    protected           List<Worklog>          expectedWorklogs          = new ArrayList<>();
    protected           FeatureApi             featureApi;
    protected static    int                    featureIndex              = 0;
    protected           LocationApi            locationApi;
    protected           NameGenerator          nameGenerator             = new NameGenerator();
    @Autowired
    protected           ObjectMapper           objectMapper;
    protected           OffDayApi              offDayApi;
    private             int                    offDaysIterations;
    @LocalServerPort
    private             int                    port;
    protected           ProductApi             productApi;
    protected static    int                    productIndex              = 0;
    protected final     Random                 random                    = new Random();
    protected           SprintApi              sprintApi;
    private static      int                    sprintIndex               = 0;
    @Autowired
    protected           StableDiffusionService stableDiffusionService;
    protected           TaskApi                taskApi;
    @Autowired
    private             TestRestTemplate       testRestTemplate; // Use TestRestTemplate instead of RestTemplate
    protected           UserApi                userApi;
    protected static    int                    userIndex                 = 0;
    protected           VersionApi             versionApi;
    protected static    int                    versionIndex              = 0;
    protected           WorklogApi             worklogApi;

    protected void addAvailability(User user, float availability, LocalDate start) {
        Availability a = new Availability(availability, start);
        a.setUser(user);
        a.setCreated(user.getCreated());
        a.setUpdated(user.getUpdated());
        Availability saved = availabilityApi.persist(a, user.getId());
        user.addAvailability(saved);
        expectedAvailabilities.add(saved);
    }

    protected Feature addFeature(Version version, String name) {
        Feature feature = new Feature();
        feature.setName(name);

        feature.setVersion(version);
        feature.setVersionId(version.getId());
        feature.setCreated(ParameterOptions.getNow());
        feature.setUpdated(ParameterOptions.getNow());
        Feature saved = featureApi.persist(feature);
        expectedFeatures.add(saved);

        version.addFeature(saved);

        featureIndex++;
        return saved;
    }

    protected void addLocation(User user, String country, String state, LocalDate start) {
        Location location = new Location(country, state, start);
        location.setUser(user);
        location.setCreated(user.getCreated());
        location.setUpdated(user.getUpdated());
        Location saved = locationApi.persist(location, user.getId());
        user.addLocation(saved);
        expectedLocations.add(saved);
    }

    protected void addOffDay(User user, LocalDate offDayStart, LocalDate offDayFinish, OffDayType type) {
        OffDay offDay = new OffDay(offDayStart, offDayFinish, type);
        offDay.setUser(user);
        offDay.setCreated(user.getCreated());
        offDay.setUpdated(user.getUpdated());
        try (Profiler pc = new Profiler(SampleType.JPA)) {
            long   time  = System.currentTimeMillis();
            OffDay saved = offDayApi.persist(offDay, user.getId());
            user.addOffday(saved);
            expectedOffDays.add(saved);
            System.out.println("Adding off day: " + saved.getFirstDay() + " to user: " + user.getName() + " took " + (System.currentTimeMillis() - time) + " ms");
        }
        ProjectCalendarException vacation = user.getCalendar().addCalendarException(offDayStart, offDayFinish);
        switch (type) {
            case VACATION -> vacation.setName("vacation");
            case SICK -> vacation.setName("sick");
            case TRIP -> vacation.setName("trip");
        }
    }

    /**
     * Adds vacation blocks by splitting them when non-working days are encountered
     * Returns the number of actual vacation days used
     */
    private int addOffDayBlockWithSplitting(User user, ProjectCalendar calendar, LocalDate firstDate, LocalDate startDate, int workingDaysCount, OffDayType offDayType) {
        int       daysUsed     = 0;
        LocalDate currentStart = startDate;
        LocalDate currentDate  = startDate;
        boolean   inBlock      = false;

        while (daysUsed < workingDaysCount) {
            boolean isWorkingDay = calendar.isWorkingDate(currentDate);

            if (isWorkingDay) {
                if (!inBlock) {
                    // Start a new block
                    currentStart = currentDate;
                    inBlock      = true;
                }
                daysUsed++;

                // If we've used all the days, add the final block
                if (daysUsed >= workingDaysCount) {
                    addOffDay(user, currentStart, currentDate, offDayType);
//                    logger.info(String.format("%s %s %s", currentStart, currentDate, "vacation"));
                    break;
                }
            } else {
                // We hit a non-working day, end the current block if there is one
                if (inBlock) {
                    LocalDate blockEnd = currentDate.minusDays(1);
                    addOffDay(user, currentStart, blockEnd, offDayType);
//                    logger.info(String.format("%s %s %s", currentStart, blockEnd, "vacation"));
                    inBlock = false;
                }
            }

            currentDate = currentDate.plusDays(1);

            // Safety check to prevent infinite loops
            if (currentDate.isAfter(startDate.plusYears(1))) {
                if (inBlock) {
                    // End any remaining block
                    LocalDate blockEnd = currentDate.minusDays(1);
                    addOffDay(user, currentStart, blockEnd, offDayType);
//                    logger.info(String.format("%s %s %s", currentStart, blockEnd, "vacation"));
                }
                break;
            }
        }

        return daysUsed;
    }

    private void addOffDays(User saved, LocalDate firstDate, int annualVacationDays, int year, OffDayType offDayType, int summerDurationMin, int summerDurationMax) {
        int             remainingDays = annualVacationDays;
        ProjectCalendar pc            = saved.getCalendar();

        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd   = yearStart.plusYears(1).minusDays(1);

        // First add a longer summer vacation block (2-4 weeks)
        int       summerStart         = random.nextInt(60) + 150; // Random start between day 150-210 (June-July)
        LocalDate summerVacationStart = pc.getNextWorkStart(yearStart.plusDays(summerStart).atStartOfDay()).toLocalDate();
        int       summerDuration      = random.nextInt(summerDurationMax - summerDurationMin + 1) + summerDurationMin; // 10-20 days (2-4 weeks)
        summerDuration = Math.min(summerDuration, remainingDays);

        // Add summer vacation with proper splitting of non-working days
        int daysUsed = addOffDayBlockWithSplitting(saved, pc, firstDate, summerVacationStart, summerDuration, offDayType);
        remainingDays -= daysUsed;

        // Distribute remaining days throughout the year in smaller blocks
        while (remainingDays > 0) {
            offDaysIterations++;
            int       blockDuration = Math.min(remainingDays, random.nextInt(4) + 3); // 3-6 days blocks
            LocalDate startDate;

            do {
                int dayOffset = random.nextInt(365); // Random day in the year
                startDate = pc.getNextWorkStart(yearStart.plusDays(dayOffset).atStartOfDay()).toLocalDate();
            } while (startDate.isAfter(yearEnd) || isOverlapping(saved.getOffDays(), startDate, startDate.plusDays(blockDuration)));

            if (!startDate.isAfter(yearEnd)) {
                // Add vacation block with proper splitting of non-working days
                int actualDaysUsed = addOffDayBlockWithSplitting(saved, pc, firstDate, startDate, blockDuration, offDayType);
                remainingDays -= actualDaysUsed;
            }
        }
    }

    protected Task addParentTask(String name, Sprint sprint, Task parent, Task dependency) {
        return addTask(sprint, parent, name, null, Duration.ofDays(0), null, null, dependency);
    }

    protected Product addProduct(String name) {
        Product product = new Product();
        product.setName(name);
        product.setCreated(ParameterOptions.getNow());
        product.setUpdated(ParameterOptions.getNow());

        Product saved = null;
        // Generate AI image for the product if service is available
        if (stableDiffusionService != null && stableDiffusionService.isAvailable()) {
            try {
                String prompt = "Icon representing " + name + ", minimalist, 3D object on a white background, optimized for 32px x 32px size";
                System.out.println("Generating image for product: " + name + " with prompt: " + prompt);
                long                 startTime = System.currentTimeMillis();
                GeneratedImageResult image     = stableDiffusionService.generateImageWithOriginal(prompt);
                product.setAvatarHash(AvatarUtil.computeHash(image.getResizedImage()));
                saved = productApi.persist(product);
                productApi.updateAvatarFull(saved.getId(), image.getResizedImage(), image.getOriginalImage(), image.getPrompt());
                System.out.println("Generated image for product: " + name + " in " + (System.currentTimeMillis() - startTime) + " ms");
            } catch (StableDiffusionException e) {
                System.err.println("Failed to generate image for product " + name + ": " + e.getMessage());
                // Continue without image
            }
        } else {
            saved = productApi.persist(product);
        }

        expectedProducts.add(saved);
        productIndex++;
        return saved;
    }

    protected Feature addRandomFeature(Version version) {
        return addFeature(version, nameGenerator.generateFeatureName(featureIndex));
    }

    protected void addRandomProducts(int count) {
        User user1 = addRandomUser();

        for (int i = 0; i < count; i++) {
            Product product = addProduct(nameGenerator.generateProductName(productIndex));
            Version version = addVersion(product, String.format("1.%d.0", i));
            Feature feature = addRandomFeature(version);
            Sprint  sprint  = addRandomSprint(feature);
            Task    task1   = addTask(sprint, null, "Project Phase 1", LocalDateTime.now(), Duration.ofDays(10), null, null, null);
            Task    task2   = addTask(sprint, task1, "Design", LocalDateTime.now(), Duration.ofDays(4), null, user1, null);
            Task    task3   = addTask(sprint, task2, "Implementation", LocalDateTime.now().plusDays(4), Duration.ofDays(6), null, user1, task1);
        }
        testProducts();
    }

    protected Sprint addRandomSprint(Feature feature) {
        return addSprint(feature, nameGenerator.generateSprintName(sprintIndex));
    }

    /**
     * Adds a random user with a random name and email address and location in de/nw.
     * The user is initialized with a GanttContext and has a vacation off day.
     * The user first working day is set to the given date.
     *
     * @return the created User object
     */
    protected User addRandomUser(LocalDate firstDate) {
        String       name  = nameGenerator.generateUserName(userIndex);
        String       email = nameGenerator.generateUserEmail(userIndex);
        User         saved = addUser(name, email, "de", "nw", firstDate, generateUserColor(userIndex), 0.7f);
        GanttContext gc    = new GanttContext();
        gc.initialize();
        saved.initialize(gc);
        addOffDay(saved, LocalDate.parse(FIRST_OFF_DAY_START_DATE), LocalDate.parse(FIRST_OFF_DAY_FINISH_DATE), OffDayType.VACATION);
        return saved;
    }

    /**
     * Adds a random user with a random name and email address and location in de/nw.
     * The user is initialized with a GanttContext and has a vacation off day.
     * The user first working day is set to ParameterOptions.now.
     *
     * @return the created User object
     */
    protected User addRandomUser() {
        String    name      = nameGenerator.generateUserName(userIndex);
        String    email     = nameGenerator.generateUserEmail(userIndex);
        LocalDate firstDate = ParameterOptions.getNow().toLocalDate();

        User         saved = addUser(name, email, "de", "nw", firstDate, generateUserColor(userIndex), 0.7f, LocalDate.parse(FIRST_OFF_DAY_START_DATE), LocalDate.parse(FIRST_OFF_DAY_FINISH_DATE), OffDayType.VACATION);
        GanttContext gc    = new GanttContext();
        gc.initialize();
        saved.initialize(gc);
        generateRandomOffDays(saved, firstDate);
        testUsers();
        return saved;
    }

    /**
     * Adds index random user with a random name and email address and location in de/nw.
     * The user is initialized with a GanttContext and has a vacation off day.
     * The user first working day is set to ParameterOptions.now.
     * The user availability is set to the given value.
     *
     * @return the created User object
     */
    protected User addRandomUser(int index, float availability) {
        String       name      = nameGenerator.generateUserName(index);
        String       email     = nameGenerator.generateUserEmail(userIndex);
        LocalDate    firstDate = ParameterOptions.getNow().toLocalDate().minusYears(1);
        User         saved     = addUser(name, email, "de", "nw", firstDate, generateUserColor(userIndex), availability);
        GanttContext gc        = new GanttContext();
        gc.initialize();
        saved.initialize(gc);
        generateRandomOffDays(saved, firstDate);
        testUsers();
        return saved;
    }

    /**
     * Adds the given number of random users with a random name and email address and location in de/nw.
     * The users are initialized with a GanttContext and have a vacation off day.
     * The users first working day is set to ParameterOptions.now.
     *
     * @param count the number of users to add
     */
    protected void addRandomUsers(int count) {
        for (int i = 0; i < count; i++) {
            long      time      = System.currentTimeMillis();
            String    name      = nameGenerator.generateUserName(userIndex);
            String    email     = nameGenerator.generateUserEmail(userIndex);
            LocalDate firstDate = ParameterOptions.getNow().toLocalDate().minusYears(2);
            User      saved     = addUser(name, email, "de", "nw", firstDate, generateUserColor(userIndex), 0.5f);
            System.out.println("Adding user: " + saved.getName() + " took " + (System.currentTimeMillis() - time) + " ms");
//            GanttContext gc        = new GanttContext();
//            gc.initialize();
//            saved.initialize(gc);
            saved.initialize();
            time = System.currentTimeMillis();
            generateRandomOffDays(saved, firstDate);
            Profiler.log("generateRandomOffDays");
            System.out.println("Adding off days for user: " + saved.getName() + " took " + (System.currentTimeMillis() - time) + " ms, and %d" + offDaysIterations + "iterations");
        }

        testUsers();
    }

    /**
     * Adds a random version with the given name to the given product.
     *
     * @param product the product to add the version to
     * @return the created Version object
     */
    protected Version addRandomVersion(Product product) {
        return addVersion(product, nameGenerator.generateVersionName(versionIndex));
    }

    protected Sprint addSprint(Feature feature, String sprintName) {
        Sprint sprint = new Sprint();
        sprint.setName(sprintName);
        sprint.setStatus(Status.STARTED);
        sprint.setFeature(feature);
        sprint.setFeatureId(feature.getId());
        sprint.setCreated(ParameterOptions.getNow());
        sprint.setUpdated(ParameterOptions.getNow());
        Sprint saved = sprintApi.persist(sprint);
        expectedSprints.add(saved);
        feature.addSprint(saved);

        sprintIndex++;
        return saved;
    }

    protected Task addTask(String name, String minWorkString, String maxWorkString, User user, Sprint sprint, Task parent, Task dependency) {
        return addTask(sprint, parent, name, null, DateUtil.parseWorkDayDurationString(minWorkString), DateUtil.parseWorkDayDurationString(maxWorkString), user, dependency, null, false);
    }

    protected Task addTask(Sprint sprint, Task parent, String name, LocalDateTime start, Duration minWork, Duration maxWork, User user, Task dependency) {
        return addTask(sprint, parent, name, start, minWork, maxWork, user, dependency, null, false);
    }

    protected Task addTask(Sprint sprint, Task parent, String name, LocalDateTime start, Duration minWork, Duration maxWork, User user, Task dependency, TaskMode taskMode, boolean milestone) {
        Task task = new Task();
        task.setName(name);
        task.setStart(start);
        if (minWork != null) {
            task.setMinEstimate(minWork);
            task.setOriginalEstimate(minWork);
            task.setRemainingEstimate(minWork);
        }
        if (maxWork != null && !maxWork.isZero()) {
            task.setMaxEstimate(maxWork);
        }
//        if (minWork == null || minWork.equals(Duration.ZERO)) {
//            task.setFinish(start);
//        }
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
        System.out.printf("trying to add %s%n", task);

        Task saved = taskApi.persist(task);
        expectedTasks.add(saved);
        if (parent != null) {
            parent.addChildTask(saved);
        }
        if (sprint != null) {
            saved.setSprint(sprint);
            sprint.addTask(saved);
        }
        System.out.printf("Adding %s%n", saved.toString());
        return saved;
    }

    protected User addUser(String name, String email, String country, String state, LocalDate start, Color color, float availability) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setFirstWorkingDay(start);
        user.setColor(color);
        user.setCreated(DateUtil.localDateToOffsetDateTime(start).plusHours(8));
        user.setUpdated(DateUtil.localDateToOffsetDateTime(start).plusHours(8));

        User saved = null;
        // Generate AI image for the user if service is available
        if (stableDiffusionService != null && stableDiffusionService.isAvailable()) {
            try {
//                String prompt = "Professional avatar portrait of " + name + ", minimalist, flat design, simple background, icon style";
//                System.out.println("Generating avatar for user: " + name + " with prompt: " + prompt);
                long startTime = System.currentTimeMillis();
//                GeneratedImageResult image     = stableDiffusionService.generateImageWithOriginal(prompt);
                GeneratedImageResult image = generateUserAvatar(name);
                user.setAvatarHash(AvatarUtil.computeHash(image.getResizedImage()));
                saved = userApi.persist(user);
                addLocation(saved, country, state, start);
                addAvailability(saved, availability, start);
                userApi.updateAvatarFull(saved.getId(), image.getResizedImage(), image.getOriginalImage(), image.getPrompt());
                System.out.println("Generated avatar for user: " + name + " in " + (System.currentTimeMillis() - startTime) + " ms");
            } catch (StableDiffusionException e) {
                System.err.println("Failed to generate avatar for user " + name + ": " + e.getMessage());
                // Continue without image
            }
        } else {
            saved = userApi.persist(user);
            addLocation(saved, country, state, start);
            addAvailability(saved, availability, start);
        }

        userIndex++;
        expectedUsers.add(saved);
        return saved;

    }

    protected User addUser(String name, String email, String country, String state, LocalDate start, Color color, float availability, LocalDate offDayStart, LocalDate offDayFinish, OffDayType offDayType) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setFirstWorkingDay(start);
        user.setColor(color);
        user.setCreated(DateUtil.localDateToOffsetDateTime(start).plusHours(8));
        user.setUpdated(DateUtil.localDateToOffsetDateTime(start).plusHours(8));

        User saved = userApi.persist(user);
        addLocation(saved, country, state, start);
        addAvailability(saved, availability, start);

        // Generate AI image for the user if service is available
        if (stableDiffusionService != null && stableDiffusionService.isAvailable()) {
            try {
                long                 startTime = System.currentTimeMillis();
                GeneratedImageResult image     = generateUserAvatar(name);
                userApi.updateAvatarFull(user.getId(), image.getResizedImage(), image.getOriginalImage(), image.getPrompt());
                System.out.println("Generated avatar for user: " + name + " in " + (System.currentTimeMillis() - startTime) + " ms");
            } catch (StableDiffusionException e) {
                System.err.println("Failed to generate avatar for user " + name + ": " + e.getMessage());
                // Continue without image
            }
        }


        userIndex++;
        expectedUsers.add(saved);
        return saved;

    }

    protected Version addVersion(Product product, String versionName) {
        Version version = new Version();
        version.setName(versionName);
        version.setProduct(product);
        version.setProductId(product.getId());
        version.setCreated(ParameterOptions.getNow());
        version.setUpdated(ParameterOptions.getNow());
        Version saved = versionApi.persist(version);
        product.addVersion(saved);
        expectedVersions.add(saved);
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
        worklog.setComment(comment);
        task.addWorklog(worklog);
        Worklog saved = worklogApi.persist(worklog);
        expectedWorklogs.add(saved);
        return saved;
    }

    @BeforeEach
    protected void beforeEach(TestInfo testInfo) {
        super.beforeEach(testInfo);
        productIndex = 0;
        featureIndex = 0;
        sprintIndex  = 0;
        userIndex    = 0;
        versionIndex = 0;
        nameGenerator.resetStoryPool(); // Reset story pool for each test
    }

    private void generateRandomOffDays(User saved, LocalDate employmentDate) {
        try (Profiler pc = new Profiler(SampleType.CPU)) {

            int employmentYear = employmentDate.getYear();
            offDaysIterations = 0;
            for (int yearIndex = 0; yearIndex < 2; yearIndex++) {
                int year = employmentYear + yearIndex;
                random.setSeed(generateUserYearSeed(saved, year));
                addOffDays(saved, employmentDate, 30, year, OffDayType.VACATION, 10, 20);
                addOffDays(saved, employmentDate, random.nextInt(20), year, OffDayType.SICK, 1, 5);
                addOffDays(saved, employmentDate, random.nextInt(5), year, OffDayType.TRIP, 1, 5);
            }
        }
    }

    private @NotNull GeneratedImageResult generateUserAvatar(String name) throws StableDiffusionException {
        String prompt = "Professional avatar portrait of " + name + ", minimalist, flat design, simple background, icon style";
        System.out.println("Generating avatar for user: " + name + " with prompt: " + prompt);
        return stableDiffusionService.generateImageWithOriginal(prompt);
    }

    protected Color generateUserColor(int userIndex) {
        int index = userIndex % GraphicsLightTheme.KELLY_COLORS.length;
        return GraphicsLightTheme.KELLY_COLORS[index];
    }

    private static int generateUserYearSeed(User saved, int year) {
        return (saved.getName() + year).hashCode();
    }

    protected int getCurrentSprintIndex() {
        return sprintIndex;
    }

    private LocalDate getNextWorkingDay(ProjectCalendar calendar, LocalDate start, int workingDays) {
        LocalDate current   = start;
        int       daysCount = 0;

        while (daysCount < workingDays) {
            current = current.plusDays(1);
            if (calendar.isWorkingDate(current)) {
                daysCount++;
            }
        }

        return current;
    }

    private boolean isOverlapping(List<OffDay> offDays, LocalDate start, LocalDate end) {
        return offDays.stream().anyMatch(offDay -> !(end.isBefore(offDay.getFirstDay()) || start.isAfter(offDay.getLastDay())));
    }

    /**
     * Move task from its parent to newParent
     *
     * @param task      the task to move
     * @param newParent the new parent
     */
    protected void move(Sprint sprint, Task task, Task newParent) {
        Task oldParent = task.getParentTask();
        newParent.addChildTask(task);

        taskApi.persist(newParent);
        taskApi.persist(task);
        taskApi.persist(oldParent);
    }

    @PostConstruct
    protected void postConstruct() {
        ParameterOptions.setNow(OffsetDateTime.parse("2025-05-05T08:00:00+01:00"));

        // Set the correct port after injection
        String baseUrl = "http://localhost:" + port + "/api";
        productApi      = new ProductApi(testRestTemplate.getRestTemplate(), objectMapper, baseUrl);
        featureApi      = new FeatureApi(testRestTemplate.getRestTemplate(), objectMapper, baseUrl);
        userApi         = new UserApi(testRestTemplate.getRestTemplate(), objectMapper, baseUrl);
        availabilityApi = new AvailabilityApi(testRestTemplate.getRestTemplate(), objectMapper, baseUrl);
        locationApi     = new LocationApi(testRestTemplate.getRestTemplate(), objectMapper, baseUrl);
        offDayApi       = new OffDayApi(testRestTemplate.getRestTemplate(), objectMapper, baseUrl);
        taskApi         = new TaskApi(testRestTemplate.getRestTemplate(), objectMapper, baseUrl);
        versionApi      = new VersionApi(testRestTemplate.getRestTemplate(), objectMapper, baseUrl);
        sprintApi       = new SprintApi(testRestTemplate.getRestTemplate(), objectMapper, baseUrl);
        worklogApi      = new WorklogApi(testRestTemplate.getRestTemplate(), objectMapper, baseUrl);
    }

    protected void removeAvailability(Availability availability, User user) {
        availabilityApi.deleteById(user, availability);
        user.removeAvailability(availability);
        expectedAvailabilities.remove(availability);
    }

    protected void removeFeature(Long id) {
        Feature featureToRemove = expectedFeatures.stream().filter(project -> project.getId().equals(id)).findFirst().orElse(null);
        featureApi.deleteById(id);
        if (featureToRemove != null) {
            //remove this project from its parent version
            featureToRemove.getVersion().removeProject(featureToRemove);
            // Remove all sprints and their tasks
            for (Sprint sprint : featureToRemove.getSprints()) {
                for (Task task : sprint.getTasks()) {
                    expectedTasks.remove(task);
                }
                expectedSprints.remove(sprint);
            }
            expectedFeatures.remove(featureToRemove);
        }
    }

    protected void removeLocation(Location location, User user) {
        locationApi.deleteById(user, location);
        user.removeLocation(location);
        expectedLocations.remove(location);
    }

    protected void removeOffDay(OffDay offDay, User user) {
        offDayApi.deleteById(user, offDay);
        user.removeOffDay(offDay);
        expectedOffDays.remove(offDay);
    }

    protected void removeProduct(Long id) {
        Product productToRemove = expectedProducts.stream().filter(product -> product.getId().equals(id)).findFirst().orElse(null);
        productApi.deleteById(id);

        if (productToRemove != null) {
            // Remove all versions and their projects, sprints, and tasks
            for (Version version : productToRemove.getVersions()) {
                for (Feature feature : version.getFeatures()) {
                    for (Sprint sprint : feature.getSprints()) {
                        for (Task task : sprint.getTasks()) {
                            expectedTasks.remove(task);
                        }
                        expectedSprints.remove(sprint);
                    }
                    expectedFeatures.remove(feature);
                }
                expectedVersions.remove(version);
            }
            expectedProducts.remove(productToRemove);
        }
    }

    protected void removeSprint(Long id) {
        Sprint sprintToRemove = expectedSprints.stream().filter(sprint -> sprint.getId().equals(id)).findFirst().orElse(null);
        sprintApi.deleteById(id);
        if (sprintToRemove != null) {
            //remove this sprint from its parent project
            sprintToRemove.getFeature().removePrint(sprintToRemove);
            for (Task task : sprintToRemove.getTasks()) {
                expectedTasks.remove(task);
            }
            expectedSprints.remove(sprintToRemove);
        }
    }

    protected void removeTaskTree(Task task) {
        expectedTasks.remove(task);
        taskApi.deleteById(task.getId());
        for (Task childTask : task.getChildTasks()) {
            removeTaskTree(childTask);
        }
    }

    protected void removeUser(Long id) {
        User userToRemove = expectedUsers.stream().filter(user -> user.getId().equals(id)).findFirst().orElse(null);
        userApi.deleteById(id);

        if (userToRemove != null) {
            // Remove all availabilities
            expectedAvailabilities.removeAll(userToRemove.getAvailabilities());
            // Remove all locations
            expectedLocations.removeAll(userToRemove.getLocations());
            // Remove all off days
            expectedOffDays.removeAll(userToRemove.getOffDays());
            // Remove the user
            expectedUsers.remove(userToRemove);
        }

    }

    protected void removeVersion(Long id) {
        Version versionToRemove = expectedVersions.stream().filter(version -> version.getId().equals(id)).findFirst().orElse(null);
        versionApi.deleteById(id);

        if (versionToRemove != null) {
            //remove this version from its parent product
            versionToRemove.getProduct().removeVersion(versionToRemove);
            // Remove all projects, sprints, and tasks
            for (Feature feature : versionToRemove.getFeatures()) {
                for (Sprint sprint : feature.getSprints()) {
                    for (Task task : sprint.getTasks()) {
                        expectedTasks.remove(task);
                    }
                    expectedSprints.remove(sprint);
                }
                expectedFeatures.remove(feature);
            }
            expectedVersions.remove(versionToRemove);
        }
    }

    protected static void setUser(String principal, String ROLE_ADMIN) {
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal, "password",
                        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(ROLE_ADMIN))
                )
        );
    }

    protected void testAll() {
        testProducts();
        testUsers();
    }

    @AfterEach
    protected void testAllAndPrintTables() {
        setUser("admin-user", "ROLE_ADMIN");
        testAll();
        printTables();
    }

    /**
     * ensure products in db match our expectations
     */
    protected void testProducts() {
        GanttContext gc = new GanttContext();
        gc.allUsers    = userApi.getAll().stream().sorted().toList();
        gc.allProducts = productApi.getAll().stream().sorted().toList();
        gc.allVersions = versionApi.getAll().stream().sorted().toList();
        gc.allFeatures = featureApi.getAll().stream().sorted().toList();
        gc.allSprints  = sprintApi.getAll().stream().sorted().toList();
        for (Sprint sprint : gc.allSprints) {
            sprint.setWorklogs(worklogApi.getAll(sprint.getId()).stream().sorted().toList());
        }
        gc.allTasks = taskApi.getAll().stream().sorted().toList();
        gc.initialize();

        expectedProducts.sort(Comparator.naturalOrder());
        expectedFeatures.sort(Comparator.naturalOrder());
        expectedSprints.sort(Comparator.naturalOrder());
        expectedTasks.sort(Comparator.naturalOrder());
        expectedVersions.sort(Comparator.naturalOrder());
        expectedWorklogs.sort(Comparator.naturalOrder());

        assertEquals(expectedProducts.size(), gc.allProducts.size());
        for (int i = 0; i < expectedProducts.size(); i++) {
            assertProductEquals(expectedProducts.get(i), gc.allProducts.get(i));
        }
    }

    protected void testUsers() {
        entityManager.clear();//clear the cache to get the latest data from the database
        List<User> actual = userApi.getAll();

        assertEquals(expectedUsers.size(), actual.size());
        int i = 0;
        for (User expectedUser : expectedUsers) {
            assertUserEquals(expectedUser, actual.get(i++));
        }
    }

    protected void updateAvailability(Availability availability, User user) {
        availabilityApi.update(availability, user.getId());
        expectedAvailabilities.remove(availability);
        expectedAvailabilities.add(availability);
    }

    protected void updateFeature(Feature feature) {
        featureApi.update(feature);
        expectedFeatures.remove(feature);
        expectedFeatures.add(feature);//replace old products with the updated one
    }

    protected void updateLocation(Location location, User user) throws ServerErrorException {
        locationApi.update(location, user.getId());
        expectedLocations.remove(location);
        expectedLocations.add(location);
    }

    protected void updateOffDay(OffDay offDay, User user) throws ServerErrorException {
        offDayApi.update(offDay, user.getId());
        expectedOffDays.remove(offDay);
        expectedOffDays.add(offDay);
    }

    protected void updateProduct(Product product) throws ServerErrorException {
        productApi.update(product);
        expectedProducts.remove(product);
        expectedProducts.add(product);//replace old products with the updated one
    }

    protected void updateSprint(Sprint sprint) throws ServerErrorException {
        sprintApi.update(sprint);
        expectedSprints.remove(sprint);
        expectedSprints.add(sprint); // Replace old sprint with the updated one
    }

    protected void updateTask(Task task) {
        taskApi.update(task);
        expectedTasks.remove(task);
        expectedTasks.add(task);//replace old products with the updated one
    }

    protected void updateUser(User user) throws ServerErrorException {
        userApi.update(user);
        expectedUsers.remove(user);
        expectedUsers.add(user);//replace old user with the updated one
    }

    protected void updateVersion(Version version) {
        versionApi.update(version);
        expectedVersions.remove(version);
        expectedVersions.add(version);//replace old products with the updated one
    }

}
