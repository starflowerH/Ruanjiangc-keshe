package com.demo.miniprogram.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.miniprogram.entity.TeamRoleApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 队长/副队长申请 Mapper
 *
 * @author demo
 */
@Mapper
public interface TeamRoleApplicationMapper extends BaseMapper<TeamRoleApplication> {
}
