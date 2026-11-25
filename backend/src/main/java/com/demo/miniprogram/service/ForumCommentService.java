package com.demo.miniprogram.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.miniprogram.common.BusinessException;
import com.demo.miniprogram.dto.CommentDTO;
import com.demo.miniprogram.entity.ForumComment;
import com.demo.miniprogram.entity.ForumCommentLike;
import com.demo.miniprogram.entity.HighSchoolStudent;
import com.demo.miniprogram.entity.QuestionAnswer;
import com.demo.miniprogram.entity.Volunteer;
import com.demo.miniprogram.entity.UserWechat;
import com.demo.miniprogram.mapper.ForumCommentMapper;
import com.demo.miniprogram.mapper.HighSchoolStudentMapper;
import com.demo.miniprogram.mapper.QuestionAnswerMapper;
import com.demo.miniprogram.mapper.VolunteerMapper;
import com.demo.miniprogram.mapper.UserWechatMapper;
import com.demo.miniprogram.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/**
 * 论坛评论服务
 */
@Service
@RequiredArgsConstructor
public class ForumCommentService {

    private final ForumCommentMapper commentMapper;
    private final com.demo.miniprogram.mapper.ForumCommentLikeMapper commentLikeMapper;
    private final QuestionAnswerMapper qaMapper;
    private final HighSchoolStudentMapper studentMapper;
    private final VolunteerMapper volunteerMapper;
    private final UserWechatMapper userWechatMapper;
    private final NotificationService notificationService;
    private final ForumModerationService forumModerationService;

    /**
     * 添加评论
     */
    @Transactional(rollbackFor = Exception.class)
    public ForumComment addComment(Integer userId, String userType, CommentDTO dto) {
        forumModerationService.assertNotMuted(userId, userType);
        forumModerationService.validateContent(dto.getContent());
        // 验证帖子是否存在且为公开论坛帖（一对一私密问答不允许评论）
        QuestionAnswer post = qaMapper.selectById(dto.getPostId());
        if (post == null || !"APPROVED".equals(post.getAuditStatus())
                || !Integer.valueOf(1).equals(post.getIsPublic())
                || post.getTargetVolunteerId() != null) {
            throw new BusinessException("帖子不存在或已下架");
        }

        // 如果是回复某条评论，验证父评论是否存在
        if (dto.getParentId() != null) {
            ForumComment parentComment = commentMapper.selectById(dto.getParentId());
            if (parentComment == null || !parentComment.getPostId().equals(dto.getPostId())) {
                throw new BusinessException("父评论不存在");
            }
        }

        // 创建评论
        ForumComment comment = new ForumComment();
        comment.setPostId(dto.getPostId());
        comment.setUserId(userId);
        comment.setUserType(userType);
        comment.setContent(dto.getContent());
        comment.setParentId(dto.getParentId());
        commentMapper.insert(comment);

        // 发送通知给帖子作者（从帖子自身字段判断作者身份）
        Integer postAuthorId = post.getAskerVolunteerId() != null ? post.getAskerVolunteerId() : post.getAskerStudentId();
        String postAuthorType = post.getAskerVolunteerId() != null ? "volunteer" : "student";

        if (postAuthorId != null && !postAuthorId.equals(userId)) {
            String title = post.getTitle() != null ? post.getTitle() :
                    (post.getQuestionContent() != null ? post.getQuestionContent().substring(0, Math.min(post.getQuestionContent().length(), 20)) : "新评论");
            notificationService.createCommentNotification(postAuthorId, postAuthorType, title, post.getId());
        }

        // 如果是回复某条评论，通知被回复的人
        if (dto.getParentId() != null) {
            ForumComment parentComment = commentMapper.selectById(dto.getParentId());
            if (parentComment != null && !parentComment.getUserId().equals(userId)) {
                String title = post.getTitle() != null ? post.getTitle() :
                        (post.getQuestionContent() != null ? post.getQuestionContent().substring(0, Math.min(post.getQuestionContent().length(), 20)) : "回复");
                notificationService.createReplyCommentNotification(parentComment.getUserId(), parentComment.getUserType(), title, post.getId());
            }
        }

        return comment;
    }

    /**
     * 获取帖子的评论列表（树形结构）
     *
     * 可见性：仅公开论坛帖可读评论（下架/私密帖子的评论不对外泄露）
     */
    public List<CommentVO> getCommentsByPostId(Integer postId, Integer currentUserId, String currentUserType) {
        QuestionAnswer post = qaMapper.selectById(postId);
        if (post == null || !"APPROVED".equals(post.getAuditStatus())
                || !Integer.valueOf(1).equals(post.getIsPublic())
                || post.getTargetVolunteerId() != null) {
            return new ArrayList<>();
        }
        // 查询所有评论
        LambdaQueryWrapper<ForumComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForumComment::getPostId, postId)
               .eq(ForumComment::getStatus, "VISIBLE")
               .orderByAsc(ForumComment::getCreatedAt);
        List<ForumComment> comments = commentMapper.selectList(wrapper);

        // 批量加载评论者姓名（消除逐条查询用户造成的 N+1）
        Map<String, String> userNameMap = loadUserNames(comments);
        Map<Integer, Long> likeCounts = new java.util.HashMap<>();
        Set<Integer> likedCommentIds = new java.util.HashSet<>();
        if (!comments.isEmpty()) {
            List<ForumCommentLike> likes = commentLikeMapper.selectList(new LambdaQueryWrapper<ForumCommentLike>()
                    .in(ForumCommentLike::getCommentId, comments.stream().map(ForumComment::getId).collect(Collectors.toList())));
            for (ForumCommentLike like : likes) {
                likeCounts.merge(like.getCommentId(), 1L, Long::sum);
                if (like.getUserId().equals(currentUserId) && like.getUserType().equals(currentUserType)) likedCommentIds.add(like.getCommentId());
            }
        }

        // 转换为 VO
        List<CommentVO> voList = comments.stream().map(c -> {
            CommentVO vo = new CommentVO();
            vo.setId(c.getId());
            vo.setPostId(c.getPostId());
            vo.setUserId(c.getUserId());
            vo.setUserType(c.getUserType());
            vo.setContent(c.getContent());
            vo.setParentId(c.getParentId());
            vo.setCreatedAt(c.getCreatedAt());
            vo.setLikeCount(likeCounts.getOrDefault(c.getId(), 0L));
            vo.setLiked(likedCommentIds.contains(c.getId()));

            // 获取评论者姓名（批量缓存）
            vo.setUserName(userNameMap.getOrDefault(c.getUserId() + ":" + c.getUserType(), "未知用户"));

            // 获取父评论者姓名
            if (c.getParentId() != null) {
                ForumComment parent = comments.stream()
                        .filter(p -> p.getId().equals(c.getParentId()))
                        .findFirst()
                        .orElse(null);
                if (parent != null) {
                    vo.setParentUserName(userNameMap.getOrDefault(parent.getUserId() + ":" + parent.getUserType(), "未知用户"));
                }
            }

            return vo;
        }).collect(Collectors.toList());

        // 构建树形结构
        Map<Integer, List<CommentVO>> parentMap = voList.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(CommentVO::getParentId));

        voList.forEach(c -> c.setChildren(parentMap.getOrDefault(c.getId(), new ArrayList<>())));

        return voList.stream()
                .filter(c -> c.getParentId() == null)
                .collect(Collectors.toList());
    }

    /**
     * 获取当前用户发表过的评论列表（"我的评论"）
     *
     * @param userId    当前用户ID（student/volunteer 的 relatedId）
     * @param userType  身份类型（student/volunteer）
     * @param page      页码（从1开始）
     * @param size      每页条数
     * @return 评论列表，附带对应帖子标题（可按帖子跳转）
     */
    public Map<String, Object> getMyComments(Integer userId, String userType, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(50, size));

        // 兼容历史数据：早期评论 user_type 存的是 "wechat"（volunteer 身份），
        // 查询时按 user_id 匹配 + user_type 为当前身份或历史 wechat 值。
        LambdaQueryWrapper<ForumComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForumComment::getUserId, userId)
               .eq(ForumComment::getStatus, "VISIBLE")
               .and(w -> {
                   w.eq(ForumComment::getUserType, userType);
                   if ("volunteer".equals(userType)) {
                       w.or().eq(ForumComment::getUserType, "wechat");
                   }
               })
               .orderByDesc(ForumComment::getCreatedAt)
               .last("LIMIT " + (safePage - 1) * safeSize + ", " + safeSize);
        List<ForumComment> comments = commentMapper.selectList(wrapper);

        // 总条数（用于分页）
        Long total = commentMapper.selectCount(
                new LambdaQueryWrapper<ForumComment>()
                        .eq(ForumComment::getUserId, userId)
                        .eq(ForumComment::getStatus, "VISIBLE")
                        .and(w -> {
                            w.eq(ForumComment::getUserType, userType);
                            if ("volunteer".equals(userType)) {
                                w.or().eq(ForumComment::getUserType, "wechat");
                            }
                        })
        );

        // 批量加载评论者姓名
        Map<String, String> userNameMap = loadUserNames(comments);

        // 批量加载帖子标题（消除 N+1）
        Map<Integer, QuestionAnswer> postMap = new java.util.HashMap<>();
        List<Integer> postIds = comments.stream()
                .map(ForumComment::getPostId).distinct().collect(Collectors.toList());
        if (!postIds.isEmpty()) {
            for (QuestionAnswer qa : qaMapper.selectBatchIds(postIds)) {
                postMap.put(qa.getId(), qa);
            }
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (ForumComment c : comments) {
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("postId", c.getPostId());
            item.put("content", c.getContent());
            item.put("parentId", c.getParentId());
            item.put("createdAt", c.getCreatedAt());
            item.put("userName", userNameMap.getOrDefault(c.getUserId() + ":" + c.getUserType(), "未知用户"));

            QuestionAnswer post = postMap.get(c.getPostId());
            if (post != null) {
                item.put("postTitle", post.getTitle() != null ? post.getTitle()
                        : (post.getQuestionContent() != null
                            ? post.getQuestionContent().substring(0, Math.min(post.getQuestionContent().length(), 30))
                            : "帖子"));
            } else {
                item.put("postTitle", "帖子已删除");
            }
            list.add(item);
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("records", list);
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleLike(Integer commentId, Integer userId, String userType) {
        ForumComment comment = commentMapper.selectById(commentId);
        if (comment == null) throw new BusinessException("评论不存在");
        LambdaQueryWrapper<ForumCommentLike> query = new LambdaQueryWrapper<ForumCommentLike>()
                .eq(ForumCommentLike::getCommentId, commentId)
                .eq(ForumCommentLike::getUserId, userId)
                .eq(ForumCommentLike::getUserType, userType);
        ForumCommentLike existing = commentLikeMapper.selectOne(query);
        boolean liked;
        if (existing == null) {
            ForumCommentLike like = new ForumCommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            like.setUserType(userType);
            commentLikeMapper.insert(like);
            liked = true;
        } else {
            commentLikeMapper.deleteById(existing.getId());
            liked = false;
        }
        Long count = commentLikeMapper.selectCount(new LambdaQueryWrapper<ForumCommentLike>().eq(ForumCommentLike::getCommentId, commentId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("liked", liked);
        result.put("likeCount", count);
        return result;
    }

    /**
     * 批量加载评论者的姓名（按 user_id+user_type 分组缓存，消除 N+1 查询）
     *
     * 兼容历史数据：早期评论 user_type 存的是 "wechat"（见 getUserName 注释），
     * 按 volunteer → student 顺序解析，保证历史评论作者名不显示"未知用户"。
     */
    private Map<String, String> loadUserNames(List<ForumComment> comments) {
        Map<String, String> result = new java.util.HashMap<>();
        if (comments == null || comments.isEmpty()) {
            return result;
        }
        List<Integer> volunteerIds = new ArrayList<>();
        List<Integer> studentIds = new ArrayList<>();
        Set<String> seen = new java.util.HashSet<>();
        for (ForumComment c : comments) {
            String key = c.getUserId() + ":" + c.getUserType();
            if (!seen.add(key)) continue;
            if ("volunteer".equals(c.getUserType()) || "wechat".equals(c.getUserType())) {
                volunteerIds.add(c.getUserId());
            } else if ("student".equals(c.getUserType())) {
                studentIds.add(c.getUserId());
            }
        }
        if (!volunteerIds.isEmpty()) {
            Map<Integer, Volunteer> volunteers = volunteerMapper.selectBatchIds(volunteerIds).stream()
                    .collect(Collectors.toMap(Volunteer::getId, v -> v));
            for (Integer id : volunteerIds) {
                Volunteer v = volunteers.get(id);
                if (v != null) {
                    // 志愿者统一显示报名真名（与登录/资料页一致）
                    result.put(id + ":volunteer", v.getName() != null ? v.getName() : "未知用户");
                    result.put(id + ":wechat", v.getName() != null ? v.getName() : "未知用户");
                }
            }
        }
        if (!studentIds.isEmpty()) {
            Map<Integer, HighSchoolStudent> students = studentMapper.selectBatchIds(studentIds).stream()
                    .collect(Collectors.toMap(HighSchoolStudent::getId, s -> s));
            for (Integer id : studentIds) {
                HighSchoolStudent s = students.get(id);
                if (s != null) {
                    String name = s.getNickname() != null && !s.getNickname().isBlank()
                            ? s.getNickname()
                            : (s.getName() != null && !s.getName().isBlank() ? s.getName() : "学生" + s.getId());
                    result.put(id + ":student", name);
                    result.put(id + ":wechat", name);
                }
            }
        }
        // 历史 wechat 评论的作者若两边都查不到（如 user_id 是 admin 空间），保持原 getUserName 语义返回未知
        return result;
    }

    /**
     * 获取用户姓名
     */
    private String getUserName(Integer userId, String userType) {
        // JWT-backed requests use the generic "wechat" type; resolve a volunteer
        // first so existing comments created before type normalization keep names.
        if ("volunteer".equals(userType) || "wechat".equals(userType)) {
            Volunteer volunteer = volunteerMapper.selectById(userId);
            if (volunteer != null) {
                // 志愿者统一显示报名真名（与登录/资料页一致）
                return volunteer.getName();
            }
            if ("volunteer".equals(userType)) {
                return "未知用户";
            }
        }
        // 学生显示身份昵称/真名（与登录/资料页一致）
        HighSchoolStudent student = studentMapper.selectById(userId);
        if (student != null) {
            if (student.getNickname() != null && !student.getNickname().isBlank()) {
                return student.getNickname();
            }
            if (student.getName() != null && !student.getName().isBlank()) {
                return student.getName();
            }
            return "学生" + student.getId();
        }
        return "未知用户";
    }
}
