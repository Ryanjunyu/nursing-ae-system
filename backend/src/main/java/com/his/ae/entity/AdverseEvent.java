package com.his.ae.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 不良事件主表（核心业务表） */
@Data
@TableName("adverse_event")
public class AdverseEvent {
    @TableId(type = IdType.AUTO)
    private Integer eventId;
    private String eventNo;          // 事件编号（系统生成）
    private Integer reporterId;      // 上报人（匿名时 NULL）
    private String reporterRole;     // 上报人角色快照
    private Integer isAnonymous;     // 1 匿名 0 实名
    private Integer deptId;          // 事件发生科室
    private Integer patientId;       // 关联患者（可空）
    private String eventType;        // 事件类型编码
    private String injuryLevel;      // 损伤分级 I/II/III/IV
    private LocalDateTime occurTime;  // 事件发生时间
    private String eventDesc;        // 事件经过
    private String sceneDesc;        // 现场情况
    private String status;           // REPORTED/REVIEWING/ANALYZED/CLOSED
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
