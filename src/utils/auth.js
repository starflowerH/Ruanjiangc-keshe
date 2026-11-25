/**
 * 认证工具
 * 处理微信登录、Token 管理
 */

import { getStorage, setStorage, removeStorage } from './storage'

const TOKEN_KEY = 'token'
const USER_INFO_KEY = 'userInfo'
const ROLE_KEY = 'userRole'

/**
 * 获取 Token
 */
export function getToken() {
  return getStorage(TOKEN_KEY, '')
}

/**
 * 设置 Token
 */
export function setToken(token) {
  setStorage(TOKEN_KEY, token)
}

/**
 * 移除 Token
 */
export function removeToken() {
  removeStorage(TOKEN_KEY)
}

/**
 * 获取用户信息
 */
export function getUserInfo() {
  return getStorage(USER_INFO_KEY, null)
}

/**
 * 设置用户信息
 */
export function setUserInfo(info) {
  setStorage(USER_INFO_KEY, info)
}

/**
 * 移除用户信息
 */
export function removeUserInfo() {
  removeStorage(USER_INFO_KEY)
}

/**
 * 获取用户角色
 * student | volunteer | admin
 */
export function getUserRole() {
  return getStorage(ROLE_KEY, '')
}

/**
 * 设置用户角色
 */
export function setUserRole(role) {
  setStorage(ROLE_KEY, role)
}

/**
 * 移除用户角色
 */
export function removeUserRole() {
  removeStorage(ROLE_KEY)
}

/**
 * 是否已登录
 */
export function isLoggedIn() {
  return !!getToken()
}

/**
 * 微信登录获取 code
 */
export function wxLogin() {
  return new Promise((resolve, reject) => {
    uni.login({
      provider: 'weixin',
      success: (res) => {
        if (res.code) {
          resolve(res.code)
        } else {
          reject(new Error('微信登录失败'))
        }
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}

/**
 * 获取微信用户信息（需要用户授权）
 */
export function wxGetUserProfile() {
  return new Promise((resolve, reject) => {
    uni.getUserProfile({
      desc: '用于完善用户资料',
      success: (res) => {
        resolve(res.userInfo)
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}

/**
 * 清除所有认证信息
 */
export function clearAuth() {
  removeToken()
  removeUserInfo()
  removeUserRole()
}

/**
 * 检查登录状态，未登录跳转登录页
 */
export function checkLogin(redirectUrl) {
  if (!isLoggedIn()) {
    const url = redirectUrl ? `/pages/common/login?redirect=${encodeURIComponent(redirectUrl)}` : '/pages/common/login'
    uni.reLaunch({ url })
    return false
  }
  return true
}
