<template>
  <view class="teacher-login">
    <!-- 返回身份选择页入口（本页常由 reLaunch 进入、页面栈无上一页，必须有显式返回入口） -->
    <view class="teacher-login__back" @tap="goHome">
      <text class="teacher-login__back-icon">‹</text>
      <text class="teacher-login__back-text">返回身份选择</text>
    </view>
    <!-- 顶部装饰 -->
    <view class="teacher-login__hero">
      <view class="teacher-login__hero-bg"></view>
      <view class="teacher-login__hero-content">
        <view class="teacher-login__logo-wrap">
          <view class="teacher-login__logo">
            <text class="teacher-login__logo-icon">👨‍🏫</text>
          </view>
        </view>
        <text class="teacher-login__title">大学老师端</text>
        <text class="teacher-login__subtitle">招生宣传管理平台</text>
      </view>
    </view>

    <!-- 内容区域 -->
    <view class="teacher-login__main">
      <!-- 登录模式 -->
      <view class="teacher-login__card" v-if="!showRegister">
        <text class="teacher-login__card-title">微信授权登录</text>
        <text class="teacher-login__card-desc">使用微信授权验证老师身份，一键登录</text>

        <button
          class="teacher-login__btn"
          @tap="handleLogin"
          :loading="loading"
          hover-class="teacher-login__btn--hover"
        >
          <text class="teacher-login__btn-text">微信授权登录</text>
        </button>

        <view class="teacher-login__switch" @tap="showRegister = true">
          <text class="teacher-login__switch-text">首次使用？前往注册</text>
        </view>
      </view>

      <!-- 注册模式 -->
      <view class="teacher-login__card" v-else>
        <text class="teacher-login__card-title">老师注册</text>
        <text class="teacher-login__card-desc">首次使用请先注册您的老师身份</text>

        <view class="teacher-login__field">
          <text class="teacher-login__label">真实姓名</text>
          <input class="teacher-login__input" v-model="regForm.name" placeholder="请输入您的真实姓名" />
        </view>

        <view class="teacher-login__field">
          <text class="teacher-login__label">联系电话</text>
          <input class="teacher-login__input" v-model="regForm.phone" type="number" placeholder="请输入联系电话（选填）" />
        </view>

        <view class="teacher-login__field">
          <text class="teacher-login__label">所属大学</text>
          <picker :range="universityOptions" range-key="name" :value="uniIndex" @change="onUniChange">
            <view class="teacher-login__picker">
              <text :class="regForm.universityId ? 'teacher-login__picker-text' : 'teacher-login__picker-placeholder'">
                {{ regForm.universityId ? selectedUniName : '请选择所属大学' }}
              </text>
              <text class="teacher-login__picker-arrow">›</text>
            </view>
          </picker>
        </view>

        <button
          class="teacher-login__btn"
          @tap="handleRegister"
          :loading="loading"
          hover-class="teacher-login__btn--hover"
        >
          <text class="teacher-login__btn-text">注册并登录</text>
        </button>

        <view class="teacher-login__switch" @tap="showRegister = false">
          <text class="teacher-login__switch-text">已有账号？返回登录</text>
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
import * as authApi from '@/api/auth'
import * as universityApi from '@/api/university'

const userStore = useUserStore()

const loading = ref(false)
const showRegister = ref(false)

// 返回身份选择页：本页常由 reLaunch 进入（页面栈无上一页，navigateBack 无效），必须显式跳转
function goHome() {
  uni.reLaunch({ url: '/pages/index/index' })
}

const regForm = reactive({
  name: '',
  phone: '',
  universityId: null
})

const universityOptions = ref([])
const uniIndex = ref(-1)
const selectedUniName = ref('')

onLoad(() => {
  loadUniversities()
})

async function loadUniversities() {
  try {
    const res = await universityApi.getUniversityList()
    universityOptions.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (e) {
    console.error('加载大学列表失败:', e)
  }
}

function onUniChange(e) {
  const index = e.detail.value
  uniIndex.value = index
  const uni = universityOptions.value[index]
  if (uni) {
    regForm.universityId = uni.id
    selectedUniName.value = uni.name
  }
}

async function handleLogin() {
  loading.value = true
  try {
    const code = await wxLogin()
    const result = await authApi.teacherLogin(code)
    saveAndRedirect(result)
  } catch (e) {
    console.error('登录失败:', e)
    uni.showToast({ title: '登录失败，请确认已注册或返回首页注册', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!regForm.name.trim()) {
    uni.showToast({ title: '请输入姓名', icon: 'none' }); return
  }
  if (!regForm.universityId) {
    uni.showToast({ title: '请选择所属大学', icon: 'none' }); return
  }

  loading.value = true
  try {
    const code = await wxLogin()
    const result = await authApi.teacherRegister({
      code,
      name: regForm.name.trim(),
      phone: regForm.phone.trim(),
      universityId: regForm.universityId,
      targetHighSchoolId: 1
    })
    // 后端设计：注册不签发可用 token（status=0 待超管启用，防任意人自封大学老师）
    if (result.token) {
      saveAndRedirect(result)
    } else {
      uni.showModal({
        title: '注册成功',
        content: '您的注册申请已提交，请等待系统管理员审核启用后登录',
        showCancel: false,
        confirmText: '好的',
        success: () => {
          uni.reLaunch({ url: '/pages/index/index' })
        }
      })
    }
  } catch (e) {
    console.error('注册失败:', e)
    uni.showToast({ title: '注册失败: ' + (e.message || '请重试'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

function saveAndRedirect(result) {
  userStore.setLoginInfo({
    token: result.token,
    userInfo: {
      id: result.relatedId,
      openid: result.openid,
      nickname: result.nickname || result.name || '老师',
      name: result.name || result.nickname || '老师',
      avatar: result.avatarUrl || '',
      role: result.role || 'TEACHER',
      universityId: result.universityId,
      universityName: result.universityName,
      permissions: result.permissions
    },
    role: 'teacher'
  })

  uni.showToast({ title: '登录成功', icon: 'success' })
  setTimeout(() => {
    uni.reLaunch({ url: '/pages/teacher/home' })
  }, 500)
}
</script>

<style lang="scss" scoped>
.teacher-login {
  min-height: 100vh;
  background: #f0f2f5;

  &__back {
    position: absolute;
    top: 44rpx;
    left: 24rpx;
    z-index: 20;
    display: flex;
    align-items: center;
    gap: 6rpx;
    padding: 8rpx 16rpx;
    background: rgba(255, 255, 255, 0.9);
    border-radius: 30rpx;

    &-icon {
      font-size: 36rpx;
      color: #334155;
      font-weight: 300;
      line-height: 1;
    }

    &-text {
      font-size: 24rpx;
      color: #334155;
    }
  }

  &__hero {
    position: relative;
    height: 380rpx;
    overflow: hidden;

    &-bg {
      position: absolute;
      top: 0; left: 0; right: 0; bottom: 0;
      background: linear-gradient(160deg, #3A7BAA 0%, #4A8BC4 40%, #4A8BC4 100%);
      border-radius: 0 0 60rpx 60rpx;

      &::after {
        content: '';
        position: absolute;
        top: 0; left: 0; right: 0; bottom: 0;
        background: radial-gradient(ellipse at 30% 20%, rgba(255,255,255,0.1) 0%, transparent 60%);
      }
    }

    &-content {
      position: relative;
      z-index: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      padding-top: 40rpx;
    }
  }

  &__logo-wrap { margin-bottom: 20rpx; }

  &__logo {
    width: 110rpx; height: 110rpx;
    background: rgba(255,255,255,0.15);
    border-radius: 28rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    backdrop-filter: blur(20px);
    border: 2rpx solid rgba(255,255,255,0.25);

    &-icon { font-size: 58rpx; }
  }

  &__title {
    font-size: 38rpx;
    font-weight: 700;
    color: #fff;
    letter-spacing: 6rpx;
  }

  &__subtitle {
    font-size: 24rpx;
    color: rgba(255,255,255,0.75);
    margin-top: 6rpx;
  }

  &__main {
    padding: 0 40rpx;
    margin-top: -40rpx;
    position: relative;
    z-index: 2;
  }

  &__card {
    background: #fff;
    border-radius: 24rpx;
    padding: 44rpx 36rpx;
    box-shadow: 0 8rpx 32rpx rgba(0,0,0,0.08);
  }

  &__card-title {
    font-size: 34rpx;
    font-weight: 700;
    color: #1a202c;
    display: block;
    text-align: center;
  }

  &__card-desc {
    font-size: 24rpx;
    color: #718096;
    display: block;
    text-align: center;
    margin-top: 8rpx;
    margin-bottom: 36rpx;
  }

  &__field { margin-bottom: 20rpx; }

  &__label {
    font-size: 26rpx;
    font-weight: 600;
    color: #4a5568;
    display: block;
    margin-bottom: 10rpx;
  }

  &__input {
    width: 100%;
    height: 88rpx;
    background: #f7fafc;
    border: 2rpx solid #e2e8f0;
    border-radius: 14rpx;
    padding: 0 20rpx;
    font-size: 28rpx;
    color: #1a202c;

    &:focus {
      border-color: #4A8BC4;
      background: #fff;
      box-shadow: 0 0 0 4rpx rgba(43,108,176,0.1);
    }
  }

  &__picker {
    width: 100%;
    height: 88rpx;
    background: #f7fafc;
    border: 2rpx solid #e2e8f0;
    border-radius: 14rpx;
    padding: 0 20rpx;
    display: flex;
    align-items: center;
    justify-content: space-between;

    &-text { font-size: 28rpx; color: #1a202c; }
    &-placeholder { font-size: 28rpx; color: #a0aec0; }
    &-arrow { font-size: 32rpx; color: #9ca3af; }
  }

  &__btn {
    width: 100%;
    height: 96rpx;
    background: linear-gradient(135deg, #3A7BAA, #4A8BC4);
    border-radius: 16rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 8rpx 24rpx rgba(26,54,93,0.3);
    border: none;

    &--hover { transform: scale(0.98); opacity: 0.9; }
    &::after { display: none; }

    &-text {
      font-size: 32rpx;
      color: #fff;
      font-weight: 600;
      letter-spacing: 4rpx;
    }
  }

  &__switch {
    margin-top: 28rpx;
    text-align: center;

    &-text {
      font-size: 26rpx;
      color: #4A8BC4;
    }
  }
}
</style>
