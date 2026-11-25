package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 队伍成员手动调整覆盖表 - 老师自定义组队
 *
 * 成员判定规则：组合派生成员 + IN 记录 - OUT 记录
 *   IN  - 强制加入该队伍（老师手动指定）
 *   OUT - 强制移出该队伍（老师手动移出，不可撤销）
 *
 * @author demo
 */
@Data
@TableName("team_override")
public class TeamOverride {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 队伍ID */
    private Integer promoTeamId;

    /** 志愿者ID */
    private Integer volunteerId;

    /** 操作: IN=强制入队 / OUT=强制移出 */
    private String action;

    /** 操作老师/管理员ID */
    private Integer createdBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
