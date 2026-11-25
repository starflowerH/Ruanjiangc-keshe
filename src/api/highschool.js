/**
 * 高中名册相关 API
 * 对应后端：HighSchoolController (/api/high-school)
 */

import { request } from './index'

/**
 * 获取所有省份列表
 * GET /api/high-school/provinces
 */
export function getProvinces() {
  return request({ url: '/api/high-school/provinces', method: 'GET' })
}

/**
 * 根据省份获取城市列表
 * GET /api/high-school/cities?province=河北省
 * @param {string} province - 省份名
 */
export function getCities(province) {
  return request({ url: '/api/high-school/cities', method: 'GET', data: { province } })
}

/**
 * 根据省份+城市获取高中列表
 * GET /api/high-school/schools?province=河北省&city=石家庄市
 * @param {string} province - 省份名
 * @param {string} city - 城市名
 */
export function getSchools(province, city) {
  return request({ url: '/api/high-school/schools', method: 'GET', data: { province, city } })
}

/**
 * 按关键词搜索高中
 * GET /api/high-school/search?keyword=一中
 * @param {string} keyword - 搜索关键词
 */
export function searchSchools(keyword) {
  return request({ url: '/api/high-school/search', method: 'GET', data: { keyword } })
}
