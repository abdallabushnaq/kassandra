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

package de.bushnaq.abdalla.kassandra.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.AvatarService;
import de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService;
import de.bushnaq.abdalla.kassandra.config.KassandraProperties;
import de.bushnaq.abdalla.kassandra.report.calendar.CalendarUtil;
import de.bushnaq.abdalla.kassandra.report.gantt.GanttContext;
import de.focus_shift.jollyday.core.Holiday;
import de.focus_shift.jollyday.core.HolidayManager;
import de.focus_shift.jollyday.core.ManagerParameters;
import de.focus_shift.jollyday.core.parameter.UrlManagerParameter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.sf.mpxj.*;

import java.awt.*;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class User extends AbstractTimeAware implements Comparable<User> {
    @JsonManagedReference
    private List<Availability> availabilities = new ArrayList<>();
    @JsonIgnore
    private ProjectCalendar    calendar;
    private Color              color;
    private String             darkAvatarHash;
    private String             email;
    private LocalDate          firstWorkingDay;
    private UUID               id;
    private LocalDate          lastWorkingDay;
    private String             lightAvatarHash;
    @JsonManagedReference
    private List<Location>     locations      = new ArrayList<>();
    private String             name;
    @JsonManagedReference
    private List<OffDay>       offDays        = new ArrayList<>();
    private String             roles          = "USER"; // Default role for new users
    @JsonManagedReference
    private List<UserWorkWeek> userWorkWeeks  = new ArrayList<>();

    public User() {
        setId(UUID.randomUUID());
    }

    public void addAvailability(Availability availability) {
        availabilities.add(availability);
    }

    public void addLocation(Location location) {
        if (location.getStart() == null)
            throw new IllegalArgumentException("start date is null");
        if (location.getCountry() == null)
            throw new IllegalArgumentException("start date is null");
        if (location.getState() == null)
            throw new IllegalArgumentException("start date is null");
        locations.add(location);
    }

    public void addOffday(OffDay offDay) {
        offDays.add(offDay);
    }

    /**
     * Add a role to this user
     *
     * @param role the role to add (e.g., "ADMIN", "USER")
     */
    @JsonIgnore
    public void addRole(String role) {
        List<String> roleList = getRoleList();
        if (!roleList.contains(role)) {
            roleList.add(role);
            setRoleList(roleList);
        }
    }

    /**
     * Adds a work-week assignment to this user.
     *
     * @param userWorkWeek the assignment to add
     */
    public void addUserWorkWeek(UserWorkWeek userWorkWeek) {
        if (userWorkWeek.getStart() == null)
            throw new IllegalArgumentException("start date is null");
        if (userWorkWeek.getWorkWeek() == null)
            throw new IllegalArgumentException("work week is null");
        userWorkWeeks.add(userWorkWeek);
    }

    /**
     * Applies the user's {@link UserWorkWeek} assignments to the given MPXJ {@link ProjectCalendar}
     * as date-range-scoped {@link ProjectCalendarWeek} entries.
     * <p>
     * Each assignment governs the half-open interval {@code [start, nextStart)}. The first
     * assignment is back-dated to {@code 1900-01-01} to ensure correct resolution for any
     * historical date (the first-assignment start is not guaranteed to equal the hire date).
     * The last assignment is open-ended ({@code 9999-12-31}).
     * </p>
     * <p>
     * After mapping all intervals, {@code minutesPerDay} and {@code minutesPerWeek} are set on
     * the calendar from the currently effective work-week so that MPXJ duration arithmetic
     * reflects the actual schedule rather than the project-level defaults.
     * </p>
     *
     * @param pc the derived calendar for this user
     */
    private void applyWorkWeekSchedules(ProjectCalendar pc) {
        if (userWorkWeeks.isEmpty()) return;
        List<UserWorkWeek> sorted = userWorkWeeks.stream()
                .sorted(Comparator.comparing(UserWorkWeek::getStart))
                .collect(Collectors.toList());

        // Sentinel dates – practical bounds that MPXJ handles without overflow
        LocalDate FAR_PAST   = LocalDate.of(1900, 1, 1);
        LocalDate FAR_FUTURE = LocalDate.of(9999, 12, 31);

        for (int i = 0; i < sorted.size(); i++) {
            UserWorkWeek uww = sorted.get(i);
            WorkWeek     ww  = uww.getWorkWeek();
            // First assignment covers from far past; subsequent ones from their own start date
            LocalDate rangeStart = (i == 0) ? FAR_PAST : uww.getStart();
            LocalDate rangeEnd = (i + 1 < sorted.size())
                    ? sorted.get(i + 1).getStart().minusDays(1)
                    : FAR_FUTURE;

            ProjectCalendarWeek pcw = pc.addWorkWeek();
            pcw.setDateRange(new LocalDateRange(rangeStart, rangeEnd));

            for (DayOfWeek day : DayOfWeek.values()) {
                WorkDaySchedule schedule = ww.getScheduleForDay(day);
                boolean         working  = schedule != null && schedule.isWorkingDay();
                pcw.setWorkingDay(day, working);
                if (working) {
                    ProjectCalendarHours hours = pcw.addCalendarHours(day);
                    if (schedule.getLunchStart() != null && schedule.getLunchEnd() != null) {
                        hours.add(new LocalTimeRange(schedule.getWorkStart(), schedule.getLunchStart()));
                        hours.add(new LocalTimeRange(schedule.getLunchEnd(), schedule.getWorkEnd()));
                    } else {
                        hours.add(new LocalTimeRange(schedule.getWorkStart(), schedule.getWorkEnd()));
                    }
                }
            }
        }

        // Point 2: set per-calendar minutes so MPXJ duration arithmetic reflects this user's schedule
        UserWorkWeek current = getEffectiveUserWorkWeek(LocalDate.now());
        if (current != null && current.getWorkWeek() != null) {
            WorkWeek ww = current.getWorkWeek();
            pc.setCalendarMinutesPerDay(ww.computeMinutesPerDay());
            pc.setCalendarMinutesPerWeek(ww.computeMinutesPerWeek());
        }
    }

    @Override
    public int compareTo(User other) {
        return this.id.compareTo(other.id);
    }

    /**
     * Get the avatar URL with hash parameter for proper caching.
     * When {@code dark} is {@code true} and a dark avatar has been generated, the dark variant URL is
     * returned. If no dark avatar is available yet, falls back transparently to the light variant URL.
     * <p>
     * Typical Vaadin call-site pattern:
     * <pre>
     *     boolean isDark = UI.getCurrent().getElement().getThemeList().contains(Lumo.DARK);
     *     avatarImage.setSrc(user.getAvatarUrl(isDark));
     * </pre>
     *
     * @param dark {@code true} to request the dark-background avatar variant
     * @return The avatar URL with hash parameter for cache-busting; falls back to the light URL when
     * no dark avatar has been stored yet
     */
    @JsonIgnore
    public String getAvatarUrl(boolean dark) {
        if (dark && darkAvatarHash != null && !darkAvatarHash.isEmpty()) {
            return "/frontend/dark-avatar-proxy/user/" + id + "?h=" + darkAvatarHash;
        }
        // Light variant (or dark fallback when dark avatar not yet available)
        String url = "/frontend/avatar-proxy/user/" + id;
        if (lightAvatarHash != null && !lightAvatarHash.isEmpty()) {
            url += "?h=" + lightAvatarHash;
        }
        return url;
    }

    /**
     * Get the light avatar URL with hash parameter for proper caching.
     * Delegates to {@link #getAvatarUrl(boolean)} with {@code dark = false}.
     *
     * @return The light avatar URL with hash parameter if hash is available, otherwise just the base URL
     */
    @JsonIgnore
    public String getAvatarUrl() {
        return getAvatarUrl(false);
    }

    private static String getDefaultAvatarPrompt(String userName) {
        return "Professional avatar portrait of '" + userName + "', software developer";
//        return "close-up portrait of '" + userName + "' for a profile picture, photo quality, sharp focus, high resolution, 8k resolution, 50mm lens";
    }

    /**
     * Return the default negative prompt used when generating dark-background avatars.
     *
     * @return The default dark negative prompt string
     */
    public static String getDefaultDarkAvatarNegativePrompt() {
        return StableDiffusionService.NEGATIVE_PROMPT;
    }

    /**
     * Generate a default dark-background avatar prompt by appending the dark suffix to the base prompt.
     *
     * @return A default dark avatar prompt string
     */
    @JsonIgnore
    public String getDefaultDarkAvatarPrompt() {
        return getDefaultDarkAvatarPrompt(name);
    }

    /**
     * Generate a default dark-background avatar prompt for a given user name.
     *
     * @param userName The name of the user
     * @return A default dark avatar prompt string
     */
    @JsonIgnore
    public static String getDefaultDarkAvatarPrompt(String userName) {
        return getDefaultAvatarPrompt(userName) + AvatarService.DARK_PROMPT_SUFFIX;
    }

    /**
     * Return the default negative prompt used when generating light-background avatars.
     *
     * @return The default negative prompt string
     */
    public static String getDefaultLightAvatarNegativePrompt() {
        return StableDiffusionService.NEGATIVE_PROMPT;
    }

    /**
     * Generate a default avatar prompt for AI image generation.
     * This provides a consistent prompt format for user avatars.
     *
     * @return A default prompt string for generating user avatar images
     */
    @JsonIgnore
    public String getDefaultLightAvatarPrompt() {
        return getDefaultLightAvatarPrompt(name);
    }

    /**
     * Generate a default avatar prompt for AI image generation.
     * This static method provides a consistent prompt format for user avatars.
     *
     * @param userName The name of the user
     * @return A default prompt string for generating user avatar images
     */
    public static String getDefaultLightAvatarPrompt(String userName) {
        return getDefaultAvatarPrompt(userName) + AvatarService.LIGHT_PROMPT_SUFFIX;
    }

    /**
     * Returns the {@link UserWorkWeek} assignment that is effective on the given date.
     * Assignments are sorted by start date; the last one whose start is on or before {@code date}
     * is returned. Returns {@code null} when no assignment covers the date (e.g., pre-hire).
     *
     * @param date the date to look up
     * @return the effective {@link UserWorkWeek}, or {@code null} if none applies
     */
    @JsonIgnore
    public UserWorkWeek getEffectiveUserWorkWeek(LocalDate date) {
        if (userWorkWeeks.isEmpty()) return null;
        List<UserWorkWeek> sorted = userWorkWeeks.stream()
                .sorted(Comparator.comparing(UserWorkWeek::getStart))
                .collect(Collectors.toList());
        UserWorkWeek result = null;
        for (UserWorkWeek uww : sorted) {
            if (!uww.getStart().isAfter(date)) {
                result = uww;
            }
        }
        return result;
    }

    @JsonIgnore
    public String getKey() {
        return "U-" + id;
    }

    /**
     * Get roles as a list
     *
     * @return list of role names
     */
    @JsonIgnore
    public List<String> getRoleList() {
        if (roles == null || roles.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(roles.split(",")));
    }

    /**
     * Check if user has a specific role
     *
     * @param role the role to check
     * @return true if user has the role
     */
    @JsonIgnore
    public boolean hasRole(String role) {
        return getRoleList().contains(role);
    }

    public void initialize(GanttContext gc) {
        ProjectCalendar resourceCalendar = getCalendar();
        if (resourceCalendar == null) {
            resourceCalendar = gc.getProjectFile().addDefaultDerivedCalendar();
            resourceCalendar.setParent(gc.getCalendar());
            resourceCalendar.setName(getName());
            setCalendar(resourceCalendar);
        }

        applyWorkWeekSchedules(resourceCalendar);
        initializeLocationsAndOffdays();
    }

    public void initialize(Sprint sprint) {
        ProjectCalendar resourceCalendar = getCalendar();
        if (resourceCalendar == null) {
            resourceCalendar = sprint.getProjectFile().addDefaultDerivedCalendar();
            resourceCalendar.setParent(sprint.getCalendar());
            resourceCalendar.setName(getName());
            setCalendar(resourceCalendar);
        }

        applyWorkWeekSchedules(resourceCalendar);
        initializeLocationsAndOffdays();
    }

    public void initialize() {
        long            time             = System.currentTimeMillis();
        ProjectCalendar resourceCalendar = getCalendar();
        if (resourceCalendar == null) {
            ProjectFile projectFile = new ProjectFile();
            CalendarUtil.initializeProjectProperties(projectFile);
            ProjectCalendar calendar = CalendarUtil.initializeCalendar(projectFile);
            resourceCalendar = projectFile.addDefaultDerivedCalendar();
            resourceCalendar.setParent(calendar);
            resourceCalendar.setName(getName());
            setCalendar(resourceCalendar);
        }
//        System.out.println("User.initialize() took " + (System.currentTimeMillis() - time) + " ms for user: " + getName());

        applyWorkWeekSchedules(resourceCalendar);
        time = System.currentTimeMillis();
        initializeLocationsAndOffdays();
//        System.out.println("User.initializeLocationsAndOffdays() took " + (System.currentTimeMillis() - time) + " ms for user: " + getName());
    }

    private void initializeLocationsAndOffdays() {
        //TODO rethink employee leaving company and coming back
        ProjectCalendar pc = getCalendar();
        LocalDate       endDateInclusive;
        for (OffDay offDay : getOffDays()) {
            ProjectCalendarException pce = pc.addCalendarException(offDay.getFirstDay(), offDay.getLastDay());
            pce.setName(offDay.getType().name());
        }
        for (int i = 0; i < locations.size(); i++) {
            Location  location           = locations.get(i);
            LocalDate startDateInclusive = location.getStart();
            if (i + 1 < locations.size())
                endDateInclusive = locations.get(i + 1).getStart();//end of this location is start of next location
            else
                endDateInclusive = ParameterOptions.getNow().plusMonths(KassandraProperties.getHolidayLookAheadMonths()).toLocalDate();
            HolidayManager holidayManager = HolidayManager.getInstance(ManagerParameters.create(location.getCountry()));
            List<Holiday>  holidays       = holidayManager.getHolidays(startDateInclusive, endDateInclusive, location.getState()).stream().sorted().collect(Collectors.toList());
            URL            url            = getClass().getClassLoader().getResource("holidays/carnival-holidays.xml");
            if (url != null && "nw".equals(location.getState())) {
                UrlManagerParameter urlManParam        = new UrlManagerParameter(url, new Properties());
                HolidayManager      customManager      = HolidayManager.getInstance(urlManParam);
                Set<Holiday>        additionalHolidays = customManager.getHolidays(startDateInclusive, endDateInclusive, location.getState());
                holidays.addAll(additionalHolidays.stream().toList());
            }
            for (Holiday holiday : holidays) {
                ProjectCalendarException pce = pc.addCalendarException(holiday.getDate());
                pce.setName(String.format("%s (%s/%s)", holiday.getDescription(), location.getCountry(), location.getState()));
            }
        }
    }

    /**
     * Returns {@code true} when the given date is a structurally working day according to the
     * user's effective work-week definition for that date, ignoring holidays and off-day exceptions.
     * Falls back to {@code true} when no work-week assignment covers the date.
     *
     * @param date the date to check
     * @return {@code true} if the day of week is a working day in the applicable work week
     */
    @JsonIgnore
    public boolean isWorkingDay(LocalDate date) {
        UserWorkWeek uww = getEffectiveUserWorkWeek(date);
        if (uww == null || uww.getWorkWeek() == null) return true; // no definition → assume working
        WorkDaySchedule schedule = uww.getWorkWeek().getScheduleForDay(date.getDayOfWeek());
        return schedule != null && schedule.isWorkingDay();
    }

    public void removeAvailability(Availability availability) {
        availabilities.remove(availability);
    }

    public void removeLocation(Location location) {
        locations.remove(location);
    }

    public void removeOffDay(OffDay offDay) {
        offDays.remove(offDay);
    }

    /**
     * Remove a role from this user
     *
     * @param role the role to remove
     */
    @JsonIgnore
    public void removeRole(String role) {
        List<String> roleList = getRoleList();
        roleList.remove(role);
        setRoleList(roleList);
    }

    /**
     * Removes a work-week assignment from this user.
     *
     * @param userWorkWeek the assignment to remove
     */
    public void removeUserWorkWeek(UserWorkWeek userWorkWeek) {
        userWorkWeeks.remove(userWorkWeek);
    }

    /**
     * Set roles from a list
     *
     * @param roleList list of role names
     */
    @JsonIgnore
    public void setRoleList(List<String> roleList) {
        this.roles = roleList.stream()
                .filter(r -> r != null && !r.isEmpty())
                .collect(Collectors.joining(","));
    }

}
