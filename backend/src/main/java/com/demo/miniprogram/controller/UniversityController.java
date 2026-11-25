package com.demo.miniprogram.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.miniprogram.common.Result;
import com.demo.miniprogram.entity.University;
import com.demo.miniprogram.entity.UniversityScoreLine;
import com.demo.miniprogram.mapper.UniversityMapper;
import com.demo.miniprogram.mapper.UniversityScoreLineMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 高校信息公开接口 - 供前端小程序展示高校列表和详情
 */
@RestController
@RequestMapping("/api/university")
@RequiredArgsConstructor
public class UniversityController {

    private final UniversityMapper universityMapper;
    private final UniversityScoreLineMapper scoreLineMapper;

    /**
     * 获取高校列表
     *
     * 排序规则（按知名度/分数线）：
     *   取每所大学最近一个年份中最低位次（min_section，数字越小=排名越靠前=越知名），
     *   升序排列；无分数线数据的学校排在最后（按创建时间倒序兜底）。
     *   效果：本科批学校自然排在专科批之前，同层次内按录取分/位次体现知名度。
     */
    @GetMapping("/list")
    public Result<List<University>> list() {
        List<University> list = universityMapper.selectList(
                new LambdaQueryWrapper<University>()
        );

        List<UniversityScoreLine> lines = scoreLineMapper.selectList(
                new LambdaQueryWrapper<UniversityScoreLine>()
        );

        // 每所大学最近出现的年份
        Map<Integer, Integer> latestYear = new HashMap<>();
        for (UniversityScoreLine line : lines) {
            if (line.getUniversityId() == null || line.getYear() == null) continue;
            latestYear.merge(line.getUniversityId(), line.getYear(), Math::max);
        }

        // 最近年份内的最低位次（排名越靠前数字越小）
        Map<Integer, Integer> bestSection = new HashMap<>();
        for (UniversityScoreLine line : lines) {
            if (line.getUniversityId() == null || line.getYear() == null) continue;
            Integer uid = line.getUniversityId();
            if (line.getYear().intValue() != latestYear.getOrDefault(uid, -1)) continue;
            if (line.getMinSection() == null) continue;
            bestSection.merge(uid, line.getMinSection(), Math::min);
        }

        Comparator<University> byReputation = (a, b) -> {
            Integer sa = bestSection.get(a.getId());
            Integer sb = bestSection.get(b.getId());
            if (sa == null && sb == null) return byCreatedDesc(a, b);
            if (sa == null) return 1; // 无分数线数据排最后
            if (sb == null) return -1;
            int cmp = Integer.compare(sa, sb);
            if (cmp != 0) return cmp;
            return byCreatedDesc(a, b);
        };
        list.sort(byReputation);

        return Result.ok(list);
    }

    /** 创建时间倒序（更新/新建的学校更靠前） */
    private int byCreatedDesc(University a, University b) {
        if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
        return b.getCreatedAt().compareTo(a.getCreatedAt());
    }

    /**
     * 获取高校详情
     */
    @GetMapping("/{id}")
    public Result<University> detail(@PathVariable Integer id) {
        University university = universityMapper.selectById(id);
        if (university == null) {
            return Result.fail(404, "高校不存在");
        }
        return Result.ok(university);
    }

    /**
     * 获取高校录取分数线（广东生源，掌上高考真实数据）
     */
    @GetMapping("/{id}/score-lines")
    public Result<List<UniversityScoreLine>> scoreLines(@PathVariable Integer id) {
        List<UniversityScoreLine> lines = scoreLineMapper.selectList(
                new LambdaQueryWrapper<UniversityScoreLine>()
                        .eq(UniversityScoreLine::getUniversityId, id)
                        .orderByDesc(UniversityScoreLine::getYear)
                        .orderByAsc(UniversityScoreLine::getLocalType)
        );
        return Result.ok(lines);
    }
}
