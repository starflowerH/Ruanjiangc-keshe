-- 大学生宣传资料管理员授权表（幂等，可在已有生产库重复执行）
CREATE TABLE IF NOT EXISTS volunteer_promotion_grant (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `volunteer_id` int(11) NOT NULL,
  `university_id` int(11) NOT NULL,
  `granted_by_admin_id` int(11) NOT NULL,
  `active` tinyint(4) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_volunteer_promotion_manager` (`volunteer_id`, `university_id`),
  KEY `idx_promotion_manager_university` (`university_id`, `active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='大学生宣传资料管理员授权';
