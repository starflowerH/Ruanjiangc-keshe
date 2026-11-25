/**
 * 认证相关 API
 * 对应后端：AuthController (/api/auth)
 */

import { request } from './index'

/**
 * 微信小程序登录
 * POST /api/auth/wx-login
 * @param {string} code - wx.login 获取的 code
 */
export function wxLogin(code, nickname, avatarUrl, identityRole) {
  return request({
    url: '/api/auth/wx-login',
    method: 'POST',
    data: { code, nickname, avatarUrl, identityRole }
  })
}

/**
 * 切换同一微信账号当前使用的身份
 * POST /api/auth/switch-role
 */
export function switchIdentity(role) {
  return request({ url: '/api/auth/switch-role', method: 'POST', data: { role } })
}

/**
 * 绑定手机号（场景C：已有JWT登录态时绑定手机号）
 * POST /api/auth/bind-phone
 * @param {Object} data - { phoneCode }
 */
export function bindPhone(data) {
  return request({ url: '/api/auth/bind-phone', method: 'POST', data })
}

/**
 * 管理员登录
 * POST /api/auth/admin-login
 * @param {Object} data - { username, password }
 */
export function adminLogin(data) {
  return request({ url: '/api/auth/admin-login', method: 'POST', data })
}

/**
 * 老师注册
 * POST /api/auth/teacher-register
 * @param {Object} data - { code, name, phone, universityId, targetHighSchoolId }
 */
export function teacherRegister(data) {
  return request({ url: '/api/auth/teacher-register', method: 'POST', data })
}

/**
 * 老师登录
 * POST /api/auth/teacher-login
 * @param {string} code - wx.login 获取的 code
 */
export function teacherLogin(code) {
  return request({ url: '/api/auth/teacher-login', method: 'POST', data: { code } })
}
