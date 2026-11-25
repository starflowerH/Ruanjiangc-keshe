package com.demo.miniprogram.controller;

import com.demo.miniprogram.common.Result;
import com.demo.miniprogram.dto.CommentDTO;
import com.demo.miniprogram.entity.ForumComment;
import com.demo.miniprogram.service.ForumCommentService;
import com.demo.miniprogram.vo.CommentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 论坛评论接口
 */
@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class ForumCommentController {

    private final ForumCommentService commentService;

    /**
     * 添加评论
     *
     * POST /api/comment/add
     */
    @PostMapping("/add")
    public Result<ForumComment> addComment(
            @RequestAttribute(value = "relatedId", required = false) Integer userId,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @Valid @RequestBody CommentDTO dto) {
        // relatedType 为细分身份(student/volunteer)，userType 固定为 wechat 无法区分评论者身份
        if (userId == null || !("student".equals(relatedType) || "volunteer".equals(relatedType))) {
            return Result.fail(403, "请先完成身份认证后再评论");
        }
        ForumComment comment = commentService.addComment(userId, relatedType, dto);
        return Result.ok("评论成功", comment);
    }

    /**
     * 获取帖子的评论列表
     *
     * GET /api/comment/list?postId=1
     */
    @GetMapping("/list")
    public Result<List<CommentVO>> getComments(
            @RequestParam Integer postId,
            @RequestAttribute(value = "relatedId", required = false) Integer userId,
            @RequestAttribute(value = "relatedType", required = false) String relatedType) {
        return Result.ok(commentService.getCommentsByPostId(postId, userId, relatedType));
    }

    /**
     * 获取我的评论列表（当前用户发表过的评论）
     *
     * GET /api/comment/my-comments?page=1&size=10
     * 需要登录且已绑定学生/志愿者身份
     */
    @GetMapping("/my-comments")
    public Result<Map<String, Object>> getMyComments(
            @RequestAttribute(value = "relatedId", required = false) Integer userId,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        // 与 addComment 相同：仅学生/志愿者可查看自己的评论
        if (userId == null || !("student".equals(relatedType) || "volunteer".equals(relatedType))) {
            return Result.fail(403, "请先完成身份认证后再查看评论");
        }
        return Result.ok(commentService.getMyComments(userId, relatedType, page, size));
    }

    /**
     * 评论点赞切换
     * POST /api/comment/{id}/like
     */
    @PostMapping("/{id}/like")
    public Result<Map<String, Object>> toggleLike(
            @RequestAttribute(value = "relatedId", required = false) Integer userId,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @PathVariable Integer id) {
        if (userId == null || !("student".equals(relatedType) || "volunteer".equals(relatedType))) {
            return Result.fail(403, "请先完成身份认证后再点赞");
        }
        return Result.ok(commentService.toggleLike(id, userId, relatedType));
    }
}
