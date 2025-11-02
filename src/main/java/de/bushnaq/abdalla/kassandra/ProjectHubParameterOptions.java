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

import de.bushnaq.abdalla.kassandra.report.dao.GraphicsLightTheme;
import de.bushnaq.abdalla.util.date.DateUtil;
import org.apache.commons.cli.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ProjectHubParameterOptions extends ParameterOptions {

    @Override
    public void start(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(Option.builder(CLI_OPTION_ENV).hasArgs().desc("Jira connection configuration file list. This parameter is not optional.").build());
        options.addOption(Option.builder(CLI_OPTION_THEME).hasArg().desc("Graphic theme. possible options are dark/light. Default is dark.").build());
        options.addOption(
                Option.builder(CLI_OPTION_DATE).hasArg().desc("Creates a report back in time for a specific date. Format should be 'yyyy.MM.dd'.").build());
        options.addOption(Option.builder(CLI_OPTION_DETAILED).desc("Detailed author speration in burn down chart. This parameter is optional.").build());
        options.addOption(Option.builder(CLI_OPTION_INDIVIDUAL_LOG).desc("Individual log entires will be reported. This parameter is optional.").build());
        options.addOption(Option.builder(CLI_OPTION_QUICK_MODE).desc("Quick mode, only updating active project requests. This parameter is optional.").build());
        options.addOption(Option.builder(CLI_OPTION_RESOURCE_MAP)
                .desc("Resource map mode, adds a tab that shows which resource is busy at what day. This parameter is optional.").build());
        options.addOption(Option.builder(CLI_OPTION_CAPTURE_COST)
                .desc("Capture project cost stored in Resource Allocation database. This parameter is optional.").build());
        options.addOption(Option.builder(CLI_OPTION_REPORT_FOLDER).hasArg()
                .desc("relative or absolute path to root of reporting folder (projects.dashbaord root). This parameter is optional.").build());
        options.addOption(Option.builder(CLI_OPTION_CAPTURE_OUT_OF_OFFICE_FROM_GANTT)
                .desc("Capture out-of-office information from stored in gantt chart files. This parameter is optional.").build());
        // create the parser
        CommandLineParser parser = new DefaultParser();
        // parse the command line arguments
        CommandLine line = parser.parse(options, args);
        if (line.hasOption(CLI_OPTION_THEME)) {
            String theme = line.getOptionValue(CLI_OPTION_THEME);
            if (theme.equals("dark")) {

            } else if (theme.equals("light")) {
                graphicsTheme = new GraphicsLightTheme();
            }
        } else {
            //revert to default
        }
        if (line.hasOption(CLI_OPTION_DATE)) {
            String            dateString    = line.getOptionValue(CLI_OPTION_DATE);
            DateTimeFormatter dtf           = DateTimeFormatter.ofPattern("yyyy.MMM.dd HH:mm");
            LocalDateTime     localDateTime = LocalDateTime.parse(dateString, dtf);
            localDateTime = localDateTime.minusHours(localDateTime.getHour() - 8);
            localDateTime = localDateTime.minusMinutes(localDateTime.getMinute());
            setNow(DateUtil.localDateTimeToOffsetDateTime(localDateTime));
            logger.info("simulating report for " + dtf.format(getNow()));
        } else {
            //revert to default, which is now
        }
        if (line.hasOption(CLI_OPTION_DETAILED)) {
            detailed = true;
            logger.info("detailed report enabled.");
        } else {
            logger.info("detailed report disabled.");
        }
        if (line.hasOption(CLI_OPTION_INDIVIDUAL_LOG)) {
            individualLog = true;
            logger.info("individual log report enabled.");
        } else {
            logger.info("individual log report disabled.");
        }
        if (line.hasOption(CLI_OPTION_QUICK_MODE)) {
            quickMode = true;
            logger.info("quick mode enabled.");
        } else {
            logger.info("quick mode disabled.");
        }
        if (line.hasOption(CLI_OPTION_CAPTURE_COST)) {
            captureCost = true;
            logger.info("capture cost enabled.");
        } else {
            logger.info("capture cost disabled.");
        }
        if (line.hasOption(CLI_OPTION_RESOURCE_MAP)) {
            resourceMap    = true;
            activeRequests = true;//activeDevelopmentRequestList needed as resource
            logger.info("resource map mode enabled.");
        } else {
            logger.info("resource map mode disabled.");
        }
        if (line.hasOption(CLI_OPTION_REPORT_FOLDER)) {
            reportFolder = line.getOptionValue(CLI_OPTION_REPORT_FOLDER);
        }
        if (line.hasOption(CLI_OPTION_ENV)) {
            files = line.getOptionValues(CLI_OPTION_ENV);
        }
        //        {
        //            // automatically generate the help statement
        //            HelpFormatter formatter = new HelpFormatter();
        //            formatter.printHelp("Projects Dashboard", "", options, "", true);
        //            return;
        //        }
        if (line.hasOption(CLI_OPTION_CAPTURE_OUT_OF_OFFICE_FROM_GANTT)) {
            captureOutOfOfficeFromGantt = true;
            logger.info("capture out of office from gantt enabled.");
        } else {
            logger.info("capture out of office from gantt disabled.");
        }
        if (line.hasOption(CLI_OPTION_CAPTURE_OUT_OF_OFFICE_FROM_RAM)) {
            captureOutOfOfficeFromRam = true;
            logger.info("capture out of office from ram enabled.");
        } else {
            logger.info("capture out of office from ram disabled.");
        }

        //        } catch (ParseException exp) {
        //            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
        //        }

        if (activeRequests) {
            logger.info("capture closed requests enabled.");
        }
        if (closingRequests) {
            logger.info("capture closed requests enabled.");
        }
        if (closedRequests) {
            logger.info("capture closed requests enabled.");
        }
    }

}
