package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 志愿者-高中多对多关联表
 *
 * 支持一名志愿者赴多所高中宣传的业务场景。
 * 与 volunteer 表的 target_high_school_id 字段并存，
 * target_high_school_id 保留作为报名时的首选高中（可为空）。
 *
 * @author demo
 */
@Data
@TableName("volunteer_high_school")
public class VolunteerHighSchool {

    /** 关联记录ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 志愿者报名记录ID，关联 volunteer 表 */
    private Integer volunteerId;

    /** 高中ID，关联 high_school 表 */
    private Integer highSchoolId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
