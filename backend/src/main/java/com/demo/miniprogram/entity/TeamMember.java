package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 队伍成员关系表 - 记录志愿者所属队伍
 *
 * 一人只能在一个队伍中（volunteer_id 唯一约束）
 *
 * @author demo
 */
@Data
@TableName("team_member")
public class TeamMember {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 队伍ID */
    private Integer teamId;

    /** 志愿者ID */
    private Integer volunteerId;

    /**
     * 角色
     * MEMBER       - 普通成员
     * CAPTAIN      - 队长
     */
    private String role;

    /** 加入时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime joinedAt;
}
