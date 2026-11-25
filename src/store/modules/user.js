/**
 * 用户 Store
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getToken, setToken, removeToken,
  getUserInfo, setUserInfo, removeUserInfo,
  getUserRole, setUserRole, removeUserRole,
  clearAuth
} from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref('')
  const userInfo = ref(null)
  const userRole = ref('') // student | volunteer | admin

  // 计算属性 - 基础
  const isLoggedIn = computed(() => !!token.value)
  // userInfo.userType 与 userRole 必须代表同一个当前身份。优先使用登录
  // 响应写入的 userType，兼容旧缓存时才回退到 userRole。
  const identityType = computed(() => {
    const infoType = String(userInfo.value?.userType || '').toLowerCase()
    if (infoType === 'student' || infoType === 'volunteer') return infoType
    const storedRole = String(userRole.value || '').toLowerCase()
    return storedRole === 'student' || storedRole === 'volunteer' ? storedRole : ''
  })
  const isStudent = computed(() => identityType.value === 'student')
  const isVolunteer = computed(() => identityType.value === 'volunteer')
  const hasBoundIdentity = computed(() => {
    return (isStudent.value || isVolunteer.value) && userInfo.value?.relatedId != null
  })
  // 管理员身份基于 userInfo.role 判断，不因切换模式而丢失
  const isAdmin = computed(() => {
    const role = userInfo.value?.role
    return role === 'SUPER_ADMIN' || role === 'NORMAL_ADMIN' || role === 'TEACHER'
  })

  // 计算属性 - 细粒度角色
  const isSuperAdmin = computed(() => userInfo.value?.role === 'SUPER_ADMIN')
  const isNormalAdmin = computed(() => userInfo.value?.role === 'NORMAL_ADMIN')
  const isTeacher = computed(() => userInfo.value?.role === 'TEACHER')
  const isCaptain = computed(() => userInfo.value?.isCaptain === true)

  // 计算属性 - 权限
  const canViewContacts = computed(() => {
    // 超级管理员、老师可以查看所有人联系方式
    if (isSuperAdmin.value || isTeacher.value) return true
    // 队长可以查看部分人员信息（仅姓名+学校）
    if (isCaptain.value) return true
    return false
  })

  const canAudit = computed(() => {
    return isSuperAdmin.value || isTeacher.value
  })

  const canManage = computed(() => {
    return isSuperAdmin.value
  })

  // 从本地存储初始化
  function initFromStorage() {
    token.value = getToken() || ''
    userInfo.value = getUserInfo()
    userRole.value = getUserRole() || ''
  }

  // 设置登录信息
  function setLoginInfo(data) {
    token.value = data.token
    userInfo.value = data.userInfo
    userRole.value = data.role || ''

    setToken(data.token)
    setUserInfo(data.userInfo)
    setUserRole(data.role || '')
  }

  // 切换微信账号的活动身份。昵称由后端按身份返回，禁止沿用上一身份的值。
  function setWechatIdentity(data, requestedRole) {
    const activeRole = data.userType === 'student' || data.userType === 'volunteer'
      ? data.userType
      : requestedRole
    const previous = userInfo.value || {}

    setLoginInfo({
      token: data.token || token.value,
      userInfo: {
        id: data.relatedId ?? null,
        relatedId: data.relatedId ?? null,
        openid: data.openid || previous.openid || '',
        nickname: data.nickname ?? '',
        avatar: data.avatarUrl ?? previous.avatar ?? '',
        userType: activeRole,
        name: data.name || '',
        phone: data.phone || '',
        isCaptain: data.isCaptain === true,
        highSchoolId: data.highSchoolId ?? null,
        highSchoolName: data.highSchoolName || '',
        universityId: data.universityId ?? null,
        universityName: data.universityName || '',
        wechatId: data.wechatId || '',
        grade: data.grade || '',
        major: data.major || '',
        status: data.status || '',
        targetHighSchoolId: data.targetHighSchoolId ?? null,
        targetHighSchoolName: data.targetHighSchoolName || ''
      },
      role: activeRole
    })
  }

  // 更新用户信息
  function updateUserInfo(info) {
    userInfo.value = { ...userInfo.value, ...info }
    setUserInfo(userInfo.value)
  }

  // 设置角色（管理员用，不清除数据）
  function setRole(role) {
    userRole.value = role
    setUserRole(role)
  }

  // 检查权限
  function hasPermission(perm) {
    // Granular teacher permissions were added after existing teacher accounts.
    // A missing value keeps legacy access; newly registered teachers receive an explicit all-false object.
    if (!userInfo.value?.permissions) return isTeacher.value
    try {
      const perms = typeof userInfo.value.permissions === 'string'
        ? JSON.parse(userInfo.value.permissions)
        : userInfo.value.permissions
      return perms[perm] === true
    } catch {
      return false
    }
  }

  // 清除角色数据（普通用户切换身份时调用，销毁当前角色信息）
  function clearRoleData() {
    userRole.value = ''
    userInfo.value = null
    removeUserRole()
    removeUserInfo()
  }

  // 退出登录
  function logout() {
    token.value = ''
    userInfo.value = null
    userRole.value = ''
    clearAuth()
  }

  return {
    token,
    userInfo,
    userRole,
    isLoggedIn,
    identityType,
    isStudent,
    isVolunteer,
    hasBoundIdentity,
    isAdmin,
    isSuperAdmin,
    isNormalAdmin,
    isTeacher,
    isCaptain,
    canViewContacts,
    canAudit,
    canManage,
    initFromStorage,
    setLoginInfo,
    setWechatIdentity,
    updateUserInfo,
    setRole,
    hasPermission,
    clearRoleData,
    logout
  }
})
