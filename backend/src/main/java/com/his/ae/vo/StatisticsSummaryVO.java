package com.his.ae.vo;

import lombok.Data;

/** 统计概览：总量、状态分布、匿名占比、高频类型/科室 */
@Data
public class StatisticsSummaryVO {
    private long total;
    private long reported;
    private long analyzed;
    private long anonymous;
    private String topType;
    private long topTypeCount;
    private String topDept;
    private long topDeptCount;
}
