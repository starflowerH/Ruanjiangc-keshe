package com.demo.miniprogram.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViceCaptainRemovalContractTest {

    @Test
    void teamServiceNoLongerReferencesViceCaptain() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/demo/miniprogram/service/TeamService.java"));

        assertFalse(source.contains("VICE_CAPTAIN"));
        assertFalse(source.contains("viceCaptainVolunteerId"));
        assertFalse(source.contains("viceCaptain"));
    }

    @Test
    void promoTeamEntityDropsViceCaptainColumn() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/demo/miniprogram/entity/PromoTeam.java"));

        assertFalse(source.contains("viceCaptainVolunteerId"));
    }

    @Test
    void migrationDropsViceCaptainColumnAndNormalizesLegacyData() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/vice_captain_removal.sql"));

        assertTrue(sql.contains("UPDATE team_member SET role='MEMBER' WHERE role='VICE_CAPTAIN'"));
        assertTrue(sql.contains("ALTER TABLE promo_team DROP COLUMN vice_captain_volunteer_id"));
    }
}
