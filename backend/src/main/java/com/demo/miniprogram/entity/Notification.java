package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知表 - 存储系统通知、回复通知、评论通知等
 *
 * @author demo
 */
@Data
@TableName("notification")
public class Notification {

    /** 通知ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 接收通知的用户ID */
    private Integer userId;

    /** 用户类型：student/volunteer/admin */
    private String userType;

    /**
     * 通知类型
     * reply   - 收到回复（有人回答了你的问题）
     * comment - 收到评论（有人评论了你的帖子）
     * system  - 系统通知（审核通过、被采纳为精选等）
     * like    - 收到点赞
     */
    private String type;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 关联的资源ID（问题ID/帖子ID等） */
    private Integer sourceId;

    /** 资源类型：question/answer/post */
    private String sourceType;

    /** 资源标题 */
    private String sourceTitle;

    /** 是否已读：0=未读，1=已读 */
    private Integer isRead;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
