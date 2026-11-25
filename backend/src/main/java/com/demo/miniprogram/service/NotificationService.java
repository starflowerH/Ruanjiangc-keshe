package com.demo.miniprogram.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.miniprogram.entity.Notification;
import com.demo.miniprogram.mapper.NotificationMapper;
import com.demo.miniprogram.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 通知服务
 *
 * 通知写入采用异步方式（@Async），避免阻塞主业务流程的HTTP响应。
 * 异步方法通过 Spring AOP 代理生效，外部调用者（QAService、ForumCommentService）
 * 调用的快捷创建方法会自动在线程池中执行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    /**
     * 分页查询用户的通知列表
     */
    public IPage<NotificationVO> listNotifications(Integer userId, String userType, int page, int size) {
        Page<NotificationVO> pageParam = new Page<>(page, size);
        return notificationMapper.selectNotificationPage(pageParam, userId, userType);
    }

    /** 获取帖子评论与评论回复互动。 */
    public IPage<NotificationVO> listInteractionNotifications(Integer userId, String userType, int page, int size) {
        Page<NotificationVO> pageParam = new Page<>(page, size);
        return notificationMapper.selectInteractionNotificationPage(pageParam, userId, userType);
    }

    /**
     * 统计未读通知数量
     */
    public int countUnread(Integer userId, String userType) {
        return notificationMapper.countUnread(userId, userType);
    }

    /** 仅统计帖子互动未读数。 */
    public int countUnreadInteractions(Integer userId, String userType) {
        return notificationMapper.countUnreadInteractions(userId, userType);
    }

    /**
     * 创建通知
     */
    public void createNotification(Notification notification) {
        notification.setIsRead(0);
        notificationMapper.insert(notification);
    }

    /**
     * 标记单条通知为已读（校验归属：只能标记自己的通知）
     */
    public void markAsRead(Integer id, Integer userId, String userType) {
        // 归属校验：user_id + user_type 必须匹配，防止越权标记他人通知
        Notification notification = notificationMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Notification>()
                        .eq(Notification::getId, id)
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getUserType, userType)
        );
        if (notification == null) {
            throw new com.demo.miniprogram.common.BusinessException(
                    com.demo.miniprogram.common.ResultCode.NOT_FOUND, "通知不存在");
        }
        notificationMapper.markAsRead(id);
    }

    /**
     * 标记用户所有通知为已读
     */
    public void markAllAsRead(Integer userId, String userType) {
        notificationMapper.markAllAsRead(userId, userType);
    }

    /** 仅将帖子互动通知标为已读。 */
    public void markAllInteractionsAsRead(Integer userId, String userType) {
        notificationMapper.markAllInteractionsAsRead(userId, userType);
    }

    /**
     * 快速创建回复通知（异步）
     */
    @Async
    public void createReplyNotification(Integer userId, String userType, String questionTitle, Integer questionId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setUserType(userType);
        notification.setType("reply");
        notification.setTitle("收到新回复");
        notification.setContent("你的提问\"" + questionTitle + "\"收到了新的回答");
        notification.setSourceId(questionId);
        notification.setSourceType("question");
        notification.setSourceTitle(questionTitle);
        createNotification(notification);
    }

    /**
     * 快速创建提问通知（异步）——高中生向指定学长学姐提问时，通知被提问的志愿者
     */
    @Async
    public void createAskNotification(Integer volunteerId, String questionTitle, Integer questionId) {
        Notification notification = new Notification();
        notification.setUserId(volunteerId);
        notification.setUserType("volunteer");
        notification.setType("reply");
        notification.setTitle("收到新的提问");
        notification.setContent("高中生向您提出了问题\"" + questionTitle + "\"，请前往回答");
        notification.setSourceId(questionId);
        notification.setSourceType("question");
        notification.setSourceTitle(questionTitle);
        createNotification(notification);
    }

    /**
     * 快速创建系统通知（异步）
     */
    @Async
    public void createSystemNotification(Integer userId, String userType, String title, String content,
                                          Integer sourceId, String sourceType, String sourceTitle) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setUserType(userType);
        notification.setType("system");
        notification.setTitle(title);
        notification.setContent(content);
        notification.setSourceId(sourceId);
        notification.setSourceType(sourceType);
        notification.setSourceTitle(sourceTitle);
        createNotification(notification);
    }

    /**
     * 快速创建评论通知（异步）
     */
    @Async
    public void createCommentNotification(Integer userId, String userType, String postTitle, Integer postId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setUserType(userType);
        notification.setType("comment");
        notification.setTitle("收到新评论");
        notification.setContent("你的帖子\"" + postTitle + "\"收到了新的评论");
        notification.setSourceId(postId);
        notification.setSourceType("post");
        notification.setSourceTitle(postTitle);
        createNotification(notification);
    }

    /**
     * 快速创建回复评论通知（异步）
     */
    @Async
    public void createReplyCommentNotification(Integer userId, String userType, String postTitle, Integer postId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setUserType(userType);
        notification.setType("reply");
        notification.setTitle("收到新回复");
        notification.setContent("你在帖子\"" + postTitle + "\"中的评论收到了新的回复");
        notification.setSourceId(postId);
        notification.setSourceType("post");
        notification.setSourceTitle(postTitle);
        createNotification(notification);
    }
}
