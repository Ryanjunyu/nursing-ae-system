package com.his.ae.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 事件列表返回（已脱敏/按权限展示上报人） */
@Data
public class EventListVO {
    private Integer eventId;
    private String eventNo;
    private String deptName;
    private String eventTypeName;
    private String injuryLevelName;
    private LocalDateTime occurTime;
    private String eventDesc;
    private Integer isAnonymous;
    private String reporterDisplay;   // 匿名 / 真实姓名 / （保密）
    private String status;
    private String patientMaskedName;
}
