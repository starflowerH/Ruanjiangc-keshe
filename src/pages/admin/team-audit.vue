<template>
  <view class="team-audit">
    <NavBar title="队伍角色审核" />

    <!-- 列表 -->
    <view class="team-audit__content">
      <view class="team-audit__card" v-for="item in list" :key="item.id">
        <view class="team-audit__card-header">
          <view class="team-audit__card-title-row">
            <text class="team-audit__applicant-name">{{ item.volunteerName || '未知' }}</text>
            <text :class="['team-audit__role-tag', getRoleTagClass(item.applyRole)]">
              {{ getRoleTagText(item.applyRole) }}
            </text>
          </view>
          <text class="team-audit__time">{{ formatTime(item.createdAt) }}</text>
        </view>

        <view class="team-audit__card-body">
          <view class="team-audit__info-row">
            <text class="team-audit__info-label">队伍号</text>
            <text class="team-audit__info-value">{{ item.teamCode || '-' }}</text>
          </view>
          <view class="team-audit__info-row">
            <text class="team-audit__info-label">队伍名称</text>
            <text class="team-audit__info-value">{{ item.teamName || '-' }}</text>
          </view>
          <view class="team-audit__info-row">
            <text class="team-audit__info-label">专业年级</text>
            <text class="team-audit__info-value">{{ item.volunteerMajor || '' }} {{ item.volunteerGrade || '' }}</text>
          </view>
          <view class="team-audit__info-row" v-if="item.reason">
            <text class="team-audit__info-label">{{ item.applyRole === 'CAPTAIN_TRANSFER' ? '转让理由' : '申请理由' }}</text>
            <text class="team-audit__info-value">{{ getDisplayReason(item) }}</text>
          </view>
        </view>

        <view class="team-audit__card-actions">
          <view class="team-audit__btn team-audit__btn--reject" @tap="handleAudit(item.id, 'REJECTED')">
            <text class="team-audit__btn-text">拒绝</text>
          </view>
          <view class="team-audit__btn team-audit__btn--approve" @tap="handleAudit(item.id, 'APPROVED')">
            <text class="team-audit__btn-text">通过</text>
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="team-audit__empty" v-if="list.length === 0 && !loading">
        <text class="team-audit__empty-icon">📋</text>
        <text class="team-audit__empty-text">暂无待审核申请</text>
      </view>

      <!-- 加载中 -->
      <view class="team-audit__loading" v-if="loading">
        <text class="team-audit__loading-text">加载中...</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/modules/user'
import NavBar from '@/components/NavBar.vue'
import * as adminApi from '@/api/admin'
import { formatTime } from '@/utils/format'

const userStore = useUserStore()
const loading = ref(false)
const list = ref([])

onShow(() => {
  if (!userStore.isAdmin) {
    uni.showToast({ title: '无权限访问', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 500)
    return
  }
  loadApplications()
})

async function loadApplications() {
  loading.value = true
  try {
    const res = await adminApi.getTeamApplications()
    list.value = res || []
  } catch (e) {
    console.error('加载申请列表失败:', e)
    list.value = []
  } finally {
    loading.value = false
  }
}

function getRoleTagText(role) {
  if (role === 'CAPTAIN') return '申请队长'
  if (role === 'CAPTAIN_TRANSFER') return '队长转让'
  return role
}

function getRoleTagClass(role) {
  if (role === 'CAPTAIN') return 'team-audit__role-tag--captain'
  if (role === 'CAPTAIN_TRANSFER') return 'team-audit__role-tag--transfer'
  return ''
}

function getDisplayReason(item) {
  if (item.applyRole === 'CAPTAIN_TRANSFER' && item.reason) {
    // 格式："转让给:targetId;理由"
    const parts = item.reason.split(';')
    const reasonPart = parts.length > 1 ? parts[1] : ''
    return reasonPart || '无理由'
  }
  return item.reason || ''
}

function handleAudit(id, action) {
  const actionText = action === 'APPROVED' ? '通过' : '拒绝'
  uni.showModal({
    title: '确认审核',
    content: `确定${actionText}该申请吗？`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        uni.showLoading({ title: '审核中...' })
        await adminApi.auditTeamApplication(id, { action })
        uni.hideLoading()
        uni.showToast({ title: `已${actionText}`, icon: 'success' })
        loadApplications()
      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: e.message || '审核失败', icon: 'none' })
      }
    }
  })
}

</script>

<style lang="scss" scoped>
.team-audit {
  min-height: 100vh;
  background: #F2F6FB;

  &__content {
    padding: 24rpx;
  }

  &__card {
    background: #fff;
    border-radius: 20rpx;
    margin-bottom: 24rpx;
    overflow: hidden;
    box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
  }

  &__card-header {
    padding: 24rpx 28rpx 16rpx;
    border-bottom: 1rpx solid #f5f5f5;
  }

  &__card-title-row {
    display: flex;
    align-items: center;
    gap: 12rpx;
    margin-bottom: 8rpx;
  }

  &__applicant-name {
    font-size: 30rpx;
    font-weight: 600;
    color: #333;
  }

  &__role-tag {
    font-size: 20rpx;
    padding: 4rpx 14rpx;
    border-radius: 8rpx;
    font-weight: 500;

    &--captain {
      background: #fef3c7;
      color: #d97706;
    }

    &--vice {
      background: #ede9fe;
      color: #5B9BD5;
    }

    &--transfer {
      background: #fef3c7;
      color: #d97706;
    }
  }

  &__time {
    font-size: 22rpx;
    color: #bbb;
  }

  &__card-body {
    padding: 16rpx 28rpx;
  }

  &__info-row {
    display: flex;
    padding: 8rpx 0;
  }

  &__info-label {
    font-size: 24rpx;
    color: #999;
    width: 140rpx;
    flex-shrink: 0;
  }

  &__info-value {
    font-size: 24rpx;
    color: #333;
    flex: 1;
  }

  &__card-actions {
    display: flex;
    border-top: 1rpx solid #f5f5f5;
  }

  &__btn {
    flex: 1;
    height: 88rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28rpx;
    font-weight: 500;

    &--reject {
      background: #fff;
      color: #ef4444;
      border-right: 1rpx solid #f5f5f5;

      &:active {
        background: #fef2f2;
      }
    }

    &--approve {
      background: #fff;
      color: #07c160;

      &:active {
        background: #f0fdf4;
      }
    }
  }

  &__btn-text {
    font-size: 28rpx;
  }

  // 空状态
  &__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 120rpx 0;
  }

  &__empty-icon {
    font-size: 80rpx;
    margin-bottom: 20rpx;
  }

  &__empty-text {
    font-size: 28rpx;
    color: #999;
  }

  // 加载
  &__loading {
    display: flex;
    justify-content: center;
    padding: 60rpx 0;
  }

  &__loading-text {
    font-size: 28rpx;
    color: #999;
  }
}
</style>
