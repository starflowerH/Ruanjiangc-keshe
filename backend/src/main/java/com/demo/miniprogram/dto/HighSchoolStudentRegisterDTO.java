package com.demo.miniprogram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 高中生注册请求参数
 *
 * 对应业务文档阶段4-宣传执行期：
 *   高中生扫码关注公众号 → 注册 → 填写手机号 + 选择所在高中
 *   → 系统自动匹配展示来过本校宣传的学长学姐
 *
 * @author demo
 */
@Data
public class HighSchoolStudentRegisterDTO {

    /** 高中生身份昵称 */
    private String nickname;

    /** 真实姓名 */
    @NotBlank(message = "姓名不能为空")
    private String name;

    /** 微信头像URL（可选） */
    private String avatarUrl;

    /** 手机号码 */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /** 所在高中ID（从高中名册中选择） */
    @NotNull(message = "所在高中不能为空")
    private Integer highSchoolId;
}
