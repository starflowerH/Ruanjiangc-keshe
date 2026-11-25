package com.demo.miniprogram.vo;

import lombok.Data;

/**
 * 队伍成员详情 - 点击队伍成员头像时展示
 *
 * 仅同一队伍的成员可见。展示志愿者报名时填写的信息。
 *
 * @author demo
 */
@Data
public class MemberDetailVO {

    /** 志愿者ID */
    private Integer volunteerId;

    /** 姓名 */
    private String name;

    /** 手机号 */
    private String phone;

    /** 微信号 */
    private String wechatId;

    /** 年级 */
    private String grade;

    /** 专业 */
    private String major;

    /** 大学名称 */
    private String universityName;

    /** 目标高中名称 */
    private String targetHighSchoolName;

    /** 队伍角色：CAPTAIN / MEMBER */
    private String role;
}
