-- 队长通讯录信息：与报名资料分离，便于队长确认对外联系方式。
ALTER TABLE promo_team
  ADD COLUMN captain_contact_phone VARCHAR(20) DEFAULT NULL COMMENT '队长对外通讯手机号' AFTER captain_volunteer_id,
  ADD COLUMN captain_contact_wechat VARCHAR(64) DEFAULT NULL COMMENT '队长对外通讯微信号' AFTER captain_contact_phone;
