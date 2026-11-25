package com.demo.miniprogram.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.demo.miniprogram.common.BusinessException;
import com.demo.miniprogram.common.Result;
import com.demo.miniprogram.dto.AuditDTO;
import com.demo.miniprogram.dto.PageQueryDTO;
import com.demo.miniprogram.dto.ModeratorGrantDTO;
import com.demo.miniprogram.security.RequireRole;
import com.demo.miniprogram.security.RequireTeacherPermission;
import com.demo.miniprogram.security.TeacherPermissions;
import com.demo.miniprogram.service.QAService;
import com.demo.miniprogram.service.TeamService;
import com.demo.miniprogram.service.ForumModerationService;
import com.demo.miniprogram.service.PromotionManagementService;
import com.demo.miniprogram.service.VolunteerService;
import com.demo.miniprogram.service.TeacherPermissionService;
import com.demo.miniprogram.vo.QAVO;
import com.demo.miniprogram.vo.TeamVO;
import com.demo.miniprogram.vo.VolunteerVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.miniprogram.entity.Volunteer;
import com.demo.miniprogram.entity.VolunteerHighSchool;
import com.demo.miniprogram.entity.HighSchool;
import com.demo.miniprogram.mapper.VolunteerHighSchoolMapper;
import com.demo.miniprogram.mapper.HighSchoolMapper;
import cn.hutool.core.collection.CollUtil;
import com.demo.miniprogram.entity.PromoTeam;

/**
 * 大学老师接口 - 仅管理本校志愿者和宣传信息
 *
 * 权限要求：TEACHER 角色
 * 数据隔离：通过 JWT 中的 universityId 实现
 */
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final VolunteerService volunteerService;
    private final QAService qaService;
    private final TeamService teamService;
    private final com.demo.miniprogram.mapper.VolunteerMapper volunteerMapper;
    private final com.demo.miniprogram.mapper.PromoTeamMapper promoTeamMapper;
    private final com.demo.miniprogram.mapper.QuestionAnswerMapper qaMapper;
    private final VolunteerHighSchoolMapper volunteerHighSchoolMapper;
    private final HighSchoolMapper highSchoolMapper;
    private final ForumModerationService forumModerationService;
    private final PromotionManagementService promotionManagementService;
    private final TeacherPermissionService teacherPermissionService;

    /* ==================== 本校志愿者管理 ==================== */

    /**
     * 查看本校志愿者列表
     */
    @GetMapping("/volunteers")
    @RequireRole("TEACHER")
    @RequireTeacherPermission({TeacherPermissions.VOLUNTEER_AUDIT,
            TeacherPermissions.PROMOTION_MANAGE, TeacherPermissions.FORUM_MODERATION})
    public Result<IPage<VolunteerVO>> listMyVolunteers(
            @RequestAttribute("adminUniversityId") Integer universityId,
            @Valid PageQueryDTO queryDTO) {
        return Result.ok(volunteerService.listVolunteers(queryDTO, universityId));
    }

    /**
     * 审核本校志愿者报名
     */
    @PostMapping("/volunteers/audit")
    @RequireRole("TEACHER")
    @RequireTeacherPermission(TeacherPermissions.VOLUNTEER_AUDIT)
    public Result<String> auditMyVolunteer(
            @RequestAttribute("adminUniversityId") Integer universityId,
            @Valid @RequestBody AuditDTO auditDTO) {
        volunteerService.audit(auditDTO, universityId);
        return Result.ok("审核完成");
    }

    /**
     * 授予或撤销本校大学生的论坛审核员权限。
     */
    @PostMapping("/forum/moderators/{volunteerId}")
    @RequireRole("TEACHER")
    @RequireTeacherPermission(TeacherPermissions.FORUM_MODERATION)
    public Result<com.demo.miniprogram.entity.ForumModeratorGrant> setForumModerator(
            @RequestAttribute("requesterId") Integer teacherId,
            @RequestAttribute("adminUniversityId") Integer universityId,
            @PathVariable Integer volunteerId,
            @Valid @RequestBody ModeratorGrantDTO body) {
        return Result.ok(forumModerationService.setModerator(
                volunteerId, body.getActive(), teacherId, universityId));
    }

    /**
     * 授予或撤销本校大学生的宣传资料管理权限。
     */
    @PostMapping("/promotion/managers/{volunteerId}")
    @RequireRole("TEACHER")
    @RequireTeacherPermission(TeacherPermissions.PROMOTION_MANAGE)
    public Result<com.demo.miniprogram.entity.PromotionManagerGrant> setPromotionManager(
            @RequestAttribute("requesterId") Integer teacherId,
            @RequestAttribute("adminUniversityId") Integer universityId,
            @PathVariable Integer volunteerId,
            @Valid @RequestBody ModeratorGrantDTO body) {
        return Result.ok(promotionManagementService.setManager(
                volunteerId, body.getActive(), teacherId, universityId));
    }

    /**
     * 取消本校已通过志愿者的资格
     *
     * POST /api/teacher/volunteers/{id}/cancel
     * Body(可选): {"reason": "取消原因"}
     */
    @PostMapping("/volunteers/{id}/cancel")
    @RequireRole("TEACHER")
    @RequireTeacherPermission(TeacherPermissions.VOLUNTEER_AUDIT)
    public Result<String> cancelMyVolunteer(
            @RequestAttribute("adminUniversityId") Integer universityId,
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        volunteerService.cancel(id, universityId, reason);
        return Result.ok("已取消该志愿者资格");
    }

    /**
     * 撤销取消，恢复本校志愿者资格（取消后2天内可撤销）
     *
     * POST /api/teacher/volunteers/{id}/undo-cancel
     */
    @PostMapping("/volunteers/{id}/undo-cancel")
    @RequireRole("TEACHER")
    @RequireTeacherPermission(TeacherPermissions.VOLUNTEER_AUDIT)
    public Result<String> undoCancelMyVolunteer(
            @RequestAttribute("adminUniversityId") Integer universityId,
            @PathVariable Integer id) {
        volunteerService.undoCancel(id, universityId);
        return Result.ok("已恢复该志愿者资格");
    }

    /* ==================== 本校队伍管理 ==================== */

    /**
     * 查看本校队伍
     */
    @GetMapping("/teams")
    @RequireRole("TEACHER")
    @RequireTeacherPermission(TeacherPermissions.TEAM_MANAGE)
    public Result<List<TeamVO>> getMyTeams(
            @RequestAttribute("adminUniversityId") Integer universityId,
            @RequestParam Integer highSchoolId) {
        // 数据隔离：只返回本校队伍（过滤掉其他大学的队伍，防止查看别校队长手机号等敏感信息）
        List<TeamVO> teams = volunteerService.getTeamsByHighSchool(highSchoolId);
        teams.removeIf(t -> !universityId.equals(t.getUniversityId()));
        return Result.ok(teams);
    }

    /**
     * 触发本校组队
     */
    @PostMapping("/teams/build")
    @RequireRole("TEACHER")
    @RequireTeacherPermission(TeacherPermissions.TEAM_MANAGE)
    public Result<String> buildMyTeams(
            @RequestAttribute("adminUniversityId") Integer universityId) {
        // 只组本校队伍，不影响其他大学
        int count = volunteerService.buildTeams(universityId);
        return Result.ok("组队完成，共创建 " + count + " 支队伍");
    }

    /* ==================== 本校数据统计 ==================== */

    /**
     * 本校数据统计
     */
    @GetMapping("/stats")
    @RequireRole("TEACHER")
    @RequireTeacherPermission({TeacherPermissions.VOLUNTEER_AUDIT, TeacherPermissions.TEAM_MANAGE})
    public Result<Map<String, Object>> getMyStats(
            @RequestAttribute("adminUniversityId") Integer universityId) {
        // 统计本校志愿者总数（已通过审核的）
        Long volunteerCount = volunteerMapper.selectCount(
                new LambdaQueryWrapper<Volunteer>()
                        .eq(Volunteer::getUniversityId, universityId)
                        .eq(Volunteer::getStatus, "APPROVED")
        );
        // 统计本校待审核志愿者数
        Long pendingCount = volunteerMapper.selectCount(
                new LambdaQueryWrapper<Volunteer>()
                        .eq(Volunteer::getUniversityId, universityId)
                        .eq(Volunteer::getStatus, "PENDING")
        );
        // 统计本校队伍数量
        Long teamCount = promoTeamMapper.selectCount(
                new LambdaQueryWrapper<PromoTeam>()
                        .eq(PromoTeam::getUniversityId, universityId)
        );
        // 统计本校相关问答数（提问者或回答者属于本校志愿者）
        Long qaCount = qaMapper.selectCount(
                new LambdaQueryWrapper<com.demo.miniprogram.entity.QuestionAnswer>()
                        .and(w -> w
                                .inSql(com.demo.miniprogram.entity.QuestionAnswer::getAskerVolunteerId,
                                        "SELECT id FROM volunteer WHERE university_id = " + universityId)
                                .or()
                                .inSql(com.demo.miniprogram.entity.QuestionAnswer::getTargetVolunteerId,
                                        "SELECT id FROM volunteer WHERE university_id = " + universityId))
        );
        Map<String, Object> stats = Map.of(
                "volunteerCount", volunteerCount,
                "pendingCount", pendingCount,
                "teamCount", teamCount,
                "qaCount", qaCount
        );
        return Result.ok(stats);
    }

    /* ==================== 老师权限 ==================== */

    /**
     * 获取当前老师的业务权限明细（实时查库）。
     *
     * GET /api/teacher/permissions/me
     * 仅需 TEACHER 角色（不加 @RequireTeacherPermission，否则权限被撤销的老师
     * 无法查询自身权限，导致前端无法刷新菜单显隐）。
     */
    @GetMapping("/permissions/me")
    @RequireRole("TEACHER")
    public Result<Map<String, Boolean>> getMyPermissions(
            @RequestAttribute("requesterId") Integer teacherId) {
        return Result.ok(teacherPermissionService.getPermissions(teacherId));
    }

    /* ==================== 组队状态查看与自动组队 ==================== */

    /**
     * 查看本校所有组队状态（按高中分组）
     * GET /api/teacher/team-status
     */
    @GetMapping("/team-status")
    @RequireRole("TEACHER")
    @RequireTeacherPermission(TeacherPermissions.TEAM_MANAGE)
    public Result<List<Map<String, Object>>> getTeamStatus(
            @RequestAttribute("adminUniversityId") Integer universityId) {
        return Result.ok(teamService.getTeamStatusByUniversity(universityId));
    }

    /**
     * 一键组队：将本校所有未组队的已审核志愿者自动分组
     * POST /api/teacher/teams/auto-build
     */
    @PostMapping("/teams/auto-build")
    @RequireRole("TEACHER")
    @RequireTeacherPermission(TeacherPermissions.TEAM_MANAGE)
    public Result<Map<String, Object>> autoBuildTeams(
            @RequestAttribute("adminUniversityId") Integer universityId) {
        // 只组本校队伍，不影响其他大学
        int count = volunteerService.buildTeams(universityId);
        // 获取更新后的组队状态
        List<Map<String, Object>> status = teamService.getTeamStatusByUniversity(universityId);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("teamCount", count);
        result.put("status", status);
        return Result.ok("组队完成，共创建 " + count + " 支队伍", result);
    }

    /**
     * 老师手动创建自定义队伍
     *
     * POST /api/teacher/teams/create
     * Body: {"teamName": "自定义队", "highSchoolId": 1, "captainVolunteerId": 2(可选)}
     */
    @PostMapping("/teams/create")
    @RequireRole("TEACHER")
    @RequireTeacherPermission(TeacherPermissions.TEAM_MANAGE)
    public Result<TeamVO> createTeacherTeam(
            @RequestAttribute("adminUniversityId") Integer universityId,
            @RequestBody Map<String, Object> body) {
        String teamName = (String) body.get("teamName");
        if (teamName == null || teamName.isBlank()) {
            throw new BusinessException("请输入队伍名称");
        }
        Integer highSchoolId = (Integer) body.get("highSchoolId");
        Integer captainVolunteerId = body.get("captainVolunteerId") != null
                ? ((Number) body.get("captainVolunteerId")).intValue() : null;
        TeamVO team = teamService.createTeacherTeam(universityId, teamName, highSchoolId, captainVolunteerId);
        return Result.ok("队伍创建成功", team);
    }

    /**
     * 老师手动指定志愿者加入/移出队伍
     *
     * POST /api/teacher/teams/{id}/members
     * Body: {"volunteerId": 2, "action": "ADD"|"REMOVE"}
     */
    @PostMapping("/teams/{id}/members")
    @RequireRole("TEACHER")
    @RequireTeacherPermission(TeacherPermissions.TEAM_MANAGE)
    public Result<String> modifyTeamMember(
            @RequestAttribute("adminUniversityId") Integer universityId,
            @PathVariable Integer id,
            @RequestBody Map<String, Object> body) {
        Integer volunteerId = body.get("volunteerId") != null
                ? ((Number) body.get("volunteerId")).intValue() : null;
        String action = (String) body.get("action");
        if (volunteerId == null) {
            throw new BusinessException("请选择志愿者");
        }
        if ("ADD".equals(action)) {
            teamService.addMemberToTeam(universityId, id, volunteerId);
            return Result.ok("已加入队伍");
        } else if ("REMOVE".equals(action)) {
            teamService.removeMemberFromTeam(universityId, id, volunteerId);
            return Result.ok("已移出队伍");
        }
        throw new BusinessException("无效的操作类型");
    }

    /**
     * 老师指定/更换队长
     *
     * POST /api/teacher/teams/{id}/captain
     * Body: {"volunteerId": 2}
     */
    @PostMapping("/teams/{id}/captain")
    @RequireRole("TEACHER")
    @RequireTeacherPermission(TeacherPermissions.TEAM_MANAGE)
    public Result<String> assignCaptain(
            @RequestAttribute("adminUniversityId") Integer universityId,
            @PathVariable Integer id,
            @RequestBody Map<String, Object> body) {
        Integer volunteerId = body.get("volunteerId") != null
                ? ((Number) body.get("volunteerId")).intValue() : null;
        teamService.assignCaptain(universityId, id, volunteerId);
        return Result.ok("队长已指定");
    }

    /**
     * 老师解散队伍
     *
     * DELETE /api/teacher/teams/{id}
     */
    @DeleteMapping("/teams/{id}")
    @RequireRole("TEACHER")
    @RequireTeacherPermission(TeacherPermissions.TEAM_MANAGE)
    public Result<String> deleteTeam(
            @RequestAttribute("adminUniversityId") Integer universityId,
            @PathVariable Integer id) {
        teamService.deleteTeam(universityId, id);
        return Result.ok("队伍已解散");
    }

    /**
     * 队伍详情（老师端）
     *
     * GET /api/teacher/teams/{id}
     */
    @GetMapping("/teams/{id}")
    @RequireRole("TEACHER")
    @RequireTeacherPermission(TeacherPermissions.TEAM_MANAGE)
    public Result<TeamVO> getTeamDetail(
            @RequestAttribute("adminUniversityId") Integer universityId,
            @PathVariable Integer id) {
        return Result.ok(teamService.getTeamDetail(universityId, id));
    }

    /**
     * 修改队伍目标高中（成员目标高中同步）
     *
     * PUT /api/teacher/teams/{id}/high-school
     * Body: {"highSchoolId": 2}
     */
    @PutMapping("/teams/{id}/high-school")
    @RequireRole("TEACHER")
    @RequireTeacherPermission(TeacherPermissions.TEAM_MANAGE)
    public Result<String> updateTeamHighSchool(
            @RequestAttribute("adminUniversityId") Integer universityId,
            @PathVariable Integer id,
            @RequestBody Map<String, Object> body) {
        Integer highSchoolId = body.get("highSchoolId") != null
                ? ((Number) body.get("highSchoolId")).intValue() : null;
        teamService.updateTeamHighSchool(universityId, id, highSchoolId);
        return Result.ok("目标高中已修改");
    }

    /**
     * 查看本校相关的待审核问答
     * 通过目标志愿者所属大学过滤，保证老师只能看到本校学生的问题（数据隔离）
     */
    @GetMapping("/qa/pending")
    @RequireRole("TEACHER")
    @RequireTeacherPermission(TeacherPermissions.VOLUNTEER_AUDIT)
    public Result<IPage<QAVO>> getMyPendingQA(
            @RequestAttribute("adminUniversityId") Integer universityId,
            @Valid PageQueryDTO queryDTO) {
        return Result.ok(qaService.listQuestions(queryDTO, null, null, "PENDING", null, null, universityId));
    }
    /* ==================== 志愿者高中分配管理 ==================== */

    /**
     * 为志愿者分配/更新目标高中列表
     *
     * POST /api/teacher/volunteers/{id}/high-schools
     * Body: [1, 2, 3]  (高中ID列表)
     */
    @PostMapping("/volunteers/{id}/high-schools")
    @RequireRole("TEACHER")
    @RequireTeacherPermission(TeacherPermissions.VOLUNTEER_AUDIT)
    public Result<String> assignHighSchools(
            @RequestAttribute("adminUniversityId") Integer universityId,
            @PathVariable Integer id,
            @RequestBody List<Integer> highSchoolIds) {
        // 校验该志愿者属于本校
        Volunteer volunteer = volunteerMapper.selectById(id);
        if (volunteer == null || !universityId.equals(volunteer.getUniversityId())) {
            return Result.fail("无权操作该志愿者");
        }
        volunteerService.assignHighSchools(id, highSchoolIds);
        return Result.ok("高中分配成功");
    }

    /**
     * 查询志愿者绑定的高中列表
     *
     * GET /api/teacher/volunteers/{id}/high-schools
     */
    @GetMapping("/volunteers/{id}/high-schools")
    @RequireRole("TEACHER")
    @RequireTeacherPermission(TeacherPermissions.VOLUNTEER_AUDIT)
    public Result<List<HighSchool>> getVolunteerHighSchools(
            @RequestAttribute("adminUniversityId") Integer universityId,
            @PathVariable Integer id) {
        Volunteer volunteer = volunteerMapper.selectById(id);
        if (volunteer == null || !universityId.equals(volunteer.getUniversityId())) {
            return Result.fail("无权操作该志愿者");
        }
        List<Integer> ids = volunteerService.getVolunteerHighSchoolIds(id);
        if (CollUtil.isEmpty(ids)) {
            return Result.ok(List.of());
        }
        List<HighSchool> schools = highSchoolMapper.selectBatchIds(ids);
        return Result.ok(schools);
    }

    /**
     * 删除志愿者的单个高中绑定
     *
     * DELETE /api/teacher/volunteers/{id}/high-schools/{highSchoolId}
     */
    @DeleteMapping("/volunteers/{id}/high-schools/{highSchoolId}")
    @RequireRole("TEACHER")
    @RequireTeacherPermission(TeacherPermissions.VOLUNTEER_AUDIT)
    public Result<String> removeHighSchool(
            @RequestAttribute("adminUniversityId") Integer universityId,
            @PathVariable Integer id,
            @PathVariable Integer highSchoolId) {
        Volunteer volunteer = volunteerMapper.selectById(id);
        if (volunteer == null || !universityId.equals(volunteer.getUniversityId())) {
            return Result.fail("无权操作该志愿者");
        }
        volunteerService.removeVolunteerHighSchool(id, highSchoolId);
        return Result.ok("绑定已删除");
    }
}
