/**
 * 高中生相关 API
 * 对应后端：StudentController (/api/student)
 */

import { request } from './index'

/**
 * 高中生注册
 * POST /api/student/register
 * @param {Object} data - { nickname, name, phone, avatarUrl, highSchoolId }
 * @returns {Promise<Object>} 包含新 token、relatedId 和学生基础信息
 */
export function studentRegister(data) {
  return request({ url: '/api/student/register', method: 'POST', data })
}

/**
 * 获取我的信息（高中生）
 * GET /api/student/my-info
 */
export function getMyInfo() {
  return request({ url: '/api/student/my-info', method: 'GET' })
}
