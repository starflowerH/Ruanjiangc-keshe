package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("forum_report")
public class ForumReport {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer postId;
    /** 举报对象：post(帖子) / comment(评论) */
    private String targetType;
    /** 被举报评论ID（targetType=comment 时有效，post 举报固定为 0） */
    private Integer commentId;
    private Integer reporterId;
    private String reporterType;
    private String reason;
    private String detail;
    private String status;
    private Integer reviewerId;
    private String reviewerType;
    private String decisionNote;
    private Integer penaltyId;
    /** Fields copied from the reported post for moderation display. */
    @TableField(exist = false)
    private String postTitle;
    @TableField(exist = false)
    private String postContent;
    /** Fields copied from the reported comment for moderation display. */
    @TableField(exist = false)
    private String commentContent;
    @TableField(exist = false)
    private String commentAuthorName;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
