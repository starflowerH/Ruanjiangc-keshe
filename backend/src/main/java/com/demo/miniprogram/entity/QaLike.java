package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 点赞表 - 帖子和回答的点赞记录（支持取消）
 *
 * target_type:
 *   post   - 点赞帖子
 *   answer - 点赞回答（每个帖子最多一条官方回答，按 qa_id 计数）
 */
@Data
@TableName("qa_like")
public class QaLike {

    /** 点赞ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 问答/帖子ID */
    private Integer qaId;

    /** 用户ID（高中生或志愿者） */
    private Integer userId;

    /** 用户类型：student/volunteer */
    private String userType;

    /** 点赞对象：post(帖子)/answer(回答) */
    private String targetType;

    /** 点赞时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
