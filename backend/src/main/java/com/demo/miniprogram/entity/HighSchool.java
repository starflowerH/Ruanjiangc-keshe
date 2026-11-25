package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 高中名册表 - 存储全国高中的基础信息（省/市/区/校名）
 *
 * 业务场景：
 *   - 大学生报名时从名册中选择"目标高中"（我去哪个高中宣传）
 *   - 高中生注册时选择"所在高中"（我在哪个高中读书）
 *   - 系统通过"目标高中 == 所在高中"自动匹配大学生和高中生
 *
 * 数据来源：后台管理员维护，按省份导入
 *
 * @author demo
 */
@Data
@TableName("high_school")
public class HighSchool {

    /** 高中ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 高中名称（如：石家庄一中） */
    private String name;

    /** 所属省份（如：河北省） */
    private String province;

    /** 所属城市（如：石家庄市） */
    private String city;

    /** 所属区县（如：长安区），可为空 */
    private String district;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
