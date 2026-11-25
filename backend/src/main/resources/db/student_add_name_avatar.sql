ALTER TABLE high_school_student
ADD COLUMN name VARCHAR(50) DEFAULT NULL COMMENT '真实姓名' AFTER openid;

ALTER TABLE high_school_student
ADD COLUMN avatar_url VARCHAR(500) DEFAULT NULL COMMENT '微信头像URL' AFTER name;
