package com.demo.miniprogram.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 大学录取分数线（广东生源，掌上高考真实数据）
 */
@Data
@TableName("university_score_line")
public class UniversityScoreLine {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 关联 university.id */
    private Integer universityId;

    /** 年份 */
    private Integer year;

    /** 科类：物理类/历史类 */
    private String localType;

    /** 批次：本科批/专科批/本科提前批 */
    private String localBatch;

    /** 最低分 */
    private Integer minScore;

    /** 最低位次 */
    private Integer minSection;

    /** 投档线 */
    private Integer filing;

    /** 平均分（可能为空） */
    private Integer avgScore;

    private LocalDateTime createdAt;
}
