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

package de.bushnaq.abdalla.kassandra.report.html.dao;

enum EStatus {
    CRITICAL,
    IDLE,
    WARNING
}

@Deprecated
public class ReportCell implements Comparable<ReportCell> {

    public static final String  CRITICAL   = "Critical";
    public static final String  GOOD       = "Good";
    public static final String  IDLE       = "Idle";
    public static final String  IGNORE     = "Ignore";
    public static final String  INFO       = "Info";
    public static final String  WARNING    = "Warning";
    public              int     colspan    = 0;
    public              String  columnName = null;
    public              String  cssClass   = IDLE;
    public              boolean hidden     = false;
    public              String  icon       = null;
    public              int     rowspan    = 0;
    public              String  text       = null;

    public ReportCell(String column, String text) {
        this.columnName = column;
        this.text       = text;
    }

    public ReportCell(String column, String text, boolean hidden) {
        this(column, text);
        this.hidden = hidden;
    }

    public ReportCell(String column, String text, String icon) {
        this(column, text);
        this.icon = icon;
    }

    public ReportCell(String column, String text, String icon, boolean hidden) {
        this(column, text);
        this.icon   = icon;
        this.hidden = hidden;
    }

    public ReportCell(Throwable e) {
        this("Exception", e.getMessage());
        if (text == null) {
            text = e.getClass().getName();
        }
    }

    @Override
    public int compareTo(ReportCell other) {
        if (other == null) {
            return 1;
        }
        if (other.text == null && text == null) {
            return 0;
        }
        if (other.text == null) {
            return 1;
        }
        if (text == null) {
            return -1;
        }
        return text.compareTo(other.text);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ReportCell) {
            return text.equals(((ReportCell) obj).text);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    public ReportCell setColspan(int colspan) {
        this.colspan = colspan;
        return this;
    }

    public ReportCell setColumnName(String columnName) {
        this.columnName = columnName;
        return this;
    }

    public ReportCell setCritical() {
        this.cssClass = CRITICAL;
        return this;
    }

    public ReportCell setCssClass(String cssClass) {
        this.cssClass = cssClass;
        return this;
    }

    public ReportCell setGood() {
        this.cssClass = GOOD;
        return this;
    }

    public ReportCell setHidden(boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    public ReportCell setIdle() {
        this.cssClass = IDLE;
        return this;
    }

    public ReportCell setRowspan(int rowspan) {
        this.rowspan = rowspan;
        return this;
    }

    public ReportCell setText(String text) {
        this.text = text;
        return this;
    }

    public ReportCell setWarning() {
        this.cssClass = WARNING;
        return this;
    }
}
