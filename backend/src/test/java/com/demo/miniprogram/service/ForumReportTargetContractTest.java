package com.demo.miniprogram.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ForumReportTargetContractTest {

    private static String src(String path) throws Exception {
        return Files.readString(Path.of(path));
    }

    @Test
    void reportDtoSupportsPostAndCommentTargets() throws Exception {
        String dto = src("src/main/java/com/demo/miniprogram/dto/ForumReportDTO.java");

        assertTrue(dto.contains("private String targetType"));
        assertTrue(dto.contains("private Integer commentId"));
    }

    @Test
    void submitReportValidatesCommentTargetAndDeduplicatesPerTarget() throws Exception {
        String service = src("src/main/java/com/demo/miniprogram/service/ForumModerationService.java");

        assertTrue(service.contains("commentMapper.selectById(commentId)"));
        assertTrue(service.contains("ForumReport::getTargetType"));
        assertTrue(service.contains("ForumReport::getCommentId"));
        assertTrue(service.contains("您已举报过该评论"));
    }

    @Test
    void reviewReportHidesCommentAndPenalizesCommentAuthor() throws Exception {
        String service = src("src/main/java/com/demo/miniprogram/service/ForumModerationService.java");

        assertTrue(service.contains("comment.setStatus(\"HIDDEN\")"));
        assertTrue(service.contains("commentMapper.updateById(comment)"));
        assertTrue(service.contains("targetUserId = comment.getUserId()"));
        assertTrue(service.contains("targetUserType = comment.getUserType()"));
    }

    @Test
    void pendingReportsQueryIncludesCommentContentAndAuthor() throws Exception {
        String mapper = src("src/main/java/com/demo/miniprogram/mapper/ForumReportMapper.java");

        assertTrue(mapper.contains("fc.content AS comment_content"));
        assertTrue(mapper.contains("COALESCE(cv.name, cs.nickname, cs.name) AS comment_author_name"));
    }

    @Test
    void appealRestoresCommentVisibilityForCommentReports() throws Exception {
        String service = src("src/main/java/com/demo/miniprogram/service/ForumModerationService.java");

        assertTrue(service.contains("comment.setStatus(\"VISIBLE\")"));
    }

    @Test
    void commentListExcludesHiddenComments() throws Exception {
        String service = src("src/main/java/com/demo/miniprogram/service/ForumCommentService.java");

        assertTrue(service.contains("eq(ForumComment::getStatus, \"VISIBLE\")"));
    }
}
