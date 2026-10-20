package com.his.ae.vo;

import lombok.Data;

/** 用户信息返回（不含密码） */
@Data
public class UserInfoVO {
    private Integer userId;
    private String username;
    private String realName;
    private String role;          // NURSE / HEAD_NURSE / ADMIN
    private Integer deptId;
    private String deptName;
}
