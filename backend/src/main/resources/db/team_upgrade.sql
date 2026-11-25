-- ============================================================
-- 队伍功能升级 SQL
-- 功能：自由组队 + 队伍号邀请 + 队长申请/转让审核
-- ============================================================

-- 1. 修改 promo_team 表，新增字段
ALTER TABLE promo_team ADD COLUMN team_code VARCHAR(10) NOT NULL COMMENT '队伍号，2字母+3数字随机排列' AFTER id;
ALTER TABLE promo_team ADD COLUMN team_name VARCHAR(50) DEFAULT NULL COMMENT '队伍名称' AFTER team_code;
ALTER TABLE promo_team ADD COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 添加队伍号唯一索引
ALTER TABLE promo_team ADD UNIQUE KEY uk_team_code (team_code);

-- 添加组合唯一索引（大学+高中 组合只能有一支队伍）
-- 代码依赖此约束：自动组队/老师手动建队/修改目标高中均校验"同组合不能重复建队"
-- 注：线上库若已手工添加过此索引，本语句会报 Duplicate key name，可忽略（幂等执行需先检查）
ALTER TABLE promo_team ADD UNIQUE KEY uk_team (university_id, high_school_id);

-- 2. 新建 team_member 表 - 队伍成员关系
CREATE TABLE IF NOT EXISTS team_member (
  id INT AUTO_INCREMENT PRIMARY KEY,
  team_id INT NOT NULL COMMENT '队伍ID',
  volunteer_id INT NOT NULL COMMENT '志愿者ID',
  role VARCHAR(20) DEFAULT 'MEMBER' COMMENT '角色: MEMBER/CAPTAIN',
  joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_volunteer (volunteer_id),
  KEY idx_team (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='队伍成员关系表';

-- 3. 新建 team_role_application 表 - 队长申请/转让
CREATE TABLE IF NOT EXISTS team_role_application (
  id INT AUTO_INCREMENT PRIMARY KEY,
  team_id INT NOT NULL COMMENT '队伍ID',
  volunteer_id INT NOT NULL COMMENT '申请人ID',
  apply_role VARCHAR(20) NOT NULL COMMENT '申请角色: CAPTAIN/CAPTAIN_TRANSFER',
  reason VARCHAR(200) DEFAULT NULL COMMENT '申请理由',
  status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED',
  reviewed_by INT DEFAULT NULL COMMENT '审核人ID',
  reviewed_at DATETIME DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_team (team_id),
  KEY idx_volunteer (volunteer_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='队长申请表';
