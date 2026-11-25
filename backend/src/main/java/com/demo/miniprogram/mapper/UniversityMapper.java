package com.demo.miniprogram.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.miniprogram.entity.University;
import org.apache.ibatis.annotations.Mapper;

/**
 * 高校信息 Mapper
 *
 * @author demo
 */
@Mapper
public interface UniversityMapper extends BaseMapper<University> {
}
