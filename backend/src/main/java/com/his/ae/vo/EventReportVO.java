package com.his.ae.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/** 不良事件填报表单入参（含必填校验与匿名选项） */
@Data
public class EventReportVO {

    @NotNull(message = "事件发生时间不能为空")
    private LocalDateTime occurTime;

    @NotNull(message = "请选择事件发生科室")
    private Integer deptId;

    @NotBlank(message = "请选择事件类型")
    private String eventType;

    @NotBlank(message = "请选择损伤分级")
    private String injuryLevel;

    @NotBlank(message = "事件经过不能为空")
    private String eventDesc;

    private String sceneDesc;

    /** 是否匿名：1 匿名 0 实名（默认实名） */
    private Integer isAnonymous = 0;

    // ===== 患者信息（脱敏入库，可空；填写则新建模拟患者）=====
    private String patientMaskedName;   // 脱敏姓名 如 张*
    private String patientGender;
    private Integer patientAge;
    private String patientBedNo;
    private String patientAdmissionNo;  // 住院号（脱敏）
    private String patientIdNoMasked;   // 身份证号脱敏
    private String patientDiagnosis;
}
