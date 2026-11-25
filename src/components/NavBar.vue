<template>
  <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
    <view class="nav-bar__content" :style="{ height: navBarHeight + 'px' }">
      <view class="nav-bar__left" @tap="handleBack" v-if="showBack">
        <text class="nav-bar__back-icon">‹</text>
      </view>
      <view class="nav-bar__left" v-else>
        <slot name="left"></slot>
      </view>
      <view class="nav-bar__title">
        <text class="nav-bar__title-text">{{ title }}</text>
      </view>
      <view class="nav-bar__right">
        <slot name="right"></slot>
      </view>
    </view>
  </view>
  <!-- 占位 -->
  <view :style="{ height: (statusBarHeight + navBarHeight) + 'px' }"></view>
</template>

<script setup>
import { useAppStore } from '@/store/modules/app'
import { storeToRefs } from 'pinia'
import { goRoleHome } from '@/utils/nav'

const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  showBack: {
    type: Boolean,
    default: true
  }
})

const appStore = useAppStore()
const { statusBarHeight, navBarHeight } = storeToRefs(appStore)

function handleBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack({ delta: 1 })
  } else {
    // 页面栈只有一页时 navigateBack 必然失败，兜底回身份首页（已登录）或身份选择页（未登录）
    goRoleHome()
  }
}
</script>

<style lang="scss" scoped>
.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
  background: linear-gradient(135deg, #5B9BD5, #4A8BC4);

  &__content {
    display: flex;
    align-items: center;
    padding: 0 24rpx;
  }

  &__left {
    width: 80rpx;
    display: flex;
    align-items: center;
  }

  &__back-icon {
    font-size: 48rpx;
    color: #fff;
    font-weight: bold;
  }

  &__title {
    flex: 1;
    text-align: center;
  }

  &__title-text {
    font-size: 34rpx;
    color: #fff;
    font-weight: 600;
  }

  &__right {
    width: 80rpx;
    display: flex;
    align-items: center;
    justify-content: flex-end;
  }
}
</style>
