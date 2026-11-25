package com.demo.miniprogram.controller;

import com.demo.miniprogram.common.Result;
import com.demo.miniprogram.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 个人资料接口 - 高中生/大学生/老师编辑自己的信息
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * 获取当前用户资料
     * GET /api/profile/my
     */
    @GetMapping("/my")
    public Result<Map<String, Object>> getMyProfile(
            @RequestAttribute("openid") String openid,
            @RequestAttribute("userType") String userType,
            @RequestAttribute(value = "relatedId", required = false) Integer relatedId,
            @RequestAttribute(value = "relatedType", required = false) String relatedType) {
        return Result.ok(profileService.getProfile(openid, userType, relatedId, relatedType));
    }

    /**
     * 更新当前用户资料
     * POST /api/profile/update
     * Body: { nickname, avatarUrl, name, phone, wechatId, major, grade }
     */
    @PostMapping("/update")
    public Result<Map<String, Object>> updateProfile(
            @RequestAttribute("openid") String openid,
            @RequestAttribute("userType") String userType,
            @RequestAttribute(value = "relatedId", required = false) Integer relatedId,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @RequestBody Map<String, Object> updates) {
        return Result.ok("保存成功", profileService.updateProfile(openid, userType, relatedId, relatedType, updates));
    }
}
