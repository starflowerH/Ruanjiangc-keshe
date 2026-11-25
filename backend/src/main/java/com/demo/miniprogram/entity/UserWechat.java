package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 微信用户主体表 - 小程序/公众号的核心用户标识
 *
 * 设计说明：
 *   - openid 是微信体系中用户在某个应用下的唯一标识，天然适合做主键
 *   - unionid 用于跨应用（公众号+小程序）识别同一用户，便于多端互通
 *   - 所有用户（大学生、高中生）都先有微信账号，再关联到具体角色表
 *
 * 对接微信接口：网页授权 OAuth2.0 获取 openid
 *
 * @author demo
 */
@Data
@TableName("user_wechat")
public class UserWechat {

    /** 微信OpenID - 用户在公众号/小程序下的唯一标识，作为主键 */
    @TableId
    private String openid;

    /**
     * 微信UnionID - 跨应用统一标识
     * 如果开发者拥有多个移动应用、网站应用和公众账号，
     * 可通过 UnionID 来区分用户的唯一性
     */
    private String unionid;

    /** 微信昵称（用户授权后获取） */
    private String nickname;

    /** 微信头像URL（用户授权后获取） */
    private String avatarUrl;

    /** 首次进入时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
