<template>
  <view class="uni-card" @tap="handleTap">
    <image class="uni-card__logo" :src="data.logo || '/static/images/default-uni.png'" mode="aspectFill" />
    <view class="uni-card__content">
      <text class="uni-card__name">{{ data.name }}</text>
      <text class="uni-card__slogan" v-if="data.slogan">{{ data.slogan }}</text>
      <view class="uni-card__tags" v-if="data.tags && data.tags.length">
        <text class="uni-card__tag" v-for="tag in data.tags.slice(0, 3)" :key="tag">{{ tag }}</text>
      </view>
    </view>
    <view class="uni-card__arrow">
      <text>›</text>
    </view>
  </view>
</template>

<script setup>
const props = defineProps({
  data: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['tap'])

function handleTap() {
  emit('tap', props.data)
}
</script>

<style lang="scss" scoped>
.uni-card {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.06);

  &__logo {
    width: 100rpx;
    height: 100rpx;
    border-radius: 12rpx;
    margin-right: 24rpx;
    flex-shrink: 0;
    background: #f0f0f0;
  }

  &__content {
    flex: 1;
    overflow: hidden;
  }

  &__name {
    font-size: 32rpx;
    font-weight: 600;
    color: #333;
    display: block;
    margin-bottom: 8rpx;
  }

  &__slogan {
    font-size: 24rpx;
    color: #999;
    display: block;
    margin-bottom: 12rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8rpx;
  }

  &__tag {
    font-size: 20rpx;
    color: #5B9BD5;
    background: rgba(74, 144, 217, 0.1);
    padding: 4rpx 12rpx;
    border-radius: 999rpx;
  }

  &__arrow {
    font-size: 36rpx;
    color: #ccc;
    margin-left: 16rpx;
  }
}
</style>
