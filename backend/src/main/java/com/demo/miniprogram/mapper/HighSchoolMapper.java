package com.demo.miniprogram.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.miniprogram.entity.HighSchool;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 高中名册 Mapper
 *
 * @author demo
 */
@Mapper
public interface HighSchoolMapper extends BaseMapper<HighSchool> {

    /**
     * 获取所有省份列表（去重，用于前端省市区级联选择器）
     */
    @Select("SELECT DISTINCT province FROM high_school ORDER BY province")
    List<String> selectAllProvinces();

    /**
     * 根据省份获取城市列表（去重）
     */
    @Select("SELECT DISTINCT city FROM high_school WHERE province = #{province} ORDER BY city")
    List<String> selectCitiesByProvince(@Param("province") String province);

    /**
     * 根据省份+城市获取高中列表
     */
    @Select("SELECT * FROM high_school WHERE province = #{province} AND city = #{city} ORDER BY name")
    List<HighSchool> selectByProvinceAndCity(@Param("province") String province, @Param("city") String city);
}
