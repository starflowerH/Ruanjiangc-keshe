package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 高校信息表 - 存储参与招生宣传的各大学基本信息
 *
 * 业务场景：
 *   - 大学生报名时选择自己所属的大学
 *   - 高中生浏览前来宣传的各高校信息
 *   - 大学老师上传/编辑本校宣传资料
 *
 * @author demo
 */
@Data
@TableName("university")
public class University {

    /** 高校ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 高校名称，唯一（如：清华大学、北京大学） */
    private String name;

    /** 所在省份（如：广东、北京） */
    private String province;

    /** 所在城市（如：广州、深圳，不带"市"字） */
    private String city;

    /** 办学层次（本科/专科，供学生按等级筛选） */
    private String level;

    /** 学校类型（综合/理工/财经/师范/医药等，参考夸克分类） */
    private String typeName;

    /** 高校Logo图片链接，存储OSS/CDN地址 */
    private String logoUrl;

    /**
     * 高校简介/宣传资料（富文本）
     * 包含学校历史、优势专业、录取分数线、校园生活等内容
     */
    private String description;

    /** 创建时间，数据库自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，数据变更时数据库自动更新 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
