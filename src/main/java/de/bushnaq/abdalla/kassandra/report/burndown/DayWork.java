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

package de.bushnaq.abdalla.kassandra.report.burndown;

import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.dto.Worklog;
import de.bushnaq.abdalla.util.date.DateUtil;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class DayWork {
    public Duration           duration     = Duration.ZERO;
    public List<List<String>> transactions = new ArrayList<>();

    public DayWork() {
    }

    public DayWork(Duration duration) {
        this.duration = duration;
    }

    public void add(Sprint sprint, Worklog work) {
        List<String> list = new ArrayList<>();
        //TODO get key
        list.add(String.format("%s", sprint.getTaskById(work.getTaskId()).getKey()));
        list.add(String.format("<b>%s</b>", DateUtil.create24hDurationString(work.getTimeSpent(), false, true, false)));
        list.add(String.format("%s", work.getComment()));
        transactions.add(list);
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public static String transactionsToTooltips(List<List<String>> transactions, String authorName) {
        if (transactions != null && !transactions.isEmpty()) {
            String tooltip = authorName + " <table><tr> <th><b>Key</b></th> <th><b>Work</b></th> <th><b>Summary</b></th></tr>";

            for (List<String> ll : transactions) {
                tooltip += "<tr>";
                for (String s : ll) {
                    tooltip += String.format("<td>%s</td>", s);
                }
                tooltip += "</tr>";
            }
            tooltip += "</table>";
            return tooltip;
        }
        return "";
    }

}
