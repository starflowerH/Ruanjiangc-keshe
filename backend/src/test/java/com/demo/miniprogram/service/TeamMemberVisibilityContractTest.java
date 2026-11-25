package com.demo.miniprogram.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 队伍成员判定与手机号可见性契约测试
 *
 * 覆盖 2026-08-10 审查修复：
 *  - P1 自动队成员判定统一（getMyTeam 走 findMyTeam 组合派生）
 *  - P2 成员列表同队鉴权（getTeamMembers 增加操作者校验）
 *  - P3 本队成员/按高中查队统一走 getEffectiveMembers
 *  - P5 管理端队伍列表不返回成员手机号
 *  - P6 空队伍清理二次确认 + 事务模板
 */
class TeamMemberVisibilityContractTest {

    private static final String TEAM_SERVICE = "src/main/java/com/demo/miniprogram/service/TeamService.java";
    private static final String VOLUNTEER_SERVICE = "src/main/java/com/demo/miniprogram/service/VolunteerService.java";
    private static final String TEAM_CONTROLLER = "src/main/java/com/demo/miniprogram/controller/TeamController.java";

    @Test
    void getMyTeamUsesFindMyTeamForAutoTeamMembers() throws Exception {
        String source = Files.readString(Path.of(TEAM_SERVICE));
        // P1: getMyTeam must use findMyTeam and explicit membership records.
        assertTrue(source.contains("PromoTeam team = findMyTeam(volunteerId);"));
        assertFalse(source.contains("""
                public TeamVO getMyTeam(Integer volunteerId) {
                    TeamMember member = teamMemberMapper.selectOne("""));
        assertTrue(source.contains(".eq(TeamOverride::getAction, \"IN\")"));
        assertFalse(source.contains("volunteerMapper.selectTeamMembers(team.getId())"));
    }

    @Test
    void getTeamMembersRequiresSameTeam() throws Exception {
        String controller = Files.readString(Path.of(TEAM_CONTROLLER));
        // P2: 成员列表接口必须携带操作者身份
        assertTrue(controller.contains("relatedType"));
        assertTrue(controller.contains("resolveVolunteerId(volunteerId, openid, relatedType)"));

        String source = Files.readString(Path.of(TEAM_SERVICE));
        // 服务层同队校验
        assertTrue(source.contains("isTeamMember(team, operatorId)"));
        assertTrue(source.contains("只能查看本队伍成员信息"));
    }

    @Test
    void myTeamMembersAndTeamsByHighSchoolUseEffectiveMembers() throws Exception {
        String source = Files.readString(Path.of(VOLUNTEER_SERVICE));
        // P3: getMyTeamMembers 统一判定
        assertTrue(source.contains("teamService.getEffectiveMembers(myTeam, false)"));
        // P3b: getTeamsByHighSchool 成员填充统一判定
        assertTrue(source.contains("teamService.getEffectiveMembers(promoTeam, false)"));
        // 不再按 大学+高中 全量查志愿者当成员
        assertFalse(source.contains("""
                .eq(Volunteer::getTargetHighSchoolId, myTeam.getHighSchoolId())
                        .eq(Volunteer::getUniversityId, myTeam.getUniversityId())"""));
    }

    @Test
    void getAllTeamsDoesNotLeakPhones() throws Exception {
        String source = Files.readString(Path.of(TEAM_SERVICE));
        // P5: 管理端全部队伍列表不返回成员手机号
        assertTrue(source.contains("return teams.stream().map(t -> buildTeamVO(t, false)).collect(Collectors.toList());"));
        // P6: getAllTeams 不再主动触发清理（查询路径不删数据）
        assertFalse(source.contains("""
                public List<TeamVO> getAllTeams() {
                    // 查询前自动清理空队伍
                    cleanupEmptyTeams();"""));
    }

    @Test
    void cleanupEmptyTeamsHasDoubleCheckAndTransactionTemplate() throws Exception {
        String source = Files.readString(Path.of(TEAM_SERVICE));
        // P6: 二次确认（team_member / IN override 任一存在即跳过）
        assertTrue(source.contains("memberCnt > 0 || inCnt > 0"));
        // 每队独立事务（事务模板，避免同类调用 @Transactional 不生效）
        assertTrue(source.contains("transactionTemplate.executeWithoutResult"));
    }
}
