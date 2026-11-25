/**
 * 通知相关 API
 * 对应后端：NotificationController (/api/notification)
 */

import { request } from './index'

/**
 * 获取通知列表（分页）
 * GET /api/notification/list?page=1&size=20
 */
export function getNotifications(params = {}) {
  return request({ url: '/api/notification/list', method: 'GET', data: params })
}

/**
 * 获取未读通知数量
 * GET /api/notification/unread-count
 */
export function getUnreadCount() {
  return request({ url: '/api/notification/unread-count', method: 'GET' })
}

/** 获取别人评论帖子或回复评论产生的互动消息 */
export function getInteractionNotifications(params = {}) {
  return request({ url: '/api/notification/interactions', method: 'GET', data: params })
}

/** 获取互动消息未读数 */
export function getInteractionUnreadCount() {
  return request({ url: '/api/notification/interaction-unread-count', method: 'GET' })
}

/** 将互动消息批量标记为已读 */
export function markInteractionsRead() {
  return request({ url: '/api/notification/mark-interactions-read', method: 'POST' })
}

/**
 * 标记单条通知为已读
 * POST /api/notification/mark-read?id=1
 */
export function markAsRead(id) {
  return request({ url: '/api/notification/mark-read', method: 'POST', data: { id } })
}

/**
 * 标记所有通知为已读
 * POST /api/notification/mark-all-read
 */
export function markAllAsRead() {
  return request({ url: '/api/notification/mark-all-read', method: 'POST' })
}
