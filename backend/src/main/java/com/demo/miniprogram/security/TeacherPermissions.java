package com.demo.miniprogram.security;

import java.util.Set;

/** Stable permission keys for TEACHER accounts. */
public final class TeacherPermissions {

    public static final String VOLUNTEER_AUDIT = "teacherVolunteerAudit";
    public static final String TEAM_MANAGE = "teacherTeamManage";
    public static final String PROMOTION_MANAGE = "teacherPromotionManage";
    public static final String FORUM_MODERATION = "teacherForumModeration";

    public static final Set<String> ALL = Set.of(
            VOLUNTEER_AUDIT,
            TEAM_MANAGE,
            PROMOTION_MANAGE,
            FORUM_MODERATION
    );

    private TeacherPermissions() {
    }
}
