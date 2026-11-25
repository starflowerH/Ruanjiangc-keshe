package com.demo.miniprogram.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ForumFeaturedRestrictionContractTest {

    @Test
    void setFeaturedRejectsStudentAuthoredPosts() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/demo/miniprogram/service/ForumModerationService.java"));

        assertTrue(source.contains("featured && \"student\".equals(post.getAuthorType())"));
        assertTrue(source.contains("高中生发布的帖子不能设为精华"));
    }

    @Test
    void migrationDemotesExistingStudentFeaturedPosts() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/featured_student_restriction.sql"));

        assertTrue(sql.contains("WHERE author_type = 'student' AND is_featured = 1"));
    }
}
