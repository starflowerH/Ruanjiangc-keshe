-- 论坛筛选、校级审核、举报处罚、申诉与精华帖升级脚本（幂等版本）
-- 可重复执行：所有 ALTER / ADD KEY 均先检测是否存在，已存在则跳过。
-- 全量初始化请使用 schema.sql（DROP + CREATE），本脚本用于已有生产库升级。

-- 1) Youtube 表新增论坛治理字段（幂等）
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'Youtube' AND COLUMN_NAME = 'university_id');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `Youtube` ADD COLUMN `university_id` int(11) DEFAULT NULL COMMENT ''帖子关联大学'' AFTER `target_volunteer_id`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'Youtube' AND COLUMN_NAME = 'author_type');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `Youtube` ADD COLUMN `author_type` varchar(20) DEFAULT NULL COMMENT ''发帖身份: student/volunteer'' AFTER `university_id`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'Youtube' AND COLUMN_NAME = 'is_featured');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `Youtube` ADD COLUMN `is_featured` tinyint(4) NOT NULL DEFAULT 0 COMMENT ''是否精华'' AFTER `is_public`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'Youtube' AND COLUMN_NAME = 'reviewed_by_volunteer_id');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `Youtube` ADD COLUMN `reviewed_by_volunteer_id` int(11) DEFAULT NULL COMMENT ''大学生审核员ID'' AFTER `is_featured`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'Youtube' AND COLUMN_NAME = 'reviewed_by_admin_id');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `Youtube` ADD COLUMN `reviewed_by_admin_id` int(11) DEFAULT NULL COMMENT ''管理员审核人ID'' AFTER `reviewed_by_volunteer_id`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'Youtube' AND COLUMN_NAME = 'reviewed_at');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `Youtube` ADD COLUMN `reviewed_at` datetime DEFAULT NULL COMMENT ''审核时间'' AFTER `reviewed_by_admin_id`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 精华操作审计字段（P2-4：记录谁在什么时候设置/取消精华）
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'Youtube' AND COLUMN_NAME = 'featured_by_type');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `Youtube` ADD COLUMN `featured_by_type` varchar(20) DEFAULT NULL COMMENT ''精华设置人类型: admin/volunteer'' AFTER `reviewed_at`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'Youtube' AND COLUMN_NAME = 'featured_by_id');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `Youtube` ADD COLUMN `featured_by_id` int(11) DEFAULT NULL COMMENT ''精华设置人ID'' AFTER `featured_by_type`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'Youtube' AND COLUMN_NAME = 'featured_at');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `Youtube` ADD COLUMN `featured_at` datetime DEFAULT NULL COMMENT ''精华设置时间'' AFTER `featured_by_id`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 论坛筛选复合索引（幂等）
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'Youtube' AND INDEX_NAME = 'idx_forum_filter');
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE `Youtube` ADD KEY `idx_forum_filter` (`audit_status`, `is_public`, `university_id`, `author_type`, `is_featured`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 历史数据回填 author_type / university_id（幂等：仅填充 NULL 行）
UPDATE `Youtube` qa
LEFT JOIN volunteer av ON qa.asker_volunteer_id = av.id
LEFT JOIN volunteer tv ON qa.target_volunteer_id = tv.id
SET qa.author_type = IF(qa.asker_volunteer_id IS NULL, 'student', 'volunteer'),
    qa.university_id = COALESCE(av.university_id, tv.university_id)
WHERE qa.author_type IS NULL OR qa.university_id IS NULL;

-- 2) 论坛治理表（IF NOT EXISTS 本身幂等）
CREATE TABLE IF NOT EXISTS forum_moderator_grant (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `volunteer_id` int(11) NOT NULL,
  `university_id` int(11) NOT NULL,
  `granted_by_admin_id` int(11) NOT NULL,
  `active` tinyint(4) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_forum_moderator` (`volunteer_id`, `university_id`),
  KEY `idx_moderator_university` (`university_id`, `active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='大学生论坛审核员授权';

CREATE TABLE IF NOT EXISTS forum_report (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `post_id` int(11) NOT NULL,
  `target_type` varchar(20) NOT NULL DEFAULT 'post' COMMENT '举报对象: post/comment',
  `comment_id` int(11) NOT NULL DEFAULT 0 COMMENT '评论ID（target_type=comment 时有效）',
  `reporter_id` int(11) NOT NULL,
  `reporter_type` varchar(20) NOT NULL,
  `reason` varchar(50) NOT NULL,
  `detail` varchar(500) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `reviewer_id` int(11) DEFAULT NULL,
  `reviewer_type` varchar(20) DEFAULT NULL,
  `decision_note` varchar(500) DEFAULT NULL,
  `penalty_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `reviewed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reporter_target` (`post_id`, `target_type`, `comment_id`, `reporter_id`, `reporter_type`),
  KEY `idx_report_status` (`status`, `created_at`),
  KEY `idx_report_post_status` (`post_id`, `status`),
  KEY `idx_report_comment` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛帖子举报';

CREATE TABLE IF NOT EXISTS forum_penalty (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `user_type` varchar(20) NOT NULL,
  `report_id` int(11) NOT NULL,
  `offense_number` int(11) NOT NULL,
  `muted_from` datetime NOT NULL,
  `muted_until` datetime NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `revoked_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_penalty_report` (`report_id`),
  KEY `idx_active_mute` (`user_id`, `user_type`, `status`, `muted_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛禁言处罚';

CREATE TABLE IF NOT EXISTS forum_appeal (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `penalty_id` int(11) NOT NULL,
  `appellant_id` int(11) NOT NULL,
  `appellant_type` varchar(20) NOT NULL,
  `reason` varchar(500) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `reviewer_id` int(11) DEFAULT NULL,
  `reviewer_type` varchar(20) DEFAULT NULL,
  `decision_note` varchar(500) DEFAULT NULL,
  `restore_post` tinyint(4) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `reviewed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_penalty_appeal` (`penalty_id`),
  KEY `idx_appeal_status` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛处罚申诉';

CREATE TABLE IF NOT EXISTS forum_sensitive_word (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `word` varchar(100) NOT NULL,
  `enabled` tinyint(4) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sensitive_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛屏蔽词';

-- 3) 既有表补索引（幂等）
-- 评论列表按帖子+时间查询
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'forum_comment' AND INDEX_NAME = 'idx_post_created');
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE `forum_comment` ADD KEY `idx_post_created` (`post_id`, `created_at`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 通知列表按用户+类型+时间查询
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification' AND INDEX_NAME = 'idx_user_type_created');
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE `notification` ADD KEY `idx_user_type_created` (`user_id`, `user_type`, `created_at`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
