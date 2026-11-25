package com.demo.miniprogram.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信登录请求参数 - 支持三种登录模式
 *
 * 【模式1】纯静默登录（推荐首次进入时使用）
 *   前端：wx.login() → 拿到code
 *   后端：code → openid → 判断新老用户 → 返回JWT
 *   适用场景：用户首次打开小程序，不需要手机号就能浏览
 *
 * 【模式2】登录 + 一键获取手机号（推荐注册/报名时使用）
 *   前端：wx.login() 拿到 loginCode
 *        + 用户点击 <button open-type="getPhoneNumber"> 拿到 phoneCode
 *   后端：loginCode → openid + session_key
 *        phoneCode → 调用微信API直接换手机号（新版接口，推荐）
 *   适用场景：需要手机号的场景（报名、注册等），用户点击按钮一键获取
 *
 * 【模式3】登录 + 传统加密手机号（兼容旧版，不推荐）
 *   前端：wx.login() 拿到 loginCode
 *        + 用户点击按钮拿到 {encryptedData, iv}
 *   后端：用session_key解密encryptedData得到手机号
 *   适用场景：兼容旧版微信或特殊场景
 *
 * @author demo
 */
@Data
public class WxLoginDTO {

    /** 微信登录凭证 - wx.login() 返回的临时code（必填，有效期5分钟） */
    @NotBlank(message = "登录凭证不能为空")
    private String code;

    /**
     * 手机号code - getPhoneNumber 按钮返回的code（选填）
     *
     * 前端获取方式：
     *   <button open-type="getPhoneNumber" bindgetphonenumber="onGetPhone">
     *   onGetPhone(e) { const phoneCode = e.detail.code; }
     *
     * 后端处理方式：
     *   调微信 getuserphonenumber 接口，用此code直接换取真实手机号
     *   这是2023年后的新版接口，推荐使用
     */
    private String phoneCode;

    /**
     * 加密手机号数据（选填，旧版兼容）
     * 仅当使用传统 getPhoneNumber 返回 encryptedData 时传入
     */
    private String encryptedData;

    /**
     * 加密初始向量（选填，旧版兼容）
     * 配合 encryptedData 使用
     */
    private String iv;

    /** 微信昵称（用户授权后获取，选填） */
    private String nickname;

    /** 微信头像URL（用户授权后获取，选填） */
    private String avatarUrl;

    /** 本次希望进入的身份：student / volunteer（选填） */
    private String identityRole;
}
