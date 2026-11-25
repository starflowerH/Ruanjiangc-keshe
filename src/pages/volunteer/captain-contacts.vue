<template>
  <view class="captain-contacts" :style="rootStyle">
    <view class="captain-contacts__header">
      <button class="captain-contacts__back" @tap="goBack">返回</button>
      <text class="captain-contacts__title">队长通讯录</text>
      <view class="captain-contacts__header-space" />
    </view>
    <!-- 本队队员 -->
    <view class="section">
      <view class="section__header">
        <text class="section__title">👥 本队队员</text>
        <text class="section__count">{{ teamMembers.length }}人</text>
      </view>
      <view class="member-list" v-if="teamMembers.length > 0">
        <view class="member-card" v-for="member in teamMembers" :key="member.volunteerId">
          <view class="member-card__avatar">
            <text class="member-card__avatar-text">{{ member.name?.charAt(0) || '?' }}</text>
          </view>
          <view class="member-card__info">
            <view class="member-card__name-row">
              <text class="member-card__name">{{ member.name }}</text>
              <text class="member-card__captain-badge" v-if="member.isCaptain">队长</text>
            </view>
            <text class="member-card__school">{{ member.universityName || '' }}</text>
            <text class="member-card__detail">{{ member.major || '' }} {{ member.grade || '' }}</text>
          </view>
        </view>
      </view>
      <view class="empty" v-else>
        <text class="empty__text">暂无队员</text>
      </view>
    </view>

    <!-- 其他队长 -->
    <view class="section">
      <view class="section__header">
        <text class="section__title">📋 其他队长</text>
        <text class="section__count">{{ captainContacts.length }}人</text>
      </view>
      <view class="member-list" v-if="captainContacts.length > 0">
        <view class="member-card" v-for="captain in captainContacts" :key="captain.volunteerId">
          <view class="member-card__avatar member-card__avatar--captain">
            <text class="member-card__avatar-text">{{ captain.name?.charAt(0) || '?' }}</text>
          </view>
          <view class="member-card__info">
            <view class="member-card__name-row">
              <text class="member-card__name">{{ captain.name }}</text>
              <text class="member-card__school-tag">{{ captain.universityName || '' }}</text>
            </view>
            <text class="member-card__target">目标高中：{{ captain.targetHighSchoolName || '未选择' }}</text>
            <text class="member-card__detail">来自：{{ captain.universityName || '未知大学' }} · {{ captain.teamName || '未命名队伍' }}</text>
            <text class="member-card__detail">{{ captain.major || '' }} {{ captain.grade || '' }}</text>

            <!-- 联系方式：点击展开（默认收起，避免平铺杂乱） -->
            <view class="member-card__contact" v-if="captain.phone">
              <view
                v-if="expandedId !== captain.volunteerId"
                class="member-card__contact-btn"
                @tap="expandedId = captain.volunteerId"
              >
                <text class="member-card__contact-btn-text">📞 查看联系方式</text>
                <text class="member-card__contact-btn-arrow">›</text>
              </view>
              <view v-else class="member-card__contact-detail" @tap="callCaptain(captain.phone)">
                <text class="member-card__contact-number">{{ captain.phone }}</text>
                <text class="member-card__contact-tip">点击拨打</text>
              </view>
            </view>
            <text v-if="captain.wechat" class="member-card__wechat">微信：{{ captain.wechat }}</text>
          </view>
        </view>
      </view>
      <view class="empty" v-else>
        <text class="empty__text">暂无其他队长</text>
      </view>
    </view>

    <!-- 提示信息 -->
    <view class="tips">
      <text class="tips__text">💡 队长手机号公开用于联系协调，点击号码可直接拨打</text>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/modules/user'
import * as teamApi from '@/api/team'

const userStore = useUserStore()
const teamMembers = ref([])
const captainContacts = ref([])
const expandedId = ref(null)  // 当前展开联系方式的队长 volunteerId（null=全部收起）

// 修复: 页面为 custom navigationStyle 且无原生导航栏,
// env(safe-area-inset-top) 在安卓上恒为 0, 返回按钮会被状态栏遮挡。
// 改用系统状态栏高度撑开顶部内边距（优先新 API，避免弃用警告）。
const systemInfo = typeof uni.getWindowInfo === 'function' ? uni.getWindowInfo() : uni.getSystemInfoSync()
const rootStyle = computed(() => ({
  paddingTop: `${(systemInfo.statusBarHeight || 0) + 10}px`
}))

onShow(async () => {
  if (!userStore.isCaptain) {
    uni.showToast({ title: '仅队长可查看', icon: 'none' })
    setTimeout(() => {
      if (getCurrentPages().length > 1) {
        uni.navigateBack()
      } else {
        uni.reLaunch({ url: '/pages/volunteer/home' })
      }
    }, 600)
    return
  }
  await loadData()
})

async function loadData() {
  uni.showLoading({ title: '加载中...' })
  try {
    const [members, captains] = await Promise.all([
      teamApi.getMyTeamMembers(),
      teamApi.getCaptainContacts()
    ])
    teamMembers.value = members || []
    captainContacts.value = captains || []
  } catch (e) {
    console.error('加载队长通讯录失败:', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

function callCaptain(phone) {
  if (!phone) return
  uni.makePhoneCall({
    phoneNumber: phone,
    fail: (err) => {
      console.error('拨打失败:', err)
      uni.showToast({ title: '拨打失败', icon: 'none' })
    }
  })
}

function goBack() {
  if (getCurrentPages().length > 1) {
    uni.navigateBack()
    return
  }
  uni.reLaunch({ url: '/pages/volunteer/team-view' })
}
</script>

<style lang="scss" scoped>
.captain-contacts {
  min-height: 100vh;
  background: #F2F6FB;

  padding: 0 24rpx 24rpx;
  box-sizing: border-box;

  &__header {
    height: 72rpx;
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20rpx;
  }

  &__back,
  &__header-space {
    width: 112rpx;
    height: 56rpx;
  }

  &__back {
    margin: 0;
    padding: 0;
    border: 0;
    border-radius: 12rpx;
    background: #fff;
    color: #2d5f8b;
    font-size: 26rpx;
    line-height: 56rpx;
  }

  &__title {
    font-size: 32rpx;
    font-weight: 600;
    color: #1f2d3d;
  }
}

.section {
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 24rpx;
  overflow: hidden;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24rpx;
    border-bottom: 2rpx solid #f5f5f5;
  }

  &__title {
    font-size: 30rpx;
    font-weight: 600;
    color: #333;
  }

  &__count {
    font-size: 24rpx;
    color: #999;
  }
}

.member-list {
  padding: 12rpx 24rpx;
}

.member-card {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 2rpx solid #f8f8f8;

  &:last-child {
    border-bottom: none;
  }

  &__avatar {
    width: 80rpx;
    height: 80rpx;
    border-radius: 50%;
    background: linear-gradient(135deg, #5B9BD5, #4A8BC4);
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 20rpx;
    flex-shrink: 0;

    &--captain {
      background: linear-gradient(135deg, #FFD700, #FFA500);
    }
  }

  &__avatar-text {
    font-size: 32rpx;
    color: #fff;
    font-weight: 600;
  }

  &__info {
    flex: 1;
  }

  &__name-row {
    display: flex;
    align-items: center;
    margin-bottom: 6rpx;
  }

  &__name {
    font-size: 28rpx;
    font-weight: 600;
    color: #333;
    margin-right: 12rpx;
  }

  &__captain-badge {
    font-size: 20rpx;
    color: #FFD700;
    background: rgba(255, 215, 0, 0.15);
    padding: 2rpx 12rpx;
    border-radius: 999rpx;
  }

  &__school-tag {
    font-size: 22rpx;
    color: #5B9BD5;
    background: rgba(74, 144, 217, 0.1);
    padding: 2rpx 12rpx;
    border-radius: 999rpx;
  }

  &__school {
    font-size: 24rpx;
    color: #666;
    margin-bottom: 4rpx;
  }

  &__target {
    font-size: 24rpx;
    color: #666;
    margin-bottom: 4rpx;
  }

  &__detail {
    font-size: 22rpx;
    color: #999;
  }

  &__contact {
    margin-top: 12rpx;
  }

  &__contact-btn {
    display: inline-flex;
    align-items: center;
    gap: 8rpx;
    padding: 8rpx 20rpx;
    background: #f0f7ff;
    border: 2rpx solid #b8d4fe;
    border-radius: 999rpx;

    &-text {
      font-size: 24rpx;
      color: #4A8BC4;
      font-weight: 500;
    }

    &-arrow {
      font-size: 24rpx;
      color: #4A8BC4;
    }
  }

  &__contact-detail {
    display: flex;
    align-items: center;
    gap: 12rpx;
    padding: 10rpx 20rpx;
    background: #f6ffed;
    border: 2rpx solid #b7eb8f;
    border-radius: 12rpx;
  }

  &__contact-number {
    font-size: 26rpx;
    color: #389e0d;
    font-weight: 600;
    letter-spacing: 1rpx;
  }

  &__contact-tip {
    font-size: 20rpx;
    color: #95de64;
  }
}

.empty {
  padding: 40rpx;
  text-align: center;

  &__text {
    font-size: 26rpx;
    color: #999;
  }
}

.tips {
  padding: 24rpx;
  text-align: center;

  &__text {
    font-size: 22rpx;
    color: #999;
  }
}
</style>
