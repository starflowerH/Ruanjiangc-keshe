package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("forum_moderator_grant")
public class ForumModeratorGrant {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer volunteerId;
    private Integer universityId;
    private Integer grantedByAdminId;
    private Integer active;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
