package com.demo.miniprogram.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.Instant;

/**
 * 操作审计切面 - 记录所有敏感操作的审计日志
 *
 * 记录内容：
 *   - 操作人（IP/OpenID）
 *   - 操作类型（登录/审核/组队/删除等）
 *   - 操作目标（被审核的记录ID等）
 *   - 操作结果（成功/失败）
 *   - 操作耗时
 *   - 操作时间
 *
 * 安全价值：
 *   - 事后追溯：出事时能查到谁在什么时间做了什么操作
 *   - 异常检测：同一IP短时间内大量异常操作 → 可能是攻击
 *   - 合规要求：20w用户规模需要基本的操作审计能力
 *
 * @author demo
 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    /**
     * 切点：所有Controller的公开方法（查询类跳过，减少日志量）
     */
    @Pointcut("execution(* com.demo.miniprogram.controller..*.*(..))")
    public void controllerLayer() {}

    /**
     * 仅记录写操作（POST/PUT/DELETE），GET请求跳过
     */
    @Around("controllerLayer()")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attrs.getRequest();
        String method = request.getMethod();

        // GET请求不记录审计日志（只读操作，量大无意义）
        if ("GET".equalsIgnoreCase(method)) {
            return joinPoint.proceed();
        }

        String path = request.getRequestURI();
        String ip = getClientIp(request);
        String openid = (String) request.getAttribute("openid");
        String userType = (String) request.getAttribute("userType");

        Instant start = Instant.now();
        boolean success = true;
        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            success = false;
            throw e;
        } finally {
            long elapsedMs = Duration.between(start, Instant.now()).toMillis();

            // 脱敏后的操作人标识
            String operator = openid != null
                    ? userType + ":" + SensitiveDataUtil.maskToken(openid)
                    : "anonymous";

            log.info("[审计] operator={} | ip={} | {} {} | result={} | {}ms",
                    operator, ip, method, path, success ? "成功" : "失败", elapsedMs);

            // 异常操作告警：同一接口大量失败，可能是攻击
            if (!success) {
                log.warn("[审计-异常] 操作失败: operator={} | ip={} | {} {}",
                        operator, ip, method, path);
            }
        }
    }

    private static final String[] PROXY_HEADERS = {
            "X-Real-IP", "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP"
    };

    private String getClientIp(HttpServletRequest request) {
        for (String header : PROXY_HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.contains(",") ? ip.split(",")[0].trim() : ip;
            }
        }
        return request.getRemoteAddr();
    }
}
