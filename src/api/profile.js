/**
 * 个人资料 API
 * 对应后端：ProfileController (/api/profile)
 */

import { request } from './index'

/**
 * 获取当前用户资料
 * GET /api/profile/my
 */
export function getMyProfile() {
  return request({ url: '/api/profile/my', method: 'GET' })
}

/**
 * 更新当前用户资料
 * POST /api/profile/update
 * @param {Object} data - { nickname, avatarUrl, name, phone, wechatId, major, grade }
 */
export function updateProfile(data) {
  return request({ url: '/api/profile/update', method: 'POST', data })
}
