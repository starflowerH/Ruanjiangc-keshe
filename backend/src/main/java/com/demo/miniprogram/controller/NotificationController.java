package com.demo.miniprogram.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.demo.miniprogram.common.Result;
import com.demo.miniprogram.dto.PageQueryDTO;
import com.demo.miniprogram.service.NotificationService;
import com.demo.miniprogram.vo.NotificationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 通知接口 - 消息通知、未读提醒
 */
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 获取通知列表（分页）
     *
     * GET /api/notification/list?page=1&size=20
     */
    @GetMapping("/list")
    public Result<IPage<NotificationVO>> getNotifications(
            @RequestAttribute(value = "relatedId", required = false) Integer userId,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @RequestAttribute(value = "userType", required = false) String userType,
            @Valid PageQueryDTO queryDTO) {
        // 新用户（未绑定身份）relatedId 为 null，返回友好提示而非 500
        if (userId == null) {
            return Result.fail(403, "请先完成身份认证后再查看通知");
        }
        // 通知表按实际角色类型（volunteer/student/admin）存储，JWT 中的 userType 是 wechat，需映射
        String lookupType = relatedType != null ? relatedType : userType;
        return Result.ok(notificationService.listNotifications(userId, lookupType,
                queryDTO.getPage(), queryDTO.getSize()));
    }

    /** 获取帖子评论和评论回复的互动消息。 */
    @GetMapping("/interactions")
    public Result<IPage<NotificationVO>> getInteractionNotifications(
            @RequestAttribute(value = "relatedId", required = false) Integer userId,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @RequestAttribute(value = "userType", required = false) String userType,
            @Valid PageQueryDTO queryDTO) {
        if (userId == null) return Result.fail(403, "请先完成身份认证后再查看消息");
        String lookupType = relatedType != null ? relatedType : userType;
        return Result.ok(notificationService.listInteractionNotifications(userId, lookupType,
                queryDTO.getPage(), queryDTO.getSize()));
    }

    /**
     * 获取未读通知数量
     *
     * GET /api/notification/unread-count
     */
    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount(
            @RequestAttribute(value = "relatedId", required = false) Integer userId,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @RequestAttribute(value = "userType", required = false) String userType) {
        if (userId == null) {
            return Result.ok(0);
        }
        String lookupType = relatedType != null ? relatedType : userType;
        return Result.ok(notificationService.countUnread(userId, lookupType));
    }

    /** 获取帖子互动消息未读数。 */
    @GetMapping("/interaction-unread-count")
    public Result<Integer> getInteractionUnreadCount(
            @RequestAttribute(value = "relatedId", required = false) Integer userId,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @RequestAttribute(value = "userType", required = false) String userType) {
        if (userId == null) return Result.ok(0);
        String lookupType = relatedType != null ? relatedType : userType;
        return Result.ok(notificationService.countUnreadInteractions(userId, lookupType));
    }

    /**
     * 标记单条通知为已读
     *
     * POST /api/notification/mark-read
     * Body: {"id": 1}（前端 notification.js 以 body 传 id，与 @RequestParam 契约对齐）
     */
    @PostMapping("/mark-read")
    public Result<Void> markAsRead(@RequestBody Map<String, Object> body,
                                   @RequestAttribute(value = "relatedId", required = false) Integer userId,
                                   @RequestAttribute(value = "relatedType", required = false) String relatedType,
                                   @RequestAttribute(value = "userType", required = false) String userType) {
        if (userId != null && body != null && body.get("id") != null) {
            Integer id = ((Number) body.get("id")).intValue();
            String lookupType = relatedType != null ? relatedType : userType;
            notificationService.markAsRead(id, userId, lookupType);
        }
        return Result.ok();
    }

    /**
     * 标记所有通知为已读
     *
     * POST /api/notification/mark-all-read
     */
    @PostMapping("/mark-all-read")
    public Result<Void> markAllAsRead(
            @RequestAttribute(value = "relatedId", required = false) Integer userId,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @RequestAttribute(value = "userType", required = false) String userType) {
        if (userId != null) {
            String lookupType = relatedType != null ? relatedType : userType;
            notificationService.markAllAsRead(userId, lookupType);
        }
        return Result.ok();
    }

    /** 将帖子互动消息全部标记为已读。 */
    @PostMapping("/mark-interactions-read")
    public Result<Void> markAllInteractionsAsRead(
            @RequestAttribute(value = "relatedId", required = false) Integer userId,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @RequestAttribute(value = "userType", required = false) String userType) {
        if (userId != null) {
            String lookupType = relatedType != null ? relatedType : userType;
            notificationService.markAllInteractionsAsRead(userId, lookupType);
        }
        return Result.ok();
    }
}
