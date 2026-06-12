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

    public Meta meta = new Meta();
    public List<LaneDto> lanes = new ArrayList<>();

    public static class Meta {
        public LocalDateTime chartStart;
        public LocalDateTime chartEnd;
        public LocalDateTime now;
        public Integer laneCount;
        public String version = "v2";
        // Map of theme color values used by the frontend calendar x-axes.
        // Keys correspond to the server-side XAxesTheme field names.
        // Colors are sent as 0xRRGGBB integers (no alpha).
        public java.util.Map<String, Integer> xAxesTheme = new java.util.HashMap<>();
    }

    public static class LaneDto {
        public int laneId;
        public List<SprintDto> sprints = new ArrayList<>();
    }

    public static class SprintDto {
        public UUID id;
        public String key;
        public String name;
        public LocalDateTime start;
        public LocalDateTime end;
        public String status;
        public String color; // hex rgba string like #rrggbbaa
        public Boolean hasGantt;
        public Boolean delay;
        public String jiraUrl;
        public List<String> exceptions = new ArrayList<>();
    }
}


