<template>
  <view v-if="!isLoggedIn" class="splash">
    <!-- 顶部渐变区域 -->
    <view class="splash__top">
      <view class="splash__top-bg"></view>

      <!-- 装饰元素 -->
      <view class="splash__deco splash__deco--1"></view>
      <view class="splash__deco splash__deco--2"></view>
      <view class="splash__deco splash__deco--3"></view>

      <view class="splash__brand">
        <view class="splash__logo">
          <view class="splash__logo-inner">
            <text class="splash__logo-icon">🎓</text>
          </view>
          <view class="splash__logo-shadow"></view>
        </view>
        <view class="splash__text">
          <text class="splash__title">招生宣传系统</text>
          <view class="splash__divider"></view>
          <text class="splash__subtitle">连接梦想与未来</text>
        </view>
      </view>
    </view>

    <!-- 底部卡片区域 -->
    <view class="splash__bottom">
      <view class="splash__welcome">
        <text class="splash__welcome-text">请选择你的身份</text>
      </view>

      <!-- 四角色 2×2 网格 -->
      <view class="splash__cards">
        <view class="splash__card" @tap="enterAsStudent" hover-class="splash__card--hover">
          <view class="splash__card-icon-box splash__card-icon-box--student">
            <text class="splash__card-icon">🎓</text>
          </view>
          <text class="splash__card-title">高中生</text>
          <text class="splash__card-desc">探索高校 · 咨询学长学姐</text>
        </view>

        <view class="splash__card" @tap="enterAsVolunteer" hover-class="splash__card--hover">
          <view class="splash__card-icon-box splash__card-icon-box--volunteer">
            <text class="splash__card-icon">🎯</text>
          </view>
          <text class="splash__card-title">大学生</text>
          <text class="splash__card-desc">志愿答疑 · 分享大学生活</text>
        </view>

        <view class="splash__card" @tap="enterAsAdmin" hover-class="splash__card--hover">
          <view class="splash__card-icon-box splash__card-icon-box--admin">
            <text class="splash__card-icon">🔐</text>
          </view>
          <text class="splash__card-title">管理员</text>
          <text class="splash__card-desc">系统管理 · 数据审核</text>
        </view>

        <view class="splash__card" @tap="enterAsTeacher" hover-class="splash__card--hover">
          <view class="splash__card-icon-box splash__card-icon-box--teacher">
            <text class="splash__card-icon">👨‍🏫</text>
          </view>
          <text class="splash__card-title">大学老师</text>
          <text class="splash__card-desc">管理本校 · 编辑宣传资料</text>
        </view>
      </view>

      <!-- 底部提示 -->
      <view class="splash__footer">
        <view class="splash__footer-line"></view>
        <text class="splash__footer-text">招生宣传平台 v1.0</text>
        <view class="splash__footer-line"></view>
      </view>
    </view>
  </view>

  <!-- 已登录跳转期间占位，避免闪现身份选择界面 -->
  <view v-else class="splash-loading">
    <view class="splash-loading__logo">
      <text class="splash-loading__logo-text">🎓</text>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/store/modules/user'
import { goRoleHome } from '@/utils/nav'

const userStore = useUserStore()

const isLoggedIn = computed(() => userStore.isLoggedIn)

// 已登录时同步跳转到对应身份首页，页面不渲染身份选择界面
if (isLoggedIn.value) {
  goRoleHome()
}

function enterAsStudent() {
  uni.navigateTo({ url: '/pages/common/login?role=student' })
}

function enterAsVolunteer() {
  uni.navigateTo({ url: '/pages/common/login?role=volunteer' })
}

function enterAsAdmin() {
  uni.navigateTo({ url: '/pages/common/login?role=admin' })
}

function enterAsTeacher() {
  uni.navigateTo({ url: '/pages/common/login?role=teacher' })
}
</script>

<style lang="scss" scoped>
.splash {
  min-height: 100vh;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  position: relative;

  // 顶部区域
  &__top {
    position: relative;
    height: 520rpx;
    overflow: hidden;

    &-bg {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 0 0 80rpx 80rpx;
    }
  }

  // 装饰元素
  &__deco {
    position: absolute;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.1);

    &--1 {
      width: 200rpx;
      height: 200rpx;
      top: -60rpx;
      left: -40rpx;
    }

    &--2 {
      width: 300rpx;
      height: 300rpx;
      top: 80rpx;
      right: -80rpx;
    }

    &--3 {
      width: 120rpx;
      height: 120rpx;
      bottom: 40rpx;
      left: 200rpx;
    }
  }

  // 品牌区域
  &__brand {
    position: relative;
    z-index: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    padding-top: 60rpx;
  }

  &__logo {
    position: relative;
    margin-bottom: 32rpx;

    &-inner {
      width: 140rpx;
      height: 140rpx;
      background: rgba(255, 255, 255, 0.25);
      border-radius: 36rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      backdrop-filter: blur(20px);
      border: 2rpx solid rgba(255, 255, 255, 0.4);
      position: relative;
      z-index: 1;
    }

    &-shadow {
      position: absolute;
      bottom: -10rpx;
      left: 50%;
      transform: translateX(-50%);
      width: 100rpx;
      height: 20rpx;
      background: rgba(0, 0, 0, 0.15);
      border-radius: 50%;
      filter: blur(10rpx);
    }

    &-icon {
      font-size: 72rpx;
    }
  }

  &__text {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  &__title {
    font-size: 44rpx;
    font-weight: 700;
    color: #fff;
    letter-spacing: 6rpx;
  }

  &__divider {
    width: 60rpx;
    height: 4rpx;
    background: rgba(255, 255, 255, 0.5);
    border-radius: 2rpx;
    margin: 16rpx 0;
  }

  &__subtitle {
    font-size: 26rpx;
    color: rgba(255, 255, 255, 0.85);
    letter-spacing: 8rpx;
  }

  // 底部区域
  &__bottom {
    flex: 1;
    padding: 0 40rpx;
    margin-top: -40rpx;
    position: relative;
    z-index: 2;
  }

  &__welcome {
    background: #fff;
    border-radius: 20rpx;
    padding: 28rpx 32rpx;
    box-shadow: 0 8rpx 30rpx rgba(0, 0, 0, 0.08);
    margin-bottom: 28rpx;

    &-text {
      font-size: 30rpx;
      font-weight: 600;
      color: #1f2937;
      display: block;
      text-align: center;
    }
  }

  // 卡片网格
  &__cards {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16rpx;
  }

  &__card {
    background: #fff;
    border-radius: 20rpx;
    padding: 32rpx 20rpx;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
    border: 2rpx solid transparent;
    transition: all 0.2s;

    &--hover {
      transform: scale(0.97);
      box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
    }

    &-icon-box {
      width: 88rpx;
      height: 88rpx;
      border-radius: 24rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 16rpx;

      &--student {
        background: linear-gradient(135deg, #ede9fe, #ddd6fe);
      }

      &--volunteer {
        background: linear-gradient(135deg, #fef3c7, #fde68a);
      }

      &--admin {
        background: linear-gradient(135deg, #e0e7ff, #c7d2fe);
      }

      &--teacher {
        background: linear-gradient(135deg, #dbeafe, #bfdbfe);
      }
    }

    &-icon {
      font-size: 44rpx;
    }

    &-title {
      font-size: 30rpx;
      font-weight: 700;
      color: #1f2937;
      display: block;
      margin-bottom: 6rpx;
    }

    &-desc {
      font-size: 22rpx;
      color: #9ca3af;
      display: block;
    }
  }

  // 底部
  &__footer {
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: center;
    padding: 48rpx 0 60rpx;
    gap: 20rpx;

    &-line {
      width: 40rpx;
      height: 2rpx;
      background: #d1d5db;
    }

    &-text {
      font-size: 22rpx;
      color: #9ca3af;
    }
  }
}

// 已登录跳转占位
.splash-loading {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;

  &__logo {
    width: 140rpx;
    height: 140rpx;
    background: rgba(255, 255, 255, 0.25);
    border-radius: 36rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    backdrop-filter: blur(20px);
    border: 2rpx solid rgba(255, 255, 255, 0.4);
  }

  &__logo-text {
    font-size: 72rpx;
  }
}
</style>
