package com.campus.lab.controller;

import com.campus.lab.common.Result;
import com.campus.lab.dto.LabUsage;
import com.campus.lab.dto.Overview;
import com.campus.lab.dto.StatusCount;
import com.campus.lab.dto.TrendPoint;
import com.campus.lab.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/overview")
    public Result<Overview> overview() {
        return Result.ok(statsService.overview());
    }

    @GetMapping("/trends")
    public Result<List<TrendPoint>> trends(@RequestParam(defaultValue = "14") int days) {
        return Result.ok(statsService.trends(Math.min(Math.max(days, 7), 60)));
    }

    @GetMapping("/lab-usage")
    public Result<List<LabUsage>> labUsage(@RequestParam(defaultValue = "14") int days) {
        return Result.ok(statsService.labUsage(Math.min(Math.max(days, 7), 60)));
    }

    @GetMapping("/status-distribution")
    public Result<List<StatusCount>> statusDistribution() {
        return Result.ok(statsService.statusDistribution());
    }
}
