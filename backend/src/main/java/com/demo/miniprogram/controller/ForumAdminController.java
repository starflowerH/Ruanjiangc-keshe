package com.demo.miniprogram.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.demo.miniprogram.common.Result;
import com.demo.miniprogram.dto.ForumAppealReviewDTO;
import com.demo.miniprogram.dto.ForumFeaturedDTO;
import com.demo.miniprogram.dto.ForumReportReviewDTO;
import com.demo.miniprogram.dto.ForumReviewActionDTO;
import com.demo.miniprogram.dto.ModeratorGrantDTO;
import com.demo.miniprogram.dto.SensitiveWordDTO;
import com.demo.miniprogram.entity.*;
import com.demo.miniprogram.mapper.ForumSensitiveWordMapper;
import com.demo.miniprogram.security.RequireRole;
import com.demo.miniprogram.service.ForumModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.demo.miniprogram.vo.QAVO;

@RestController
@RequestMapping("/api/admin/forum")
@RequiredArgsConstructor
public class ForumAdminController {
    private final ForumModerationService moderationService;
    private final ForumSensitiveWordMapper sensitiveWordMapper;

    @PostMapping("/moderators/{volunteerId}")
    @RequireRole("SUPER_ADMIN")
    public Result<ForumModeratorGrant> setModerator(@PathVariable Integer volunteerId,
                                                    @RequestAttribute("requesterId") Integer adminId,
                                                    @Valid @RequestBody ModeratorGrantDTO body) {
        return Result.ok(moderationService.setModerator(volunteerId, body.getActive(), adminId));
    }

    @GetMapping("/posts")
    @RequireRole("SUPER_ADMIN")
    public Result<IPage<QuestionAnswer>> pendingPosts(@RequestAttribute("requesterId") Integer adminId,
                                                      @RequestAttribute("requesterRole") String role,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return Result.ok(moderationService.pendingPosts(page, size, adminId, "admin", role));
    }

    @PostMapping("/posts/{id}/review")
    @RequireRole("SUPER_ADMIN")
    public Result<String> reviewPost(@PathVariable Integer id,
                                     @RequestAttribute("requesterId") Integer adminId,
                                     @RequestAttribute("requesterRole") String role,
                                     @Valid @RequestBody ForumReviewActionDTO body) {
        moderationService.reviewPost(id, body.getAction(), adminId, "admin", role);
        return Result.ok("审核完成");
    }

    @PostMapping("/posts/{id}/featured")
    @RequireRole("SUPER_ADMIN")
    public Result<String> featured(@PathVariable Integer id,
                                   @RequestAttribute("requesterId") Integer adminId,
                                   @RequestAttribute("requesterRole") String role,
                                   @Valid @RequestBody ForumFeaturedDTO body) {
        moderationService.setFeatured(id, body.getFeatured(), adminId, "admin", role);
        return Result.ok("精华状态已更新");
    }

    @GetMapping("/featured-candidates")
    @RequireRole("SUPER_ADMIN")
    public Result<IPage<QAVO>> featuredCandidates(@RequestAttribute("requesterId") Integer adminId,
                                                  @RequestAttribute("requesterRole") String role,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return Result.ok(moderationService.featuredCandidates(page, size, adminId, "admin", role));
    }

    @GetMapping("/reports")
    @RequireRole("SUPER_ADMIN")
    public Result<IPage<ForumReport>> reports(@RequestAttribute("requesterId") Integer adminId,
                                              @RequestAttribute("requesterRole") String role,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return Result.ok(moderationService.pendingReports(page, size, adminId, "admin", role));
    }

    @PostMapping("/reports/{id}/review")
    @RequireRole("SUPER_ADMIN")
    public Result<String> reviewReport(@PathVariable Integer id,
                                       @RequestAttribute("requesterId") Integer adminId,
                                       @RequestAttribute("requesterRole") String role,
                                       @Valid @RequestBody ForumReportReviewDTO body) {
        moderationService.reviewReport(id, body.getFounded(),
                body.getNote(), adminId, "admin", role);
        return Result.ok("举报处理完成");
    }

    @GetMapping("/appeals")
    @RequireRole("SUPER_ADMIN")
    public Result<IPage<ForumAppeal>> appeals(@RequestAttribute("requesterId") Integer adminId,
                                              @RequestAttribute("requesterRole") String role,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return Result.ok(moderationService.pendingAppeals(page, size, adminId, "admin", role));
    }

    @PostMapping("/appeals/{id}/review")
    @RequireRole("SUPER_ADMIN")
    public Result<String> reviewAppeal(@PathVariable Integer id,
                                       @RequestAttribute("requesterId") Integer adminId,
                                       @RequestAttribute("requesterRole") String role,
                                       @Valid @RequestBody ForumAppealReviewDTO body) {
        moderationService.reviewAppeal(id, body.getApproved(),
                Boolean.TRUE.equals(body.getRestorePost()),
                body.getNote(), adminId, "admin", role);
        return Result.ok("申诉复核完成");
    }

    @GetMapping("/sensitive-words")
    @RequireRole("SUPER_ADMIN")
    public Result<List<ForumSensitiveWord>> sensitiveWords() {
        return Result.ok(sensitiveWordMapper.selectList(new LambdaQueryWrapper<ForumSensitiveWord>()
                .orderByDesc(ForumSensitiveWord::getCreatedAt)));
    }

    @PostMapping("/sensitive-words")
    @RequireRole("SUPER_ADMIN")
    public Result<ForumSensitiveWord> addSensitiveWord(@Valid @RequestBody SensitiveWordDTO body) {
        ForumSensitiveWord item = new ForumSensitiveWord(); item.setWord(body.getWord().trim()); item.setEnabled(1);
        sensitiveWordMapper.insert(item); return Result.ok(item);
    }

    @DeleteMapping("/sensitive-words/{id}")
    @RequireRole("SUPER_ADMIN")
    public Result<String> deleteSensitiveWord(@PathVariable Integer id) {
        sensitiveWordMapper.deleteById(id); return Result.ok("已删除");
    }
}
