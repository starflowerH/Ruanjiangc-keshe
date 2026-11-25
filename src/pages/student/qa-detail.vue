<template>
  <view class="qa-detail">
    <NavBar title="问答详情" />

    <view class="qa-detail__content">
      <!-- 问题 -->
      <view class="qa-detail__question">
        <view class="qa-detail__q-header">
          <view class="qa-detail__q-icon">问</view>
          <text class="qa-detail__q-title">我的问题</text>
        </view>
        <text class="qa-detail__q-text">{{ qa.questionContent || qa.question }}</text>
        <text class="qa-detail__time">{{ formatTime(qa.askedAt || qa.createdAt) }}</text>
      </view>

      <!-- 回答 -->
      <view class="qa-detail__answer" v-if="qa.answerContent || qa.answer">
        <view class="qa-detail__a-header">
          <view class="qa-detail__a-icon">答</view>
          <text class="qa-detail__a-title">学长学姐的回答</text>
        </view>
        <text class="qa-detail__a-text">{{ qa.answerContent || qa.answer }}</text>
        <view class="qa-detail__a-meta">
          <view class="qa-detail__answerer-info">
            <image class="qa-detail__answerer-avatar" :src="qa.answererAvatar || '/static/images/default-avatar.png'" mode="aspectFill" />
            <view class="qa-detail__answerer-detail">
              <text class="qa-detail__answerer-name">{{ qa.answererName }}</text>
              <text class="qa-detail__answerer-school">{{ qa.answererUniversity }}</text>
            </view>
          </view>
          <text class="qa-detail__time">{{ qa.answeredAt }}</text>
        </view>
      </view>

      <!-- 待回答提示 -->
      <view class="qa-detail__pending" v-else>
        <text class="qa-detail__pending-icon">⏳</text>
        <text class="qa-detail__pending-text">等待学长学姐回答中...</text>
        <text class="qa-detail__pending-hint">回答后会通过消息通知您</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { formatTime } from '@/utils/format'
import NavBar from '@/components/NavBar.vue'
import * as qaApi from '@/api/qa'

const qa = reactive({
  id: null,
  questionContent: '',
  answerContent: '',
  askerName: '',
  answererName: '',
  answererAvatar: '',
  answererUniversity: '',
  askedAt: '',
  answeredAt: '',
  status: ''
})

let pageAlive = true

onUnload(() => {
  pageAlive = false
})

onLoad(async (options) => {
  if (options.id && options.id !== 'undefined') {
    await loadDetail(options.id)
  }
})

async function loadDetail(id) {
  uni.showLoading({ title: '加载中...' })
  try {
    const detail = await qaApi.getQADetail(id)
    // 页面已销毁时不再操作组件状态，避免 LifeCycle start fail / Java object is gone
    if (!pageAlive) return
    Object.assign(qa, detail)
  } catch (e) {
    if (!pageAlive) return
    console.error('加载问答详情失败:', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    if (pageAlive) uni.hideLoading()
  }
}
</script>

<style lang="scss" scoped>
.qa-detail {
  min-height: 100vh;
  background: #F2F6FB;

  &__content {
    padding: 24rpx;
  }

  &__question {
    background: #fff;
    border-radius: 16rpx;
    padding: 24rpx;
    margin-bottom: 20rpx;
  }

  &__q-header, &__a-header {
    display: flex;
    align-items: center;
    margin-bottom: 16rpx;
  }

  &__q-icon {
    background: #5B9BD5;
    color: #fff;
    font-size: 22rpx;
    width: 40rpx;
    height: 40rpx;
    border-radius: 8rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 12rpx;
  }

  &__q-title, &__a-title {
    font-size: 28rpx;
    font-weight: 600;
    color: #333;
  }

  &__q-text {
    font-size: 28rpx;
    color: #333;
    line-height: 1.6;
    display: block;
    margin-bottom: 12rpx;
  }

  &__time {
    font-size: 22rpx;
    color: #999;
  }

  &__answer {
    background: #fff;
    border-radius: 16rpx;
    padding: 24rpx;
    margin-bottom: 20rpx;
  }

  &__a-icon {
    background: #52c41a;
    color: #fff;
    font-size: 22rpx;
    width: 40rpx;
    height: 40rpx;
    border-radius: 8rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 12rpx;
  }

  &__a-text {
    font-size: 28rpx;
    color: #333;
    line-height: 1.8;
    display: block;
    margin-bottom: 16rpx;
  }

  &__a-meta {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  &__answerer-info {
    display: flex;
    align-items: center;
  }

  &__answerer-avatar {
    width: 48rpx;
    height: 48rpx;
    border-radius: 50%;
    margin-right: 12rpx;
    background: #f0f0f0;
  }

  &__answerer-detail {
    display: flex;
    flex-direction: column;
  }

  &__answerer-name {
    font-size: 24rpx;
    color: #333;
    font-weight: 500;
  }

  &__answerer-school {
    font-size: 20rpx;
    color: #5B9BD5;
  }

  &__pending {
    background: #fff;
    border-radius: 16rpx;
    padding: 48rpx;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  &__pending-icon {
    font-size: 64rpx;
    margin-bottom: 16rpx;
  }

  &__pending-text {
    font-size: 28rpx;
    color: #333;
    margin-bottom: 8rpx;
  }

  &__pending-hint {
    font-size: 24rpx;
    color: #999;
  }
}
</style>
