<template>
  <view class="my-comments">
    <NavBar title="我的评论" />

    <!-- 评论列表 -->
    <view class="my-comments__list">
      <view class="comment-card" v-for="item in comments" :key="item.id" @tap="goPost(item.postId)">
        <view class="comment-card__head">
          <view class="comment-card__avatar">
            <text class="comment-card__avatar-text">{{ (item.userName || '我').charAt(0) }}</text>
          </view>
          <view class="comment-card__head-info">
            <text class="comment-card__name">{{ item.userName || '我' }}{{ myIdentitySuffix }}</text>
            <text class="comment-card__time">{{ formatTime(item.createdAt) }}</text>
          </view>
        </view>
        <view class="comment-card__content-wrap">
          <text class="comment-card__content">{{ item.content }}</text>
        </view>
        <view class="comment-card__post">
          <text class="comment-card__post-label">评论了帖子</text>
          <text class="comment-card__post-title">{{ item.postTitle }}</text>
          <text class="comment-card__jump">查看原帖 ›</text>
        </view>
      </view>

      <Empty v-if="!loading && comments.length === 0" text="还没有发表过评论" icon="💬" />
    </view>

    <!-- 加载状态 -->
    <view v-if="loading" class="my-comments__loading">
      <text class="my-comments__loading-text">加载中...</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import NavBar from '@/components/NavBar.vue'
import Empty from '@/components/Empty.vue'
import { formatTime } from '@/utils/format'
import { useUserStore } from '@/store/modules/user'
import * as commentApi from '@/api/comment'

const userStore = useUserStore()
const comments = ref([])
const loading = ref(false)
const myIdentitySuffix = computed(() => (userStore.isVolunteer ? '-大学生' : '-高中生'))

onShow(() => {
  // 评论是个人数据：未绑定身份时引导完善资料，避免请求 403
  if (!userStore.hasBoundIdentity) {
    uni.showModal({
      title: '提示',
      content: '请先完善个人资料后再查看评论',
      showCancel: false,
      confirmText: '知道了'
    })
    comments.value = []
    return
  }
  loadComments()
})

async function loadComments() {
  loading.value = true
  try {
    const res = await commentApi.getMyComments({ page: 1, size: 50 })
    comments.value = res?.records || []
  } catch (e) {
    console.error('加载我的评论失败:', e)
    comments.value = []
  } finally {
    loading.value = false
  }
}

function goPost(postId) {
  if (!postId) return
  uni.navigateTo({ url: `/pages/common/qa-detail?id=${postId}` })
}
</script>

<style lang="scss" scoped>
.my-comments {
  min-height: 100vh;
  background: #F2F6FB;
  padding-bottom: 40rpx;

  &__list {
    padding: 24rpx;
  }

  &__loading {
    padding: 60rpx 0;
    text-align: center;

    &-text {
      font-size: 26rpx;
      color: #9CA3AF;
    }
  }
}

.comment-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(91, 155, 213, 0.08);

  &__head {
    display: flex;
    align-items: center;
    margin-bottom: 16rpx;
  }

  &__avatar {
    width: 64rpx;
    height: 64rpx;
    border-radius: 50%;
    background: linear-gradient(135deg, #8FC1EC, #5B9BD5);
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 16rpx;
    flex-shrink: 0;

    &-text {
      font-size: 28rpx;
      color: #fff;
      font-weight: 600;
    }
  }

  &__head-info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    font-size: 28rpx;
    color: #1F2937;
    font-weight: 600;
    display: block;
  }

  &__time {
    font-size: 22rpx;
    color: #9CA3AF;
    margin-top: 4rpx;
    display: block;
  }

  &__content-wrap {
    background: #F8FAFC;
    border-radius: 12rpx;
    padding: 16rpx 20rpx;
    margin-bottom: 16rpx;
  }

  &__content {
    font-size: 28rpx;
    color: #1F2937;
    line-height: 42rpx;
  }

  &__post {
    display: flex;
    align-items: center;
    gap: 12rpx;

    &-label {
      font-size: 22rpx;
      color: #9CA3AF;
      flex-shrink: 0;
      background: #F2F6FB;
      padding: 4rpx 12rpx;
      border-radius: 8rpx;
    }

    &-title {
      flex: 1;
      font-size: 24rpx;
      color: #4A8BC4;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  &__jump {
    font-size: 22rpx;
    color: #5B9BD5;
    flex-shrink: 0;
  }
}
</style>
