package com.demo.miniprogram.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 后台管理登录请求参数
 *
 * 管理员/老师使用账号密码登录后台管理系统
 *
 * @author demo
 */
@Data
public class AdminLoginDTO {

    /** 登录账号 */
    @NotBlank(message = "账号不能为空")
    private String username;

    /** 登录密码（明文传输，后端BCrypt比对） */
    @NotBlank(message = "密码不能为空")
    private String password;
}
