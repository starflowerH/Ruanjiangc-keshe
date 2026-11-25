package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("forum_comment_like")
public class ForumCommentLike {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer commentId;
    private Integer userId;
    private String userType;
}
