/**
 * 报名相关 API
 * 对应后端：VolunteerController (/api/volunteer)
 */

import { request } from './index'
import { searchSchools } from './highschool'

/**
 * 获取大学列表（报名时选择用）
 * 复用 university 模块，这里提供便捷入口
 */
export { getUniversityList } from './university'

/**
 * 获取高中名册（选择目标高中用）
 * 使用搜索接口，空关键词返回全部
 */
export function getHighSchoolList() {
  return searchSchools('')
}

/**
 * 提交志愿者报名
 * POST /api/volunteer/apply
 */
export function submitEnrollment(data) {
  return request({ url: '/api/volunteer/apply', method: 'POST', data })
}

/**
 * 查询我的报名状态
 * GET /api/volunteer/my-apply
 */
export function getMyEnrollment() {
  return request({ url: '/api/volunteer/my-apply', method: 'GET' })
}
