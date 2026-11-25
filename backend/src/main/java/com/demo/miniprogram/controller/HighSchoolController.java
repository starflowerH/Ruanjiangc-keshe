package com.demo.miniprogram.controller;

import com.demo.miniprogram.common.Result;
import com.demo.miniprogram.entity.HighSchool;
import com.demo.miniprogram.mapper.HighSchoolMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 高中名册接口 - 省/市/校三级联动查询
 *
 * 使用场景：
 *   - 大学生报名时选择"目标高中"
 *   - 高中生注册时选择"所在高中"
 *
 * 前端交互：级联选择器 → 先选省份 → 再选城市 → 再选具体学校
 *
 * @author demo
 */
@RestController
@RequestMapping("/api/high-school")
@RequiredArgsConstructor
public class HighSchoolController {

    private final HighSchoolMapper highSchoolMapper;

    /**
     * 获取所有省份列表
     *
     * GET /api/high-school/provinces
     */
    @GetMapping("/provinces")
    public Result<List<String>> getProvinces() {
        return Result.ok(highSchoolMapper.selectAllProvinces());
    }

    /**
     * 根据省份获取城市列表
     *
     * GET /api/high-school/cities?province=河北省
     */
    @GetMapping("/cities")
    public Result<List<String>> getCities(@RequestParam String province) {
        return Result.ok(highSchoolMapper.selectCitiesByProvince(province));
    }

    /**
     * 根据省份+城市获取高中名册列表
     *
     * GET /api/high-school/schools?province=河北省&city=石家庄市
     */
    @GetMapping("/schools")
    public Result<List<HighSchool>> getSchools(@RequestParam String province,
                                               @RequestParam String city) {
        return Result.ok(highSchoolMapper.selectByProvinceAndCity(province, city));
    }

    /**
     * 按关键词搜索高中（模糊匹配）
     * keyword 为空时返回全部高中
     *
     * GET /api/high-school/search?keyword=一中
     * GET /api/high-school/search?keyword=
     */
    @GetMapping("/search")
    public Result<List<HighSchool>> search(@RequestParam(defaultValue = "") String keyword) {
        var query = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HighSchool>()
                .orderByAsc(HighSchool::getProvince, HighSchool::getCity);
        if (keyword != null && !keyword.isBlank()) {
            query.like(HighSchool::getName, keyword);
        }
        query.last("LIMIT 100");
        return Result.ok(highSchoolMapper.selectList(query));
    }
}
