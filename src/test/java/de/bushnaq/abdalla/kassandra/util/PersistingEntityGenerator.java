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

import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.AvatarService;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.GeneratedImageResult;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.config.DefaultEntitiesInitializer;
import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.dto.util.AvatarUtil;
import de.bushnaq.abdalla.kassandra.report.dao.theme.LightTheme;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttContext;
import de.bushnaq.abdalla.kassandra.rest.api.*;
import de.bushnaq.abdalla.profiler.Profiler;
import de.bushnaq.abdalla.profiler.SampleType;
import de.bushnaq.abdalla.util.date.DateUtil;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectCalendarException;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ServerErrorException;
import tools.jackson.databind.json.JsonMapper;

import java.awt.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.List;
import java.util.function.UnaryOperator;

@Slf4j
@Component
@ActiveProfiles("test")
public class PersistingEntityGenerator {
    public static final String                 FIRST_OFF_DAY_FINISH_DATE = "2024-04-10";
    public static final String                 FIRST_OFF_DAY_START_DATE  = "2024-04-01";
    public              AvailabilityApi        availabilityApi;
    @Autowired
    protected           AvatarService          avatarService;
    public final        EntityGenerator        eg                        = new EntityGenerator();
    @Autowired
    protected           EntityManager          entityManager;
    public              FeatureApi             featureApi;
    @Autowired
    protected           JsonMapper             jsonMapper;
    public              LocationApi            locationApi;
    public              NameGenerator          nameGenerator             = new NameGenerator();
    protected           OffDayApi              offDayApi;
    private final       List<OffDay>           offDayBuffer              = new ArrayList<>();
    private             TreeSet<OffDay>        offDays                   = new TreeSet<>();
    private             int                    offDaysIterations;
    public              ProductAclApi          productAclApi;
    public              ProductApi             productApi;
    public final        Random                 random                    = new Random();
    public              SprintApi              sprintApi;
    @Autowired
    protected           StableDiffusionService stableDiffusionService;
    public              TaskApi                taskApi;
    @Autowired
    private             TestRestTemplate       testRestTemplate; // Use TestRestTemplate instead of RestTemplate
    public              UserApi                userApi;
    public              UserGroupApi           userGroupApi;
    public              UserWorkWeekApi        userWorkWeekApi;
    public              VersionApi             versionApi;
    public              WorkWeekApi            workWeekApi;
    public              WorklogApi             worklogApi;
    private final       List<Worklog>          worklogBuffer             = new ArrayList<>();

    public Availability addAvailability(User user, float availability, LocalDate start) {
        return eg.addAvailability(user, availability, start, a -> availabilityApi.persist(a, user.getId()));
    }

    public Feature addFeature(Version version, String name) {
        return eg.addFeature(version, name, feature -> {
            long                 startTime          = System.currentTimeMillis();
            String               basePrompt         = Feature.getDefaultLightAvatarPrompt(name);
            String               darkBasePrompt     = Feature.getDefaultDarkAvatarPrompt(name);
            String               negativePrompt     = Feature.getDefaultLightAvatarNegativePrompt();
            String               darkNegativePrompt = Feature.getDefaultDarkAvatarNegativePrompt();
            GeneratedImageResult lightImage         = avatarService.generateLightAvatarWithFallback(basePrompt, negativePrompt, "lightbulb");
            feature.setLightAvatarHash(AvatarUtil.computeHash(lightImage.getResizedImage()));
            GeneratedImageResult darkImage = avatarService.generateDarkAvatarWithFallback(darkBasePrompt, darkNegativePrompt, lightImage, "lightbulb");
            feature.setDarkAvatarHash(AvatarUtil.computeHash(darkImage.getResizedImage()));

            Feature saved = featureApi.persist(feature);
            featureApi.updateAvatarFull(saved.getId(), lightImage.getResizedImage(), lightImage.getOriginalImage(), basePrompt,
                    darkImage.getResizedImage(), darkImage.getOriginalImage(), darkImage.getPrompt(),
                    lightImage.getNegativePrompt(), darkImage.getNegativePrompt());
            System.out.println("Generated lightImage for feature: " + name + " in " + (System.currentTimeMillis() - startTime) + " ms");
            return saved;
        });
    }

    public void addLocation(User user, String country, String state, LocalDate start) {
        eg.addLocation(user, country, state, start, l -> locationApi.persist(l, user.getId()));
    }

    public void addOffDay(User user, LocalDate offDayStart, LocalDate offDayFinish, OffDayType type) {
        OffDay offDay = new OffDay(offDayStart, offDayFinish, type);
        offDay.setUser(user);
        offDay.setCreated(user.getCreated());
        offDay.setUpdated(user.getUpdated());
        // Add to in-memory state immediately so overlap detection and calendar work during generation.
        // The actual DB persist is deferred – call flushOffDayBuffer(user) when all off days for the
        // user have been accumulated.
        user.addOffday(offDay);
        offDayApi.persist(offDay, user.getId());
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
                    addOffDayToBuffer(user, currentStart, currentDate, offDayType);
//                    logger.info(String.format("%s %s %s", currentStart, currentDate, "vacation"));
                    break;
                }
            } else {
                // We hit a non-working day, end the current block if there is one
                if (inBlock) {
                    LocalDate blockEnd = currentDate.minusDays(1);
                    addOffDayToBuffer(user, currentStart, blockEnd, offDayType);
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
                    addOffDayToBuffer(user, currentStart, blockEnd, offDayType);
//                    logger.info(String.format("%s %s %s", currentStart, blockEnd, "vacation"));
                }
                break;
            }
        }

        return daysUsed;
    }

    private void addOffDayToBuffer(User user, LocalDate offDayStart, LocalDate offDayFinish, OffDayType type) {
        OffDay offDay = new OffDay(offDayStart, offDayFinish, type);
        offDay.setUser(user);
        offDay.setCreated(user.getCreated());
        offDay.setUpdated(user.getUpdated());
        // Add to in-memory state immediately so overlap detection and calendar work during generation.
        // The actual DB persist is deferred – call flushOffDayBuffer(user) when all off days for the
        // user have been accumulated.
        user.addOffday(offDay);
        offDayBuffer.add(offDay);
        ProjectCalendarException vacation = user.getCalendar().addCalendarException(offDayStart, offDayFinish);
        switch (type) {
            case VACATION -> vacation.setName("vacation");
            case SICK -> vacation.setName("sick");
            case TRIP -> vacation.setName("trip");
        }
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

    public Task addParentTask(String name, Sprint sprint, Task parent, Task dependency) {
        return addTask(sprint, parent, name, null, Duration.ofDays(0), null, null, dependency);
    }

    public Product addProduct(String name) {
        return eg.addProduct(name, product -> {
            long                 startTime          = System.currentTimeMillis();
            String               basePrompt         = Product.getDefaultLightAvatarPrompt(name);
            String               darkBasePrompt     = Product.getDefaultDarkAvatarPrompt(name);
            String               negativePrompt     = Product.getDefaultLightAvatarNegativePrompt();
            String               darkNegativePrompt = Product.getDefaultDarkAvatarNegativePrompt();
            GeneratedImageResult lightImage         = avatarService.generateLightAvatarWithFallback(basePrompt, negativePrompt, "cube");
            product.setLightAvatarHash(AvatarUtil.computeHash(lightImage.getResizedImage()));
            GeneratedImageResult darkImage = avatarService.generateDarkAvatarWithFallback(darkBasePrompt, darkNegativePrompt, lightImage, "cube");
            product.setDarkAvatarHash(AvatarUtil.computeHash(darkImage.getResizedImage()));

            Product saved = productApi.persist(product);

            productApi.updateAvatarFull(saved.getId(), lightImage.getResizedImage(), lightImage.getOriginalImage(), basePrompt,
                    darkImage.getResizedImage(), darkImage.getOriginalImage(), darkImage.getPrompt(),
                    lightImage.getNegativePrompt(), darkImage.getNegativePrompt());
            System.out.println("Generated lightImage for product: " + name + " in " + (System.currentTimeMillis() - startTime) + " ms");
            return saved;
        });
    }

    public Feature addRandomFeature(Version version) {
        return addFeature(version, nameGenerator.generateFeatureName(getFeatureIndex()));
    }

    protected void addRandomOffDays(User saved, LocalDate employmentDate) {
        try (Profiler pc = new Profiler(SampleType.CPU)) {

            int employmentYear = employmentDate.getYear();
            offDaysIterations = 0;
            for (int yearIndex = 0; yearIndex < 2; yearIndex++) {
                int year = employmentYear + yearIndex;
                random.setSeed(generateUserYearSeed(saved, year));
                addOffDays(saved, employmentDate, 30, year, OffDayType.VACATION, 10, 20);
                addOffDays(saved, employmentDate, random.nextInt(5), year, OffDayType.TRIP, 1, 5);
            }
            flushOffDayBuffer(saved);
        }
    }

    public void addRandomProducts(int count) {
        User user1 = addRandomUser();

        for (int i = 0; i < count; i++) {
            Product product = addProduct(nameGenerator.generateProductName(getProductIndex()));
            Version version = addVersion(product, String.format("1.%d.0", i));
            Feature feature = addRandomFeature(version);
            Sprint  sprint  = addRandomSprint(feature);
            Task    task1   = addTask(sprint, null, "Project Phase 1", LocalDateTime.now(), Duration.ofDays(10), null, null, null);
            Task    task2   = addTask(sprint, task1, "Design", LocalDateTime.now(), Duration.ofDays(4), null, user1, null);
            Task    task3   = addTask(sprint, task2, "Implementation", LocalDateTime.now().plusDays(4), Duration.ofDays(6), null, user1, task1);
        }
        testProducts();
    }

    protected void addRandomSickDays() {
        try (Profiler pc = new Profiler(SampleType.CPU)) {
            List<User> all = userApi.getAll();
            for (User user : all) {
                user.initialize();
                LocalDate employmentDate = ParameterOptions.getNow().toLocalDate().minusYears(1);

                int employmentYear = employmentDate.getYear();
                offDaysIterations = 0;
                for (int yearIndex = 0; yearIndex < 2; yearIndex++) {
                    int year = employmentYear + yearIndex;
                    random.setSeed(generateUserYearSeed(user, year));
                    addOffDays(user, employmentDate, random.nextInt(20), year, OffDayType.SICK, 1, 5);
                }
                flushOffDayBuffer(user);
            }
        }
    }

    public Sprint addRandomSprint(Feature feature) {
        return addSprint(feature, nameGenerator.generateSprintName(getSprintIndex()));
    }

    /**
     * Adds a random user with a random name and email address and location in de/nw.
     * The user is initialized with a GanttContext and has a vacation off day.
     * The user first working day is set to the given date.
     *
     * @return the created User object
     */
    public User addRandomUser(LocalDate firstDate) {
        String       name  = nameGenerator.generateUserName(getUserIndex());
        String       email = nameGenerator.generateUserEmail(getUserIndex());
        User         saved = addUser(name, email, "USER", "de", "nw", firstDate, generateUserColor(getUserIndex()), 0.7f);
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
    public User addRandomUser() {
        String    name      = nameGenerator.generateUserName(getUserIndex());
        String    email     = nameGenerator.generateUserEmail(getUserIndex());
        LocalDate firstDate = ParameterOptions.getNow().toLocalDate();

        User         saved = addUser(name, email, "USER", "de", "nw", firstDate, generateUserColor(getUserIndex()), 0.7f);
        GanttContext gc    = new GanttContext();
        gc.initialize();
        saved.initialize(gc);
        addRandomOffDays(saved, firstDate);
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
    public User addRandomUser(int index, float availability) {
        String       name      = nameGenerator.generateUserName(index);
        String       email     = nameGenerator.generateUserEmail(getUserIndex());
        LocalDate    firstDate = ParameterOptions.getNow().toLocalDate().minusYears(1);
        User         saved     = addUser(name, email, "USER", "de", "nw", firstDate, generateUserColor(getUserIndex()), availability);
        GanttContext gc        = new GanttContext();
        gc.initialize();
        saved.initialize(gc);
        addRandomOffDays(saved, firstDate);
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
    public void addRandomUsers(int count) {
        for (int i = 0; i < count; i++) {
            long      time      = System.currentTimeMillis();
            String    name      = nameGenerator.generateUserName(getUserIndex());
            String    email     = nameGenerator.generateUserEmail(getUserIndex());
            LocalDate firstDate = ParameterOptions.getNow().toLocalDate().minusYears(2);
            User      saved;
            if (email.equalsIgnoreCase("christopher.paul@kassandra.org"))
                saved = addUser(name, email, "ADMIN,USER", "de", "nw", firstDate, generateUserColor(getUserIndex()), 0.5f);
            else
                saved = addUser(name, email, "USER", "de", "nw", firstDate, generateUserColor(getUserIndex()), 0.5f + ((float) random.nextInt(5)) / 5f);
            System.out.println("Adding user: " + saved.getName() + " took " + (System.currentTimeMillis() - time) + " ms");
            saved.initialize();
            time = System.currentTimeMillis();
            if (saved.getOffDays().isEmpty()) {
                //only in case it is a new user
                addRandomOffDays(saved, firstDate);
            }
            Profiler.log("generateRandomOffDays");
            System.out.println("Adding off days for user: " + saved.getName() + " took " + (System.currentTimeMillis() - time) + " ms, and " + offDaysIterations + " iterations");
        }
//        printTables();
        testUsers();
    }

    /**
     * Adds a random version with the given name to the given product.
     *
     * @param product the product to add the version to
     * @return the created Version object
     */
    public Version addRandomVersion(Product product) {
        return addVersion(product, nameGenerator.generateVersionName(getVersionIndex()));
    }

    public Sprint addSprint(Feature feature, String name) {
        return eg.addSprint(feature, name, sprint -> {
            long                 startTime          = System.currentTimeMillis();
            String               basePrompt         = Sprint.getDefaultLightAvatarPrompt(name);
            String               darkBasePrompt     = Sprint.getDefaultDarkAvatarPrompt(name);
            String               negativePrompt     = Sprint.getDefaultLightAvatarNegativePrompt();
            String               darkNegativePrompt = Sprint.getDefaultDarkAvatarNegativePrompt();
            GeneratedImageResult lightImage         = avatarService.generateLightAvatarWithFallback(basePrompt, negativePrompt, "exit");
            sprint.setLightAvatarHash(AvatarUtil.computeHash(lightImage.getResizedImage()));
            GeneratedImageResult darkImage = avatarService.generateDarkAvatarWithFallback(darkBasePrompt, darkNegativePrompt, lightImage, "exit");
            sprint.setDarkAvatarHash(AvatarUtil.computeHash(darkImage.getResizedImage()));

            Sprint saved = sprintApi.persist(sprint);

            sprintApi.updateAvatarFull(saved.getId(), lightImage.getResizedImage(), lightImage.getOriginalImage(), basePrompt,
                    darkImage.getResizedImage(), darkImage.getOriginalImage(), darkImage.getPrompt(),
                    lightImage.getNegativePrompt(), darkImage.getNegativePrompt());
            System.out.println("Generated lightImage for sprint: " + name + " in " + (System.currentTimeMillis() - startTime) + " ms");
            return saved;
        });
    }

    public Task addTask(String name, String minWorkString, String maxWorkString, User user, Sprint sprint, Task parent, Task dependency) {
        return addTask(sprint, parent, name, null, DateUtil.parseWorkDayDurationString(minWorkString), DateUtil.parseWorkDayDurationString(maxWorkString), user, dependency, null, false);
    }

    public Task addTask(Sprint sprint, Task parent, String name, LocalDateTime start, Duration minWork, Duration maxWork, User user, Task dependency) {
        return addTask(sprint, parent, name, start, minWork, maxWork, user, dependency, null, false);
    }

    public Task addTask(Sprint sprint, Task parent, String name, LocalDateTime start, Duration minWork, Duration maxWork, User user, Task dependency, TaskMode taskMode, boolean milestone) {
        return eg.addTask(sprint, parent, name, start, minWork, maxWork, user, dependency, taskMode, milestone, task -> {
            Task saved = taskApi.persist(task);
            System.out.printf("Adding %s%n", saved.toString());
            return saved;
        });
    }

    public User addUser(String name, String email, String roles, String country, String state, LocalDate start, Color color, float availability) {
        // Check if user already exists by email
        User existingUser = null;
        try {
            existingUser = userApi.getByEmail(email).get();
        } catch (Exception e) {
            // User doesn't exist, which is fine - we'll create a new one
        }
        if (existingUser != null) {
            System.out.println("User with email " + email + " already exists, skipping creation");
            getUsers().add(existingUser);
            return existingUser;
        }

        long                 startTime          = System.currentTimeMillis();
        String               basePrompt         = User.getDefaultLightAvatarPrompt(name);
        String               darkBasePrompt     = User.getDefaultDarkAvatarPrompt(name);
        String               negativePrompt     = User.getDefaultLightAvatarNegativePrompt();
        String               darkNegativePrompt = User.getDefaultDarkAvatarNegativePrompt();
        GeneratedImageResult lightImage         = avatarService.generateLightAvatarWithFallback(basePrompt, negativePrompt, "user");
        GeneratedImageResult darkImage          = avatarService.generateDarkAvatarWithFallback(darkBasePrompt, darkNegativePrompt, lightImage, "user");

        // eg.addUser builds the User, assigns an ID, persists it, and registers it in the collection
        User saved = eg.addUser(name, email, roles, start, color, user -> {
            user.setLightAvatarHash(AvatarUtil.computeHash(lightImage.getResizedImage()));
            user.setDarkAvatarHash(AvatarUtil.computeHash(darkImage.getResizedImage()));
            userApi.persist(user);
            log.info("Created user: " + user.getName() + " with email: " + user.getEmail());
            return user;
        });
        userApi.updateAvatarFull(saved.getId(), lightImage.getResizedImage(), lightImage.getOriginalImage(), basePrompt,
                darkImage.getResizedImage(), darkImage.getOriginalImage(), darkImage.getPrompt(),
                lightImage.getNegativePrompt(), darkImage.getNegativePrompt());

        // Chain sub-entities – each method already delegates to eg with its own persister
        addLocation(saved, country, state, start);
        addAvailability(saved, availability, start);

        // Assign the default Western work week starting on the user's first working day
        WorkWeek defaultWorkWeek = workWeekApi.getAll().stream()
                .filter(ww -> DefaultEntitiesInitializer.WORK_WEEK_5X8.equals(ww.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Default work week '" + DefaultEntitiesInitializer.WORK_WEEK_5X8 + "' not found"));
        addWorkWeek(saved, defaultWorkWeek, start);

        System.out.println("Generated avatar for user: " + name + " in " + (System.currentTimeMillis() - startTime) + " ms");

        return saved;
    }

    public Version addVersion(Product product, String versionName) {
        return eg.addVersion(product, versionName, v -> {
            Version saved = versionApi.persist(v);
            return saved;
        });
    }

    /**
     * Assigns a {@link WorkWeek} to a user starting on {@code start}.
     * The saved assignment is added to {@code user.getUserWorkWeeks()} to keep the
     * in-memory state in sync with the database.
     *
     * @param user     the owning user
     * @param workWeek the work-week definition to assign
     * @param start    the effective start date of the assignment
     */
    public void addWorkWeek(User user, WorkWeek workWeek, LocalDate start) {
        UserWorkWeek uww = new UserWorkWeek(workWeek, start);
        uww.setUser(user);
        uww.setCreated(user.getCreated());
        uww.setUpdated(user.getUpdated());
        UserWorkWeek saved = userWorkWeekApi.persist(uww, user.getId());
        user.addUserWorkWeek(saved);
    }

    public Worklog addWorklog(Task task, User user, OffsetDateTime start, Duration timeSpent, String comment) {
        return eg.addWorklog(task, user, start, timeSpent, comment, worklogApi::persist);
    }

    /**
     * Buffers a worklog for deferred batch persistence.
     * <p>
     * The worklog is added to the task's in-memory state immediately so that time-spent /
     * remaining-estimate accounting stays correct during generation. The actual DB persist is
     * deferred – call {@link #flushWorklogBuffer(Sprint)} once all worklogs for the sprint have
     * been accumulated.
     * </p>
     *
     * @param task      the owning task
     * @param user      the worklog author
     * @param start     the log timestamp
     * @param timeSpent duration of work logged
     * @param comment   log comment
     * @return the buffered (not yet persisted) worklog
     */
    protected Worklog addWorklogToBuffer(Task task, User user, OffsetDateTime start, Duration timeSpent, String comment) {
        Worklog worklog = eg.addWorklog(task, user, start, timeSpent, comment, UnaryOperator.identity());
//        Worklog worklog = new Worklog();
//        worklog.setSprintId(task.getSprintId());
//        worklog.setTaskId(task.getId());
//        worklog.setAuthorId(user.getId());
//        worklog.setStart(start);
//        worklog.setTimeSpent(timeSpent);
//        worklog.setTimeRemainingEstimate(task.getRemainingEstimate().minus(timeSpent));
//        worklog.setComment(comment);
//        task.addWorklog(worklog);
        // Add to in-memory state immediately so accounting (timeSpent, remainingEstimate) stays correct.
        // The actual DB persist is deferred – call flushWorklogBuffer(sprint) when done.
        worklogBuffer.add(worklog);
        return worklog;
    }

    public Task createDeliveryBufferTask(Sprint sprint, Duration minWork) {
        //create the buffer task
        Task task = new Task();
        task.setName(Task.DELIVERY_BUFFER);
        task.setImpactOnCost(false);//delivery buffer has no impact on cost
        if (sprint != null) {
            task.setSprint(sprint);
            task.setSprintId(sprint.getId());
        }
        task.setMinEstimate(minWork);

        Task saved = taskApi.persist(task);
//        Task saved = task;
        saved.setId(UUID.randomUUID());
        if (sprint != null) {
            saved.setSprint(sprint);
            sprint.addTask(saved);
        }
        return task;
    }

    /**
     * Flushes all buffered off days for the given user to the database in a single batch HTTP call.
     * <p>
     * The temp (ID-less) OffDay objects that were added to {@code user.getOffDays()} during generation
     * are replaced by the server-returned objects carrying their assigned IDs. The flushed entries are
     * also added to {@link #offDays}.
     * </p>
     *
     * @param user the user whose buffered off days should be persisted
     */
    protected void flushOffDayBuffer(User user) {
        List<OffDay> pending = offDayBuffer.stream()
                .filter(o -> o.getUser() != null && o.getUser().getId() != null && o.getUser().getId().equals(user.getId()))
                .toList();
        if (pending.isEmpty()) {
            return;
        }
        try (Profiler pc = new Profiler(SampleType.JPA)) {
            long         time  = System.currentTimeMillis();
            List<OffDay> saved = offDayApi.persistBatch(pending, user.getId());
            log.debug("Batch-persisted {} off days for user '{}' in {} ms", saved.size(), user.getName(), System.currentTimeMillis() - time);
            // Replace in-memory temp objects with the saved ones (which carry server-assigned IDs).
            user.getOffDays().removeAll(pending);
            saved.forEach(user::addOffday);
            getOffDays().addAll(saved);
        }
        offDayBuffer.removeAll(pending);
    }

    /**
     * Flushes all buffered worklogs for the given sprint to the database in a single batch HTTP call.
     * <p>
     * The temp (ID-less) {@link Worklog} objects that were added to each task's worklog list during
     * generation receive their server-assigned IDs in-place (the response is assumed to be in the same
     * order as the request). The flushed entries are also added to {@link #getWorklogs()}.
     * </p>
     *
     * @param sprint the sprint whose buffered worklogs should be persisted
     */
    protected void flushWorklogBuffer(Sprint sprint) {
        List<Worklog> pending = worklogBuffer.stream()
                .filter(w -> sprint.getId().equals(w.getSprintId()))
                .toList();
        if (pending.isEmpty()) {
            return;
        }
        try (Profiler pc = new Profiler(SampleType.JPA)) {
            long          time  = System.currentTimeMillis();
            List<Worklog> saved = worklogApi.persistBatch(pending);
            log.debug("Batch-persisted {} worklogs for sprint '{}' in {} ms", saved.size(), sprint.getName(), System.currentTimeMillis() - time);
            // Copy server-assigned IDs back to the existing in-memory worklog objects so they can be
            // referenced by ID going forward. Re-adding via task.addWorklog() is intentionally avoided
            // because it would double-count time-spent / remaining-estimate accounting.
            for (int i = 0; i < pending.size(); i++) {
                pending.get(i).setId(saved.get(i).getId());
            }
            getWorklogs().addAll(pending);
        }
        worklogBuffer.removeAll(pending);
    }

    public void generateRandomOffDays(User saved, LocalDate employmentDate) {
        try (Profiler pc = new Profiler(SampleType.CPU)) {

            int employmentYear = employmentDate.getYear();
            offDaysIterations = 0;
            for (int yearIndex = 0; yearIndex < 2; yearIndex++) {
                int year = employmentYear + yearIndex;
                random.setSeed(generateUserYearSeed(saved, year));
                addOffDays(saved, employmentDate, 30, year, OffDayType.VACATION, 10, 20);
                addOffDays(saved, employmentDate, random.nextInt(5), year, OffDayType.TRIP, 1, 5);
            }
            flushOffDayBuffer(saved);
        }
    }

    public void generateRandomSickDays() {
        try (Profiler pc = new Profiler(SampleType.CPU)) {
            List<User> all = userApi.getAll();
            for (User user : all) {
                user.initialize();
                LocalDate employmentDate = ParameterOptions.getNow().toLocalDate().minusYears(1);

                int employmentYear = employmentDate.getYear();
                offDaysIterations = 0;
                for (int yearIndex = 0; yearIndex < 2; yearIndex++) {
                    int year = employmentYear + yearIndex;
                    random.setSeed(generateUserYearSeed(user, year));
                    addOffDays(user, employmentDate, random.nextInt(20), year, OffDayType.SICK, 1, 5);
                }
                flushOffDayBuffer(user);
            }
        }
    }

    public Color generateUserColor(int userIndex) {
        int index = userIndex % LightTheme.KELLY_COLORS.length;
        return LightTheme.KELLY_COLORS[index];
    }

    private static int generateUserYearSeed(User saved, int year) {
        return (saved.getName() + year).hashCode();
    }

    public TreeSet<Availability> getAvailabilities() {
        return eg.getAvailabilities();
    }

    protected int getCurrentSprintIndex() {
        return getSprintIndex();
    }

    public int getFeatureIndex() {
        return eg.getFeatureIndex();
    }

    public List<Feature> getFeatures() {
        return eg.getFeatures();
    }

    public TreeSet<Location> getLocations() {
        return eg.getLocations();
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

    public TreeSet<OffDay> getOffDays() {
        return offDays;
    }

    public int getProductIndex() {
        return eg.getProductIndex();
    }

    public List<Product> getProducts() {
        return eg.getProducts();
    }

    public int getSprintIndex() {
        return eg.getSprintIndex();
    }

    public List<Sprint> getSprints() {
        return eg.getSprints();
    }

    public List<Task> getTasks() {
        return eg.getTasks();
    }

    public int getUserIndex() {
        return eg.getUserIndex();
    }

    public TreeSet<User> getUsers() {
        return eg.getUsers();
    }

    public int getVersionIndex() {
        return eg.getVersionIndex();
    }

    public List<Version> getVersions() {
        return eg.getVersions();
    }

    public List<Worklog> getWorklogs() {
        return eg.getWorklogs();
    }

    public void init() {
        eg.init();
        nameGenerator.resetStoryPool(); // Reset story pool for each test
        getOffDays().clear();
        getLocations().clear();
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
    public void move(Sprint sprint, Task task, Task newParent) {
        Task oldParent = task.getParentTask();
        newParent.addChildTask(task);

        taskApi.update(newParent);
        taskApi.update(task);
        taskApi.update(oldParent);
    }

    /**
     * Initialises all REST API adapter instances once the embedded web server has started and its
     * port is known. Using {@link EventListener} instead of {@code @PostConstruct} is necessary
     * because {@code local.server.port} is registered in the {@link org.springframework.core.env.Environment}
     * only during {@code finishRefresh()} — after all singleton beans have already been created.
     *
     * @param event the event carrying the started {@link org.springframework.boot.web.server.WebServer}
     */
    @EventListener
    public void onWebServerInitialized(WebServerInitializedEvent event) {
        init();
        ParameterOptions.setNow(OffsetDateTime.parse("2025-05-05T08:00:00+01:00"));

        // Obtain the actual random port from the event — available only after the server has bound.
        String baseUrl = "http://localhost:" + event.getWebServer().getPort() + "/api";
        productApi      = new ProductApi(testRestTemplate.getRestTemplate(), jsonMapper, baseUrl);
        featureApi      = new FeatureApi(testRestTemplate.getRestTemplate(), jsonMapper, baseUrl);
        userApi         = new UserApi(testRestTemplate.getRestTemplate(), jsonMapper, baseUrl);
        availabilityApi = new AvailabilityApi(testRestTemplate.getRestTemplate(), jsonMapper, baseUrl);
        locationApi     = new LocationApi(testRestTemplate.getRestTemplate(), jsonMapper, baseUrl);
        offDayApi       = new OffDayApi(testRestTemplate.getRestTemplate(), jsonMapper, baseUrl);
        taskApi         = new TaskApi(testRestTemplate.getRestTemplate(), jsonMapper, baseUrl);
        versionApi      = new VersionApi(testRestTemplate.getRestTemplate(), jsonMapper, baseUrl);
        sprintApi       = new SprintApi(testRestTemplate.getRestTemplate(), jsonMapper, baseUrl);
        worklogApi      = new WorklogApi(testRestTemplate.getRestTemplate(), jsonMapper, baseUrl);
        userGroupApi    = new UserGroupApi(testRestTemplate.getRestTemplate(), jsonMapper, baseUrl);
        productAclApi   = new ProductAclApi(testRestTemplate.getRestTemplate(), jsonMapper, baseUrl);
        workWeekApi     = new WorkWeekApi(testRestTemplate.getRestTemplate(), jsonMapper, baseUrl);
        userWorkWeekApi = new UserWorkWeekApi(testRestTemplate.getRestTemplate(), jsonMapper, baseUrl);
    }

    public void removeAvailability(Availability availability, User user) {
        availabilityApi.deleteById(user.getId(), availability.getId());
        user.removeAvailability(availability);
        getAvailabilities().remove(availability);
    }

    public void removeFeature(UUID id) {
        Feature featureToRemove = eg.getFeatures().stream().filter(project -> project.getId().equals(id)).findFirst().orElse(null);
        featureApi.deleteById(id);
        if (featureToRemove != null) {
            //remove this project from its parent version
            featureToRemove.getVersion().removeFeature(featureToRemove);
            // Remove all sprints and their tasks
            for (Sprint sprint : featureToRemove.getSprints()) {
                for (Task task : sprint.getTasks()) {
                    getTasks().remove(task);
                }
                getSprints().remove(sprint);
            }
            eg.getFeatures().remove(featureToRemove);
        }
    }

    public void removeLocation(Location location, User user) {
        locationApi.deleteById(user.getId(), location.getId());
        user.removeLocation(location);
        getLocations().remove(location);
    }

    public void removeOffDay(OffDay offDay, User user) {
        offDayApi.deleteById(user.getId(), offDay.getId());
        user.removeOffDay(offDay);
        getOffDays().remove(offDay);
    }

    public void removeProduct(UUID id) {
        Product productToRemove = getProducts().stream().filter(product -> product.getId().equals(id)).findFirst().orElse(null);
        productApi.deleteById(id);

        if (productToRemove != null) {
            // Remove all versions and their projects, sprints, and tasks
            for (Version version : productToRemove.getVersions()) {
                for (Feature feature : version.getFeatures()) {
                    for (Sprint sprint : feature.getSprints()) {
                        for (Task task : sprint.getTasks()) {
                            getTasks().remove(task);
                        }
                        getSprints().remove(sprint);
                    }
                    eg.getFeatures().remove(feature);
                }
                getVersions().remove(version);
            }
            getProducts().remove(productToRemove);
        }
    }

    public void removeSprint(UUID id) {
        Sprint sprintToRemove = getSprints().stream().filter(sprint -> sprint.getId().equals(id)).findFirst().orElse(null);
        sprintApi.deleteById(id);
        if (sprintToRemove != null) {
            //remove this sprint from its parent project
            sprintToRemove.getFeature().removeSprint(sprintToRemove);
            for (Task task : sprintToRemove.getTasks()) {
                getTasks().remove(task);
            }
            getSprints().remove(sprintToRemove);
        }
    }

    public void removeTaskTree(Task task) {
        getTasks().remove(task);
        taskApi.deleteById(task.getId());
        for (Task childTask : task.getChildTasks()) {
            removeTaskTree(childTask);
        }
    }

    public void removeUser(UUID id) {
        User userToRemove = getUsers().stream().filter(user -> user.getId().equals(id)).findFirst().orElse(null);
        userApi.deleteById(id);

        if (userToRemove != null) {
            // Remove all availabilities
            getAvailabilities().removeAll(userToRemove.getAvailabilities());
            // Remove all locations
            getLocations().removeAll(userToRemove.getLocations());
            // Remove all off days
            getOffDays().removeAll(userToRemove.getOffDays());
            // Remove the user
            getUsers().remove(userToRemove);
        }

    }

    public void removeVersion(UUID id) {
        Version versionToRemove = getVersions().stream().filter(version -> version.getId().equals(id)).findFirst().orElse(null);
        versionApi.deleteById(id);

        if (versionToRemove != null) {
            //remove this version from its parent product
            versionToRemove.getProduct().removeVersion(versionToRemove);
            // Remove all projects, sprints, and tasks
            for (Feature feature : versionToRemove.getFeatures()) {
                for (Sprint sprint : feature.getSprints()) {
                    for (Task task : sprint.getTasks()) {
                        getTasks().remove(task);
                    }
                    getSprints().remove(sprint);
                }
                eg.getFeatures().remove(feature);
            }
            getVersions().remove(versionToRemove);
        }
    }

    /**
     * Removes a work-week assignment from a user.
     * Both the REST endpoint and the in-memory list are updated.
     *
     * @param userWorkWeek the assignment to remove
     * @param user         the owning user
     */
    public void removeWorkWeek(UserWorkWeek userWorkWeek, User user) {
        userWorkWeekApi.deleteById(user.getId(), userWorkWeek.getId());
        user.removeUserWorkWeek(userWorkWeek);
    }

    public void setOffDays(TreeSet<OffDay> offDays) {
        this.offDays = offDays;
    }

    public static void setUser(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

//    public static void setSprintIndex(int sprintIndex) {
//        PersistingEntityGenerator.sprintIndex = sprintIndex;
//    }

//    public void setTasks(List<Task> tasks) {
//        this.tasks = tasks;
//    }

    public static Authentication setUser(String email, String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // If both email and role are null, clear the security context (anonymous user)
        if (email == null && role == null) {
            SecurityContextHolder.clearContext();
        } else {
            SecurityContextHolder.getContext().setAuthentication(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            email, "password",
                            List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(role))
                    )
            );
        }
        return authentication;
    }

    public void setUserIndex(int userIndex) {
        eg.setUserIndex(userIndex);
    }

    protected void testAll() {
        testProducts();
        testUsers();
    }

    @AfterEach
    public void testAllAndPrintTables() {
        setUser("admin-user", "ROLE_ADMIN");
        testAll();
//        printTables();
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
        gc.allTasks    = taskApi.getAll().stream().sorted().toList();
        gc.initialize();

        // Add the always-present "Default" product (created by DefaultEntitiesInitializer) to expectedProducts if not already tracked.
        // Use gc.allProducts as it is already fully populated with versions, features and sprints.
        boolean defaultProductTracked = getProducts().stream().anyMatch(p -> DefaultEntitiesInitializer.DEFAULT_NAME.equals(p.getName()));
        if (!defaultProductTracked) {
            gc.allProducts.stream()
                    .filter(p -> DefaultEntitiesInitializer.DEFAULT_NAME.equals(p.getName()))
                    .findFirst()
                    .ifPresent(getProducts()::add);
        }
        boolean defaultVersionTracked = getVersions().stream().anyMatch(p -> DefaultEntitiesInitializer.DEFAULT_NAME.equals(p.getName()));
        if (!defaultVersionTracked) {
            gc.allVersions.stream()
                    .filter(p -> DefaultEntitiesInitializer.DEFAULT_NAME.equals(p.getName()))
                    .findFirst()
                    .ifPresent(getVersions()::add);
        }
        boolean defaultFeatureTracked = eg.getFeatures().stream().anyMatch(p -> DefaultEntitiesInitializer.DEFAULT_NAME.equals(p.getName()));
        if (!defaultFeatureTracked) {
            gc.allFeatures.stream()
                    .filter(p -> DefaultEntitiesInitializer.DEFAULT_NAME.equals(p.getName()))
                    .findFirst()
                    .ifPresent(eg.getFeatures()::add);
        }
        boolean defaultSprintTracked = getSprints().stream().anyMatch(p -> DefaultEntitiesInitializer.BACKLOG_SPRINT_NAME.equals(p.getName()));
        if (!defaultFeatureTracked) {
            gc.allSprints.stream()
                    .filter(p -> DefaultEntitiesInitializer.BACKLOG_SPRINT_NAME.equals(p.getName()))
                    .findFirst()
                    .ifPresent(getSprints()::add);
        }

        // add default product, version, feature and backlog sprint
        Product defaultProduct = getProducts().stream().filter(f -> f.getName().equals(DefaultEntitiesInitializer.DEFAULT_NAME)).findFirst().get();
        Version defaultVersion = getVersions().stream().filter(f -> f.getName().equals(DefaultEntitiesInitializer.DEFAULT_NAME)).findFirst().get();
        defaultProduct.getVersions().add(defaultVersion);
        defaultVersion.setProduct(defaultProduct);
        Feature defaultFeature = eg.getFeatures().stream().filter(f -> f.getName().equals(DefaultEntitiesInitializer.DEFAULT_NAME)).findFirst().get();
        defaultVersion.getFeatures().add(defaultFeature);
        defaultFeature.setVersion(defaultVersion);
        Sprint defaultSprint = getSprints().stream().filter(f -> f.getName().equals(DefaultEntitiesInitializer.BACKLOG_SPRINT_NAME)).findFirst().get();
        defaultFeature.getSprints().add(defaultSprint);
        defaultSprint.setFeature(defaultFeature);

        getProducts().sort(Comparator.comparing(Product::getName));
        eg.getFeatures().sort(Comparator.comparing(Feature::getName));
        getSprints().sort(Comparator.comparing(Sprint::getName));
        getTasks().sort(Comparator.comparing(Task::getName));
        getVersions().sort(Comparator.comparing(Version::getName));
        getWorklogs().sort(Comparator.comparing(Worklog::getStart));

        GanttContext egc = new GanttContext();
        egc.allUsers    = getUsers().stream().sorted().toList();
        egc.allProducts = getProducts();
        egc.allVersions = getVersions();
        egc.allFeatures = eg.getFeatures();
        egc.allSprints  = getSprints();
        egc.allTasks    = taskApi.getAll().stream().sorted().toList();
        egc.initialize();


        DTOAsserts.assertUnorderedListEquals(egc.allProducts, gc.allProducts, Comparator.comparing(Product::getName), "products", DTOAsserts::assertProductEquals);
    }

    public void testUsers() {
        entityManager.clear();//clear the cache to get the latest data from the database
        List<User> actual = userApi.getAll();

        DTOAsserts.assertUnorderedListEquals(getUsers(), actual, Comparator.comparing(User::getId), "users",
                DTOAsserts::assertUserEquals);
    }

    public void updateAvailability(Availability availability, User user) {
        availabilityApi.update(availability, user.getId());
        getAvailabilities().remove(availability);
        getAvailabilities().add(availability);
    }

    public void updateFeature(Feature feature) {
        featureApi.update(feature);
        eg.getFeatures().remove(feature);
        eg.getFeatures().add(feature);//replace old products with the updated one
    }

    public void updateLocation(Location location, User user) throws ServerErrorException {
        locationApi.update(location, user.getId());
        getLocations().remove(location);
        getLocations().add(location);
    }

    public void updateOffDay(OffDay offDay, User user) throws ServerErrorException {
        offDayApi.update(offDay, user.getId());
        getOffDays().remove(offDay);
        getOffDays().add(offDay);
    }

    public void updateProduct(Product product) throws ServerErrorException {
        productApi.update(product);
        getProducts().remove(product);
        getProducts().add(product);//replace old products with the updated one
    }

    public void updateSprint(Sprint sprint) throws ServerErrorException {
        sprintApi.update(sprint);
        getSprints().remove(sprint);
        getSprints().add(sprint); // Replace old sprint with the updated one
    }

    public void updateTask(Task task) {
        taskApi.update(task);
        getTasks().remove(task);
        getTasks().add(task);//replace old products with the updated one
    }

    public void updateUser(User user) throws ServerErrorException {
        userApi.update(user);
        getUsers().remove(user);
        getUsers().add(user);//replace old user with the updated one
    }

    public void updateVersion(Version version) {
        versionApi.update(version);
        getVersions().remove(version);
        getVersions().add(version);//replace old products with the updated one
    }

    /**
     * Persists an updated work-week assignment via the REST API.
     *
     * @param userWorkWeek the assignment with updated fields (must have a non-null ID)
     * @param user         the owning user
     */
    public void updateWorkWeek(UserWorkWeek userWorkWeek, User user) {
        userWorkWeekApi.update(userWorkWeek, user.getId());
    }

    public void updateWorklog(Worklog worklog) throws ServerErrorException {
        worklogApi.update(worklog);
        getWorklogs().remove(worklog);
        getWorklogs().add(worklog);//replace old products with the updated one
    }

}
