package com.demo.miniprogram.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论视图对象
 */
@Data
public class CommentVO {

    /** 评论ID */
    private Integer id;

    /** 帖子ID */
    private Integer postId;

    /** 评论者ID */
    private Integer userId;

    /** 评论者类型：student/volunteer */
    private String userType;

    /** 评论者姓名 */
    private String userName;

    /** 评论内容 */
    private String content;

    /** 父评论ID */
    private Integer parentId;

    /** 父评论者姓名（回复某条评论时显示） */
    private String parentUserName;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 子评论列表 */
    private List<CommentVO> children;

    /** 点赞数 */
    private Long likeCount;

    /** 当前用户是否已点赞 */
    private Boolean liked;
}
