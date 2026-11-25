package com.demo.miniprogram.mapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionAnswerNicknameContractTest {

    @Test
    void answererUsesVolunteerRealNameForConsistentDisplay() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/demo/miniprogram/mapper/QuestionAnswerMapper.java"));

        // 志愿者统一显示报名真名（与登录/资料页一致），不使用微信昵称避免显示不一致
        assertTrue(source.contains("v.name AS answerer_name"));
        // 提问者：学生优先身份昵称/真名，志愿者回退真名（不再显示微信昵称）
        assertTrue(source.contains("COALESCE(hss.nickname, hss.name, av.name) AS asker_name"));
        assertTrue(source.contains("LEFT JOIN high_school_student hss ON qa.asker_student_id = hss.id"));
    }
}
