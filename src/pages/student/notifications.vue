<template>
  <view class="notifications">
    <NavBar title="消息通知" />

    <!-- 通知列表 -->
    <view class="notifications__list">
      <view
        v-for="item in notifications"
        :key="item.id"
        class="notifications__item"
        :class="{ 'notifications__item--unread': !item.isRead }"
        @tap="handleTap(item)"
      >
        <!-- 通知图标 -->
        <view class="notifications__icon" :class="`notifications__icon--${item.type}`">
          <text class="notifications__icon-text">{{ getIcon(item.type) }}</text>
        </view>

        <!-- 通知内容 -->
        <view class="notifications__content">
          <view class="notifications__header">
            <text class="notifications__title">{{ item.title }}</text>
            <text class="notifications__time">{{ formatTime(item.createdAt) }}</text>
          </view>
          <text class="notifications__desc">{{ item.content }}</text>
          <view class="notifications__source" v-if="item.sourceTitle">
            <text class="notifications__source-label">来自：</text>
            <text class="notifications__source-text">{{ item.sourceTitle }}</text>
          </view>
        </view>

        <!-- 未读标记 -->
        <view v-if="!item.isRead" class="notifications__dot" />
      </view>

      <!-- 空状态 -->
      <Empty v-if="!loading && !notifications.length" text="暂无消息通知" icon="🔔" />
    </view>

    <!-- 加载状态 -->
    <view v-if="loading" class="notifications__loading">
      <text class="notifications__loading-text">加载中...</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import NavBar from '@/components/NavBar.vue'
import Empty from '@/components/Empty.vue'
import { formatTime } from '@/utils/format'
import { useUserStore } from '@/store/modules/user'
import * as notificationApi from '@/api/notification'

const userStore = useUserStore()
const notifications = ref([])
const loading = ref(false)

onShow(() => {
  loadNotifications()
})

async function loadNotifications() {
  loading.value = true
  try {
    const res = await notificationApi.getNotifications({ page: 1, size: 50 })
    notifications.value = res?.records || []
  } catch (e) {
    console.error('加载通知失败:', e)
    notifications.value = []
  } finally {
    loading.value = false
  }
}

function getIcon(type) {
  const map = {
    reply: '💬',
    comment: '💭',
    system: '📢',
    like: '❤️'
  }
  return map[type] || '🔔'
}

async function handleTap(item) {
  // 标记为已读
  if (!item.isRead) {
    try {
      await notificationApi.markAsRead(item.id)
      item.isRead = true
    } catch (e) {
      console.error('标记已读失败:', e)
    }
  }

  // 跳转到相关详情页（志愿者收到提问通知直接进入答题页，学生进入问答详情）
  if (item.sourceType === 'question' && item.sourceId) {
    const url = userStore.isVolunteer
      ? `/pages/volunteer/answer-question?id=${item.sourceId}`
      : `/pages/common/qa-detail?id=${item.sourceId}`
    uni.navigateTo({ url })
  }
}
</script>

<style lang="scss" scoped>
.notifications {
  min-height: 100vh;
  background: #F2F6FB;

  &__list {
    padding: 24rpx;
  }

  &__item {
    background: #fff;
    border-radius: 16rpx;
    padding: 24rpx;
    margin-bottom: 20rpx;
    display: flex;
    align-items: flex-start;
    box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
    position: relative;

    &--unread {
      background: #f0f5ff;
    }
  }

  &__icon {
    width: 80rpx;
    height: 80rpx;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 20rpx;
    flex-shrink: 0;

    &--reply {
      background: rgba(74, 144, 217, 0.1);
    }

    &--comment {
      background: rgba(82, 196, 26, 0.1);
    }

    &--system {
      background: rgba(250, 173, 20, 0.1);
    }

    &--like {
      background: rgba(255, 77, 79, 0.1);
    }

    &-text {
      font-size: 40rpx;
    }
  }

  &__content {
    flex: 1;
    min-width: 0;
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8rpx;
  }

  &__title {
    font-size: 28rpx;
    font-weight: 600;
    color: #333;
  }

  &__time {
    font-size: 22rpx;
    color: #999;
    flex-shrink: 0;
    margin-left: 16rpx;
  }

  &__desc {
    font-size: 26rpx;
    color: #666;
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__source {
    margin-top: 12rpx;
    padding: 12rpx 16rpx;
    background: #f8f9fc;
    border-radius: 8rpx;
    display: flex;
    align-items: center;
  }

  &__source-label {
    font-size: 22rpx;
    color: #999;
    margin-right: 8rpx;
  }

  &__source-text {
    font-size: 22rpx;
    color: #5B9BD5;
  }

  &__dot {
    position: absolute;
    top: 24rpx;
    right: 24rpx;
    width: 16rpx;
    height: 16rpx;
    background: #ff4d4f;
    border-radius: 50%;
  }

  &__loading {
    padding: 40rpx;
    text-align: center;

    &-text {
      font-size: 26rpx;
      color: #999;
    }
  }
}
</style>
