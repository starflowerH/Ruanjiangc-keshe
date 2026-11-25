package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 志愿者报名表 - 大学生报名参与招生宣传的核心业务表
 *
 * 业务流程（对应业务文档阶段1-报名期）：
 *   1. 大学生通过公众号H5选择所属大学
 *   2. 展示该大学的定制表单（含自定义字段）
 *   3. 填写姓名、手机号、微信号、年级、专业
 *   4. 从省内高中名册中选择目标高中（我去哪个高中宣传）
 *   5. 选择是否申请队长
 *   6. 提交 → 状态为 PENDING（待审核）
 *
 * 业务流转：
 *   PENDING → (老师审核通过) → APPROVED → (系统自动组队) → 关联到 promo_team
 *   PENDING → (老师拒绝)     → REJECTED
 *
 * @author demo
 */
@Data
@TableName("volunteer")
public class Volunteer {

    /** 报名记录ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 微信OpenID，关联微信用户表，标识是哪个大学生 */
    private String openid;

    /** 大学生身份显示名称 */
    private String name;

    /** 手机号码 */
    private String phone;

    /** 微信号（非微信昵称，是微信账号ID） */
    private String wechatId;

    /** 学号 */
    private String studentNo;

    /** 性别 */
    private String gender;

    /** 邮箱 */
    private String email;

    /** 所属大学ID，关联 university 表 */
    private Integer universityId;

    /** 年级（如：24级、23级） */
    private String grade;

    /** 专业（如：计算机科学与技术） */
    private String major;

    /**
     * 目标高中ID，关联 high_school 表
     * 含义：这个大学生想去哪个高中宣传自己的大学
     * 这是后续自动组队的关键字段
     */
    private Integer targetHighSchoolId;

    /** 是否申请队长：0=否，1=是 */
    private Integer isCaptainApplicant;

    /**
     * 审核状态枚举
     * PENDING  - 待审核（报名后默认状态）
     * APPROVED - 已通过（老师确认后）
     * REJECTED - 已拒绝
     */
    private String status;

    /** 报名提交时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 最后状态变更时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 取消时间（老师取消时记录，用于限时撤销判断） */
    private LocalDateTime cancelledAt;

    /** 取消理由（选填） */
    private String cancelReason;
}
