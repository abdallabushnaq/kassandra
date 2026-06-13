package de.bushnaq.abdalla.kassandra.service;

import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.dao.SprintDAO;
import de.bushnaq.abdalla.kassandra.dto.Status;
import de.bushnaq.abdalla.kassandra.report.dao.theme.DarkTheme;
import de.bushnaq.abdalla.kassandra.report.dao.theme.LightTheme;
import de.bushnaq.abdalla.kassandra.report.dao.theme.Theme;
import de.bushnaq.abdalla.kassandra.report.dao.theme.XAxesTheme;
import de.bushnaq.abdalla.kassandra.repository.FeatureRepository;
import de.bushnaq.abdalla.kassandra.repository.SprintRepository;
import de.bushnaq.abdalla.kassandra.repository.VersionRepository;
import de.bushnaq.abdalla.kassandra.rest.dto.SprintOverviewDto;
import de.bushnaq.abdalla.util.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

@Service
@Slf4j
public class SprintsOverviewService {

    private final DarkTheme         darkTheme;
    private final FeatureRepository featureRepository;
    private final LightTheme        lightTheme;
    private final ProductAclService productAclService;
    private final SprintRepository  sprintRepository;
    private final VersionRepository versionRepository;

    @Autowired
    public SprintsOverviewService(SprintRepository sprintRepository,
                                  FeatureRepository featureRepository,
                                  VersionRepository versionRepository,
                                  ProductAclService productAclService,
                                  LightTheme lightTheme,
                                  DarkTheme darkTheme) {
        this.sprintRepository  = sprintRepository;
        this.featureRepository = featureRepository;
        this.versionRepository = versionRepository;
        this.productAclService = productAclService;
        this.lightTheme        = lightTheme;
        this.darkTheme         = darkTheme;
    }

    private <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<>(c);
        Collections.sort(list);
        return list;
    }

    private int findFirstFreeLane(Map<Integer, List<SprintDAO>> projectScheduleList, SprintDAO o) {
        LocalDateTime os = o.getStart();
        LocalDateTime of = o.getEnd();
        if (os != null && of != null) {
            for (int laneId : asSortedList(projectScheduleList.keySet())) {
                boolean overlapping = false;
                for (SprintDAO p : projectScheduleList.get(laneId)) {
                    LocalDateTime ps = p.getStart();
                    LocalDateTime pf = p.getEnd();
                    if (ps != null && pf != null) {
                        overlapping = DateUtil.isOverlapping(os, of, overlapping, ps, pf);
                    }
                }
                if (!overlapping) return laneId;
            }
            return projectScheduleList.size();
        }
        return -1;
    }

    public SprintOverviewDto getOverview(LocalDateTime now, Integer limitMonths, boolean dark) {
        if (now == null) now = ParameterOptions.getLocalNow();

        List<SprintDAO> sprints = sprintRepository.findAll();
        // Ensure we operate on a modifiable list: repositories or tests may return immutable lists
        sprints = new ArrayList<>(sprints);

        // apply same ACL filtering as SprintController.getAll
//        if (!SecurityUtils.isAdmin()) {
//            sprints = sprints.stream().filter(sprint -> {
//                UUID productId = featureRepository.findById(sprint.getFeatureId())
//                        .flatMap(feature -> versionRepository.findById(feature.getVersionId()))
//                        .map(version -> version.getProductId())
//                        .orElse(null);
//                return productId != null && productAclService.hasAccess(productId, SecurityUtils.getUserEmail());
//            }).toList();
//            // Stream.toList() may return an unmodifiable list. Make sure we have a modifiable list
//            sprints = new ArrayList<>(sprints);
//        }

        // sort by start date then id to get deterministic order
        sprints.sort(Comparator.comparing(SprintDAO::getStart, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(SprintDAO::getId));

        SprintOverviewDto dto = new SprintOverviewDto();
        dto.meta.now = now;

        // find chart extents
        // Start with sensible defaults. Previously maxDate was initialized to 1900-01-01 which
        // produced an inverted range when no sprints existed (chartStart > chartEnd) and
        // caused the client-side time scale to collapse all ticks into a few pixels.
        LocalDate minDate = LocalDate.now();
        LocalDate maxDate = LocalDate.parse("1900-01-01");

        for (SprintDAO s : sprints) {
            if (s.getStart() != null) {
                LocalDate sd = s.getStart().toLocalDate();
                if (sd.isBefore(minDate)) minDate = sd;
            }
            if (s.getEnd() != null) {
                LocalDate ed = s.getEnd().toLocalDate();
                if (ed.isAfter(maxDate)) maxDate = ed;
            }
        }

        // apply optional limitMonths: ensure first date not earlier than now - limit
        if (limitMonths != null) {
            LocalDate cutDay = DateUtil.addDay(now.toLocalDate(), -(limitMonths * 30));
            if (cutDay.isBefore(minDate)) {
                minDate = cutDay;
            }
        }

        // If no sprints were found the computed maxDate may still be the placeholder (1900-01-01)
        // or otherwise be before minDate. In that case provide a reasonable default range
        // so the frontend scale has a non-inverted domain (e.g. 6 months forward).
        if (maxDate.isBefore(minDate)) {
            maxDate = minDate.plusMonths(6);
        }

        dto.meta.chartStart = minDate.atStartOfDay();
        dto.meta.chartEnd   = maxDate.atStartOfDay();

        // compute lanes: reuse overlapping logic. Only consider sprints with both start and end for lane placement
        Map<Integer, List<SprintDAO>> projectScheduleList = new TreeMap<>();

        for (SprintDAO sprint : sprints) {
            // Accept sprints that have at least one temporal endpoint. If one endpoint is missing
            // we treat the sprint as a point-in-time (start==end) for placement and rendering so
            // it is visible in the overview. Sprints with neither start nor end are skipped.
            LocalDateTime sStart = sprint.getStart();
            LocalDateTime sEnd   = sprint.getEnd();
            if (sStart == null && sEnd == null) continue;
            if (sStart == null) sStart = sEnd;
            if (sEnd == null) sEnd = sStart;

            // visibility: include if end >= chartStart
            if (!sEnd.toLocalDate().isBefore(minDate)) {
                // Create a lightweight wrapper object with the inferred dates for lane placement
                final LocalDateTime fs = sStart;
                final LocalDateTime fe = sEnd;
                int laneId = findFirstFreeLane(projectScheduleList, new SprintDAO() {
                    @Override
                    public LocalDateTime getEnd() {
                        return fe;
                    }

                    // anonymous SprintDAO override to provide start/end for overlapping check
                    @Override
                    public LocalDateTime getStart() {
                        return fs;
                    }
                });
                if (laneId != -1) {
                    projectScheduleList.computeIfAbsent(laneId, k -> new ArrayList<>()).add(sprint);
                }
            }
        }

        dto.meta.laneCount = projectScheduleList.size();

        // fill lanes in DTO
        for (int laneId : projectScheduleList.keySet()) {
            SprintOverviewDto.LaneDto lane = new SprintOverviewDto.LaneDto();
            lane.laneId = laneId;
            for (SprintDAO s : projectScheduleList.get(laneId)) {
                SprintOverviewDto.SprintDto sd = new SprintOverviewDto.SprintDto();
                sd.id   = s.getId();
                sd.key  = "S-" + s.getId();
                sd.name = s.getName();
                // Infer missing endpoints so the frontend always receives concrete start/end datetimes
                LocalDateTime sStart = s.getStart();
                LocalDateTime sEnd   = s.getEnd();
                if (sStart == null && sEnd != null) sStart = sEnd;
                if (sEnd == null && sStart != null) sEnd = sStart;
                sd.start    = sStart;
                sd.end      = sEnd;
                sd.status   = s.getStatus() == null ? null : s.getStatus().name();
                sd.hasGantt = s.getStart() != null && s.getEnd() != null; // preserve original hasGantt semantics
                sd.delay    = Boolean.FALSE; // frontend can compute delay if needed
                sd.color    = getStatusColorHex(s.getStatus());
                lane.sprints.add(sd);
            }
            dto.lanes.add(lane);
        }

        // --- add x-axes theme colors for the frontend (keys match XAxesTheme field names)
        try {
            Theme                                       theme = dark ? darkTheme : lightTheme;
            XAxesTheme                                  xt    = theme.xAxesTheme;
            java.util.function.Function<Color, Integer> rgb   = c -> c == null ? null : ((c.getRed() & 0xff) << 16) | ((c.getGreen() & 0xff) << 8) | (c.getBlue() & 0xff);
            dto.meta.xAxesTheme.put("gridColor", rgb.apply(theme.ganttTheme.gridColor));

            dto.meta.xAxesTheme.put("dayOfMonthBgColor", rgb.apply(xt.dayOfMonthBgColor));
            dto.meta.xAxesTheme.put("dayOfMonthBorderColor", rgb.apply(xt.dayOfMonthBorderColor));
            dto.meta.xAxesTheme.put("dayOfMonthTextColor", rgb.apply(xt.dayOfMonthTextColor));
            dto.meta.xAxesTheme.put("dayOfMonthWeekendBgColor", rgb.apply(xt.dayOfMonthWeekendBgColor));
            dto.meta.xAxesTheme.put("dayOfMonthWeekendTextColor", rgb.apply(xt.dayOfMonthWeekendTextColor));

            dto.meta.xAxesTheme.put("dayOfWeekBorderColor", rgb.apply(xt.dayOfWeekBorderColor));
            dto.meta.xAxesTheme.put("dayOfWeekTextColor", rgb.apply(xt.dayOfWeekTextColor));
            dto.meta.xAxesTheme.put("dayOfWeekWeekendTextColor", rgb.apply(xt.dayOfWeekWeekendTextColor));
            dto.meta.xAxesTheme.put("dayOfweekBgColor", rgb.apply(xt.dayOfweekBgColor));
            dto.meta.xAxesTheme.put("dayOfweekSaturdayBgColor", rgb.apply(xt.dayOfweekSaturdayBgColor));
            dto.meta.xAxesTheme.put("dayOfweekSundayBgColor", rgb.apply(xt.dayOfweekSundayBgColor));

            dto.meta.xAxesTheme.put("futureEventColor", rgb.apply(xt.futureEventColor));
            dto.meta.xAxesTheme.put("milestoneFlagColor", rgb.apply(xt.milestoneFlagColor));
            dto.meta.xAxesTheme.put("milestoneTextColor", rgb.apply(xt.milestoneTextColor));

            for (int i = 0; i < xt.monthBgColors.length; i++) {
                dto.meta.xAxesTheme.put(String.format("monthBgColors.%d", i), rgb.apply(xt.monthBgColors[i]));
            }
            dto.meta.xAxesTheme.put("monthBorderColor", rgb.apply(xt.monthBorderColor));
            dto.meta.xAxesTheme.put("monthTextColor", rgb.apply(xt.monthTextColor));

            dto.meta.xAxesTheme.put("nowEventColor", rgb.apply(xt.nowEventColor));
            dto.meta.xAxesTheme.put("pastEventColor", rgb.apply(xt.pastEventColor));

            dto.meta.xAxesTheme.put("weekBgColor", rgb.apply(xt.weekBgColor));
            dto.meta.xAxesTheme.put("weekBoderColor", rgb.apply(xt.weekBorderColor));
            dto.meta.xAxesTheme.put("weekTextColor", rgb.apply(xt.weekTextColor));

            dto.meta.xAxesTheme.put("yearBgColor", rgb.apply(xt.yearBgColor));
            dto.meta.xAxesTheme.put("yearBorderColor", rgb.apply(xt.yearBorderColor));
            dto.meta.xAxesTheme.put("yearTextColor", rgb.apply(xt.yearTextColor));
        } catch (Exception ignored) {
            // non-fatal: if theme cannot be constructed, leave map empty
        }

        return dto;
    }

    private String getStatusColorHex(Status status) {
        Color color = switch (status) {
            case CREATED -> new Color(0x0, 0x0, 0x0, 0xa0);
            case STARTED -> new Color(0x1f, 0x8f, 0xff, 0x50);
            case CLOSED -> new Color(0x0, 0x0, 0x0, 0x20);
            default -> new Color(0x0, 0x0, 0x0, 0xa0);
        };
        return String.format("#%02x%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

}



