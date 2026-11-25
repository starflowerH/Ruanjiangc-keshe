package com.demo.miniprogram.security;

import com.demo.miniprogram.common.BusinessException;
import com.demo.miniprogram.common.ResultCode;
import com.demo.miniprogram.service.TeacherPermissionService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class TeacherPermissionAspect {

    private final TeacherPermissionService permissionService;

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint,
                                  RequireTeacherPermission requirePermission) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        String userType = (String) attributes.getRequest().getAttribute("userType");
        String role = (String) attributes.getRequest().getAttribute("requesterRole");
        if (!"admin".equals(userType)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无老师功能权限");
        }
        if ("SUPER_ADMIN".equals(role)) {
            return joinPoint.proceed();
        }
        if (!"TEACHER".equals(role)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无老师功能权限");
        }

        Integer teacherId = (Integer) attributes.getRequest().getAttribute("requesterId");
        if (!permissionService.hasAny(teacherId, requirePermission.value())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该功能权限已被撤销，请联系超级管理员");
        }
        return joinPoint.proceed();
    }
}
