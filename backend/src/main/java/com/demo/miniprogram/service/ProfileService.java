package com.demo.miniprogram.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.miniprogram.common.BusinessException;
import com.demo.miniprogram.common.ResultCode;
import com.demo.miniprogram.entity.*;
import com.demo.miniprogram.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 个人资料服务 - 支持高中生/大学生/老师修改自己的信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserWechatMapper userWechatMapper;
    private final HighSchoolStudentMapper studentMapper;
    private final VolunteerMapper volunteerMapper;
    private final AdminUserMapper adminUserMapper;
    private final HighSchoolMapper highSchoolMapper;
    private final UniversityMapper universityMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final PromoTeamMapper promoTeamMapper;
    private final TeamOverrideMapper teamOverrideMapper;

    /**
     * 获取个人资料（按角色返回不同字段）
     */
    public Map<String, Object> getProfile(String openid, String userType, Integer relatedId,
                                          String relatedType) {
        Map<String, Object> profile = new LinkedHashMap<>();

        // 微信基础信息
        if (openid != null && !openid.startsWith("admin_")) {
            UserWechat user = userWechatMapper.selectById(openid);
            if (user != null) {
                // 共享昵称仅供尚未建立身份资料时预填；已有身份会在下方覆盖。
                profile.put("nickname", user.getNickname());
                profile.put("avatarUrl", user.getAvatarUrl());
            }

            // 报名状态与当前登录身份解耦：学生或尚未切换到志愿者身份时，
            // 个人中心仍需要知道是否已有 PENDING/REJECTED/APPROVED 报名。
            Volunteer latestVolunteer = volunteerMapper.selectOne(
                    new LambdaQueryWrapper<Volunteer>()
                            .eq(Volunteer::getOpenid, openid)
                            .orderByDesc(Volunteer::getCreatedAt)
                            .last("LIMIT 1")
            );
            profile.put("volunteerStatus", latestVolunteer == null || latestVolunteer.getStatus() == null
                    ? "NONE" : latestVolunteer.getStatus());
        }

        if ("wechat".equals(userType) && relatedId != null && "volunteer".equals(relatedType)) {
            Volunteer volunteer = volunteerMapper.selectOne(
                    new LambdaQueryWrapper<Volunteer>()
                            .eq(Volunteer::getId, relatedId)
                            .eq(Volunteer::getOpenid, openid)
                            .last("LIMIT 1")
            );
            if (volunteer != null) {
                profile.put("role", "volunteer");
                profile.put("nickname", volunteer.getName());
                profile.put("name", volunteer.getName());
                profile.put("phone", volunteer.getPhone());
                profile.put("wechatId", volunteer.getWechatId());
                profile.put("major", volunteer.getMajor());
                profile.put("grade", volunteer.getGrade());
                profile.put("universityId", volunteer.getUniversityId());
                profile.put("targetHighSchoolId", volunteer.getTargetHighSchoolId());
                University university = universityMapper.selectById(volunteer.getUniversityId());
                HighSchool highSchool = highSchoolMapper.selectById(volunteer.getTargetHighSchoolId());
                profile.put("universityName", university != null ? university.getName() : null);
                profile.put("targetHighSchoolName", highSchool != null ? highSchool.getName() : null);
                profile.put("status", volunteer.getStatus());
                return profile;
            }
        }
        if ("wechat".equals(userType) && "student".equals(relatedType)) {
            HighSchoolStudent student = studentMapper.selectOne(
                    new LambdaQueryWrapper<HighSchoolStudent>().eq(HighSchoolStudent::getOpenid, openid)
            );
            if (student != null) {
                profile.put("role", "student");
                profile.put("nickname", identityNickname(student.getNickname(), student.getName()));
                profile.put("name", student.getName());
                profile.put("avatarUrl", student.getAvatarUrl());
                profile.put("phone", student.getPhone());
                profile.put("highSchoolId", student.getHighSchoolId());
                HighSchool highSchool = highSchoolMapper.selectById(student.getHighSchoolId());
                profile.put("highSchoolName", highSchool != null ? highSchool.getName() : null);
                return profile;
            }
        }

        if ("admin".equals(userType) && relatedId != null) {
            AdminUser admin = adminUserMapper.selectById(relatedId);
            if (admin != null) {
                profile.put("role", admin.getRole());
                profile.put("name", admin.getName());
                profile.put("phone", admin.getPhone());
                profile.put("universityId", admin.getUniversityId());
                profile.put("nickname", admin.getName());
                return profile;
            }
        }

        return profile;
    }

    /**
     * 更新个人资料
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateProfile(String openid, String userType, Integer relatedId,
                                              String relatedType,
                                              Map<String, Object> updates) {
        Map<String, Object> result = new LinkedHashMap<>();
        String role = null;

        // 更新志愿者信息
        if ("volunteer".equals(relatedType) && relatedId != null) {
            Volunteer volunteer = volunteerMapper.selectOne(
                    new LambdaQueryWrapper<Volunteer>()
                            .eq(Volunteer::getId, relatedId)
                            .eq(Volunteer::getOpenid, openid)
                            .last("LIMIT 1")
            );
            if (volunteer != null) {
                role = "volunteer";
                boolean changed = false;
                if (updates.containsKey("nickname")) {
                    volunteer.setName((String) updates.get("nickname"));
                    changed = true;
                } else if (updates.containsKey("name")) {
                    volunteer.setName((String) updates.get("name"));
                    changed = true;
                }
                if (updates.containsKey("phone")) {
                    volunteer.setPhone((String) updates.get("phone"));
                    changed = true;
                }
                if (updates.containsKey("wechatId")) {
                    volunteer.setWechatId((String) updates.get("wechatId"));
                    changed = true;
                }
                if (updates.containsKey("major")) {
                    volunteer.setMajor((String) updates.get("major"));
                    changed = true;
                }
                if (updates.containsKey("grade")) {
                    volunteer.setGrade((String) updates.get("grade"));
                    changed = true;
                }
                if (updates.containsKey("universityId")) {
                    Integer universityId = parseInteger(updates.get("universityId"));
                    if (universityId == null || universityMapper.selectById(universityId) == null) {
                        throw new BusinessException(ResultCode.PARAM_ERROR, "所选大学不存在");
                    }
                    if (!universityId.equals(volunteer.getUniversityId())) {
                        long teamMemberCount = teamMemberMapper.selectCount(
                                new LambdaQueryWrapper<TeamMember>().eq(TeamMember::getVolunteerId, relatedId));
                        long captainCount = promoTeamMapper.selectCount(
                                new LambdaQueryWrapper<PromoTeam>().eq(PromoTeam::getCaptainVolunteerId, relatedId));
                        long assignedCount = teamOverrideMapper.selectCount(
                                new LambdaQueryWrapper<TeamOverride>()
                                        .eq(TeamOverride::getVolunteerId, relatedId)
                                        .eq(TeamOverride::getAction, "IN"));
                        if (teamMemberCount > 0 || captainCount > 0 || assignedCount > 0) {
                            throw new BusinessException("修改所属大学前请先退出当前队伍");
                        }
                        volunteer.setUniversityId(universityId);
                        changed = true;
                    }
                }
                if (updates.containsKey("avatarUrl")) {
                    // volunteer 表无 avatar_url 字段，头像统一写入 user_wechat（共享头像）
                    UserWechat wechatUser = userWechatMapper.selectById(volunteer.getOpenid());
                    if (wechatUser != null) {
                        wechatUser.setAvatarUrl((String) updates.get("avatarUrl"));
                        wechatUser.setUpdatedAt(LocalDateTime.now());
                        userWechatMapper.updateById(wechatUser);
                    }
                    changed = true;
                }
                if (changed) {
                    volunteerMapper.updateById(volunteer);
                    log.info("志愿者信息更新: volunteerId={}", relatedId);
                }
            }
        }

        // 更新高中生信息
        if (role == null && "student".equals(relatedType)
                && openid != null && !openid.startsWith("admin_")) {
            HighSchoolStudent student = studentMapper.selectOne(
                    new LambdaQueryWrapper<HighSchoolStudent>().eq(HighSchoolStudent::getOpenid, openid)
            );
            if (student != null) {
                role = "student";
                boolean changed = false;
                if (updates.containsKey("nickname")) {
                    student.setNickname((String) updates.get("nickname"));
                    changed = true;
                }
                if (updates.containsKey("name")) {
                    student.setName((String) updates.get("name"));
                    changed = true;
                }
                if (updates.containsKey("avatarUrl")) {
                    student.setAvatarUrl((String) updates.get("avatarUrl"));
                    changed = true;
                }
                if (updates.containsKey("phone")) {
                    student.setPhone((String) updates.get("phone"));
                    changed = true;
                }
                if (changed) {
                    studentMapper.updateById(student);
                    log.info("高中生信息更新: openid={}", openid);
                }
            }
        }

        // 更新老师/管理员信息
        if (role == null && "admin".equals(userType) && relatedId != null) {
            AdminUser admin = adminUserMapper.selectById(relatedId);
            if (admin != null) {
                role = admin.getRole();
                boolean changed = false;
                if (updates.containsKey("name")) {
                    admin.setName((String) updates.get("name"));
                    changed = true;
                }
                if (updates.containsKey("phone")) {
                    admin.setPhone((String) updates.get("phone"));
                    changed = true;
                }
                if (changed) {
                    adminUserMapper.updateById(admin);
                    log.info("管理员信息更新: adminId={}", relatedId);
                }
            }
        }

        // 微信主体只保留共享头像；身份昵称已在各自身份表中更新。
        if (openid != null && !openid.startsWith("admin_")) {
            UserWechat user = userWechatMapper.selectById(openid);
            if (user != null) {
                boolean changed = false;
                if (updates.containsKey("avatarUrl")) {
                    user.setAvatarUrl((String) updates.get("avatarUrl"));
                    changed = true;
                }
                if (changed) {
                    user.setUpdatedAt(LocalDateTime.now());
                    userWechatMapper.updateById(user);
                }
            }
        }

        result.put("success", true);
        result.put("role", role);
        return result;
    }

    private String identityNickname(String nickname, String name) {
        return nickname == null || nickname.isBlank() ? name : nickname;
    }

    private Integer parseInteger(Object value) {
        if (value instanceof Number number) return number.intValue();
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.valueOf(text);
            } catch (NumberFormatException ignored) {
                // Fall through to the common invalid-parameter response.
            }
        }
        return null;
    }
}
