package com.demo.miniprogram.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.miniprogram.common.Result;
import com.demo.miniprogram.dto.AuditDTO;
import com.demo.miniprogram.dto.PageQueryDTO;
import com.demo.miniprogram.dto.TeacherPermissionsDTO;
import com.demo.miniprogram.entity.*;
import com.demo.miniprogram.mapper.*;
import com.demo.miniprogram.security.RequireRole;
import com.demo.miniprogram.service.QAService;
import com.demo.miniprogram.service.TeamService;
import com.demo.miniprogram.service.PromotionManagementService;
import com.demo.miniprogram.service.TeacherPermissionService;
import com.demo.miniprogram.service.VolunteerService;
import com.demo.miniprogram.vo.QAVO;
import com.demo.miniprogram.vo.TeamVO;
import com.demo.miniprogram.vo.VolunteerVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.hutool.core.collection.CollUtil;

/**
 * 后台管理接口 - 审核、组队、问答管理
 *
 * 权限说明：
 *   - SUPER_ADMIN：所有权限
 *   - NORMAL_ADMIN：仅查看数据（默认），由 SUPER_ADMIN 动态分配更多权限
 *   - TEACHER：使用 /api/teacher/ 接口管理本校数据
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final VolunteerService volunteerService;
    private final QAService qaService;
    private final TeamService teamService;
    private final HighSchoolMapper highSchoolMapper;
    private final VolunteerMapper volunteerMapper;
    private final PromoTeamMapper promoTeamMapper;
    private final QuestionAnswerMapper qaMapper;
    private final HighSchoolStudentMapper studentMapper;
    private final ForumCommentMapper commentMapper;
    private final AdminUserMapper adminUserMapper;
    private final TeacherPermissionService teacherPermissionService;
    private final PromotionManagementService promotionManagementService;

    /* ==================== 数据看板（所有管理员可访问） ==================== */

    /**
     * 系统数据统计看板（实时查库，非写死数据）
     */
    @GetMapping("/stats")
    @RequireRole({"SUPER_ADMIN", "NORMAL_ADMIN"})
    public Result<Map<String, Object>> getStats(
            @RequestAttribute(value = "adminUniversityId", required = false) Integer adminUniversityId) {
        // 注意：TEACHER 不能访问本接口（全平台统计），老师端统计走 /api/teacher/stats（本校范围）
        Map<String, Object> stats = new LinkedHashMap<>();
        // 志愿者总数只统计已通过审核的正式志愿者（待审核/被拒/被顶替的报名记录不计入）
        stats.put("volunteerCount", volunteerMapper.selectCount(
                new LambdaQueryWrapper<Volunteer>().eq(Volunteer::getStatus, "APPROVED")));
        stats.put("pendingCount", volunteerMapper.selectCount(
                new LambdaQueryWrapper<Volunteer>().eq(Volunteer::getStatus, "PENDING")));
        stats.put("teamCount", promoTeamMapper.selectCount(null));
        // 问答总数只统计已通过审核的帖子（待审核帖子不对外展示）
        stats.put("qaCount", qaMapper.selectCount(
                new LambdaQueryWrapper<QuestionAnswer>().eq(QuestionAnswer::getAuditStatus, "APPROVED")));
        stats.put("studentCount", studentMapper.selectCount(null));
        stats.put("commentCount", commentMapper.selectCount(null));
        return Result.ok(stats);
    }

    /* ==================== 报名管理（仅超级管理员） ==================== */

    /**
     * 分页查询志愿者报名列表
     */
    @GetMapping("/volunteers")
    @RequireRole("SUPER_ADMIN")
    public Result<IPage<VolunteerVO>> listVolunteers(
            @RequestAttribute(value = "adminUniversityId", required = false) Integer adminUniversityId,
            @Valid PageQueryDTO queryDTO) {
        return Result.ok(volunteerService.listVolunteers(queryDTO, adminUniversityId));
    }

    /**
     * 审核志愿者报名
     */
    @PostMapping("/volunteers/audit")
    @RequireRole("SUPER_ADMIN")
    public Result<String> auditVolunteer(
            @RequestAttribute(value = "adminUniversityId", required = false) Integer adminUniversityId,
            @Valid @RequestBody AuditDTO auditDTO) {
        volunteerService.audit(auditDTO, adminUniversityId);
        return Result.ok("审核完成");
    }

    /**
     * 取消已通过志愿者的资格（全平台）
     *
     * POST /api/admin/volunteers/{id}/cancel
     * Body(可选): {"reason": "取消原因"}
     */
    @PostMapping("/volunteers/{id}/cancel")
    @RequireRole("SUPER_ADMIN")
    public Result<String> cancelVolunteer(
            @RequestAttribute(value = "adminUniversityId", required = false) Integer adminUniversityId,
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        volunteerService.cancel(id, adminUniversityId, reason);
        return Result.ok("已取消该志愿者资格");
    }

    /**
     * 撤销取消，恢复志愿者资格（取消后2天内可撤销）
     *
     * POST /api/admin/volunteers/{id}/undo-cancel
     */
    @PostMapping("/volunteers/{id}/undo-cancel")
    @RequireRole("SUPER_ADMIN")
    public Result<String> undoCancelVolunteer(
            @RequestAttribute(value = "adminUniversityId", required = false) Integer adminUniversityId,
            @PathVariable Integer id) {
        volunteerService.undoCancel(id, adminUniversityId);
        return Result.ok("已恢复该志愿者资格");
    }

    /* ==================== 志愿者高中分配（仅超级管理员） ==================== */

    @PostMapping("/promotion/managers/{volunteerId}")
    @RequireRole("SUPER_ADMIN")
    public Result<PromotionManagerGrant> setPromotionManager(
            @PathVariable Integer volunteerId,
            @RequestAttribute("requesterId") Integer adminId,
            @Valid @RequestBody com.demo.miniprogram.dto.ModeratorGrantDTO body) {
        return Result.ok(promotionManagementService.setManager(
                volunteerId, body.getActive(), adminId));
    }

    @GetMapping("/volunteers/{id}/high-schools")
    @RequireRole("SUPER_ADMIN")
    public Result<List<HighSchool>> getVolunteerHighSchools(@PathVariable Integer id) {
        List<Integer> ids = volunteerService.getVolunteerHighSchoolIds(id);
        if (CollUtil.isEmpty(ids)) {
            return Result.ok(List.of());
        }
        return Result.ok(highSchoolMapper.selectBatchIds(ids));
    }

    @PostMapping("/volunteers/{id}/high-schools")
    @RequireRole("SUPER_ADMIN")
    public Result<String> assignVolunteerHighSchools(
            @PathVariable Integer id,
            @RequestBody List<Integer> highSchoolIds) {
        volunteerService.assignHighSchools(id, highSchoolIds);
        return Result.ok("高中分配成功");
    }

    @DeleteMapping("/volunteers/{id}/high-schools/{highSchoolId}")
    @RequireRole("SUPER_ADMIN")
    public Result<String> removeVolunteerHighSchool(
            @PathVariable Integer id,
            @PathVariable Integer highSchoolId) {
        volunteerService.removeVolunteerHighSchool(id, highSchoolId);
        return Result.ok("绑定已删除");
    }

    /* ==================== 队伍管理（仅超级管理员） ==================== */

    /**
     * 触发自动组队
     */
    @PostMapping("/teams/build")
    @RequireRole("SUPER_ADMIN")
    public Result<String> buildTeams() {
        // 管理员端触发全平台组队（universityId=null 表示所有大学）
        int count = volunteerService.buildTeams(null);
        return Result.ok("组队完成，共创建 " + count + " 支队伍");
    }

    /**
     * 查看某个高中的所有宣传队伍（所有管理员可查看）
     */
    @GetMapping("/teams")
    @RequireRole({"SUPER_ADMIN", "NORMAL_ADMIN"})
    public Result<List<TeamVO>> getTeams(@RequestParam Integer highSchoolId) {
        return Result.ok(volunteerService.getTeamsByHighSchool(highSchoolId));
    }

    /**
     * 查看所有宣传队伍（超级管理员查看全部队伍列表）
     *
     * GET /api/admin/teams/all
     */
    @GetMapping("/teams/all")
    @RequireRole({"SUPER_ADMIN", "NORMAL_ADMIN"})
    public Result<List<TeamVO>> getAllTeams() {
        return Result.ok(teamService.getAllTeams());
    }

    /**
     * 分页查询所有高中生列表（管理员查看高中生信息）
     *
     * GET /api/admin/students?page=1&size=20&keyword=张三&highSchoolId=1
     */
    @GetMapping("/students")
    @RequireRole({"SUPER_ADMIN", "NORMAL_ADMIN"})
    public Result<IPage<Map<String, Object>>> listStudents(
            @RequestParam(required = false) Integer highSchoolId,
            @Valid PageQueryDTO queryDTO) {
        Page<HighSchoolStudent> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        LambdaQueryWrapper<HighSchoolStudent> wrapper = new LambdaQueryWrapper<HighSchoolStudent>()
                .orderByDesc(HighSchoolStudent::getCreatedAt);
        if (highSchoolId != null) {
            wrapper.eq(HighSchoolStudent::getHighSchoolId, highSchoolId);
        }
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isBlank()) {
            String kw = queryDTO.getKeyword().trim();
            wrapper.and(w -> w.like(HighSchoolStudent::getName, kw)
                    .or().like(HighSchoolStudent::getNickname, kw)
                    .or().like(HighSchoolStudent::getPhone, kw));
        }
        IPage<HighSchoolStudent> result = studentMapper.selectPage(page, wrapper);

        // 组装返回（填充高中名）
        List<Map<String, Object>> records = result.getRecords().stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("name", s.getName());
            m.put("nickname", s.getNickname());
            m.put("avatarUrl", s.getAvatarUrl());
            m.put("phone", s.getPhone());
            m.put("highSchoolId", s.getHighSchoolId());
            m.put("createdAt", s.getCreatedAt());
            HighSchool hs = s.getHighSchoolId() != null ? highSchoolMapper.selectById(s.getHighSchoolId()) : null;
            m.put("highSchoolName", hs != null ? hs.getName() : "");
            return m;
        }).collect(java.util.stream.Collectors.toList());

        // 返回分页结果（records 替换为组装后的 Map 列表）
        @SuppressWarnings("unchecked")
        IPage<Map<String, Object>> res = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        res.setRecords(records);
        return Result.ok(res);
    }

    /* ==================== 问答管理（仅超级管理员） ==================== */

    /**
     * 查看待审核的问答列表
     */
    @GetMapping("/qa/pending")
    @RequireRole("SUPER_ADMIN")
    public Result<IPage<QAVO>> pendingQuestions(@Valid PageQueryDTO queryDTO) {
        return Result.ok(qaService.listQuestions(queryDTO, null, null, "PENDING", null));
    }

    /**
     * 审核问答内容
     */
    @PostMapping("/qa/audit")
    @RequireRole("SUPER_ADMIN")
    public Result<String> auditQA(@Valid @RequestBody AuditDTO auditDTO) {
        qaService.auditQuestion(auditDTO);
        return Result.ok("审核完成");
    }

    /**
     * 切换精选问答的公开/隐藏状态
     */
    @PutMapping("/qa/{id}/toggle-public")
    @RequireRole("SUPER_ADMIN")
    public Result<String> togglePublic(@PathVariable Integer id, @RequestParam Integer isPublic) {
        qaService.togglePublic(id, isPublic);
        return Result.ok(isPublic == 1 ? "已设为公开" : "已设为隐藏");
    }

    /* ==================== 队伍角色审核（仅超级管理员） ==================== */

    /**
     * 获取待审核的队长/副队长申请列表
     */
    @GetMapping("/team-applications")
    @RequireRole("SUPER_ADMIN")
    public Result<List<Map<String, Object>>> getTeamApplications() {
        return Result.ok(teamService.getPendingApplications());
    }

    /**
     * 审核队长/副队长申请
     *
     * POST /api/admin/team-applications/{id}/audit
     * Body: {"action": "APPROVED"} 或 {"action": "REJECTED"}
     */
    @PostMapping("/team-applications/{id}/audit")
    @RequireRole("SUPER_ADMIN")
    public Result<String> auditTeamApplication(
            @PathVariable Integer id,
            @RequestAttribute("requesterId") Integer adminId,
            @RequestBody Map<String, String> body) {
        String action = body.get("action");
        if (!"APPROVED".equals(action) && !"REJECTED".equals(action)) {
            return Result.fail("审核动作无效");
        }
        teamService.auditRoleApplication(id, adminId, action);
        return Result.ok("审核完成");
    }

    /* ==================== 用户管理（仅超级管理员） ==================== */

    /**
     * 获取老师账号列表（含 status，用于超管审核启用待启用老师）
     *
     * GET /api/admin/teachers
     */
    @GetMapping("/teachers")
    @RequireRole("SUPER_ADMIN")
    public Result<List<AdminUser>> listTeachers() {
        List<AdminUser> teachers = adminUserMapper.selectList(
                new LambdaQueryWrapper<AdminUser>()
                        .eq(AdminUser::getRole, "TEACHER")
                        .orderByAsc(AdminUser::getStatus)
                        .orderByDesc(AdminUser::getCreatedAt));
        // 不下发密码哈希
        teachers.forEach(t -> t.setPasswordHash(null));
        return Result.ok(teachers);
    }

    /**
     * 启用/禁用老师账号
     *
     * POST /api/admin/teachers/{id}/enable
     * Body: {"enabled": true/false}，true 置 status=1，false 置 status=0
     */
    @PostMapping("/teachers/{id}/enable")
    @RequireRole("SUPER_ADMIN")
    public Result<String> enableTeacher(@PathVariable Integer id,
                                        @RequestBody Map<String, Boolean> body) {
        Boolean enabled = body != null ? body.get("enabled") : null;
        if (enabled == null) {
            return Result.fail("缺少 enabled 参数");
        }
        AdminUser teacher = adminUserMapper.selectById(id);
        if (teacher == null || !"TEACHER".equals(teacher.getRole())) {
            return Result.fail("老师账号不存在");
        }
        teacher.setStatus(Boolean.TRUE.equals(enabled) ? 1 : 0);
        adminUserMapper.updateById(teacher);
        return Result.ok(Boolean.TRUE.equals(enabled) ? "已启用该老师账号" : "已禁用该老师账号");
    }

    /**
     * 获取管理员用户列表
     */
    @GetMapping("/users")
    @RequireRole("SUPER_ADMIN")
    public Result<Object> listAdminUsers() {
        // TODO: 实现管理员用户列表
        return Result.ok("用户管理接口（待实现）");
    }

    /**
     * 分配管理员权限
     */
    @PutMapping("/teachers/{id}/permissions")
    @RequireRole("SUPER_ADMIN")
    public Result<String> updateTeacherPermissions(@PathVariable Integer id,
                                                   @Valid @RequestBody TeacherPermissionsDTO body) {
        AdminUser teacher = adminUserMapper.selectById(id);
        if (teacher == null || !"TEACHER".equals(teacher.getRole())) {
            return Result.fail("老师账号不存在");
        }
        teacher.setPermissions(teacherPermissionService.serialize(body.toMap()));
        adminUserMapper.updateById(teacher);
        return Result.ok("老师权限已更新");
    }
}
