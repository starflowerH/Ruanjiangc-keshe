package com.demo.miniprogram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 评论请求 DTO
 */
@Data
public class CommentDTO {

    /** 帖子ID */
    @NotNull(message = "帖子ID不能为空")
    private Integer postId;

    /** 评论内容 */
    @NotBlank(message = "评论内容不能为空")
    private String content;

    /** 父评论ID（回复某条评论时使用） */
    private Integer parentId;
}
