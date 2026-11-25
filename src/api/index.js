/**
 * API 基础配置
 */

import { getToken, clearAuth } from '@/utils/auth'
import { useUserStore } from '@/store/modules/user'
import { reportError } from '@/utils/report'
import { BASE_URL } from '@/config/env'

// 防止多个 401 同时触发重复跳转
let isRedirectingToLogin = false

// 统一错误提示节流：多个请求同时失败时只提示一次，避免 toast 连弹
let lastErrorToastAt = 0
const ERROR_TOAST_THROTTLE_MS = 1500

function showErrorToast(title) {
  const now = Date.now()
  if (now - lastErrorToastAt < ERROR_TOAST_THROTTLE_MS) return
  lastErrorToastAt = now
  uni.showToast({ title, icon: 'none', duration: 2000 })
}

/**
 * 封装 uni.request，自动携带 JWT Token
 */
export function request(options) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    const fullUrl = BASE_URL + options.url

    uni.request({
      url: fullUrl,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.header,
      },
      success: (res) => {
        if (res.statusCode === 200) {
          const data = res.data
          // 后端返回格式: { code: 200, message: "...", data: ... }
          if (data.code === 200 || data.code === 0) {
            resolve(data.data)
          } else {
            reject(new Error(data.message || '请求失败'))
          }
        } else if (res.statusCode === 401) {
          // Token 过期或未登录，清除认证信息并跳转登录页
          if (!isRedirectingToLogin) {
            isRedirectingToLogin = true
            // 同时清除 storage 和 store，保持状态一致
            clearAuth()
            const userStore = useUserStore()
            userStore.token = ''
            userStore.userInfo = null
            userStore.userRole = ''
            uni.reLaunch({
              url: '/pages/common/login',
              complete: () => {
                setTimeout(() => { isRedirectingToLogin = false }, 2000)
              }
            })
          }
          reject(new Error('请先登录'))
        } else if (res.statusCode === 403) {
          // 403 = 权限不足/身份未认证（非"服务异常"），展示后端真实原因
          const msg = res.data?.message || '无权限访问'
          if (!options.silent) {
            uni.showToast({ title: msg, icon: 'none', duration: 2500 })
          }
          reject(new Error(msg))
        } else {
          const msg = res.data?.message || `请求错误 (${res.statusCode})`
          // 非业务错误码（如 500/502）：统一提示，避免页面静默失败
          if (!options.silent) {
            showErrorToast('服务异常，请稍后重试')
          }
          reportError({
            message: msg,
            url: options.url,
            method: options.method || 'GET',
            status: res.statusCode,
            type: 'http-error',
          })
          reject(new Error(msg))
        }
      },
      fail: (err) => {
        // 网络层失败（断网/超时/域名解析失败）：统一提示，避免页面静默失败
        const errorMessage = err?.errMsg || '网络连接失败'
        if (!options.silent) {
          showErrorToast('网络连接失败，请检查网络')
        }
        reportError({
          message: errorMessage,
          url: options.url,
          method: options.method || 'GET',
          type: 'network-error',
        })
        reject(new Error(errorMessage))
      },
    })
  })
}

export { BASE_URL }
