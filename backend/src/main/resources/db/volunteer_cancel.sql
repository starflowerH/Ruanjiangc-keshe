-- ============================================================
-- 志愿者取消/撤销功能
-- 功能：老师取消已通过志愿者（2天内可撤销），被取消后可重新报名
-- ============================================================

ALTER TABLE volunteer
  ADD COLUMN cancelled_at DATETIME DEFAULT NULL COMMENT '取消时间（用于限时撤销判断）' AFTER updated_at,
  ADD COLUMN cancel_reason VARCHAR(200) DEFAULT NULL COMMENT '取消理由（选填）' AFTER cancelled_at;

-- openid 索引：重新报名/撤销的锁定读（FOR UPDATE）走索引只锁目标行，避免全表锁
ALTER TABLE volunteer ADD INDEX idx_openid (openid);
