package de.bushnaq.abdalla.kassandra.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DTO for the interactive frontend sprint overview (v2).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SprintOverviewDto {

    public List<LaneDto> lanes = new ArrayList<>();
    public Meta          meta  = new Meta();

    public static class LaneDto {
        public int             laneId;
        public List<SprintDto> sprints = new ArrayList<>();
    }

    public static class Meta {
        public LocalDateTime chartEnd;
        public LocalDateTime chartStart;
        public Integer       laneCount;
        public LocalDateTime now;
        public String        version = "v2";
        /**
         * Theme colors as a nested class-structured object mirroring Java's {@code Theme} hierarchy.
         * Mirrors Java: {@code Theme → ChartTheme / GanttTheme / XAxesTheme / ...}
         */
        public ThemeDto theme = new ThemeDto();
    }

    public static class SprintDto {
        public String        color; // hex rgba string like #rrggbbaa
        public Boolean       delay;
        public LocalDateTime end;
        public List<String>  exceptions = new ArrayList<>();
        public Boolean       hasGantt;
        public UUID          id;
        public String        jiraUrl;
        public String        key;
        public String        name;
        public LocalDateTime start;
        public String        status;
    }
}


