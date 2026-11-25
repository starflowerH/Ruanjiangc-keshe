<template>
  <view class="team-list">
    <NavBar title="队伍列表" show-back />
    <!-- 目标高中分类导航条 -->
    <scroll-view class="team-list__nav" scroll-x :show-scrollbar="false">
      <view class="team-list__nav-inner">
        <view
          class="team-list__nav-item"
          :class="{ 'team-list__nav-item--active': currentHighSchoolId === null }"
          @tap="selectHighSchool(null)"
        >
          <text class="team-list__nav-text">全部高中</text>
        </view>
        <view
          class="team-list__nav-item"
          :class="{ 'team-list__nav-item--active': currentHighSchoolId === hs.id }"
          v-for="hs in highSchoolList"
          :key="hs.id"
          @tap="selectHighSchool(hs.id)"
        >
          <text class="team-list__nav-text">{{ hs.name }}</text>
        </view>
      </view>
    </scroll-view>
    <view class="team-list__body">
      <!-- 搜索栏 -->
      <view class="team-list__search">
        <input
          class="team-list__search-input"
          v-model="keyword"
          placeholder="搜索队伍名称 / 队伍编号 / 高中"
          confirm-type="search"
          @confirm="onSearch"
        />
      </view>

      <!-- 统计 -->
      <view class="team-list__stat" v-if="teams.length">
        <text class="team-list__stat-text">共 {{ teams.length }} 支队伍</text>
      </view>

      <!-- 加载中 -->
      <view class="team-list__loading" v-if="loading">
        <text class="team-list__loading-text">加载中...</text>
      </view>

      <!-- 空状态 -->
      <view class="team-list__empty" v-if="!loading && !teams.length">
        <text class="team-list__empty-text">暂无队伍</text>
      </view>

      <!-- 队伍列表 -->
      <view class="team-list__content" v-for="team in teams" :key="team.id">
        <view class="team-list__card" @tap="toggleExpand(team.id)">
          <view class="team-list__card-main">
            <view class="team-list__card-left">
              <text class="team-list__card-name">{{ team.teamName || '未命名队伍' }}</text>
              <view class="team-list__card-code">
                <text class="team-list__card-code-label">编号</text>
                <text class="team-list__card-code-value">{{ team.teamCode }}</text>
              </view>
            </view>
            <view class="team-list__card-right">
              <view class="team-list__card-count">
                <text class="team-list__card-count-num">{{ team.memberCount }}</text>
                <text class="team-list__card-count-label">人</text>
              </view>
              <text class="team-list__card-arrow">{{ expandedId === team.id ? '⌃' : '⌄' }}</text>
            </view>
          </view>

          <view class="team-list__card-meta">
            <view class="team-list__meta-item" v-if="team.universityName">
              <text class="team-list__meta-label">大学</text>
              <text class="team-list__meta-value">{{ team.universityName }}</text>
            </view>
            <view class="team-list__meta-item" v-if="team.highSchoolName">
              <text class="team-list__meta-label">目标高中</text>
              <text class="team-list__meta-value">{{ team.highSchoolName }}</text>
            </view>
            <view class="team-list__meta-item" v-if="team.captainName">
              <text class="team-list__meta-label">队长</text>
              <text class="team-list__meta-value">{{ team.captainName }}</text>
            </view>
          </view>

          <!-- 展开：成员列表 -->
          <view class="team-list__members" v-if="expandedId === team.id">
            <view class="team-list__members-title">队伍成员</view>
            <view class="team-list__member" v-for="m in team.members || []" :key="m.volunteerId">
              <view class="team-list__member-avatar" :class="'team-list__member-avatar--' + (m.role || 'member').toLowerCase()">
                <text class="team-list__member-avatar-text">{{ (m.name || '?')[0] }}</text>
              </view>
              <view class="team-list__member-info">
                <view class="team-list__member-name-row">
                  <text class="team-list__member-name">{{ m.name }}</text>
                  <text v-if="m.role === 'CAPTAIN'" class="team-list__member-tag team-list__member-tag--captain">队长</text>
                </view>
                <text class="team-list__member-detail">{{ m.universityName }} · {{ m.major }}</text>
              </view>
            </view>
            <view class="team-list__members-empty" v-if="!team.members || !team.members.length">
              <text class="team-list__members-empty-text">暂无成员</text>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import NavBar from '@/components/NavBar.vue'
import { getAllTeams, getHighSchoolList } from '@/api/admin'

const loading = ref(false)
const teams = ref([])
const allTeams = ref([])
const keyword = ref('')
const expandedId = ref(null)
const highSchoolList = ref([])
const currentHighSchoolId = ref(null)

onShow(() => {
  loadHighSchools()
  loadTeams()
})

// 加载高中列表（用于导航按钮，参照高中生列表）
async function loadHighSchools() {
  try {
    const res = await getHighSchoolList()
    highSchoolList.value = res || []
  } catch (e) {
    console.error('加载高中列表失败:', e)
  }
}

async function loadTeams() {
  loading.value = true
  try {
    const res = await getAllTeams()
    allTeams.value = res || []
    applyFilter()
  } catch (e) {
    teams.value = []
    allTeams.value = []
    uni.showToast({ title: e.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

// 选择高中（导航筛选，按 id 匹配）
function selectHighSchool(id) {
  currentHighSchoolId.value = id
  applyFilter()
}

// 组合筛选：高中导航 + 关键词（队伍名/编号/高中）
function applyFilter() {
  const kw = keyword.value.trim().toUpperCase()
  const selected = highSchoolList.value.find(h => h.id === currentHighSchoolId.value)
  const selectedName = selected ? selected.name : null
  teams.value = allTeams.value.filter(t => {
    // 高中筛选（按 id 对应的高中名称匹配）
    if (currentHighSchoolId.value !== null && (t.highSchoolName || '') !== selectedName) {
      return false
    }
    // 关键词搜索
    if (!kw) return true
    return (t.teamName || '').toUpperCase().includes(kw)
      || (t.teamCode || '').toUpperCase().includes(kw)
      || (t.highSchoolName || '').toUpperCase().includes(kw)
      || (t.universityName || '').toUpperCase().includes(kw)
  })
}

function onSearch() {
  applyFilter()
}

function toggleExpand(id) {
  expandedId.value = expandedId.value === id ? null : id
}
</script>

<style lang="scss" scoped>
.team-list {
  min-height: 100vh;
  background: #f2f6fb;
  padding-bottom: 40rpx;

  // 目标高中分类导航条
  &__nav {
    background: #fff;
    white-space: nowrap;
    box-shadow: 0 4rpx 16rpx rgba(76, 122, 168, 0.06);
  }

  &__nav-inner {
    display: inline-flex;
    padding: 0 24rpx;
    height: 92rpx;
    align-items: center;
  }

  &__nav-item {
    padding: 14rpx 30rpx;
    margin-right: 16rpx;
    border-radius: 999rpx;
    background: #f4f8fc;
    flex-shrink: 0;
    max-width: 240rpx;

    &--active {
      background: linear-gradient(135deg, #6aa6dd, #4a86c4);
    }
  }

  &__nav-text {
    font-size: 26rpx;
    color: #5b708a;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    display: block;
  }

  &__nav-item--active &__nav-text {
    color: #fff;
    font-weight: 600;
  }

  &__body {
    padding: 24rpx 28rpx;
  }

  &__search {
    background: #fff;
    border-radius: 20rpx;
    padding: 8rpx 24rpx;
    margin-bottom: 20rpx;
    box-shadow: 0 4rpx 16rpx rgba(76, 122, 168, 0.06);
  }

  &__search-input {
    height: 76rpx;
    font-size: 28rpx;
    color: #33445c;
  }

  &__stat {
    margin-bottom: 20rpx;
    padding-left: 4rpx;
  }

  &__stat-text {
    font-size: 26rpx;
    color: #8aa0b8;
  }

  &__card {
    background: #fff;
    border-radius: 24rpx;
    padding: 28rpx;
    margin-bottom: 20rpx;
    box-shadow: 0 6rpx 24rpx rgba(76, 122, 168, 0.08);
  }

  &__card-main {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20rpx;
  }

  &__card-left {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 10rpx;
    margin-right: 20rpx;
  }

  &__card-name {
    font-size: 34rpx;
    font-weight: 700;
    color: #2b3f5a;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__card-code {
    display: flex;
    align-items: center;
    gap: 12rpx;
    min-width: 0;
  }

  &__card-code-label {
    font-size: 22rpx;
    color: #a0b4c8;
    flex-shrink: 0;
  }

  &__card-code-value {
    font-size: 28rpx;
    font-weight: 600;
    color: #4a86c4;
    letter-spacing: 3rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    min-width: 0;
  }

  &__card-right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 8rpx;
    flex-shrink: 0;
  }

  &__card-count {
    display: flex;
    align-items: baseline;
    gap: 4rpx;
  }

  &__card-count-num {
    font-size: 40rpx;
    font-weight: 800;
    color: #2b3f5a;
  }

  &__card-count-label {
    font-size: 24rpx;
    color: #8aa0b8;
  }

  &__card-arrow {
    font-size: 28rpx;
    color: #a9bfd4;
  }

  &__card-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;
    padding-top: 18rpx;
    border-top: 1rpx solid #f0f4f9;
  }

  &__meta-item {
    display: flex;
    align-items: center;
    gap: 8rpx;
    background: #f4f8fc;
    border-radius: 12rpx;
    padding: 6rpx 16rpx;
    max-width: 100%;
    box-sizing: border-box;
  }

  &__meta-label {
    font-size: 22rpx;
    color: #a0b4c8;
    flex-shrink: 0;
  }

  &__meta-value {
    font-size: 24rpx;
    color: #4a86c4;
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 260rpx;
  }

  &__members {
    margin-top: 24rpx;
    padding-top: 20rpx;
    border-top: 1rpx solid #f0f4f9;
  }

  &__members-title {
    font-size: 26rpx;
    font-weight: 600;
    color: #2b3f5a;
    margin-bottom: 16rpx;
  }

  &__member {
    display: flex;
    align-items: center;
    padding: 16rpx 0;
    border-bottom: 1rpx solid #f6f9fc;

    &:last-child {
      border-bottom: none;
    }
  }

  &__member-avatar {
    width: 64rpx;
    height: 64rpx;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 18rpx;
    flex-shrink: 0;

    &--captain {
      background: linear-gradient(135deg, #f5b042, #e89b1c);
    }

    &--member {
      background: linear-gradient(135deg, #a8c4e0, #8ba9c9);
    }
  }

  &__member-avatar-text {
    font-size: 26rpx;
    font-weight: 600;
    color: #fff;
  }

  &__member-info {
    flex: 1;
    min-width: 0;
  }

  &__member-name-row {
    display: flex;
    align-items: center;
    gap: 10rpx;
    margin-bottom: 4rpx;
    min-width: 0;
  }

  &__member-name {
    font-size: 28rpx;
    font-weight: 600;
    color: #33445c;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    min-width: 0;
  }

  &__member-tag {
    font-size: 18rpx;
    padding: 2rpx 10rpx;
    border-radius: 8rpx;

    &--captain {
      background: #fef3c7;
      color: #d97706;
    }

    &--vice {
      background: #e3f0fb;
      color: #4a86c4;
    }
  }

  &__member-detail {
    font-size: 22rpx;
    color: #a0b4c8;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__members-empty {
    padding: 24rpx 0;
    text-align: center;
  }

  &__members-empty-text {
    font-size: 26rpx;
    color: #a9bfd4;
  }

  &__loading {
    padding: 120rpx 0;
    text-align: center;
  }

  &__loading-text {
    font-size: 28rpx;
    color: #8aa0b8;
  }

  &__empty {
    padding: 120rpx 0;
    text-align: center;
  }

  &__empty-text {
    font-size: 28rpx;
    color: #8aa0b8;
  }
}
</style>
