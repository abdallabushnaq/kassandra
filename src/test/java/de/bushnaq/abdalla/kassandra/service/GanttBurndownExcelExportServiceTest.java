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
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package de.bushnaq.abdalla.kassandra.service;

import de.bushnaq.abdalla.kassandra.rest.dto.gantt.AuthorSeriesDto;
import de.bushnaq.abdalla.kassandra.rest.dto.gantt.GanttBurndownChartDto;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("UnitTest")
public class GanttBurndownExcelExportServiceTest {

    @Test
    void exportsDailyValuesAndChart() throws Exception {
        GanttBurndownChartDto chart = new GanttBurndownChartDto();
        chart.meta.sprintName = "Sprint 1";
        chart.meta.chartStart = LocalDateTime.of(2026, 8, 3, 0, 0);
        chart.meta.chartEnd = LocalDateTime.of(2026, 8, 5, 0, 0);
        chart.meta.totalDays = 3;
        chart.authors.add(author("Alice", List.of(0L, 3600L, 7200L), 7200L, 3600L));
        chart.authors.add(author("Bob", List.of(0L, 1800L, 3600L), 3600L, 7200L));
        chart.ganttGuideWithBuffer = List.of(14400L, 10800L, 7200L);
        chart.ganttGuideWithoutBuffer = List.of(10800L, 7200L, 3600L);

        byte[] bytes = new GanttBurndownExcelExportService().export(chart);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertEquals("Burndown", workbook.getSheetAt(0).getSheetName());
            assertEquals("Summary", workbook.getSheetAt(1).getSheetName());
            assertEquals(3, workbook.getSheet("Burndown").getLastRowNum());
            assertEquals("2026-08-04", workbook.getSheet("Burndown").getRow(2).getCell(0).getStringCellValue());
            assertEquals(1.0, workbook.getSheet("Burndown").getRow(2).getCell(1).getNumericCellValue());
            assertEquals(1.5, workbook.getSheet("Burndown").getRow(2).getCell(3).getNumericCellValue());
            assertEquals(4.0, workbook.getSheet("Burndown").getRow(1).getCell(4).getNumericCellValue());
            assertEquals(3.0, workbook.getSheet("Burndown").getRow(1).getCell(5).getNumericCellValue());
            assertEquals(1, ((XSSFSheet) workbook.getSheet("Burndown")).getDrawingPatriarch().getCharts().size());
        }
    }

    private AuthorSeriesDto author(String name, List<Long> values, long totalWorked, long totalRemaining) {
        AuthorSeriesDto author = new AuthorSeriesDto();
        author.userName = name;
        author.accumulatedWorkPerDay.addAll(values);
        author.totalWorkedSeconds = totalWorked;
        author.totalRemainingSeconds = totalRemaining;
        return author;
    }
}
