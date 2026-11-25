package com.demo.miniprogram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 老师注册请求 DTO（微信授权登录，无需密令）
 */
@Data
public class TeacherRegisterDTO {

    /** 微信登录code */
    @NotBlank(message = "微信登录凭证不能为空")
    private String code;

    /** 真实姓名 */
    @NotBlank(message = "姓名不能为空")
    private String name;

    /** 联系电话 */
    private String phone;

    /** 所属大学ID */
    @NotNull(message = "所属大学不能为空")
    private Integer universityId;

    /** 目标高中ID（可选） */
    private Integer targetHighSchoolId;
}
