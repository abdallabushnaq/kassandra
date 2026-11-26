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

import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.OffDay;
import de.bushnaq.abdalla.kassandra.dto.OffDayType;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Task;
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.rest.api.OffDayApi;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import de.bushnaq.abdalla.profiler.Profiler;
import de.bushnaq.abdalla.profiler.SampleType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectCalendarException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.client.HttpCookieHandling;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@Slf4j
@Component
public class OffDayGenerator {
    @Autowired
    protected     JsonMapper       jsonMapper;
    private       OffDayApi        offDayApi;
    private final List<OffDay>     offDayBuffer = new ArrayList<>();
    @Getter
    private final TreeSet<OffDay>  offDays      = new TreeSet<>();
    @Getter
    private       int              offDaysIterations;
    private final Random           random       = new LoggingRandom("OffDayGenerator.random");
    @Autowired
    private       ObjectProvider<TestRestTemplate> testRestTemplateProvider;
    private       UserApi          userApi;

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
            case VACATION -> vacation.setName(OffDayType.VACATION.name());
            case SICK -> vacation.setName(OffDayType.SICK.name());
            case TRIP -> vacation.setName(OffDayType.TRIP.name());
        }
    }

    void addOffDays(User saved, LocalDate firstDate, int annualVacationDays, int year, OffDayType offDayType, int summerDurationMin, int summerDurationMax) {
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
            long time = System.currentTimeMillis();
            Authentication previous = PersistingEntityGenerator.setUser(user.getEmail(), "ROLE_USER");
            List<OffDay> saved;
            try {
                saved = offDayApi.persistBatch(pending, user.getId());
            } finally {
                PersistingEntityGenerator.setUser(previous);
            }
            log.debug("Batch-persisted {} off days for user '{}' in {} ms", saved.size(), user.getName(), System.currentTimeMillis() - time);
            // Replace in-memory temp objects with the saved ones (which carry server-assigned IDs).
            user.getOffDays().removeAll(pending);
            saved.forEach(user::addOffday);
            offDays.addAll(saved);
        }
        offDayBuffer.removeAll(pending);
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

    public void generateRandomSickDays(List<User> users) {
        try (Profiler pc = new Profiler(SampleType.CPU)) {
//            List<User> users = userApi.getAll();
            for (User user : users) {
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

    /**
     * Adds unplanned sick leave after the sprint has been leveled, without changing its planned
     * dates. The assigned developer with the most estimated work on critical leaf tasks is chosen,
     * because an absence on that path is more likely to delay the sprint. Half of that developer's
     * available critical-task dates are marked sick, centered in chronological task-date order.
     * This can delay the sprint, but does not guarantee an overrun.
     *
     * <p>The new off days are persisted and propagated to every supplied in-memory copy of the
     * user and its calendar. The worklog simulation can then observe the absences even when
     * different sprints hold different user instances.</p>
     *
     * @param sprint the already-leveled sprint whose critical work is inspected
     * @param sprints generated sprints whose user copies and calendars must receive the absences
     * @param users generated users whose copies and calendars must also receive the absences
     */
    void injectSickDays(Sprint sprint, Collection<Sprint> sprints, Collection<User> users, Path logFile) throws IOException {
        // Only scheduled, assigned cost-bearing leaf tasks on the calculated critical path
        // contribute. Exclude stories, milestones, delivery buffers and incomplete plans.
        List<Task> criticalTasks = sprint.getTasks().stream()
                .filter(task -> task.isTask() && task.getChildTasks().isEmpty() && task.isImpactOnCost() && task.isCritical())
                .filter(task -> task.getResourceId() != null && sprint.getUser(task.getResourceId()) != null)
                .filter(task -> task.getStart() != null && task.getFinish() != null && task.getFinish().isAfter(task.getStart()))
                .toList();
        // Sum the planned minimum estimates, not task durations: duration also reflects
        // resource availability and is not the amount of work assigned to the developer.
        Map<UUID, Duration> workByUser = new HashMap<>();
        for (Task task : criticalTasks) {
            workByUser.merge(task.getResourceId(), task.getMinEstimate(), Duration::plus);
        }
        // Break equal-work ties by stable user attributes rather than random UUIDs, so the
        // same seeded fixture selects the same developer on subsequent generations.
        User user = workByUser.keySet().stream()
                .map(sprint::getUser)
                .sorted(Comparator.<User, Duration>comparing(candidate -> workByUser.get(candidate.getId())).reversed()
                        .thenComparing(User::getName).thenComparing(User::getEmail))
                .findFirst().orElse(null);
        if (user == null) {
            log.info("No assigned critical-path work in sprint '{}'; skipping sick leave.", sprint.getName());
            appendSickDayLog(logFile, "Sprint: " + sprint.getName()
                    + " | Status: skipped | Reason: no assigned, scheduled critical-path leaf tasks");
            return;
        }

        // Collect distinct calendar dates with scheduled critical work. The user's existing
        // calendar excludes weekends, holidays, vacations and previously injected absences;
        // a date with multiple tasks still represents only one potential sick day.
        TreeSet<LocalDate> plannedDays = new TreeSet<>();
        for (Task task : criticalTasks) {
            if (!user.getId().equals(task.getResourceId())) {
                continue;
            }
            for (LocalDate day = task.getStart().toLocalDate(); day.atStartOfDay().isBefore(task.getFinish()); day = day.plusDays(1)) {
                if (user.getCalendar().isWorkingDate(day)) {
                    plannedDays.add(day);
                }
            }
        }
        if (plannedDays.isEmpty()) {
            log.info("No available critical-path work days for '{}' in sprint '{}'; skipping sick leave.", user.getName(), sprint.getName());
            appendSickDayLog(logFile, "Sprint: " + sprint.getName() + " | User: " + user.getName() + " (" + user.getEmail()
                    + ") | Status: skipped | Reason: no available critical-path work days");
            return;
        }
        // Round half up for odd-sized sets, then take the centered slice. If the unused
        // dates cannot be split evenly, the extra date is left at the end of the sequence.
        List<LocalDate> days = new ArrayList<>(plannedDays);
        int count = (days.size() + 1) / 2;
        int first = (days.size() - count) / 2;
        List<LocalDate> sickDays = days.subList(first, first + count);
        appendSickDayLog(logFile, "Sprint: " + sprint.getName() + " | User: " + user.getName() + " (" + user.getEmail()
                + ") | Sick days: " + sickDays + " | Status: injecting");
        // Buffer individual working dates rather than a single range: the selected dates
        // may be separated by non-working days or existing off days. The batch flush saves
        // them and replaces the temporary in-memory entries with server-returned records.
        for (LocalDate day : sickDays) {
            addOffDayToBuffer(user, day, day, OffDayType.SICK);
        }
        flushOffDayBuffer(user);

        // Generated users and sprint user maps can contain distinct DTO instances for the
        // same database user. Find all of them by identity so equal IDs do not collapse copies.
        List<OffDay> savedDays = user.getOffDays().stream()
                .filter(offDay -> offDay.getType() == OffDayType.SICK && sickDays.contains(offDay.getFirstDay()))
                .toList();
        Set<User> copies = Collections.newSetFromMap(new IdentityHashMap<>());
        copies.addAll(users);
        for (Sprint other : sprints) {
            copies.addAll(other.getUserMap().values());
        }
        // Different DTOs can also share a calendar. Add each exception once per calendar,
        // including the selected user's calendar, which the buffer already updated.
        Set<ProjectCalendar> calendars = Collections.newSetFromMap(new IdentityHashMap<>());
        calendars.add(user.getCalendar());
        for (User copy : copies) {
            if (copy == user || !user.getId().equals(copy.getId())) {
                continue;
            }
            for (OffDay offDay : savedDays) {
                if (!copy.getOffDays().contains(offDay)) {
                    copy.addOffday(offDay);
                }
            }
            if (copy.getCalendar() != null && calendars.add(copy.getCalendar())) {
                for (LocalDate day : sickDays) {
                    copy.getCalendar().addCalendarException(day, day).setName(OffDayType.SICK.name());
                }
            }
        }
        log.info("Injected {} sick days for '{}' on the critical path of sprint '{}'.", sickDays.size(), user.getName(), sprint.getName());
        appendSickDayLog(logFile, "Sprint: " + sprint.getName() + " | User: " + user.getName() + " (" + user.getEmail()
                + ") | Status: injected | Sick days: " + sickDays);
    }

    private void appendSickDayLog(Path logFile, String message) throws IOException {
        Files.writeString(logFile, message + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static int generateUserYearSeed(User saved, int year) {
        return (saved.getName() + year).hashCode();
    }

    public void init() {
        offDays.clear();
    }

    private boolean isOverlapping(List<OffDay> offDays, LocalDate start, LocalDate end) {
        return offDays.stream().anyMatch(offDay -> !(end.isBefore(offDay.getFirstDay()) || start.isAfter(offDay.getLastDay())));
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
        TestRestTemplate testRestTemplate = testRestTemplateProvider.getObject().withCookieHandling(HttpCookieHandling.DISABLE);
        ParameterOptions.setNow(OffsetDateTime.parse("2025-05-05T08:00:00+01:00"));

        // Obtain the actual random port from the event — available only after the server has bound.
        String baseUrl = "http://localhost:" + event.getWebServer().getPort() + "/api";
        userApi   = new UserApi(testRestTemplate.getRestTemplate(), jsonMapper, baseUrl);
        offDayApi = new OffDayApi(testRestTemplate.getRestTemplate(), jsonMapper, baseUrl);
    }
}
