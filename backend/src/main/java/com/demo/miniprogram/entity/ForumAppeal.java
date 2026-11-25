package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("forum_appeal")
public class ForumAppeal {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer penaltyId;
    private Integer appellantId;
    private String appellantType;
    private String reason;
    private String status;
    private Integer reviewerId;
    private String reviewerType;
    private String decisionNote;
    private Integer restorePost;
    /** 对应处罚的举报目标类型（post/comment），用于申诉页恢复提示 */
    @TableField(exist = false)
    private String targetType;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
