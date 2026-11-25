package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 收藏表 - 用户收藏的帖子（支持取消）
 */
@Data
@TableName("qa_favorite")
public class QaFavorite {

    /** 收藏ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 问答/帖子ID */
    private Integer qaId;

    /** 用户ID（高中生或志愿者） */
    private Integer userId;

    /** 用户类型：student/volunteer */
    private String userType;

    /** 收藏时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
