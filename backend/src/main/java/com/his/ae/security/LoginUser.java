package com.his.ae.security;

import lombok.Data;

/** 当前登录用户上下文 */
@Data
public class LoginUser {
    private Integer userId;
    private String username;
    private String realName;
    private String role;       // NURSE / HEAD_NURSE / ADMIN
    private Integer deptId;
}
