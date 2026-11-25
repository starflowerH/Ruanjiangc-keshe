package com.demo.miniprogram.service;

import com.demo.miniprogram.common.BusinessException;
import com.demo.miniprogram.common.ResultCode;
import com.demo.miniprogram.entity.AdminUser;
import com.demo.miniprogram.mapper.AdminUserMapper;
import com.demo.miniprogram.security.TeacherPermissions;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TeacherPermissionService {

    private final AdminUserMapper adminUserMapper;
    private final ObjectMapper objectMapper;

    public boolean hasAny(Integer teacherId, String... requiredPermissions) {
        AdminUser teacher = teacherId == null ? null : adminUserMapper.selectById(teacherId);
        if (teacher == null || !"TEACHER".equals(teacher.getRole())
                || teacher.getStatus() == null || teacher.getStatus() != 1) {
            return false;
        }
        // Existing teacher accounts predate granular permissions and retain their old access
        // until a super admin saves an explicit permission set.
        if (teacher.getPermissions() == null || teacher.getPermissions().isBlank()) {
            return true;
        }
        Map<String, Boolean> permissions = parse(teacher.getPermissions());
        for (String permission : requiredPermissions) {
            if (Boolean.TRUE.equals(permissions.get(permission))) {
                return true;
            }
        }
        return false;
    }

    public void require(Integer teacherId, String permission) {
        if (!hasAny(teacherId, permission)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "未获得该老师功能权限");
        }
    }

    /**
     * 获取当前老师的权限明细（实时查库，供前端菜单显隐与授权状态标注）。
     *
     * 旧老师账号（permissions 为空）在存量期保持全权限，返回全部 true；
     * 新老师账号返回其显式保存的权限集合（全部键，true/false）。
     */
    public Map<String, Boolean> getPermissions(Integer teacherId) {
        AdminUser teacher = teacherId == null ? null : adminUserMapper.selectById(teacherId);
        if (teacher == null || !"TEACHER".equals(teacher.getRole())
                || teacher.getStatus() == null || teacher.getStatus() != 1) {
            return Collections.emptyMap();
        }
        Map<String, Boolean> result = new LinkedHashMap<>();
        // 存量老师：permissions 为空时保持全权限（与 hasAny 的兼容逻辑一致）
        if (teacher.getPermissions() == null || teacher.getPermissions().isBlank()) {
            TeacherPermissions.ALL.forEach(key -> result.put(key, true));
            return result;
        }
        Map<String, Boolean> parsed = parse(teacher.getPermissions());
        TeacherPermissions.ALL.forEach(key -> result.put(key, Boolean.TRUE.equals(parsed.get(key))));
        return result;
    }

    public String serialize(Map<String, Boolean> requestedPermissions) {
        Map<String, Boolean> normalized = new LinkedHashMap<>();
        TeacherPermissions.ALL.forEach(key -> normalized.put(key, Boolean.TRUE.equals(requestedPermissions.get(key))));
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "老师权限保存失败");
        }
    }

    private Map<String, Boolean> parse(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<Map<String, Boolean>>() { });
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
