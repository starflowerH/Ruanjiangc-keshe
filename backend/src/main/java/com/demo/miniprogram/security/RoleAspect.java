package com.demo.miniprogram.security;

import com.demo.miniprogram.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.Arrays;

/**
 * 角色校验 AOP 切面
 * 拦截 @RequireRole 注解，验证当前用户是否有权访问
 */
@Slf4j
@Aspect
@Component
public class RoleAspect {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(requireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint, RequireRole requireRole) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        // 从 request attributes 中获取角色信息（由 JwtAuthFilter 注入）
        String userType = (String) attributes.getRequest().getAttribute("userType");
        String requesterRole = (String) attributes.getRequest().getAttribute("requesterRole");
        String openid = (String) attributes.getRequest().getAttribute("openid");

        String[] allowedRoles = requireRole.value();
        boolean allowed = false;

        // 判断用户是否在允许的角色列表中
        for (String role : allowedRoles) {
            switch (role) {
                case "SUPER_ADMIN":
                    if ("admin".equals(userType) && "SUPER_ADMIN".equals(requesterRole)) {
                        allowed = true;
                    }
                    break;
                case "NORMAL_ADMIN":
                    if ("admin".equals(userType) && "NORMAL_ADMIN".equals(requesterRole)) {
                        allowed = true;
                    }
                    break;
                case "TEACHER":
                    if ("admin".equals(userType) && "TEACHER".equals(requesterRole)) {
                        allowed = true;
                    }
                    break;
                case "ADMIN":
                    // 任意管理员角色
                    if ("admin".equals(userType)) {
                        allowed = true;
                    }
                    break;
                case "CAPTAIN":
                    // 队长身份需要额外查询，这里通过 request attribute 判断
                    Boolean isCaptain = (Boolean) attributes.getRequest().getAttribute("isCaptain");
                    if (Boolean.TRUE.equals(isCaptain)) {
                        allowed = true;
                    }
                    break;
                case "VOLUNTEER":
                    if ("wechat".equals(userType)) {
                        String relatedType = (String) attributes.getRequest().getAttribute("relatedType");
                        if ("volunteer".equals(relatedType)) {
                            allowed = true;
                        }
                    }
                    break;
                case "STUDENT":
                    if ("wechat".equals(userType)) {
                        String relatedType = (String) attributes.getRequest().getAttribute("relatedType");
                        if ("student".equals(relatedType)) {
                            allowed = true;
                        }
                    }
                    break;
                default:
                    log.warn("未知角色: {}", role);
            }
        }

        if (!allowed) {
            log.warn("权限校验失败: 用户={}, 角色={}, 需要={}", openid, requesterRole, Arrays.toString(allowedRoles));
            sendForbidden(attributes.getRequest().getAttribute("javax.servlet.error.message") != null ? null : attributes.getResponse());
            return null;
        }

        return joinPoint.proceed();
    }

    private void sendForbidden(HttpServletResponse response) throws IOException {
        if (response == null) return;
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        Result<?> result = Result.fail(403, "权限不足，无法执行此操作");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
