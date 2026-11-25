package com.demo.miniprogram.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.miniprogram.entity.UserWechat;
import org.apache.ibatis.annotations.Mapper;

/**
 * 微信用户 Mapper
 * 继承 MyBatis-Plus BaseMapper，自动具备单表CRUD能力
 *
 * @author demo
 */
@Mapper
public interface UserWechatMapper extends BaseMapper<UserWechat> {
    // BaseMapper 已提供：insert, deleteById, updateById, selectById, selectList 等
    // 如需自定义复杂SQL，在此添加方法并在XML中实现
}
