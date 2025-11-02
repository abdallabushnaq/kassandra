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

package de.bushnaq.abdalla.kassandra;

import de.bushnaq.abdalla.kassandra.report.dao.BurnDownGraphicsTheme;
import de.bushnaq.abdalla.kassandra.report.dao.GraphicsLightTheme;
import de.bushnaq.abdalla.util.date.DateUtil;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;


public abstract class ParameterOptions {
    protected static final String                CLI_OPTION_CAPTURE_COST                     = "capturecost";
    protected static final String                CLI_OPTION_CAPTURE_OUT_OF_OFFICE_FROM_GANTT = "captureoutofofficefromgantt";
    protected static final String                CLI_OPTION_CAPTURE_OUT_OF_OFFICE_FROM_RAM   = "captureoutofofficefromram";
    protected static final String                CLI_OPTION_DATE                             = "date";
    protected static final String                CLI_OPTION_DETAILED                         = "detailed";
    protected static final String                CLI_OPTION_DISABLE_PROXY                    = "disableproxy";
    protected static final String                CLI_OPTION_ENV                              = "env";
    protected static final String                CLI_OPTION_INDIVIDUAL_LOG                   = "individuallog";
    protected static final String                CLI_OPTION_QUERY_TEAM_PLANNER               = "queryteamplanner";
    protected static final String                CLI_OPTION_QUICK_MODE                       = "quick";
    protected static final String                CLI_OPTION_REPORT_FOLDER                    = "reportfolder";
    protected static final String                CLI_OPTION_RESOURCE_MAP                     = "resourcemap";
    protected static final String                CLI_OPTION_THEME                            = "theme";
    protected static final String                CLI_OPTION_VERBOSE                          = "verbose";
    protected static final String                CLI_OPTION_XLSX_FILE                        = "xlsxfile";
    public                 boolean               activeRequests                              = true;//for test purposes
    public                 boolean               captureCost                                 = false;//for cost charts
    public                 boolean               captureOutOfOfficeFromGantt                 = false;//get out of office information from Gantt charts
    public                 boolean               captureOutOfOfficeFromRam                   = false;//get out of office information from RAM db
    public                 boolean               closedRequests                              = true;
    public                 boolean               closingRequests                             = true;
    public                 boolean               detailed                                    = true;//should always be true, otherwise resources will not be shown in burn down chart
    public                 List<Throwable>       exceptions                                  = new ArrayList<>();
    public                 String[]              files                                       = {};
    public                 BurnDownGraphicsTheme graphicsTheme                               = new GraphicsLightTheme();
    public                 boolean               individualLog                               = false;//resource individual log visible as drill down list of work burn down chart. Legacy!
    public                 Integer               limitProjectOverview                        = null;// 6 * 30;
    public                 Integer               limitResourceUtilization                    = null;// 1 * 30;
    protected final        Logger                logger                                      = LoggerFactory.getLogger(this.getClass());
    @Setter
    private static         OffsetDateTime        now;
    public                 boolean               outOfOfficeOnly                             = false;//no ramdb cost records
    public                 boolean               queryTeamPlanner;
    public                 boolean               quickMode                                   = false;//only query active projects and resource map
    public                 String                reportFolder                                = "./";
    public                 boolean               resourceMap                                 = false;
    public                 boolean               resourceUtilizationPane                     = false;//only used in tests to cover code, currently cannot be enabled in production mode
    public                 boolean               verbose                                     = false;//in verbose mode, temporary <filename>-tp.xml file will not be deleted.
    public                 String                xlsxFile                                    = null;//used only by Xlsx2mppMain

    public static LocalDateTime getLocalNow() {
        return DateUtil.offsetDateTimeToLocalDateTime(now);
    }

    public static OffsetDateTime getNow() {
        if (now != null)
            return now;
        return OffsetDateTime.now();
    }

    public abstract void start(String[] args) throws Exception;

}
