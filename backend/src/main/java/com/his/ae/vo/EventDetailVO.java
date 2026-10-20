package com.his.ae.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 事件明细返回（按权限；患者信息脱敏） */
@Data
public class EventDetailVO {
    private Integer eventId;
    private String eventNo;
    private String deptName;
    private String eventTypeName;
    private String injuryLevelName;
    private LocalDateTime occurTime;
    private String eventDesc;
    private String sceneDesc;
    private Integer isAnonymous;
    private String reporterDisplay;
    private String status;

    // 患者（脱敏）
    private String patientMaskedName;
    private String patientGender;
    private Integer patientAge;
    private String patientBedNo;
    private String patientAdmissionNo;
    private String patientDiagnosis;
}
