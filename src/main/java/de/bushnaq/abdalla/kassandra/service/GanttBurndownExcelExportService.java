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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Creates Excel workbooks containing the data shown by the Gantt/burndown chart.
 */
@Service
public class GanttBurndownExcelExportService {
    private static final double                    SECONDS_PER_HOUR = 3600.0;
    private static final DateTimeFormatter         DATE_FORMAT      = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String                     BURNDOWN_SHEET   = "Burndown";
    private static final String                     SUMMARY_SHEET    = "Summary";

    /**
     * Exports all rendered burndown series as a LibreOffice-compatible XLSX workbook.
     *
     * @param chart the server-side chart data transferred to the browser
     * @return XLSX bytes containing the daily values, summary, and a line chart
     * @throws IOException if Apache POI cannot write the workbook
     */
    public byte[] export(GanttBurndownChartDto chart) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            createBurndownSheet(workbook, chart, headerStyle);
            createSummarySheet(workbook, chart, headerStyle);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void createBurndownSheet(XSSFWorkbook workbook, GanttBurndownChartDto chart, CellStyle headerStyle) {
        XSSFSheet sheet = workbook.createSheet(BURNDOWN_SHEET);
        Row       header = sheet.createRow(0);
        int       column = 0;
        createHeaderCell(header, column++, "Date", headerStyle);
        for (AuthorSeriesDto author : chart.authors) {
            createHeaderCell(header, column++, author.userName + " - cumulative actual (h)", headerStyle);
        }
        int actualTotalColumn = column;
        createHeaderCell(header, column++, "Actual total - cumulative (h)", headerStyle);
        int guideWithBufferColumn = column;
        createHeaderCell(header, column++, "Gantt guide with buffer - remaining (h)", headerStyle);
        int guideWithoutBufferColumn = column;
        createHeaderCell(header, column, "Gantt guide without buffer - remaining (h)", headerStyle);

        int dayCount = calculateDayCount(chart);
        for (int day = 0; day < dayCount; day++) {
            Row row = sheet.createRow(day + 1);
            row.createCell(0).setCellValue(chart.meta.chartStart.toLocalDate().plusDays(day).format(DATE_FORMAT));
            double actualTotal = 0;
            for (int authorIndex = 0; authorIndex < chart.authors.size(); authorIndex++) {
                double actual = secondsToHours(valueAt(chart.authors.get(authorIndex).accumulatedWorkPerDay, day));
                row.createCell(authorIndex + 1).setCellValue(actual);
                actualTotal += actual;
            }
            row.createCell(actualTotalColumn).setCellValue(actualTotal);
            row.createCell(guideWithBufferColumn).setCellValue(secondsToHours(valueAt(chart.ganttGuideWithBuffer, day)));
            row.createCell(guideWithoutBufferColumn).setCellValue(secondsToHours(valueAt(chart.ganttGuideWithoutBuffer, day)));
        }

        sheet.createFreezePane(1, 1);
        for (int index = 0; index <= guideWithoutBufferColumn; index++) {
            sheet.autoSizeColumn(index);
        }
        if (dayCount > 0) {
            createChart(sheet, dayCount, actualTotalColumn, guideWithBufferColumn, guideWithoutBufferColumn);
        }
    }

    private void createChart(XSSFSheet sheet, int dayCount, int actualTotalColumn, int guideWithBufferColumn,
            int guideWithoutBufferColumn) {
        XSSFDrawing     drawing = sheet.createDrawingPatriarch();
        XSSFChart       chart   = drawing.createChart(drawing.createAnchor(0, 0, 0, 0, 1, 3, 13, 21));
        XDDFCategoryAxis categoryAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis    valueAxis    = chart.createValueAxis(AxisPosition.LEFT);
        valueAxis.setCrosses(AxisCrosses.AUTO_ZERO);

        XDDFDataSource<String>       dates = XDDFDataSourcesFactory.fromStringCellRange(sheet, new CellRangeAddress(1, dayCount, 0, 0));
        XDDFChartData                 data  = chart.createData(ChartTypes.LINE, categoryAxis, valueAxis);
        String[]                      names = {"Actual total - cumulative (h)", "Gantt guide with buffer - remaining (h)",
                "Gantt guide without buffer - remaining (h)"};
        int[]                         columns = {actualTotalColumn, guideWithBufferColumn, guideWithoutBufferColumn};
        for (int index = 0; index < columns.length; index++) {
            XDDFNumericalDataSource<Double> values = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
                    new CellRangeAddress(1, dayCount, columns[index], columns[index]));
            XDDFChartData.Series series = data.addSeries(dates, values);
            series.setTitle(names[index], null);
        }
        chart.plot(data);
        chart.setTitleText("Burndown values");
        chart.getOrAddLegend().setPosition(LegendPosition.BOTTOM);
    }

    private void createSummarySheet(XSSFWorkbook workbook, GanttBurndownChartDto chart, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet(SUMMARY_SHEET);
        createHeaderCell(sheet.createRow(0), 0, "Sprint", headerStyle);
        sheet.getRow(0).createCell(1).setCellValue(chart.meta.sprintName == null ? "" : chart.meta.sprintName);
        createHeaderCell(sheet.createRow(1), 0, "Chart start", headerStyle);
        sheet.getRow(1).createCell(1).setCellValue(chart.meta.chartStart == null ? "" : chart.meta.chartStart.toString());
        createHeaderCell(sheet.createRow(2), 0, "Chart end", headerStyle);
        sheet.getRow(2).createCell(1).setCellValue(chart.meta.chartEnd == null ? "" : chart.meta.chartEnd.toString());

        Row authorHeader = sheet.createRow(4);
        createHeaderCell(authorHeader, 0, "Author", headerStyle);
        createHeaderCell(authorHeader, 1, "Actual work (h)", headerStyle);
        createHeaderCell(authorHeader, 2, "Remaining work (h)", headerStyle);
        for (int index = 0; index < chart.authors.size(); index++) {
            AuthorSeriesDto author = chart.authors.get(index);
            Row             row    = sheet.createRow(index + 5);
            row.createCell(0).setCellValue(author.userName);
            row.createCell(1).setCellValue(secondsToHours(author.totalWorkedSeconds));
            row.createCell(2).setCellValue(secondsToHours(author.totalRemainingSeconds));
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
    }

    private int calculateDayCount(GanttBurndownChartDto chart) {
        int dayCount = chart.meta.totalDays;
        for (AuthorSeriesDto author : chart.authors) {
            dayCount = Math.max(dayCount, author.accumulatedWorkPerDay.size());
        }
        dayCount = Math.max(dayCount, chart.ganttGuideWithBuffer == null ? 0 : chart.ganttGuideWithBuffer.size());
        return Math.max(dayCount, chart.ganttGuideWithoutBuffer == null ? 0 : chart.ganttGuideWithoutBuffer.size());
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font      font  = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void createHeaderCell(Row row, int column, String value, CellStyle headerStyle) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(headerStyle);
    }

    private double secondsToHours(long seconds) {
        return seconds / SECONDS_PER_HOUR;
    }

    private long valueAt(List<Long> values, int index) {
        return values != null && index < values.size() && values.get(index) != null ? values.get(index) : 0;
    }
}
