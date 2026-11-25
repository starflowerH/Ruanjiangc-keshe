package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 论坛评论表 - 存储论坛帖子的评论
 */
@Data
@TableName("forum_comment")
public class ForumComment {

    /** 评论ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 帖子ID（关联 Youtube 表） */
    private Integer postId;

    /** 评论者ID */
    private Integer userId;

    /** 评论者类型：student/volunteer */
    private String userType;

    /** 评论内容 */
    private String content;

    /** 父评论ID（回复某条评论时使用） */
    private Integer parentId;

    /** 评论状态：VISIBLE(正常) / HIDDEN(违规隐藏) */
    private String status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
