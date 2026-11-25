package com.demo.miniprogram.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.miniprogram.entity.TeamOverride;
import org.apache.ibatis.annotations.Mapper;

/**
 * 队伍成员手动调整覆盖 Mapper
 *
 * @author demo
 */
@Mapper
public interface TeamOverrideMapper extends BaseMapper<TeamOverride> {
}
