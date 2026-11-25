-- ============================================================
-- 大学等级/类型筛选功能
-- 第1项: 学校搜索-等级筛选(本科/职校/综合-参考夸克)
-- ============================================================

ALTER TABLE university
  ADD COLUMN level VARCHAR(10) DEFAULT NULL COMMENT '办学层次: 本科/专科' AFTER city,
  ADD COLUMN type_name VARCHAR(20) DEFAULT NULL COMMENT '学校类型: 综合/理工/财经等(参考夸克分类)' AFTER level;

-- 数据填充: 从 description 解析(简介开头为"公办 本科 综合类"格式)
-- 已执行(2026-08-13): 本科214 / 专科57, 12种类型
