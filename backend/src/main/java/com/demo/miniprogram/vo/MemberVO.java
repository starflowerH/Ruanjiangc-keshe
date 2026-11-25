package com.demo.miniprogram.vo;

import lombok.Data;

/**
 * 队员信息视图（精简版，用于队伍成员列表）
 *
 * @author demo
 */
@Data
public class MemberVO {

    /** 志愿者ID */
    private Integer volunteerId;

    /** 姓名 */
    private String name;

    /** 手机号 */
    private String phone;

    /** 队长对外通讯微信号 */
    private String wechat;

    /** 所属大学名称 */
    private String universityName;

    /** 队伍名称，用于队长通讯录区分来源。 */
    private String teamName;

    /** 目标高中名称 */
    private String targetHighSchoolName;

    /** 专业 */
    private String major;

    /** 年级 */
    private String grade;

    /** 是否为队长 */
    private Boolean isCaptain;

    /** 队伍内角色: MEMBER / CAPTAIN */
    private String role;
}
