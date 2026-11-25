-- ============================================================
-- 队伍成员手动调整覆盖表
-- 功能：老师自定义组队（手动指定成员入队/移出、自定义建队）
-- 成员判定规则：组合派生成员 + IN 记录 - OUT 记录
-- ============================================================

CREATE TABLE IF NOT EXISTS team_override (
  id INT AUTO_INCREMENT PRIMARY KEY,
  promo_team_id INT NOT NULL COMMENT '队伍ID',
  volunteer_id INT NOT NULL COMMENT '志愿者ID',
  action VARCHAR(10) NOT NULL COMMENT 'IN=强制入队 / OUT=强制移出',
  created_by INT DEFAULT NULL COMMENT '操作老师/管理员ID',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_team_volunteer (promo_team_id, volunteer_id),
  KEY idx_volunteer (volunteer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='队伍成员手动调整覆盖表';
