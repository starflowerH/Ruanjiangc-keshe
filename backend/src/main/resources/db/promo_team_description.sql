-- 队伍简介字段（队伍成员可自行填写）
-- 用于"队伍详情页"的【队伍简介】模块
ALTER TABLE promo_team ADD COLUMN description VARCHAR(500) DEFAULT NULL COMMENT '队伍简介' AFTER captain_volunteer_id;
