package com.demo.miniprogram.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression contracts for the volunteer-to-team lifecycle. */
class VolunteerTeamSnapshotContractTest {

    private static final String SERVICE =
            "src/main/java/com/demo/miniprogram/service/VolunteerService.java";

    @Test
    void automaticBuildPersistsAnExplicitMemberSnapshot() throws Exception {
        String source = Files.readString(Path.of(SERVICE));

        assertTrue(source.contains("members = members.stream()"));
        assertTrue(source.contains("filter(member -> !isAssignedToTeam(member.getId()))"));
        assertTrue(source.contains("teamMemberMapper.insert(teamMember);"));
        assertTrue(source.contains("teamMember.setRole(member.getId().equals(captainId)"));
    }

    @Test
    void applyingAndAuditingDoNotCreateTeams() throws Exception {
        String source = Files.readString(Path.of(SERVICE));
        String apply = methodBody(source, "public Volunteer apply(", "/* ====================");
        String audit = methodBody(source, "public void audit(", "/* ====================");

        assertFalse(apply.contains("promoTeamMapper.insert"));
        assertFalse(apply.contains("teamMemberMapper.insert"));
        assertFalse(audit.contains("promoTeamMapper.insert"));
        assertFalse(audit.contains("teamMemberMapper.insert"));
        assertFalse(audit.contains("buildTeams("));
    }

    private static String methodBody(String source, String signature, String nextSection) {
        int start = source.indexOf(signature);
        int end = source.indexOf(nextSection, start);
        if (start < 0 || end < 0) {
            throw new AssertionError("Could not locate method section: " + signature);
        }
        return source.substring(start, end);
    }
}
