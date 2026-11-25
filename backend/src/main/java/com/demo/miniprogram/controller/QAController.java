package com.demo.miniprogram.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.demo.miniprogram.common.Result;
import com.demo.miniprogram.dto.AnswerDTO;
import com.demo.miniprogram.dto.PageQueryDTO;
import com.demo.miniprogram.dto.QuestionDTO;
import com.demo.miniprogram.entity.QuestionAnswer;
import com.demo.miniprogram.service.QAService;
import com.demo.miniprogram.vo.QAVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 问答接口 - 提问、回答、查询
 *
 * 对应业务文档阶段5-线上答疑期
 *
 * @author demo
 */
@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
public class QAController {

    private final QAService qaService;

    /**
     * 提交问题（高中生提问 / 大学生论坛发帖）
     *
     * 请求示例：
     *   POST /api/qa/ask
     *   Header: Authorization: Bearer {token}
     *   Body: {"questionContent": "...", "title": "...", "targetVolunteerId": 10}
     */
    @PostMapping("/ask")
    public Result<QuestionAnswer> ask(@RequestAttribute(value = "relatedId", required = false) Integer askerId,
                                      @RequestAttribute(value = "askerType", required = false) String askerType,
                                      @Valid @RequestBody QuestionDTO dto) {
        // 身份校验：askerType 由 JwtAuthFilter 注入，必须为 student/volunteer 且非空，
        // 防止管理员（无 askerType）或未绑定身份的用户混入提问
        if (askerId == null || !("student".equals(askerType) || "volunteer".equals(askerType))) {
            return Result.fail(403, "请先完成身份认证后再提问");
        }
        QuestionAnswer qa = qaService.ask(askerId, askerType, dto);
        return Result.ok("问题已提交", qa);
    }

    /**
     * 大学生回答问题
     *
     * 请求示例：
     *   POST /api/qa/answer
     *   Header: Authorization: Bearer {token}
     *   Body: {"questionId": 5, "answerContent": "我们学校计算机专业很好，有ACM金牌团队..."}
     */
    @PostMapping("/answer")
    public Result<String> answer(@RequestAttribute(value = "relatedId", required = false) Integer volunteerId,
                                 @RequestAttribute(value = "relatedType", required = false) String relatedType,
                                 @Valid @RequestBody AnswerDTO dto) {
        // 身份校验：仅 volunteer 身份可回答问题，防止学生 ID 与志愿者 ID 空间混淆
        if (!"volunteer".equals(relatedType) || volunteerId == null) {
            return Result.fail(403, "请先完成身份认证后再回答");
        }
        qaService.answer(volunteerId, dto);
        return Result.ok("回答成功");
    }

    /**
     * 高中生查看"我的提问"列表
     *
     * 请求示例：
     *   GET /api/qa/my-questions?page=1&size=20
     *   Header: Authorization: Bearer {token}
     */
    @GetMapping("/my-questions")
    public Result<IPage<QAVO>> myQuestions(@RequestAttribute(value = "relatedId", required = false) Integer studentId,
                                           @RequestAttribute(value = "relatedType", required = false) String relatedType,
                                           @Valid PageQueryDTO queryDTO) {
        // 仅 student 身份可查看"我的提问"，防止志愿者/管理员用他人 ID 空间查询
        if (studentId == null || !"student".equals(relatedType)) {
            return Result.fail(403, "请先完成身份认证后再查看");
        }
        return Result.ok(qaService.listQuestions(queryDTO, studentId, null, null, null));
    }

    /**
     * 大学生查看"向我提问"列表（仅未回答，回答后自动消失）
     *
     * 请求示例：
     *   GET /api/qa/to-me?page=1&size=20
     *   Header: Authorization: Bearer {token}
     */
    @GetMapping("/to-me")
    public Result<IPage<QAVO>> questionsToMe(@RequestAttribute(value = "relatedId", required = false) Integer volunteerId,
                                             @RequestAttribute(value = "relatedType", required = false) String relatedType,
                                             @Valid PageQueryDTO queryDTO) {
        // 仅 volunteer 身份可查看"向我提问"
        if (volunteerId == null || !"volunteer".equals(relatedType)) {
            return Result.fail(403, "请先完成身份认证后再查看");
        }
        return Result.ok(qaService.listUnanswered(volunteerId, queryDTO.getPage(), queryDTO.getSize()));
    }

    /** 志愿者查看自己已回答的一对一问答。 */
    @GetMapping("/answered-by-me")
    public Result<IPage<QAVO>> answeredByMe(@RequestAttribute(value = "relatedId", required = false) Integer volunteerId,
                                            @RequestAttribute(value = "relatedType", required = false) String relatedType,
                                            @Valid PageQueryDTO queryDTO) {
        if (volunteerId == null || !"volunteer".equals(relatedType)) {
            return Result.fail(403, "请先完成身份认证后再查看");
        }
        return Result.ok(qaService.listAnsweredByVolunteer(volunteerId, queryDTO.getPage(), queryDTO.getSize()));
    }

    /** 当前志愿者发布且可在论坛显示的帖子。 */
    @GetMapping("/my-posts")
    public Result<IPage<QAVO>> myPosts(@RequestAttribute(value = "relatedId", required = false) Integer volunteerId,
                                       @RequestAttribute(value = "relatedType", required = false) String relatedType,
                                       @Valid PageQueryDTO queryDTO) {
        if (volunteerId == null || !"volunteer".equals(relatedType)) {
            return Result.fail(403, "请先完成身份认证后再查看");
        }
        return Result.ok(qaService.listMyForumPosts(volunteerId, queryDTO.getPage(), queryDTO.getSize()));
    }

    /**
     * 获取待回答数量（小红点提醒）
     *
     * 请求示例：
     *   GET /api/qa/unanswered-count
     *   Header: Authorization: Bearer {token}
     */
    @GetMapping("/unanswered-count")
    public Result<Integer> unansweredCount(@RequestAttribute(value = "relatedId", required = false) Integer volunteerId,
                                           @RequestAttribute(value = "relatedType", required = false) String relatedType) {
        // 仅 volunteer 身份可查待回答数量
        if (volunteerId == null || !"volunteer".equals(relatedType)) {
            return Result.fail(403, "请先完成身份认证后再查看");
        }
        return Result.ok(qaService.countUnanswered(volunteerId));
    }

    /**
     * 精选问答公开页 → 所有高中生可查看
     *
     * 请求示例：
     *   GET /api/qa/featured?page=1&size=20
     */
    @GetMapping("/featured")
    public Result<IPage<QAVO>> featuredQuestions(@Valid PageQueryDTO queryDTO) {
        // isPublic=1 只查公开的精选问答，auditStatus=APPROVED
        return Result.ok(qaService.listFeaturedQuestions(queryDTO));
    }

    /**
     * 获取单个问答详情（同时增加浏览量）
     *
     * 请求示例：
     *   GET /api/qa/5
     */
    @GetMapping("/{id}")
    public Result<QAVO> getDetail(@PathVariable Integer id,
                                  @RequestAttribute(value = "relatedId", required = false) Integer relatedId,
                                  @RequestAttribute(value = "relatedType", required = false) String relatedType,
                                  @RequestAttribute(value = "userType", required = false) String userType,
                                  @RequestAttribute(value = "requesterRole", required = false) String requesterRole) {
        // 先做可见性校验，通过后再计浏览（避免不可见内容被刷浏览/探测）
        QAVO detail = qaService.getDetail(id, relatedId, relatedType, userType, requesterRole);
        qaService.incrementViewCount(id);
        return Result.ok(detail);
    }

    /**
     * 热门问答（今日热度最高的N条）
     *
     * 请求示例：
     *   GET /api/qa/hot?limit=10
     */
    @GetMapping("/hot")
    public Result<List<QAVO>> hotQuestions(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(qaService.listHotQuestions(limit));
    }

    /**
     * 论坛帖子列表（targetVolunteerId 为 null 的帖子）
     *
     * 请求示例：
     *   GET /api/qa/forum?page=1&size=20
     */
    @GetMapping("/forum")
    public Result<IPage<QAVO>> forumPosts(PageQueryDTO queryDTO,
                                          @RequestParam(required = false) String category,
                                          @RequestParam(required = false) String authorType,
                                          @RequestParam(required = false) Integer universityId,
                                          @RequestParam(required = false) Integer featured,
                                          @RequestParam(defaultValue = "latest") String sort) {
        return Result.ok(qaService.listForumPosts(queryDTO.getPage(), queryDTO.getSize(), category,
                authorType, universityId, featured, sort));
    }

    /* ==================== 点赞 / 收藏 ==================== */

    /**
     * 帖子点赞切换（已赞取消，未赞点赞）
     *
     * 请求示例：
     *   POST /api/qa/{id}/like
     *   Header: Authorization: Bearer {token}（需已绑定身份）
     */
    @PostMapping("/{id}/like")
    public Result<Map<String, Object>> likePost(@PathVariable Integer id,
                                                @RequestAttribute(value = "relatedId", required = false) Integer relatedId,
                                                @RequestAttribute(value = "relatedType", required = false) String relatedType) {
        return Result.ok(interaction(id, relatedId, relatedType, "post"));
    }

    /**
     * 回答点赞切换（已赞取消，未赞点赞）
     *
     * 请求示例：
     *   POST /api/qa/{id}/answer-like
     */
    @PostMapping("/{id}/answer-like")
    public Result<Map<String, Object>> likeAnswer(@PathVariable Integer id,
                                                  @RequestAttribute(value = "relatedId", required = false) Integer relatedId,
                                                  @RequestAttribute(value = "relatedType", required = false) String relatedType) {
        return Result.ok(interaction(id, relatedId, relatedType, "answer"));
    }

    /**
     * 收藏切换（已收藏取消，未收藏收藏）
     *
     * 请求示例：
     *   POST /api/qa/{id}/favorite
     */
    @PostMapping("/{id}/favorite")
    public Result<Map<String, Object>> favoritePost(@PathVariable Integer id,
                                                    @RequestAttribute(value = "relatedId", required = false) Integer relatedId,
                                                    @RequestAttribute(value = "relatedType", required = false) String relatedType) {
        if (relatedId == null || !isValidIdentity(relatedType)) {
            return Result.fail(403, "请先绑定学生/志愿者身份后再收藏");
        }
        return Result.ok(qaService.toggleFavorite(id, relatedId, relatedType));
    }

    /**
     * 我的收藏列表（分页）
     *
     * 请求示例：
     *   GET /api/qa/my-favorites?page=1&size=20
     */
    @GetMapping("/my-favorites")
    public Result<IPage<QAVO>> myFavorites(@RequestAttribute(value = "relatedId", required = false) Integer relatedId,
                                           @RequestAttribute(value = "relatedType", required = false) String relatedType,
                                           @Valid PageQueryDTO queryDTO) {
        if (relatedId == null || !isValidIdentity(relatedType)) {
            return Result.fail(403, "请先绑定学生/志愿者身份后再查看收藏");
        }
        return Result.ok(qaService.listMyFavorites(queryDTO, relatedId, relatedType));
    }

    /* ==================== 私有方法 ==================== */

    /** 点赞通用入口：校验身份后调用服务 */
    private Map<String, Object> interaction(Integer id, Integer relatedId, String relatedType, String targetType) {
        if (relatedId == null || !isValidIdentity(relatedType)) {
            throw new com.demo.miniprogram.common.BusinessException(
                    com.demo.miniprogram.common.ResultCode.FORBIDDEN, "请先绑定学生/志愿者身份后再点赞");
        }
        return qaService.toggleLike(id, relatedId, relatedType, targetType);
    }

    /** 身份类型校验：仅学生/志愿者可点赞收藏 */
    private boolean isValidIdentity(String relatedType) {
        return "student".equals(relatedType) || "volunteer".equals(relatedType);
    }
}
