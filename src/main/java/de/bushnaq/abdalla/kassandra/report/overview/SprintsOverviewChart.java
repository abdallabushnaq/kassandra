package de.bushnaq.abdalla.kassandra.report.overview;

import de.bushnaq.abdalla.kassandra.Context;
import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dto.Sprint;
import de.bushnaq.abdalla.kassandra.report.AbstractChart;
import de.bushnaq.abdalla.kassandra.report.burndown.RenderDao;
import de.bushnaq.abdalla.kassandra.report.dao.theme.Theme;
import de.bushnaq.abdalla.kassandra.ui.util.RenderUtil;

import java.time.LocalDateTime;
import java.util.List;

public class SprintsOverviewChart extends AbstractChart {

    public SprintsOverviewChart(Context context, String relateCssPath, String column, String sprintName, Integer limit, LocalDateTime now,
                                List<Sprint> sprintList, int chartWidth, int chartHeight, String cssClass, Theme graphicsTheme) throws Exception {
        super("Project Overview Chart", "", relateCssPath, column, sprintName + "-projectOverviewChart", "project_overview_map", null, graphicsTheme);
        RenderDao dao = RenderUtil.createOverviewRenderDao(context, sprintList, "overview", ParameterOptions.getLocalNow(), 3, chartWidth, chartHeight);
        getRenderers().add(new SprintsOverviewRenderer(dao));
        this.setChartWidth(getRenderers().get(0).chartWidth);
        this.setChartHeight(getRenderers().get(0).chartHeight + captionElement.height + footerElement.height - 1);
        captionElement.width = chartWidth;
        footerElement.y      = getRenderers().get(0).chartHeight + captionElement.height;
    }

    @Override
    protected void createReport() throws Exception {
        getRenderers().get(0).draw(graphics2D, 0, captionElement.height);
    }

}
