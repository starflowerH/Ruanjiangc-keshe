package com.demo.miniprogram.vo;

import lombok.Data;
import java.util.List;

/**
 * 宣传队伍视图 - 对应业务文档阶段3-组队期"队伍查看页面"
 *
 * 页面展示内容：
 *   - 目标高中名称
 *   - 前往该高中的所有大学队伍列表
 *   - 每个大学的队长姓名、手机号、队伍人数
 *
 * @author demo
 */
@Data
public class TeamVO {

    /** 队伍ID */
    private Integer id;

    /** 队伍号 */
    private String teamCode;

    /** 队伍名称 */
    private String teamName;

    /** 目标高中ID */
    private Integer highSchoolId;

    /** 目标高中名称 */
    private String highSchoolName;

    /** 大学ID */
    private Integer universityId;

    /** 大学名称 */
    private String universityName;

    /** 队长ID */
    private Integer captainVolunteerId;

    /** 队长姓名 */
    private String captainName;

    /** 队长手机号（仅队长之间可见） */
    private String captainPhone;

    /** 队长对外通讯微信号（仅队长通讯录可见） */
    private String captainWechat;

    /** 是否已配置队长对外通讯方式 */
    private Boolean captainContactConfigured;

    /** 副队长姓名 */
    private String viceCaptainName;
    /** 队伍人数 */
    private Integer memberCount;

    /** 队伍简介 */
    private String description;

    /** 队伍成员列表 */
    private List<MemberVO> members;
}
