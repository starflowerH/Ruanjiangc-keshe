-- 身份显示名称隔离迁移
-- 高中生使用 nickname，大学生沿用 volunteer.name 作为唯一显示名称。

SET @ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'high_school_student' AND column_name = 'nickname'),
  'SELECT 1',
  'ALTER TABLE high_school_student ADD COLUMN nickname VARCHAR(100) DEFAULT NULL COMMENT ''高中生身份昵称'' AFTER name'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 旧账号使用各身份自己的姓名初始化，避免继续展示共享微信昵称。
UPDATE high_school_student
SET nickname = name
WHERE nickname IS NULL OR nickname = '';

-- 兼容已执行过旧版迁移的数据库：保留大学生昵称值后删除重复字段。
SET @ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'volunteer' AND column_name = 'nickname'),
  'UPDATE volunteer SET name = TRIM(nickname) WHERE nickname IS NOT NULL AND CHAR_LENGTH(TRIM(nickname)) > 0',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'volunteer' AND column_name = 'nickname'),
  'ALTER TABLE volunteer DROP COLUMN nickname',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
