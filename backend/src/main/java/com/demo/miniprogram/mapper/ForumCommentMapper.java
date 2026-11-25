package com.demo.miniprogram.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.miniprogram.entity.ForumComment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 论坛评论 Mapper
 */
@Mapper
public interface ForumCommentMapper extends BaseMapper<ForumComment> {
}
