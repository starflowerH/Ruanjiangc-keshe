package com.demo.miniprogram.config;

import com.demo.miniprogram.util.JwtUtil;
import com.demo.miniprogram.mapper.PromoTeamMapper;
import com.demo.miniprogram.mapper.AdminUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.miniprogram.entity.PromoTeam;
import com.demo.miniprogram.entity.AdminUser;
import com.demo.miniprogram.entity.HighSchoolStudent;
import com.demo.miniprogram.entity.Volunteer;
import com.demo.miniprogram.mapper.HighSchoolStudentMapper;
import com.demo.miniprogram.mapper.VolunteerMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器 - 四层校验：Token有效性 → 路径权限 → 角色校验 → 数据隔离
 *
 * @author demo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final PromoTeamMapper promoTeamMapper;
    private final VolunteerMapper volunteerMapper;
    private final HighSchoolStudentMapper highSchoolStudentMapper;
    private final AdminUserMapper adminUserMapper;

    /** 无需认证的白名单 */
    private static final List<String> WHITE_LIST = List.of(
            "/api/auth/wx-login",
            "/api/auth/admin-login",
            "/api/auth/teacher-register",
            "/api/auth/teacher-login",
            "/api/high-school/",
            "/api/qa/featured",
            "/api/qa/hot",
            "/api/qa/forum",
            "/api/volunteer/by-high-school",
            "/api/university/",
            "/api/comment/list",
            "/api/report/",
            "/logo/",  // 大学校徽静态资源(小程序<image>不带token, 必须放行)
            "/uploads/"  // 用户上传图片静态资源(小程序<image>不带token, 必须放行; 上传接口/api/upload/仍需登录)
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 归一化路径：去除 ../ 段。否则 /api/qa/featured/../admin/stats 这类请求
        // 原始URI前缀命中白名单，Tomcat 归一化后却路由到受保护接口（白名单绕过）
        String rawPath = request.getRequestURI();
        String path = normalizePath(rawPath);

        // ---- 第0层：白名单放行 ----
        if (isWhiteListed(path, request)) {
            chain.doFilter(request, response);
            return;
        }

        // ---- 第1层：Token有效性校验 ----
        String token = extractToken(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            reject(response, 401, "请先登录");
            return;
        }

        // 解析JWT中的用户信息
        String openid = jwtUtil.getSubject(token);
        String userType = jwtUtil.getUserType(token);
        Integer relatedId = jwtUtil.getRelatedId(token);
        String role = jwtUtil.getRole(token);       // admin 角色或微信账号当前身份
        Integer universityId = jwtUtil.getUniversityId(token); // 仅TEACHER有值
        String identityRole = "wechat".equals(userType)
                && ("student".equals(role) || "volunteer".equals(role))
                ? role
                : null;

        // ---- 第1.5层：管理员/老师账号实时状态校验 ----
        // 防止账号被禁用后旧 Token 仍可访问（JWT 最长7天，禁用必须即时生效）
        if ("admin".equals(userType)) {
            AdminUser admin = relatedId != null ? adminUserMapper.selectById(relatedId) : null;
            if (admin == null || admin.getStatus() == null || admin.getStatus() != 1) {
                reject(response, 401, "账号已禁用或不存在，请重新登录");
                return;
            }
        }

        // 兼容没有身份 claim 的旧 Token，但只有在身份唯一时才恢复身份。
        // 高中生 ID 与志愿者 ID 属于不同表，不能仅凭数字 ID 猜测，否则会错配身份。
        if ("wechat".equals(userType) && identityRole == null && relatedId != null) {
            Volunteer volunteer = volunteerMapper.selectById(relatedId);
            HighSchoolStudent student = highSchoolStudentMapper.selectById(relatedId);
            boolean volunteerMatch = volunteer != null && openid.equals(volunteer.getOpenid());
            boolean studentMatch = student != null && openid.equals(student.getOpenid());
            if (volunteerMatch ^ studentMatch) {
                identityRole = volunteerMatch ? "volunteer" : "student";
            } else {
                relatedId = null;
            }
        }
        if ("wechat".equals(userType) && identityRole == null && relatedId == null) {
            List<Volunteer> volunteers = volunteerMapper.selectList(
                    new LambdaQueryWrapper<Volunteer>()
                            .eq(Volunteer::getOpenid, openid)
                            .orderByDesc(Volunteer::getUpdatedAt)
                            .orderByDesc(Volunteer::getId)
                            .last("LIMIT 1")
            );
            Volunteer volunteer = volunteers.size() == 1 ? volunteers.get(0) : null;
            HighSchoolStudent student = highSchoolStudentMapper.selectOne(
                    new LambdaQueryWrapper<HighSchoolStudent>()
                            .eq(HighSchoolStudent::getOpenid, openid)
                            .last("LIMIT 1")
            );
            if (volunteer != null && student == null) {
                relatedId = volunteer.getId();
                identityRole = "volunteer";
            } else if (volunteers.isEmpty() && student != null) {
                relatedId = student.getId();
                identityRole = "student";
            }
        }

        // ---- 第2层：路径级别角色校验 ----
        if (path.startsWith("/api/admin/super/")) {
            // 超级管理员专属接口
            if (!"admin".equals(userType) || !"SUPER_ADMIN".equals(role)) {
                log.warn("权限拒绝: 非超级管理员访问 super 接口 path={}, role={}", path, role);
                reject(response, 403, "仅超级管理员可执行此操作");
                return;
            }
        } else if (path.startsWith("/api/admin/")) {
            // 所有管理员可访问
            if (!"admin".equals(userType)) {
                log.warn("权限拒绝: 非管理员访问管理接口 path={}", path);
                reject(response, 403, "无权限访问");
                return;
            }
        } else if (path.startsWith("/api/teacher/")) {
            // 老师专属接口
            if (!"admin".equals(userType) || !"TEACHER".equals(role)) {
                log.warn("权限拒绝: 非老师访问教师接口 path={}, role={}", path, role);
                reject(response, 403, "仅大学老师可执行此操作");
                return;
            }
        }

        // ---- 第2.5层：QA写操作需要角色绑定 ----
        if ("wechat".equals(userType) && (relatedId == null || identityRole == null)
                && (path.startsWith("/api/qa/ask") || path.startsWith("/api/qa/answer")
                    || path.startsWith("/api/qa/my-questions") || path.startsWith("/api/qa/to-me")
                    || path.startsWith("/api/qa/unanswered-count"))) {
            reject(response, 403, "请先完成身份认证（报名志愿者或绑定高中生身份）后再使用此功能");
            return;
        }

        // ---- 注入用户信息到Request ----
        request.setAttribute("openid", openid);
        request.setAttribute("userType", userType);
        request.setAttribute("requesterRole", role);
        request.setAttribute("requesterId", relatedId);
        request.setAttribute("relatedType", identityRole);

        // 注入提问者类型（用于问答接口区分高中生/大学生）
        if ("wechat".equals(userType) && path.startsWith("/api/qa/")) {
            request.setAttribute("askerType", identityRole != null ? identityRole : "student");
        }

        if (relatedId != null) {
            request.setAttribute("relatedId", relatedId);
        }

        // ---- 第3层：TEACHER 数据隔离 ----
        if ("admin".equals(userType) && "TEACHER".equals(role)) {
            request.setAttribute("adminUniversityId", universityId);
            request.setAttribute("requesterUniversityId", universityId);
            log.debug("教师数据隔离: adminId={}, universityId={}", relatedId, universityId);
        }

        // ---- 第4层：队长身份标记 ----
        if ("wechat".equals(userType) && "volunteer".equals(identityRole) && relatedId != null) {
            // 查询 PromoTeam 表判断是否为队长
            boolean isCaptain = promoTeamMapper.exists(
                    new LambdaQueryWrapper<PromoTeam>()
                            .eq(PromoTeam::getCaptainVolunteerId, relatedId)
            );
            request.setAttribute("isCaptain", isCaptain);
            if (isCaptain) {
                log.debug("队长身份确认: volunteerId={}", relatedId);
            }
        }

        chain.doFilter(request, response);
    }

    /* ========== 私有辅助方法 ========== */

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private boolean isWhiteListed(String path, HttpServletRequest request) {
        // 大学宣传资料：仅公开 GET 读取放行（GET /api/university-content/{universityId}），
        // POST /save 必须走 JWT 解析 + @RequireRole("TEACHER")，防止老师保存资料绕过身份注入导致 403
        if (path.matches("/api/university-content/\\d+/?") && "GET".equals(request.getMethod())) {
            return true;
        }
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    /** 归一化 URI 中的 ../ 段（含 URL 编码变体 %2e/%2f），防止白名单前缀 + 路径穿越绕过鉴权 */
    private String normalizePath(String path) {
        if (path == null) {
            return null;
        }
        // 解码 %2e(.) / %2E 与 %2f(/) / %2F，使 /api/qa/featured/%2e%2e/1 与明文 ../ 同样被处理。
        // 正常业务路径为纯 ASCII，不包含 % 编码，解码不影响合法请求。
        String decoded = path.replaceAll("(?i)%2e", ".").replaceAll("(?i)%2f", "/");
        if (decoded.equals(path) && !decoded.contains("../")) {
            return decoded;
        }
        String result = decoded;
        while (result.contains("/../")) {
            int idx = result.indexOf("/../");
            int prev = result.lastIndexOf('/', idx - 1);
            if (prev < 0) break;
            result = result.substring(0, prev) + result.substring(idx + 3);
        }
        // 末尾 /.. 同样归一化
        while (result.endsWith("/..")) {
            int prev = result.lastIndexOf('/', result.length() - 4);
            if (prev < 0) break;
            result = result.substring(0, prev);
        }
        return result;
    }

    /** 统一拒绝响应 */
    private void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(status);
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + message + "\"}");
    }
}
