-- 通知表
CREATE TABLE IF NOT EXISTS notification (
    id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL COMMENT '接收通知的用户ID',
    user_type VARCHAR(20) NOT NULL COMMENT '用户类型：student/volunteer/admin',
    type VARCHAR(30) NOT NULL COMMENT '通知类型：reply/comment/system/like',
    title VARCHAR(100) NOT NULL COMMENT '通知标题',
    content TEXT NOT NULL COMMENT '通知内容',
    source_id INT DEFAULT NULL COMMENT '关联的资源ID（问题ID/帖子ID等）',
    source_type VARCHAR(30) DEFAULT NULL COMMENT '资源类型：question/answer/post',
    source_title VARCHAR(200) DEFAULT NULL COMMENT '资源标题',
    is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0=未读，1=已读',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_user_type (user_type),
    KEY idx_is_read (is_read),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';
