<template>
  <view class="dashboard">
    <NavBar title="管理后台" :showBack="false">
      <template #left>
        <view class="dashboard__refresh" @tap="handleRefresh" aria-label="刷新数据">
          <text>↻</text>
        </view>
      </template>
    </NavBar>
    <!-- 管理员信息 -->
    <view class="dashboard__header">
      <view class="dashboard__admin-info">
        <text class="dashboard__admin-icon">👑</text>
        <view class="dashboard__admin-detail">
          <text class="dashboard__admin-name">{{ userStore.userInfo?.nickname || '管理员' }}</text>
          <text class="dashboard__admin-role">{{ roleText }}</text>
        </view>
      </view>
    </view>

    <!-- 数据统计卡片（可点击查看详情） -->
    <view class="dashboard__stats">
      <view class="stat-card" @tap="goTo('/pages/admin/audit-list?status=')">
        <text class="stat-card__number">{{ stats.volunteerCount || 0 }}</text>
        <text class="stat-card__label">志愿者总数 ›</text>
      </view>
      <view class="stat-card" @tap="goTo('/pages/admin/audit-list?status=PENDING')">
        <text class="stat-card__number stat-card__number--warning">{{ totalPending }}</text>
        <text class="stat-card__label">待审核 ›</text>
      </view>
      <view class="stat-card" @tap="goTo('/pages/admin/team-list')">
        <text class="stat-card__number">{{ stats.teamCount || 0 }}</text>
        <text class="stat-card__label">队伍数量 ›</text>
      </view>
      <view class="stat-card">
        <text class="stat-card__number">{{ stats.qaCount || 0 }}</text>
        <text class="stat-card__label">问答总数</text>
      </view>
      <view class="stat-card" @tap="goTo('/pages/admin/student-list')">
        <text class="stat-card__number">{{ stats.studentCount || 0 }}</text>
        <text class="stat-card__label">高中生总数 ›</text>
      </view>
      <view class="stat-card">
        <text class="stat-card__number">{{ stats.commentCount || 0 }}</text>
        <text class="stat-card__label">评论总数</text>
      </view>
    </view>

    <!-- 功能菜单 -->
    <view class="dashboard__menu">
      <!-- 超级管理员专属 -->
      <view class="menu-group" v-if="userStore.isSuperAdmin">
        <text class="menu-group__title">系统管理</text>
        <view class="menu-item" @tap="goTo('/pages/admin/audit-list')">
          <text class="menu-item__icon">📋</text>
          <text class="menu-item__text">志愿者审核</text>
          <text class="menu-item__arrow">›</text>
        </view>
        <view class="menu-item" @tap="goTo('/pages/admin/audit-list?type=qa')">
          <text class="menu-item__icon">💬</text>
          <text class="menu-item__text">问答审核</text>
          <text class="menu-item__arrow">›</text>
        </view>
        <view class="menu-item" @tap="goTo('/pages/volunteer/forum-moderation')">
          <text class="menu-item__icon">✓</text>
          <text class="menu-item__text">论坛治理</text>
          <text class="menu-item__arrow">›</text>
        </view>
        <view class="menu-item" @tap="handleBuildTeams">
          <text class="menu-item__icon">👥</text>
          <text class="menu-item__text">自动创建未组队队伍</text>
          <text class="menu-item__arrow">›</text>
        </view>
        <view class="menu-item" @tap="goTo('/pages/admin/team-audit')">
          <text class="menu-item__icon">🏅</text>
          <text class="menu-item__text">队伍角色审核</text>
          <text class="menu-item__arrow">›</text>
        </view>
        <view class="menu-item" @tap="goTo('/pages/admin/teacher-audit')">
          <text class="menu-item__icon">🧑‍🏫</text>
          <text class="menu-item__text">老师审核</text>
          <text class="menu-item__arrow">›</text>
        </view>
      </view>

      <!-- 老师专属 -->
      <view class="menu-group" v-if="userStore.isTeacher">
        <text class="menu-group__title">本校管理</text>
        <view class="menu-item" @tap="goTo('/pages/admin/audit-list')">
          <text class="menu-item__icon">📋</text>
          <text class="menu-item__text">本校志愿者审核</text>
          <text class="menu-item__arrow">›</text>
        </view>
        <view class="menu-item" @tap="handleBuildTeams">
          <text class="menu-item__icon">👥</text>
          <text class="menu-item__text">本校自动创建未组队队伍</text>
          <text class="menu-item__arrow">›</text>
        </view>
      </view>

      <!-- 普通管理员（只读） -->
      <view class="menu-group" v-if="userStore.isNormalAdmin">
        <text class="menu-group__title">数据查看</text>
        <view class="menu-item">
          <text class="menu-item__icon">📊</text>
          <text class="menu-item__text">数据看板（只读）</text>
          <text class="menu-item__badge">已开放</text>
        </view>
      </view>
    </view>

    <view class="dashboard__logout-btn" @tap="handleLogout">退出登录</view>
    <text class="dashboard__version">管理后台 v1.0.0</text>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/modules/user'
import NavBar from '@/components/NavBar.vue'
import * as adminApi from '@/api/admin'
import { safeReLaunch } from '@/utils/nav'

const userStore = useUserStore()

const stats = ref({
  volunteerCount: 0,
  pendingCount: 0,
  teamCount: 0,
  qaCount: 0,
  studentCount: 0,
  commentCount: 0
})

// 队伍角色申请待审核数（超管可查看）
const teamApplyPendingCount = ref(0)

// 待审核总数 = 志愿者待审核 + 队伍角色申请待审核
const totalPending = computed(() => {
  return (stats.value.pendingCount || 0) + teamApplyPendingCount.value
})

const roleText = computed(() => {
  if (userStore.isSuperAdmin) return '超级管理员'
  if (userStore.isNormalAdmin) return '普通管理员'
  if (userStore.isTeacher) return `大学老师 · ${userStore.userInfo?.universityName || ''}`
  return ''
})

onShow(async () => {
  if (!userStore.isLoggedIn) {
    safeReLaunch('/pages/common/login?role=admin')
    return
  }
  if (userStore.isTeacher) {
    uni.reLaunch({ url: '/pages/teacher/home' })
    return
  }
  if (!userStore.isAdmin) {
    uni.reLaunch({ url: '/pages/index/index' })
    return
  }
  await loadStats()
})

async function loadStats() {
  try {
    const data = userStore.isTeacher
      ? await adminApi.getTeacherStats()
      : await adminApi.getStats()
    if (data) {
      stats.value = data
    }
    // 队伍角色申请待审核数（仅超级管理员可查看，接口仅返回待审核申请）
    if (userStore.isSuperAdmin) {
      try {
        const apps = await adminApi.getTeamApplications()
        teamApplyPendingCount.value = (apps || []).length
      } catch (e) {
        console.error('加载队伍申请统计失败:', e)
      }
    }
  } catch (e) {
    console.error('加载统计数据失败:', e)
  }
}

function goTo(url) {
  uni.navigateTo({ url })
}

// 退出登录：确认后清空登录态并回身份选择页
function handleLogout() {
  uni.showModal({
    title: '退出登录',
    content: '确定退出当前账号吗？',
    success: (r) => {
      if (r.confirm) {
        userStore.logout()
        safeReLaunch('/pages/index/index')
      }
    }
  })
}

async function handleRefresh() {
  uni.showLoading({ title: '刷新中...' })
  try {
    await loadStats()
    uni.showToast({ title: '已刷新', icon: 'success' })
  } finally {
    uni.hideLoading()
  }
}

async function handleBuildTeams() {
  uni.showModal({
    title: '确认操作',
    content: '确定要开始吗？系统将按大学和目标高中，为已审核且未组队的志愿者创建队伍。',
    success: async (res) => {
      if (res.confirm) {
        uni.showLoading({ title: '组队中...' })
        try {
          const result = await adminApi.buildTeams(userStore.isTeacher)
          uni.hideLoading()
          uni.showToast({ title: result || '组队完成', icon: 'success' })
        } catch (e) {
          uni.hideLoading()
          uni.showToast({ title: '组队失败', icon: 'none' })
        }
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.dashboard {
  min-height: 100vh;
  background: #F2F6FB;

  &__header {
    background: linear-gradient(135deg, #5B9BD5, #4A8BC4);
    padding: 40rpx 32rpx;
  }

  &__admin-info {
    display: flex;
    align-items: center;
  }

  &__admin-icon {
    font-size: 48rpx;
    margin-right: 16rpx;
  }

  &__admin-detail {
    flex: 1;
  }

  &__admin-name {
    font-size: 32rpx;
    font-weight: 600;
    color: #fff;
    display: block;
  }

  &__admin-role {
    font-size: 24rpx;
    color: rgba(255, 255, 255, 0.8);
  }

  &__logout-btn {
    margin: 48rpx 32rpx 0;
    padding: 24rpx;
    text-align: center;
    border-radius: 16rpx;
    background: #fff0ef;
    color: #d15e5a;
    font-size: 28rpx;
    font-weight: 600;
  }

  &__stats {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16rpx;
    padding: 24rpx;
    margin-top: -20rpx;
  }

  &__menu {
    padding: 0 24rpx;
  }

  &__version {
    font-size: 22rpx;
    color: #999;
    text-align: center;
    display: block;
    padding: 40rpx;
  }

  &__refresh {
    width: 64rpx;
    height: 64rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 44rpx;
    line-height: 1;
  }
}

.stat-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  text-align: center;

  &__number {
    font-size: 48rpx;
    font-weight: 700;
    color: #5B9BD5;
    display: block;

    &--warning {
      color: #faad14;
    }
  }

  &__label {
    font-size: 24rpx;
    color: #999;
    margin-top: 8rpx;
  }
}

.menu-group {
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 24rpx;
  overflow: hidden;

  &__title {
    font-size: 26rpx;
    font-weight: 600;
    color: #666;
    padding: 20rpx 24rpx 12rpx;
  }
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 28rpx 24rpx;
  border-bottom: 2rpx solid #f8f8f8;

  &:last-child {
    border-bottom: none;
  }

  &__icon {
    font-size: 36rpx;
    margin-right: 16rpx;
  }

  &__text {
    flex: 1;
    font-size: 28rpx;
    color: #333;
  }

  &__arrow {
    font-size: 32rpx;
    color: #ccc;
  }

  &__badge {
    font-size: 22rpx;
    color: #52c41a;
    background: rgba(82, 196, 26, 0.1);
    padding: 4rpx 16rpx;
    border-radius: 999rpx;
  }
}
</style>
