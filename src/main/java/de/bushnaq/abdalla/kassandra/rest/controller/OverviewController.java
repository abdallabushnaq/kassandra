package de.bushnaq.abdalla.kassandra.rest.controller;

import de.bushnaq.abdalla.kassandra.ParameterOptions;
import de.bushnaq.abdalla.kassandra.rest.dto.SprintOverviewDto;
import de.bushnaq.abdalla.kassandra.service.SprintsOverviewService;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/overview")
@Slf4j
public class OverviewController {

    @Autowired
    private SprintsOverviewService sprintsOverviewService;

    @GetMapping("/sprints")
    @PermitAll
    public SprintOverviewDto getSprintsOverview(
            @RequestParam(required = false) Integer limitMonths,
            @RequestParam(required = false, defaultValue = "light") String theme) {
        LocalDateTime now  = ParameterOptions.getLocalNow();
        boolean       dark = "dark".equalsIgnoreCase(theme);
        return sprintsOverviewService.getOverview(now, limitMonths, dark);
    }

}
