package com.demo.miniprogram.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.miniprogram.entity.PromoTeam;
import com.demo.miniprogram.vo.TeamVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 宣传队伍 Mapper
 *
 * 核心查询：按高中查看所有来宣传的大学队伍列表
 * 对应业务文档阶段3-组队期"队伍查看页面"
 *
 * @author demo
 */
@Mapper
public interface PromoTeamMapper extends BaseMapper<PromoTeam> {

    /**
     * 查询某个高中的所有宣传队伍（含队长信息、队伍人数）
     * 管理列表不返回联系方式（队长手机号等），防隐私泄露
     *
     * @param highSchoolId 高中ID
     * @return 队伍列表（含队长姓名、人数统计）
     */
    @Select("SELECT pt.id, pt.high_school_id, pt.university_id, pt.captain_volunteer_id, "
            + "       hs.name AS high_school_name, "
            + "       u.name AS university_name, "
            + "       cv.name AS captain_name, "
            + "       (SELECT COUNT(*) FROM volunteer v2 "
            + "        WHERE v2.university_id = pt.university_id "
            + "          AND v2.target_high_school_id = pt.high_school_id "
            + "          AND v2.status = 'APPROVED') AS member_count "
            + "FROM promo_team pt "
            + "LEFT JOIN high_school hs ON pt.high_school_id = hs.id "
            + "LEFT JOIN university u ON pt.university_id = u.id "
            + "LEFT JOIN volunteer cv ON pt.captain_volunteer_id = cv.id "
            + "WHERE pt.high_school_id = #{highSchoolId} "
            + "ORDER BY u.name")
    List<TeamVO> selectTeamsByHighSchool(@Param("highSchoolId") Integer highSchoolId);

    /**
     * 查询已确认但未组队的（highSchoolId, universityId）组合
     * 用于自动组队逻辑
     */
    @Select("<script>"
            + "SELECT DISTINCT v.target_high_school_id AS high_school_id, v.university_id "
            + "FROM volunteer v "
            + "WHERE v.status = 'APPROVED' "
            + "  AND NOT EXISTS ("
            + "    SELECT 1 FROM promo_team pt "
            + "    WHERE pt.high_school_id = v.target_high_school_id "
            + "      AND pt.university_id = v.university_id"
            + "  ) "
            + "  <if test='universityId != null'> AND v.university_id = #{universityId} </if>"
            + "</script>")
    List<PromoTeam> selectUngroupedCombinations(@Param("universityId") Integer universityId);

    /**
     * 通过队伍号查询队伍
     */
    @Select("SELECT * FROM promo_team WHERE team_code = #{teamCode}")
    PromoTeam selectByTeamCode(@Param("teamCode") String teamCode);
}
