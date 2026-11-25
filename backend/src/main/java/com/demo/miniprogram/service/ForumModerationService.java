package com.demo.miniprogram.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.miniprogram.common.BusinessException;
import com.demo.miniprogram.common.ResultCode;
import com.demo.miniprogram.dto.ForumAppealDTO;
import com.demo.miniprogram.dto.ForumReportDTO;
import com.demo.miniprogram.vo.QAVO;
import com.demo.miniprogram.entity.*;
import com.demo.miniprogram.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ForumModerationService {
    private final ForumModeratorGrantMapper grantMapper;
    private final ForumReportMapper reportMapper;
    private final ForumCommentMapper commentMapper;
    private final ForumPenaltyMapper penaltyMapper;
    private final ForumAppealMapper appealMapper;
    private final ForumSensitiveWordMapper sensitiveWordMapper;
    private final QuestionAnswerMapper qaMapper;
    private final VolunteerMapper volunteerMapper;
    private final HighSchoolStudentMapper studentMapper;
    private final UniversityMapper universityMapper;
    private final AdminUserMapper adminUserMapper;
    private final TeacherPermissionService teacherPermissionService;
    private final NotificationService notificationService;

    public void validateContent(String... values) {
        List<ForumSensitiveWord> words = sensitiveWordMapper.selectList(
                new LambdaQueryWrapper<ForumSensitiveWord>().eq(ForumSensitiveWord::getEnabled, 1));
        for (ForumSensitiveWord item : words) {
            if (item.getWord() == null || item.getWord().isBlank()) continue;
            for (String value : values) {
                if (value != null && value.toLowerCase().contains(item.getWord().toLowerCase())) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "内容包含屏蔽词，请修改后提交");
                }
            }
        }
    }

    public void assertNotMuted(Integer userId, String userType) {
        ForumPenalty active = penaltyMapper.selectOne(new LambdaQueryWrapper<ForumPenalty>()
                .eq(ForumPenalty::getUserId, userId)
                .eq(ForumPenalty::getUserType, userType)
                .eq(ForumPenalty::getStatus, "ACTIVE")
                .gt(ForumPenalty::getMutedUntil, LocalDateTime.now())
                .orderByDesc(ForumPenalty::getMutedUntil).last("LIMIT 1"));
        if (active != null) {
            String until = active.getMutedUntil().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            throw new BusinessException(ResultCode.FORBIDDEN, "账号禁言中，至 " + until + " 可恢复发言");
        }
    }

    public Integer resolvePostUniversity(Integer authorId, String authorType, Integer requestedUniversityId) {
        if ("volunteer".equals(authorType)) {
            Volunteer volunteer = volunteerMapper.selectById(authorId);
            if (volunteer == null || volunteer.getUniversityId() == null) {
                throw new BusinessException("大学生身份未绑定学校");
            }
            return volunteer.getUniversityId();
        }
        // 高中生选择“全部高校”时不关联具体大学，使用 null 表示全站公开论坛。
        // 非空时仍必须验证大学存在，避免伪造高校 ID。
        if (requestedUniversityId == null) {
            return null;
        }
        if (universityMapper.selectById(requestedUniversityId) == null) {
            throw new BusinessException("请选择帖子关联大学");
        }
        return requestedUniversityId;
    }

    public boolean isModerator(Integer volunteerId) {
        return getGrant(volunteerId) != null;
    }

    public ForumModeratorGrant getGrant(Integer volunteerId) {
        if (volunteerId == null) return null;
        // 权限实时校验：授权记录 active=1 且志愿者当前仍为 APPROVED（防止取消资格后权限残留）
        Volunteer volunteer = volunteerMapper.selectById(volunteerId);
        if (volunteer == null || !"APPROVED".equals(volunteer.getStatus())) {
            return null;
        }
        return grantMapper.selectOne(new LambdaQueryWrapper<ForumModeratorGrant>()
                .eq(ForumModeratorGrant::getVolunteerId, volunteerId)
                .eq(ForumModeratorGrant::getActive, 1).last("LIMIT 1"));
    }

    @Transactional(rollbackFor = Exception.class)
    public ForumModeratorGrant setModerator(Integer volunteerId, boolean active, Integer adminId) {
        return setModerator(volunteerId, active, adminId, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public ForumModeratorGrant setModerator(Integer volunteerId, boolean active, Integer adminId,
                                             Integer allowedUniversityId) {
        Volunteer volunteer = volunteerMapper.selectById(volunteerId);
        if (volunteer == null || !"APPROVED".equals(volunteer.getStatus())) {
            throw new BusinessException("只能授权审核通过的大学生");
        }
        if (allowedUniversityId != null && !allowedUniversityId.equals(volunteer.getUniversityId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能管理本校大学生的论坛审核权限");
        }
        ForumModeratorGrant grant = grantMapper.selectOne(new LambdaQueryWrapper<ForumModeratorGrant>()
                .eq(ForumModeratorGrant::getVolunteerId, volunteerId)
                .eq(ForumModeratorGrant::getUniversityId, volunteer.getUniversityId()).last("LIMIT 1"));
        if (grant == null) {
            grant = new ForumModeratorGrant();
            grant.setVolunteerId(volunteerId);
            grant.setUniversityId(volunteer.getUniversityId());
            grant.setGrantedByAdminId(adminId);
            grant.setActive(active ? 1 : 0);
            grantMapper.insert(grant);
        } else {
            grant.setActive(active ? 1 : 0);
            grant.setGrantedByAdminId(adminId);
            grantMapper.updateById(grant);
        }
        return grant;
    }

    public IPage<QuestionAnswer> pendingPosts(int page, int size, Integer reviewerId, String userType, String role) {
        Integer universityId = reviewerUniversity(reviewerId, userType, role);
        LambdaQueryWrapper<QuestionAnswer> wrapper = new LambdaQueryWrapper<QuestionAnswer>()
                .isNull(QuestionAnswer::getTargetVolunteerId)
                .eq(QuestionAnswer::getAuditStatus, "PENDING")
                .orderByAsc(QuestionAnswer::getAskedAt);
        if (universityId != null) wrapper.eq(QuestionAnswer::getUniversityId, universityId);
        return qaMapper.selectPage(new Page<>(sanitizePage(page), sanitizeSize(size)), wrapper);
    }

    public IPage<QAVO> featuredCandidates(int page, int size, Integer reviewerId, String userType, String role) {
        Integer universityId = reviewerUniversity(reviewerId, userType, role);
        return qaMapper.selectForumPosts(new Page<>(sanitizePage(page), sanitizeSize(size)), null, null, universityId, null, "featured");
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewPost(Integer postId, String action, Integer reviewerId, String userType, String role) {
        if (!"APPROVED".equals(action) && !"REJECTED".equals(action)) throw new BusinessException("审核动作无效");
        requireScopedPost(postId, reviewerId, userType, role, true);
        // 状态机条件更新：仅 PENDING 可被审核（防止重复审核/并发双审覆盖审核人与结果）
        int affected = qaMapper.update(null, new LambdaUpdateWrapper<QuestionAnswer>()
                .eq(QuestionAnswer::getId, postId)
                .eq(QuestionAnswer::getAuditStatus, "PENDING")
                .set(QuestionAnswer::getAuditStatus, action)
                .set(QuestionAnswer::getIsPublic, "APPROVED".equals(action) ? 1 : 0)
                .set(QuestionAnswer::getReviewedAt, LocalDateTime.now())
                .set("admin".equals(userType) ? QuestionAnswer::getReviewedByAdminId : QuestionAnswer::getReviewedByVolunteerId, reviewerId));
        if (affected == 0) {
            throw new BusinessException(ResultCode.AUDIT_STATUS_ERROR, "帖子已被审核或状态已变化，请刷新后重试");
        }
        // 审核结果通知作者（拒绝与通过均告知）
        QuestionAnswer post = qaMapper.selectById(postId);
        Integer authorId = post.getAskerVolunteerId() != null ? post.getAskerVolunteerId() : post.getAskerStudentId();
        String authorType = post.getAskerVolunteerId() != null ? "volunteer" : "student";
        String title = post.getTitle() != null ? post.getTitle() : "论坛帖子";
        if ("APPROVED".equals(action)) {
            notificationService.createSystemNotification(authorId, authorType, "帖子审核通过",
                    "你的帖子\"" + title + "\"已通过审核并公开", postId, "post", title);
        } else {
            notificationService.createSystemNotification(authorId, authorType, "帖子审核未通过",
                    "你的帖子\"" + title + "\"未通过审核，请修改后重新发布", postId, "post", title);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void setFeatured(Integer postId, boolean featured, Integer reviewerId, String userType, String role) {
        QuestionAnswer post = requireScopedPost(postId, reviewerId, userType, role, true);
        if (!"APPROVED".equals(post.getAuditStatus()) || !Integer.valueOf(1).equals(post.getIsPublic())) {
            throw new BusinessException("只能设置已审核公开的帖子");
        }
        if (featured && "student".equals(post.getAuthorType())) {
            throw new BusinessException("高中生发布的帖子不能设为精华");
        }
        post.setIsFeatured(featured ? 1 : 0);
        // 精华操作审计：记录操作人与时间
        post.setFeaturedByType(userType);
        post.setFeaturedById(reviewerId);
        post.setFeaturedAt(LocalDateTime.now());
        qaMapper.updateById(post);
    }

    @Transactional(rollbackFor = Exception.class)
    public ForumReport submitReport(Integer userId, String userType, ForumReportDTO dto) {
        QuestionAnswer post = qaMapper.selectById(dto.getPostId());
        if (post == null || !"APPROVED".equals(post.getAuditStatus()) || !Integer.valueOf(1).equals(post.getIsPublic())) {
            throw new BusinessException("帖子不存在或已下架");
        }
        // 仅论坛帖子可举报：一对一私密问答不允许进入举报队列（否则审核员无法处理，形成死队列）
        if (post.getTargetVolunteerId() != null) {
            throw new BusinessException("仅论坛广场帖子可举报");
        }
        String targetType = dto.getTargetType() == null || dto.getTargetType().isBlank() ? "post" : dto.getTargetType();
        if (!"post".equals(targetType) && !"comment".equals(targetType)) {
            throw new BusinessException("举报对象类型无效");
        }
        Integer commentId = 0;
        if ("comment".equals(targetType)) {
            commentId = dto.getCommentId();
            if (commentId == null) {
                throw new BusinessException("请选择要举报的评论");
            }
            ForumComment comment = commentMapper.selectById(commentId);
            if (comment == null || !comment.getPostId().equals(post.getId())) {
                throw new BusinessException("评论不存在");
            }
            if (!"VISIBLE".equals(comment.getStatus())) {
                throw new BusinessException("评论已被处理");
            }
        }
        Long duplicate = reportMapper.selectCount(new LambdaQueryWrapper<ForumReport>()
                .eq(ForumReport::getPostId, dto.getPostId())
                .eq(ForumReport::getTargetType, targetType)
                .eq(ForumReport::getCommentId, commentId)
                .eq(ForumReport::getReporterId, userId)
                .eq(ForumReport::getReporterType, userType));
        if (duplicate > 0) {
            throw new BusinessException("comment".equals(targetType) ? "您已举报过该评论" : "您已举报过该帖子");
        }
        ForumReport report = new ForumReport();
        report.setPostId(dto.getPostId());
        report.setTargetType(targetType);
        report.setCommentId(commentId);
        report.setReporterId(userId); report.setReporterType(userType);
        report.setReason(dto.getReason()); report.setDetail(dto.getDetail()); report.setStatus("PENDING");
        reportMapper.insert(report);
        return report;
    }

    public IPage<ForumReport> pendingReports(int page, int size, Integer reviewerId, String userType, String role) {
        Integer universityId = reviewerUniversity(reviewerId, userType, role);
        return reportMapper.selectPendingPage(new Page<>(sanitizePage(page), sanitizeSize(size)), universityId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewReport(Integer reportId, boolean founded, String note, Integer reviewerId, String userType, String role) {
        ForumReport report = reportMapper.selectById(reportId);
        if (report == null || !"PENDING".equals(report.getStatus())) throw new BusinessException("举报不存在或已处理");
        QuestionAnswer post = requireScopedPost(report.getPostId(), reviewerId, userType, role, true);

        boolean commentReport = "comment".equals(report.getTargetType());
        Integer penaltyId = report.getPenaltyId();
        Integer targetUserId = null;
        String targetUserType = null;
        if (founded) {
            if (commentReport) {
                // 评论举报成立：仅隐藏该评论并处罚评论作者，帖子本身不受影响
                ForumComment comment = commentMapper.selectById(report.getCommentId());
                if (comment == null || !"VISIBLE".equals(comment.getStatus())) {
                    throw new BusinessException("评论不存在或已处理");
                }
                comment.setStatus("HIDDEN");
                commentMapper.updateById(comment);
                targetUserId = comment.getUserId();
                targetUserType = comment.getUserType();
            } else {
                post.setIsPublic(0); post.setIsFeatured(0); qaMapper.updateById(post);
                targetUserId = post.getAskerVolunteerId() != null ? post.getAskerVolunteerId() : post.getAskerStudentId();
                targetUserType = post.getAskerVolunteerId() != null ? "volunteer" : "student";
            }
            // 同一举报目标一次处罚：后续成立的举报关联同一处罚记录（保证每条成立举报都可追溯/申诉）
            // FOR UPDATE 当前读：并发处理同帖另一条举报时，能看到对方刚提交的 FOUNDED 记录，
            // 避免 RR 快照读下"看不到新记录"导致同一帖子/评论生成两条处罚
            ForumReport priorFounded = reportMapper.selectOne(new LambdaQueryWrapper<ForumReport>()
                    .eq(ForumReport::getPostId, post.getId())
                    .eq(ForumReport::getTargetType, report.getTargetType())
                    .eq(ForumReport::getCommentId, report.getCommentId())
                    .eq(ForumReport::getStatus, "FOUNDED")
                    .isNotNull(ForumReport::getPenaltyId)
                    .orderByAsc(ForumReport::getReviewedAt).last("LIMIT 1 FOR UPDATE"));
            if (priorFounded != null) {
                penaltyId = priorFounded.getPenaltyId();
            } else {
                penaltyId = createPenalty(targetUserId, targetUserType, reportId).getId();
            }
        }

        // 状态机条件更新：仅 PENDING 举报可被处理（并发双审只有一个生效）
        int affected = reportMapper.update(null, new LambdaUpdateWrapper<ForumReport>()
                .eq(ForumReport::getId, reportId)
                .eq(ForumReport::getStatus, "PENDING")
                .set(ForumReport::getStatus, founded ? "FOUNDED" : "DISMISSED")
                .set(ForumReport::getReviewerId, reviewerId)
                .set(ForumReport::getReviewerType, userType)
                .set(ForumReport::getDecisionNote, note)
                .set(ForumReport::getReviewedAt, LocalDateTime.now())
                .set(ForumReport::getPenaltyId, penaltyId));
        if (affected == 0) {
            throw new BusinessException(ResultCode.AUDIT_STATUS_ERROR, "举报已被处理或状态已变化，请刷新后重试");
        }

        if (founded) {
            ForumPenalty penalty = penaltyId != null ? penaltyMapper.selectById(penaltyId) : null;
            String title = post.getTitle() != null ? post.getTitle() : "论坛帖子";
            String until = penalty != null && penalty.getMutedUntil() != null
                    ? penalty.getMutedUntil().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "";
            if (commentReport) {
                ForumComment comment = commentMapper.selectById(report.getCommentId());
                notificationService.createSystemNotification(comment.getUserId(), comment.getUserType(), "评论违规隐藏",
                        "你的评论被举报违规已隐藏" + (until.isEmpty() ? "" : "，账号禁言至 " + until),
                        post.getId(), "post", title);
            } else {
                notificationService.createSystemNotification(targetUserId, targetUserType, "帖子违规下架",
                        "你的帖子\"" + title + "\"因违规被下架" + (until.isEmpty() ? "" : "，账号禁言至 " + until),
                        post.getId(), "post", title);
            }
        }
    }

    private ForumPenalty createPenalty(Integer userId, String userType, Integer reportId) {
        // 锁用户行防并发：同一用户的处罚次数计算串行化（否则并发成立举报会读到相同 offense 次数）
        if ("volunteer".equals(userType)) {
            volunteerMapper.selectByIdForUpdate(userId);
        } else {
            studentMapper.selectByIdForUpdate(userId);
        }
        int offense = Math.toIntExact(penaltyMapper.selectCount(new LambdaQueryWrapper<ForumPenalty>()
                .eq(ForumPenalty::getUserId, userId).eq(ForumPenalty::getUserType, userType)
                .ne(ForumPenalty::getStatus, "REVOKED"))) + 1;
        Duration duration = switch (Math.min(offense, 6)) {
            case 1 -> Duration.ofHours(1); case 2 -> Duration.ofHours(6); case 3 -> Duration.ofHours(24);
            case 4 -> Duration.ofDays(3); case 5 -> Duration.ofDays(7); default -> Duration.ofDays(30);
        };
        LocalDateTime now = LocalDateTime.now();
        ForumPenalty current = penaltyMapper.selectOne(new LambdaQueryWrapper<ForumPenalty>()
                .eq(ForumPenalty::getUserId, userId).eq(ForumPenalty::getUserType, userType)
                .eq(ForumPenalty::getStatus, "ACTIVE").gt(ForumPenalty::getMutedUntil, now)
                .orderByDesc(ForumPenalty::getMutedUntil).last("LIMIT 1"));
        LocalDateTime start = current != null ? current.getMutedUntil() : now;
        ForumPenalty penalty = new ForumPenalty();
        penalty.setUserId(userId); penalty.setUserType(userType); penalty.setReportId(reportId);
        penalty.setOffenseNumber(offense); penalty.setMutedFrom(now); penalty.setMutedUntil(start.plus(duration));
        penalty.setStatus("ACTIVE"); penaltyMapper.insert(penalty); return penalty;
    }

    public List<ForumPenalty> myPenalties(Integer userId, String userType) {
        return penaltyMapper.selectList(new LambdaQueryWrapper<ForumPenalty>()
                .eq(ForumPenalty::getUserId, userId).eq(ForumPenalty::getUserType, userType)
                .orderByDesc(ForumPenalty::getCreatedAt));
    }

    @Transactional(rollbackFor = Exception.class)
    public ForumAppeal appeal(Integer userId, String userType, ForumAppealDTO dto) {
        ForumPenalty penalty = penaltyMapper.selectById(dto.getPenaltyId());
        if (penalty == null || !penalty.getUserId().equals(userId) || !penalty.getUserType().equals(userType)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权申诉该处罚");
        }
        if (appealMapper.selectCount(new LambdaQueryWrapper<ForumAppeal>().eq(ForumAppeal::getPenaltyId, penalty.getId())) > 0) {
            throw new BusinessException("该处罚已经申诉过");
        }
        ForumAppeal appeal = new ForumAppeal(); appeal.setPenaltyId(penalty.getId());
        appeal.setAppellantId(userId); appeal.setAppellantType(userType); appeal.setReason(dto.getReason());
        appeal.setStatus("PENDING"); appeal.setRestorePost(0); appealMapper.insert(appeal); return appeal;
    }

    public IPage<ForumAppeal> pendingAppeals(int page, int size, Integer reviewerId, String userType, String role) {
        Integer universityId = reviewerUniversity(reviewerId, userType, role);
        return appealMapper.selectPendingPage(new Page<>(sanitizePage(page), sanitizeSize(size)), universityId);
    }

    /** 分页页码收敛：page >= 1 */
    private long sanitizePage(int page) {
        return Math.max(page, 1);
    }

    /** 分页大小收敛：1 <= size <= 100（防止 size=超大值一次性拉全表） */
    private long sanitizeSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewAppeal(Integer appealId, boolean approved, boolean restorePost, String note,
                             Integer reviewerId, String userType, String role) {
        ForumAppeal appeal = appealMapper.selectById(appealId);
        if (appeal == null || !"PENDING".equals(appeal.getStatus())) throw new BusinessException("申诉不存在或已处理");
        ForumPenalty penalty = penaltyMapper.selectById(appeal.getPenaltyId());
        ForumReport report = penalty != null ? reportMapper.selectById(penalty.getReportId()) : null;
        if (penalty == null || report == null) throw new BusinessException("处罚记录不完整");
        requireScopedPost(report.getPostId(), reviewerId, userType, role, false);
        // 利益冲突校验：按真实微信主体（openid）判断，防止同一主体学生/志愿者双身份绕过
        if (report.getReviewerId() != null && !"SUPER_ADMIN".equals(role) && isSameSubject(report.getReviewerId(), report.getReviewerType(), reviewerId, userType)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "原举报处理人不能复核申诉");
        }
        // 状态机条件更新：仅 PENDING 申诉可被复核（并发双审只有一个生效）
        int affected = appealMapper.update(null, new LambdaUpdateWrapper<ForumAppeal>()
                .eq(ForumAppeal::getId, appealId)
                .eq(ForumAppeal::getStatus, "PENDING")
                .set(ForumAppeal::getStatus, approved ? "APPROVED" : "REJECTED")
                .set(ForumAppeal::getReviewerId, reviewerId)
                .set(ForumAppeal::getReviewerType, userType)
                .set(ForumAppeal::getDecisionNote, note)
                .set(ForumAppeal::getRestorePost, restorePost ? 1 : 0)
                .set(ForumAppeal::getReviewedAt, LocalDateTime.now()));
        if (affected == 0) {
            throw new BusinessException(ResultCode.AUDIT_STATUS_ERROR, "申诉已被复核或状态已变化，请刷新后重试");
        }
        if (approved) {
            penalty.setStatus("REVOKED"); penalty.setRevokedAt(LocalDateTime.now()); penaltyMapper.updateById(penalty);
            if (restorePost) {
                if ("comment".equals(report.getTargetType())) {
                    ForumComment comment = commentMapper.selectById(report.getCommentId());
                    if (comment != null) {
                        comment.setStatus("VISIBLE");
                        commentMapper.updateById(comment);
                    }
                } else {
                    QuestionAnswer post = qaMapper.selectById(report.getPostId());
                    post.setIsPublic(1);
                    qaMapper.updateById(post);
                }
            }
        }
        // 申诉结果通知申诉人
        notificationService.createSystemNotification(appeal.getAppellantId(), appeal.getAppellantType(),
                approved ? "申诉通过" : "申诉驳回",
                approved
                        ? (restorePost
                            ? ("comment".equals(report.getTargetType()) ? "您的处罚已撤销，评论已恢复显示" : "您的处罚已撤销，帖子已恢复公开")
                            : "您的处罚已撤销，可正常发言")
                        : "您的申诉未通过，维持原处罚决定",
                appeal.getId(), "appeal", "论坛处罚申诉");
    }

    /** 判断两个处理人是否为同一微信主体（openid 相同即视为同一人，防止双身份绕过） */
    private boolean isSameSubject(Integer idA, String typeA, Integer idB, String typeB) {
        if (idA == null || idB == null) return false;
        if (typeA == null || !typeA.equals(typeB)) return false;
        if ("volunteer".equals(typeA)) {
            Volunteer a = volunteerMapper.selectById(idA);
            Volunteer b = volunteerMapper.selectById(idB);
            return a != null && b != null && a.getOpenid() != null && a.getOpenid().equals(b.getOpenid());
        }
        if ("student".equals(typeA)) {
            HighSchoolStudent a = studentMapper.selectById(idA);
            HighSchoolStudent b = studentMapper.selectById(idB);
            return a != null && b != null && a.getOpenid() != null && a.getOpenid().equals(b.getOpenid());
        }
        return idA.equals(idB);
    }

    private Integer reviewerUniversity(Integer reviewerId, String userType, String role) {
        if ("admin".equals(userType) && "SUPER_ADMIN".equals(role)) return null;
        if ("admin".equals(userType) && "TEACHER".equals(role)) {
            teacherPermissionService.require(reviewerId, com.demo.miniprogram.security.TeacherPermissions.FORUM_MODERATION);
            AdminUser teacher = adminUserMapper.selectById(reviewerId);
            if (teacher == null || teacher.getUniversityId() == null) {
                throw new BusinessException(ResultCode.FORBIDDEN, "老师账号未绑定大学");
            }
            return teacher.getUniversityId();
        }
        if (!"volunteer".equals(userType)) throw new BusinessException(ResultCode.FORBIDDEN, "无论坛审核权限");
        ForumModeratorGrant grant = getGrant(reviewerId);
        if (grant == null) throw new BusinessException(ResultCode.FORBIDDEN, "未获得论坛审核授权");
        return grant.getUniversityId();
    }

    private QuestionAnswer requireScopedPost(Integer postId, Integer reviewerId, String userType, String role, boolean blockSelf) {
        QuestionAnswer post = qaMapper.selectById(postId);
        if (post == null || post.getTargetVolunteerId() != null) throw new BusinessException("论坛帖子不存在");
        Integer universityId = reviewerUniversity(reviewerId, userType, role);
        if (universityId != null && !universityId.equals(post.getUniversityId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能处理本校帖子");
        }
        // 自审拦截按真实微信主体（openid）判断：同一主体同时拥有 student/volunteer 双身份时
        // 不能通过切换身份绕过"不能审核自己的帖子"
        if (blockSelf && "volunteer".equals(userType) && reviewerId != null) {
            Volunteer reviewer = volunteerMapper.selectById(reviewerId);
            String reviewerOpenid = reviewer != null ? reviewer.getOpenid() : null;
            if (reviewerOpenid != null && sameOpenidAsAuthor(post, reviewerOpenid)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "不能审核或设置自己的帖子");
            }
        }
        return post;
    }

    private boolean sameOpenidAsAuthor(QuestionAnswer post, String reviewerOpenid) {
        if (post.getAskerVolunteerId() != null) {
            Volunteer author = volunteerMapper.selectById(post.getAskerVolunteerId());
            return author != null && reviewerOpenid.equals(author.getOpenid());
        }
        if (post.getAskerStudentId() != null) {
            HighSchoolStudent author = studentMapper.selectById(post.getAskerStudentId());
            return author != null && reviewerOpenid.equals(author.getOpenid());
        }
        return false;
    }
}
