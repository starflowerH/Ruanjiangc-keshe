package com.demo.miniprogram.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.miniprogram.entity.QuestionAnswer;
import com.demo.miniprogram.vo.QAVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 问答 Mapper
 */
@Mapper
public interface QuestionAnswerMapper extends BaseMapper<QuestionAnswer> {

    /**
     * 分页查询问答列表（支持分类筛选）
     */
    @Select("<script>"
            + "SELECT qa.id, qa.title, qa.category, qa.question_content, qa.answer_content, "
            + "       qa.view_count, qa.asked_at, qa.answered_at, qa.audit_status, qa.is_public, "
            + "       qa.author_type, qa.university_id, qa.is_featured, pu.name AS university_name, "
            + "       COALESCE(hss.nickname, hss.name, av.name) AS asker_name, "
            + "       COALESCE(uw.avatar_url, uw2.avatar_url, hss.avatar_url) AS asker_avatar, "
            + "       COALESCE(hs.name, au.name) AS asker_high_school, "
            + "       v.name AS answerer_name, "
            + "       uwv.avatar_url AS answerer_avatar, "
            + "       u.name AS answerer_university, "
            + "       (SELECT COUNT(*) FROM forum_comment fc WHERE fc.post_id = qa.id) AS comment_count "
            + "FROM Youtube qa "
            + "LEFT JOIN high_school_student hss ON qa.asker_student_id = hss.id "
            + "LEFT JOIN user_wechat uw ON hss.openid = uw.openid "
            + "LEFT JOIN high_school hs ON hss.high_school_id = hs.id "
            + "LEFT JOIN volunteer av ON qa.asker_volunteer_id = av.id "
            + "LEFT JOIN user_wechat uw2 ON av.openid = uw2.openid "
            + "LEFT JOIN university au ON av.university_id = au.id "
            + "LEFT JOIN university pu ON qa.university_id = pu.id "
            + "LEFT JOIN volunteer v ON qa.target_volunteer_id = v.id "
            + "LEFT JOIN user_wechat uwv ON v.openid = uwv.openid "
            + "LEFT JOIN university u ON v.university_id = u.id "
            + "<where>"
            + "  <if test='askerStudentId != null'> AND qa.asker_student_id = #{askerStudentId} </if>"
            + "  <if test='targetVolunteerId != null'> AND qa.target_volunteer_id = #{targetVolunteerId} </if>"
            + "  <if test='universityId != null'> AND v.university_id = #{universityId} </if>"
            + "  <if test='auditStatus != null and auditStatus != \"\"'> AND qa.audit_status = #{auditStatus} </if>"
            + "  <if test='isPublic != null'> AND qa.is_public = #{isPublic} </if>"
            + "  <if test='category != null and category != \"\" and category != \"all\"'> AND qa.category = #{category} </if>"
            + "</where>"
            + "ORDER BY qa.asked_at DESC"
            + "</script>")
    IPage<QAVO> selectQAPage(Page<QAVO> page,
                             @Param("askerStudentId") Integer askerStudentId,
                             @Param("targetVolunteerId") Integer targetVolunteerId,
                             @Param("universityId") Integer universityId,
                             @Param("auditStatus") String auditStatus,
                             @Param("isPublic") Integer isPublic,
                             @Param("category") String category);

    /**
     * 统计待回答问题数量
     */
    @Select("SELECT COUNT(*) FROM Youtube WHERE target_volunteer_id = #{volunteerId} AND answer_content IS NULL")
    int countUnanswered(@Param("volunteerId") Integer volunteerId);

    /**
     * 查询单个问答详情
     */
    @Select("<script>"
            + "SELECT qa.id, qa.title, qa.category, qa.question_content, qa.answer_content, "
            + "       qa.view_count, qa.asked_at, qa.answered_at, qa.audit_status, qa.is_public, "
            + "       qa.author_type, qa.university_id, qa.is_featured, pu.name AS university_name, "
            + "       COALESCE(hss.nickname, hss.name, av.name) AS asker_name, "
            + "       COALESCE(uw.avatar_url, uw2.avatar_url, hss.avatar_url) AS asker_avatar, "
            + "       COALESCE(hs.name, au.name) AS asker_high_school, "
            + "       v.name AS answerer_name, "
            + "       uwv.avatar_url AS answerer_avatar, "
            + "       u.name AS answerer_university, "
            + "       (SELECT COUNT(*) FROM forum_comment fc WHERE fc.post_id = qa.id) AS comment_count, "
            + "       (SELECT COUNT(*) FROM qa_like ql WHERE ql.qa_id = qa.id AND ql.target_type = 'post') AS like_count, "
            + "       (SELECT COUNT(*) FROM qa_favorite qf WHERE qf.qa_id = qa.id) AS favorite_count, "
            + "       (SELECT COUNT(*) FROM qa_like qla WHERE qla.qa_id = qa.id AND qla.target_type = 'answer') AS answer_like_count, "
            + "       (SELECT COUNT(*) FROM qa_like ql2 WHERE ql2.qa_id = qa.id AND ql2.target_type = 'post' "
            + "        <if test='relatedId == null or identityRole == null'>"
            + "           AND 1 = 0"
            + "        </if>"
            + "        <if test='relatedId != null and identityRole != null'>"
            + "           AND ql2.user_id = #{relatedId} AND ql2.user_type = #{identityRole}"
            + "        </if>) AS liked, "
            + "       (SELECT COUNT(*) FROM qa_favorite qf2 WHERE qf2.qa_id = qa.id "
            + "        <if test='relatedId == null or identityRole == null'>"
            + "           AND 1 = 0"
            + "        </if>"
            + "        <if test='relatedId != null and identityRole != null'>"
            + "           AND qf2.user_id = #{relatedId} AND qf2.user_type = #{identityRole}"
            + "        </if>) AS favorited, "
            + "       (SELECT COUNT(*) FROM qa_like qla2 WHERE qla2.qa_id = qa.id AND qla2.target_type = 'answer' "
            + "        <if test='relatedId == null or identityRole == null'>"
            + "           AND 1 = 0"
            + "        </if>"
            + "        <if test='relatedId != null and identityRole != null'>"
            + "           AND qla2.user_id = #{relatedId} AND qla2.user_type = #{identityRole}"
            + "        </if>) AS answer_liked "
            + "FROM Youtube qa "
            + "LEFT JOIN high_school_student hss ON qa.asker_student_id = hss.id "
            + "LEFT JOIN user_wechat uw ON hss.openid = uw.openid "
            + "LEFT JOIN high_school hs ON hss.high_school_id = hs.id "
            + "LEFT JOIN volunteer av ON qa.asker_volunteer_id = av.id "
            + "LEFT JOIN user_wechat uw2 ON av.openid = uw2.openid "
            + "LEFT JOIN university au ON av.university_id = au.id "
            + "LEFT JOIN university pu ON qa.university_id = pu.id "
            + "LEFT JOIN volunteer v ON qa.target_volunteer_id = v.id "
            + "LEFT JOIN user_wechat uwv ON v.openid = uwv.openid "
            + "LEFT JOIN university u ON v.university_id = u.id "
            + "WHERE qa.id = #{id} "
            + "  AND ("
            // 论坛公开帖：所有已审核通过且公开的内容（is_public=1 是唯一公开口径）
            + "       (qa.target_volunteer_id IS NULL AND qa.audit_status = 'APPROVED' AND qa.is_public = 1) "
            // 超级管理员：可查看全部（普通管理员/老师仅按公开口径，防止越权读取一对一私密问答）
            + "       OR (#{isSuperAdmin} = 1) "
            // 一对一私密问答参与者：提问者或目标志愿者可查看待审核/已通过（REJECTED 不可见，仅超管可见）
            + "       OR (#{identityRole} = 'volunteer' AND (qa.asker_volunteer_id = #{relatedId} OR qa.target_volunteer_id = #{relatedId}) AND qa.audit_status &lt;&gt; 'REJECTED') "
            + "       OR (#{identityRole} = 'student' AND qa.asker_student_id = #{relatedId} AND qa.audit_status &lt;&gt; 'REJECTED')"
            + "  )"
            + "</script>")
    QAVO selectQADetail(@Param("id") Integer id,
                        @Param("relatedId") Integer relatedId,
                        @Param("identityRole") String identityRole,
                        @Param("userType") String userType,
                        @Param("isSuperAdmin") Integer isSuperAdmin);

    /**
     * 查询未回答问题
     */
    @Select("SELECT qa.id, qa.title, qa.category, qa.question_content, qa.answer_content, "
            + "       qa.view_count, qa.is_featured, qa.asked_at, qa.answered_at, qa.audit_status, qa.is_public, "
            + "       COALESCE(hss.nickname, hss.name, av.name) AS asker_name, "
            + "       COALESCE(uw.avatar_url, uw2.avatar_url, hss.avatar_url) AS asker_avatar, "
            + "       COALESCE(hs.name, au.name) AS asker_high_school, "
            + "       v.name AS answerer_name, "
            + "       uwv.avatar_url AS answerer_avatar, "
            + "       u.name AS answerer_university "
            + "FROM Youtube qa "
            + "LEFT JOIN high_school_student hss ON qa.asker_student_id = hss.id "
            + "LEFT JOIN user_wechat uw ON hss.openid = uw.openid "
            + "LEFT JOIN high_school hs ON hss.high_school_id = hs.id "
            + "LEFT JOIN volunteer av ON qa.asker_volunteer_id = av.id "
            + "LEFT JOIN user_wechat uw2 ON av.openid = uw2.openid "
            + "LEFT JOIN university au ON av.university_id = au.id "
            + "LEFT JOIN volunteer v ON qa.target_volunteer_id = v.id "
            + "LEFT JOIN user_wechat uwv ON v.openid = uwv.openid "
            + "LEFT JOIN university u ON v.university_id = u.id "
            + "WHERE qa.target_volunteer_id = #{volunteerId} AND qa.answer_content IS NULL "
            + "ORDER BY qa.asked_at DESC")
    IPage<QAVO> selectUnanswered(Page<QAVO> page, @Param("volunteerId") Integer volunteerId);

    /** 查询当前志愿者已回答的一对一问答。 */
    @Select("SELECT qa.id, qa.title, qa.category, qa.question_content, qa.answer_content, "
            + "       qa.view_count, qa.is_featured, qa.asked_at, qf.created_at AS activity_at, qa.answered_at, qa.audit_status, qa.is_public, "
            + "       COALESCE(hss.nickname, hss.name, av.name) AS asker_name, "
            + "       COALESCE(uw.avatar_url, uw2.avatar_url, hss.avatar_url) AS asker_avatar, "
            + "       COALESCE(hs.name, au.name) AS asker_high_school, "
            + "       v.name AS answerer_name, uwv.avatar_url AS answerer_avatar, u.name AS answerer_university "
            + "FROM Youtube qa "
            + "LEFT JOIN high_school_student hss ON qa.asker_student_id = hss.id "
            + "LEFT JOIN user_wechat uw ON hss.openid = uw.openid "
            + "LEFT JOIN high_school hs ON hss.high_school_id = hs.id "
            + "LEFT JOIN volunteer av ON qa.asker_volunteer_id = av.id "
            + "LEFT JOIN user_wechat uw2 ON av.openid = uw2.openid "
            + "LEFT JOIN university au ON av.university_id = au.id "
            + "LEFT JOIN volunteer v ON qa.target_volunteer_id = v.id "
            + "LEFT JOIN user_wechat uwv ON v.openid = uwv.openid "
            + "LEFT JOIN university u ON v.university_id = u.id "
            + "WHERE qa.target_volunteer_id = #{volunteerId} "
            + "  AND qa.answer_content IS NOT NULL "
            + "ORDER BY qa.answered_at DESC")
    IPage<QAVO> selectAnsweredByVolunteer(Page<QAVO> page, @Param("volunteerId") Integer volunteerId);

    /** 当前志愿者发布且已审核通过的论坛帖子。 */
    @Select("SELECT qa.id, qa.title, qa.category, qa.question_content, qa.answer_content, "
            + "       qa.view_count, qa.asked_at, qa.asked_at AS activity_at, qa.answered_at, qa.audit_status, qa.is_public, "
            + "       qa.author_type, qa.university_id, qa.is_featured, pu.name AS university_name, "
            + "       v.name AS asker_name, uwv.avatar_url AS asker_avatar, u.name AS asker_high_school, "
            + "       (SELECT COUNT(*) FROM forum_comment fc WHERE fc.post_id = qa.id) AS comment_count "
            + "FROM Youtube qa "
            + "LEFT JOIN volunteer v ON qa.asker_volunteer_id = v.id "
            + "LEFT JOIN user_wechat uwv ON v.openid = uwv.openid "
            + "LEFT JOIN university u ON v.university_id = u.id "
            + "LEFT JOIN university pu ON qa.university_id = pu.id "
            + "WHERE qa.asker_volunteer_id = #{volunteerId} "
            + "  AND qa.target_volunteer_id IS NULL "
            + "  AND qa.audit_status = 'APPROVED' AND qa.is_public = 1 "
            + "ORDER BY qa.asked_at DESC")
    IPage<QAVO> selectMyForumPosts(Page<QAVO> page, @Param("volunteerId") Integer volunteerId);

    /**
     * 今日热搜 — 热度 = 浏览量 + 评论数×5
     */
    @Select("SELECT qa.id, qa.title, qa.category, qa.question_content, qa.answer_content, qa.author_type, "
            + "       qa.view_count, qa.is_featured, qa.asked_at, qa.answered_at, qa.audit_status, qa.is_public, "
            + "       COALESCE(hss.nickname, hss.name, av.name) AS asker_name, "
            + "       COALESCE(uw.avatar_url, uw2.avatar_url, hss.avatar_url) AS asker_avatar, "
            + "       COALESCE(hs.name, au.name) AS asker_high_school, "
            + "       (SELECT COUNT(*) FROM forum_comment fc WHERE fc.post_id = qa.id) AS comment_count "
            + "FROM Youtube qa "
            + "LEFT JOIN high_school_student hss ON qa.asker_student_id = hss.id "
            + "LEFT JOIN user_wechat uw ON hss.openid = uw.openid "
            + "LEFT JOIN high_school hs ON hss.high_school_id = hs.id "
            + "LEFT JOIN volunteer av ON qa.asker_volunteer_id = av.id "
            + "LEFT JOIN user_wechat uw2 ON av.openid = uw2.openid "
            + "LEFT JOIN university au ON av.university_id = au.id "
            + "WHERE qa.target_volunteer_id IS NULL AND qa.audit_status = 'APPROVED' AND qa.is_public = 1 "
            + "ORDER BY (qa.view_count + (SELECT COUNT(*) FROM forum_comment fc2 WHERE fc2.post_id = qa.id) * 5) DESC "
            + "LIMIT #{limit}")
    List<QAVO> selectHotQuestions(@Param("limit") int limit);

    /** 论坛帖子列表：身份、大学、分类、精华和排序可组合。 */
    @Select("<script>"
            + "SELECT qa.id, qa.title, qa.category, qa.question_content, qa.answer_content, "
            + "       qa.view_count, qa.asked_at, qa.answered_at, qa.audit_status, qa.is_public, "
            + "       qa.author_type, qa.university_id, qa.is_featured, pu.name AS university_name, "
            + "       COALESCE(hss.nickname, hss.name, av.name) AS asker_name, "
            + "       COALESCE(uw.avatar_url, uw2.avatar_url, hss.avatar_url) AS asker_avatar, "
            + "       COALESCE(hs.name, au.name) AS asker_high_school, "
            + "       (SELECT COUNT(*) FROM forum_comment fc WHERE fc.post_id = qa.id) AS comment_count, "
            + "       (SELECT COUNT(*) FROM qa_like ql WHERE ql.qa_id = qa.id AND ql.target_type = 'post') AS like_count, "
            + "       (SELECT COUNT(*) FROM qa_favorite qf WHERE qf.qa_id = qa.id) AS favorite_count, "
            + "       (qa.view_count "
            + "        + (SELECT COUNT(*) FROM forum_comment fc2 WHERE fc2.post_id = qa.id) * 5 "
            + "        + (SELECT COUNT(*) FROM qa_like ql2 WHERE ql2.qa_id = qa.id AND ql2.target_type = 'post') * 3 "
            + "        + (SELECT COUNT(*) FROM qa_favorite qf2 WHERE qf2.qa_id = qa.id) * 4) AS heat_score "
            + "FROM Youtube qa "
            + "LEFT JOIN high_school_student hss ON qa.asker_student_id = hss.id "
            + "LEFT JOIN user_wechat uw ON hss.openid = uw.openid "
            + "LEFT JOIN high_school hs ON hss.high_school_id = hs.id "
            + "LEFT JOIN volunteer av ON qa.asker_volunteer_id = av.id "
            + "LEFT JOIN user_wechat uw2 ON av.openid = uw2.openid "
            + "LEFT JOIN university au ON av.university_id = au.id "
            + "LEFT JOIN university pu ON qa.university_id = pu.id "
            + "<where>"
            + "  qa.target_volunteer_id IS NULL AND qa.audit_status = 'APPROVED' AND qa.is_public = 1 "
            + "  <if test='category != null and category != \"\" and category != \"all\"'> AND qa.category = #{category} </if>"
            + "  <if test='authorType != null and authorType != \"\" and authorType != \"all\"'> AND qa.author_type = #{authorType} </if>"
            + "  <if test='universityId != null'> AND qa.university_id = #{universityId} </if>"
            + "  <if test='featured != null and featured == 1'> AND qa.is_featured = 1 </if>"
            + "</where>"
            + "<choose>"
            + "  <when test='sort == \"heat\"'> ORDER BY heat_score DESC, qa.asked_at DESC </when>"
            + "  <when test='sort == \"featured\"'> ORDER BY qa.is_featured DESC, heat_score DESC, qa.asked_at DESC </when>"
            + "  <otherwise> ORDER BY qa.asked_at DESC </otherwise>"
            + "</choose>"
            + "</script>")
    IPage<QAVO> selectForumPosts(Page<QAVO> page,
                                 @Param("category") String category,
                                 @Param("authorType") String authorType,
                                 @Param("universityId") Integer universityId,
                                 @Param("featured") Integer featured,
                                 @Param("sort") String sort);

    /**
     * 我的收藏帖子分页（按收藏时间倒序）
     */
    @Select("SELECT qa.id, qa.title, qa.category, qa.question_content, qa.answer_content, qa.author_type, "
            + "       qa.view_count, qa.is_featured, qa.asked_at, qa.answered_at, qa.audit_status, qa.is_public, "
            + "       COALESCE(hss.nickname, hss.name, av.name) AS asker_name, "
            + "       COALESCE(uw.avatar_url, uw2.avatar_url, hss.avatar_url) AS asker_avatar, "
            + "       COALESCE(hs.name, au.name) AS asker_high_school, "
            + "       v.name AS answerer_name, "
            + "       uwv.avatar_url AS answerer_avatar, "
            + "       u.name AS answerer_university, "
            + "       (SELECT COUNT(*) FROM forum_comment fc WHERE fc.post_id = qa.id) AS comment_count, "
            + "       (SELECT COUNT(*) FROM qa_like ql WHERE ql.qa_id = qa.id AND ql.target_type = 'post') AS like_count, "
            + "       (SELECT COUNT(*) FROM qa_favorite qf WHERE qf.qa_id = qa.id) AS favorite_count "
            + "FROM Youtube qa "
            + "INNER JOIN qa_favorite qf ON qf.qa_id = qa.id AND qf.user_id = #{userId} AND qf.user_type = #{userType} "
            + "LEFT JOIN high_school_student hss ON qa.asker_student_id = hss.id "
            + "LEFT JOIN user_wechat uw ON hss.openid = uw.openid "
            + "LEFT JOIN high_school hs ON hss.high_school_id = hs.id "
            + "LEFT JOIN volunteer av ON qa.asker_volunteer_id = av.id "
            + "LEFT JOIN user_wechat uw2 ON av.openid = uw2.openid "
            + "LEFT JOIN university au ON av.university_id = au.id "
            + "LEFT JOIN volunteer v ON qa.target_volunteer_id = v.id "
            + "LEFT JOIN user_wechat uwv ON v.openid = uwv.openid "
            + "LEFT JOIN university u ON v.university_id = u.id "
            + "WHERE qa.audit_status = 'APPROVED' AND qa.is_public = 1 AND qa.target_volunteer_id IS NULL "
            + "ORDER BY qf.created_at DESC")
    IPage<QAVO> selectFavoritePage(Page<QAVO> page,
                                   @Param("userId") Integer userId,
                                   @Param("userType") String userType);
}
