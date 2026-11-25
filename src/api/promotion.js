/**
 * 大学宣传资料 API
 * 对应后端：UniversityContentController (/api/university-content)
 */

import { request } from './index'

/**
 * 获取大学宣传资料（公开接口）
 * GET /api/university-content/{universityId}
 * @param {number} universityId - 大学ID
 */
export function getPromotion(universityId) {
  return request({ url: `/api/university-content/${universityId}`, method: 'GET' })
}

/**
 * 保存或更新大学宣传资料（已授权老师或大学生）
 * POST /api/university-content/save
 * @param {Object} data - 宣传资料DTO
 */
export function savePromotion(data) {
  return request({ url: '/api/university-content/save', method: 'POST', data })
}

export function getManagerStatus() {
  return request({ url: '/api/university-content/manager/status', method: 'GET' })
}
