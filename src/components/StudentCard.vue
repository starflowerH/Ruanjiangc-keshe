<template>
  <view class="student-card" @tap="handleTap">
    <view class="student-card__avatar-wrap">
      <image class="student-card__avatar" :src="data.avatar || '/static/images/default-avatar.png'" mode="aspectFill" />
      <view class="student-card__captain-badge" v-if="data.isCaptain">
        <text class="student-card__captain-text">队长</text>
      </view>
    </view>
    <view class="student-card__info">
      <view class="student-card__name-row">
        <text class="student-card__name">{{ data.name }}</text>
        <text class="student-card__university">{{ data.universityName }}</text>
      </view>
      <text class="student-card__major" v-if="data.major">{{ data.major }}</text>
      <text class="student-card__grade" v-if="data.grade">{{ data.grade }}</text>
    </view>
    <view class="student-card__action" v-if="showAsk" @tap.stop="handleAsk">
      <text class="student-card__action-text">提问</text>
    </view>
  </view>
</template>

<script setup>
const props = defineProps({
  data: {
    type: Object,
    required: true
  },
  showAsk: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['tap', 'ask'])

function handleTap() {
  emit('tap', props.data)
}

function handleAsk() {
  emit('ask', props.data)
}
</script>

<style lang="scss" scoped>
.student-card {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);

  &__avatar-wrap {
    position: relative;
    margin-right: 20rpx;
    flex-shrink: 0;
  }

  &__avatar {
    width: 88rpx;
    height: 88rpx;
    border-radius: 50%;
    background: #f0f0f0;
  }

  &__captain-badge {
    position: absolute;
    bottom: -4rpx;
    right: -4rpx;
    background: #faad14;
    border-radius: 999rpx;
    padding: 2rpx 10rpx;
  }

  &__captain-text {
    font-size: 18rpx;
    color: #fff;
  }

  &__info {
    flex: 1;
    overflow: hidden;
  }

  &__name-row {
    display: flex;
    align-items: center;
    margin-bottom: 6rpx;
  }

  &__name {
    font-size: 30rpx;
    font-weight: 600;
    color: #333;
    margin-right: 12rpx;
  }

  &__university {
    font-size: 22rpx;
    color: #5B9BD5;
    background: rgba(74, 144, 217, 0.1);
    padding: 2rpx 12rpx;
    border-radius: 999rpx;
  }

  &__major {
    font-size: 24rpx;
    color: #666;
    display: block;
    margin-bottom: 4rpx;
  }

  &__grade {
    font-size: 22rpx;
    color: #999;
    display: block;
  }

  &__action {
    background: #5B9BD5;
    border-radius: 999rpx;
    padding: 12rpx 28rpx;
    margin-left: 16rpx;
    flex-shrink: 0;
  }

  &__action-text {
    font-size: 24rpx;
    color: #fff;
  }
}
</style>
