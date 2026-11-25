-- ============================================================
-- 点赞 + 收藏功能 SQL
-- 功能：帖子点赞/回答点赞/帖子收藏（支持取消）
-- ============================================================

-- 1. 点赞表（通用型，target_type 区分帖子/回答）
CREATE TABLE IF NOT EXISTS qa_like (
  id INT AUTO_INCREMENT PRIMARY KEY,
  qa_id INT NOT NULL COMMENT '问答/帖子ID',
  user_id INT NOT NULL COMMENT '用户ID',
  user_type VARCHAR(20) NOT NULL COMMENT '用户类型: student/volunteer',
  target_type VARCHAR(20) NOT NULL COMMENT '点赞对象: post(帖子)/answer(回答)',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_target (user_id, user_type, qa_id, target_type),
  KEY idx_qa (qa_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞表';

-- 2. 收藏表（仅针对帖子）
CREATE TABLE IF NOT EXISTS qa_favorite (
  id INT AUTO_INCREMENT PRIMARY KEY,
  qa_id INT NOT NULL COMMENT '问答/帖子ID',
  user_id INT NOT NULL COMMENT '用户ID',
  user_type VARCHAR(20) NOT NULL COMMENT '用户类型: student/volunteer',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_qa (user_id, user_type, qa_id),
  KEY idx_qa (qa_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';
