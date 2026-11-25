package com.demo.miniprogram.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 接口限流拦截器 - 基于IP + 接口路径的滑动时间窗口限流
 *
 * 防护目标：
 *   - 防止暴力破解管理员密码（admin-login接口严格限制）
 *   - 防止恶意刷微信登录接口（消耗微信API调用额度）
 *   - 防止接口滥用导致数据库压力过大
 *
 * 算法：固定时间窗口 + 计数器
 *   每个窗口内超过阈值 → 返回429 Too Many Requests
 *   窗口过期后自动重置
 *
 * 限流规则（按接口分级）：
 *   L1-宽松: 普通查询接口      → 100次/分钟  （正常用户不会超）
 *   L2-标准: 业务操作接口      → 30次/分钟   （报名、提问、回答）
 *   L3-严格: 登录/认证接口     → 10次/分钟   （防暴力破解）
 *   L4-极严: 管理员登录接口    → 5次/分钟    （最关键的保护对象）
 *
 * @author demo
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    /** 限流记录：key=ip:path, value=窗口开始时间+计数 */
    private static final Map<String, WindowCounter> COUNTERS = new ConcurrentHashMap<>();

    /** 窗口大小（毫秒），默认1分钟 */
    private static final long WINDOW_MS = 60_000;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String path = request.getRequestURI();
        String ip = getClientIp(request);

        // 获取该路径的限流阈值
        int limit = getLimit(path);
        if (limit <= 0) {
            return true; // 不限流
        }

        // 构建限流key：IP + 路径
        String key = ip + ":" + path;

        WindowCounter counter = COUNTERS.compute(key, (k, v) -> {
            long now = System.currentTimeMillis();
            if (v == null || now - v.windowStart > WINDOW_MS) {
                // 新窗口或窗口过期 → 重置
                return new WindowCounter(now, new AtomicInteger(1));
            }
            v.count.incrementAndGet();
            return v;
        });

        // 超过阈值 → 拒绝请求
        if (counter != null && counter.count.get() > limit) {
            log.warn("限流触发: ip={}, path={}, count={}, limit={}", ip, path, counter.count.get(), limit);
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(429);
            response.getWriter().write("{\"code\":429,\"message\":\"请求太频繁，请稍后再试\"}");
            return false;
        }

        return true;
    }

    /* ========== 限流规则 ========== */

    /**
     * 根据请求路径返回限流阈值（次/分钟）
     */
    private int getLimit(String path) {
        // L4: 管理员登录 → 5次/分钟（防暴力破解）
        if (path.contains("/auth/admin-login")) {
            return 5;
        }
        // L3: 微信登录 → 10次/分钟（防恶意消耗微信API额度）
        if (path.contains("/auth/wx-login")) {
            return 10;
        }
        // L3: 绑定手机号 → 10次/分钟
        if (path.contains("/auth/bind-phone")) {
            return 10;
        }
        // L2: 提交报名/提问/回答 → 30次/分钟
        if (path.contains("/volunteer/apply")
                || path.contains("/qa/ask")
                || path.contains("/qa/answer")
                || path.contains("/admin/")) {
            return 30;
        }
        // L1: 其他查询接口 → 100次/分钟
        return 100;
    }

    /* ========== 获取真实IP（考虑反向代理） ========== */

    /**
     * 获取客户端真实IP
     *
     * 优先从代理头获取（适用于Nginx/Cloudflare等反向代理场景）
     * 如果没有代理头，则使用直接连接的IP
     */
    private String getClientIp(HttpServletRequest request) {
        // 常见代理头（按优先级排序，注意X-Forwarded-For可被伪造，需配置可信代理）
        String[] headers = {
                "X-Real-IP",                    // Nginx 真实IP
                "X-Forwarded-For",              // 标准代理头（取第一个IP）
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For 格式：client, proxy1, proxy2 → 取第一个
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    /* ========== 内部类：时间窗口计数器 ========== */

    private static class WindowCounter {
        final long windowStart;        // 窗口开始时间戳
        final AtomicInteger count;     // 当前窗口请求计数

        WindowCounter(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
