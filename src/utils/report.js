/**
 * 前端错误上报 - 收集小程序端异常并转发到后端，由后端代发到 Sentry
 *
 * 注意：小程序受微信 request 合法域名限制，不能直连 Sentry ingest 域名，
 * 所以这里只 POST 到自己的后端 /api/report/error（hynu.site 已在白名单内）。
 */

// 注意：不要从 @/api/index 导入 BASE_URL，避免与 api/index.js 循环依赖
const BASE_URL = 'https://hynu.site'

// 上报节流：同类型错误 10 秒内只报一次，防止错误风暴刷爆 Sentry 配额
let lastReportAt = 0
const REPORT_THROTTLE_MS = 10000
// 记录最近 20 条错误指纹，用于去重
const recentFingerprints = []
const MAX_RECENT = 20

function fingerprint(msg, url) {
  return (msg || '').slice(0, 80) + '@' + (url || '')
}

// 序列化单个上报字段：JSON.stringify 优先，循环引用等场景退化为 toString，
// 避免错误对象被 String() 转成 "[object Object]" 导致无法定位
function serializeField(value, maxLen) {
  if (value == null) return undefined
  if (typeof value === 'string') return value.slice(0, maxLen)
  let str = ''
  try {
    str = JSON.stringify(value, (key, val) => {
      // 序列化 Error 时保留 name/message/stack，避免变成 {}
      if (val instanceof Error) {
        return {
          name: val.name,
          message: val.message,
          stack: val.stack ? String(val.stack).slice(0, maxLen) : undefined
        }
      }
      return val
    })
  } catch (e) {
    try {
      str = String(value)
    } catch (e2) {
      str = '[unserializable]'
    }
  }
  if (!str) str = String(value)
  return str.slice(0, maxLen)
}

/**
 * 上报错误到后端
 * @param {Object} payload { message, stack, url, method, status, type, version }
 */
export function reportError(payload = {}) {
  try {
    // 未提供 message 的直接忽略
    if (!payload.message) return

    const fp = fingerprint(payload.message, payload.url)
    if (recentFingerprints.includes(fp)) return
    recentFingerprints.push(fp)
    if (recentFingerprints.length > MAX_RECENT) recentFingerprints.shift()

    const now = Date.now()
    if (now - lastReportAt < REPORT_THROTTLE_MS) return
    lastReportAt = now

    uni.request({
      url: BASE_URL + '/api/report/error',
      method: 'POST',
      header: { 'Content-Type': 'application/json' },
      data: {
        message: serializeField(payload.message, 500),
        stack: serializeField(payload.stack, 2000),
        url: serializeField(payload.url, 2000),
        method: serializeField(payload.method, 2000),
        status: serializeField(payload.status, 2000),
        type: serializeField(payload.type, 500),
        version: '1.0.0',
      },
      // 上报失败不影响业务，静默忽略
      fail: () => {},
    })
  } catch (e) {
    // 上报模块自身出错绝不能影响主流程
  }
}

/**
 * 初始化全局错误监听（微信小程序平台）
 * 在 App.vue onLaunch 中调用
 */
export function initGlobalErrorReport() {
  // #ifdef MP-WEIXIN
  // 微信开发者工具的 SDK 在处理异常回调时偶发传入 undefined，
  // 跳过全局监听，避免错误上报机制阻塞页面启动；真机和正式环境仍保留上报。
  // 优先新 API（getSystemInfoSync 已废弃，控制台会有弃用警告）
  const systemInfo = typeof wx !== 'undefined'
    ? (typeof wx.getDeviceInfo === 'function' ? wx.getDeviceInfo()
      : typeof wx.getSystemInfoSync === 'function' ? wx.getSystemInfoSync()
      : null)
    : null
  if (systemInfo?.platform === 'devtools') return

  wx.onError((res) => {
    reportError({ message: '小程序全局错误', stack: res, type: 'wx.onError' })
  })
  wx.onUnhandledRejection((res) => {
    reportError({
      message: res && res.reason ? String(res.reason).slice(0, 200) : 'UnhandledRejection',
      stack: res && res.reason ? String(res.reason) : undefined,
      type: 'onUnhandledRejection',
    })
  })
  // #endif
}
