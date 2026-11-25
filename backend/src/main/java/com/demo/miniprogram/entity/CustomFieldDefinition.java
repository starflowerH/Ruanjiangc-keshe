package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 自定义字段定义表 - 每个大学可自定义报名表单的额外字段
 *
 * 业务场景（对应业务文档阶段1-报名期）：
 *   大学生选择所属大学后 → 展示该大学的定制表单
 *   例如：清华大学可能需要「是否有清华自行车」「高中竞赛获奖情况」
 *        北京大学可能需要「是否参加过北大夏令营」
 *   这些字段由各大学老师在后台自行配置
 *
 * 字段类型说明：
 *   TEXT   - 文本输入框（自由填写）
 *   SELECT - 下拉选择框（选项存 field_options 的 JSON 数组）
 *   NUMBER - 数字输入框
 *
 * @author demo
 */
@Data
@TableName("custom_field_definition")
public class CustomFieldDefinition {

    /** 字段定义ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 所属大学ID - 这个自定义字段属于哪个大学 */
    private Integer universityId;

    /** 字段标签/提示文本（如："是否有单车"、"高中获奖情况"） */
    private String fieldLabel;

    /**
     * 字段输入类型
     * TEXT   - 文本输入
     * SELECT - 下拉选择
     * NUMBER - 数字输入
     */
    private String fieldType;

    /**
     * 下拉框可选项（仅 field_type = SELECT 时使用）
     * JSON数组格式，如：["是", "否"] 或 ["北京", "上海", "广州"]
     */
    private String fieldOptions;

    /** 是否必填：0=选填，1=必填 */
    private Integer isRequired;
}
