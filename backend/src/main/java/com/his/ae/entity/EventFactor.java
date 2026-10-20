package com.his.ae.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 事件诱因记录表（人-机-料-法-环 维度诱因采集） */
@Data
@TableName("event_factor")
public class EventFactor {
    @TableId(type = IdType.AUTO)
    private Integer factorId;
    private Integer eventId;
    private String dimension;       // 人/机/料/法/环
    private String factorType;      // PROXIMAL 近端 / ROOT 根本
    private String factorContent;
    private LocalDateTime createTime;
}
