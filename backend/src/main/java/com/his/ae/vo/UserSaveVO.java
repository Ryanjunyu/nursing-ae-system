package com.his.ae.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 用户新增/修改入参 */
@Data
public class UserSaveVO {
    @NotBlank(message = "用户名不能为空")
    private String username;
    /** 新增必填；修改时为空表示不改密码 */
    private String password;
    @NotBlank(message = "姓名不能为空")
    private String realName;
    @NotBlank(message = "角色不能为空")
    private String role;        // NURSE / HEAD_NURSE / ADMIN
    private Integer deptId;
    private String phone;
    private Integer status;     // 1 启用 0 停用
}
