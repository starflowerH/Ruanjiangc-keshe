-- 举报拆分：支持举报帖子或帖子内单条评论（幂等迁移，可重复执行）

SET @has_target_type := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'forum_report' AND COLUMN_NAME = 'target_type'
);
SET @sql := IF(@has_target_type = 0,
  'ALTER TABLE forum_report ADD COLUMN target_type VARCHAR(20) NOT NULL DEFAULT ''post'' COMMENT ''举报对象: post/comment'' AFTER post_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_comment_id := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'forum_report' AND COLUMN_NAME = 'comment_id'
);
SET @sql := IF(@has_comment_id = 0,
  'ALTER TABLE forum_report ADD COLUMN comment_id INT(11) NOT NULL DEFAULT 0 COMMENT ''评论ID（target_type=comment 时有效）'' AFTER target_type',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_old_key := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'forum_report' AND INDEX_NAME = 'uk_reporter_post'
);
SET @sql := IF(@has_old_key > 0,
  'ALTER TABLE forum_report DROP INDEX uk_reporter_post',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_new_key := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'forum_report' AND INDEX_NAME = 'uk_reporter_target'
);
SET @sql := IF(@has_new_key = 0,
  'ALTER TABLE forum_report ADD UNIQUE KEY uk_reporter_target (post_id, target_type, comment_id, reporter_id, reporter_type)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_comment_idx := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'forum_report' AND INDEX_NAME = 'idx_report_comment'
);
SET @sql := IF(@has_comment_idx = 0,
  'ALTER TABLE forum_report ADD KEY idx_report_comment (comment_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_status := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'forum_comment' AND COLUMN_NAME = 'status'
);
SET @sql := IF(@has_status = 0,
  'ALTER TABLE forum_comment ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT ''VISIBLE'' COMMENT ''评论状态: VISIBLE/HIDDEN'' AFTER parent_id, ADD KEY idx_status (status)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
