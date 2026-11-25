package com.demo.miniprogram.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.miniprogram.entity.Notification;
import com.demo.miniprogram.vo.NotificationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 通知 Mapper
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 分页查询用户的通知列表
     */
    @Select("SELECT id, user_id, user_type, type, title, content, "
            + "       source_id, source_type, source_title, is_read, created_at "
            + "FROM notification "
            + "WHERE user_id = #{userId} AND user_type = #{userType} "
            + "ORDER BY created_at DESC")
    IPage<NotificationVO> selectNotificationPage(Page<NotificationVO> page,
                                                  @Param("userId") Integer userId,
                                                  @Param("userType") String userType);

    /** 仅查询别人对本人帖子或评论产生的互动通知。 */
    @Select("SELECT id, user_id, user_type, type, title, content, "
            + "       source_id, source_type, source_title, is_read, created_at "
            + "FROM notification "
            + "WHERE user_id = #{userId} AND user_type = #{userType} "
            + "  AND source_type = 'post' AND type IN ('comment', 'reply') "
            + "ORDER BY created_at DESC")
    IPage<NotificationVO> selectInteractionNotificationPage(Page<NotificationVO> page,
                                                             @Param("userId") Integer userId,
                                                             @Param("userType") String userType);

    /**
     * 统计未读通知数量
     */
    @Select("SELECT COUNT(*) FROM notification "
            + "WHERE user_id = #{userId} AND user_type = #{userType} AND is_read = 0")
    int countUnread(@Param("userId") Integer userId, @Param("userType") String userType);

    /** 统计帖子评论和评论回复的未读数。 */
    @Select("SELECT COUNT(*) FROM notification "
            + "WHERE user_id = #{userId} AND user_type = #{userType} AND is_read = 0 "
            + "  AND source_type = 'post' AND type IN ('comment', 'reply')")
    int countUnreadInteractions(@Param("userId") Integer userId, @Param("userType") String userType);

    /**
     * 标记单条通知为已读
     */
    @Update("UPDATE notification SET is_read = 1 WHERE id = #{id}")
    int markAsRead(@Param("id") Integer id);

    /**
     * 标记用户所有通知为已读
     */
    @Update("UPDATE notification SET is_read = 1 "
            + "WHERE user_id = #{userId} AND user_type = #{userType} AND is_read = 0")
    int markAllAsRead(@Param("userId") Integer userId, @Param("userType") String userType);

    /** 将本人互动通知全部标记为已读，不影响系统通知。 */
    @Update("UPDATE notification SET is_read = 1 "
            + "WHERE user_id = #{userId} AND user_type = #{userType} AND is_read = 0 "
            + "  AND source_type = 'post' AND type IN ('comment', 'reply')")
    int markAllInteractionsAsRead(@Param("userId") Integer userId, @Param("userType") String userType);
}
