package com.demo.miniprogram.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.miniprogram.entity.CustomFieldDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 自定义字段定义 Mapper
 *
 * @author demo
 */
@Mapper
public interface CustomFieldDefinitionMapper extends BaseMapper<CustomFieldDefinition> {

    /**
     * 查询某个大学的所有自定义字段定义
     * 大学生报名时，选择大学后前端调用此接口，动态渲染表单
     *
     * @param universityId 大学ID
     * @return 该大学的自定义字段定义列表
     */
    @Select("SELECT * FROM custom_field_definition WHERE university_id = #{universityId} ORDER BY id")
    List<CustomFieldDefinition> selectByUniversityId(@Param("universityId") Integer universityId);
}
