package com.his.ae.controller;

import com.his.ae.annotation.Audit;
import com.his.ae.common.Result;
import com.his.ae.service.StatisticsService;
import com.his.ae.vo.StatItemVO;
import com.his.ae.vo.StatisticsSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    /** 概览（总量/状态/匿名/高频类型与科室） */
    @GetMapping("/summary")
    @Audit(actionType = "VIEW", targetType = "统计")
    public Result<StatisticsSummaryVO> summary() {
        return Result.ok(statisticsService.summary());
    }

    /** 各科室事件数量 */
    @GetMapping("/by-dept")
    @Audit(actionType = "VIEW", targetType = "统计")
    public Result<List<StatItemVO>> byDept() {
        return Result.ok(statisticsService.byDept());
    }

    /** 各事件类型数量 */
    @GetMapping("/by-type")
    @Audit(actionType = "VIEW", targetType = "统计")
    public Result<List<StatItemVO>> byType() {
        return Result.ok(statisticsService.byType());
    }

    /** 各损伤分级数量 */
    @GetMapping("/by-level")
    @Audit(actionType = "VIEW", targetType = "统计")
    public Result<List<StatItemVO>> byLevel() {
        return Result.ok(statisticsService.byLevel());
    }

    /** 近 N 个月趋势 */
    @GetMapping("/trend")
    @Audit(actionType = "VIEW", targetType = "统计")
    public Result<List<StatItemVO>> trend(@RequestParam(defaultValue = "6") int months) {
        return Result.ok(statisticsService.trend(months));
    }
}
