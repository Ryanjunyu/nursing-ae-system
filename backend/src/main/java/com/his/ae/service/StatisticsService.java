package com.his.ae.service;

import com.his.ae.vo.StatItemVO;
import com.his.ae.vo.StatisticsSummaryVO;

import java.util.List;

public interface StatisticsService {

    /** 概览：总量/状态/匿名/高频类型与科室 */
    StatisticsSummaryVO summary();

    /** 各科室事件数量 */
    List<StatItemVO> byDept();

    /** 各事件类型数量 */
    List<StatItemVO> byType();

    /** 各损伤分级数量 */
    List<StatItemVO> byLevel();

    /** 近 N 个月发生趋势 */
    List<StatItemVO> trend(int months);
}
