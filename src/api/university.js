/**
 * 高校信息相关 API
 * 注意：后端暂无独立的 UniversityController，高校列表由管理员管理
 * 此模块提供前端展示所需的接口，后续需要后端补充对应 Controller
 */

import { request } from './index'

/**
 * 获取高校列表
 * 暂时调用管理员接口获取，后续需后端新增公开接口
 */
export function getUniversityList(params = {}) {
  // TODO: 后端需要新增 GET /api/university/list 公开接口
  return request({ url: '/api/university/list', method: 'GET', data: params })
}

/**
 * 获取高校详情
 * @param {number} id - 高校ID
 */
export function getUniversityDetail(id) {
  return request({ url: `/api/university/${id}`, method: 'GET' })
}

/**
 * 获取高校录取分数线（广东生源，真实数据）
 * @param {number} id - 高校ID
 */
export function getUniversityScoreLines(id) {
  return request({ url: `/api/university/${id}/score-lines`, method: 'GET' })
}

/**
 * 获取到过某高中的大学列表（高中生视角）
 * @param {number} highSchoolId - 高中ID
 */
export function getUniversitiesByHighSchool(highSchoolId) {
  return request({ url: `/api/volunteer/by-high-school`, method: 'GET', data: { highSchoolId } })
}
