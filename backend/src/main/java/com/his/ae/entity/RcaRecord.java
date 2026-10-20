package com.his.ae.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** RCA 根因分析记录表（5 维度根因 + 时间序列表 + 改进措施） */
@Data
@TableName("rca_record")
public class RcaRecord {
    @TableId(type = IdType.AUTO)
    private Integer rcaId;
    private Integer eventId;
    private Integer analystId;
    private String timeline;
    private String rootHuman;
    private String rootMachine;
    private String rootMaterial;
    private String rootMethod;
    private String rootEnv;
    private String improvement;
    private String analysisStatus;  // NOT_STARTED/ANALYZING/DONE
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
