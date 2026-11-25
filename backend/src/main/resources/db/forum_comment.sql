-- 论坛评论表
CREATE TABLE IF NOT EXISTS forum_comment (
    id INT NOT NULL AUTO_INCREMENT,
    post_id INT NOT NULL COMMENT '帖子ID（关联 Youtube 表）',
    user_id INT NOT NULL COMMENT '评论者ID',
    user_type VARCHAR(20) NOT NULL COMMENT '评论者类型：student/volunteer',
    content TEXT NOT NULL COMMENT '评论内容',
    parent_id INT DEFAULT NULL COMMENT '父评论ID（回复某条评论时使用）',
    status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE' COMMENT '评论状态: VISIBLE/HIDDEN',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_post_id (post_id),
    KEY idx_user_id (user_id),
    KEY idx_parent_id (parent_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛评论表';
