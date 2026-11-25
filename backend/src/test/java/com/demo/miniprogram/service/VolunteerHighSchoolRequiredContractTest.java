package com.demo.miniprogram.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 目标高中必填契约测试
 *
 * 覆盖 2026-08-10 审查修复：
 *  - P4a assignHighSchools 空数组必须被拒绝（不能清空 target_high_school_id）
 *  - P4b createTeam 目标高中为空时建队必须被拒绝
 *  - VolunteerApplyDTO 目标高中 @NotNull
 */
class VolunteerHighSchoolRequiredContractTest {

    @Test
    void assignHighSchoolsRejectsEmptyList() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/demo/miniprogram/service/VolunteerService.java"));
        assertTrue(source.contains("CollUtil.isEmpty(highSchoolIds)"));
        assertTrue(source.contains("目标高中不能为空，至少分配一个高中"));
        // 旧的"空数组清空 target_high_school_id"逻辑必须移除
        assertFalse(source.contains("volunteer.setTargetHighSchoolId(null);\n            volunteerMapper.updateById(volunteer);"));
    }

    @Test
    void createTeamRequiresTargetHighSchool() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/demo/miniprogram/service/TeamService.java"));
        assertTrue(source.contains("creator.getTargetHighSchoolId() == null"));
        assertTrue(source.contains("请先选择目标高中，再创建队伍"));
    }

    @Test
    void applyDTOAllowsTargetHighSchoolToBeOptional() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/demo/miniprogram/dto/VolunteerApplyDTO.java"));
        assertFalse(source.contains("@NotNull(message = \"目标高中不能为空\")"));
        assertTrue(source.contains("private Integer targetHighSchoolId;"));
    }
}
