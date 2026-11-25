-- Team membership snapshot migration
--
-- Before this migration, automatic and teacher-created teams derived members
-- from the current volunteer list. Backfill the members that existed when
-- the migration runs so later approvals do not silently join those teams.
-- Teams without a captain are left untouched because they may be empty
-- teacher-created teams waiting for an explicit member assignment.

INSERT INTO team_member (team_id, volunteer_id, role, joined_at)
SELECT pt.id,
       v.id,
       CASE WHEN pt.captain_volunteer_id = v.id THEN 'CAPTAIN' ELSE 'MEMBER' END,
       COALESCE(pt.created_at, CURRENT_TIMESTAMP)
FROM promo_team pt
JOIN volunteer v
  ON v.university_id = pt.university_id
 AND v.target_high_school_id = pt.high_school_id
 AND v.status = 'APPROVED'
WHERE pt.captain_volunteer_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM team_member existing_team
      WHERE existing_team.team_id = pt.id
  )
  AND NOT EXISTS (
      SELECT 1 FROM team_member assigned_member
      WHERE assigned_member.volunteer_id = v.id
  )
  AND NOT EXISTS (
      SELECT 1 FROM team_override current_assignment
      WHERE current_assignment.volunteer_id = v.id
        AND current_assignment.action = 'IN'
  )
  AND NOT EXISTS (
      SELECT 1 FROM team_override removed_member
      WHERE removed_member.promo_team_id = pt.id
        AND removed_member.volunteer_id = v.id
        AND removed_member.action = 'OUT'
  );
