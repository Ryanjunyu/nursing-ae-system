package com.his.ae.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 系统用户表（三角色：NURSE / HEAD_NURSE / ADMIN） */
@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Integer userId;
    private String username;
    private String password;     // BCrypt 加密
    private String realName;
    private String role;         // NURSE / HEAD_NURSE / ADMIN
    private Integer deptId;      // 所属科室（管理员可空）
    private String phone;
    private Integer status;     // 1 启用 0 停用
    private LocalDateTime createTime;
}
