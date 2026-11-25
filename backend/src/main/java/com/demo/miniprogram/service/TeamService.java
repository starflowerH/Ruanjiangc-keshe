package com.demo.miniprogram.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.demo.miniprogram.common.BusinessException;
import com.demo.miniprogram.common.ResultCode;
import com.demo.miniprogram.entity.PromoTeam;
import com.demo.miniprogram.entity.TeamMember;
import com.demo.miniprogram.entity.TeamOverride;
import com.demo.miniprogram.entity.TeamRoleApplication;
import com.demo.miniprogram.entity.Volunteer;
import com.demo.miniprogram.mapper.*;
import com.demo.miniprogram.vo.MemberDetailVO;
import com.demo.miniprogram.vo.MemberVO;
import com.demo.miniprogram.vo.TeamVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 队伍服务 - 自由组队、队伍号管理、队长/副队长申请审核
 *
 * @author demo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

    private final PromoTeamMapper promoTeamMapper;
    // 事务模板：同类方法内部调用 @Transactional 不会经过代理（历史坑），
    // 定时清理等需要独立事务的场景显式走 TransactionTemplate
    private final TransactionTemplate transactionTemplate;
    // 事务管理器：用于构建 REQUIRES_NEW 独立事务（如审核拒绝落库）
    private final PlatformTransactionManager transactionManager;
    private final TeamMemberMapper teamMemberMapper;
    private final TeamOverrideMapper teamOverrideMapper;
    private final TeamRoleApplicationMapper roleApplicationMapper;
    private final VolunteerMapper volunteerMapper;
    private final HighSchoolMapper highSchoolMapper;
    private final UniversityMapper universityMapper;
    private final ThreadPoolTaskExecutor taskExecutor;

    /** 队伍号字符池：大写字母 + 数字 */
    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /* ==================== 创建队伍 ==================== */

    /**
     * 校验志愿者存在且报名已审核通过（REJECTED/PENDING 不能进行队伍操作）
     *
     * @param volunteerId 志愿者ID
     * @return 志愿者记录
     */
    private Volunteer requireApprovedVolunteer(Integer volunteerId) {
        Volunteer volunteer = volunteerMapper.selectById(volunteerId);
        if (volunteer == null) {
            throw new BusinessException("志愿者信息不存在");
        }
        if (!"APPROVED".equals(volunteer.getStatus())) {
            throw new BusinessException("您的志愿者报名尚未通过审核，暂不能进行队伍操作");
        }
        return volunteer;
    }

    /**
     * 创建队伍
     *
     * @param volunteerId 创建人志愿者ID
     * @param teamName    队伍名称
     * @return 创建的队伍信息
     */
    @Transactional
    public TeamVO createTeam(Integer volunteerId, String teamName) {
        return createTeam(volunteerId, teamName, null, null);
    }

    @Transactional
    public TeamVO createTeam(Integer volunteerId, String teamName, String contactPhone, String contactWechat) {
        // 检查是否已在某个队伍中
        TeamMember existing = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getVolunteerId, volunteerId)
        );
        if (existing != null) {
            throw new BusinessException("您已在队伍中，请先退出当前队伍");
        }

        // 获取创建人的志愿者信息（含目标高中），并校验报名已审核通过
        Volunteer creator = requireApprovedVolunteer(volunteerId);
        // 目标高中必填校验：报名必填后仍可能被历史数据绕过，此处兜底
        if (creator.getTargetHighSchoolId() == null) {
            throw new BusinessException("请先选择目标高中，再创建队伍");
        }
        String normalizedPhone = contactPhone == null ? "" : contactPhone.trim();
        String normalizedWechat = contactWechat == null ? "" : contactWechat.trim();
        if (normalizedPhone.isEmpty() && normalizedWechat.isEmpty()) {
            throw new BusinessException("请填写至少一种队长通讯方式");
        }

        // 组合查重：同一 大学+高中 组合只能有一支队伍（uk_team 唯一约束）。
        // 自动队/老师建的队已占用组合时，不前置拦截会撞唯一约束返回 500
        Long existTeam = promoTeamMapper.selectCount(
                new LambdaQueryWrapper<PromoTeam>()
                        .eq(PromoTeam::getUniversityId, creator.getUniversityId())
                        .eq(PromoTeam::getHighSchoolId, creator.getTargetHighSchoolId())
        );
        if (existTeam > 0) {
            throw new BusinessException("该目标高中已有队伍，请通过队伍号加入或联系老师");
        }

        // 生成唯一队伍号
        String teamCode = generateUniqueTeamCode();

        // 创建队伍，记录目标高中和大学
        PromoTeam team = new PromoTeam();
        team.setTeamCode(teamCode);
        team.setTeamName(teamName);
        team.setCaptainVolunteerId(volunteerId);
        team.setHighSchoolId(creator.getTargetHighSchoolId());
        team.setUniversityId(creator.getUniversityId());
        team.setCaptainContactPhone(normalizedPhone.isEmpty() ? creator.getPhone() : normalizedPhone);
        team.setCaptainContactWechat(normalizedWechat.isEmpty() ? creator.getWechatId() : normalizedWechat);
        try {
            promoTeamMapper.insert(team);
        } catch (DuplicateKeyException e) {
            // 并发兜底：组合查重通过后，仍有并发请求先创建了同组合队伍
            throw new BusinessException("该目标高中已有队伍，请通过队伍号加入或联系老师");
        }

        // 创建人成为队长
        TeamMember member = new TeamMember();
        member.setTeamId(team.getId());
        member.setVolunteerId(volunteerId);
        member.setRole("CAPTAIN");
        teamMemberMapper.insert(member);

        log.info("队伍创建成功: volunteerId={}, teamCode={}, highSchoolId={}, universityId={}",
                volunteerId, teamCode, creator.getTargetHighSchoolId(), creator.getUniversityId());
        return getMyTeam(volunteerId);
    }

    /* ==================== 加入队伍 ==================== */

    /**
     * 通过队伍号加入队伍
     *
     * @param volunteerId 加入人志愿者ID
     * @param teamCode    队伍号
     * @return 队伍信息
     */
    @Transactional
    public TeamVO joinTeam(Integer volunteerId, String teamCode) {
        // 检查是否已在某个队伍中
        TeamMember existing = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getVolunteerId, volunteerId)
        );
        if (existing != null) {
            throw new BusinessException("您已在队伍中，请先退出当前队伍");
        }

        // 查找队伍
        PromoTeam team = promoTeamMapper.selectByTeamCode(teamCode.toUpperCase());
        if (team == null) {
            throw new BusinessException("队伍号不存在，请检查后重试");
        }

        // 校验：加入者必须与该队伍的目标高中一致
        Volunteer joiner = requireApprovedVolunteer(volunteerId);
        // 大学一致性校验：队伍以创建人大学为归属，跨大学加入会产生
        // target_high_school_id 与队伍不一致的脏成员（详见 updateTeamHighSchool 防连坐说明）
        if (!team.getUniversityId().equals(joiner.getUniversityId())) {
            throw new BusinessException("只能加入本校队伍");
        }
        if (team.getHighSchoolId() != null && !team.getHighSchoolId().equals(joiner.getTargetHighSchoolId())) {
            // 获取高中名称用于友好提示
            String teamHsName = "";
            if (team.getHighSchoolId() != null) {
                var hs = highSchoolMapper.selectById(team.getHighSchoolId());
                if (hs != null) teamHsName = hs.getName();
            }
            String myHsName = "";
            if (joiner.getTargetHighSchoolId() != null) {
                var hs = highSchoolMapper.selectById(joiner.getTargetHighSchoolId());
                if (hs != null) myHsName = hs.getName();
            }
            throw new BusinessException("目标高中不一致！\n"
                    + "该队伍的目标高中是「" + teamHsName + "」\n"
                    + "您选择的目标高中是「" + myHsName + "」\n"
                    + "只有前往同一所高中的志愿者才能组建队伍");
        }

        // 加入队伍
        TeamMember member = new TeamMember();
        member.setTeamId(team.getId());
        member.setVolunteerId(volunteerId);
        member.setRole("MEMBER");
        member.setJoinedAt(LocalDateTime.now());
        teamMemberMapper.insert(member);

        log.info("成员加入队伍: volunteerId={}, teamId={}, teamCode={}, highSchoolId={}",
                volunteerId, team.getId(), teamCode, joiner.getTargetHighSchoolId());

        return getMyTeam(volunteerId);
    }

    /* ==================== 退出队伍 ==================== */

    /**
     * 退出队伍
     *
     * @param volunteerId 退出人志愿者ID
     */
    @Transactional
    public void leaveTeam(Integer volunteerId) {
        // 统一找队伍：自由队查 team_member，自动/手动队按组合派生（P1-5：
        // 此前只查 team_member，自动队成员无记录会误报"您不在任何队伍中"）
        PromoTeam team = findMyTeam(volunteerId);
        if (team == null) {
            throw new BusinessException("您不在任何队伍中");
        }

        TeamMember member = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getVolunteerId, volunteerId)
        );
        if (member == null) {
            // 自动/手动组队成员（无 team_member 记录）：写 OUT override 移出组合派生队伍
            if (volunteerId.equals(team.getCaptainVolunteerId())) {
                // 队长退出需先转让或解散（与自由队规则一致）
                throw new BusinessException("队长不能直接退出，请先转让队长或解散队伍");
            }
            // 同一 (队,人) 的 IN 与 OUT 互斥：先删旧 override 再写 OUT（与 removeMemberFromTeam 一致）
            teamOverrideMapper.delete(
                    new LambdaQueryWrapper<TeamOverride>()
                            .eq(TeamOverride::getPromoTeamId, team.getId())
                            .eq(TeamOverride::getVolunteerId, volunteerId)
            );
            upsertOverride(team.getId(), volunteerId, "OUT");
            log.info("自动队成员退出队伍: volunteerId={}, teamId={}", volunteerId, team.getId());
            return;
        }

        // 如果是队长，需要先转让或解散队伍
        if ("CAPTAIN".equals(member.getRole())) {
            // 检查是否有其他成员可以接任
            TeamMember nextMember = teamMemberMapper.selectOne(
                    new LambdaQueryWrapper<TeamMember>()
                            .eq(TeamMember::getTeamId, member.getTeamId())
                            .ne(TeamMember::getVolunteerId, volunteerId)
                            .last("LIMIT 1")
            );
            if (nextMember != null) {
                // 指定一个成员为队长
                nextMember.setRole("CAPTAIN");
                teamMemberMapper.updateById(nextMember);
                team.setCaptainVolunteerId(nextMember.getVolunteerId());
                promoTeamMapper.updateById(team);
            } else {
                // 队伍只剩队长一人，解散队伍
                teamMemberMapper.deleteById(member.getId());
                promoTeamMapper.deleteById(team.getId());
                // 清理该队遗留的待审核申请（队长/转让），避免管理员审核时碰到已解散队伍
                List<TeamRoleApplication> staleApps = roleApplicationMapper.selectList(
                        new LambdaQueryWrapper<TeamRoleApplication>()
                                .eq(TeamRoleApplication::getTeamId, team.getId())
                                .eq(TeamRoleApplication::getStatus, "PENDING")
                );
                if (!staleApps.isEmpty()) {
                    for (TeamRoleApplication app : staleApps) {
                        app.setStatus("REJECTED");
                        app.setReviewedAt(LocalDateTime.now());
                    }
                    staleApps.forEach(roleApplicationMapper::updateById);
                    log.info("队伍解散，清理待审核申请: teamId={}, count={}", team.getId(), staleApps.size());
                }
                log.info("队伍已解散: teamId={}", team.getId());
                return;
            }
        }

        teamMemberMapper.deleteById(member.getId());
        log.info("成员退出队伍: volunteerId={}, teamId={}", volunteerId, member.getTeamId());
    }

    /* ==================== 查询 ==================== */

    /**
     * 获取我的队伍信息
     *
     * 成员判定统一走 findMyTeam：自由组队查 team_member，自动/手动组队按组合派生+OUT 校验。
     * 修复：自动组队只写 promo_team 不写 team_member，若仅查 team_member 会把自动队成员误判为未入队。
     */
    public TeamVO getMyTeam(Integer volunteerId) {
        // P1-6：被取消资格的旧 JWT 不能继续读取队伍
        requireApprovedVolunteer(volunteerId);
        PromoTeam team = findMyTeam(volunteerId);
        if (team == null) {
            return null;
        }
        // 志愿者端：不返回成员/队长手机号
        return buildTeamVO(team, false);
    }

    /**
     * 更新队伍简介（本队成员均可编辑）
     *
     * @param volunteerId 操作者志愿者ID
     * @param teamId      队伍ID
     * @param description 新的队伍简介
     * @return 更新后的队伍信息
     */
    @Transactional
    public TeamVO updateDescription(Integer volunteerId, Integer teamId, String description) {
        requireApprovedVolunteer(volunteerId);
        PromoTeam team = promoTeamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("队伍不存在");
        }
        // 校验操作者是本队成员（自由组队校验 team_member；自动/手动组队校验有效成员）
        boolean isMember = isTeamMember(team, volunteerId);
        if (!isMember) {
            throw new BusinessException("您不是该队伍的成员，无法编辑简介");
        }
        if (description != null && description.length() > 500) {
            throw new BusinessException("队伍简介不能超过500字");
        }
        team.setDescription(description);
        promoTeamMapper.updateById(team);
        log.info("队伍简介更新: teamId={}, volunteerId={}", teamId, volunteerId);
        return buildTeamVO(team, false);
    }

    /** 仅队长可修改目标高中，并复用既有组合冲突与成员同步校验。 */
    @Transactional
    public TeamVO updateTargetForCaptain(Integer volunteerId, Integer teamId, Integer highSchoolId) {
        requireApprovedVolunteer(volunteerId);
        PromoTeam team = promoTeamMapper.selectById(teamId);
        if (team == null || !volunteerId.equals(team.getCaptainVolunteerId())) {
            throw new BusinessException("仅队长可以修改目标高中");
        }
        updateTeamHighSchool(team.getUniversityId(), teamId, highSchoolId);
        return getMyTeam(volunteerId);
    }

    /** 仅队长可维护队长通讯录信息。 */
    @Transactional
    public TeamVO updateCaptainContact(Integer volunteerId, Integer teamId, String phone, String wechat) {
        requireApprovedVolunteer(volunteerId);
        PromoTeam team = promoTeamMapper.selectById(teamId);
        if (team == null || !volunteerId.equals(team.getCaptainVolunteerId())) {
            throw new BusinessException("仅队长可以修改通讯方式");
        }
        String normalizedPhone = phone == null ? "" : phone.trim();
        String normalizedWechat = wechat == null ? "" : wechat.trim();
        if (normalizedPhone.isEmpty() && normalizedWechat.isEmpty()) {
            throw new BusinessException("请至少保留一种通讯方式");
        }
        team.setCaptainContactPhone(normalizedPhone);
        team.setCaptainContactWechat(normalizedWechat);
        promoTeamMapper.updateById(team);
        return getMyTeam(volunteerId);
    }

    /**
     * 判断志愿者是否为队伍的有效成员
     */
    private boolean isTeamMember(PromoTeam team, Integer volunteerId) {
        // 自由组队：检查 team_member
        TeamMember tm = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, team.getId())
                        .eq(TeamMember::getVolunteerId, volunteerId)
        );
        if (tm != null) {
            return true;
        }
        // 自动/手动组队：检查有效成员列表
        return getEffectiveMembers(team, false).stream()
                .anyMatch(m -> volunteerId.equals(m.getVolunteerId()));
    }

    /**
     * 获取队伍成员列表
     *
     * 权限：仅该队伍的有效成员可查看本队成员列表（自由队/自动队成员判定统一走 getEffectiveMembers）。
     *
     * @param operatorId 操作者志愿者ID（当前登录用户）
     * @param teamId     队伍ID
     * @return 本队成员列表（不返回联系方式）
     */
    public List<MemberVO> getTeamMembers(Integer operatorId, Integer teamId) {
        // P1-6：被取消资格的旧 JWT 不能继续读取队伍成员
        requireApprovedVolunteer(operatorId);
        PromoTeam team = promoTeamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("队伍不存在");
        }
        // 同队校验：操作者必须是该队伍的有效成员
        if (!isTeamMember(team, operatorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能查看本队伍成员信息");
        }
        // 统一成员判定（自由队 team_member + override，自动队组合派生 + override），不返回手机号
        return getEffectiveMembers(team, false);
    }

    /**
     * 查询队伍成员详情（点击成员头像查看报名信息）
     *
     * 权限约束：仅同一队伍的成员可查看本队成员信息。
     *
     * @param operatorId   操作者志愿者ID（当前登录用户）
     * @param targetVolunteerId 被查看成员的志愿者ID
     * @return 成员详情
     */
    public MemberDetailVO getMemberDetail(Integer operatorId, Integer targetVolunteerId) {
        requireApprovedVolunteer(operatorId);

        // 查找操作者所在的队伍（自由组队优先 team_member，否则按组合派生）
        PromoTeam myTeam = findMyTeam(operatorId);
        if (myTeam == null) {
            throw new BusinessException("您不在任何队伍中");
        }

        // 校验目标成员与操作者同队
        List<MemberVO> myMembers = getEffectiveMembers(myTeam, false);
        boolean sameTeam = myMembers.stream()
                .anyMatch(m -> targetVolunteerId.equals(m.getVolunteerId()));
        if (!sameTeam) {
            throw new BusinessException("只能查看本队伍成员的信息");
        }

        // 查询目标志愿者信息
        Volunteer target = volunteerMapper.selectById(targetVolunteerId);
        if (target == null) {
            throw new BusinessException("成员信息不存在");
        }

        MemberDetailVO vo = new MemberDetailVO();
        vo.setVolunteerId(target.getId());
        vo.setName(target.getName());
        vo.setPhone(target.getPhone());
        vo.setWechatId(target.getWechatId());
        vo.setGrade(target.getGrade());
        vo.setMajor(target.getMajor());

        // 角色
        String role = myMembers.stream()
                .filter(m -> targetVolunteerId.equals(m.getVolunteerId()))
                .map(MemberVO::getRole)
                .findFirst().orElse("MEMBER");
        vo.setRole(role);

        // 大学名称
        if (target.getUniversityId() != null) {
            var uni = universityMapper.selectById(target.getUniversityId());
            vo.setUniversityName(uni != null ? uni.getName() : "");
        }
        // 目标高中名称
        if (target.getTargetHighSchoolId() != null) {
            var hs = highSchoolMapper.selectById(target.getTargetHighSchoolId());
            vo.setTargetHighSchoolName(hs != null ? hs.getName() : "");
        }

        return vo;
    }

    /**
     * 查找志愿者所在的队伍（返回 PromoTeam）
     *
     * 优先：自由组队队伍（team_member 记录）
     * 其次：按 (university_id, target_high_school_id) 组合派生的自动/手动组队队伍
     */
    private PromoTeam findMyTeam(Integer volunteerId) {
        // 1. 自由组队
        TeamMember tm = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getVolunteerId, volunteerId)
        );
        if (tm != null) {
            return promoTeamMapper.selectById(tm.getTeamId());
        }

        // 2. 组合派生队伍
        TeamOverride override = teamOverrideMapper.selectOne(
                new LambdaQueryWrapper<TeamOverride>()
                        .eq(TeamOverride::getVolunteerId, volunteerId)
                        .eq(TeamOverride::getAction, "IN")
        );
        if (override != null) {
            return promoTeamMapper.selectById(override.getPromoTeamId());
        }
        return null;
    }

    /**
     * 获取队伍的有效成员列表（统一成员判定）
     *
     * 按队伍类型区分：
     *   1. 自由组队队伍（存在 team_member 记录）：成员 = team_member + override IN - override OUT，
     *      team_member 的角色（CAPTAIN/MEMBER）完整保留
     *   2. 自动/手动组队队伍（无 team_member 记录）：成员 = 组合派生（该组合下所有已审核志愿者）
     *      + override IN - override OUT
     *
     * @param team           队伍
     * @param includeContact 是否返回联系方式（手机号）。仅老师端队伍详情可见，
     *                       志愿者端一律 false，避免通过通用 buildTeamVO 泄露同队成员手机号
     * @return 去重后的有效成员列表
     */
    public List<MemberVO> getEffectiveMembers(PromoTeam team, boolean includeContact) {
        Map<Integer, MemberVO> memberMap = new LinkedHashMap<>();

        if (team.getId() == null) {
            return new ArrayList<>(memberMap.values());
        }

        boolean freeTeam = isFreeTeam(team.getId())
                || teamOverrideMapper.selectCount(
                new LambdaQueryWrapper<TeamOverride>()
                        .eq(TeamOverride::getPromoTeamId, team.getId())
                        .eq(TeamOverride::getAction, "IN")) > 0;

        if (freeTeam) {
            // ===== 自由组队队伍：以 team_member 为准 =====
            for (TeamMember tm : teamMemberMapper.selectList(
                    new LambdaQueryWrapper<TeamMember>()
                            .eq(TeamMember::getTeamId, team.getId())
                            .last("ORDER BY CASE role WHEN 'CAPTAIN' THEN 0 ELSE 1 END"))) {
                // OUT 记录排除（移出时 team_member 记录会被删除，此处双保险）
                Long outCnt = teamOverrideMapper.selectCount(
                        new LambdaQueryWrapper<TeamOverride>()
                                .eq(TeamOverride::getPromoTeamId, team.getId())
                                .eq(TeamOverride::getVolunteerId, tm.getVolunteerId())
                                .eq(TeamOverride::getAction, "OUT"));
                if (outCnt > 0) {
                    continue;
                }
                Volunteer v = volunteerMapper.selectById(tm.getVolunteerId());
                if (v == null) continue;
                MemberVO vo = new MemberVO();
                vo.setVolunteerId(v.getId());
                vo.setName(v.getName());
                if (includeContact) {
                    vo.setPhone(v.getPhone());
                }
                vo.setMajor(v.getMajor());
                vo.setGrade(v.getGrade());
                vo.setRole(tm.getRole());
                vo.setIsCaptain("CAPTAIN".equals(tm.getRole()));
                if (v.getUniversityId() != null) {
                    var uni = universityMapper.selectById(v.getUniversityId());
                    vo.setUniversityName(uni != null ? uni.getName() : "");
                }
                memberMap.put(v.getId(), vo);
            }
            // 补充老师手动指定入队（IN override）但未写入 team_member 的成员
            for (TeamOverride o : teamOverrideMapper.selectList(
                    new LambdaQueryWrapper<TeamOverride>()
                            .eq(TeamOverride::getPromoTeamId, team.getId())
                            .eq(TeamOverride::getAction, "IN"))) {
                if (memberMap.containsKey(o.getVolunteerId())) {
                    continue;
                }
                Volunteer v = volunteerMapper.selectById(o.getVolunteerId());
                if (v == null) continue;
                MemberVO vo = new MemberVO();
                vo.setVolunteerId(v.getId());
                vo.setName(v.getName());
                if (includeContact) {
                    vo.setPhone(v.getPhone());
                }
                vo.setMajor(v.getMajor());
                vo.setGrade(v.getGrade());
                vo.setRole("MEMBER");
                vo.setIsCaptain(false);
                if (v.getUniversityId() != null) {
                    var uni = universityMapper.selectById(v.getUniversityId());
                    vo.setUniversityName(uni != null ? uni.getName() : "");
                }
                memberMap.put(v.getId(), vo);
            }
        } else {
            // ===== 自动/手动组队队伍：派生成员 + override 修正（SQL 已处理 IN/OUT） =====
            // Legacy teams without a snapshot are intentionally empty until an administrator
            // explicitly rebuilds or assigns their members.
            List<MemberVO> derived = Collections.emptyList();
            for (MemberVO m : derived) {
                // selectTeamMembers 统一返回 phone，非老师详情场景剥离开来
                if (!includeContact) {
                    m.setPhone(null);
                }
                memberMap.put(m.getVolunteerId(), m);
            }
        }

        return new ArrayList<>(memberMap.values());
    }

    /* ==================== 队长转让 ==================== */

    /**
     * 队长申请转让队长身份给其他成员
     *
     * @param captainId   当前队长的志愿者ID
     * @param teamId      队伍ID
     * @param targetId    转让目标的志愿者ID
     * @param reason      转让理由
     */
    @Transactional
    public void transferCaptain(Integer captainId, Integer teamId, Integer targetId, String reason) {
        // 校验操作者志愿者身份已审核通过
        requireApprovedVolunteer(captainId);
        // 验证当前用户是队长
        PromoTeam team = promoTeamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("队伍不存在");
        }
        if (!captainId.equals(team.getCaptainVolunteerId())) {
            throw new BusinessException("只有队长才能转让队长身份");
        }

        // 验证目标是队伍成员（P1-5：统一判定，自动队成员也可作为转让目标）
        if (!isTeamMember(team, targetId)) {
            throw new BusinessException("转让目标不是该队伍的成员");
        }

        // 检查是否已有待审核的转让申请
        Long pendingCount = roleApplicationMapper.selectCount(
                new LambdaQueryWrapper<TeamRoleApplication>()
                        .eq(TeamRoleApplication::getTeamId, teamId)
                        .eq(TeamRoleApplication::getVolunteerId, captainId)
                        .eq(TeamRoleApplication::getApplyRole, "CAPTAIN_TRANSFER")
                        .eq(TeamRoleApplication::getStatus, "PENDING")
        );
        if (pendingCount > 0) {
            throw new BusinessException("您已有待审核的转让申请，请等待审核结果");
        }

        // 创建转让申请（reason 中记录目标人ID）
        TeamRoleApplication application = new TeamRoleApplication();
        application.setTeamId(teamId);
        application.setVolunteerId(captainId);
        application.setApplyRole("CAPTAIN_TRANSFER");
        application.setReason("转让给:" + targetId + ";" + (reason != null ? reason : ""));
        application.setStatus("PENDING");
        application.setCreatedAt(LocalDateTime.now());
        roleApplicationMapper.insert(application);

        log.info("队长转让申请提交: captainId={}, teamId={}, targetId={}", captainId, teamId, targetId);
    }

    /* ==================== 队长/副队长申请 ==================== */

    /**
     * 申请成为队长或转让队长
     *
     * @param volunteerId 申请人ID
     * @param teamId      队伍ID
     * @param applyRole   申请角色：CAPTAIN 或 CAPTAIN_TRANSFER
     * @param reason      申请理由
     */
    @Transactional
    public void applyForRole(Integer volunteerId, Integer teamId, String applyRole, String reason) {
        // 校验操作者志愿者身份已审核通过
        requireApprovedVolunteer(volunteerId);
        PromoTeam team = promoTeamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("队伍不存在");
        }
        // 验证是否是队伍成员（P1-5：统一判定，自动队成员无 team_member 记录也可申请）
        if (!isTeamMember(team, volunteerId)) {
            throw new BusinessException("您不是该队伍的成员");
        }

        // 检查是否已经有待审核的申请
        Long pendingCount = roleApplicationMapper.selectCount(
                new LambdaQueryWrapper<TeamRoleApplication>()
                        .eq(TeamRoleApplication::getTeamId, teamId)
                        .eq(TeamRoleApplication::getVolunteerId, volunteerId)
                        .eq(TeamRoleApplication::getStatus, "PENDING")
        );
        if (pendingCount > 0) {
            throw new BusinessException("您已有待审核的申请，请等待审核结果");
        }

        // 如果申请队长，检查是否已有队长
        if ("CAPTAIN".equals(applyRole)) {
            if (team.getCaptainVolunteerId() != null) {
                throw new BusinessException("该队伍已有队长，无法申请");
            }
        }

        // 创建申请
        TeamRoleApplication application = new TeamRoleApplication();
        application.setTeamId(teamId);
        application.setVolunteerId(volunteerId);
        application.setApplyRole(applyRole);
        application.setReason(reason);
        application.setStatus("PENDING");
        application.setCreatedAt(LocalDateTime.now());
        roleApplicationMapper.insert(application);

        log.info("角色申请提交: volunteerId={}, teamId={}, applyRole={}", volunteerId, teamId, applyRole);
    }

    /**
     * 审核队长/副队长申请
     *
     * @param applicationId 申请ID
     * @param adminId       管理员ID
     * @param action        审核动作：APPROVED 或 REJECTED
     */
    @Transactional
    public void auditRoleApplication(Integer applicationId, Integer adminId, String action) {
        TeamRoleApplication application = roleApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException("申请记录不存在");
        }
        if (!"PENDING".equals(application.getStatus())) {
            throw new BusinessException("该申请已审核，请勿重复操作");
        }

        application.setStatus(action);
        application.setReviewedBy(adminId);
        application.setReviewedAt(LocalDateTime.now());
        roleApplicationMapper.updateById(application);

        // 如果通过，更新队伍的队长/副队长
        if ("APPROVED".equals(action)) {
            PromoTeam team = promoTeamMapper.selectById(application.getTeamId());
            if (team == null) {
                // 队伍已被解散/删除（如队长退出后自动解散），无法执行角色变更。
                // 申请已标记为审核完成，避免永远卡在待审核且审核时抛 500。
                log.warn("审核通过但队伍不存在，跳过角色变更: applicationId={}, teamId={}",
                        applicationId, application.getTeamId());
                return;
            }

            // P1-8：审核通过前重新验证成员资格（自由队/自动队统一判定），
            // 防止申请人已退队/被移出后仍被提升；转让场景目标必须仍是有效成员
            boolean applicantValid = isTeamMember(team, application.getVolunteerId());
            boolean transferTargetValid = true;
            if (applicantValid && "CAPTAIN_TRANSFER".equals(application.getApplyRole())) {
                Integer targetId = parseTransferTargetId(application.getReason());
                transferTargetValid = targetId != null && isTeamMember(team, targetId);
            }
            if (!applicantValid || !transferTargetValid) {
                // 拒绝落库需独立事务：BusinessException 会回滚当前事务，
                // 直接 set+update+throw 会把 REJECTED 一起回滚，申请将永远卡在 PENDING
                application.setStatus("REJECTED");
                application.setReviewedBy(adminId);
                application.setReviewedAt(LocalDateTime.now());
                TransactionTemplate rejectionTx = new TransactionTemplate(transactionManager);
                rejectionTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                rejectionTx.executeWithoutResult(status -> roleApplicationMapper.updateById(application));
                log.warn("审核拒绝：成员已不在队伍中: applicationId={}, volunteerId={}, teamId={}",
                        applicationId, application.getVolunteerId(), application.getTeamId());
                throw new BusinessException("成员已不在队伍中，申请已拒绝");
            }
            TeamMember member = teamMemberMapper.selectOne(
                    new LambdaQueryWrapper<TeamMember>()
                            .eq(TeamMember::getTeamId, application.getTeamId())
                            .eq(TeamMember::getVolunteerId, application.getVolunteerId())
            );

            if ("CAPTAIN".equals(application.getApplyRole())) {
                // 如果有旧队长，降为普通成员
                if (team.getCaptainVolunteerId() != null) {
                    TeamMember oldCaptain = teamMemberMapper.selectOne(
                            new LambdaQueryWrapper<TeamMember>()
                                    .eq(TeamMember::getTeamId, application.getTeamId())
                                    .eq(TeamMember::getRole, "CAPTAIN")
                    );
                    if (oldCaptain != null) {
                        oldCaptain.setRole("MEMBER");
                        teamMemberMapper.updateById(oldCaptain);
                    }
                }
                team.setCaptainVolunteerId(application.getVolunteerId());
                if (member != null) {
                    member.setRole("CAPTAIN");
                    teamMemberMapper.updateById(member);
                }
            } else if ("CAPTAIN_TRANSFER".equals(application.getApplyRole())) {
                // 从 reason 中解析目标人ID（格式："转让给:targetId;理由"）
                Integer targetId = parseTransferTargetId(application.getReason());
                if (targetId != null) {
                    // 旧队长降为普通成员
                    if (member != null) {
                        member.setRole("MEMBER");
                        teamMemberMapper.updateById(member);
                    }
                    // 目标成员升为队长
                    TeamMember targetMember = teamMemberMapper.selectOne(
                            new LambdaQueryWrapper<TeamMember>()
                                    .eq(TeamMember::getTeamId, application.getTeamId())
                                    .eq(TeamMember::getVolunteerId, targetId)
                    );
                    if (targetMember != null) {
                        targetMember.setRole("CAPTAIN");
                        teamMemberMapper.updateById(targetMember);
                    }
                    team.setCaptainVolunteerId(targetId);
                }
            }

            promoTeamMapper.updateById(team);
            log.info("角色申请通过: applicationId={}, applyRole={}", applicationId, application.getApplyRole());
        } else {
            log.info("角色申请拒绝: applicationId={}", applicationId);
        }
    }

    /**
     * 获取待审核的角色申请列表
     */
    public List<Map<String, Object>> getPendingApplications() {
        List<TeamRoleApplication> applications = roleApplicationMapper.selectList(
                new LambdaQueryWrapper<TeamRoleApplication>()
                        .eq(TeamRoleApplication::getStatus, "PENDING")
                        .orderByDesc(TeamRoleApplication::getCreatedAt)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (TeamRoleApplication app : applications) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", app.getId());
            item.put("teamId", app.getTeamId());
            item.put("volunteerId", app.getVolunteerId());
            item.put("applyRole", app.getApplyRole());
            item.put("reason", app.getReason());
            item.put("status", app.getStatus());
            item.put("createdAt", app.getCreatedAt());

            // 查询申请人信息
            Volunteer volunteer = volunteerMapper.selectById(app.getVolunteerId());
            if (volunteer != null) {
                item.put("volunteerName", volunteer.getName());
                item.put("volunteerMajor", volunteer.getMajor());
                item.put("volunteerGrade", volunteer.getGrade());
            }

            // 查询队伍信息
            PromoTeam team = promoTeamMapper.selectById(app.getTeamId());
            if (team != null) {
                item.put("teamCode", team.getTeamCode());
                item.put("teamName", team.getTeamName());
            }

            result.add(item);
        }

        return result;
    }

    /**
     * 获取我的申请记录
     */
    public List<TeamRoleApplication> getMyApplications(Integer volunteerId) {
        return roleApplicationMapper.selectList(
                new LambdaQueryWrapper<TeamRoleApplication>()
                        .eq(TeamRoleApplication::getVolunteerId, volunteerId)
                        .orderByDesc(TeamRoleApplication::getCreatedAt)
        );
    }

    /**
     * 解析转让申请中的目标人ID
     * reason 格式："转让给:targetId;理由"
     */
    private Integer parseTransferTargetId(String reason) {
        if (reason == null || !reason.startsWith("转让给:")) {
            return null;
        }
        try {
            String afterPrefix = reason.substring(4); // "targetId;理由"
            String idStr = afterPrefix.contains(";") ? afterPrefix.substring(0, afterPrefix.indexOf(";")) : afterPrefix;
            return Integer.parseInt(idStr.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /* ==================== 私有辅助方法 ==================== */

    /**
     * 生成唯一队伍号：2个字母 + 3个数字，随机排列
     */
    private String generateUniqueTeamCode() {
        Random random = new Random();
        String code;
        int maxRetries = 100;

        do {
            // 确保包含2个字母和3个数字
            List<Character> chars = new ArrayList<>();
            // 2个随机字母
            for (int i = 0; i < 2; i++) {
                chars.add(CODE_CHARS.charAt(random.nextInt(26))); // A-Z
            }
            // 3个随机数字
            for (int i = 0; i < 3; i++) {
                chars.add(CODE_CHARS.charAt(26 + random.nextInt(10))); // 0-9
            }
            // 随机打乱顺序
            Collections.shuffle(chars);

            StringBuilder sb = new StringBuilder();
            for (char c : chars) {
                sb.append(c);
            }
            code = sb.toString();

            maxRetries--;
            if (maxRetries <= 0) {
                throw new BusinessException(ResultCode.SERVER_ERROR, "队伍号生成失败，请重试");
            }
        } while (isTeamCodeExists(code));

        return code;
    }

    /**
     * 检查队伍号是否已存在
     */
    private boolean isTeamCodeExists(String code) {
        return promoTeamMapper.exists(
                new LambdaQueryWrapper<PromoTeam>()
                        .eq(PromoTeam::getTeamCode, code)
        );
    }

    /**
     * 构建 TeamVO
     *
     * @param includeContact 是否返回联系方式（队长电话+成员手机号）。
     *                       仅老师端队伍详情（getTeamDetail）传 true，
     *                       志愿者端 getMyTeam / 老师列表页一律 false，防止手机号泄露
     */
    private TeamVO buildTeamVO(PromoTeam team, boolean includeContact) {
        TeamVO vo = new TeamVO();
        vo.setId(team.getId());
        vo.setTeamCode(team.getTeamCode());
        vo.setTeamName(team.getTeamName());
        vo.setHighSchoolId(team.getHighSchoolId());
        vo.setUniversityId(team.getUniversityId());
        vo.setCaptainVolunteerId(team.getCaptainVolunteerId());
        vo.setDescription(team.getDescription());
        vo.setCaptainContactConfigured((team.getCaptainContactPhone() != null && !team.getCaptainContactPhone().isBlank())
                || (team.getCaptainContactWechat() != null && !team.getCaptainContactWechat().isBlank()));

        // 队长信息
        if (team.getCaptainVolunteerId() != null) {
            Volunteer captain = volunteerMapper.selectById(team.getCaptainVolunteerId());
            if (captain != null) {
                vo.setCaptainName(captain.getName());
                if (includeContact) {
                    vo.setCaptainPhone(team.getCaptainContactPhone() != null && !team.getCaptainContactPhone().isBlank()
                            ? team.getCaptainContactPhone() : captain.getPhone());
                    vo.setCaptainWechat(team.getCaptainContactWechat() != null && !team.getCaptainContactWechat().isBlank()
                            ? team.getCaptainContactWechat() : captain.getWechatId());
                }
            }
        }

        // 高中名称
        if (team.getHighSchoolId() != null) {
            var hs = highSchoolMapper.selectById(team.getHighSchoolId());
            vo.setHighSchoolName(hs != null ? hs.getName() : "");
        }

        // 大学名称
        if (team.getUniversityId() != null) {
            var uni = universityMapper.selectById(team.getUniversityId());
            vo.setUniversityName(uni != null ? uni.getName() : "");
        }

        // 成员数和成员列表
        List<MemberVO> members = getEffectiveMembers(team, includeContact);
        vo.setMemberCount(members.size());
        vo.setMembers(members);

        return vo;
    }

    /* ==================== 老师端：组队状态查看 ==================== */

    /**
     * 老师手动创建自定义队伍
     *
     * @param universityId       老师所属大学（数据隔离）
     * @param teamName           队伍名称
     * @param highSchoolId       目标高中
     * @param captainVolunteerId 指定队长（可选）
     * @return 创建的队伍
     */
    @Transactional
    public TeamVO createTeacherTeam(Integer universityId, String teamName, Integer highSchoolId, Integer captainVolunteerId) {
        if (highSchoolId == null) {
            throw new BusinessException("请选择目标高中");
        }
        if (highSchoolId != null && highSchoolMapper.selectById(highSchoolId) == null) {
            throw new BusinessException("目标高中不存在");
        }

        // uk_team 约束：同一 高中+大学 组合只能有一支队伍
        Long exist = promoTeamMapper.selectCount(
                new LambdaQueryWrapper<PromoTeam>()
                        .eq(PromoTeam::getHighSchoolId, highSchoolId)
                        .eq(PromoTeam::getUniversityId, universityId)
        );
        if (exist > 0) {
            throw new BusinessException("该高中已存在队伍，不能再创建");
        }

        PromoTeam team = new PromoTeam();
        team.setTeamCode(generateUniqueTeamCode());
        team.setTeamName(teamName != null ? teamName.trim() : null);
        team.setHighSchoolId(highSchoolId);
        team.setUniversityId(universityId);
        promoTeamMapper.insert(team);

        // 指定队长（可选）：必须是本校已审核志愿者
        if (captainVolunteerId != null) {
            Volunteer captain = volunteerMapper.selectById(captainVolunteerId);
            if (captain == null || !universityId.equals(captain.getUniversityId())) {
                throw new BusinessException("指定的队长不是本校志愿者");
            }
            if (!"APPROVED".equals(captain.getStatus())) {
                throw new BusinessException("指定的队长志愿者报名尚未通过审核");
            }
            // 队长不能已在其他队伍（IN 到其他队伍 或 已在自由组队中）
            Long otherIn = teamOverrideMapper.selectCount(
                    new LambdaQueryWrapper<TeamOverride>()
                            .eq(TeamOverride::getVolunteerId, captainVolunteerId)
                            .eq(TeamOverride::getAction, "IN")
            );
            if (otherIn > 0) {
                throw new BusinessException("该志愿者已被指定到其他队伍，不能担任队长");
            }
            Long otherMember = teamMemberMapper.selectCount(
                    new LambdaQueryWrapper<TeamMember>()
                            .eq(TeamMember::getVolunteerId, captainVolunteerId)
            );
            if (otherMember > 0) {
                throw new BusinessException("该志愿者已在自由组队队伍中，不能担任队长");
            }
            // 已是其他自动/手动组队队伍的派生成员（属于其他组合），且未被移出
            // 注意：自由组队队伍（有 team_member）不产生派生成员，不在此列
            if (captain.getTargetHighSchoolId() != null) {
                PromoTeam derivedTeam = promoTeamMapper.selectOne(
                        new LambdaQueryWrapper<PromoTeam>()
                                .eq(PromoTeam::getUniversityId, captain.getUniversityId())
                                .eq(PromoTeam::getHighSchoolId, captain.getTargetHighSchoolId())
                                .ne(PromoTeam::getId, team.getId())
                );
                if (derivedTeam != null && !isFreeTeam(derivedTeam.getId())) {
                    Long outCnt = teamOverrideMapper.selectCount(
                            new LambdaQueryWrapper<TeamOverride>()
                                    .eq(TeamOverride::getPromoTeamId, derivedTeam.getId())
                                    .eq(TeamOverride::getVolunteerId, captainVolunteerId)
                                    .eq(TeamOverride::getAction, "OUT")
                    );
                    if (outCnt == 0) {
                        throw new BusinessException("该志愿者已属于其他队伍，不能担任队长");
                    }
                }
            }
            // 若队长不属于该组合（目标高中不同），自动写 IN 覆盖使其成为成员
            if (!highSchoolId.equals(captain.getTargetHighSchoolId())) {
                upsertOverride(team.getId(), captainVolunteerId, "IN");
            }
            team.setCaptainVolunteerId(captainVolunteerId);
            promoTeamMapper.updateById(team);

            TeamMember captainMember = new TeamMember();
            captainMember.setTeamId(team.getId());
            captainMember.setVolunteerId(captainVolunteerId);
            captainMember.setRole("CAPTAIN");
            captainMember.setJoinedAt(LocalDateTime.now());
            teamMemberMapper.insert(captainMember);
        }

        log.info("老师手动建队: universityId={}, teamId={}, highSchoolId={}, captainId={}",
                universityId, team.getId(), highSchoolId, captainVolunteerId);
        return buildTeamVO(team, false);
    }

    /**
     * 老师手动指定志愿者加入队伍（写 IN 覆盖）
     *
     * 语义说明：移出（OUT）后老师可重新 ADD 将该志愿者重新指定入队——
     * 先删旧 override 再写 IN，等效"撤销移出"。因此"移出不可撤销"仅指
     * 界面上不提供单独的撤销入口，重新指定入队仍是允许的新操作。
     *
     * @param universityId 老师所属大学（数据隔离）
     * @param teamId       队伍ID
     * @param volunteerId  志愿者ID
     */
    @Transactional
    public void addMemberToTeam(Integer universityId, Integer teamId, Integer volunteerId) {
        PromoTeam team = requireMyTeam(universityId, teamId);
        Volunteer v = volunteerMapper.selectById(volunteerId);
        if (v == null || !universityId.equals(v.getUniversityId())) {
            throw new BusinessException("该志愿者不是本校志愿者");
        }
        if (!"APPROVED".equals(v.getStatus())) {
            throw new BusinessException("该志愿者报名尚未通过审核，无法加入队伍");
        }

        // 已被指定到其他队伍（其他队伍存在 IN 记录）
        Long otherIn = teamOverrideMapper.selectCount(
                new LambdaQueryWrapper<TeamOverride>()
                        .eq(TeamOverride::getVolunteerId, volunteerId)
                        .eq(TeamOverride::getAction, "IN")
                        .ne(TeamOverride::getPromoTeamId, teamId)
        );
        if (otherIn > 0) {
            throw new BusinessException("该志愿者已被指定到其他队伍");
        }
        // 已是其他自动/手动组队队伍的派生成员（属于其他组合），且未被移出
        // 注意：自由组队队伍（有 team_member）不产生派生成员，不在此列
        if (v.getTargetHighSchoolId() != null) {
            PromoTeam derivedTeam = promoTeamMapper.selectOne(
                    new LambdaQueryWrapper<PromoTeam>()
                            .eq(PromoTeam::getUniversityId, v.getUniversityId())
                            .eq(PromoTeam::getHighSchoolId, v.getTargetHighSchoolId())
                            .ne(PromoTeam::getId, teamId)
            );
            if (derivedTeam != null && !isFreeTeam(derivedTeam.getId())) {
                Long outCnt = teamOverrideMapper.selectCount(
                        new LambdaQueryWrapper<TeamOverride>()
                                .eq(TeamOverride::getPromoTeamId, derivedTeam.getId())
                                .eq(TeamOverride::getVolunteerId, volunteerId)
                                .eq(TeamOverride::getAction, "OUT")
                );
                if (outCnt == 0) {
                    throw new BusinessException("该志愿者已属于其他队伍，请先将其移出");
                }
            }
        }
        // 自由组队已入队
        Long otherMember = teamMemberMapper.selectCount(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getVolunteerId, volunteerId)
                        .ne(TeamMember::getTeamId, teamId)
        );
        if (otherMember > 0) {
            throw new BusinessException("该志愿者已在自由组队队伍中");
        }

        // 同一 (队,人) 的 OUT 与 IN 互斥
        teamOverrideMapper.delete(
                new LambdaQueryWrapper<TeamOverride>()
                        .eq(TeamOverride::getPromoTeamId, teamId)
                        .eq(TeamOverride::getVolunteerId, volunteerId)
        );
        upsertOverride(teamId, volunteerId, "IN");
        log.info("老师指定成员入队: teamId={}, volunteerId={}", teamId, volunteerId);
    }

    /**
     * 老师手动将志愿者移出队伍（写 OUT 覆盖，不可撤销）
     *
     * @param universityId 老师所属大学（数据隔离）
     * @param teamId       队伍ID
     * @param volunteerId  志愿者ID
     */
    @Transactional
    public void removeMemberFromTeam(Integer universityId, Integer teamId, Integer volunteerId) {
        PromoTeam team = requireMyTeam(universityId, teamId);

        // 移出自由组队记录（若存在）
        teamMemberMapper.delete(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, teamId)
                        .eq(TeamMember::getVolunteerId, volunteerId)
        );
        // 同一 (队,人) 的 IN 与 OUT 互斥
        teamOverrideMapper.delete(
                new LambdaQueryWrapper<TeamOverride>()
                        .eq(TeamOverride::getPromoTeamId, teamId)
                        .eq(TeamOverride::getVolunteerId, volunteerId)
        );
        upsertOverride(teamId, volunteerId, "OUT");

        // 被移出者若为队长，位置留空，等待老师重新指定
        // 注意：updateById 对 null 字段默认跳过更新，必须用 UpdateWrapper 显式置空
        boolean changed = false;
        if (volunteerId.equals(team.getCaptainVolunteerId())) {
            changed = true;
        }
        if (changed) {
            promoTeamMapper.update(null, new LambdaUpdateWrapper<PromoTeam>()
                    .eq(PromoTeam::getId, teamId)
                    .set(PromoTeam::getCaptainVolunteerId, null));
        }
        log.info("老师移出成员: teamId={}, volunteerId={}", teamId, volunteerId);
    }

    /**
     * 老师指定/更换队长
     *
     * @param universityId 老师所属大学（数据隔离）
     * @param teamId       队伍ID
     * @param volunteerId  新队长志愿者ID
     */
    @Transactional
    public void assignCaptain(Integer universityId, Integer teamId, Integer volunteerId) {
        PromoTeam team = requireMyTeam(universityId, teamId);
        if (volunteerId == null) {
            throw new BusinessException("请选择队长");
        }

        // 队长必须是队伍有效成员
        List<MemberVO> members = getEffectiveMembers(team, false);
        boolean isMember = members.stream().anyMatch(m -> volunteerId.equals(m.getVolunteerId()));
        if (!isMember) {
            throw new BusinessException("该志愿者不是队伍成员，无法指定为队长");
        }

        team.setCaptainVolunteerId(volunteerId);
        promoTeamMapper.updateById(team);

        // 同步自由组队 role（若有记录）
        TeamMember tm = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, teamId)
                        .eq(TeamMember::getVolunteerId, volunteerId)
        );
        if (tm != null && !"CAPTAIN".equals(tm.getRole())) {
            tm.setRole("CAPTAIN");
            teamMemberMapper.updateById(tm);
        }
        log.info("老师指定队长: teamId={}, volunteerId={}", teamId, volunteerId);
    }

    /**
     * 老师解散队伍
     *
     * @param universityId 老师所属大学（数据隔离）
     * @param teamId       队伍ID
     */
    @Transactional
    public void deleteTeam(Integer universityId, Integer teamId) {
        PromoTeam team = requireMyTeam(universityId, teamId);
        cleanupTeam(teamId);
        log.info("老师解散队伍: universityId={}, teamId={}", universityId, teamId);
    }

    /**
     * 自动清理空队伍（成员数为0的队伍）
     *
     * 后台自动删除：当队伍的有效成员数为 0 时，删除该队伍及其关联数据。
     * 在查询队伍列表前调用，实现"查询时自动清理"。
     */
    public void cleanupEmptyTeams() {
        List<PromoTeam> allTeams = promoTeamMapper.selectList(new LambdaQueryWrapper<>());
        for (PromoTeam team : allTeams) {
            List<MemberVO> members = getEffectiveMembers(team, false);
            if (members == null || members.isEmpty()) {
                final Integer teamId = team.getId();
                final String teamCode = team.getTeamCode();
                final String teamName = team.getTeamName();
                // 每支队伍独立事务：锁行 + 二次确认 + 删除原子化（P1-7），
                // 避免"查-删"间隙老师并发添加成员导致误删。
                // 定时任务内部调用同类 @Transactional 方法不经过代理（历史坑），此处显式走事务模板
                transactionTemplate.executeWithoutResult(status -> {
                    // 锁行：对候选空队伍加 FOR UPDATE，串行化并发清理与改队操作
                    PromoTeam locked = promoTeamMapper.selectOne(
                            new LambdaQueryWrapper<PromoTeam>()
                                    .eq(PromoTeam::getId, teamId)
                                    .last("FOR UPDATE"));
                    if (locked == null) {
                        // 已被并发清理删除
                        log.info("空队伍已被并发清理，跳过: teamId={}", teamId);
                        return;
                    }
                    // 二次确认（锁内复查）：有效成员 = team_member 记录 或 IN override（老师手动指定入队），
                    // 任一存在即不删（并发场景老师正在添加成员）
                    Long memberCnt = teamMemberMapper.selectCount(
                            new LambdaQueryWrapper<TeamMember>()
                                    .eq(TeamMember::getTeamId, team.getId()));
                    Long inCnt = teamOverrideMapper.selectCount(
                            new LambdaQueryWrapper<TeamOverride>()
                                    .eq(TeamOverride::getPromoTeamId, team.getId())
                                    .eq(TeamOverride::getAction, "IN"));
                    if (memberCnt > 0 || inCnt > 0) {
                        log.info("队伍已有成员，跳过清理: teamId={}", teamId);
                        return;
                    }
                    cleanupTeam(teamId);
                    log.info("自动清理空队伍: teamId={}, teamCode={}, teamName={}", teamId, teamCode, teamName);
                });
            }
        }
    }

    /**
     * 删除队伍及其关联数据（团队解散/空队伍清理共用）
     *
     * @param teamId 队伍ID
     */
    @Transactional
    public void cleanupTeam(Integer teamId) {
        if (teamId == null) {
            return;
        }
        promoTeamMapper.deleteById(teamId);
        teamOverrideMapper.delete(
                new LambdaQueryWrapper<TeamOverride>().eq(TeamOverride::getPromoTeamId, teamId)
        );
        teamMemberMapper.delete(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, teamId)
        );
        roleApplicationMapper.delete(
                new LambdaQueryWrapper<TeamRoleApplication>().eq(TeamRoleApplication::getTeamId, teamId)
        );
    }

    /**
     * 定时清理空队伍（后台自动删除）
     *
     * 每天凌晨 2:00 执行一次，删除所有成员数为 0 的队伍。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledCleanupEmptyTeams() {
        log.info("定时清理空队伍任务开始");
        try {
            cleanupEmptyTeams();
        } catch (Exception e) {
            log.error("定时清理空队伍任务异常", e);
        }
    }

    /**
     * 判断队伍是否为自由组队队伍（存在 team_member 记录）
     */
    private boolean isFreeTeam(Integer teamId) {
        return teamMemberMapper.selectCount(
                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getTeamId, teamId)) > 0;
    }

    /**
     * 校验队伍存在且属于本校（数据隔离），返回队伍
     */
    private PromoTeam requireMyTeam(Integer universityId, Integer teamId) {
        PromoTeam team = promoTeamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("队伍不存在");
        }
        if (!universityId.equals(team.getUniversityId())) {
            throw new BusinessException("无权操作其他大学的队伍");
        }
        return team;
    }

    /**
     * 获取队伍详情（老师端）
     *
     * @param universityId 老师所属大学（数据隔离）
     * @param teamId       队伍ID
     * @return 队伍完整信息（含成员电话等）
     */
    public TeamVO getTeamDetail(Integer universityId, Integer teamId) {
        PromoTeam team = requireMyTeam(universityId, teamId);
        // 老师端详情：唯一允许返回手机号的入口
        return buildTeamVO(team, true);
    }

    /**
     * 修改队伍目标高中
     *
     * 成员处理：将该队所有有效成员的 target_high_school_id 同步改为新高中（成员跟着队伍走），
     * 被移出（OUT）的成员不动；若队长不属于新组合，自动补 IN 使其保持成员资格。
     *
     * 防连坐说明（重要）：
     *   volunteer.target_high_school_id 是一个"一字段三用"的耦合字段：
     *     1. 报名时填写的目标高中（业务展示）
     *     2. joinTeam 组队资格的比对基准（必须与队伍高中一致）
     *     3. 自动队成员关系的派生维度（selectTeamMembers 按
     *        university_id + target_high_school_id 匹配组合派生成员）
     *   因此批量改 target 会连锁影响"其他队伍的派生成员关系"。
     *   本方法在改前做组合校验，防止：
     *     - 成员同时是其他组合队伍的派生成员，改 target 后从该队伍静默消失（连坐）
     *     - 跨大学成员（joinTeam 漏洞历史数据）被批量更新遗漏，target 与队伍不一致
     *   治本方案（解除字段过载）见 REVIEW-2026-08-09.md。
     *
     * @param universityId   老师所属大学（数据隔离）
     * @param teamId         队伍ID
     * @param newHighSchoolId 新目标高中ID
     */
    @Transactional
    public void updateTeamHighSchool(Integer universityId, Integer teamId, Integer newHighSchoolId) {
        PromoTeam team = requireMyTeam(universityId, teamId);
        if (newHighSchoolId == null || highSchoolMapper.selectById(newHighSchoolId) == null) {
            throw new BusinessException("目标高中不存在");
        }
        if (newHighSchoolId.equals(team.getHighSchoolId())) {
            throw new BusinessException("目标高中未变化");
        }
        // uk_team 约束：新组合下不能已有其他队伍
        Long exist = promoTeamMapper.selectCount(
                new LambdaQueryWrapper<PromoTeam>()
                        .eq(PromoTeam::getHighSchoolId, newHighSchoolId)
                        .eq(PromoTeam::getUniversityId, universityId)
                        .ne(PromoTeam::getId, teamId)
        );
        if (exist > 0) {
            throw new BusinessException("该学校已有队伍，无法修改");
        }

        // ===== 防连坐校验：改 target 前确认每个成员只属于本队 =====
        // 成员 = 本队有效成员（不含 OUT）。逐人检查其是否同时被其他队伍引用：
        //   A. 是其他队伍的自由队成员（team_member）
        //   B. 被其他队伍 IN 指定
        //   C. 是其他自动/手动组队队伍的派生成员（其 target 组合已有他队，且未被 OUT）
        // 任一命中即拒绝修改，避免批量更新 target 后"成员从其他队伍静默消失"。
        // 注意：C 场景下即使成员在本队，改 target 也会让他在其他队伍的派生关系失效。
        List<MemberVO> members = getEffectiveMembers(team, false);
        for (MemberVO m : members) {
            Integer vId = m.getVolunteerId();

            Long otherTeamMember = teamMemberMapper.selectCount(
                    new LambdaQueryWrapper<TeamMember>()
                            .eq(TeamMember::getVolunteerId, vId)
                            .ne(TeamMember::getTeamId, teamId));
            if (otherTeamMember > 0) {
                throw new BusinessException("成员「" + m.getName() + "」同时是其他队伍的自由队成员，请先移出再修改目标高中");
            }

            Long otherIn = teamOverrideMapper.selectCount(
                    new LambdaQueryWrapper<TeamOverride>()
                            .eq(TeamOverride::getVolunteerId, vId)
                            .eq(TeamOverride::getAction, "IN")
                            .ne(TeamOverride::getPromoTeamId, teamId));
            if (otherIn > 0) {
                throw new BusinessException("成员「" + m.getName() + "」已被指定到其他队伍，请先移出再修改目标高中");
            }

            Volunteer v = volunteerMapper.selectById(vId);
            if (v == null) {
                continue;
            }
            // 跨大学成员：批量更新带 eq(universityId) 会漏掉他，导致 target 与队伍不一致
            if (!universityId.equals(v.getUniversityId())) {
                throw new BusinessException("成员「" + v.getName() + "」不属于本校，请先移出再修改目标高中");
            }
            // 派生成员连坐：成员 target 组合存在其他队伍且未 OUT
            if (v.getTargetHighSchoolId() != null) {
                PromoTeam derivedTeam = promoTeamMapper.selectOne(
                        new LambdaQueryWrapper<PromoTeam>()
                                .eq(PromoTeam::getUniversityId, v.getUniversityId())
                                .eq(PromoTeam::getHighSchoolId, v.getTargetHighSchoolId())
                                .ne(PromoTeam::getId, teamId)
                );
                if (derivedTeam != null && !isFreeTeam(derivedTeam.getId())) {
                    Long outCnt = teamOverrideMapper.selectCount(
                            new LambdaQueryWrapper<TeamOverride>()
                                    .eq(TeamOverride::getPromoTeamId, derivedTeam.getId())
                                    .eq(TeamOverride::getVolunteerId, vId)
                                    .eq(TeamOverride::getAction, "OUT"));
                    if (outCnt == 0) {
                        throw new BusinessException("成员「" + v.getName() + "」同时属于「" + derivedTeam.getTeamName() + "」队伍的派生组合，无法修改目标高中");
                    }
                }
            }
        }

        // 更新队伍目标高中
        team.setHighSchoolId(newHighSchoolId);
        promoTeamMapper.updateById(team);

        // 同步队伍有效成员的 target_high_school_id（成员跟着队伍走）
        if (!members.isEmpty()) {
            List<Integer> memberIds = members.stream()
                    .map(MemberVO::getVolunteerId).collect(Collectors.toList());
            volunteerMapper.update(null, new LambdaUpdateWrapper<Volunteer>()
                    .in(Volunteer::getId, memberIds)
                    .eq(Volunteer::getUniversityId, universityId)
                    .set(Volunteer::getTargetHighSchoolId, newHighSchoolId));
        }

        // 若队长不属于新组合（目标高中不同），自动补 IN 保持成员资格
        if (team.getCaptainVolunteerId() != null) {
            Volunteer captain = volunteerMapper.selectById(team.getCaptainVolunteerId());
            if (captain != null && !newHighSchoolId.equals(captain.getTargetHighSchoolId())) {
                upsertOverride(teamId, captain.getId(), "IN");
            }
        }

        log.info("队伍目标高中已修改: universityId={}, teamId={}, newHighSchoolId={}", universityId, teamId, newHighSchoolId);
    }

    /**
     * 写入/更新 override 记录（同一 (队,人,动作) 唯一）
     */
    private void upsertOverride(Integer teamId, Integer volunteerId, String action) {
        // 唯一键为 (promo_team_id, volunteer_id)（不含 action），因此按 (队,人) 查重，
        // 已有任意动作记录即不重复写入，避免与 addMemberToTeam 先删后插的语义冲突。
        TeamOverride existing = teamOverrideMapper.selectOne(
                new LambdaQueryWrapper<TeamOverride>()
                        .eq(TeamOverride::getPromoTeamId, teamId)
                        .eq(TeamOverride::getVolunteerId, volunteerId)
        );
        if (existing != null) {
            return;
        }
        TeamOverride o = new TeamOverride();
        o.setPromoTeamId(teamId);
        o.setVolunteerId(volunteerId);
        o.setAction(action);
        teamOverrideMapper.insert(o);
    }

    /**
     * 按大学ID查询所有队伍（老师查看本校组队情况）
     */
    public List<TeamVO> getTeamsByUniversity(Integer universityId) {
        List<PromoTeam> teams = promoTeamMapper.selectList(
                new LambdaQueryWrapper<PromoTeam>()
                        .eq(PromoTeam::getUniversityId, universityId)
                        .orderByAsc(PromoTeam::getHighSchoolId)
        );
        return teams.stream().map(t -> buildTeamVO(t, false)).collect(Collectors.toList());
    }

    /**
     * 查询所有队伍（管理员查看全部队伍列表，含队长/成员/高中/大学信息）
     */
    public List<TeamVO> getAllTeams() {
        // 不再主动触发空队伍清理：查询路径删除会与"老师添加成员"并发竞争，误删有效队伍。
        // 空队伍统一由 @Scheduled 定时任务清理。
        List<PromoTeam> teams = promoTeamMapper.selectList(
                new LambdaQueryWrapper<PromoTeam>()
                        .orderByAsc(PromoTeam::getUniversityId)
                        .orderByAsc(PromoTeam::getHighSchoolId)
        );
        // 管理员列表不返回成员手机号（普通成员手机号仅同队详情可见）
        return teams.stream().map(t -> buildTeamVO(t, false)).collect(Collectors.toList());
    }

    /**
     * 按大学ID和高中ID查询未组队的已审核志愿者
     *
     * 未组队判定：组合下所有已审核志愿者 - 任意队伍的有效成员
     * 队伍类型区分：自由组队队伍（有 team_member）只算 team_member 成员；
     *              自动/手动组队队伍（无 team_member）算组合派生成员。
     */
    public List<Volunteer> getUngroupedVolunteers(Integer universityId, Integer highSchoolId) {
        // 查找该大学-高中组合下所有已审核志愿者
        List<Volunteer> allApproved = volunteerMapper.selectList(
                new LambdaQueryWrapper<Volunteer>()
                        .eq(Volunteer::getUniversityId, universityId)
                        .eq(Volunteer::getTargetHighSchoolId, highSchoolId)
                        .eq(Volunteer::getStatus, "APPROVED")
        );
        if (allApproved.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> allVolunteerIds = allApproved.stream()
                .map(Volunteer::getId).collect(Collectors.toList());

        // 1. 该组合下的所有队伍
        List<PromoTeam> comboTeams = promoTeamMapper.selectList(
                new LambdaQueryWrapper<PromoTeam>()
                        .eq(PromoTeam::getUniversityId, universityId)
                        .eq(PromoTeam::getHighSchoolId, highSchoolId)
        );
        // 自由组队队伍（有 team_member 记录）不产生"派生成员"，仅 team_member 算成员
        List<PromoTeam> autoTeams = new ArrayList<>();
        for (PromoTeam t : comboTeams) {
            if (!isFreeTeam(t.getId())) {
                autoTeams.add(t);
            }
        }

        // 2. 该组合志愿者的所有 override 记录
        List<TeamOverride> overrides = teamOverrideMapper.selectList(
                new LambdaQueryWrapper<TeamOverride>()
                        .in(TeamOverride::getVolunteerId, allVolunteerIds)
        );
        // volunteerId -> 该志愿者被强制移出/加入的队伍ID集合
        Map<Integer, java.util.Set<Integer>> outTeamIds = new java.util.HashMap<>();
        Map<Integer, java.util.Set<Integer>> inTeamIds = new java.util.HashMap<>();
        for (TeamOverride o : overrides) {
            if ("OUT".equals(o.getAction())) {
                outTeamIds.computeIfAbsent(o.getVolunteerId(), k -> new java.util.HashSet<>()).add(o.getPromoTeamId());
            } else if ("IN".equals(o.getAction())) {
                inTeamIds.computeIfAbsent(o.getVolunteerId(), k -> new java.util.HashSet<>()).add(o.getPromoTeamId());
            }
        }

        // 3. team_member 表（自由组队）已入队的人
        List<TeamMember> inTeamMembers = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>()
                        .in(TeamMember::getVolunteerId, allVolunteerIds)
        );
        java.util.Set<Integer> memberIds = inTeamMembers.stream()
                .map(TeamMember::getVolunteerId).collect(Collectors.toSet());

        // 未组队 = 组合下 APPROVED - 有效成员
        return allApproved.stream()
                .filter(v -> {
                    // 有 IN 记录 => 已组队（被老师指定到某队）
                    if (inTeamIds.containsKey(v.getId())) {
                        return false;
                    }
                    // 自由组队已入队 => 已组队
                    if (memberIds.contains(v.getId())) {
                        return false;
                    }
                    // 组合下存在自动/手动组队队伍 => 派生成员（除非该组合队伍都被 OUT）
                    if (!autoTeams.isEmpty()) {
                        java.util.Set<Integer> outs = outTeamIds.get(v.getId());
                        for (PromoTeam t : autoTeams) {
                            if (outs == null || !outs.contains(t.getId())) {
                                // 存在未被移出的自动组队队伍 => 已组队
                                return false;
                            }
                        }
                        // 组合下所有自动组队队伍都被 OUT => 未组队
                        return true;
                    }
                    // 组合下无自动/手动队伍（仅自由组队队伍或没有队伍）=> 未组队
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取老师本校所有组队状态（按高中分组）
     * 优化：每个高中的查询在线程池中并行执行
     */
    public List<Map<String, Object>> getTeamStatusByUniversity(Integer universityId) {
        if (universityId == null) {
            return new ArrayList<>();
        }

        try {
            // 查询本校所有队伍（主线程执行）
            List<TeamVO> teams = getTeamsByUniversity(universityId);

            // 查询本校所有已审核志愿者
            List<Volunteer> volunteers = volunteerMapper.selectList(
                    new LambdaQueryWrapper<Volunteer>()
                            .eq(Volunteer::getUniversityId, universityId)
                            .eq(Volunteer::getStatus, "APPROVED")
            );

            // 手动提取高中ID去重
            java.util.Set<Integer> hsIdSet = new java.util.HashSet<>();
            for (Volunteer v : volunteers) {
                if (v.getTargetHighSchoolId() != null) {
                    hsIdSet.add(v.getTargetHighSchoolId());
                }
            }

            if (hsIdSet.isEmpty()) {
                return new ArrayList<>();
            }

            // 并行处理每个高中的组队状态查询
            List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();
            for (Integer hsId : hsIdSet) {
                final Integer finalHsId = hsId;
                futures.add(CompletableFuture.supplyAsync(
                        () -> buildHighSchoolGroup(universityId, finalHsId, teams),
                        taskExecutor
                ));
            }

            // 等待所有并行任务完成，按顺序汇总
            List<Map<String, Object>> result = new ArrayList<>();
            for (CompletableFuture<Map<String, Object>> future : futures) {
                try {
                    Map<String, Object> group = future.get(30, TimeUnit.SECONDS);
                    if (group != null) {
                        result.add(group);
                    }
                } catch (Exception e) {
                    log.error("查询高中组队状态失败: universityId={}, hsId=unknown", universityId, e);
                }
            }

            return result;
        } catch (Exception e) {
            log.error("getTeamStatusByUniversity 查询失败: universityId={}", universityId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 构建单个高中的组队状态数据（在线程池中执行）
     */
    private Map<String, Object> buildHighSchoolGroup(Integer universityId, Integer hsId, List<TeamVO> teams) {
        Map<String, Object> group = new LinkedHashMap<>();

        // 高中名称
        var hs = highSchoolMapper.selectById(hsId);
        group.put("highSchoolId", hsId);
        group.put("highSchoolName", hs != null ? hs.getName() : "未知高中");

        // 该高中的队伍
        List<TeamVO> hsTeams = new ArrayList<>();
        for (TeamVO t : teams) {
            if (hsId.equals(t.getHighSchoolId())) {
                hsTeams.add(t);
            }
        }
        group.put("teams", hsTeams);
        group.put("teamCount", hsTeams.size());

        // 该高中已组队人数
        int groupedCount = 0;
        for (TeamVO t : hsTeams) {
            groupedCount += (t.getMemberCount() != null ? t.getMemberCount() : 0);
        }
        group.put("groupedCount", groupedCount);

        // 该高中未组队人员
        List<Volunteer> ungrouped = getUngroupedVolunteers(universityId, hsId);
        List<Map<String, Object>> ungroupedList = new ArrayList<>();
        for (Volunteer v : ungrouped) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", v.getId());
            m.put("name", v.getName());
            m.put("major", v.getMajor());
            m.put("grade", v.getGrade());
            m.put("phone", v.getPhone());
            ungroupedList.add(m);
        }
        group.put("ungrouped", ungroupedList);
        group.put("ungroupedCount", ungrouped.size());

        return group;
    }
}
