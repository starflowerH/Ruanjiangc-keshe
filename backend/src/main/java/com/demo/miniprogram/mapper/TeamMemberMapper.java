package com.demo.miniprogram.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.miniprogram.entity.TeamMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * 队伍成员 Mapper
 *
 * @author demo
 */
@Mapper
public interface TeamMemberMapper extends BaseMapper<TeamMember> {
}
