package com.demo.miniprogram.security;

import java.lang.annotation.*;

/**
 * 角色校验注解 - 标注在 Controller 方法上，限制访问角色
 *
 * 使用示例：
 *   @RequireRole("SUPER_ADMIN")                           - 仅超级管理员
 *   @RequireRole({"SUPER_ADMIN", "NORMAL_ADMIN"})         - 所有管理员
 *   @RequireRole({"SUPER_ADMIN", "TEACHER"})              - 超级管理员 + 老师
 *   @RequireRole({"SUPER_ADMIN", "NORMAL_ADMIN", "TEACHER"}) - 所有管理员角色
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    /**
     * 允许的角色列表
     * 支持：SUPER_ADMIN, NORMAL_ADMIN, TEACHER, CAPTAIN, VOLUNTEER, STUDENT
     */
    String[] value();
}
