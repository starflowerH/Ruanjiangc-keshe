package com.demo.miniprogram.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.miniprogram.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 管理员 Mapper
 *
 * @author demo
 */
@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {

    /**
     * 根据用户名查找管理员（用于登录验证）
     */
    @Select("SELECT * FROM admin_user WHERE username = #{username} AND status = 1")
    AdminUser selectByUsername(@Param("username") String username);
}
