package com.demo.miniprogram.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.miniprogram.common.BusinessException;
import com.demo.miniprogram.common.ResultCode;
import com.demo.miniprogram.dto.AnswerDTO;
import com.demo.miniprogram.dto.AuditDTO;
import com.demo.miniprogram.dto.PageQueryDTO;
import com.demo.miniprogram.dto.QuestionDTO;
import com.demo.miniprogram.entity.*;
import com.demo.miniprogram.mapper.*;
import com.demo.miniprogram.vo.QAVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 问答服务 - 处理提问、回答、审核、精选公开
 *
 * 对应业务文档阶段5-线上答疑期 + 准实时问答机制：
 *   提问 → 存库 → 微信模板消息通知大学生
 *   回答 → 存库 → 微信模板消息通知高中生
 *   审核 → 筛选优质问答 → 公开到精选版块
 *
 * @author demo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QAService {

    private final QuestionAnswerMapper qaMapper;
    private final HighSchoolStudentMapper studentMapper;
    private final VolunteerMapper volunteerMapper;
    private final NotificationService notificationService;
    private final QaLikeMapper qaLikeMapper;
    private final QaFavoriteMapper qaFavoriteMapper;
    private final ForumModerationService forumModerationService;

    /* ==================== 提问 ==================== */

    /**
     * 提交问题（支持高中生提问 + 大学生论坛发帖）
     *
     * @param askerId   提问者ID（从JWT解析，可能是高中生ID或大学生ID）
     * @param askerType 提问者类型："student" 或 "volunteer"
     * @param dto       提问内容
     * @return 问答记录
     */
    @Transactional(rollbackFor = Exception.class)
    public QuestionAnswer ask(Integer askerId, String askerType, QuestionDTO dto) {
        QuestionAnswer qa = new QuestionAnswer();

        boolean forumPost = dto.getTargetVolunteerId() == null;
        // 内容治理：所有提问/发帖统一校验禁言与屏蔽词（一对一私密提问同样校验，防止绕过内容治理）
        forumModerationService.assertNotMuted(askerId, askerType);
        forumModerationService.validateContent(dto.getTitle(), dto.getQuestionContent());

        if ("volunteer".equals(askerType)) {
            // 校验：发帖者必须仍具志愿者资格（被取消/未通过者不可再发帖，历史帖子保留）
            Volunteer asker = volunteerMapper.selectById(askerId);
            if (asker == null || !"APPROVED".equals(asker.getStatus())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "您的志愿者资格已失效，无法发帖");
            }
            // 大学生发帖（论坛模式）
            qa.setAskerVolunteerId(askerId);
        } else {
            // 高中生提问
            qa.setAskerStudentId(askerId);
        }

        // 如果指定了目标志愿者，校验是否存在且已确认
        if (dto.getTargetVolunteerId() != null) {
            Volunteer volunteer = volunteerMapper.selectById(dto.getTargetVolunteerId());
            if (volunteer == null || !"APPROVED".equals(volunteer.getStatus())) {
                throw new BusinessException("该学长/学姐暂不可提问");
            }
            // 指定提问时，校验提问学生高中与志愿者目标高中一致（学生高中为空时不强制）
            if ("student".equals(askerType)) {
                HighSchoolStudent student = studentMapper.selectById(askerId);
                Integer studentHighSchoolId = student != null ? student.getHighSchoolId() : null;
                if (studentHighSchoolId != null
                        && !studentHighSchoolId.equals(volunteer.getTargetHighSchoolId())) {
                    throw new BusinessException("只能向前往您所在高中的学长学姐提问");
                }
            }
            qa.setTargetVolunteerId(dto.getTargetVolunteerId());
        }

        qa.setTitle(dto.getTitle());
        qa.setQuestionContent(dto.getQuestionContent());
        qa.setCategory(dto.getCategory());
        qa.setAuthorType(askerType);
        if (forumPost) {
            qa.setUniversityId(forumModerationService.resolvePostUniversity(askerId, askerType, dto.getUniversityId()));
            qa.setIsFeatured(0);
        }
        if (qa.getTargetVolunteerId() != null) {
            // 指定学长学姐的一对一提问：私密待审核，不进入精选/公开列表，由管理员审核通过后再公开
            qa.setAuditStatus("PENDING");
            qa.setIsPublic(0);
        } else {
            // 论坛发帖：屏蔽词校验通过后仍需本校审核员审核。
            qa.setAuditStatus("PENDING");
            qa.setIsPublic(0);
        }
        qaMapper.insert(qa);

        // 指定学长学姐提问时，通知被提问的志愿者（提问推流）
        if (qa.getTargetVolunteerId() != null) {
            String questionTitle = dto.getQuestionContent() != null
                    ? dto.getQuestionContent().substring(0, Math.min(dto.getQuestionContent().length(), 20))
                    : "新问题";
            notificationService.createAskNotification(qa.getTargetVolunteerId(), questionTitle, qa.getId());
        }

        log.info("新问题提交: askerId={}, askerType={}, targetVolunteerId={}", askerId, askerType, dto.getTargetVolunteerId());
        return qa;
    }

    /* ==================== 回答 ==================== */

    /**
     * 大学生回答问题
     *
     * 前置条件：
     *   - 回答者必须是对应的问题接收者
     *   - 问题尚未被回答
     *
     * 后置动作：
     *   - 发送微信模板消息通知提问的高中生
     *
     * @param volunteerId 回答的大学生ID（从JWT解析）
     * @param dto         回答内容
     */
    @Transactional(rollbackFor = Exception.class)
    public void answer(Integer volunteerId, AnswerDTO dto) {
        QuestionAnswer qa = qaMapper.selectById(dto.getQuestionId());
        if (qa == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "问答记录不存在");
        }

        // 校验：回答者必须仍具志愿者资格（被取消/未通过者不可再回答新问题，历史回答保留）
        Volunteer answerer = volunteerMapper.selectById(volunteerId);
        if (answerer == null || !"APPROVED".equals(answerer.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "您的志愿者资格已失效，无法回答问题");
        }

        // 校验：只有被提问的人才能回答（论坛帖子 targetVolunteerId 为 null，无人可直接回答）
        if (qa.getTargetVolunteerId() == null || !qa.getTargetVolunteerId().equals(volunteerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该问题不是向您提问的");
        }

        // 校验：被管理员拒绝的一对一问题不可回答
        if ("REJECTED".equals(qa.getAuditStatus())) {
            throw new BusinessException("该问题已被拒绝，无法回答");
        }

        // 内容治理：回答同样校验禁言与屏蔽词
        forumModerationService.assertNotMuted(volunteerId, "volunteer");
        forumModerationService.validateContent(dto.getAnswerContent());

        // 更新回答：条件更新防并发（两个并发请求同时通过"未回答"检查时只有一个生效），
        // 一对一问题回答后视为审核通过（保持私密 is_public=0，仅参与者可见，绝不进公开列表）
        int affected = qaMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<QuestionAnswer>()
                .eq(QuestionAnswer::getId, dto.getQuestionId())
                .isNull(QuestionAnswer::getAnswerContent)
                .set(QuestionAnswer::getAnswerContent, dto.getAnswerContent())
                .set(QuestionAnswer::getAnsweredAt, LocalDateTime.now())
                .set(QuestionAnswer::getAuditStatus, "APPROVED"));
        if (affected == 0) {
            throw new BusinessException("该问题已回答过，如需修改请联系管理员");
        }

        // 发送通知给提问的高中生
        if (qa.getAskerStudentId() != null) {
            String questionTitle = qa.getTitle() != null ? qa.getTitle() :
                    (qa.getQuestionContent() != null ? qa.getQuestionContent().substring(0, Math.min(qa.getQuestionContent().length(), 20)) : "新回答");
            notificationService.createReplyNotification(qa.getAskerStudentId(), "student", questionTitle, qa.getId());
        }

        log.info("问题已回答: qaId={}, volunteerId={}", dto.getQuestionId(), volunteerId);
    }

    /* ==================== 查询 ==================== */

    /**
     * 分页查询问答列表（根据不同角色传不同参数）
     *
     * 场景：
     *   - 高中生查看"我的提问"：传 askerStudentId
     *   - 大学生查看"我的回答/待答"：传 targetVolunteerId
     *   - 管理员审核列表：传 auditStatus = PENDING
     *   - 精选问答公开页：传 isPublic = 1
     */
    public IPage<QAVO> listQuestions(PageQueryDTO queryDTO,
                                     Integer askerStudentId,
                                     Integer targetVolunteerId,
                                     String auditStatus,
                                     Integer isPublic) {
        return listQuestions(queryDTO, askerStudentId, targetVolunteerId, auditStatus, isPublic, null, null);
    }

    /**
     * 分页查询问答列表（带分类筛选）
     */
    public IPage<QAVO> listQuestions(PageQueryDTO queryDTO,
                                     Integer askerStudentId,
                                     Integer targetVolunteerId,
                                     String auditStatus,
                                     Integer isPublic,
                                     String category) {
        return listQuestions(queryDTO, askerStudentId, targetVolunteerId, auditStatus, isPublic, category, null);
    }

    /**
     * 分页查询问答列表（带分类筛选 + 按大学过滤）
     *
     * @param universityId 仅返回目标志愿者所属大学为该大学的问答（老师端数据隔离用，null 表示不过滤）
     */
    public IPage<QAVO> listQuestions(PageQueryDTO queryDTO,
                                     Integer askerStudentId,
                                     Integer targetVolunteerId,
                                     String auditStatus,
                                     Integer isPublic,
                                     String category,
                                     Integer universityId) {
        Page<QAVO> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        return qaMapper.selectQAPage(page, askerStudentId, targetVolunteerId, universityId, auditStatus, isPublic, category);
    }

    /** Returns only public forum posts that have been marked as featured. */
    public IPage<QAVO> listFeaturedQuestions(PageQueryDTO queryDTO) {
        Page<QAVO> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        return qaMapper.selectForumPosts(page, null, null, null, 1, "featured");
    }

    /**
     * 获取待回答问题数量（小红点提醒）
     */
    public int countUnanswered(Integer volunteerId) {
        return qaMapper.countUnanswered(volunteerId);
    }

    /**
     * 获取单个问答详情（带可见性控制）
     *
     * 可见性规则：
     *   - 论坛公开帖：APPROVED + is_public=1
     *   - 一对一私密问答：仅提问者/目标志愿者可见（PENDING/APPROVED），REJECTED 仅超级管理员可见
     *   - 普通管理员/老师不再拥有"admin 通读"特权（防止越权读取私密内容），仅超级管理员可通读
     */
    public QAVO getDetail(Integer id, Integer relatedId, String identityRole, String userType, String role) {
        int isSuperAdmin = "admin".equals(userType) && "SUPER_ADMIN".equals(role) ? 1 : 0;
        QAVO detail = qaMapper.selectQADetail(id, relatedId, identityRole, userType, isSuperAdmin);
        if (detail == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "问答记录不存在");
        }
        return detail;
    }

    /**
     * 获取未回答问题列表（便携回复区域）
     */
    public IPage<QAVO> listUnanswered(Integer volunteerId, int page, int size) {
        Page<QAVO> p = new Page<>(page, size);
        return qaMapper.selectUnanswered(p, volunteerId);
    }

    /** 获取当前志愿者已回答的一对一问答，按回答时间倒序。 */
    public IPage<QAVO> listAnsweredByVolunteer(Integer volunteerId, int page, int size) {
        Page<QAVO> p = new Page<>(page, size);
        return qaMapper.selectAnsweredByVolunteer(p, volunteerId);
    }

    /** 获取当前志愿者已发布且审核通过的论坛帖子。 */
    public IPage<QAVO> listMyForumPosts(Integer volunteerId, int page, int size) {
        Page<QAVO> p = new Page<>(page, size);
        return qaMapper.selectMyForumPosts(p, volunteerId);
    }

    /**
     * 获取热门问答（今日热度最高的10条）
     */
    public List<QAVO> listHotQuestions(int limit) {
        return qaMapper.selectHotQuestions(limit);
    }

    /**
     * 获取论坛帖子列表（支持按分类筛选）
     *
     * @param category 分类筛选（可选，为null则查全部）
     */
    public IPage<QAVO> listForumPosts(int page, int size, String category, String authorType,
                                      Integer universityId, Integer featured, String sort) {
        Page<QAVO> p = new Page<>(page, size);
        String safeSort = "heat".equals(sort) || "featured".equals(sort) ? sort : "latest";
        return qaMapper.selectForumPosts(p, category, authorType, universityId, featured, safeSort);
    }

    /**
     * 增加浏览量（原子自增，且仅对论坛公开帖计数，避免私密问答浏览量被刷）
     */
    public void incrementViewCount(Integer id) {
        QuestionAnswer qa = qaMapper.selectById(id);
        if (qa == null) return;
        // 仅公开论坛帖计入浏览（一对一私密问答/下架帖不计，防止通过浏览量探测私密内容热度）
        if (qa.getTargetVolunteerId() != null
                || !"APPROVED".equals(qa.getAuditStatus())
                || !Integer.valueOf(1).equals(qa.getIsPublic())) {
            return;
        }
        qaMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<QuestionAnswer>()
                .eq(QuestionAnswer::getId, id)
                .setSql("view_count = IFNULL(view_count, 0) + 1"));
    }

    /* ==================== 审核 ==================== */

    /**
     * 审核问答内容
     *
     * 管理员可以对问答进行审核：
     *   - APPROVED：通过，可设置为公开（isPublic=1）
     *   - REJECTED：拒绝（违规内容，不展示）
     */
    @Transactional(rollbackFor = Exception.class)
    public void auditQuestion(AuditDTO auditDTO) {
        QuestionAnswer qa = qaMapper.selectById(auditDTO.getId());
        if (qa == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "问答记录不存在");
        }

        // 审核状态机：仅 PENDING 可被审核，防止重复审核覆盖已通过/已拒绝的结果
        if (!"PENDING".equals(qa.getAuditStatus())) {
            throw new BusinessException(ResultCode.AUDIT_STATUS_ERROR, "该问答已审核，不能重复审核");
        }

        // 审核状态校验
        String status = auditDTO.getStatus();
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无效的审核状态: " + status);
        }

        qa.setAuditStatus(status);

        if ("APPROVED".equals(auditDTO.getStatus())) {
            // 一对一私密问答（targetVolunteerId 非空）即使审核通过也绝不公开，
            // 仅参与者可见，防止私密提问内容被公开泄露
            qa.setIsPublic(qa.getTargetVolunteerId() != null ? 0 : 1);
        } else {
            qa.setIsPublic(0);
        }
        qa.setReviewedAt(LocalDateTime.now());

        qaMapper.updateById(qa);
        log.info("问答审核完成: qaId={}, status={}", auditDTO.getId(), auditDTO.getStatus());
    }

    /**
     * 切换问答的公开/隐藏状态（精选问答管理）
     *
     * 安全约束：只有审核通过的内容可以公开（防止把 REJECTED/PENDING 直接公开）
     */
    @Transactional(rollbackFor = Exception.class)
    public void togglePublic(Integer qaId, Integer isPublic) {
        QuestionAnswer qa = qaMapper.selectById(qaId);
        if (qa == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "问答记录不存在");
        }
        if (Integer.valueOf(1).equals(isPublic) && !"APPROVED".equals(qa.getAuditStatus())) {
            throw new BusinessException(ResultCode.AUDIT_STATUS_ERROR, "仅审核通过的问答可设为公开");
        }
        // 一对一私密问答不可公开（is_public 恒为 0）
        if (qa.getTargetVolunteerId() != null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "一对一私密问答不可公开");
        }
        qa.setIsPublic(isPublic);
        qaMapper.updateById(qa);
    }

    /* ==================== 点赞/收藏 ==================== */

    /**
     * 点赞切换（帖子/回答通用）：已赞则取消，未赞则点赞
     *
     * @param qaId       问答/帖子ID
     * @param userId     用户ID
     * @param userType   用户类型：student/volunteer
     * @param targetType 点赞对象：post/answer
     * @return { liked: 是否已赞, count: 该对象点赞总数 }
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleLike(Integer qaId, Integer userId, String userType, String targetType) {
        QuestionAnswer qa = qaMapper.selectById(qaId);
        if (qa == null || !isPublicForumPost(qa)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在或已下架");
        }
        if (!"post".equals(targetType) && !"answer".equals(targetType)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "点赞类型无效");
        }
        // 回答点赞：一帖一答（回答存于 youtube.answer_content），qa_id 复用帖子ID；
        // 需确认该帖确有回答，防止对未回答的帖子点赞回答
        if ("answer".equals(targetType) && qa.getAnswerContent() == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该帖暂无回答");
        }

        QaLike like = qaLikeMapper.selectOne(
                new LambdaQueryWrapper<QaLike>()
                        .eq(QaLike::getQaId, qaId)
                        .eq(QaLike::getUserId, userId)
                        .eq(QaLike::getUserType, userType)
                        .eq(QaLike::getTargetType, targetType)
        );

        boolean liked;
        if (like != null) {
            qaLikeMapper.deleteById(like.getId());
            liked = false;
        } else {
            QaLike newLike = new QaLike();
            newLike.setQaId(qaId);
            newLike.setUserId(userId);
            newLike.setUserType(userType);
            newLike.setTargetType(targetType);
            try {
                qaLikeMapper.insert(newLike);
            } catch (DuplicateKeyException e) {
                // 并发重复点赞：唯一键 uk_user_target 兜底，视为已点赞（幂等，避免500）
                liked = true;
                Long count = qaLikeMapper.selectCount(
                        new LambdaQueryWrapper<QaLike>()
                                .eq(QaLike::getQaId, qaId)
                                .eq(QaLike::getTargetType, targetType)
                );
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("liked", true);
                result.put("count", count);
                return result;
            }
            liked = true;
        }

        Long count = qaLikeMapper.selectCount(
                new LambdaQueryWrapper<QaLike>()
                        .eq(QaLike::getQaId, qaId)
                        .eq(QaLike::getTargetType, targetType)
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("liked", liked);
        result.put("count", count);
        return result;
    }

    /**
     * 收藏切换：已收藏则取消，未收藏则收藏
     *
     * @return { favorited: 是否已收藏, count: 收藏总数 }
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleFavorite(Integer qaId, Integer userId, String userType) {
        QuestionAnswer qa = qaMapper.selectById(qaId);
        if (qa == null || !isPublicForumPost(qa)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在或已下架");
        }

        QaFavorite fav = qaFavoriteMapper.selectOne(
                new LambdaQueryWrapper<QaFavorite>()
                        .eq(QaFavorite::getQaId, qaId)
                        .eq(QaFavorite::getUserId, userId)
                        .eq(QaFavorite::getUserType, userType)
        );

        boolean favorited;
        if (fav != null) {
            qaFavoriteMapper.deleteById(fav.getId());
            favorited = false;
        } else {
            QaFavorite newFav = new QaFavorite();
            newFav.setQaId(qaId);
            newFav.setUserId(userId);
            newFav.setUserType(userType);
            try {
                qaFavoriteMapper.insert(newFav);
            } catch (DuplicateKeyException e) {
                // 并发重复收藏：唯一键 uk_user_qa 兜底，视为已收藏（幂等，避免500）
                Long count = qaFavoriteMapper.selectCount(
                        new LambdaQueryWrapper<QaFavorite>().eq(QaFavorite::getQaId, qaId)
                );
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("favorited", true);
                result.put("count", count);
                return result;
            }
            favorited = true;
        }

        Long count = qaFavoriteMapper.selectCount(
                new LambdaQueryWrapper<QaFavorite>().eq(QaFavorite::getQaId, qaId)
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("favorited", favorited);
        result.put("count", count);
        return result;
    }

    /**
     * 我的收藏列表（分页，按收藏时间倒序）
     */
    public IPage<QAVO> listMyFavorites(PageQueryDTO queryDTO, Integer userId, String userType) {
        Page<QAVO> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        return qaMapper.selectFavoritePage(page, userId, userType);
    }

    /** 是否为公开可见的论坛帖（点赞/收藏/浏览量计数的统一口径） */
    private boolean isPublicForumPost(QuestionAnswer qa) {
        return qa.getTargetVolunteerId() == null
                && "APPROVED".equals(qa.getAuditStatus())
                && Integer.valueOf(1).equals(qa.getIsPublic());
    }
}
