package com.demo.miniprogram.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.miniprogram.common.BusinessException;
import com.demo.miniprogram.common.ResultCode;
import com.demo.miniprogram.dto.UniversityContentDTO;
import com.demo.miniprogram.entity.UniversityContent;
import com.demo.miniprogram.mapper.UniversityContentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 大学宣传资料服务 - 老师编辑本校宣传信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UniversityContentService {

    private final UniversityContentMapper contentMapper;

    /**
     * 获取大学的宣传资料
     */
    public UniversityContent getByUniversityId(Integer universityId) {
        UniversityContent content = contentMapper.selectOne(
                new LambdaQueryWrapper<UniversityContent>()
                        .eq(UniversityContent::getUniversityId, universityId)
        );
        return content;
    }

    /**
     * 保存或更新大学宣传资料
     * 每所大学仅一条记录
     */
    @Transactional(rollbackFor = Exception.class)
    public UniversityContent saveOrUpdate(Integer teacherUniversityId, UniversityContentDTO dto) {
        // 校验：只能编辑自己本校的资料
        if (!teacherUniversityId.equals(dto.getUniversityId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能编辑本校资料");
        }

        // 查找已有记录
        UniversityContent content = contentMapper.selectOne(
                new LambdaQueryWrapper<UniversityContent>()
                        .eq(UniversityContent::getUniversityId, dto.getUniversityId())
        );

        boolean isNew = (content == null);
        if (isNew) {
            content = new UniversityContent();
            content.setUniversityId(dto.getUniversityId());
            content.setCreatedAt(LocalDateTime.now());
        }

        // 更新字段
        content.setLogoUrl(dto.getLogoUrl());
        content.setBannerUrl(dto.getBannerUrl());
        content.setSlogan(dto.getSlogan());
        content.setDescription(dto.getDescription());
        content.setAdmissionPolicy(dto.getAdmissionPolicy());
        content.setAdmissionUrl(dto.getAdmissionUrl());

        // 序列化 JSON 字段
        if (dto.getGalleryImages() != null) {
            content.setGalleryImages(JSONUtil.toJsonStr(dto.getGalleryImages()));
        }
        if (dto.getTags() != null) {
            content.setTags(String.join(",", dto.getTags()));
        }
        if (dto.getMajors() != null) {
            content.setMajors(JSONUtil.toJsonStr(dto.getMajors()));
        }
        if (dto.getScoreLines() != null) {
            content.setScoreLines(JSONUtil.toJsonStr(dto.getScoreLines()));
        }

        content.setUpdatedAt(LocalDateTime.now());

        if (isNew) {
            contentMapper.insert(content);
            log.info("大学宣传资料创建: universityId={}", dto.getUniversityId());
        } else {
            contentMapper.updateById(content);
            log.info("大学宣传资料更新: universityId={}", dto.getUniversityId());
        }

        return content;
    }
}
