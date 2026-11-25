package com.demo.miniprogram.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class TeacherPermissionsDTO {

    @NotNull
    private Boolean teacherVolunteerAudit;

    @NotNull
    private Boolean teacherTeamManage;

    @NotNull
    private Boolean teacherPromotionManage;

    @NotNull
    private Boolean teacherForumModeration;

    public Map<String, Boolean> toMap() {
        Map<String, Boolean> permissions = new LinkedHashMap<>();
        permissions.put("teacherVolunteerAudit", teacherVolunteerAudit);
        permissions.put("teacherTeamManage", teacherTeamManage);
        permissions.put("teacherPromotionManage", teacherPromotionManage);
        permissions.put("teacherForumModeration", teacherForumModeration);
        return permissions;
    }
}
