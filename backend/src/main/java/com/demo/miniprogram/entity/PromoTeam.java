package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 宣传队伍表
 *
 * 支持两种组队方式：
 *   1. 自动组队：管理员触发，按 (high_school_id, university_id) 自动分组
 *   2. 自由组队：志愿者创建队伍，通过队伍号邀请好友加入
 *
 * 队伍号格式：2个英文字母 + 3个数字，随机排列（如 AB123, X9Y3Z）
 *
 * @author demo
 */
@Data
@TableName("promo_team")
public class PromoTeam {

    /** 队伍ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 队伍号，2字母+3数字随机排列，唯一 */
    private String teamCode;

    /** 队伍名称（自由组队时创建人设置） */
    private String teamName;

    /** 目标高中ID - 队伍要去哪个高中宣传（自动组队时使用） */
    private Integer highSchoolId;

    /** 大学ID - 队伍代表哪个大学（自动组队时使用） */
    private Integer universityId;

    /** 队长ID，关联 volunteer 表 */
    private Integer captainVolunteerId;

    /** 队长对外通讯手机号（创建/接任队长时确认） */
    private String captainContactPhone;

    /** 队长对外通讯微信号（可选） */
    private String captainContactWechat;

    /** 副队长ID，关联 volunteer 表 */
    private Integer viceCaptainVolunteerId;
    /** 队伍简介（队伍成员可自行填写） */
    private String description;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
