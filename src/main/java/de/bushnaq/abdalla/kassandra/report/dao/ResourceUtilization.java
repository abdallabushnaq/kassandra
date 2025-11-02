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

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ResourceUtilization {
    private Double[] aw         = null;
    private Double   maxUnit    = null;
    private Double[] nonProject = null;

    private Double[] sickness = null;

    private Double[] vacation = null;

    public ResourceUtilization(LocalDate firstDay, int days) {
        //        this.firstDay = firstDay;
        aw = new Double[days];
        for (int i = 0; i < days; i++) {
            aw[i] = 0.0;
        }
        vacation   = new Double[days];
        sickness   = new Double[days];
        nonProject = new Double[days];
    }

    public ResourceUtilization(LocalDateTime firstDay, int days) {
        this(firstDay.toLocalDate(), days);
    }

    public void add(int index, Double value) {
        if (index > 0 && index < aw.length) {
            aw[index] += value;
        }
    }

    public void addNonProject(int index, double value) {
        if (index > 0 && index < nonProject.length) {
            if (nonProject[index] == null) {
                nonProject[index] = 0.0;
            }
            nonProject[index] += value;
        }
    }

    public void addSickness(int index, double value) {
        if (index > 0 && index < sickness.length) {
            if (sickness[index] == null) {
                sickness[index] = 0.0;
            }
            sickness[index] += value;
        }
    }

    public void addVacation(int index, Double value) {
        if (index > 0 && index < vacation.length) {
            if (vacation[index] == null) {
                vacation[index] = 0.0;
            }
            vacation[index] += value;
        }
    }

    public Double get(int index) {
        return aw[index];
    }

    public Double getMaxUnit() {
        return maxUnit;
    }

    public Double getNonProject(int index) {
        if (index > -1 && index < nonProject.length) {
            return nonProject[index];
        } else {
            return null;
        }
    }

    public Double getSickness(int index) {
        if (index > -1 && index < sickness.length) {
            return sickness[index];
        } else {
            return null;
        }
    }

    public int getSize() {
        return aw.length;
    }

    public Double getVacation(int index) {
        if (index > -1 && index < vacation.length) {
            return vacation[index];
        } else {
            return null;
        }
    }

    public void setMaxUnit(Double maxUnit) {
        this.maxUnit = maxUnit;
    }

}
