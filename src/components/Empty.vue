<template>
  <view class="empty">
    <text class="empty__icon">{{ icon }}</text>
    <text class="empty__text">{{ text }}</text>
    <view class="empty__action" v-if="actionText" @tap="$emit('action')">
      <text class="empty__action-text">{{ actionText }}</text>
    </view>
  </view>
</template>

<script setup>
defineProps({
  icon: {
    type: String,
    default: '📭'
  },
  text: {
    type: String,
    default: '暂无数据'
  },
  actionText: {
    type: String,
    default: ''
  }
})

defineEmits(['action'])
</script>

<style lang="scss" scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 48rpx;

  &__icon {
    font-size: 96rpx;
    margin-bottom: 24rpx;
  }

  &__text {
    font-size: 28rpx;
    color: #999;
    margin-bottom: 32rpx;
  }

  &__action {
    padding: 16rpx 48rpx;
    background: #5B9BD5;
    border-radius: 999rpx;
  }

  &__action-text {
    font-size: 28rpx;
    color: #fff;
  }
}
</style>
