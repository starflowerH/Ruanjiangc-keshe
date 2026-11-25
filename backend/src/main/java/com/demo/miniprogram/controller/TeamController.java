package com.demo.miniprogram.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.miniprogram.common.BusinessException;
import com.demo.miniprogram.common.Result;
import com.demo.miniprogram.entity.TeamRoleApplication;
import com.demo.miniprogram.entity.Volunteer;
import com.demo.miniprogram.mapper.VolunteerMapper;
import com.demo.miniprogram.service.TeamService;
import com.demo.miniprogram.vo.MemberDetailVO;
import com.demo.miniprogram.vo.MemberVO;
import com.demo.miniprogram.vo.TeamVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 队伍接口 - 自由组队、队伍号管理、队长/副队长申请
 *
 * @author demo
 */
@RestController
@RequestMapping("/api/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final VolunteerMapper volunteerMapper;

    /**
     * 创建队伍
     *
     * POST /api/team/create
     * Body: {"teamName": "我的队伍"}
     */
    @PostMapping("/create")
    public Result<TeamVO> createTeam(
            @RequestAttribute(value = "relatedId", required = false) Integer volunteerId,
            @RequestAttribute(value = "openid", required = false) String openid,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @RequestBody Map<String, String> body) {
        volunteerId = resolveVolunteerId(volunteerId, openid, relatedType);
        if (volunteerId == null) {
            throw new BusinessException("请先完成志愿者报名");
        }
        String teamName = body.get("teamName");
        String contactPhone = body.get("captainContactPhone");
        String contactWechat = body.get("captainContactWechat");
        if (teamName == null || teamName.isBlank()) {
            throw new BusinessException("请输入队伍名称");
        }
        TeamVO team = teamService.createTeam(volunteerId, teamName, contactPhone, contactWechat);
        return Result.ok("队伍创建成功", team);
    }

    /**
     * 通过队伍号加入队伍
     *
     * POST /api/team/join
     * Body: {"teamCode": "AB123"}
     */
    @PostMapping("/join")
    public Result<TeamVO> joinTeam(
            @RequestAttribute(value = "relatedId", required = false) Integer volunteerId,
            @RequestAttribute(value = "openid", required = false) String openid,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @RequestBody Map<String, String> body) {
        volunteerId = resolveVolunteerId(volunteerId, openid, relatedType);
        if (volunteerId == null) {
            throw new BusinessException("请先完成志愿者报名");
        }
        String teamCode = body.get("teamCode");
        if (teamCode == null || teamCode.isBlank()) {
            throw new BusinessException("请输入队伍号");
        }
        TeamVO team = teamService.joinTeam(volunteerId, teamCode);
        return Result.ok("加入队伍成功", team);
    }

    /**
     * 退出队伍
     *
     * POST /api/team/leave
     */
    @PostMapping("/leave")
    public Result<String> leaveTeam(
            @RequestAttribute(value = "relatedId", required = false) Integer volunteerId,
            @RequestAttribute(value = "openid", required = false) String openid,
            @RequestAttribute(value = "relatedType", required = false) String relatedType) {
        volunteerId = resolveVolunteerId(volunteerId, openid, relatedType);
        if (volunteerId == null) {
            throw new BusinessException("请先完成志愿者报名");
        }
        teamService.leaveTeam(volunteerId);
        return Result.ok("已退出队伍");
    }

    /**
     * 获取我的队伍信息
     *
     * GET /api/team/my
     */
    @GetMapping("/my")
    public Result<TeamVO> getMyTeam(
            @RequestAttribute(value = "relatedId", required = false) Integer volunteerId,
            @RequestAttribute(value = "openid", required = false) String openid,
            @RequestAttribute(value = "relatedType", required = false) String relatedType) {
        volunteerId = resolveVolunteerId(volunteerId, openid, relatedType);
        if (volunteerId == null) {
            return Result.ok(null);
        }
        TeamVO team = teamService.getMyTeam(volunteerId);
        return Result.ok(team);
    }

    /**
     * 获取队伍成员列表
     *
     * 权限：仅该队伍的有效成员可查看本队成员列表
     * GET /api/team/{id}/members
     */
    @GetMapping("/{id}/members")
    public Result<List<MemberVO>> getTeamMembers(
            @RequestAttribute(value = "relatedId", required = false) Integer volunteerId,
            @RequestAttribute(value = "openid", required = false) String openid,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @PathVariable Integer id) {
        volunteerId = resolveVolunteerId(volunteerId, openid, relatedType);
        if (volunteerId == null) {
            throw new BusinessException("请先完成志愿者报名");
        }
        return Result.ok(teamService.getTeamMembers(volunteerId, id));
    }

    /**
     * 获取队伍成员详情（点击成员头像查看报名信息，仅同队可见）
     *
     * GET /api/team/member/{targetVolunteerId}
     */
    @GetMapping("/member/{targetVolunteerId}")
    public Result<MemberDetailVO> getMemberDetail(
            @RequestAttribute(value = "relatedId", required = false) Integer volunteerId,
            @RequestAttribute(value = "openid", required = false) String openid,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @PathVariable Integer targetVolunteerId) {
        volunteerId = resolveVolunteerId(volunteerId, openid, relatedType);
        if (volunteerId == null) {
            throw new BusinessException("请先完成志愿者报名");
        }
        return Result.ok(teamService.getMemberDetail(volunteerId, targetVolunteerId));
    }

    /**
     * 更新队伍简介（本队成员均可编辑）
     *
     * POST /api/team/update-desc
     * Body: {"teamId": 1, "description": "队伍简介内容"}
     */
    @PostMapping("/update-desc")
    public Result<TeamVO> updateDescription(
            @RequestAttribute(value = "relatedId", required = false) Integer volunteerId,
            @RequestAttribute(value = "openid", required = false) String openid,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @RequestBody Map<String, Object> body) {
        volunteerId = resolveVolunteerId(volunteerId, openid, relatedType);
        if (volunteerId == null) {
            throw new BusinessException("请先完成志愿者报名");
        }
        Object teamIdObj = body.get("teamId");
        if (teamIdObj == null) {
            throw new BusinessException("队伍ID不能为空");
        }
        Integer teamId = ((Number) teamIdObj).intValue();
        String description = (String) body.get("description");
        TeamVO team = teamService.updateDescription(volunteerId, teamId, description);
        return Result.ok("队伍简介已更新", team);
    }

    /** 队长修改目标高中，普通成员无此权限。 */
    @PostMapping("/update-target")
    public Result<TeamVO> updateTarget(
            @RequestAttribute(value = "relatedId", required = false) Integer volunteerId,
            @RequestAttribute(value = "openid", required = false) String openid,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @RequestBody Map<String, Object> body) {
        volunteerId = resolveVolunteerId(volunteerId, openid, relatedType);
        if (volunteerId == null) throw new BusinessException("请先完成志愿者报名");
        Object teamIdObj = body.get("teamId");
        Object highSchoolIdObj = body.get("highSchoolId");
        if (!(teamIdObj instanceof Number) || !(highSchoolIdObj instanceof Number)) {
            throw new BusinessException("队伍和目标高中不能为空");
        }
        return Result.ok("目标高中已更新", teamService.updateTargetForCaptain(
                volunteerId, ((Number) teamIdObj).intValue(), ((Number) highSchoolIdObj).intValue()));
    }

    /** 队长维护队长通讯录信息，普通成员无此权限。 */
    @PostMapping("/update-contact")
    public Result<TeamVO> updateContact(
            @RequestAttribute(value = "relatedId", required = false) Integer volunteerId,
            @RequestAttribute(value = "openid", required = false) String openid,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @RequestBody Map<String, Object> body) {
        volunteerId = resolveVolunteerId(volunteerId, openid, relatedType);
        if (volunteerId == null) throw new BusinessException("请先完成志愿者报名");
        Object teamIdObj = body.get("teamId");
        if (!(teamIdObj instanceof Number)) throw new BusinessException("队伍ID不能为空");
        return Result.ok("通讯方式已更新", teamService.updateCaptainContact(
                volunteerId, ((Number) teamIdObj).intValue(),
                body.get("captainContactPhone") == null ? null : String.valueOf(body.get("captainContactPhone")),
                body.get("captainContactWechat") == null ? null : String.valueOf(body.get("captainContactWechat"))));
    }

    /**
     * 申请成为队长或转让队长
     *
     * POST /api/team/apply-role
     * Body: {"teamId": 1, "applyRole": "CAPTAIN", "reason": "我有经验"}
     */
    @PostMapping("/apply-role")
    public Result<String> applyForRole(
            @RequestAttribute(value = "relatedId", required = false) Integer volunteerId,
            @RequestAttribute(value = "openid", required = false) String openid,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @RequestBody Map<String, Object> body) {
        volunteerId = resolveVolunteerId(volunteerId, openid, relatedType);
        if (volunteerId == null) {
            throw new BusinessException("请先完成志愿者报名");
        }
        Integer teamId = (Integer) body.get("teamId");
        String applyRole = (String) body.get("applyRole");
        String reason = (String) body.get("reason");

        if (teamId == null) {
            throw new BusinessException("队伍ID不能为空");
        }
        if (!"CAPTAIN".equals(applyRole) && !"CAPTAIN_TRANSFER".equals(applyRole)) {
            throw new BusinessException("仅支持申请队长或队长转让");
        }

        teamService.applyForRole(volunteerId, teamId, applyRole, reason);
        return Result.ok("申请已提交，等待管理员审核");
    }

    /**
     * 队长转让队长身份给其他成员
     *
     * POST /api/team/transfer-captain
     * Body: {"teamId": 1, "targetVolunteerId": 3, "reason": "工作调动"}
     */
    @PostMapping("/transfer-captain")
    public Result<String> transferCaptain(
            @RequestAttribute(value = "relatedId", required = false) Integer volunteerId,
            @RequestAttribute(value = "openid", required = false) String openid,
            @RequestAttribute(value = "relatedType", required = false) String relatedType,
            @RequestBody Map<String, Object> body) {
        volunteerId = resolveVolunteerId(volunteerId, openid, relatedType);
        if (volunteerId == null) {
            throw new BusinessException("请先完成志愿者报名");
        }
        Integer teamId = (Integer) body.get("teamId");
        Integer targetVolunteerId = (Integer) body.get("targetVolunteerId");
        String reason = (String) body.get("reason");

        if (teamId == null) {
            throw new BusinessException("队伍ID不能为空");
        }
        if (targetVolunteerId == null) {
            throw new BusinessException("请选择转让目标");
        }

        teamService.transferCaptain(volunteerId, teamId, targetVolunteerId, reason);
        return Result.ok("转让申请已提交，等待管理员审核");
    }

    /**
     * 获取我的申请记录
     *
     * GET /api/team/my-applications
     */
    @GetMapping("/my-applications")
    public Result<List<TeamRoleApplication>> getMyApplications(
            @RequestAttribute(value = "relatedId", required = false) Integer volunteerId,
            @RequestAttribute(value = "openid", required = false) String openid,
            @RequestAttribute(value = "relatedType", required = false) String relatedType) {
        volunteerId = resolveVolunteerId(volunteerId, openid, relatedType);
        if (volunteerId == null) {
            return Result.ok(Collections.emptyList());
        }
        return Result.ok(teamService.getMyApplications(volunteerId));
    }

    /* ==================== 私有方法 ==================== */

    /**
     * 解析志愿者ID：优先用 JWT 中的 relatedId，若为空则通过 openid 查询
     */
    private Integer resolveVolunteerId(Integer volunteerId, String openid, String relatedType) {
        // 身份校验：仅 volunteer 身份可操作队伍接口，防止学生 ID 空间与志愿者 ID 空间混淆
        if (!"volunteer".equals(relatedType)) {
            return null;
        }
        if (volunteerId != null) {
            return volunteerId;
        }
        if (openid == null || openid.isBlank()) {
            return null;
        }
        Volunteer v = volunteerMapper.selectOne(
                new LambdaQueryWrapper<Volunteer>().eq(Volunteer::getOpenid, openid)
                        .orderByDesc(Volunteer::getId)
                        .last("LIMIT 1")
        );
        return v != null ? v.getId() : null;
    }
}
