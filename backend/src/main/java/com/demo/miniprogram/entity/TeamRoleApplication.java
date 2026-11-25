package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 队长申请表 - 队伍成员申请成为队长或转让队长
 *
 * 流程：
 *   1. 队伍成员提交申请（选择角色 + 填写理由）
 *   2. 管理员审核（通过/拒绝）
 *   3. 通过后自动更新队伍的队长
 *
 * @author demo
 */
@Data
@TableName("team_role_application")
public class TeamRoleApplication {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 队伍ID */
    private Integer teamId;

    /** 申请人ID（志愿者ID） */
    private Integer volunteerId;

    /**
     * 申请角色
     * CAPTAIN      - 申请成为队长
     * CAPTAIN_TRANSFER - 队长转让
     */
    private String applyRole;

    /** 申请理由 */
    private String reason;

    /**
     * 审核状态
     * PENDING  - 待审核（默认）
     * APPROVED - 已通过
     * REJECTED - 已拒绝
     */
    private String status;

    /** 审核人ID（管理员ID） */
    private Integer reviewedBy;

    /** 审核时间 */
    private LocalDateTime reviewedAt;

    /** 申请提交时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
