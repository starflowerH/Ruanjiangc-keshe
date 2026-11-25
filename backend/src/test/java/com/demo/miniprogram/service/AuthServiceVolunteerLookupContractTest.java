package com.demo.miniprogram.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceVolunteerLookupContractTest {

    @Test
    void wxLoginSelectsTheNewestApprovedVolunteerInsteadOfAssumingOneRecord() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/demo/miniprogram/service/AuthService.java"));

        assertTrue(source.contains("volunteerMapper.selectList("));
        assertTrue(source.contains("Volunteer::getStatus, \"APPROVED\""));
        assertTrue(source.contains("orderByDesc(Volunteer::getUpdatedAt)"));
        assertFalse(source.contains("volunteerMapper.selectOne("));
    }
}
