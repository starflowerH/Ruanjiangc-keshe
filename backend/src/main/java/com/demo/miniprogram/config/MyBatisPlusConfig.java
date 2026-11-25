package com.demo.miniprogram.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置
 *
 * 包含：
 *   1. 分页插件 - 支持物理分页（MySQL LIMIT）
 *   2. 自动填充处理器 - INSERT/UPDATE 时自动填充时间字段
 *
 * @author demo
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * 分页插件
     * MyBatis-Plus 默认不开启分页，需要手动注册此拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加 MySQL 分页方言
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 自动填充处理器
     *
     * 功能：在 INSERT/UPDATE 操作时自动填充 createdAt/updatedAt
     * 配合实体类上的 @TableField(fill = FieldFill.INSERT) 使用
     *
     * 好处：不用每次在业务代码中手动 setCreatedAt(new Date())
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {

            /** INSERT 时自动填充 */
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
                // QuestionAnswer.askedAt / TeamMember.joinedAt 也标了 FieldFill.INSERT，
                // 不填充会把 null 写进 INSERT SQL，覆盖数据库 DEFAULT current_timestamp()，
                // 导致 asked_at/joined_at 恒为 NULL（历史数据已受影响，见修复记录）
                this.strictInsertFill(metaObject, "askedAt", LocalDateTime.class, now);
                this.strictInsertFill(metaObject, "joinedAt", LocalDateTime.class, now);
            }

            /** UPDATE 时自动填充 */
            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
