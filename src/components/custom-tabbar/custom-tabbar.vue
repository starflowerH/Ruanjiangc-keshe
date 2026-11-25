<template>
  <view class="tabbar volunteer-community-ui">
    <view
      v-for="(item, index) in tabList"
      :key="index"
      class="tabbar__item"
      :class="{ 'tabbar__item--active': currentPath === item.pagePath }"
      @tap="switchTab(item.pagePath)"
    >
      <view class="tabbar__icon-wrap" :class="{ 'tabbar__icon-wrap--active': currentPath === item.pagePath }">
        <text class="tabbar__icon">{{ item.icon }}</text>
      </view>
      <text class="tabbar__label" :class="{ 'tabbar__label--active': currentPath === item.pagePath }">{{ item.text }}</text>
    </view>
  </view>
</template>

<script setup>
import { ref, watch } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'

const tabList = [
  {
    pagePath: '/pages/volunteer/home',
    icon: '🏠',
    text: '首页'
  },
  {
    pagePath: '/pages/volunteer/team-view',
    icon: '👥',
    text: '队伍'
  },
  {
    pagePath: '/pages/volunteer/qa-center',
    icon: '💬',
    text: '答疑'
  },
  {
    pagePath: '/pages/volunteer/profile',
    icon: '👤',
    text: '我的'
  }
]

const currentPath = ref('/pages/volunteer/home')

onShow(() => {
  const pages = getCurrentPages()
  if (pages.length > 0) {
    const currentPage = pages[pages.length - 1]
    currentPath.value = '/' + currentPage.route
  }
})

function switchTab(path) {
  if (currentPath.value === path) return
  uni.redirectTo({
    url: path
  })
}
</script>

<style lang="scss" scoped>
.tabbar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 120rpx;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-around;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
  padding-bottom: env(safe-area-inset-bottom);
  z-index: 999;

  &__item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    flex: 1;
    height: 100%;
    position: relative;

    &--active {
      .tabbar__icon-wrap {
        background: linear-gradient(135deg, #6FB1E5, #4A8BC4);
        transform: scale(1.12);
      }
    }
  }

  &__icon-wrap {
    width: 56rpx;
    height: 56rpx;
    border-radius: 16rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 6rpx;
    transition: all 0.3s ease;

    &--active {
      box-shadow: 0 6rpx 16rpx rgba(74, 139, 196, 0.4);
    }
  }

  &__icon {
    font-size: 32rpx;
  }

  &__label {
    font-size: 20rpx;
    color: #9ca3af;
    font-weight: 500;
    transition: all 0.3s ease;

    &--active {
      color: #5B9BD5;
      font-weight: 600;
    }
  }
}
// Volunteer community navigation visual layer
.volunteer-community-ui.tabbar {
  height: 120rpx;
  border-top: 1rpx solid #e5eaf0;
  box-shadow: none;

  .tabbar__icon-wrap { background: transparent; border-radius: 8rpx; }
  .tabbar__item--active .tabbar__icon-wrap { background: transparent; box-shadow: none; transform: none; }
  .tabbar__label { color: #7a899a; }
  .tabbar__label--active { color: #2879e8; }
}
</style>
