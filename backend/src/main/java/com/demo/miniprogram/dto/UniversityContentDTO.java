package com.demo.miniprogram.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 大学宣传资料提交 DTO
 */
@Data
public class UniversityContentDTO {

    /** 大学ID */
    private Integer universityId;

    /** 校徽 URL */
    private String logoUrl;

    /** 头图 URL */
    private String bannerUrl;

    /** 校园风光图片列表 */
    private List<String> galleryImages;

    /** 校训 */
    private String slogan;

    /** 标签列表 */
    private List<String> tags;

    /** 简介 */
    private String description;

    /** 专业列表 [{name, description}] */
    private List<Map<String, String>> majors;

    /** 分数线列表 [{year, scienceMin, artMin, minRank}] */
    private List<Map<String, Object>> scoreLines;

    /** 招生政策 */
    private String admissionPolicy;

    /** 招生官网 */
    private String admissionUrl;
}
