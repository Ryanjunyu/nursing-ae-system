package com.his.ae.vo;

import lombok.Data;

/** 事件检索筛选入参 */
@Data
public class EventQueryVO {
    private Integer deptId;            // 科室（管理员可按科室筛选）
    private String eventType;         // 事件类型编码
    private String injuryLevel;        // 损伤分级代码
    private String startTime;          // yyyy-MM-dd HH:mm:ss
    private String endTime;
    private Integer page = 1;
    private Integer size = 10;
}
