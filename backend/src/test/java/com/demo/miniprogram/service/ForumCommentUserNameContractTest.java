package com.demo.miniprogram.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ForumCommentUserNameContractTest {

    @Test
    void commentsResolveIdentityDisplayNameConsistentWithProfile() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/demo/miniprogram/service/ForumCommentService.java"));

        // 志愿者统一显示报名真名（与登录/资料页一致）
        assertTrue(source.contains("\"volunteer\".equals(userType) || \"wechat\".equals(userType)"));
        assertTrue(source.contains("Volunteer volunteer = volunteerMapper.selectById(userId)"));
        assertTrue(source.contains("return volunteer.getName();"));
        // 学生显示身份昵称/真名（与登录/资料页一致）
        assertTrue(source.contains("student.getNickname()"));
        assertTrue(source.contains("student.getName()"));
    }
}
