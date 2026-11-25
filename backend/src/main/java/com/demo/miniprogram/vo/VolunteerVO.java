package com.demo.miniprogram.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 志愿者信息视图 - 包含关联的大学名称、高中名称等冗余信息
 * 避免前端多次请求做关联查询
 *
 * @author demo
 */
@Data
public class VolunteerVO {

    /** 报名记录ID */
    private Integer id;

    /** 姓名 */
    private String name;

    /** 手机号 */
    private String phone;

    /** 微信号 */
    private String wechatId;

    /** 学号（仅本人和审核人员可见） */
    private String studentNo;

    /** 性别（仅本人和审核人员可见） */
    private String gender;

    /** 邮箱（仅本人和审核人员可见） */
    private String email;

    /** 大学名称 */
    private String universityName;

    /** 年级 */
    private String grade;

    /** 专业 */
    private String major;

    /** 目标高中名称 */
    private String highSchoolName;

    /** 目标高中省份 */
    private String highSchoolProvince;

    /** 目标高中城市 */
    private String highSchoolCity;

    /** 是否申请队长 */
    private Integer isCaptainApplicant;

    /** 审核状态：PENDING/APPROVED/REJECTED/CANCELLED/REPLACED */
    private String status;

    /** 取消时间（老师取消时记录，用于限时撤销） */
    private LocalDateTime cancelledAt;

    /** 取消理由（选填） */
    private String cancelReason;

    /** 是否可撤销取消（CANCELLED 且未超过时限，由后端计算） */
    private Boolean canUndo;

    /** 是否已授予论坛审核员权限（老师授权管理界面用） */
    private Boolean forumModeratorGranted;

    /** 是否已授予宣传资料管理权限（老师授权管理界面用） */
    private Boolean promotionManagerGranted;

    private LocalDateTime createdAt;
}
