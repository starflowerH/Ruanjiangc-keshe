package com.demo.miniprogram.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置
 *
 * 为后端提供线程池并发处理能力，用于：
 *   - 通知异步发送（fire-and-forget）
 *   - 组队并行处理（CompletableFuture）
 *   - 批量查询并行化
 *
 * @author demo
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 通用异步任务线程池
     *
     * 参数说明：
     *   corePoolSize=10   — 核心线程数，常驻线程
     *   maxPoolSize=50    — 最大线程数，峰值时临时扩容
     *   queueCapacity=200 — 缓冲队列，超出核心线程数的任务先排队
     *   CallerRunsPolicy  — 队列满时由调用线程执行，防止任务丢失
     */
    @Bean("taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：平时保持10个线程待命
        executor.setCorePoolSize(10);
        // 最大线程数：高峰期最多扩展到50个
        executor.setMaxPoolSize(50);
        // 缓冲队列：200个任务排队等待
        executor.setQueueCapacity(200);
        // 空闲线程60秒后回收
        executor.setKeepAliveSeconds(60);
        // 线程名前缀，便于日志追踪
        executor.setThreadNamePrefix("async-");
        // 拒绝策略：队列满时由调用线程执行，保证任务不丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 优雅关闭：等待所有任务完成后才关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * 让 @Async 注解默认使用 taskExecutor
     */
    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor();
    }

    /**
     * 异步方法抛出异常时的处理器（记录日志，不中断主流程）
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
