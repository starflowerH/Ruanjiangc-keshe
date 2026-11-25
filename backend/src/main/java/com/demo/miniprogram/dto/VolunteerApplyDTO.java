package com.demo.miniprogram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

/**
 * 志愿者报名请求参数
 *
 * 对应业务文档阶段1-报名期：
 *   大学生选择所属大学 → 填写表单 → 选择目标高中 → 提交报名
 *
 * 自定义字段通过 customFields 动态传递：
 *   key = field_definition_id (字段定义ID)
 *   value = 用户填写的值
 *   例如：{"3": "是", "4": "北京"} 表示字段3填了"是"，字段4填了"北京"
 *
 * @author demo
 */
@Data
public class VolunteerApplyDTO {

    /** 真实姓名 */
    @NotBlank(message = "姓名不能为空")
    private String name;

    /** 手机号码（与邮箱至少填写一项） */
    private String phone;

    /** 微信号（历史兼容字段） */
    private String wechatId;

    /** 学号 */
    @NotBlank(message = "学号不能为空")
    private String studentNo;

    /** 性别 */
    @NotBlank(message = "性别不能为空")
    private String gender;

    /** 邮箱（与电话至少填写一项） */
    private String email;

    /** 所属大学ID（选择要宣传的大学） */
    @NotNull(message = "所属大学不能为空")
    private Integer universityId;

    /** 年级（如：24级） */
    @NotBlank(message = "年级不能为空")
    private String grade;

    /** 专业 */
    @NotBlank(message = "专业不能为空")
    private String major;

    /** 目标高中ID（从高中名册选择：我要去哪个高中宣传，选填） */
    private Integer targetHighSchoolId;

    /** 是否申请队长：0=否，1=是 */
    private Integer isCaptainApplicant = 0;

    /**
     * 自定义字段值
     * key   → 自定义字段定义ID (custom_field_definition.id)
     * value → 用户填写的值
     */
    private Map<Integer, String> customFields;
}
