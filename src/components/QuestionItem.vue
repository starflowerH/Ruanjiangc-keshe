<template>
  <view class="qa-item" @tap="handleTap">
    <view class="qa-item__question">
      <view class="qa-item__q-icon">问</view>
      <text class="qa-item__q-text">{{ data.question || data.questionContent }}</text>
      <text class="qa-item__featured" v-if="data.isFeatured">精华</text>
    </view>
    <view class="qa-item__answer" v-if="data.answer || data.answerContent">
      <view class="qa-item__a-icon">答</view>
      <text class="qa-item__a-text">{{ data.answer || data.answerContent }}</text>
    </view>
    <view class="qa-item__meta">
      <text class="qa-item__asker" v-if="data.askerName">提问者：{{ formatIdentityName(data.askerName, data.authorType) }}</text>
      <text class="qa-item__answerer" v-if="data.answererName">回答者：{{ data.answererName }}</text>
      <text class="qa-item__time">{{ formatTime(data.askedAt || data.createdAt) }}</text>
    </view>
    <view class="qa-item__status" v-if="showStatus">
      <text class="qa-item__status-text" :class="statusClass">{{ statusText }}</text>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { formatTime, formatIdentityName } from '@/utils/format'

const props = defineProps({
  data: {
    type: Object,
    required: true
  },
  showStatus: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['tap'])

const statusText = computed(() => {
  const map = {
    pending: '待回答',
    answered: '已回答',
    featured: '精选'
  }
  return map[props.data.status] || ''
})

const statusClass = computed(() => {
  return `qa-item__status-text--${props.data.status}`
})

function handleTap() {
  emit('tap', props.data)
}
</script>

<style lang="scss" scoped>
.qa-item {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);

  &__question {
    display: flex;
    align-items: flex-start;
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
    flex-shrink: 0;
  }

  &__q-text {
    flex: 1;
    min-width: 0;
    font-size: 28rpx;
    color: #333;
    font-weight: 500;
    line-height: 1.5;
  }

  &__featured {
    flex-shrink: 0;
    margin-left: 10rpx;
    padding: 4rpx 10rpx;
    color: #9a3412;
    background: #ffedd5;
    font-size: 20rpx;
    line-height: 1.4;
  }

  &__answer {
    display: flex;
    align-items: flex-start;
    margin-bottom: 16rpx;
    padding-left: 8rpx;
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
    flex-shrink: 0;
  }

  &__a-text {
    font-size: 26rpx;
    color: #666;
    line-height: 1.6;
  }

  &__meta {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;
    font-size: 22rpx;
    color: #999;
  }

  &__status {
    margin-top: 12rpx;
  }

  &__status-text {
    font-size: 22rpx;
    padding: 4rpx 16rpx;
    border-radius: 999rpx;

    &--pending {
      color: #faad14;
      background: rgba(250, 173, 20, 0.1);
    }

    &--answered {
      color: #52c41a;
      background: rgba(82, 196, 26, 0.1);
    }

    &--featured {
      color: #ff4d4f;
      background: rgba(255, 77, 79, 0.1);
    }
  }
}
</style>
