package com.demo.miniprogram.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.miniprogram.entity.HighSchoolStudent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 高中生 Mapper
 *
 * @author demo
 */
@Mapper
public interface HighSchoolStudentMapper extends BaseMapper<HighSchoolStudent> {

    /**
     * 锁定读用户行（论坛处罚次数并发控制用）
     */
    @Select("SELECT * FROM high_school_student WHERE id = #{id} FOR UPDATE")
    HighSchoolStudent selectByIdForUpdate(@Param("id") Integer id);
}
