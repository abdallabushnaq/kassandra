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

package de.bushnaq.abdalla.kassandra.ui;


public class HtmlColor {

    public static String calculateStatusColor(Double delayFraction) {
        if (delayFraction == null) {
            return SprintStatus.NORMAL.name();
        }
        String status = null;
        if (delayFraction <= 0.0) {
            status = SprintStatus.GOOD.name();
        }
        if (delayFraction > 0.0) {
            status = SprintStatus.NORMAL.name();
        }
        if (delayFraction > 0.1) {
            status = SprintStatus.WARNING.name();
        }
        if (delayFraction > 0.2) {
            status = SprintStatus.CRITICAL.name();
        }
        return status;
    }

//    public static String calculateStatusColor(String status) {
//        switch (status) {
//            case SfpsTicket.REQUEST_STATUS_ANALAZING:
//                return status = ReportCell.WARNING;
//            case SfpsTicket.REQUEST_STATUS_ESCALATED:
//                return status = ReportCell.IDLE;
//            case SfpsTicket.REQUEST_STATUS_IN_PROGRESS:
//                return status = ReportCell.IDLE;
//            case SfpsTicket.REQUEST_STATUS_OPEN:
//                return status = ReportCell.WARNING;
//            case SfpsTicket.REQUEST_STATUS_WAITING_FOR_CUSTOMER:
//                return status = ReportCell.IDLE;
//            case AbstractIssue.REQUEST_STATUS_CLOSED:
//                return status = ReportCell.IGNORE;
//            case SfpsTicket.REQUEST_STATUS_RESOLVED:
//                return status = ReportCell.IGNORE;
//            default:
//                return status = ReportCell.IDLE;
//        }
//    }

}
