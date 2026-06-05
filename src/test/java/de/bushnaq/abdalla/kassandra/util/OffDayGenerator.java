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
import de.bushnaq.abdalla.kassandra.dto.User;
import de.bushnaq.abdalla.kassandra.rest.api.OffDayApi;
import de.bushnaq.abdalla.kassandra.rest.api.UserApi;
import de.bushnaq.abdalla.profiler.Profiler;
import de.bushnaq.abdalla.profiler.SampleType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectCalendarException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

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
    private final Random           random       = new Random();
    @Autowired
    private       TestRestTemplate testRestTemplate; // Use TestRestTemplate instead of RestTemplate
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
            case VACATION -> vacation.setName("vacation");
            case SICK -> vacation.setName("sick");
            case TRIP -> vacation.setName("trip");
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
            long         time  = System.currentTimeMillis();
            List<OffDay> saved = offDayApi.persistBatch(pending, user.getId());
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
        ParameterOptions.setNow(OffsetDateTime.parse("2025-05-05T08:00:00+01:00"));

        // Obtain the actual random port from the event — available only after the server has bound.
        String baseUrl = "http://localhost:" + event.getWebServer().getPort() + "/api";
        userApi   = new UserApi(testRestTemplate.getRestTemplate(), jsonMapper, baseUrl);
        offDayApi = new OffDayApi(testRestTemplate.getRestTemplate(), jsonMapper, baseUrl);
    }
}
