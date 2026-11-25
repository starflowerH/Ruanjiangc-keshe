package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 高中生用户表 - 在高中端注册的高中生
 *
 * 业务流程（对应业务文档阶段4-宣传执行期）：
 *   1. 大学生到达高中，发放印有公众号二维码的宣传物料
 *   2. 高中生扫码关注公众号
 *   3. 注册：填写手机号，选择自己所在的高中
 *   4. 系统自动匹配：展示去过/准备去该高中宣传的各大学学长学姐
 *
 * 匹配逻辑（核心）：
 *   volunteer.target_high_school_id == high_school_student.high_school_id
 *   → 高中生能看到所有"目标高中=我所在高中"的大学生
 *
 * @author demo
 */
@Data
@TableName("high_school_student")
public class HighSchoolStudent {

    /** 高中生记录ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 微信OpenID，唯一（一个微信只能注册一个高中生身份） */
    private String openid;

    /** 真实姓名 */
    private String name;

    /** 高中生身份昵称，与同一微信账号的其他身份独立 */
    private String nickname;

    /** 微信头像URL */
    private String avatarUrl;

    /** 手机号码 */
    private String phone;

    /**
     * 所在高中ID，关联 high_school 表
     * 系统通过这个字段自动匹配来本校宣传的大学生
     */
    private Integer highSchoolId;

    /** 所在高中名称，仅用于接口返回，不写入学生表 */
    @TableField(exist = false)
    private String highSchoolName;

    /** 注册时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
