<template>
  <view class="teacher-home">
    <!-- 顶部头部 -->
    <view class="teacher-home__header">
      <view class="teacher-home__header-bg"></view>
      <view class="teacher-home__header-top">
        <view class="teacher-home__user">
          <view class="teacher-home__avatar">
            <text class="teacher-home__avatar-text">👨‍🏫</text>
          </view>
          <view class="teacher-home__user-info">
            <text class="teacher-home__name">{{ userName }}</text>
            <text class="teacher-home__uni">{{ universityName }}</text>
          </view>
        </view>
      </view>
      <view class="teacher-home__greeting">
        <text class="teacher-home__greeting-text">{{ greetingText }}</text>
      </view>
    </view>

    <!-- 统计卡片（可点击跳转查看详情） -->
    <view class="teacher-home__stats" v-if="canVolunteerAudit || canTeamManage">
      <view v-if="canVolunteerAudit" class="stat-card stat-card--volunteer" @tap="goTo('/pages/admin/audit-list?status=')">
        <text class="stat-card__num">{{ stats.volunteerCount }}</text>
        <text class="stat-card__label">本校志愿者</text>
      </view>
      <view v-if="canVolunteerAudit" class="stat-card stat-card--pending" @tap="goTo('/pages/admin/audit-list?status=PENDING')">
        <text class="stat-card__num">{{ stats.pendingCount }}</text>
        <text class="stat-card__label">待审核报名</text>
      </view>
      <view v-if="canTeamManage" class="stat-card stat-card--team" @tap="goTo('/pages/teacher/team-manage')">
        <text class="stat-card__num">{{ stats.teamCount }}</text>
        <text class="stat-card__label">宣传队伍</text>
      </view>
    </view>
    <!-- 统计加载失败提示：仅在有权限但加载失败时提示，避免误导 -->
    <view v-if="statsLoadFailed && (canVolunteerAudit || canTeamManage)" class="teacher-home__stats-error">
      <text class="teacher-home__stats-error-text">⚠️ 统计数据加载失败，请下拉重试</text>
    </view>

    <!-- 功能菜单 -->
    <view class="teacher-home__menu">
      <!-- 志愿者管理 -->
      <view class="teacher-home__section" v-if="canVolunteerAudit || canTeamManage">
        <text class="teacher-home__section-title">👥 志愿者管理</text>
        <view v-if="canVolunteerAudit" class="menu-card" @tap="goTo('/pages/admin/audit-list')">
          <view class="menu-card__icon menu-card__icon--audit">📋</view>
          <view class="menu-card__info">
            <text class="menu-card__title">报名审核</text>
            <text class="menu-card__desc">审核大学生志愿者的报名申请</text>
          </view>
          <view class="menu-card__badge" v-if="stats.pendingCount > 0">
            <text class="menu-card__badge-text">{{ stats.pendingCount }}</text>
          </view>
          <text class="menu-card__arrow">›</text>
        </view>
        <view v-if="canTeamManage" class="menu-card" @tap="goTo('/pages/teacher/team-manage')">
          <view class="menu-card__icon menu-card__icon--team">👥</view>
          <view class="menu-card__info">
            <text class="menu-card__title">组队管理</text>
            <text class="menu-card__desc">按目标高中自动创建未组队队伍</text>
          </view>
          <text class="menu-card__arrow">›</text>
        </view>
      </view>

      <!-- 宣传管理 -->
      <view class="teacher-home__section" v-if="canPromotionManage">
        <text class="teacher-home__section-title">📢 宣传管理</text>
        <view class="menu-card" @tap="goTo('/pages/teacher/promotion')">
          <view class="menu-card__icon menu-card__icon--promo">🎨</view>
          <view class="menu-card__info">
            <text class="menu-card__title">宣传资料管理</text>
            <text class="menu-card__desc">编辑本校简介、上传宣传图片和内容</text>
          </view>
          <text class="menu-card__arrow">›</text>
        </view>
        <view class="menu-card" @tap="goTo('/pages/admin/audit-list?status=APPROVED')">
          <view class="menu-card__icon menu-card__icon--promo">👤</view>
          <view class="menu-card__info">
            <text class="menu-card__title">宣传资料人员授权</text>
            <text class="menu-card__desc">向本校已通过大学生下发或撤销管理权限</text>
          </view>
          <text class="menu-card__arrow">›</text>
        </view>
      </view>

      <view class="teacher-home__section" v-if="canForumModeration">
        <text class="teacher-home__section-title">论坛管理</text>
        <view class="menu-card" @tap="goTo('/pages/volunteer/forum-moderation')">
          <view class="menu-card__icon menu-card__icon--forum">✓</view>
          <view class="menu-card__info">
            <text class="menu-card__title">本校论坛治理</text>
            <text class="menu-card__desc">审核帖子、举报、申诉和精华内容</text>
          </view>
          <text class="menu-card__arrow">›</text>
        </view>
        <view class="menu-card" @tap="goTo('/pages/admin/audit-list?status=APPROVED')">
          <view class="menu-card__icon menu-card__icon--forum">👤</view>
          <view class="menu-card__info">
            <text class="menu-card__title">论坛审核员授权</text>
            <text class="menu-card__desc">向本校已通过大学生下发或撤销审核权限</text>
          </view>
          <text class="menu-card__arrow">›</text>
        </view>
      </view>

      <!-- 无权限提示：管理员已撤销全部权限时，避免空白页困惑 -->
      <view class="teacher-home__section" v-if="!canVolunteerAudit && !canTeamManage && !canPromotionManage && !canForumModeration">
        <view class="teacher-home__no-perm">
          <text class="teacher-home__no-perm-icon">🔒</text>
          <text class="teacher-home__no-perm-text">暂无业务权限</text>
          <text class="teacher-home__no-perm-sub">您当前没有任何管理权限，请联系超级管理员开通</text>
        </view>
      </view>

      <!-- 账号操作 -->
      <view class="teacher-home__section">
        <text class="teacher-home__section-title">👤 账号操作</text>
        <view class="menu-card">
          <view class="menu-card__icon menu-card__icon--account">账</view>
          <view class="menu-card__info">
            <text class="menu-card__title">当前账号</text>
            <text class="menu-card__desc">{{ userName }} · {{ universityName }}</text>
          </view>
        </view>
        <view class="menu-card" @tap="handleLogout">
          <view class="menu-card__icon menu-card__icon--logout">🚪</view>
          <view class="menu-card__info">
            <text class="menu-card__title">退出登录</text>
            <text class="menu-card__desc">退出当前管理账号</text>
          </view>
          <text class="menu-card__arrow">›</text>
        </view>
      </view>
    </view>

    <!-- 底部 -->
    <view class="teacher-home__footer">
      <text class="teacher-home__footer-text">大学老师端 v1.0.0</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/modules/user'
import * as adminApi from '@/api/admin'

const userStore = useUserStore()

const stats = ref({
  volunteerCount: 0,
  pendingCount: 0,
  teamCount: 0
})
// 统计加载失败标志：真实失败时提示，而非显示误导性的 0/0/0
const statsLoadFailed = ref(false)

const userName = computed(() => userStore.userInfo?.name || userStore.userInfo?.nickname || '老师')
const universityName = computed(() => userStore.userInfo?.universityName || '本校')
const canVolunteerAudit = computed(() => userStore.hasPermission('teacherVolunteerAudit'))
const canTeamManage = computed(() => userStore.hasPermission('teacherTeamManage'))
const canPromotionManage = computed(() => userStore.hasPermission('teacherPromotionManage'))
const canForumModeration = computed(() => userStore.hasPermission('teacherForumModeration'))

const greetingText = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '早上好！新的一天，看看宣传进展如何？'
  if (hour < 18) return '下午好！持续关注志愿者报名情况。'
  return '晚上好！今天的工作辛苦了。'
})

onShow(async () => {
  if (!userStore.isLoggedIn || !userStore.isTeacher) {
    uni.reLaunch({ url: '/pages/teacher/login' })
    return
  }
  // 实时拉取权限：管理员可能刚下发/撤销权限，本地缓存的 permissions 已过期
  await refreshPermissions()
  if (canVolunteerAudit.value || canTeamManage.value) {
    await loadStats()
  }
})

// 实时刷新老师权限（更新 userInfo.permissions，驱动菜单显隐同步）
async function refreshPermissions() {
  try {
    const perms = await adminApi.getTeacherPermissions()
    if (perms && typeof perms === 'object') {
      // 后端返回 { teacherVolunteerAudit: true, teacherTeamManage: false, ... }
      userStore.updateUserInfo({ permissions: perms })
    }
  } catch (e) {
    // 拉取失败不阻塞首页，保留本地缓存权限（后端操作时会实时拦截）
    console.error('刷新老师权限失败:', e)
  }
}

async function loadStats() {
  try {
    // 加载本校验数据统计
    const statData = await adminApi.getTeacherStats()
    if (statData && typeof statData === 'object') {
      stats.value = {
        volunteerCount: statData.volunteerCount || 0,
        pendingCount: statData.pendingCount || 0,
        teamCount: statData.teamCount || 0
      }
      statsLoadFailed.value = false
    }
  } catch (e) {
    console.error('加载统计数据失败:', e)
    // 尝试从志愿者列表获取统计数据
    await loadStatsFromList()
  }
}

// 备选：从志愿者列表获取统计
async function loadStatsFromList() {
  try {
    const list = await adminApi.getTeacherVolunteerList({ page: 1, size: 100 })
    const records = list?.records || []
    stats.value.volunteerCount = records.filter(v => v.status === 'APPROVED').length
    stats.value.pendingCount = records.filter(v => v.status === 'PENDING').length
    stats.value.teamCount = 0
  } catch (e) {
    // 统计与列表都失败：标记失败，界面显示"加载失败"而非误导性的 0/0/0
    console.error('加载志愿者列表失败:', e)
    statsLoadFailed.value = true
  }
}

function goTo(url) {
  uni.navigateTo({ url })
}

function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        userStore.logout()
        // 与其他端保持一致：退出后回身份选择页（可重新选择学生/大学生/老师入口）
        uni.reLaunch({ url: '/pages/index/index' })
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.teacher-home {
  min-height: 100vh;
  background: #f0f2f5;

  // 头部
  &__header {
    position: relative;
    padding: 0 32rpx 48rpx;

    &-bg {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 340rpx;
      background: linear-gradient(135deg, #3A7BAA 0%, #4A8BC4 50%, #4A8BC4 100%);
      border-radius: 0 0 40rpx 40rpx;

      &::after {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: radial-gradient(ellipse at 70% 30%, rgba(255,255,255,0.08) 0%, transparent 60%);
      }
    }

    &-top {
      position: relative;
      z-index: 1;
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: 60rpx;
    }

    &-greeting {
      position: relative;
      z-index: 1;
      margin-top: 24rpx;

      &-text {
        font-size: 26rpx;
        color: rgba(255, 255, 255, 0.85);
      }
    }
  }

  &__user {
    display: flex;
    align-items: center;
    gap: 16rpx;
  }

  &__avatar {
    width: 72rpx;
    height: 72rpx;
    background: rgba(255, 255, 255, 0.15);
    border-radius: 20rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    backdrop-filter: blur(10px);
    border: 2rpx solid rgba(255, 255, 255, 0.2);

    &-text {
      font-size: 36rpx;
    }
  }

  &__user-info {
    display: flex;
    flex-direction: column;
  }

  &__name {
    font-size: 32rpx;
    font-weight: 700;
    color: #fff;
  }

  &__uni {
    font-size: 22rpx;
    color: rgba(255, 255, 255, 0.75);
    margin-top: 2rpx;
  }

  // 统计卡片
  &__stats {
    display: flex;
    gap: 16rpx;
    padding: 0 24rpx;
    margin-top: -28rpx;
    position: relative;
    z-index: 2;
    margin-bottom: 32rpx;
  }

  &__stats-error {
    padding: 0 24rpx;
    margin-top: -16rpx;
    margin-bottom: 24rpx;

    &-text {
      font-size: 24rpx;
      color: #e6a23c;
      background: #fdf6ec;
      border-radius: 12rpx;
      padding: 12rpx 20rpx;
      display: block;
    }
  }

  // 无权限提示
  &__no-perm {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 48rpx 24rpx;
    background: #fff;
    border-radius: 20rpx;

    &-icon {
      font-size: 64rpx;
      margin-bottom: 16rpx;
    }

    &-text {
      font-size: 30rpx;
      font-weight: 600;
      color: #374151;
      margin-bottom: 8rpx;
    }

    &-sub {
      font-size: 24rpx;
      color: #9ca3af;
      text-align: center;
      line-height: 36rpx;
    }
  }

  // 菜单
  &__menu {
    padding: 0 24rpx;
  }

  &__section {
    margin-bottom: 28rpx;

    &-title {
      font-size: 28rpx;
      font-weight: 700;
      color: #2d3748;
      display: block;
      margin-bottom: 16rpx;
    }
  }

  &__footer {
    text-align: center;
    padding: 60rpx 0 40rpx;

    &-text {
      font-size: 22rpx;
      color: #a0aec0;
    }
  }
}

// 统计卡片
.stat-card {
  flex: 1;
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx 20rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 6rpx;
  }

  &--volunteer::before { background: #3182ce; }
  &--pending::before { background: #ecc94b; }
  &--team::before { background: #48bb78; }

  &__num {
    font-size: 48rpx;
    font-weight: 800;
    color: #2d3748;
  }

  &__label {
    font-size: 22rpx;
    color: #a0aec0;
    margin-top: 6rpx;
  }
}

// 菜单卡片
.menu-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx 24rpx;
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
  transition: all 0.15s;

  &:active {
    transform: scale(0.98);
    background: #f7fafc;
  }

  &__icon {
    width: 72rpx;
    height: 72rpx;
    border-radius: 18rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 20rpx;
    font-size: 36rpx;
    flex-shrink: 0;

    &--audit { background: #ebf8ff; }
    &--team { background: #f0fff4; }
    &--promo { background: #faf5ff; }
    &--forum { background: #ecfdf5; color: #047857; }
    &--student { background: #fff5f5; }
    &--volunteer { background: #fffaf0; }
    &--logout { background: #fef2f2; }
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__title {
    font-size: 28rpx;
    font-weight: 600;
    color: #2d3748;
    display: block;
    margin-bottom: 4rpx;
  }

  &__desc {
    font-size: 22rpx;
    color: #a0aec0;
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__badge {
    background: #e53e3e;
    border-radius: 999rpx;
    min-width: 44rpx;
    height: 44rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 8rpx;

    &-text {
      font-size: 22rpx;
      color: #fff;
      font-weight: 600;
      padding: 0 8rpx;
    }
  }

  &__arrow {
    font-size: 36rpx;
    color: #cbd5e0;
    flex-shrink: 0;
    margin-left: 8rpx;
  }
}
</style>
