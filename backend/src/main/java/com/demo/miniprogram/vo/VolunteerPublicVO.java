package com.demo.miniprogram.vo;

import lombok.Data;

/**
 * 志愿者信息公开视图（面向高中生/未登录用户）
 *
 * 与 VolunteerVO 的区别：
 *   - 不返回手机号、微信号等个人隐私信息
 *   - 只展示姓名、大学、专业等宣传需要的公开信息
 *
 * 使用场景：GET /api/volunteer/by-high-school（白名单接口，无需登录）
 *
 * @author demo
 */
@Data
public class VolunteerPublicVO {

    /** 志愿者ID */
    private Integer id;

    /** 姓名 */
    private String name;

    /** 志愿者头像（微信头像，公开展示用） */
    private String avatarUrl;

    /** 大学名称 */
    private String universityName;

    /** 年级 */
    private String grade;

    /** 专业 */
    private String major;

    /** 是否为队长 */
    private Integer isCaptainApplicant;
}
