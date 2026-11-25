package com.demo.miniprogram.controller;

import com.demo.miniprogram.common.Result;
import com.demo.miniprogram.dto.AdminLoginDTO;
import com.demo.miniprogram.dto.TeacherRegisterDTO;
import com.demo.miniprogram.dto.WxLoginDTO;
import com.demo.miniprogram.service.AuthService;
import com.demo.miniprogram.vo.LoginVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口 - 微信登录 & 管理员登录 & 手机号绑定
 *
 * 微信登录完整流程（前端开发指南）：
 *
 *   【场景A - 静默登录（首次打开小程序）】
 *     1. 前端调 wx.login() 获取 code
 *     2. POST /api/auth/wx-login  { "code": "xxx" }
 *     3. 后端返回 JWT + userType="new_user"（说明还未注册身份）
 *     4. 前端根据 userType 跳转对应页面
 *
 *   【场景B - 登录时一键获取手机号（推荐，在报名/注册时使用）】
 *     1. 前端调 wx.login() 获取 loginCode
 *     2. 用户点击 <button open-type="getPhoneNumber"> → 获取 phoneCode
 *     3. POST /api/auth/wx-login { "code": "xxx", "phoneCode": "yyy" }
 *     4. 后端自动绑定手机号，返回 JWT + phone="138xxxx1234"
 *
 *   【场景C - 先登录后绑定手机号（静默登录后，在需要手机号时再绑）】
 *     1. 已有JWT登录态
 *     2. 用户点击按钮 → POST /api/auth/bind-phone { "phoneCode": "yyy" }
 *     3. 后端绑定手机号，同步更新到报名/注册记录
 *
 * @author demo
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 微信用户登录（小程序/公众号H5）
     *
     * 支持三种模式：
     *   1. 纯静默登录        → 只传 code
     *   2. 登录+新版手机号    → 传 code + phoneCode（★推荐）
     *   3. 登录+旧版加密手机号 → 传 code + encryptedData + iv
     *
     * 请求示例：
     *   // 静默登录
     *   POST /api/auth/wx-login
     *   {"code": "081xxxx"}
     *
     *   // 登录+一键获取手机号
     *   POST /api/auth/wx-login
     *   {"code": "081xxxx", "phoneCode": "d7c9xxxx...", "nickname": "张三"}
     *
     * 响应示例：
     *   {
     *     "code": 200,
     *     "data": {
     *       "token": "eyJhbG...",
     *       "openid": "oXXXX...",
     *       "nickname": "张三",
     *       "userType": "new_user",
     *       "phone": "138xxxx1234"    // 如果传了phoneCode则有值
     *     }
     *   }
     *
     * @param dto 微信登录参数
     * @return JWT Token + 用户信息 + 手机号
     */
    @PostMapping("/wx-login")
    public Result<LoginVO> wxLogin(@Valid @RequestBody WxLoginDTO dto) {
        LoginVO loginVO = authService.wxLogin(dto);
        return Result.ok(loginVO);
    }

    /** 切换同一微信账号当前使用的高中生/志愿者身份。 */
    @PostMapping("/switch-role")
    public Result<LoginVO> switchRole(
            @RequestAttribute("openid") String openid,
            @Valid @RequestBody SwitchRoleDTO dto) {
        return Result.ok(authService.switchIdentity(openid, dto.getRole()));
    }

    /**
     * 单独绑定手机号 - 用于已登录但未绑定的用户
     *
     * 使用场景：
     *   用户静默登录成功（有JWT），但还没有手机号。
     *   当进行需要手机号的操作（如报名）时，前端弹出getPhoneNumber授权按钮，
     *   用户点击授权后，调此接口绑定。
     *
     * 请求示例：
     *   POST /api/auth/bind-phone
     *   Header: Authorization: Bearer {token}
     *   {"phoneCode": "d7c9xxxx..."}
     *
     * 响应示例：
     *   {"code": 200, "data": "138xxxx1234", "message": "success"}
     */
    @PostMapping("/bind-phone")
    public Result<String> bindPhone(
            @RequestAttribute("openid") String openid,
            @Valid @RequestBody BindPhoneDTO dto) {
        String phone = authService.bindPhone(openid, dto.getPhoneCode());
        return Result.ok("手机号绑定成功", phone);
    }

    /**
     * 管理员/老师登录（后台管理系统）
     *
     * 请求示例：
     *   POST /api/auth/admin-login
     *   {"username": "admin", "password": "123456"}
     */
    @PostMapping("/admin-login")
    public Result<LoginVO> adminLogin(@Valid @RequestBody AdminLoginDTO dto) {
        LoginVO loginVO = authService.adminLogin(dto);
        return Result.ok(loginVO);
    }

    /**
     * 老师注册（微信授权登录，无需密令）
     *
     * 请求示例：
     *   POST /api/auth/teacher-register
     *   {
     *     "code": "wx_login_code",
     *     "name": "张老师",
     *     "phone": "138xxxx1234",
     *     "universityId": 1
     *   }
     */
    @PostMapping("/teacher-register")
    public Result<LoginVO> teacherRegister(@Valid @RequestBody TeacherRegisterDTO dto) {
        LoginVO loginVO = authService.teacherRegister(dto);
        return Result.ok("注册成功", loginVO);
    }

    /**
     * 老师登录（微信授权登录，无需密令）
     *
     * 请求示例：
     *   POST /api/auth/teacher-login
     *   { "code": "wx_login_code" }
     */
    @PostMapping("/teacher-login")
    public Result<LoginVO> teacherLogin(@Valid @RequestBody TeacherLoginDTO dto) {
        LoginVO loginVO = authService.teacherLogin(dto.getCode());
        return Result.ok(loginVO);
    }

    /**
     * 老师登录请求体
     */
    @lombok.Data
    static class TeacherLoginDTO {
        @NotBlank(message = "微信登录凭证不能为空")
        private String code;
    }

    /**
     * 手机号绑定请求体
     */
    @lombok.Data
    static class BindPhoneDTO {
        /** 前端 getPhoneNumber 按钮返回的 code */
        @NotBlank(message = "手机号凭证不能为空")
        private String phoneCode;
    }

    @lombok.Data
    static class SwitchRoleDTO {
        @NotBlank(message = "身份不能为空")
        @Pattern(regexp = "student|volunteer", message = "身份只能是 student 或 volunteer")
        private String role;
    }
}
