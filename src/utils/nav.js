import { useUserStore } from '@/store/modules/user'

// 微信开发者工具偶发 reLaunch:fail timeout（页面栈销毁+重建超时），
// 失败后自动重试一次；真机上通常不会触发。
export function safeReLaunch(url, retry = 1) {
  uni.reLaunch({
    url,
    fail: (err) => {
      console.warn('reLaunch 失败，重试:', url, err)
      if (retry > 0) {
        setTimeout(() => safeReLaunch(url, retry - 1), 400)
      }
    }
  })
}

// 已登录时按角色直达对应身份首页，未登录时回身份选择页
export function goRoleHome() {
  const userStore = useUserStore()
  if (!userStore.isLoggedIn) {
    safeReLaunch('/pages/index/index')
    return
  }
  const role = userStore.userRole
  const userRole = userStore.userInfo?.role
  // 老师 → 跳转老师首页
  if (userRole === 'TEACHER' || role === 'teacher') {
    safeReLaunch('/pages/teacher/home')
    return
  }
  // 管理员 → 管理后台（项目无 tabBar，switchTab 会静默失败导致卡启动页）
  if (role === 'admin' || userRole === 'SUPER_ADMIN' || userRole === 'NORMAL_ADMIN') {
    safeReLaunch('/pages/admin/dashboard')
    return
  }
  // 学生
  if (role === 'student') {
    safeReLaunch('/pages/student/home')
    return
  }
  // 大学生
  safeReLaunch('/pages/volunteer/home')
}
