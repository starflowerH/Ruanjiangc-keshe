package com.demo.miniprogram.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 - 返回JWT Token和用户基本信息
 *
 * @author demo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    /** JWT Token，前端存储后每次请求携带在Header中 */
    private String token;

    /** Token 过期时间（秒），默认7天 */
    private Long expiresIn;

    /** 用户OpenID */
    private String openid;

    /** 用户昵称 */
    private String nickname;

    /** 用户头像 */
    private String avatarUrl;

    /**
     * 用户类型（用于前端跳转不同页面）
     * "volunteer"        - 大学生志愿者
     * "high_school_student" - 高中生
     * "admin"            - 后台管理员/老师
     */
    private String userType;

    /** 角色（仅管理员，如 TEACHER、SUPER_ADMIN） */
    private String role;

    /** 关联ID（志愿者ID/高中生ID/管理员ID），用于后续接口调用 */
    private Integer relatedId;

    /**
     * 手机号 - 通过微信getPhoneNumber一键获取
     * 纯静默登录时此字段为null，前端可引导用户点击按钮绑定
     */
    private String phone;

    /** 是否为队长（仅志愿者角色有效） */
    private Boolean isCaptain;

    /** 权限配置JSON（仅管理员角色有效） */
    private String permissions;

    /** 所属高校ID（仅老师角色有效） */
    private Integer universityId;

    /** 所属高校名称（仅老师角色有效） */
    private String universityName;

    /** 高中生/志愿者的真实姓名 */
    private String name;

    /** 高中生所在高中 */
    private Integer highSchoolId;
    private String highSchoolName;

    /** 志愿者身份资料 */
    private String wechatId;
    private String grade;
    private String major;
    private String status;
    private Integer targetHighSchoolId;
    private String targetHighSchoolName;
}
