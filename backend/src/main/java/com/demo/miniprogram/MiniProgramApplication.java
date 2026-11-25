package com.demo.miniprogram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 高校招生宣传小程序 - Spring Boot 启动类
 *
 * 项目简介：
 *   为高校招生宣传活动提供完整的线上管理平台，
 *   涵盖大学生报名、组队、高中生问答、后台管理等核心功能。
 *
 * 技术栈：Spring Boot 3.2 + MyBatis-Plus 3.5 + MySQL 8.0 + JWT + BCrypt
 *
 * 业务角色：
 *   - 超级管理员    → 后台全部权限
 *   - 普通管理员    → 部分权限
 *   - 大学老师      → 管理本校志愿者和宣传信息
 *   - 大学生志愿者  → 报名、回答问题、查看队伍
 *   - 高中生        → 浏览高校信息、学长学姐、提问
 *
 * 业务阶段：
 *   阶段1-报名期 → 阶段2-确认期 → 阶段3-组队期 → 阶段4-宣传期 → 阶段5-答疑期
 *
 * @author demo
 */
@SpringBootApplication
@EnableScheduling
public class MiniProgramApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniProgramApplication.class, args);
        System.out.println("========================================");
        System.out.println("  高校招生宣传小程序后端启动成功！");
        System.out.println("  API文档: http://localhost:8080/doc.html");
        System.out.println("========================================");
    }
}
