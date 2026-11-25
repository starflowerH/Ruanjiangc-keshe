package com.demo.miniprogram.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthVolunteerLookupContractTest {

    @Test
    void qaRequestsHandleMultipleVolunteerRowsForOneOpenid() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/demo/miniprogram/config/JwtAuthFilter.java"));

        assertTrue(source.contains("volunteerMapper.selectList("));
        // main 有意识别全部志愿者身份（含 CANCELLED/PENDING），仅要求按 openid+时间排序
        assertTrue(source.contains("eq(Volunteer::getOpenid, openid)"));
        assertTrue(source.contains("orderByDesc(Volunteer::getUpdatedAt)"));
        assertFalse(source.contains("volunteerMapper.selectOne("));
    }
}
