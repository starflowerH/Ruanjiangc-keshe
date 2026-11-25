package com.demo.miniprogram.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.miniprogram.common.BusinessException;
import com.demo.miniprogram.common.ResultCode;
import com.demo.miniprogram.entity.PromotionManagerGrant;
import com.demo.miniprogram.entity.Volunteer;
import com.demo.miniprogram.mapper.PromotionManagerGrantMapper;
import com.demo.miniprogram.mapper.VolunteerMapper;
import com.demo.miniprogram.security.TeacherPermissions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PromotionManagementService {
    private final PromotionManagerGrantMapper grantMapper;
    private final VolunteerMapper volunteerMapper;
    private final TeacherPermissionService teacherPermissionService;

    public PromotionManagerGrant getGrant(Integer volunteerId) {
        if (volunteerId == null) {
            return null;
        }
        Volunteer volunteer = volunteerMapper.selectById(volunteerId);
        if (volunteer == null || !"APPROVED".equals(volunteer.getStatus())
                || volunteer.getUniversityId() == null) {
            return null;
        }
        return grantMapper.selectOne(new LambdaQueryWrapper<PromotionManagerGrant>()
                .eq(PromotionManagerGrant::getVolunteerId, volunteerId)
                .eq(PromotionManagerGrant::getUniversityId, volunteer.getUniversityId())
                .eq(PromotionManagerGrant::getActive, 1)
                .last("LIMIT 1"));
    }

    @Transactional(rollbackFor = Exception.class)
    public PromotionManagerGrant setManager(Integer volunteerId, boolean active, Integer adminId) {
        return setManager(volunteerId, active, adminId, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public PromotionManagerGrant setManager(Integer volunteerId, boolean active, Integer adminId,
                                             Integer allowedUniversityId) {
        Volunteer volunteer = volunteerMapper.selectById(volunteerId);
        if (volunteer == null || !"APPROVED".equals(volunteer.getStatus())
                || volunteer.getUniversityId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只能授权已通过审核的大学生");
        }
        if (allowedUniversityId != null && !allowedUniversityId.equals(volunteer.getUniversityId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能管理本校大学生的宣传资料权限");
        }

        PromotionManagerGrant grant = grantMapper.selectOne(
                new LambdaQueryWrapper<PromotionManagerGrant>()
                        .eq(PromotionManagerGrant::getVolunteerId, volunteerId)
                        .eq(PromotionManagerGrant::getUniversityId, volunteer.getUniversityId())
                        .last("LIMIT 1"));
        if (grant == null) {
            grant = new PromotionManagerGrant();
            grant.setVolunteerId(volunteerId);
            grant.setUniversityId(volunteer.getUniversityId());
            grant.setGrantedByAdminId(adminId);
            grant.setActive(active ? 1 : 0);
            grantMapper.insert(grant);
        } else {
            grant.setGrantedByAdminId(adminId);
            grant.setActive(active ? 1 : 0);
            grantMapper.updateById(grant);
        }
        return grant;
    }

    public Integer resolveManagedUniversity(Integer requesterId, String userType, String role,
                                            String relatedType, Integer adminUniversityId) {
        if ("admin".equals(userType) && "TEACHER".equals(role)) {
            if (adminUniversityId == null
                    || !teacherPermissionService.hasAny(requesterId, TeacherPermissions.PROMOTION_MANAGE)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "未获得宣传资料管理权限");
            }
            return adminUniversityId;
        }
        if ("wechat".equals(userType) && "volunteer".equals(relatedType)) {
            PromotionManagerGrant grant = getGrant(requesterId);
            if (grant != null) {
                return grant.getUniversityId();
            }
        }
        throw new BusinessException(ResultCode.FORBIDDEN, "未获得宣传资料管理权限");
    }
}
