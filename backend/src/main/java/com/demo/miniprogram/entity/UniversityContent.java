package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 大学宣传资料表 - 老师编辑本校宣传内容
 *
 * 每所大学仅一条记录，老师登录后编辑本校数据
 *
 * @author demo
 */
@Data
@TableName("university_content")
public class UniversityContent {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 关联大学ID（唯一） */
    private Integer universityId;

    /** 校徽Logo URL */
    private String logoUrl;

    /** 宣传头图 URL */
    private String bannerUrl;

    /** 校园风光图片，JSON数组字符串 */
    private String galleryImages;

    /** 校训/标语 */
    private String slogan;

    /** 学校标签，逗号分隔（如：985,211,双一流） */
    private String tags;

    /** 学校简介（富文本） */
    private String description;

    /** 优势专业介绍，JSON数组 */
    private String majors;

    /** 历年分数线，JSON数组 */
    private String scoreLines;

    /** 招生政策（富文本） */
    private String admissionPolicy;

    /** 招生官网地址 */
    private String admissionUrl;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
