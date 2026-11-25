<template>
  <view class="login">
    <!-- 顶部装饰区域 -->
    <view class="login__hero">
      <view class="login__hero-bg"></view>
      <view class="login__hero-content">
        <view class="login__logo-wrap">
          <view class="login__logo">
            <text class="login__logo-icon">🎓</text>
          </view>
        </view>
        <text class="login__app-name">招生宣传系统</text>
        <text class="login__app-desc">连接梦想与未来</text>
      </view>
    </view>

    <!-- 主内容区域 -->
    <view class="login__main">
      <!-- 四角色选择卡片 - 2x2 网格，全部可见 -->
      <view class="login__roles">
        <view
          v-for="role in roles"
          :key="role.key"
          :class="['role-card', loginRole === role.key && 'role-card--active']"
          @tap="switchRole(role.key)"
        >
          <text class="role-card__icon">{{ role.icon }}</text>
          <text class="role-card__title">{{ role.title }}</text>
          <text class="role-card__desc">{{ role.desc }}</text>
        </view>
      </view>

      <!-- ==================== 高中生 / 大学生：微信授权登录 ==================== -->
      <view class="login__form" v-if="loginRole === 'student' || loginRole === 'volunteer'">
        <view class="login__form-tag">
          <text class="login__form-tag-text">{{ loginRole === 'student' ? '🎓 高中生' : '🎯 大学生' }}</text>
        </view>

        <!-- 微信一键登录（已注册用户直接登录，无需再选头像昵称；首次使用引导完善资料） -->
        <view class="login__wx-center">
          <text class="login__wx-title">微信授权登录</text>
          <text class="login__wx-desc">使用微信一键登录，无需注册</text>
          <button class="login__submit login__submit--wx" @tap="handleWxLogin" hover-class="login__submit--hover">
            <text class="login__submit-text">微信授权登录</text>
          </button>
        </view>
      </view>

      <!-- ==================== 管理员登录 ==================== -->
      <view class="login__form" v-else-if="loginRole === 'admin'">
        <view class="login__form-tag">
          <text class="login__form-tag-text">🔐 管理员</text>
        </view>

        <view class="login__section">
          <view class="login__field">
            <view class="login__input-group">
              <text class="login__input-label">账号</text>
              <input class="login__input" v-model="adminForm.username" placeholder="请输入管理员账号" />
            </view>
          </view>
          <view class="login__field">
            <view class="login__input-group">
              <text class="login__input-label">密码</text>
              <input class="login__input" v-model="adminForm.password" type="password" placeholder="请输入密码" />
            </view>
          </view>
        </view>

        <button class="login__submit login__submit--admin" @tap="handleAdminLogin" hover-class="login__submit--hover">
          <text class="login__submit-text">登 录</text>
        </button>
      </view>

      <!-- ==================== 老师登录 ==================== -->
      <view class="login__form" v-else-if="loginRole === 'teacher'">
        <view class="login__form-tag">
          <text class="login__form-tag-text">👨‍🏫 大学老师</text>
        </view>

        <!-- 登录模式 -->
        <view v-if="teacherMode === 'login'">
          <text class="login__wx-title">微信授权登录</text>
          <text class="login__wx-desc">已注册老师直接微信授权登录</text>

          <button class="login__submit login__submit--teacher" @tap="handleTeacherLogin" :loading="teacherLoading" hover-class="login__submit--hover">
            <text class="login__submit-text">微信授权登录</text>
          </button>

          <view class="login__switch-link" @tap="teacherMode = 'register'">
            <text class="login__switch-link-text">首次使用？点击注册老师账号</text>
          </view>
        </view>

        <!-- 注册模式 -->
        <view v-else>
          <text class="login__auth-title">老师注册</text>
          <text class="login__auth-desc">填写信息完成注册</text>

          <view class="login__section">
            <view class="login__field">
              <view class="login__input-group">
                <text class="login__input-label">真实姓名</text>
                <input class="login__input" v-model="teacherRegForm.name" placeholder="请输入真实姓名" />
              </view>
            </view>
            <view class="login__field">
              <view class="login__input-group">
                <text class="login__input-label">联系电话</text>
                <input class="login__input" v-model="teacherRegForm.phone" type="number" placeholder="请输入联系电话（选填）" />
              </view>
            </view>
            <view class="login__field">
              <view class="login__input-group">
                <text class="login__input-label">所属大学</text>
                <picker :range="teacherUniOptions" range-key="name" :value="teacherUniIndex" @change="onTeacherUniChange">
                  <view class="login__picker">
                    <text :class="teacherRegForm.universityId ? 'login__picker-text' : 'login__picker-placeholder'">
                      {{ teacherRegForm.universityId ? teacherSelectedUniName : '请选择所属大学' }}
                    </text>
                    <text class="login__picker-arrow">›</text>
                  </view>
                </picker>
              </view>
            </view>
          </view>

          <button class="login__submit login__submit--teacher" @tap="handleTeacherRegister" :loading="teacherLoading" hover-class="login__submit--hover">
            <text class="login__submit-text">注册并登录</text>
          </button>

          <view class="login__switch-link" @tap="teacherMode = 'login'">
            <text class="login__switch-link-text">已有账号？返回登录</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/modules/user'
import { wxLogin } from '@/utils/auth'
import { safeReLaunch } from '@/utils/nav'
import * as authApi from '@/api/auth'
import * as universityApi from '@/api/university'

const userStore = useUserStore()

function generateNickname(prefix) {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
  let suffix = ''
  for (let i = 0; i < 4; i++) {
    suffix += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return prefix + '_' + suffix
}

const loginRole = ref('student')

// 四种角色（2x2网格展示）
const roles = [
  { key: 'student', icon: '🎓', title: '高中生', desc: '咨询大学招生问题' },
  { key: 'volunteer', icon: '🎯', title: '大学生', desc: '回母校宣传答疑' },
  { key: 'admin', icon: '🔐', title: '管理员', desc: '系统后台管理' },
  { key: 'teacher', icon: '👨‍🏫', title: '大学老师', desc: '管理本校宣传' }
]

const adminForm = reactive({ username: '', password: '' })

const teacherMode = ref('login')
const teacherLoading = ref(false)
const teacherRegForm = reactive({ name: '', phone: '', universityId: null })
const teacherUniOptions = ref([])
const teacherUniIndex = ref(-1)
const teacherSelectedUniName = ref('')

onLoad((options) => {
  const validRoles = ['student', 'volunteer', 'admin', 'teacher']
  if (options.role && validRoles.includes(options.role)) {
    loginRole.value = options.role
  }
  loadTeacherUniversities()
})

function switchRole(role) {
  loginRole.value = role
  if (role === 'teacher' && teacherUniOptions.value.length === 0) {
    loadTeacherUniversities()
  }
}

async function loadTeacherUniversities() {
  try {
    const res = await universityApi.getUniversityList()
    teacherUniOptions.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (e) { /* ignore */ }
}

function onTeacherUniChange(e) {
  const index = e.detail.value
  teacherUniIndex.value = index
  const uni = teacherUniOptions.value[index]
  if (uni) {
    teacherRegForm.universityId = uni.id
    teacherSelectedUniName.value = uni.name
  }
}

// ==================== 老师登录/注册 ====================
async function handleTeacherLogin() {
  teacherLoading.value = true
  try {
    const code = await wxLogin()
    const result = await authApi.teacherLogin(code)
    teacherSaveAndRedirect(result)
  } catch (e) {
    console.error('老师登录失败:', e)
    uni.showToast({ title: '登录失败，请确认已注册', icon: 'none' })
  } finally { teacherLoading.value = false }
}

async function handleTeacherRegister() {
  if (!teacherRegForm.name.trim()) { uni.showToast({ title: '请输入姓名', icon: 'none' }); return }
  if (!teacherRegForm.universityId) { uni.showToast({ title: '请选择所属大学', icon: 'none' }); return }

  teacherLoading.value = true
  try {
    const code = await wxLogin()
    const result = await authApi.teacherRegister({
      code, name: teacherRegForm.name.trim(), phone: teacherRegForm.phone.trim(),
      universityId: teacherRegForm.universityId, targetHighSchoolId: 1
    })
    teacherSaveAndRedirect(result)
  } catch (e) {
    console.error('老师注册失败:', e)
    uni.showToast({ title: '注册失败: ' + (e.message || '请重试'), icon: 'none' })
  } finally { teacherLoading.value = false }
}

function teacherSaveAndRedirect(result) {
  // 注册后账号待管理员审核启用，不签发 token
  if (!result.token) {
    uni.showToast({ title: '注册成功，请等待管理员审核启用后登录', icon: 'none', duration: 2500 })
    // 回身份选择页（而非困在老师登录页），可随时从"大学老师"入口重新登录
    setTimeout(() => { uni.reLaunch({ url: '/pages/index/index' }) }, 1200)
    return
  }
  userStore.setLoginInfo({
    token: result.token,
    userInfo: {
      id: result.relatedId, openid: result.openid,
      nickname: result.nickname || result.name || '老师',
      name: result.name || result.nickname || '老师',
      avatar: result.avatarUrl || '', role: result.role || 'TEACHER',
      universityId: result.universityId, universityName: result.universityName,
      permissions: result.permissions
    },
    role: 'teacher'
  })
  uni.showToast({ title: '登录成功', icon: 'success' })
  setTimeout(() => { uni.reLaunch({ url: '/pages/teacher/home' }) }, 500)
}

// ==================== 学生/志愿者微信登录 ====================
async function handleWxLogin() {
  const role = loginRole.value
  const prefix = role === 'student' ? '同学' : '志愿者'

  uni.showLoading({ title: '登录中...' })
  try {
    const code = await wxLogin()
    // 一键登录：不要求用户填写头像昵称。
    // 已注册用户：后端返回已保存的头像/昵称，直接登录；首次使用：引导去完善资料。
    const loginResult = await authApi.wxLogin(code, '', '', role)
    const userType = loginResult.userType || 'new_user'
    const normalizedUserType = userType === 'high_school_student' ? 'student' : userType
    const resolvedRole = ['student', 'volunteer'].includes(normalizedUserType)
      ? normalizedUserType
      : role
    const identityNickname = loginResult.nickname || generateNickname(prefix)

    userStore.setWechatIdentity({
      ...loginResult,
      nickname: identityNickname,
      avatarUrl: loginResult.avatarUrl || ''
    }, resolvedRole)
    uni.hideLoading()

    // 根据后端返回的 userType 判断是否需要引导填写资料
    if (userType === 'new_user') {
      uni.showModal({
        title: '欢迎加入',
        content: role === 'student' ? '请先完善学生信息' : '请先填写志愿者报名信息',
        showCancel: false,
        success: () => {
          const url = role === 'student'
            ? '/pages/common/edit-profile?register=1'
            : '/pages/volunteer/register'
          uni.redirectTo({ url })
        }
      })
    } else {
      uni.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => {
        if (resolvedRole === 'student') { uni.reLaunch({ url: '/pages/student/home' }) }
        else { uni.reLaunch({ url: '/pages/volunteer/home' }) }
      }, 500)
    }
  } catch (e) {
    uni.hideLoading()
    console.error('微信登录失败:', e)
    userStore.logout()
    // 用弹窗展示具体失败原因（如"账号已被禁用/等待审核"），toast 一闪而过容易错过
    uni.showModal({
      title: '登录失败',
      content: e.message || '登录失败，请重试',
      showCancel: false,
      confirmText: '知道了'
    })
  }
}

// ==================== 管理员登录 ====================
const adminLoggingIn = ref(false)
async function handleAdminLogin() {
  if (adminLoggingIn.value) return
  if (!adminForm.username) { uni.showToast({ title: '请输入账号', icon: 'none' }); return }
  if (!adminForm.password) { uni.showToast({ title: '请输入密码', icon: 'none' }); return }

  adminLoggingIn.value = true
  uni.showLoading({ title: '登录中...' })
  try {
    const result = await authApi.adminLogin({ username: adminForm.username, password: adminForm.password })
    uni.hideLoading()
    userStore.setLoginInfo({
      token: result.token,
      userInfo: {
        id: result.relatedId, nickname: result.nickname || '管理员', name: result.nickname || '管理员',
        avatar: result.avatarUrl || '', role: result.role, permissions: result.permissions,
        universityId: result.universityId, universityName: result.universityName
      },
      role: 'admin'
    })
    uni.showToast({ title: '登录成功', icon: 'success' })
    // 注意: 项目未配置 tabBar, uni.switchTab 跳转非 tabBar 页面会静默失败,
    // 导致管理员登录后卡在启动页。必须用 reLaunch 直达管理后台。
    setTimeout(() => { safeReLaunch('/pages/admin/dashboard') }, 500)
  } catch (e) {
    uni.hideLoading()
    console.error('管理员登录失败:', e)
    // 用弹窗展示具体失败原因（如"账号已被禁用"），toast 一闪而过容易错过
    uni.showModal({
      title: '登录失败',
      content: e.message || '请检查账号密码后重试',
      showCancel: false,
      confirmText: '知道了'
    })
  } finally {
    adminLoggingIn.value = false
  }
}
</script>

<style lang="scss" scoped>
.login {
  min-height: 100vh;
  background: #F2F6FB;
  display: flex;
  flex-direction: column;

  // 顶部 Hero
  &__hero {
    position: relative;
    height: 340rpx;
    overflow: hidden;

    &-bg {
      position: absolute; top: 0; left: 0; right: 0; bottom: 0;
      background: linear-gradient(160deg, #8FC1EC 0%, #5B9BD5 55%, #4A8BC4 100%);
      border-radius: 0 0 60rpx 60rpx;
      box-shadow: 0 16rpx 40rpx rgba(91, 155, 213, 0.3);
    }

    &-content {
      position: relative; z-index: 1;
      display: flex; flex-direction: column; align-items: center;
      justify-content: center; height: 100%; padding-top: 40rpx;
    }
  }

  &__logo-wrap { margin-bottom: 16rpx; }

  &__logo {
    width: 100rpx; height: 100rpx;
    background: rgba(255,255,255,0.2); border-radius: 28rpx;
    display: flex; align-items: center; justify-content: center;
    backdrop-filter: blur(20px); border: 2rpx solid rgba(255,255,255,0.3);

    &-icon { font-size: 54rpx; }
  }

  &__app-name { font-size: 38rpx; font-weight: 700; color: #fff; letter-spacing: 4rpx; }
  &__app-desc { font-size: 24rpx; color: rgba(255,255,255,0.8); margin-top: 6rpx; letter-spacing: 8rpx; }

  // 主区域
  &__main {
    flex: 1; padding: 0 32rpx;
    margin-top: -30rpx; position: relative; z-index: 2;
  }

  // ======== 四角色 2x2 网格选择 ========
  &__roles {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16rpx;
    margin-bottom: 28rpx;
  }

  // 表单容器
  &__form {
    background: #fff; border-radius: 24rpx;
    padding: 36rpx 32rpx; margin-bottom: 40rpx;
    box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.06);
  }

  &__form-tag {
    display: flex; justify-content: center; margin-bottom: 28rpx;

    &-text {
      font-size: 26rpx; font-weight: 600; color: #5B9BD5;
      background: rgba(91,155,213,0.1); padding: 10rpx 28rpx; border-radius: 999rpx;
    }
  }

  &__wx-title { font-size: 32rpx; font-weight: 600; color: #333; display: block; text-align: center; margin-bottom: 8rpx; }
  &__wx-desc { font-size: 24rpx; color: #999; display: block; text-align: center; margin-bottom: 36rpx; }

  &__wx-center { display: flex; flex-direction: column; align-items: center; }

  // 授权
  &__auth-title { font-size: 28rpx; font-weight: 600; color: #333; margin-bottom: 24rpx; }
  &__auth-desc { font-size: 24rpx; color: #999; margin-bottom: 24rpx; }

  // 表单通用
  &__section { margin-bottom: 28rpx; &:last-of-type { margin-bottom: 0; } }
  &__field { margin-bottom: 20rpx; &:last-child { margin-bottom: 0; } }
  &__input-group { display: flex; flex-direction: column; }
  &__input-label { font-size: 24rpx; color: #999; margin-bottom: 10rpx; padding-left: 4rpx; }
  &__input {
    height: 88rpx; background: #f8f9fc; border: 2rpx solid #eef0f6; border-radius: 16rpx;
    padding: 0 24rpx; font-size: 28rpx; color: #333;
    &:focus { background: #fff; border-color: #5B9BD5; box-shadow: 0 0 0 4rpx rgba(91,155,213,0.12); }
  }

  // 选择器
  &__picker {
    width: 100%; height: 88rpx; background: #f8f9fc; border: 2rpx solid #eef0f6;
    border-radius: 16rpx; padding: 0 24rpx; display: flex; align-items: center; justify-content: space-between;
    &-text { font-size: 28rpx; color: #333; }
    &-placeholder { font-size: 28rpx; color: #999; }
    &-arrow { font-size: 32rpx; color: #9ca3af; }
  }

  // 切换链接
  &__switch-link { text-align: center; margin-top: 28rpx;
    &-text { font-size: 26rpx; color: #5B9BD5; }
  }

  // 按钮
  &__submit {
    width: 100%; height: 96rpx; border-radius: 16rpx;
    display: flex; align-items: center; justify-content: center;
    margin-top: 16rpx; border: none;
    transition: all 0.3s ease;

    &--wx   { background: linear-gradient(135deg, #07c160, #06ad56); box-shadow: 0 8rpx 24rpx rgba(7,193,96,0.35); }
    &--admin { background: linear-gradient(135deg, #475569, #334155); box-shadow: 0 8rpx 24rpx rgba(51,65,85,0.35); }
    &--teacher { background: linear-gradient(135deg, #1e3a5f, #2d5986); box-shadow: 0 8rpx 24rpx rgba(30,58,95,0.35); }
    &--hover { transform: scale(0.98); }
    &::after { display: none; }

    &-text { font-size: 32rpx; color: #fff; font-weight: 600; letter-spacing: 6rpx; }
  }
}

// ======== 角色选择卡片 ========
.role-card {
  background: #fff; border-radius: 20rpx; padding: 28rpx 20rpx;
  display: flex; flex-direction: column; align-items: center;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.05);
  border: 2rpx solid transparent;
  transition: all 0.2s;

  &--active {
    border-color: #5B9BD5;
    background: linear-gradient(135deg, #f5f3ff, #ede9fe);
    box-shadow: 0 4rpx 20rpx rgba(91,155,213,0.22);
  }

  &__icon {
    font-size: 52rpx; margin-bottom: 10rpx;
  }

  &__title {
    font-size: 28rpx; font-weight: 700; color: #2d3748; margin-bottom: 4rpx;
  }

  &__desc {
    font-size: 20rpx; color: #a0aec0;
  }
}
</style>
