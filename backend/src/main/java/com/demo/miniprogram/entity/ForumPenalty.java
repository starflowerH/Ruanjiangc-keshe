package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("forum_penalty")
public class ForumPenalty {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String userType;
    private Integer reportId;
    private Integer offenseNumber;
    private LocalDateTime mutedFrom;
    private LocalDateTime mutedUntil;
    private String status;
    private LocalDateTime revokedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
