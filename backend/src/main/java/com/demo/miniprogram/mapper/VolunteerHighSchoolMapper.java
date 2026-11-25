package com.demo.miniprogram.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.miniprogram.entity.VolunteerHighSchool;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 志愿者-高中关联 Mapper
 *
 * @author demo
 */
@Mapper
public interface VolunteerHighSchoolMapper extends BaseMapper<VolunteerHighSchool> {

    /**
     * 查询某志愿者绑定的所有高中ID
     */
    @Select("SELECT high_school_id FROM volunteer_high_school WHERE volunteer_id = #{volunteerId}")
    List<Integer> selectHighSchoolIdsByVolunteerId(@Param("volunteerId") Integer volunteerId);

    /**
     * 查询某高中下所有志愿者ID
     */
    @Select("SELECT volunteer_id FROM volunteer_high_school WHERE high_school_id = #{highSchoolId}")
    List<Integer> selectVolunteerIdsByHighSchoolId(@Param("highSchoolId") Integer highSchoolId);

    /**
     * 删除志愿者的所有高中绑定
     */
    @Delete("DELETE FROM volunteer_high_school WHERE volunteer_id = #{volunteerId}")
    void deleteByVolunteerId(@Param("volunteerId") Integer volunteerId);

    /**
     * 删除指定的高中绑定
     */
    @Delete("DELETE FROM volunteer_high_school WHERE volunteer_id = #{volunteerId} AND high_school_id = #{highSchoolId}")
    int deleteByVolunteerAndHighSchool(@Param("volunteerId") Integer volunteerId,
                                        @Param("highSchoolId") Integer highSchoolId);
}
