package com.demo.miniprogram.controller;

import com.demo.miniprogram.common.Result;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.protocol.Message;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 错误上报接口 - 接收小程序前端捕获的异常，转发到 Sentry 错误监控
 *
 * 小程序受微信 request 合法域名限制，无法直连 Sentry ingest 域名，
 * 因此前端把错误 JSON POST 到本接口（hynu.site 已备案、在微信白名单内），
 * 由后端代转发到 Sentry。
 */
@Slf4j
@RestController
@RequestMapping("/api/report")
public class ReportController {

    /** 每 IP 在窗口内允许的最大上报条数（防刷 Sentry 配额） */
    private static final int MAX_REPORTS_PER_WINDOW = 5;
    /** 窗口时长（毫秒） */
    private static final long RATE_WINDOW_MS = 10_000L;

    /** IP -> [窗口起始时间, 已上报条数]，超限的旧条目惰性清理 */
    private static final Map<String, long[]> rateLimits = new ConcurrentHashMap<>();

    @Data
    public static class ErrorReport {
        private String message;
        private String stack;
        private String url;
        private String method;
        private Integer status;
        private String type;
        private String version;
    }

    @PostMapping("/error")
    public Result<Void> reportError(@RequestBody(required = false) ErrorReport report,
                                    HttpServletRequest request) {
        try {
            if (report == null || report.getMessage() == null) {
                return Result.fail("上报内容为空");
            }

            // 按 IP 限流：10 秒内最多 5 条，超出直接丢弃（返回成功但不上报）
            String clientIp = resolveClientIp(request);
            if (isRateLimited(clientIp)) {
                log.warn("错误上报被限流: ip={}", clientIp);
                return Result.ok();
            }

            SentryEvent event = new SentryEvent();
            Message message = new Message();
            message.setFormatted(report.getMessage());
            event.setMessage(message);
            event.setTag("source", "frontend");
            if (report.getType() != null) {
                event.setTag("error_type", report.getType());
            }
            if (report.getVersion() != null) {
                event.setTag("version", report.getVersion());
            }
            Map<String, Object> extra = new HashMap<>();
            extra.put("url", report.getUrl());
            extra.put("method", report.getMethod());
            extra.put("status", report.getStatus());
            if (report.getStack() != null) {
                extra.put("stack", report.getStack());
            }
            event.setExtras(extra);
            Sentry.captureEvent(event);
            log.info("前端错误已上报 Sentry: message={}, url={}", report.getMessage(), report.getUrl());
        } catch (Exception e) {
            log.warn("前端错误上报 Sentry 失败: {}", e.getMessage());
        }
        return Result.ok();
    }

    /** 获取客户端真实 IP（考虑反向代理 X-Forwarded-For） */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int idx = forwarded.indexOf(',');
            return (idx > 0 ? forwarded.substring(0, idx) : forwarded).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    /** 限流判断：窗口内超过 MAX_REPORTS_PER_WINDOW 则返回 true */
    private boolean isRateLimited(String ip) {
        long now = System.currentTimeMillis();
        long[] entry = rateLimits.get(ip);
        if (entry == null || now - entry[0] >= RATE_WINDOW_MS) {
            // 新窗口（原子写入或覆盖过期条目）
            rateLimits.put(ip, new long[]{now, 1});
            return false;
        }
        if (entry[1] >= MAX_REPORTS_PER_WINDOW) {
            return true;
        }
        entry[1]++;
        return false;
    }
}
