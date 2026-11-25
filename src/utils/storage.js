/**
 * 本地存储工具
 * 封装 uni.setStorageSync / uni.getStorageSync
 */

const PREFIX = 'zscx_'

export function setStorage(key, value) {
  try {
    uni.setStorageSync(`${PREFIX}${key}`, value)
  } catch (e) {
    console.error('存储失败:', key, e)
  }
}

export function getStorage(key, defaultValue = null) {
  try {
    const value = uni.getStorageSync(`${PREFIX}${key}`)
    // getStorageSync 对不存在的 key 返回空字符串
    // 用 hasOwnProperty 检查 key 是否真正存在
    if (value === '' || value === undefined || value === null) {
      // 进一步检查：是真正不存在还是存的就是空字符串
      const info = uni.getStorageInfoSync()
      const fullKey = `${PREFIX}${key}`
      if (!info.keys || !info.keys.includes(fullKey)) {
        return defaultValue
      }
    }
    return value
  } catch (e) {
    console.error('读取存储失败:', key, e)
    return defaultValue
  }
}

export function removeStorage(key) {
  try {
    uni.removeStorageSync(`${PREFIX}${key}`)
  } catch (e) {
    console.error('删除存储失败:', key, e)
  }
}

export function clearStorage() {
  try {
    uni.clearStorageSync()
  } catch (e) {
    console.error('清空存储失败:', e)
  }
}
