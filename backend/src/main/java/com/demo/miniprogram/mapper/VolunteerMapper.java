package com.demo.miniprogram.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.miniprogram.entity.Volunteer;
import com.demo.miniprogram.vo.MemberVO;
import com.demo.miniprogram.vo.VolunteerPublicVO;
import com.demo.miniprogram.vo.VolunteerVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 志愿者报名 Mapper
 *
 * 包含自定义多表联查SQL，用于：
 *   - 分页查询志愿者列表（关联大学名、高中名）
 *   - 按高中查询去过该校宣传的大学生列表（高中生视角）
 *   - 查询队伍成员
 *
 * @author demo
 */
@Mapper
public interface VolunteerMapper extends BaseMapper<Volunteer> {

    /**
     * 分页查询志愿者列表 - 联表查询大学名和高中名
     *
     * @param page         分页参数
     * @param universityId 大学ID筛选（可选，为null则不筛选）
     * @param highSchoolId 高中ID筛选（可选）
     * @param status       状态筛选（可选）
     * @param keyword      关键词搜索（姓名/手机号模糊匹配，可选）
     * @return 分页的志愿者视图数据
     */
    @Select("<script>"
            + "SELECT v.id, v.name, v.phone, v.wechat_id, v.student_no, v.gender, v.email, v.grade, v.major, "
            + "       v.is_captain_applicant, v.status, v.created_at, v.cancelled_at, "
            + "       u.name AS university_name, "
            + "       h.name AS high_school_name, h.province AS high_school_province, h.city AS high_school_city, "
            + "       EXISTS(SELECT 1 FROM forum_moderator_grant fm WHERE fm.volunteer_id = v.id AND fm.active = 1) AS forum_moderator_granted, "
            + "       EXISTS(SELECT 1 FROM volunteer_promotion_grant pg WHERE pg.volunteer_id = v.id AND pg.active = 1) AS promotion_manager_granted "
            + "FROM volunteer v "
            + "LEFT JOIN university u ON v.university_id = u.id "
            + "LEFT JOIN high_school h ON v.target_high_school_id = h.id "
            + "<where>"
            + "  <if test='universityId != null'> AND v.university_id = #{universityId} </if>"
            + "  <if test='city != null and city != \"\"'> AND u.city = #{city} </if>"
            + "  <if test='highSchoolId != null'> AND v.target_high_school_id = #{highSchoolId} </if>"
            + "  <if test='status != null and status != \"\"'> AND v.status = #{status} </if>"
            + "  <if test='keyword != null and keyword != \"\"'> AND (v.name LIKE CONCAT('%',#{keyword},'%') OR v.phone LIKE CONCAT('%',#{keyword},'%')) </if>"
            + "</where>"
            + "ORDER BY v.created_at DESC"
            + "</script>")
    IPage<VolunteerVO> selectVolunteerPage(Page<VolunteerVO> page,
                                           @Param("universityId") Integer universityId,
                                           @Param("city") String city,
                                           @Param("highSchoolId") Integer highSchoolId,
                                           @Param("status") String status,
                                           @Param("keyword") String keyword);

    /**
     * 【公开接口】根据高中ID查询学长学姐列表（高中生视角）
     * 只返回公开信息：姓名、大学、专业、年级，不包含手机号/微信号
     * 自动排除 REJECTED（已拒绝）的报名
     */
    @Select("SELECT v.id, v.name, v.grade, v.major, v.is_captain_applicant, "
            + "       uw.avatar_url AS avatar_url, "
            + "       u.name AS university_name "
            + "FROM volunteer v "
            + "LEFT JOIN university u ON v.university_id = u.id "
            + "LEFT JOIN user_wechat uw ON v.openid = uw.openid "
            + "WHERE v.target_high_school_id = #{highSchoolId} "
            + "  AND v.status = 'APPROVED' "
            + "ORDER BY u.name, v.is_captain_applicant DESC")
    List<VolunteerPublicVO> selectPublicByHighSchool(@Param("highSchoolId") Integer highSchoolId);

    /**
     * 根据openid查询自己的报名记录（分页）
     */
    @Select("SELECT v.id, v.name, v.phone, v.wechat_id, v.student_no, v.gender, v.email, v.grade, v.major, "
            + "       v.is_captain_applicant, v.status, v.created_at, "
            + "       u.name AS university_name, "
            + "       h.name AS high_school_name, h.province AS high_school_province, h.city AS high_school_city "
            + "FROM volunteer v "
            + "LEFT JOIN university u ON v.university_id = u.id "
            + "LEFT JOIN high_school h ON v.target_high_school_id = h.id "
            + "WHERE v.openid = #{openid} "
            + "ORDER BY v.created_at DESC")
    IPage<VolunteerVO> selectMyApply(Page<VolunteerVO> page, @Param("openid") String openid);

    /**
     * 锁定读用户行（论坛处罚次数并发控制用）
     */
    @Select("SELECT * FROM volunteer WHERE id = #{id} FOR UPDATE")
    Volunteer selectByIdForUpdate(@Param("id") Integer id);

    /**
     * 查询某个队伍的成员列表
     *
     * 成员判定规则：组合派生成员 + team_override IN 记录 - team_override OUT 记录
     *
     * @param promoTeamId 队伍ID
     * @return 队伍成员信息（含队长标识）
     */
    @Select("SELECT v.id AS volunteer_id, v.name, v.phone, v.major, v.grade, "
            + "       CASE WHEN pt.captain_volunteer_id = v.id THEN 'CAPTAIN' ELSE 'MEMBER' END AS role "
            + "FROM volunteer v "
            + "INNER JOIN promo_team pt ON v.university_id = pt.university_id "
            + "       AND v.target_high_school_id = pt.high_school_id "
            + "WHERE pt.id = #{promoTeamId} AND v.status = 'APPROVED' "
            + "  AND NOT EXISTS ("
            + "    SELECT 1 FROM team_override o "
            + "    WHERE o.promo_team_id = pt.id AND o.volunteer_id = v.id AND o.action = 'OUT'"
            + "  ) "
            + "UNION "
            + "SELECT v.id AS volunteer_id, v.name, v.phone, v.major, v.grade, "
            + "       CASE WHEN pt.captain_volunteer_id = v.id THEN 'CAPTAIN' ELSE 'MEMBER' END AS role "
            + "FROM volunteer v "
            + "INNER JOIN promo_team pt ON pt.id = #{promoTeamId} "
            + "INNER JOIN team_override o ON o.promo_team_id = pt.id AND o.volunteer_id = v.id AND o.action = 'IN' "
            + "WHERE v.status = 'APPROVED' "
            + "ORDER BY role = 'CAPTAIN' DESC, name")
    List<MemberVO> selectTeamMembers(@Param("promoTeamId") Integer promoTeamId);
}
