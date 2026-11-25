-- 删除副队长（幂等迁移，可重复执行）
-- 1) 现有副队长成员降为普通成员
UPDATE team_member SET role='MEMBER' WHERE role='VICE_CAPTAIN';
-- 2) 拒绝遗留的副队长申请
UPDATE team_role_application SET status='REJECTED', reviewed_at=NOW()
WHERE apply_role='VICE_CAPTAIN' AND status='PENDING';
-- 3) 删除 promo_team 副队长列（幂等）
SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='promo_team' AND COLUMN_NAME='vice_captain_volunteer_id');
SET @sql := IF(@has_col > 0, 'ALTER TABLE promo_team DROP COLUMN vice_captain_volunteer_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
