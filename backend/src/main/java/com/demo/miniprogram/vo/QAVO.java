package com.demo.miniprogram.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 问答详情视图 - 显示问答内容及相关人员信息
 *
 * @author demo
 */
@Data
public class QAVO {

    /** 问答记录ID */
    private Integer id;

    /** 帖子标题（论坛模式） */
    private String title;

    /** 帖子分类 */
    private String category;

    private String authorType;
    private Integer universityId;
    private String universityName;
    private Integer isFeatured;
    private Integer heatScore;

    /** 提问者姓名 */
    private String askerName;

    /** 提问者头像 */
    private String askerAvatar;

    /** 提问的高中学校名称 */
    private String askerHighSchool;

    /** 回答的大学生姓名 */
    private String answererName;

    /** 回答的大学生头像 */
    private String answererAvatar;

    /** 回答的大学生所属大学 */
    private String answererUniversity;

    /** 提问内容 */
    private String questionContent;

    /** 回答内容（未回答时为 null） */
    private String answerContent;

    /** 提问时间 */
    private LocalDateTime askedAt;

    /** 动态时间：发布帖子取发布时间，收藏帖子取收藏时间。 */
    private LocalDateTime activityAt;

    /** 回答时间 */
    private LocalDateTime answeredAt;

    /** 审核状态 */
    private String auditStatus;

    /** 是否公开 */
    private Integer isPublic;

    /** 浏览量 */
    private Integer viewCount;

    /** 评论数 */
    private Integer commentCount;

    /** 帖子点赞数 */
    private Integer likeCount;

    /** 收藏数 */
    private Integer favoriteCount;

    /** 当前用户是否已点赞帖子 */
    private Boolean liked;

    /** 当前用户是否已收藏 */
    private Boolean favorited;

    /** 回答点赞数 */
    private Integer answerLikeCount;

    /** 当前用户是否已点赞回答 */
    private Boolean answerLiked;
}
