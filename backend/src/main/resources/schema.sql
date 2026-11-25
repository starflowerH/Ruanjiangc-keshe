-- 高校招生宣传小程序 - 数据库初始化脚本
-- 完整建库脚本：19 张当前业务表（已排除废弃表：user/school_info/question/answer/like_record/favorite/school_wiki/youtube 旧版）
CREATE DATABASE IF NOT EXISTS miniprogram DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
USE miniprogram;

-- ============================================================
-- 基础数据表
-- ============================================================

-- 高校信息表
DROP TABLE IF EXISTS university;
CREATE TABLE university (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '高校ID',
  `name` varchar(128) NOT NULL COMMENT '高校名称',
  `province` varchar(20) DEFAULT NULL COMMENT '所在省份',
  `city` varchar(20) DEFAULT NULL COMMENT '所在城市',
  `logo_url` varchar(512) DEFAULT NULL COMMENT '高校Logo链接',
  `description` text DEFAULT NULL COMMENT '高校简介/宣传资料',
  `created_at` datetime DEFAULT current_timestamp() COMMENT '创建时间',
  `updated_at` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='高校信息表';

-- 高中名册表
DROP TABLE IF EXISTS high_school;
CREATE TABLE high_school (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '高中ID',
  `name` varchar(128) NOT NULL COMMENT '高中名称',
  `province` varchar(32) NOT NULL COMMENT '省份',
  `city` varchar(32) NOT NULL COMMENT '城市',
  `district` varchar(32) DEFAULT NULL COMMENT '区县',
  `created_at` datetime DEFAULT current_timestamp() COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='高中名册表';

-- 管理员用户表
DROP TABLE IF EXISTS admin_user;
CREATE TABLE admin_user (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `username` varchar(64) NOT NULL COMMENT '登录账号',
  `password_hash` varchar(256) NOT NULL COMMENT '密码哈希(BCrypt)',
  `role` varchar(20) NOT NULL COMMENT '角色: SUPER_ADMIN/NORMAL_ADMIN/TEACHER',
  `university_id` int(11) DEFAULT NULL COMMENT '所属高校ID(仅TEACHER)',
  `name` varchar(64) DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `permissions` text DEFAULT NULL COMMENT 'JSON权限列表',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态: 1=正常, 0=禁用',
  `created_at` datetime DEFAULT current_timestamp() COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_university_id` (`university_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员用户表';

-- 微信用户主体表
DROP TABLE IF EXISTS user_wechat;
CREATE TABLE user_wechat (
  `openid` varchar(64) NOT NULL COMMENT '微信OpenID',
  `unionid` varchar(64) DEFAULT NULL COMMENT '微信UnionID',
  `nickname` varchar(128) DEFAULT NULL COMMENT '微信昵称',
  `avatar_url` varchar(512) DEFAULT NULL COMMENT '微信头像URL',
  `created_at` datetime DEFAULT current_timestamp() COMMENT '首次进入时间',
  `updated_at` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '最后更新时间',
  PRIMARY KEY (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='微信用户主体表';

-- ============================================================
-- 身份表（依赖 university/high_school）
-- ============================================================

-- 高中生用户表
DROP TABLE IF EXISTS high_school_student;
CREATE TABLE high_school_student (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '高中生记录ID',
  `openid` varchar(64) NOT NULL COMMENT '微信OpenID',
  `name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `nickname` varchar(100) DEFAULT NULL COMMENT '高中生身份昵称',
  `avatar_url` varchar(500) DEFAULT NULL COMMENT '微信头像URL',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号码',
  `high_school_id` int(11) DEFAULT NULL COMMENT '所属高中ID',
  `created_at` datetime DEFAULT current_timestamp() COMMENT '注册时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  KEY `idx_high_school_id` (`high_school_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='高中生用户表';

-- 志愿者报名表
DROP TABLE IF EXISTS volunteer;
CREATE TABLE volunteer (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '报名记录ID',
  `openid` varchar(64) NOT NULL COMMENT '微信OpenID',
  `name` varchar(64) NOT NULL COMMENT '真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号码（与邮箱至少填写一项）',
  `wechat_id` varchar(64) DEFAULT NULL COMMENT '微信号',
  `student_no` varchar(64) DEFAULT NULL COMMENT '学号',
  `gender` varchar(16) DEFAULT NULL COMMENT '性别',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱（与电话至少填写一项）',
  `university_id` int(11) NOT NULL COMMENT '所属大学ID',
  `grade` varchar(20) DEFAULT NULL COMMENT '年级',
  `major` varchar(128) DEFAULT NULL COMMENT '专业',
  `target_high_school_id` int(11) DEFAULT NULL COMMENT '目标高中ID(选填,可多校)',
  `is_captain_applicant` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否申请队长: 0=否, 1=是',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '审核状态: PENDING/APPROVED/REJECTED',
  `created_at` datetime DEFAULT current_timestamp() COMMENT '报名时间',
  `updated_at` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '状态变更时间',
  `cancelled_at` datetime DEFAULT NULL COMMENT '取消时间（用于限时撤销判断）',
  `cancel_reason` varchar(200) DEFAULT NULL COMMENT '取消理由（选填）',
  PRIMARY KEY (`id`),
  KEY `idx_openid` (`openid`),
  KEY `idx_university_id` (`university_id`),
  KEY `idx_target_high_school_id` (`target_high_school_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='志愿者报名表';

-- 志愿者-目标高中关联表（多校选填）
DROP TABLE IF EXISTS volunteer_high_school;
CREATE TABLE volunteer_high_school (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `volunteer_id` int(11) NOT NULL,
  `high_school_id` int(11) NOT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_volunteer_high_school` (`volunteer_id`,`high_school_id`),
  KEY `idx_volunteer` (`volunteer_id`),
  KEY `idx_high_school` (`high_school_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='志愿者目标高中关联表';

-- 自定义字段定义表
DROP TABLE IF EXISTS custom_field_definition;
CREATE TABLE custom_field_definition (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '字段定义ID',
  `university_id` int(11) NOT NULL COMMENT '所属大学ID',
  `field_label` varchar(128) NOT NULL COMMENT '字段标签',
  `field_type` varchar(20) NOT NULL COMMENT '字段类型: TEXT/SELECT/NUMBER',
  `field_options` text DEFAULT NULL COMMENT '下拉选项JSON数组',
  `is_required` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否必填: 0=选填, 1=必填',
  PRIMARY KEY (`id`),
  KEY `idx_university_id` (`university_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自定义字段定义表';

-- 志愿者自定义字段值表
DROP TABLE IF EXISTS volunteer_custom_value;
CREATE TABLE volunteer_custom_value (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `volunteer_id` int(11) NOT NULL COMMENT '报名记录ID',
  `field_id` int(11) NOT NULL COMMENT '字段定义ID',
  `field_value` varchar(512) DEFAULT NULL COMMENT '填写的值',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vol_field` (`volunteer_id`,`field_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='志愿者自定义字段值表';

-- ============================================================
-- 队伍相关表
-- ============================================================

-- 宣传队伍表
DROP TABLE IF EXISTS promo_team;
CREATE TABLE promo_team (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '队伍ID',
  `team_code` varchar(10) NOT NULL COMMENT '队伍号，2字母+3数字随机排列',
  `team_name` varchar(50) DEFAULT NULL COMMENT '队伍名称',
  `high_school_id` int(11) DEFAULT NULL,
  `university_id` int(11) DEFAULT NULL,
  `captain_volunteer_id` int(11) DEFAULT NULL COMMENT '队长志愿者ID',
  `description` varchar(500) DEFAULT NULL COMMENT '队伍简介',
  `created_at` datetime DEFAULT current_timestamp() COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_team_code` (`team_code`),
  UNIQUE KEY `uk_team` (`university_id`,`high_school_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宣传队伍表';

-- 队伍成员关系表
DROP TABLE IF EXISTS team_member;
CREATE TABLE team_member (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `team_id` int(11) NOT NULL COMMENT '队伍ID',
  `volunteer_id` int(11) NOT NULL COMMENT '志愿者ID',
  `role` varchar(20) DEFAULT 'MEMBER' COMMENT '角色: MEMBER/CAPTAIN',
  `joined_at` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_volunteer` (`volunteer_id`),
  KEY `idx_team` (`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='队伍成员关系表';

-- 队长申请表
DROP TABLE IF EXISTS team_role_application;
CREATE TABLE team_role_application (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `team_id` int(11) NOT NULL COMMENT '队伍ID',
  `volunteer_id` int(11) NOT NULL COMMENT '申请人ID',
  `apply_role` varchar(20) NOT NULL COMMENT '申请角色: CAPTAIN/CAPTAIN_TRANSFER',
  `reason` varchar(200) DEFAULT NULL COMMENT '申请理由',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED',
  `reviewed_by` int(11) DEFAULT NULL COMMENT '审核人ID',
  `reviewed_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_team` (`team_id`),
  KEY `idx_volunteer` (`volunteer_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='队长申请表';

-- 队伍成员手动调整覆盖表
DROP TABLE IF EXISTS team_override;
CREATE TABLE team_override (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `promo_team_id` int(11) NOT NULL COMMENT '队伍ID',
  `volunteer_id` int(11) NOT NULL COMMENT '志愿者ID',
  `action` varchar(10) NOT NULL COMMENT 'IN=强制入队 / OUT=强制移出',
  `created_by` int(11) DEFAULT NULL COMMENT '操作老师/管理员ID',
  `created_at` datetime DEFAULT current_timestamp() COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_team_volunteer` (`promo_team_id`,`volunteer_id`),
  KEY `idx_volunteer` (`volunteer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='队伍成员手动调整覆盖表';

-- ============================================================
-- 高校宣传内容
-- ============================================================

-- 大学宣传资料表（老师编辑本校宣传内容）
DROP TABLE IF EXISTS university_content;
CREATE TABLE university_content (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `university_id` int(11) NOT NULL,
  `logo_url` varchar(500) DEFAULT NULL COMMENT '校徽Logo地址',
  `banner_url` varchar(500) DEFAULT NULL COMMENT '宣传头图地址',
  `gallery_images` text DEFAULT NULL COMMENT '校园风光图片JSON数组',
  `slogan` varchar(100) DEFAULT NULL COMMENT '校训/标语',
  `tags` varchar(300) DEFAULT NULL COMMENT '学校标签逗号分隔',
  `description` text DEFAULT NULL COMMENT '学校简介',
  `majors` text DEFAULT NULL COMMENT '优势专业JSON数组',
  `score_lines` text DEFAULT NULL COMMENT '历年分数线JSON数组',
  `admission_policy` text DEFAULT NULL COMMENT '招生政策说明',
  `admission_url` varchar(500) DEFAULT NULL COMMENT '招生官网地址',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `created_at` datetime DEFAULT current_timestamp() COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_university_id` (`university_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='大学宣传资料表';

DROP TABLE IF EXISTS volunteer_promotion_grant;
CREATE TABLE volunteer_promotion_grant (
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

-- ============================================================
-- 问答/论坛相关表
-- ============================================================

-- 问答表（表名为历史遗留，实际是问答/帖子系统）
DROP TABLE IF EXISTS Youtube;
CREATE TABLE `Youtube` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `asker_student_id` int(11) DEFAULT NULL COMMENT '提问者学生ID（高中生）',
  `asker_volunteer_id` int(11) DEFAULT NULL COMMENT '提问者志愿者ID（大学生）',
  `target_volunteer_id` int(11) DEFAULT NULL COMMENT '目标志愿者ID（一对一提问时用）',
  `university_id` int(11) DEFAULT NULL COMMENT '帖子关联大学，NULL 表示全部高校',
  `author_type` varchar(20) DEFAULT NULL COMMENT '发帖身份: student/volunteer',
  `title` varchar(200) DEFAULT NULL COMMENT '帖子标题（论坛模式用）',
  `question_content` text NOT NULL COMMENT '问题内容',
  `answer_content` text DEFAULT NULL COMMENT '回答内容',
  `asked_at` datetime DEFAULT current_timestamp() COMMENT '提问时间',
  `answered_at` datetime DEFAULT NULL COMMENT '回答时间',
  `audit_status` varchar(20) DEFAULT 'PENDING' COMMENT '审核状态：PENDING/APPROVED/REJECTED',
  `is_public` tinyint(4) DEFAULT 0 COMMENT '是否公开：0=私密，1=公开',
  `is_featured` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否精华',
  `reviewed_by_volunteer_id` int(11) DEFAULT NULL COMMENT '大学生审核员ID',
  `reviewed_by_admin_id` int(11) DEFAULT NULL COMMENT '管理员审核人ID',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
  `featured_by_type` varchar(20) DEFAULT NULL COMMENT '精华设置人类型: admin/volunteer',
  `featured_by_id` int(11) DEFAULT NULL COMMENT '精华设置人ID',
  `featured_at` datetime DEFAULT NULL COMMENT '精华设置时间',
  `view_count` int(11) DEFAULT 0 COMMENT '浏览次数',
  `category` varchar(32) DEFAULT NULL COMMENT '分类: admission/major/campus/study/other',
  PRIMARY KEY (`id`),
  KEY `idx_asker_student_id` (`asker_student_id`),
  KEY `idx_asker_volunteer_id` (`asker_volunteer_id`),
  KEY `idx_target_volunteer_id` (`target_volunteer_id`),
  KEY `idx_audit_status` (`audit_status`),
  KEY `idx_is_public` (`is_public`),
  KEY `idx_asked_at` (`asked_at`),
  KEY `idx_forum_filter` (`audit_status`, `is_public`, `university_id`, `author_type`, `is_featured`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问答表（表名为历史遗留）';

DROP TABLE IF EXISTS forum_moderator_grant;
CREATE TABLE forum_moderator_grant (
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

DROP TABLE IF EXISTS forum_report;
CREATE TABLE forum_report (
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
  KEY `idx_report_comment` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛帖子举报';

DROP TABLE IF EXISTS forum_penalty;
CREATE TABLE forum_penalty (
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

DROP TABLE IF EXISTS forum_appeal;
CREATE TABLE forum_appeal (
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

DROP TABLE IF EXISTS forum_sensitive_word;
CREATE TABLE forum_sensitive_word (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `word` varchar(100) NOT NULL,
  `enabled` tinyint(4) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sensitive_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛屏蔽词';

-- 论坛评论表
DROP TABLE IF EXISTS forum_comment;
CREATE TABLE forum_comment (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL COMMENT '评论者ID',
  `user_type` varchar(20) NOT NULL COMMENT '评论者类型: student/volunteer',
  `post_id` int(11) NOT NULL COMMENT '帖子ID（关联Youtube表id）',
  `content` text NOT NULL COMMENT '评论内容',
  `parent_id` int(11) DEFAULT NULL COMMENT '父评论ID（回复某条评论时使用）',
  `status` varchar(20) NOT NULL DEFAULT 'VISIBLE' COMMENT '评论状态: VISIBLE/HIDDEN',
  `created_at` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_post_created` (`post_id`, `created_at`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛评论表';

-- 评论点赞表
DROP TABLE IF EXISTS forum_comment_like;
CREATE TABLE forum_comment_like (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `comment_id` int(11) NOT NULL COMMENT '评论ID',
  `user_id` int(11) NOT NULL COMMENT '点赞用户ID',
  `user_type` varchar(20) NOT NULL COMMENT '用户类型: student/volunteer',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_liker` (`comment_id`, `user_id`, `user_type`),
  KEY `idx_comment_like_comment` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞表';

-- 点赞表（target_type 区分帖子/回答）
DROP TABLE IF EXISTS qa_like;
CREATE TABLE qa_like (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `qa_id` int(11) NOT NULL COMMENT '问答/帖子ID',
  `user_id` int(11) NOT NULL COMMENT '用户ID',
  `user_type` varchar(20) NOT NULL COMMENT '用户类型: student/volunteer',
  `target_type` varchar(20) NOT NULL COMMENT '点赞对象: post(帖子)/answer(回答)',
  `created_at` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`,`user_type`,`qa_id`,`target_type`),
  KEY `idx_qa` (`qa_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞表';

-- 收藏表（仅针对帖子）
DROP TABLE IF EXISTS qa_favorite;
CREATE TABLE qa_favorite (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `qa_id` int(11) NOT NULL COMMENT '问答/帖子ID',
  `user_id` int(11) NOT NULL COMMENT '用户ID',
  `user_type` varchar(20) NOT NULL COMMENT '用户类型: student/volunteer',
  `created_at` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_qa` (`user_id`,`user_type`,`qa_id`),
  KEY `idx_qa` (`qa_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- 通知表
DROP TABLE IF EXISTS notification;
CREATE TABLE notification (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL COMMENT '接收通知的用户ID',
  `user_type` varchar(20) NOT NULL COMMENT '用户类型：student/volunteer/admin',
  `type` varchar(30) NOT NULL COMMENT '通知类型：reply/comment/system/like',
  `title` varchar(100) NOT NULL COMMENT '通知标题',
  `content` text NOT NULL COMMENT '通知内容',
  `source_id` int(11) DEFAULT NULL COMMENT '关联的资源ID（问题ID/帖子ID等）',
  `source_type` varchar(30) DEFAULT NULL COMMENT '资源类型：question/answer/post',
  `source_title` varchar(200) DEFAULT NULL COMMENT '资源标题',
  `is_read` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否已读：0=未读，1=已读',
  `created_at` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_type` (`user_type`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_user_type_created` (`user_id`, `user_type`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';
