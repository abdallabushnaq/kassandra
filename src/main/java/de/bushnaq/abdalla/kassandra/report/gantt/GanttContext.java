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

package de.bushnaq.abdalla.kassandra.report.gantt;

import de.bushnaq.abdalla.kassandra.dto.*;
import de.bushnaq.abdalla.kassandra.dto.Task;
import lombok.Getter;
import net.sf.mpxj.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
public class GanttContext {
    public        List<Feature>   allFeatures = new ArrayList<>();
    public        List<Product>   allProducts = new ArrayList<>();
    public        List<Sprint>    allSprints  = new ArrayList<>();
    public        List<Task>      allTasks    = new ArrayList<>();
    public        List<User>      allUsers    = new ArrayList<>();
    public        List<Version>   allVersions = new ArrayList<>();
    public        List<Worklog>   allWorklogs = new ArrayList<>();
    private       ProjectCalendar calendar;
    private final ProjectFile     projectFile = new ProjectFile();

    private void defineCalendar() {
        final boolean[] DEFAULT_WORKING_WEEK = {false, true, true, true, true, true, false};
        // HolidaysDownloader hd = new HolidaysDownloader();
        // Map<LocalDate, String> downloadHolidays = hd.downloadHolidays();

        // define the base calendar
        calendar = projectFile.addDefaultBaseCalendar();
        calendar.setWorkingDay(DayOfWeek.SATURDAY, false);
        calendar.setWorkingDay(DayOfWeek.SUNDAY, false);
        for (int index = 0; index < 7; index++) {
            DayOfWeek day = DayOfWeek.of(index + 1);
            //            calendar.setWorkingDay(day, DEFAULT_WORKING_WEEK[index]);
            if (calendar.isWorkingDay(day)) {
                ProjectCalendarHours hours = calendar.addCalendarHours(DayOfWeek.of(index + 1));
                hours.add(new LocalTimeRange(LocalTime.of(8, 0), LocalTime.of(12, 0)));
                hours.add(new LocalTimeRange(LocalTime.of(13, 0), LocalTime.of(16, 30)));
            }
        }
//        BankHolidayReader bhr = new BankHolidayReader();
//        context.bankHolidays = bhr.read(context.parameters.smbParameters, ParameterOptions.now.toLocalDate());
//        for (LocalDate ld : context.bankHolidays.keySet()) {
//            // System.out.println(ld.toString() + " " + bankHolidays.get(ld));
//            ProjectCalendarException calendarException = calendar.addCalendarException(ld, ld);
//            calendarException.setName(context.bankHolidays.get(ld));
//        }
    }

    public void initialize() {
        setProjectProperties();
        initializeCalendar();

        if (!allUsers.isEmpty())
            allUsers.forEach(user -> user.initialize(this));
        if (!allProducts.isEmpty())
            allProducts.forEach(product -> product.initialize(this));
        else if (!allVersions.isEmpty())
            allVersions.forEach(version -> version.initialize(this));
        else if (!allFeatures.isEmpty())
            allFeatures.forEach(project -> project.initialize(this));
        else if (!allSprints.isEmpty())
            allSprints.forEach(sprint -> sprint.initialize(this));
    }

    private void initializeCalendar() {
        defineCalendar();
        ProjectProperties projectProperties = projectFile.getProjectProperties();
        ProjectCalendar   projectCalendar   = projectFile.getCalendarByName(ProjectCalendar.DEFAULT_BASE_CALENDAR_NAME);
        if (projectCalendar == null) {
            projectCalendar = projectFile.getDefaultCalendar();
        }
        if (projectCalendar == null) {
            projectCalendar = projectFile.addDefaultBaseCalendar();
            for (DayOfWeek day : DayOfWeek.values()) {
                if (projectCalendar.getCalendarDayType(day) == DayType.WORKING) {
                    ProjectCalendarHours hours = projectCalendar.getCalendarHours(day);
                    hours.clear();
                    hours.add(new LocalTimeRange(LocalTime.of(8, 0), LocalTime.of(12, 0)));
                    hours.add(new LocalTimeRange(LocalTime.of(13, 0), LocalTime.of(16, 30)));
                }
            }
//            BankHolidayReader bhr = new BankHolidayReader();
//            context.bankHolidays = bhr.read(context.parameters.smbParameters, ParameterOptions.now.toLocalDate());
//            for (LocalDate ld : context.bankHolidays.keySet()) {
//                // System.out.println(ld.toString() + " " + bankHolidays.get(ld));
//                ProjectCalendarException calendarException = projectCalendar.addCalendarException(ld, ld);
//                calendarException.setName(context.bankHolidays.get(ld));
//            }

        }
        ProjectCalendar projectPropertiesCalendar = projectProperties.getDefaultCalendar();
        if (projectPropertiesCalendar == null) {
            projectProperties.setDefaultCalendar(projectCalendar);
        }

    }

    private void setProjectProperties() {
        ProjectProperties properties = projectFile.getProjectProperties();
//        properties.setProjectTitle(new File(XlsxUtil.removeExtension(xlsxFile)).getName());
//        properties.setAuthor("XLSX-to-MPP " + moduleVersion);
        properties.setCurrentDate(null);
        properties.setDefaultStartTime(LocalTime.of(8, 0));
        properties.setDefaultEndTime(LocalTime.of(16, 30));
        properties.setMinutesPerDay((int) (7.5 * 60));
        properties.setMinutesPerWeek(5 * (int) (7.5 * 60));
        //        properties.setDefaultDurationUnits(TimeUnit.HOURS);
        //        TaskUtil.createMetaData(projectFile);
        //        Map<String, Object> customProperties = new HashMap<>();
        //        HashMap<Task, MetaData> mdMap = new HashMap<>();
        //        customProperties.put(MetaData.METADATA, mdMap);
        //        properties.setCustomProperties(customProperties);
        //        HashMap<Task, MetaData> mdMap1 = (HashMap<Task, MetaData>)projectfile.getProjectProperties().getCustomProperties().get(MetaData.METADATA);
    }

}
