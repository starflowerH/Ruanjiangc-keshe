package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 后台管理用户表 - 管理员/大学老师的登录账号
 *
 * 角色说明（对应业务文档）：
 *   SUPER_ADMIN  - 超级管理员：拥有后台所有权限
 *   NORMAL_ADMIN - 普通管理员：部分权限（如只读、只管理指定模块）
 *   TEACHER      - 大学老师：仅管理本校志愿者、本校宣传信息
 *
 * 安全设计：
 *   - 密码使用哈希存储（BCrypt），不存明文
 *   - 教师角色必须绑定 university_id，实现数据隔离
 *
 * @author demo
 */
@Data
@TableName("admin_user")
public class AdminUser {

    /** 管理员ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 登录账号名，唯一 */
    private String username;

    /** 密码哈希值，使用 BCryptPasswordEncoder 加密存储 */
    private String passwordHash;

    /** 角色枚举：SUPER_ADMIN / NORMAL_ADMIN / TEACHER */
    private String role;

    /**
     * 所属高校ID（仅 TEACHER 角色需要）
     * 绑定后教师只能看到自己大学的数据
     */
    private Integer universityId;

    /** 真实姓名/联系人姓名 */
    private String name;

    /** 联系电话 */
    private String phone;

    /**
     * 权限配置（JSON格式）
     * 示例：{"canEdit":true,"canDelete":true,"canAudit":true}
     * 仅 SUPER_ADMIN 可修改其他人的权限
     * NORMAL_ADMIN 默认为空（仅看数据），由 SUPER_ADMIN 动态分配
     */
    private String permissions;

    /** 账号状态：1=正常，0=禁用 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
