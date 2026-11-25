package com.demo.miniprogram.controller;

import com.demo.miniprogram.common.Result;
import com.demo.miniprogram.dto.UniversityContentDTO;
import com.demo.miniprogram.entity.UniversityContent;
import com.demo.miniprogram.entity.PromotionManagerGrant;
import com.demo.miniprogram.service.PromotionManagementService;
import com.demo.miniprogram.service.UniversityContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 大学宣传资料接口 - 老师编辑本校宣传信息
 *
 * 权限：仅 TEACHER 角色可编辑本校数据
 * 公开读取：无需登录即可查看
 */
@RestController
@RequestMapping("/api/university-content")
@RequiredArgsConstructor
public class UniversityContentController {

    private final UniversityContentService contentService;
    private final PromotionManagementService promotionManagementService;

    /**
     * 获取大学宣传资料（公开接口）
     *
     * GET /api/university-content/{universityId}
     */
    @GetMapping("/{universityId}")
    public Result<UniversityContent> getContent(@PathVariable Integer universityId) {
        UniversityContent content = contentService.getByUniversityId(universityId);
        if (content == null) {
            return Result.ok(null); // 无资料时返回空，前端使用默认mock数据
        }
        return Result.ok(content);
    }

    /**
     * 保存或更新本校宣传资料（已授权老师或大学生）
     *
     * POST /api/university-content/save
     * Header: Authorization: Bearer {token}
     */
    @PostMapping("/save")
    public Result<UniversityContent> saveContent(
            @RequestAttribute("requesterId") Integer requesterId,
            @RequestAttribute("userType") String userType,
            @RequestAttribute(value = "requesterRole", required = false) String role,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @RequestAttribute(value = "adminUniversityId", required = false) Integer adminUniversityId,
            @Valid @RequestBody UniversityContentDTO dto) {
        Integer universityId = promotionManagementService.resolveManagedUniversity(
                requesterId, userType, role, relatedType, adminUniversityId);
        UniversityContent saved = contentService.saveOrUpdate(universityId, dto);
        return Result.ok("保存成功", saved);
    }

    @GetMapping("/manager/status")
    public Result<java.util.Map<String, Object>> managerStatus(
            @RequestAttribute(value = "requesterId", required = false) Integer requesterId,
            @RequestAttribute(value = "userType", required = false) String userType,
            @RequestAttribute(value = "requesterRole", required = false) String role,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @RequestAttribute(value = "adminUniversityId", required = false) Integer adminUniversityId) {
        if ("admin".equals(userType) && "TEACHER".equals(role)) {
            try {
                Integer universityId = promotionManagementService.resolveManagedUniversity(
                        requesterId, userType, role, relatedType, adminUniversityId);
                return Result.ok(java.util.Map.of("manager", true, "universityId", universityId));
            } catch (com.demo.miniprogram.common.BusinessException ignored) {
                return Result.ok(java.util.Map.of("manager", false, "universityId", 0));
            }
        }
        PromotionManagerGrant grant = "volunteer".equals(relatedType)
                ? promotionManagementService.getGrant(requesterId) : null;
        return Result.ok(java.util.Map.of(
                "manager", grant != null,
                "universityId", grant != null ? grant.getUniversityId() : 0));
    }
}
