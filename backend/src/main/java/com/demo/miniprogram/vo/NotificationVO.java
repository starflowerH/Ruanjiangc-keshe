package com.demo.miniprogram.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知视图对象
 */
@Data
public class NotificationVO {

    /** 通知ID */
    private Integer id;

    /** 接收通知的用户ID */
    private Integer userId;

    /** 用户类型：student/volunteer/admin */
    private String userType;

    /** 通知类型：reply/comment/system/like */
    private String type;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 关联的资源ID */
    private Integer sourceId;

    /** 资源类型：question/answer/post */
    private String sourceType;

    /** 资源标题 */
    private String sourceTitle;

    /** 是否已读：0=未读，1=已读 */
    private Integer isRead;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
