/**
 * 应用 Store
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getStorage, setStorage } from '@/utils/storage'

export const useAppStore = defineStore('app', () => {
  // 状态
  const systemInfo = ref(null)
  const statusBarHeight = ref(0)
  const navBarHeight = ref(44)
  const menuButtonWidth = ref(0)  // 微信胶囊按钮宽度(右上角), 用于自定义导航栏避让
  const selectedHighSchoolId = ref(null)
  const selectedHighSchoolName = ref('')

  // 计算属性
  const isIOS = computed(() => {
    return systemInfo.value?.platform === 'ios'
  })

  const totalNavHeight = computed(() => {
    return statusBarHeight.value + navBarHeight.value
  })

  // 初始化系统信息
  function initSystemInfo() {
    try {
      // 优先新 API，避免 getSystemInfoSync 弃用警告；老版本基础库自动降级
      const info = typeof uni.getWindowInfo === 'function' ? uni.getWindowInfo() : uni.getSystemInfoSync()
      systemInfo.value = info
      statusBarHeight.value = info.statusBarHeight || 0
    } catch (e) {
      console.error('获取系统信息失败:', e)
    }
    // 读取微信胶囊按钮尺寸(右上角), 自定义导航栏需避让, 否则按钮会被盖住点不到
    try {
      const menu = uni.getMenuButtonBoundingClientRect?.()
      if (menu && menu.width) {
        menuButtonWidth.value = menu.width
        // 部分机型胶囊宽度较大(如折叠屏/平板), 按实际宽度+右侧留白计算
        navBarHeight.value = Math.max(navBarHeight.value, menu.height || 32)
      }
    } catch (e) {
      console.error('获取胶囊按钮信息失败:', e)
    }
  }

  // 设置选中的高中
  function setSelectedHighSchool(id, name) {
    selectedHighSchoolId.value = id
    selectedHighSchoolName.value = name
    setStorage('selectedHighSchool', { id, name })
  }

  // 从缓存恢复选中的高中
  function restoreSelectedHighSchool() {
    const cached = getStorage('selectedHighSchool')
    if (cached) {
      selectedHighSchoolId.value = cached.id
      selectedHighSchoolName.value = cached.name
    }
  }

  return {
    systemInfo,
    statusBarHeight,
    navBarHeight,
    menuButtonWidth,
    selectedHighSchoolId,
    selectedHighSchoolName,
    isIOS,
    totalNavHeight,
    initSystemInfo,
    setSelectedHighSchool,
    restoreSelectedHighSchool
  }
})
