package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 志愿者自定义字段值表 - 存储志愿者在报名时填写的自定义字段内容
 *
 * 数据关系：
 *   volunteer_id  → 关联到具体某次报名
 *   field_id      → 关联到 custom_field_definition 的字段定义
 *   field_value   → 志愿者填写的具体内容
 *
 * 唯一约束 uk_vol_field 保证：同一志愿者对同一字段只会有一条记录
 *
 * @author demo
 */
@Data
@TableName("volunteer_custom_value")
public class VolunteerCustomValue {

    /** 记录ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 关联的报名记录ID（volunteer 表） */
    private Integer volunteerId;

    /** 关联的字段定义ID（custom_field_definition 表） */
    private Integer fieldId;

    /** 志愿者填写的具体值 */
    private String fieldValue;
}
