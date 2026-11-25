package com.demo.miniprogram.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.demo.miniprogram.common.Result;
import com.demo.miniprogram.dto.ForumAppealDTO;
import com.demo.miniprogram.dto.ForumAppealReviewDTO;
import com.demo.miniprogram.dto.ForumFeaturedDTO;
import com.demo.miniprogram.dto.ForumReportDTO;
import com.demo.miniprogram.dto.ForumReportReviewDTO;
import com.demo.miniprogram.dto.ForumReviewActionDTO;
import com.demo.miniprogram.entity.*;
import com.demo.miniprogram.security.TeacherPermissions;
import com.demo.miniprogram.service.ForumModerationService;
import com.demo.miniprogram.service.TeacherPermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import com.demo.miniprogram.vo.QAVO;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
public class ForumModerationController {
    private final ForumModerationService moderationService;
    private final TeacherPermissionService teacherPermissionService;

    @PostMapping("/reports")
    public Result<ForumReport> report(@RequestAttribute(value = "relatedId", required = false) Integer userId,
                                      @RequestAttribute(value = "relatedType", required = false) String userType,
                                      @Valid @RequestBody ForumReportDTO dto) {
        if (userId == null || !("student".equals(userType) || "volunteer".equals(userType))) {
            return Result.fail(403, "请先完成身份认证后再举报");
        }
        return Result.ok("举报已提交", moderationService.submitReport(userId, userType, dto));
    }

    @GetMapping("/penalties/mine")
    public Result<List<ForumPenalty>> myPenalties(
            @RequestAttribute(value = "relatedId", required = false) Integer userId,
            @RequestAttribute(value = "relatedType", required = false) String userType) {
        if (userId == null || !("student".equals(userType) || "volunteer".equals(userType))) {
            return Result.fail(403, "请先完成身份认证后再查看");
        }
        return Result.ok(moderationService.myPenalties(userId, userType));
    }

    @PostMapping("/appeals")
    public Result<ForumAppeal> appeal(@RequestAttribute(value = "relatedId", required = false) Integer userId,
                                      @RequestAttribute(value = "relatedType", required = false) String userType,
                                      @Valid @RequestBody ForumAppealDTO dto) {
        if (userId == null || !("student".equals(userType) || "volunteer".equals(userType))) {
            return Result.fail(403, "请先完成身份认证后再申诉");
        }
        return Result.ok("申诉已提交", moderationService.appeal(userId, userType, dto));
    }

    @GetMapping("/moderation/status")
    public Result<Map<String, Object>> moderatorStatus(
            @RequestAttribute(value = "requesterId", required = false) Integer reviewerId,
            @RequestAttribute(value = "userType", required = false) String userType,
            @RequestAttribute(value = "requesterRole", required = false) String role,
            @RequestAttribute(value = "relatedType", required = false) String relatedType) {
        if ("admin".equals(userType) && "TEACHER".equals(role)) {
            return Result.ok(Map.of("moderator",
                    teacherPermissionService.hasAny(reviewerId, TeacherPermissions.FORUM_MODERATION)));
        }
        ForumModeratorGrant grant = "volunteer".equals(relatedType) ? moderationService.getGrant(reviewerId) : null;
        return Result.ok(Map.of("moderator", grant != null,
                "universityId", grant != null ? grant.getUniversityId() : 0));
    }

    @GetMapping("/moderation/posts")
    public Result<IPage<QuestionAnswer>> pendingPosts(
            @RequestAttribute("requesterId") Integer reviewerId,
            @RequestAttribute("userType") String userType,
            @RequestAttribute(value = "requesterRole", required = false) String role,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(moderationService.pendingPosts(page, size, reviewerId, reviewerType(userType, relatedType), role));
    }

    @PostMapping("/moderation/posts/{id}/review")
    public Result<String> reviewPost(@PathVariable Integer id,
                                     @RequestAttribute("requesterId") Integer reviewerId,
                                     @RequestAttribute("userType") String userType,
                                     @RequestAttribute(value = "requesterRole", required = false) String role,
                                     @RequestAttribute(value = "relatedType", required = false) String relatedType,
                                     @Valid @RequestBody ForumReviewActionDTO body) {
        moderationService.reviewPost(id, body.getAction(), reviewerId, reviewerType(userType, relatedType), role);
        return Result.ok("审核完成");
    }

    @PostMapping("/moderation/posts/{id}/featured")
    public Result<String> featured(@PathVariable Integer id,
                                   @RequestAttribute("requesterId") Integer reviewerId,
                                   @RequestAttribute("userType") String userType,
                                   @RequestAttribute(value = "requesterRole", required = false) String role,
                                   @RequestAttribute(value = "relatedType", required = false) String relatedType,
                                   @Valid @RequestBody ForumFeaturedDTO body) {
        moderationService.setFeatured(id, body.getFeatured(), reviewerId, reviewerType(userType, relatedType), role);
        return Result.ok("精华状态已更新");
    }

    @GetMapping("/moderation/featured-candidates")
    public Result<IPage<QAVO>> featuredCandidates(
            @RequestAttribute("requesterId") Integer reviewerId,
            @RequestAttribute("userType") String userType,
            @RequestAttribute(value = "requesterRole", required = false) String role,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(moderationService.featuredCandidates(page, size, reviewerId, reviewerType(userType, relatedType), role));
    }

    @GetMapping("/moderation/reports")
    public Result<IPage<ForumReport>> pendingReports(
            @RequestAttribute("requesterId") Integer reviewerId,
            @RequestAttribute("userType") String userType,
            @RequestAttribute(value = "requesterRole", required = false) String role,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(moderationService.pendingReports(page, size, reviewerId, reviewerType(userType, relatedType), role));
    }

    @PostMapping("/moderation/reports/{id}/review")
    public Result<String> reviewReport(@PathVariable Integer id,
                                       @RequestAttribute("requesterId") Integer reviewerId,
                                       @RequestAttribute("userType") String userType,
                                       @RequestAttribute(value = "requesterRole", required = false) String role,
                                       @RequestAttribute(value = "relatedType", required = false) String relatedType,
                                       @Valid @RequestBody ForumReportReviewDTO body) {
        moderationService.reviewReport(id, body.getFounded(),
                body.getNote(), reviewerId, reviewerType(userType, relatedType), role);
        return Result.ok("举报处理完成");
    }

    @GetMapping("/moderation/appeals")
    public Result<IPage<ForumAppeal>> pendingAppeals(
            @RequestAttribute("requesterId") Integer reviewerId,
            @RequestAttribute("userType") String userType,
            @RequestAttribute(value = "requesterRole", required = false) String role,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(moderationService.pendingAppeals(page, size, reviewerId, reviewerType(userType, relatedType), role));
    }

    @PostMapping("/moderation/appeals/{id}/review")
    public Result<String> reviewAppeal(@PathVariable Integer id,
                                       @RequestAttribute("requesterId") Integer reviewerId,
                                       @RequestAttribute("userType") String userType,
                                       @RequestAttribute(value = "requesterRole", required = false) String role,
                                       @RequestAttribute(value = "relatedType", required = false) String relatedType,
                                       @Valid @RequestBody ForumAppealReviewDTO body) {
        moderationService.reviewAppeal(id, body.getApproved(),
                Boolean.TRUE.equals(body.getRestorePost()),
                body.getNote(), reviewerId, reviewerType(userType, relatedType), role);
        return Result.ok("申诉复核完成");
    }

    private String reviewerType(String userType, String relatedType) {
        return "admin".equals(userType) ? "admin" : relatedType;
    }
}
