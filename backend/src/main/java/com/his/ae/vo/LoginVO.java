package com.his.ae.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 登录入参 */
@Data
public class LoginVO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
