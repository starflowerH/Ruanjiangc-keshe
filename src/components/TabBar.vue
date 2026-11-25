<template>
  <view class="tabbar">
    <view
      v-for="tab in tabs"
      :key="tab.path"
      :class="['tabbar__item', currentPath === tab.path && 'tabbar__item--active']"
      @tap="switchTab(tab)"
    >
      <text class="tabbar__icon">{{ currentPath === tab.path ? tab.activeIcon : tab.icon }}</text>
      <text class="tabbar__text">{{ tab.text }}</text>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/store/modules/user'

const props = defineProps({
  currentPath: {
    type: String,
    default: ''
  }
})

const userStore = useUserStore()

const studentTabs = [
  {
    path: '/pages/student/home',
    text: '首页',
    icon: '⌂',
    activeIcon: '⌂'
  },
  {
    path: '/pages/student/university-list',
    text: '高校',
    icon: '▥',
    activeIcon: '▥'
  },
  {
    path: '/pages/student/qa-forum',
    text: '问答',
    icon: '◯',
    activeIcon: '◯'
  },
  {
    path: '/pages/student/profile',
    text: '我的',
    icon: '♙',
    activeIcon: '♙'
  }
]

const volunteerTabs = [
  {
    path: '/pages/volunteer/home',
    text: '首页',
    icon: '⌂',
    activeIcon: '⌂'
  },
  {
    path: '/pages/volunteer/team-view',
    text: '队伍',
    icon: '♧',
    activeIcon: '♧'
  },
  {
    path: '/pages/volunteer/qa-center',
    text: '答疑',
    icon: '◯',
    activeIcon: '◯'
  },
  {
    path: '/pages/volunteer/profile',
    text: '我的',
    icon: '♙',
    activeIcon: '♙'
  }
]

const tabs = computed(() => {
  if (userStore.isVolunteer || userStore.isAdmin) return volunteerTabs
  return studentTabs
})

function switchTab(tab) {
  if (props.currentPath === tab.path) return
  uni.redirectTo({ url: tab.path })
}
</script>

<style lang="scss" scoped>
.tabbar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 144rpx;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-around;
  border-top: 1rpx solid #e6edf4;
  box-shadow: 0 -6rpx 18rpx rgba(49, 77, 96, 0.06);
  z-index: 999;
  padding-bottom: env(safe-area-inset-bottom);

  &__item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    flex: 1;
    padding: 8rpx 0;

    &--active {
      position: relative;
      .tabbar__text {
        color: #5B9BD5;
        font-weight: 800;
      }
      &::after {
        position: absolute;
        right: 50%;
        bottom: 1rpx;
        width: 36rpx;
        height: 6rpx;
        border-radius: 99rpx;
        background: #5B9BD5;
        content: '';
        transform: translateX(50%);
      }
    }
  }

  &__icon {
    font-family: sans-serif;
    font-size: 42rpx;
    margin-bottom: 6rpx;
    color: #8a9aaa;
  }

  &__text {
    font-size: 22rpx;
    color: #999;
  }
}
</style>
