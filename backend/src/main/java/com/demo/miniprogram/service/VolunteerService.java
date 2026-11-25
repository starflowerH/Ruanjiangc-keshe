package com.demo.miniprogram.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.miniprogram.common.BusinessException;
import com.demo.miniprogram.common.ResultCode;
import com.demo.miniprogram.dto.AuditDTO;
import com.demo.miniprogram.dto.PageQueryDTO;
import com.demo.miniprogram.dto.VolunteerApplyDTO;
import com.demo.miniprogram.entity.*;
import com.demo.miniprogram.mapper.*;
import com.demo.miniprogram.vo.MemberVO;
import com.demo.miniprogram.vo.TeamVO;
import com.demo.miniprogram.vo.VolunteerPublicVO;
import com.demo.miniprogram.vo.VolunteerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.demo.miniprogram.mapper.VolunteerHighSchoolMapper;

/**
 * 志愿者服务 - 处理报名、审核、组队等核心业务
 *
 * 业务阶段对应：
 *   阶段1（报名期） → apply()     大学生填写表单提交报名
 *   阶段2（确认期） → audit()     老师审核确认名单
 *   阶段3（组队期） → buildTeams() 系统自动分组，指定队长
 *
 * @author demo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VolunteerService {

    // @Lazy 打破循环依赖：TeamService 构造期也间接依赖 VolunteerService 的场景由代理延迟注入
    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private com.demo.miniprogram.service.TeamService teamService;
    private final VolunteerMapper volunteerMapper;
    private final PromoTeamMapper promoTeamMapper;
    private final VolunteerCustomValueMapper customValueMapper;
    private final CustomFieldDefinitionMapper fieldDefMapper;
    private final UniversityMapper universityMapper;
    private final HighSchoolMapper highSchoolMapper;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final VolunteerHighSchoolMapper volunteerHighSchoolMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final TeamRoleApplicationMapper roleApplicationMapper;
    private final TeamOverrideMapper teamOverrideMapper;
    private final ForumModeratorGrantMapper forumModeratorGrantMapper;
    // 事务模板：buildSingleTeam 为同类自调用（线程池内），@Transactional 不经过代理（历史坑），
    // 由 buildTeams 显式事务模板包裹，保证每个组合独立事务
    private final TransactionTemplate transactionTemplate;

    /** 取消后可撤销的时限（天） */
    private static final int UNDO_CANCEL_DAYS = 2;

    /* ==================== 阶段1：报名期 ==================== */

    /**
     * 大学生报名 - 填写表单提交申请
     *
     * 业务规则：
     *   1. 同一openid在同一大学下只能报名一次（防止重复提交）
     *   2. 目标高中必须从系统名册中选择（非手动输入，保证数据一致性）
     *   3. 支持大学自定义字段的动态填写
     *
     * @param openid 微信OpenID（从JWT中获取）
     * @param dto    报名表单数据
     * @return 报名记录
     */
    @Transactional(rollbackFor = Exception.class)
    public Volunteer apply(String openid, VolunteerApplyDTO dto) {
        // ---- 规则1：并发安全判重 ----
        try {
            // 锁定读：一次锁住该 openid 名下所有报名记录行。
            // 与 undoCancel 的锁定读使用相同的加锁顺序（先锁行再判断），避免死锁环；
            // 当前读保证能看到"撤销取消"刚提交的最新状态。
            List<Volunteer> existing = volunteerMapper.selectList(
                    new LambdaQueryWrapper<Volunteer>()
                            .eq(Volunteer::getOpenid, openid)
                            .last("FOR UPDATE"));

            // 只有待审核和已通过记录会阻止再次报名；被拒绝的记录允许重新提交。
            Volunteer active = null;
            for (Volunteer v : existing) {
                String s = v.getStatus();
                if ("APPROVED".equals(s) || "PENDING".equals(s)) {
                    active = v;
                    break;
                }
            }
            if (active != null) {
                String reason = "APPROVED".equals(active.getStatus())
                        ? "您已是志愿者，无需重复报名"
                        : "您已有报名正在审核中，请勿重复提交";
                throw new BusinessException(ResultCode.ALREADY_APPLIED, reason);
            }

            // 将已经拒绝或取消的旧记录标记为 REPLACED，保留历史审核记录并允许创建新报名。
            for (Volunteer v : existing) {
                if ("CANCELLED".equals(v.getStatus()) || "REJECTED".equals(v.getStatus())) {
                    volunteerMapper.update(null, new LambdaUpdateWrapper<Volunteer>()
                            .eq(Volunteer::getId, v.getId())
                            .in(Volunteer::getStatus, "CANCELLED", "REJECTED")
                            .set(Volunteer::getStatus, "REPLACED"));
                }
            }
        } catch (org.springframework.dao.DeadlockLoserDataAccessException e) {
            // 兜底：理论上已通过统一锁定顺序消除死锁，此处防止极端场景返回 500
            throw new BusinessException(ResultCode.AUDIT_STATUS_ERROR, "操作冲突，请刷新后重试");
        }

        // ---- 必填自定义字段完整性校验：以数据库定义为准，防止提交时跳过必填字段 ----
        Map<Integer, String> customFields = dto.getCustomFields();
        List<CustomFieldDefinition> requiredFields = fieldDefMapper.selectList(
                new LambdaQueryWrapper<CustomFieldDefinition>()
                        .eq(CustomFieldDefinition::getUniversityId, dto.getUniversityId())
                        .eq(CustomFieldDefinition::getIsRequired, 1));
        for (CustomFieldDefinition fd : requiredFields) {
            String value = customFields == null ? null : customFields.get(fd.getId());
            if (value == null || value.isBlank()) {
                throw new BusinessException("请填写必填项：" + fd.getFieldLabel());
            }
        }

        // ---- 步骤2：校验固定报名字段并创建报名记录 ----
        String phone = dto.getPhone() == null ? "" : dto.getPhone().trim();
        String email = dto.getEmail() == null ? "" : dto.getEmail().trim();
        if (phone.isEmpty() && email.isEmpty()) {
            throw new BusinessException("电话或邮箱至少填写一项");
        }
        if (!phone.isEmpty() && !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("请输入正确的手机号");
        }
        if (!email.isEmpty() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new BusinessException("请输入正确的邮箱地址");
        }
        String gender = dto.getGender().trim();
        if (!List.of("男", "女", "其他").contains(gender)) {
            throw new BusinessException("请选择正确的性别");
        }

        Volunteer volunteer = new Volunteer();
        volunteer.setOpenid(openid);
        volunteer.setName(dto.getName().trim());
        volunteer.setPhone(phone.isEmpty() ? null : phone);
        volunteer.setWechatId(dto.getWechatId());
        volunteer.setStudentNo(dto.getStudentNo().trim());
        volunteer.setGender(dto.getGender().trim());
        volunteer.setEmail(email.isEmpty() ? null : email);
        volunteer.setUniversityId(dto.getUniversityId());
        volunteer.setGrade(dto.getGrade());
        volunteer.setMajor(dto.getMajor());
        volunteer.setTargetHighSchoolId(dto.getTargetHighSchoolId());
        volunteer.setIsCaptainApplicant(dto.getIsCaptainApplicant());
        volunteer.setStatus("PENDING"); // 默认待审核
        volunteerMapper.insert(volunteer);

        // ---- 目标高中选填：填写时才同步写入多对多关联表 ----
        if (dto.getTargetHighSchoolId() != null) {
            VolunteerHighSchool vhs = new VolunteerHighSchool();
            vhs.setVolunteerId(volunteer.getId());
            vhs.setHighSchoolId(dto.getTargetHighSchoolId());
            volunteerHighSchoolMapper.insert(vhs);
        }

        // ---- 步骤3：保存自定义字段值 ----
        if (CollUtil.isNotEmpty(customFields)) {
            for (Map.Entry<Integer, String> entry : customFields.entrySet()) {
                // 校验字段定义存在性
                CustomFieldDefinition fieldDef = fieldDefMapper.selectById(entry.getKey());
                if (fieldDef == null || !fieldDef.getUniversityId().equals(dto.getUniversityId())) {
                    throw new BusinessException("自定义字段配置异常，请刷新重试");
                }
                // 必填校验
                if (fieldDef.getIsRequired() == 1 && (entry.getValue() == null || entry.getValue().isBlank())) {
                    throw new BusinessException("请填写必填项：" + fieldDef.getFieldLabel());
                }

                VolunteerCustomValue cv = new VolunteerCustomValue();
                cv.setVolunteerId(volunteer.getId());
                cv.setFieldId(entry.getKey());
                cv.setFieldValue(entry.getValue());
                customValueMapper.insert(cv);
            }
        }

        log.info("志愿者报名成功: name={}, universityId={}, targetHighSchoolId={}",
                dto.getName(), dto.getUniversityId(), dto.getTargetHighSchoolId());
        return volunteer;
    }

    /* ==================== 阶段2：名单确认期 ==================== */

    /**
     * 审核志愿者报名（通过/拒绝）
     *
     * 权限控制：
     *   - TEACHER角色：只能审核自己大学的报名
     *   - 管理员角色：可审核所有报名
     *
     * @param auditDTO 审核参数
     * @param adminUniversityId 管理员的大学ID（TEACHER角色时有值）
     */
    @Transactional(rollbackFor = Exception.class)
    public void audit(AuditDTO auditDTO, Integer adminUniversityId) {
        Volunteer volunteer = volunteerMapper.selectById(auditDTO.getId());
        if (volunteer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报名记录不存在");
        }

        // TEACHER角色只能审核自己大学的报名
        if (adminUniversityId != null
                && !adminUniversityId.equals(volunteer.getUniversityId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权审核其他大学的报名");
        }

        // 状态校验：只能审核待处理的报名
        if (!"PENDING".equals(volunteer.getStatus())) {
            throw new BusinessException(ResultCode.AUDIT_STATUS_ERROR, "该报名已审核，请勿重复操作");
        }

        // 审核状态校验
        String status = auditDTO.getStatus();
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无效的审核状态: " + status);
        }

        // 更新审核状态
        volunteer.setStatus(status);
        volunteerMapper.updateById(volunteer);

        log.info("志愿者审核完成: id={}, status={}, auditorId={}",
                auditDTO.getId(), auditDTO.getStatus(), adminUniversityId);
    }

    /**
     * 取消已通过志愿者的资格（老师/管理员操作）
     *
     * 业务规则：
     *   - 仅 APPROVED 状态的志愿者可被取消
     *   - 取消后自动清理队伍关系：自由组队删除 team_member；队长/副队长位清空（留空待老师指定）
     *   - 历史问答/论坛/评论记录保留不变
     *   - 大学生可重新报名（旧记录标记 REPLACED），老师可在 2 天内撤销取消
     *
     * @param volunteerId 志愿者ID
     * @param adminUniversityId 管理员的大学ID（TEACHER角色时有值，ADMIN为null）
     * @param reason 取消理由（选填）
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Integer volunteerId, Integer adminUniversityId, String reason) {
        Volunteer volunteer = volunteerMapper.selectById(volunteerId);
        if (volunteer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "志愿者不存在");
        }
        // TEACHER角色只能操作本校志愿者
        if (adminUniversityId != null
                && !adminUniversityId.equals(volunteer.getUniversityId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作其他大学的志愿者");
        }

        // 目标记录取消（含队伍关系清理）
        doCancel(volunteer, reason);

        // 连带取消：同 openid 下其他仍 APPROVED 的记录（一人一个志愿者身份，
        // 防止历史多校报名记录导致"被取消者仍有其他有效资格"）
        List<Volunteer> sameOpenidApproved = volunteerMapper.selectList(
                new LambdaQueryWrapper<Volunteer>()
                        .eq(Volunteer::getOpenid, volunteer.getOpenid())
                        .eq(Volunteer::getStatus, "APPROVED")
                        .ne(Volunteer::getId, volunteerId));
        for (Volunteer v : sameOpenidApproved) {
            doCancel(v, reason);
            log.info("连带取消同openid的其他志愿者资格: id={}, name={}", v.getId(), v.getName());
        }

        log.info("志愿者资格已取消: id={}, auditorId={}, reason={}",
                volunteerId, adminUniversityId, reason);
    }

    /**
     * 取消单条志愿者记录：条件更新状态 + 清理队伍关系（供目标记录与同openid连带记录共用）
     */
    private void doCancel(Volunteer volunteer, String reason) {
        Integer volunteerId = volunteer.getId();
        // 条件更新防并发：仅 APPROVED 可被取消（与撤销/重复审核互斥）
        int affected = volunteerMapper.update(null, new LambdaUpdateWrapper<Volunteer>()
                .eq(Volunteer::getId, volunteerId)
                .eq(Volunteer::getStatus, "APPROVED")
                .set(Volunteer::getStatus, "CANCELLED")
                .set(Volunteer::getCancelledAt, LocalDateTime.now())
                .set(Volunteer::getCancelReason, reason));
        if (affected == 0) {
            // 条件更新失败：重查当前状态，给出具体原因（竞态输方）
            Volunteer latest = volunteerMapper.selectById(volunteerId);
            if (latest != null && "CANCELLED".equals(latest.getStatus())) {
                throw new BusinessException(ResultCode.AUDIT_STATUS_ERROR, "该志愿者已被取消资格，请勿重复操作");
            }
            throw new BusinessException(ResultCode.AUDIT_STATUS_ERROR, "该志愿者状态已变化，请刷新后重试");
        }

        // 清理队伍关系（自由组队：删除 team_member 成员记录）
        TeamMember member = teamMemberMapper.selectOne(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getVolunteerId, volunteerId)
        );
        if (member != null) {
            teamMemberMapper.deleteById(member.getId());
            log.info("取消志愿者后移出队伍: volunteerId={}, teamId={}", volunteerId, member.getTeamId());
        }

        // 自动/手动组队成员清理：无 team_member 记录，写 OUT override 到其组合派生队伍
        // （组合 = university_id + target_high_school_id），防止取消后仍被视为成员。
        // 先删旧 override 再写 OUT：该志愿者若已有 IN 覆盖（老师指定入队），
        // 不删则 OUT 写入会被"已有记录"跳过（upsertOverride 语义），取消即失效
        if (volunteer.getUniversityId() != null && volunteer.getTargetHighSchoolId() != null) {
            List<PromoTeam> comboTeams = promoTeamMapper.selectList(
                    new LambdaQueryWrapper<PromoTeam>()
                            .eq(PromoTeam::getUniversityId, volunteer.getUniversityId())
                            .eq(PromoTeam::getHighSchoolId, volunteer.getTargetHighSchoolId())
            );
            for (PromoTeam comboTeam : comboTeams) {
                teamOverrideMapper.delete(
                        new LambdaQueryWrapper<TeamOverride>()
                                .eq(TeamOverride::getPromoTeamId, comboTeam.getId())
                                .eq(TeamOverride::getVolunteerId, volunteerId));
                TeamOverride out = new TeamOverride();
                out.setPromoTeamId(comboTeam.getId());
                out.setVolunteerId(volunteerId);
                out.setAction("OUT");
                teamOverrideMapper.insert(out);
                log.info("取消志愿者后写 OUT override: volunteerId={}, teamId={}", volunteerId, comboTeam.getId());
            }
        }

        // 清理队长引用（自动组队与自由组队都可能产生，统一清空留待老师指定）
        // 注意：必须用 LambdaUpdateWrapper.set(..., null) 显式置空，
        // updateById 默认更新策略会跳过 null 字段，导致队长位无法真正清空（历史坑，见 removeMemberFromTeam）
        int clearedCaptain = promoTeamMapper.update(null, new LambdaUpdateWrapper<PromoTeam>()
                .eq(PromoTeam::getCaptainVolunteerId, volunteerId)
                .set(PromoTeam::getCaptainVolunteerId, null));
        if (clearedCaptain > 0) {
            log.info("取消志愿者后清空队长位: volunteerId={}, captain={}",
                    volunteerId, clearedCaptain);
        }

        // 清理该志愿者遗留的待审核队长/转让申请
        List<TeamRoleApplication> pendingApps = roleApplicationMapper.selectList(
                new LambdaQueryWrapper<TeamRoleApplication>()
                        .eq(TeamRoleApplication::getVolunteerId, volunteerId)
                        .eq(TeamRoleApplication::getStatus, "PENDING"));
        for (TeamRoleApplication app : pendingApps) {
            app.setStatus("REJECTED");
            app.setReviewedAt(LocalDateTime.now());
            roleApplicationMapper.updateById(app);
        }

        // 取消资格后立即停用论坛审核授权（防取消后仍可审核帖子/处理举报/设置精华）
        forumModeratorGrantMapper.update(null, new LambdaUpdateWrapper<com.demo.miniprogram.entity.ForumModeratorGrant>()
                .eq(com.demo.miniprogram.entity.ForumModeratorGrant::getVolunteerId, volunteerId)
                .set(com.demo.miniprogram.entity.ForumModeratorGrant::getActive, 0));
    }

    /**
     * 撤销取消（恢复志愿者资格）
     *
     * 业务规则：
     *   - 仅 CANCELLED 状态、且取消未超过 {@value #UNDO_CANCEL_DAYS} 天可撤销
     *   - 已被重新报名取代（REPLACED）的记录不可撤销
     *   - 撤销只恢复状态，不恢复队伍关系（自由组队需重新入队，队长位留空待老师指定）
     *
     * @param volunteerId 志愿者ID
     * @param adminUniversityId 管理员的大学ID（TEACHER角色时有值，ADMIN为null）
     */
    @Transactional(rollbackFor = Exception.class)
    public void undoCancel(Integer volunteerId, Integer adminUniversityId) {
        try {
            // 锁定读（当前读）：一次锁住该行，与重新报名的锁定顺序一致，避免死锁环；
            // 当前读保证能看到"重新报名"刚提交的 REPLACED 状态（普通快照读看不到）
            Volunteer volunteer = volunteerMapper.selectOne(
                    new LambdaQueryWrapper<Volunteer>()
                            .eq(Volunteer::getId, volunteerId)
                            .last("FOR UPDATE"));
            if (volunteer == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "志愿者不存在");
            }
            // TEACHER角色只能操作本校志愿者
            if (adminUniversityId != null
                    && !adminUniversityId.equals(volunteer.getUniversityId())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "无权操作其他大学的志愿者");
            }

            if (!"CANCELLED".equals(volunteer.getStatus())) {
                if ("REPLACED".equals(volunteer.getStatus())) {
                    throw new BusinessException(ResultCode.AUDIT_STATUS_ERROR, "该生已重新提交报名，无法撤销取消");
                }
                throw new BusinessException(ResultCode.AUDIT_STATUS_ERROR, "该志愿者当前状态不可撤销取消");
            }
            // 时限校验
            LocalDateTime cancelledAt = volunteer.getCancelledAt();
            if (cancelledAt == null
                    || cancelledAt.isBefore(LocalDateTime.now().minusDays(UNDO_CANCEL_DAYS))) {
                throw new BusinessException(ResultCode.AUDIT_STATUS_ERROR,
                        "已超过" + UNDO_CANCEL_DAYS + "天可撤销时限，请让该生重新报名");
            }

            // 当前已持有行锁，条件更新必然成功（防重复操作仍保留条件，失败即竞态输方）
            int affected = volunteerMapper.update(null, new LambdaUpdateWrapper<Volunteer>()
                    .eq(Volunteer::getId, volunteerId)
                    .eq(Volunteer::getStatus, "CANCELLED")
                    .set(Volunteer::getStatus, "APPROVED")
                    .set(Volunteer::getCancelledAt, null)
                    .set(Volunteer::getCancelReason, null));
            if (affected == 0) {
                // 条件更新失败：锁定读重查当前状态，给出具体原因（竞态输方）
                Volunteer latest = volunteerMapper.selectOne(
                        new LambdaQueryWrapper<Volunteer>()
                                .eq(Volunteer::getId, volunteerId)
                                .last("FOR UPDATE"));
                String reason;
                if (latest == null) {
                    reason = "该志愿者不存在，请刷新后重试";
                } else if ("REPLACED".equals(latest.getStatus())) {
                    reason = "该生已重新提交报名，无法撤销取消";
                } else if ("APPROVED".equals(latest.getStatus())) {
                    reason = "该志愿者已恢复资格，请勿重复操作";
                } else {
                    reason = "该志愿者当前状态不可撤销取消，请刷新后重试";
                }
                throw new BusinessException(ResultCode.AUDIT_STATUS_ERROR, reason);
            }

            log.info("志愿者资格已恢复: id={}, auditorId={}", volunteerId, adminUniversityId);

            // 撤销取消时同步恢复论坛审核授权（与 doCancel 停用授权对称）
            forumModeratorGrantMapper.update(null, new LambdaUpdateWrapper<com.demo.miniprogram.entity.ForumModeratorGrant>()
                    .eq(com.demo.miniprogram.entity.ForumModeratorGrant::getVolunteerId, volunteerId)
                    .set(com.demo.miniprogram.entity.ForumModeratorGrant::getActive, 1));
        } catch (org.springframework.dao.DeadlockLoserDataAccessException e) {
            // 兜底：理论上已通过统一锁定顺序消除死锁，此处防止极端场景返回 500
            throw new BusinessException(ResultCode.AUDIT_STATUS_ERROR, "操作冲突，请刷新后重试");
        }
    }

    /**
     * 分页查询志愿者报名列表
     *
     * 不同角色看到不同数据范围：
     *   - TEACHER：只能看到本校报名
     *   - ADMIN：可以看到所有
     *
     * @param queryDTO      分页+筛选参数
     * @param adminUniversityId 教师角色的大学ID
     * @return 分页结果
     */
    public IPage<VolunteerVO> listVolunteers(PageQueryDTO queryDTO, Integer adminUniversityId) {
        Page<VolunteerVO> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        // 超管(adminUniversityId=null)可按 universityId/city 分块查看; 老师强制只看本校, 忽略前端传入的universityId和city(防越权)
        Integer filterUniversityId = adminUniversityId != null ? adminUniversityId : queryDTO.getUniversityId();
        String filterCity = adminUniversityId != null ? null : queryDTO.getCity();  // 老师只看本校, 城市筛选无意义
        IPage<VolunteerVO> pageResult = volunteerMapper.selectVolunteerPage(
                page,
                filterUniversityId,   // 大学筛选(超管按分块/老师锁本校)
                filterCity,           // 城市筛选(仅超管生效, 老师忽略)
                null,                 // highSchoolId筛选（暂不传）
                queryDTO.getStatus(), // status筛选（管理员按状态分块查看）
                queryDTO.getKeyword() // 关键词搜索（姓名/手机号）
        );
        // 填充"是否可撤销取消"标志（CANCELLED 且未超过时限）
        LocalDateTime undoDeadline = LocalDateTime.now().minusDays(UNDO_CANCEL_DAYS);
        for (VolunteerVO vo : pageResult.getRecords()) {
            vo.setCanUndo("CANCELLED".equals(vo.getStatus())
                    && vo.getCancelledAt() != null
                    && !vo.getCancelledAt().isBefore(undoDeadline));
        }
        return pageResult;
    }

    /**
     * 【公开接口】根据高中查询学长学姐列表
     * 只返回公开信息（姓名、大学、专业），不暴露手机号/微信号
     *
     * @param highSchoolId 高中生所在高中ID
     * @return 公开的志愿者信息列表
     */
    public List<VolunteerPublicVO> listPublicByHighSchool(Integer highSchoolId) {
        return volunteerMapper.selectPublicByHighSchool(highSchoolId);
    }

    /**
     * 查询我自己的报名记录
     */
    public IPage<VolunteerVO> listMyApply(String openid, int page, int size) {
        Page<VolunteerVO> p = new Page<>(page, size);
        return volunteerMapper.selectMyApply(p, openid);
    }

    /* ==================== 阶段3：组队期 ==================== */

    /**
     * 自动组队 - 核心业务逻辑（多线程并行处理）
     *
     * 组队规则：
     *   1. 查询所有已确认但未组队的 (highSchoolId, universityId) 组合
     *   2. 对于每个组合（在线程池中并行处理）：
     *      a. 创建一支队伍，关联到 promo_team 表
     *      b. 优先从"申请队长"的志愿者中随机选一位当队长
     *      c. 如果没有申请队长的，从队员中随机选一位
     *      d. 如果该组合只有1人，自动成为队长
     *   3. 利用 uk_team 唯一约束防止重复建队
     *
     * 触发时机：
     *   - 管理员/老师在后台手动点击"开始组队"
     *   - 或定时任务每天自动执行一次
     *
     * @return 新创建的队伍数量
     */
    public int buildTeams(Integer universityId) {
        // 步骤1：获取所有未组队的组合（universityId 非空时只处理指定大学，老师端"本校组队"用）
        List<PromoTeam> ungrouped = promoTeamMapper.selectUngroupedCombinations(universityId);
        if (CollUtil.isEmpty(ungrouped)) {
            log.info("没有需要组队的组合");
            return 0;
        }

        log.info("开始并行组队，共 {} 个组合待处理", ungrouped.size());

        // 步骤2：在线程池中并行处理每个组合
        List<CompletableFuture<Integer>> futures = ungrouped.stream()
                .map(combo -> CompletableFuture.supplyAsync(
                        // buildSingleTeam 为同类自调用，@Transactional 注解不经过代理（历史坑），
                        // 此处用事务模板显式包裹：每个组合独立事务，DuplicateKey 冲突由内部跳过
                        () -> transactionTemplate.execute(status -> buildSingleTeam(combo)), taskExecutor))
                .toList();

        // 步骤3：等待所有并行任务完成，汇总结果
        int teamCount = futures.stream()
                .mapToInt(f -> {
                    try {
                        return f.get(60, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        log.error("组队任务执行异常", e);
                        return 0;
                    }
                })
                .sum();

        log.info("并行组队完成，共创建 {} 支队伍", teamCount);
        return teamCount;
    }

    /**
     * 为单个 (highSchoolId, universityId) 组合创建队伍
     * 事务由调用方 buildTeams 通过 transactionTemplate 包裹（同类自调用注解不生效，历史坑），
     * 每个组合独立事务，线程安全
     *
     * @param combo 包含 highSchoolId 和 universityId 的组合
     * @return 1=创建成功, 0=跳过
     */
    public int buildSingleTeam(PromoTeam combo) {
        Integer highSchoolId = combo.getHighSchoolId();
        Integer universityId = combo.getUniversityId();

        // 获取该组合下所有已确认的志愿者
        List<Volunteer> members = volunteerMapper.selectList(
                new LambdaQueryWrapper<Volunteer>()
                        .eq(Volunteer::getTargetHighSchoolId, highSchoolId)
                        .eq(Volunteer::getUniversityId, universityId)
                        .eq(Volunteer::getStatus, "APPROVED")
        );

        // Only snapshot volunteers that are approved and not already explicitly assigned.
        members = members.stream()
                .filter(member -> !isAssignedToTeam(member.getId()))
                .toList();

        if (CollUtil.isEmpty(members)) {
            return 0;
        }

        // 确定队长
        Integer captainId = selectCaptain(members);

        // 创建队伍
        PromoTeam team = new PromoTeam();
        team.setTeamCode(generateUniqueTeamCode());
        team.setHighSchoolId(highSchoolId);
        team.setUniversityId(universityId);
        team.setCaptainVolunteerId(captainId);

        try {
            promoTeamMapper.insert(team);
            for (Volunteer member : members) {
                TeamMember teamMember = new TeamMember();
                teamMember.setTeamId(team.getId());
                teamMember.setVolunteerId(member.getId());
                teamMember.setRole(member.getId().equals(captainId) ? "CAPTAIN" : "MEMBER");
                teamMember.setJoinedAt(LocalDateTime.now());
                teamMemberMapper.insert(teamMember);
            }
            log.info("队伍创建成功: highSchoolId={}, universityId={}, captainId={}, memberCount={}",
                    highSchoolId, universityId, captainId, members.size());
            return 1;
        } catch (DuplicateKeyException e) {
            // uk_team 冲突说明已组过队，跳过
            log.warn("队伍已存在，跳过: highSchoolId={}, universityId={}", highSchoolId, universityId);
            return 0;
        }
    }

    /**
     * 查看某个高中的所有队伍详情
     * 对应业务文档 - 队伍查看页面
     *
     * @param highSchoolId 高中ID
     * @return 队伍列表（含队长信息、队员列表）
     */
    /** 判断志愿者是否已经被明确分配到其他队伍。 */
    private boolean isAssignedToTeam(Integer volunteerId) {
        if (volunteerId == null) {
            return true;
        }
        return teamMemberMapper.selectCount(
                new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getVolunteerId, volunteerId)) > 0
                || teamOverrideMapper.selectCount(
                new LambdaQueryWrapper<TeamOverride>()
                        .eq(TeamOverride::getVolunteerId, volunteerId)
                        .eq(TeamOverride::getAction, "IN")) > 0;
    }

    public List<TeamVO> getTeamsByHighSchool(Integer highSchoolId) {
        List<TeamVO> teams = promoTeamMapper.selectTeamsByHighSchool(highSchoolId);

        // 为每支队伍填充成员列表（统一成员判定：自由队 team_member + override，自动队组合派生 + override）
        for (TeamVO team : teams) {
            PromoTeam promoTeam = new PromoTeam();
            promoTeam.setId(team.getId());
            promoTeam.setHighSchoolId(team.getHighSchoolId());
            promoTeam.setUniversityId(team.getUniversityId());
            promoTeam.setCaptainVolunteerId(team.getCaptainVolunteerId());
            List<MemberVO> members = teamService.getEffectiveMembers(promoTeam, false);
            team.setMembers(members);
            // 成员数同步为统一判定结果（避免 SQL 按组合派生统计与自由队实际成员不一致）
            team.setMemberCount(members.size());
        }

        return teams;
    }

    /**
     * 生成唯一队伍号：2个字母 + 3个数字，随机排列（与 TeamService 规则一致）
     */
    private String generateUniqueTeamCode() {
        final String codeChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        java.util.Random random = new java.util.Random();
        String code;
        int maxRetries = 100;
        do {
            List<Character> chars = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                chars.add(codeChars.charAt(random.nextInt(26)));
            }
            for (int i = 0; i < 3; i++) {
                chars.add(codeChars.charAt(26 + random.nextInt(10)));
            }
            java.util.Collections.shuffle(chars);
            StringBuilder sb = new StringBuilder();
            for (char c : chars) {
                sb.append(c);
            }
            code = sb.toString();
            maxRetries--;
            if (maxRetries <= 0) {
                throw new BusinessException(ResultCode.SERVER_ERROR, "队伍号生成失败，请重试");
            }
        } while (promoTeamMapper.exists(
                new LambdaQueryWrapper<PromoTeam>().eq(PromoTeam::getTeamCode, code)));
        return code;
    }

    /**
     * 获取其他队长列表（仅姓名+大学+目标高中，不返回联系方式）
     * 优化：批量查询大学和高中名称，避免 N+1 问题
     *
     * @param volunteerId 当前队长的志愿者ID
     * @return 其他队长列表
     */
    public List<MemberVO> getCaptainContacts(Integer volunteerId) {
        // 校验：仅具志愿者资格的队长可查看（被取消者即使旧JWT未过期也不可访问）
        Volunteer me = volunteerMapper.selectById(volunteerId);
        if (me == null || !"APPROVED".equals(me.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "您的志愿者资格已失效，无法查看队长通讯录");
        }
        // 1. 查找当前队长所在的队伍
        PromoTeam myTeam = promoTeamMapper.selectOne(
                new LambdaQueryWrapper<PromoTeam>()
                        .eq(PromoTeam::getCaptainVolunteerId, volunteerId)
        );
        if (myTeam == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "您不是队长，无法查看队长通讯录");
        }

        // 2. 查找同大学或同目标高中的其他队伍队长（排除自己）
        List<PromoTeam> allTeams = promoTeamMapper.selectList(
                new LambdaQueryWrapper<PromoTeam>()
                        .and(w -> w.eq(PromoTeam::getUniversityId, myTeam.getUniversityId())
                                .or().eq(PromoTeam::getHighSchoolId, myTeam.getHighSchoolId()))
                        .ne(PromoTeam::getCaptainVolunteerId, volunteerId)
        );

        if (CollUtil.isEmpty(allTeams)) {
            return List.of();
        }

        // 3. 批量查询队长信息
        List<Integer> captainIds = allTeams.stream()
                .map(PromoTeam::getCaptainVolunteerId)
                .toList();

        List<Volunteer> captains = volunteerMapper.selectBatchIds(captainIds);
        Map<Integer, Volunteer> captainMap = captains.stream()
                .collect(Collectors.toMap(Volunteer::getId, v -> v));

        // 4. 批量查询大学名称和高中名称（避免N+1查询）
        List<Integer> universityIds = captains.stream()
                .map(Volunteer::getUniversityId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Integer, String> universityNameMap = CollUtil.isEmpty(universityIds) ? Map.of()
                : universityMapper.selectBatchIds(universityIds).stream()
                .collect(Collectors.toMap(University::getId, University::getName));

        List<Integer> highSchoolIds = allTeams.stream()
                .map(PromoTeam::getHighSchoolId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Integer, String> highSchoolNameMap = CollUtil.isEmpty(highSchoolIds) ? Map.of()
                : highSchoolMapper.selectBatchIds(highSchoolIds).stream()
                .collect(Collectors.toMap(HighSchool::getId, HighSchool::getName));

        // 5. 构建返回值（仅姓名+大学+目标高中，不返回联系方式）
        List<MemberVO> result = new ArrayList<>();
        for (PromoTeam team : allTeams) {
            Volunteer captain = captainMap.get(team.getCaptainVolunteerId());
            if (captain == null) continue;

            MemberVO vo = new MemberVO();
            vo.setVolunteerId(captain.getId());
            vo.setName(captain.getName());
            vo.setPhone(team.getCaptainContactPhone() != null && !team.getCaptainContactPhone().isBlank()
                    ? team.getCaptainContactPhone() : captain.getPhone());
            vo.setWechat(team.getCaptainContactWechat() != null && !team.getCaptainContactWechat().isBlank()
                    ? team.getCaptainContactWechat() : captain.getWechatId());
            vo.setMajor(captain.getMajor());
            vo.setGrade(captain.getGrade());
            vo.setIsCaptain(true);
            vo.setTeamName(team.getTeamName());
            vo.setUniversityName(universityNameMap.getOrDefault(captain.getUniversityId(), ""));
            vo.setTargetHighSchoolName(highSchoolNameMap.getOrDefault(team.getHighSchoolId(), ""));

            result.add(vo);
        }

        return result;
    }

    /**
     * 获取本队队员列表（队长可查看，仅姓名+大学，不返回联系方式）
     * 优化：批量查询大学名称，避免 N+1 问题
     *
     * @param volunteerId 当前队长的志愿者ID
     * @return 本队队员列表
     */
    public List<MemberVO> getMyTeamMembers(Integer volunteerId) {
        // 校验：仅具志愿者资格的队长可查看（被取消者即使旧JWT未过期也不可访问）
        Volunteer me = volunteerMapper.selectById(volunteerId);
        if (me == null || !"APPROVED".equals(me.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "您的志愿者资格已失效，无法查看队员列表");
        }
        // 1. 查找当前队长所在的队伍
        PromoTeam myTeam = promoTeamMapper.selectOne(
                new LambdaQueryWrapper<PromoTeam>()
                        .eq(PromoTeam::getCaptainVolunteerId, volunteerId)
        );
        if (myTeam == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "您不是队长，无法查看队员列表");
        }

        // 2. 统一成员判定（自由队 team_member + override，自动队组合派生 + override），
        //    避免按"大学+高中"全量查询导致跨队成员/错误成员数/联系方式暴露
        return teamService.getEffectiveMembers(myTeam, false);
    }

    /* ========== 私有辅助方法 ========== */

    /**
     * 从志愿者列表中选出一位队长
     *
     * 优先级：申请队长 > 随机指定
     * 如果队内有申请队长的志愿者，随机选其中一位
     * 否则从所有成员中随机选一位
     */    /* ==================== 多高中分配管理 ==================== */

    /**
     * 为志愿者分配/更新目标高中列表（覆盖式写入）
     *
     * 目标高中为必填业务字段（报名/组队/通讯录均依赖），空数组将被拒绝，
     * 防止通过该接口清空 target_high_school_id 绕过必填校验。
     *
     * @param volunteerId   志愿者ID
     * @param highSchoolIds 高中ID列表（必须至少 1 个）
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignHighSchools(Integer volunteerId, List<Integer> highSchoolIds) {
        Volunteer volunteer = volunteerMapper.selectById(volunteerId);
        if (volunteer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "志愿者不存在");
        }
        if (CollUtil.isEmpty(highSchoolIds)) {
            throw new BusinessException("目标高中不能为空，至少分配一个高中");
        }
        // 清除旧的高中绑定
        volunteerHighSchoolMapper.deleteByVolunteerId(volunteerId);
        // 写入新的高中绑定
        for (Integer highSchoolId : highSchoolIds) {
            VolunteerHighSchool vhs = new VolunteerHighSchool();
            vhs.setVolunteerId(volunteerId);
            vhs.setHighSchoolId(highSchoolId);
            volunteerHighSchoolMapper.insert(vhs);
        }
        // 同步更新 volunteer 表 target_high_school_id 为首个高中（保持兼容）
        volunteer.setTargetHighSchoolId(highSchoolIds.get(0));
        volunteerMapper.updateById(volunteer);
        log.info("志愿者 {} 高中分配已更新: {}", volunteerId, highSchoolIds);
    }

    /**
     * 查询志愿者绑定的所有高中ID
     */
    public List<Integer> getVolunteerHighSchoolIds(Integer volunteerId) {
        return volunteerHighSchoolMapper.selectHighSchoolIdsByVolunteerId(volunteerId);
    }

    /**
     * 删除志愿者的单个高中绑定
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeVolunteerHighSchool(Integer volunteerId, Integer highSchoolId) {
        int deleted = volunteerHighSchoolMapper.deleteByVolunteerAndHighSchool(volunteerId, highSchoolId);
        if (deleted == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该高中绑定不存在");
        }
        log.info("已删除志愿者 {} 对高中 {} 的绑定", volunteerId, highSchoolId);
    }

    /* ========== 私有辅助方法 ========== */

    private Integer selectCaptain(List<Volunteer> members) {
        // 筛选申请队长的志愿者
        List<Volunteer> captainApplicants = members.stream()
                .filter(v -> v.getIsCaptainApplicant() != null && v.getIsCaptainApplicant() == 1)
                .toList();

        if (CollUtil.isNotEmpty(captainApplicants)) {
            // 随机选一位申请者当队长
            return captainApplicants.get((int) (Math.random() * captainApplicants.size())).getId();
        }

        // 没人申请队长，随机选一位
        return members.get((int) (Math.random() * members.size())).getId();
    }
}
