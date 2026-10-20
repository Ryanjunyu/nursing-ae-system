package com.his.ae.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 系统操作审计日志表（填报/查看/修改/删除/导出 全留痕） */
@Data
@TableName("sys_audit_log")
public class SysAuditLog {
    @TableId(type = IdType.AUTO)
    private Long logId;
    private Integer userId;         // 操作人（匿名记 0）
    private String username;        // 操作人账号（匿名 ANONYMOUS）
    private String role;
    private Integer deptId;
    private String actionType;      // LOGIN/REPORT/VIEW/UPDATE/DELETE/EXPORT
    private String targetType;      // 事件/患者/用户/字典
    private String targetId;
    private String reqParam;        // 请求参数（敏感字段已脱敏）
    private String ipAddr;
    private LocalDateTime createTime;
}
