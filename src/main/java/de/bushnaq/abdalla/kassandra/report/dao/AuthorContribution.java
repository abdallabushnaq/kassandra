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

package de.bushnaq.abdalla.kassandra.report.dao;

import de.bushnaq.abdalla.util.date.DateUtil;

import java.io.PrintStream;
import java.time.Duration;

public class AuthorContribution {
    public Duration remaining = Duration.ZERO;
    public Duration worked    = Duration.ZERO;

    public AuthorContribution() {
    }

    public AuthorContribution(Duration worked, Duration remaining) {
        this.worked    = worked;
        this.remaining = remaining;
    }

    public void add(Duration worked, Duration remaining) {
        this.worked    = this.worked.plus(worked);
        this.remaining = this.remaining.plus(remaining);
    }

    public void addRemaining(Duration remaining) {
        this.remaining = this.remaining.plus(remaining);
    }

    public void addWorked(Duration worked) {
        this.worked = this.worked.plus(worked);
    }

    public void print(PrintStream out) {
        out.printf("worked=%14s, remaining=%14s\n", DateUtil.createWorkDayDurationString(worked, false, true, false),
                DateUtil.createWorkDayDurationString(remaining, false, true, false));
    }

    public void set(Duration worked, Duration remaining) {
        this.worked    = worked;
        this.remaining = remaining;
    }
}
