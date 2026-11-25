package com.demo.miniprogram.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 通用分页查询参数
 *
 * 使用示例：
 *   GET /api/volunteers?page=1&size=20&keyword=张三
 *
 * @author demo
 */
@Data
public class PageQueryDTO {

    /** 当前页码，从1开始，默认第1页 */
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer page = 1;

    /** 每页条数，默认20条，最大100条 */
    @Min(value = 1, message = "每页条数必须大于等于1")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer size = 20;

    /** 关键词搜索（模糊匹配名称等字段） */
    private String keyword;

    /** 省份筛选（用于高中查询） */
    private String province;

    /** 城市筛选 */
    private String city;

    /** 状态筛选（管理员按审核状态分块查看: PENDING/APPROVED/REJECTED） */
    private String status;

    /** 大学ID筛选（管理员按大学分块查看志愿者时用） */
    private Integer universityId;
}
