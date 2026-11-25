<template>
  <view class="my-favorites">
    <NavBar title="我的收藏" />

    <!-- 收藏列表 -->
    <view class="my-favorites__list">
      <view class="favorite-card" v-for="item in favorites" :key="item.id" @tap="goDetail(item.id)">
        <view class="favorite-card__header">
          <text class="favorite-card__asker">{{ item.askerName || '匿名同学' }}</text>
          <text class="favorite-card__time">{{ formatTime(item.askedAt || item.createdAt) }}</text>
        </view>
        <view class="favorite-card__title-row">
          <text class="favorite-card__title" v-if="item.title">{{ item.title }}</text>
          <text class="favorite-card__featured" v-if="item.isFeatured">精华</text>
        </view>
        <text class="favorite-card__question">{{ item.questionContent }}</text>
        <view class="favorite-card__footer">
          <text class="favorite-card__status">
            {{ item.answerContent ? '已回答' : `${item.commentCount || 0} 条讨论` }}
          </text>
          <view class="favorite-card__cancel" @tap.stop="cancelFavorite(item)">
            <text class="favorite-card__cancel-text">取消收藏</text>
          </view>
        </view>
      </view>

      <Empty v-if="!loading && favorites.length === 0" text="还没有收藏的帖子" icon="⭐" />
    </view>

    <!-- 加载状态 -->
    <view v-if="loading" class="my-favorites__loading">
      <text class="my-favorites__loading-text">加载中...</text>
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
import * as qaApi from '@/api/qa'

const userStore = useUserStore()
const favorites = ref([])
const loading = ref(false)

onShow(() => {
  // 收藏是个人数据：未绑定身份时引导完善资料，避免请求 403
  if (!userStore.hasBoundIdentity) {
    uni.showModal({
      title: '提示',
      content: '请先完善个人资料后再查看收藏',
      showCancel: false,
      confirmText: '知道了'
    })
    favorites.value = []
    return
  }
  loadFavorites()
})

async function loadFavorites() {
  loading.value = true
  try {
    const res = await qaApi.getMyFavorites({ page: 1, size: 50 })
    favorites.value = res?.records || []
  } catch (e) {
    console.error('加载收藏失败:', e)
    favorites.value = []
    uni.showToast({ title: e.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function cancelFavorite(item) {
  try {
    await qaApi.toggleFavorite(item.id)
    favorites.value = favorites.value.filter(f => f.id !== item.id)
    uni.showToast({ title: '已取消收藏', icon: 'none' })
  } catch (e) {
    uni.showToast({ title: e.message || '操作失败', icon: 'none' })
  }
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/common/qa-detail?id=${id}` })
}
</script>

<style lang="scss" scoped>
.my-favorites {
  min-height: 100vh;
  background: #F2F6FB;

  &__list {
    padding: 24rpx;
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

.favorite-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12rpx;
  }

  &__asker {
    font-size: 26rpx;
    font-weight: 600;
    color: #333;
  }

  &__time {
    font-size: 22rpx;
    color: #999;
  }

  &__title {
    flex: 1;
    min-width: 0;
    font-size: 30rpx;
    font-weight: 600;
    color: #333;
    display: block;
    margin-bottom: 8rpx;
  }

  &__title-row {
    display: flex;
    align-items: flex-start;
    gap: 10rpx;
  }

  &__featured {
    flex-shrink: 0;
    padding: 3rpx 8rpx;
    color: #9a3412;
    background: #ffedd5;
    font-size: 20rpx;
  }

  &__question {
    font-size: 26rpx;
    color: #666;
    line-height: 1.6;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 16rpx;
  }

  &__status {
    font-size: 22rpx;
    color: #5B9BD5;
  }

  &__cancel {
    background: #f5f5f5;
    border-radius: 999rpx;
    padding: 8rpx 20rpx;

    &-text {
      font-size: 22rpx;
      color: #999;
    }
  }
}
</style>
